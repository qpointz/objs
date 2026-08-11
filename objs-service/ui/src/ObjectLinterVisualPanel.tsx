import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
  type MouseEvent as ReactMouseEvent,
  type PointerEvent as ReactPointerEvent,
} from 'react'
import {
  ActionIcon,
  Alert,
  Anchor,
  Badge,
  Box,
  Button,
  Code,
  Group,
  Loader,
  Menu,
  Modal,
  Paper,
  ScrollArea,
  Select,
  Stack,
  Switch,
  Tabs,
  Text,
  Tooltip,
} from '@mantine/core'
import { IconX } from '@tabler/icons-react'
import { getSchema, getTypeEdges, listSchemas, schemaDetailPath, toGraphData } from './api'
import { Link } from 'react-router-dom'
import { AddObjectsPanel } from './AddObjectsPanel'
import { colorForType, nodeLabel } from './color'
import {
  GraphCanvas,
  type GraphCanvasHandle,
  type GraphLayout,
} from './GraphCanvas'
import { edgeStatus, entityStatus, type GraphDraftState } from './graphDraft'
import { newEntityId } from './graphDraft'
import { payloadFieldKindsByTypeVersion } from './payloadFieldKinds'
import type { QueryExecStats } from './queryExecStats'
import {
  AnnotationsEditor,
  PayloadInspector,
  SchemaInstanceForm,
  defaultValueForSchema,
  migrateNeedsConfirm,
  migratePayloadByKey,
} from './SchemaInstanceForm'
import { projectIdentityPaths } from './identityProjection'
import { applyTypeHighlightDimming, toggleTypeInSet } from './typeHighlightDimming'
import type {
  BoMAllowedEdgeRule,
  BoMEdge,
  BoMEntity,
  BoMSchema,
  GraphLink,
  GraphNode,
  GraphSelection,
} from './types'

const GRAPH_LAYOUTS: { value: GraphLayout; label: string }[] = [
  { value: 'TB', label: 'Top to bottom' },
  { value: 'BT', label: 'Bottom to top' },
  { value: 'LR', label: 'Left to right' },
  { value: 'RL', label: 'Right to left' },
]

const SIDE_PANE_WIDTH_KEY = 'objs.ui.composer.sidePaneWidth'
const DEFAULT_SIDE_PANE_WIDTH = 360
const ADD_OBJECTS_SIDE_PANE_FLOOR = 420
const MIN_SIDE_PANE_WIDTH = 280
const MIN_CANVAS_WIDTH = 240
const SPLITTER_WIDTH = 8

function clamp(n: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, n))
}

function readSidePaneWidth(): number {
  try {
    const raw = window.localStorage.getItem(SIDE_PANE_WIDTH_KEY)
    if (raw == null) return DEFAULT_SIDE_PANE_WIDTH
    const n = Number(raw)
    if (!Number.isFinite(n)) return DEFAULT_SIDE_PANE_WIDTH
    return clamp(n, MIN_SIDE_PANE_WIDTH, 720)
  } catch {
    return DEFAULT_SIDE_PANE_WIDTH
  }
}

function writeSidePaneWidth(width: number) {
  try {
    window.localStorage.setItem(SIDE_PANE_WIDTH_KEY, String(Math.round(width)))
  } catch {
    /* ignore */
  }
}

function isChangedStatus(status: GraphNode['draftStatus'] | GraphLink['draftStatus']): boolean {
  return status === 'new' || status === 'modified' || status === 'deleted'
}

/** When changes-only is on, dim unchanged nodes/edges but keep them for stable layout. */
export function applyChangesOnlyDimming(
  nodes: GraphNode[],
  links: GraphLink[],
  changesOnly: boolean,
): { nodes: GraphNode[]; links: GraphLink[] } {
  if (!changesOnly) {
    return {
      nodes: nodes.map((n) => (n.dimmed ? { ...n, dimmed: false } : n)),
      links: links.map((l) => (l.dimmed ? { ...l, dimmed: false } : l)),
    }
  }
  return {
    nodes: nodes.map((n) => ({
      ...n,
      dimmed: !isChangedStatus(n.draftStatus),
    })),
    links: links.map((l) => ({
      ...l,
      dimmed: !isChangedStatus(l.draftStatus),
    })),
  }
}

/** Overlay wins on key collision (Paste Merge). */
export function mergeAnnotations(
  base: Record<string, string>,
  overlay: Record<string, string>,
): Record<string, string> {
  return { ...base, ...overlay }
}

export function versionsForEntityType(schemas: BoMSchema[], type: string | null): string[] {
  if (!type) return []
  return schemas
    .filter((s) => s.type === type && s.usage === 'ENTITY')
    .map((s) => s.version)
    .sort((a, b) => b.localeCompare(a))
}

function entityToGraphNode(entity: BoMEntity): GraphNode {
  return {
    id: entity.id,
    name: nodeLabel(entity.payload, entity.id),
    type: entity.type,
    schemaVersion: entity.schemaVersion ?? '?',
    color: colorForType(entity.type),
    payload: entity.payload ?? {},
    annotations: entity.annotations ?? {},
  }
}

type Props = {
  draftState: GraphDraftState
  canvasDocument: { entities: BoMEntity[]; edges: BoMEdge[] }
  selection: GraphSelection | null
  onSelect: (selection: GraphSelection | null) => void
  onUpsertEntity: (entity: BoMEntity) => void
  onUpsertEdge: (edge: BoMEdge) => void
  onRemoveEntity: (id: string) => void
  onRemoveEdge: (id: string) => void
  onExcludeEntity: (id: string) => void
  onExcludeEdge: (id: string) => void
  onRestoreDeletedEntity: (id: string) => void
  onRestoreDeletedEdge: (id: string) => void
  onRevertEntityChanges: (id: string) => void
  onRevertEdgeChanges: (id: string) => void
  /** Entity ids failing the latest Validate (red blink until result cleared). */
  invalidEntityIds?: ReadonlySet<string>
  /** Edge ids failing the latest Validate. */
  invalidEdgeIds?: ReadonlySet<string>
  /** Add objects side panel (replaces edit form while open). */
  addObjectsOpen?: boolean
  /** Toggle Add objects side pane (Visual toolbar). */
  onToggleAddObjects?: () => void
  /** Current graph: Add objects Search scopes to members; when null, Search runs across all graphs. */
  graphId?: string | null
  onCloseAddObjects?: () => void
  addObjectsMatcher?: unknown | null
  addObjectsAutoSearch?: boolean
  addObjectsAutoAddAll?: boolean
  draftEntityIds?: ReadonlySet<string>
  onMergeEntities?: (entities: BoMEntity[]) => void
  onMergeEdges?: (edges: BoMEdge[]) => void
  onAddObjectsSearchSuccess?: (matcherBody: unknown, stats: QueryExecStats) => void
  /** Last Add objects Search stats (Visual L2 badge). */
  addObjectsStats?: QueryExecStats | null
  /** True while the opened graph is loading into the Visual canvas. */
  canvasLoading?: boolean
  /** Graph-header annotations (empty selection side pane). */
  graphAnnotations?: Record<string, string>
  onGraphAnnotationsChange?: (next: Record<string, string>) => void
}

export type ObjectLinterVisualPanelHandle = {
  focusNode: (nodeId: string) => void
  openNew: () => void
  openNewLinked: () => void
  openLink: () => void
}

function latestEntitySchemas(schemas: BoMSchema[]): BoMSchema[] {
  const byType = new Map<string, BoMSchema>()
  for (const schema of schemas.filter((s) => s.usage === 'ENTITY')) {
    const prev = byType.get(schema.type)
    if (!prev || schema.version.localeCompare(prev.version) > 0) {
      byType.set(schema.type, schema)
    }
  }
  return [...byType.values()].sort((a, b) => a.type.localeCompare(b.type))
}

/** ENTITY schemas for the same type (version list) for Schema ▾ migrate. */
export function entitySchemasSameType(schemas: BoMSchema[], type: string | null): BoMSchema[] {
  if (!type) return []
  return schemas
    .filter((s) => s.usage === 'ENTITY' && s.type === type)
    .slice()
    .sort((a, b) => b.version.localeCompare(a.version))
}

export const ObjectLinterVisualPanel = forwardRef<ObjectLinterVisualPanelHandle, Props>(
  function ObjectLinterVisualPanel(
    {
      draftState,
      canvasDocument,
      selection,
      onSelect,
      onUpsertEntity,
      onUpsertEdge,
      onRemoveEntity,
      onRemoveEdge,
      onExcludeEntity,
      onExcludeEdge,
      onRestoreDeletedEntity,
      onRestoreDeletedEdge,
      onRevertEntityChanges,
      onRevertEdgeChanges,
      invalidEntityIds,
      invalidEdgeIds,
      addObjectsOpen = false,
      onToggleAddObjects,
      graphId = null,
      onCloseAddObjects,
      addObjectsMatcher = null,
      addObjectsAutoSearch = false,
      addObjectsAutoAddAll = false,
      draftEntityIds,
      onMergeEntities,
      onMergeEdges,
      onAddObjectsSearchSuccess,
      addObjectsStats = null,
      canvasLoading = false,
      graphAnnotations = {},
      onGraphAnnotationsChange,
    },
    ref,
  ) {
  const document = canvasDocument
  const liveDocument = draftState.document
  const [changesOnly, setChangesOnly] = useState(false)
  const [highlightedTypes, setHighlightedTypes] = useState<Set<string>>(() => new Set())
  const graphRef = useRef<GraphCanvasHandle>(null)
  const splitHostRef = useRef<HTMLDivElement>(null)
  const dragRef = useRef<{ startX: number; startWidth: number } | null>(null)
  const [sidePaneWidth, setSidePaneWidth] = useState(readSidePaneWidth)
  const resolvedDraftEntityIds = draftEntityIds ?? new Set(liveDocument.entities.map((e) => e.id))

  useEffect(() => {
    writeSidePaneWidth(sidePaneWidth)
  }, [sidePaneWidth])

  useEffect(() => {
    if (!addObjectsOpen) return
    setSidePaneWidth((w) => (w < ADD_OBJECTS_SIDE_PANE_FLOOR ? ADD_OBJECTS_SIDE_PANE_FLOOR : w))
  }, [addObjectsOpen])

  const onSplitterPointerDown = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      e.preventDefault()
      dragRef.current = { startX: e.clientX, startWidth: sidePaneWidth }
      e.currentTarget.setPointerCapture(e.pointerId)
    },
    [sidePaneWidth],
  )

  const onSplitterPointerMove = useCallback((e: ReactPointerEvent<HTMLDivElement>) => {
    const drag = dragRef.current
    if (drag == null) return
    const host = splitHostRef.current
    if (host == null) return
    const hostWidth = host.clientWidth
    const maxWidth = Math.max(
      MIN_SIDE_PANE_WIDTH,
      hostWidth - MIN_CANVAS_WIDTH - SPLITTER_WIDTH,
    )
    // Dragging the splitter left increases pane width.
    const next = clamp(drag.startWidth - (e.clientX - drag.startX), MIN_SIDE_PANE_WIDTH, maxWidth)
    setSidePaneWidth(next)
  }, [])

  const onSplitterPointerUp = useCallback((e: ReactPointerEvent<HTMLDivElement>) => {
    dragRef.current = null
    if (e.currentTarget.hasPointerCapture(e.pointerId)) {
      e.currentTarget.releasePointerCapture(e.pointerId)
    }
  }, [])

  const [layout, setLayout] = useState<GraphLayout>('TB')
  const [schemas, setSchemas] = useState<BoMSchema[]>([])
  const [schemasError, setSchemasError] = useState<string | null>(null)
  const fieldKindsByTypeVersion = useMemo(
    () => payloadFieldKindsByTypeVersion(schemas),
    [schemas],
  )
  const graphView = useMemo(() => {
    const base = toGraphData(canvasDocument)
    return {
      nodes: base.nodes.map((n) => ({
        ...n,
        draftStatus: entityStatus(draftState, n.id),
        validationError: invalidEntityIds?.has(n.id) === true,
        payloadFieldKinds: fieldKindsByTypeVersion.get(`${n.type}@${n.schemaVersion}`),
      })),
      links: base.links.map((l) => ({
        ...l,
        draftStatus: edgeStatus(draftState, l.id),
        validationError: invalidEdgeIds?.has(l.id) === true,
      })),
    }
  }, [canvasDocument, draftState, fieldKindsByTypeVersion, invalidEdgeIds, invalidEntityIds])
  const types = useMemo(() => {
    const set = new Map<string, string>()
    for (const n of graphView.nodes) {
      if (!set.has(n.type)) set.set(n.type, colorForType(n.type))
    }
    return [...set.entries()].sort(([a], [b]) => a.localeCompare(b))
  }, [graphView.nodes])
  const displayGraph = useMemo(() => {
    const withChanges = applyChangesOnlyDimming(graphView.nodes, graphView.links, changesOnly)
    return applyTypeHighlightDimming(withChanges.nodes, withChanges.links, highlightedTypes, {
      compose: true,
    })
  }, [changesOnly, graphView, highlightedTypes])
  const clearTypeHighlight = useCallback(() => {
    setHighlightedTypes((prev) => (prev.size === 0 ? prev : new Set()))
  }, [])
  const toggleTypeHighlight = useCallback((type: string) => {
    setHighlightedTypes((prev) => toggleTypeInSet(prev, type))
  }, [])
  const [addOpen, setAddOpen] = useState(false)
  const [linkOpen, setLinkOpen] = useState(false)
  const [connectOpen, setConnectOpen] = useState(false)
  const [addType, setAddType] = useState<string | null>(null)
  const [addVersion, setAddVersion] = useState<string | null>(null)
  const [linkOptions, setLinkOptions] = useState<LinkOption[]>([])
  const [linkOptionKey, setLinkOptionKey] = useState<string | null>(null)
  const [linkVersion, setLinkVersion] = useState<string | null>(null)
  const [annotationBuffer, setAnnotationBuffer] = useState<Record<string, string> | null>(null)
  const [pairIds, setPairIds] = useState<string[]>([])
  const [canvasMenu, setCanvasMenu] = useState<{
    x: number
    y: number
    pairCount: number
    hasEdge: boolean
    entityDeleted: boolean
  } | null>(null)
  const [connectOptions, setConnectOptions] = useState<ConnectOption[]>([])
  const [connectOptionKey, setConnectOptionKey] = useState<string | null>(null)
  const [connectBusy, setConnectBusy] = useState(false)
  const [editSchema, setEditSchema] = useState<BoMSchema | null>(null)
  const [edgePropSchema, setEdgePropSchema] = useState<BoMSchema | null>(null)
  const [migrateConfirm, setMigrateConfirm] = useState<{
    type: string
    version: string
    schema: BoMSchema
    payload: Record<string, unknown>
    copied: number
    dropped: number
  } | null>(null)
  const [schemaMigrateBusy, setSchemaMigrateBusy] = useState(false)

  const entitySchemaOptions = useMemo(() => latestEntitySchemas(schemas), [schemas])

  useEffect(() => {
    const ids = new Set(document.entities.map((e) => e.id))
    setPairIds((prev) => {
      const next = prev.filter((id) => ids.has(id))
      return next.length === prev.length ? prev : next
    })
  }, [document.entities])

  // Sync pair highlight when selection is restored from browser history.
  useEffect(() => {
    if (selection?.kind === 'node') {
      setPairIds((prev) => (prev.includes(selection.node.id) ? prev : [selection.node.id]))
      return
    }
    setPairIds((prev) => (prev.length === 0 ? prev : []))
  }, [selection])

  // Clear type highlight when selection changes (canvas, history, validation jump).
  const selectionKey =
    selection?.kind === 'node'
      ? `n:${selection.node.id}`
      : selection?.kind === 'edge'
        ? `e:${selection.edge.id}`
        : 'none'
  const prevSelectionKeyRef = useRef(selectionKey)
  useEffect(() => {
    if (prevSelectionKeyRef.current === selectionKey) return
    prevSelectionKeyRef.current = selectionKey
    clearTypeHighlight()
  }, [clearTypeHighlight, selectionKey])

  const pairIdsRef = useRef(pairIds)
  pairIdsRef.current = pairIds

  const handleSelect = useCallback(
    (sel: GraphSelection | null, meta?: { additive?: boolean }) => {
      clearTypeHighlight()
      if (!sel) {
        setPairIds([])
        onSelect(null)
        return
      }
      if (sel.kind === 'edge') {
        setPairIds([])
        onSelect(sel)
        return
      }
      const next = advancePairSelection(pairIdsRef.current, sel.node.id, meta?.additive === true)
      setPairIds(next)
      if (next.length === 0) {
        onSelect(null)
      } else if (next.length === 1) {
        const entity = document.entities.find((e) => e.id === next[0])
        onSelect(entity ? { kind: 'node', node: entityToGraphNode(entity) } : sel)
      } else {
        onSelect(sel)
      }
    },
    [clearTypeHighlight, document.entities, onSelect],
  )

  const selectEntityAlone = useCallback(
    (entity: BoMEntity) => {
      clearTypeHighlight()
      setPairIds([entity.id])
      onSelect({ kind: 'node', node: entityToGraphNode(entity) })
    },
    [clearTypeHighlight, onSelect],
  )

  const selectAndFocusEntity = useCallback(
    (entityId: string) => {
      const entity = document.entities.find((e) => e.id === entityId)
      if (!entity) return
      selectEntityAlone(entity)
      requestAnimationFrame(() => graphRef.current?.focusNode(entityId))
    },
    [document.entities, selectEntityAlone],
  )

  function endpointLabel(entityId: string): string {
    const entity = document.entities.find((e) => e.id === entityId)
    if (!entity) return entityId
    return `${nodeLabel(entity.payload, entity.id)} (${entity.type})`
  }

  useEffect(() => {
    let cancelled = false
    listSchemas('ENTITY')
      .then((list) => {
        if (!cancelled) setSchemas(list)
      })
      .catch((e) => {
        if (!cancelled) setSchemasError(e instanceof Error ? e.message : String(e))
      })
    return () => {
      cancelled = true
    }
  }, [])

  const selectedEntity = useMemo(() => {
    if (selection?.kind !== 'node') return null
    return document.entities.find((e) => e.id === selection.node.id) ?? null
  }, [document.entities, selection])

  const selectedEdge = useMemo(() => {
    if (selection?.kind !== 'edge') return null
    return document.edges.find((e) => e.id === selection.edge.id) ?? null
  }, [document.edges, selection])

  function deleteSelection() {
    if (selectedEdge?.id && pairIds.length === 0) {
      const keep =
        draftState.baselineEdgeIds.has(selectedEdge.id) &&
        !draftState.pendingDeleteEdgeIds.has(selectedEdge.id)
      onRemoveEdge(selectedEdge.id)
      if (!keep) handleSelect(null)
      return
    }
    if (pairIds.length === 0 && selectedEntity) {
      const keep =
        draftState.baselineEntityIds.has(selectedEntity.id) &&
        !draftState.pendingDeleteEntityIds.has(selectedEntity.id)
      onRemoveEntity(selectedEntity.id)
      if (!keep) handleSelect(null)
      return
    }
    if (pairIds.length > 0) {
      for (const id of [...pairIds]) {
        onRemoveEntity(id)
      }
      handleSelect(null)
    }
  }

  /** Drop from draft without Apply delete (no pending-delete chrome). */
  function excludeSelection() {
    if (selectedEdge?.id && pairIds.length === 0) {
      onExcludeEdge(selectedEdge.id)
      handleSelect(null)
      return
    }
    if (pairIds.length === 0 && selectedEntity) {
      onExcludeEntity(selectedEntity.id)
      handleSelect(null)
      return
    }
    if (pairIds.length > 0) {
      for (const id of [...pairIds]) {
        onExcludeEntity(id)
      }
      handleSelect(null)
    }
  }

  const canExcludeFromDraft =
    (pairIds.length > 0 &&
      pairIds.every((id) => !draftState.pendingDeleteEntityIds.has(id))) ||
    (selectedEdge?.id != null &&
      pairIds.length === 0 &&
      !draftState.pendingDeleteEdgeIds.has(selectedEdge.id))

  const selectedEntityId = selectedEntity?.id ?? null
  const selectedEntityType = selectedEntity?.type ?? null
  const selectedEntityVersion = selectedEntity?.schemaVersion ?? null
  const selectedEdgeId = selectedEdge?.id ?? null
  const selectedEdgeType = selectedEdge?.type ?? null
  const selectedEdgeVersion = selectedEdge?.schemaVersion ?? null
  const schemaMenuOptions = useMemo(
    () => entitySchemasSameType(schemas, selectedEntityType),
    [schemas, selectedEntityType],
  )
  /** Frozen identity paths from baseline (stored) schema+payload — unset/blank stay editable (G-15). */
  const lockedEntityIdentifierPaths = useMemo(() => {
    if (!selectedEntityId || !draftState.baselineEntityIds.has(selectedEntityId)) {
      return new Set<string>()
    }
    const baseline = draftState.baselineEntities.get(selectedEntityId)
    if (!baseline) return new Set<string>()
    const baselineSchema = schemas.find(
      (s) => s.type === baseline.type && s.version === (baseline.schemaVersion ?? '1.0.0'),
    )
    if (!baselineSchema) return new Set<string>()
    return projectIdentityPaths(
      baselineSchema.contentSchema,
      (baseline.payload ?? {}) as Record<string, unknown>,
    )
  }, [
    draftState.baselineEntities,
    draftState.baselineEntityIds,
    schemas,
    selectedEntityId,
  ])
  const lockedEdgeIdentifierPaths = useMemo(() => {
    if (!selectedEdgeId || !draftState.baselineEdgeIds.has(selectedEdgeId)) {
      return new Set<string>()
    }
    const baseline = draftState.baselineEdges.get(selectedEdgeId)
    if (!baseline?.type || !baseline.schemaVersion) return new Set<string>()
    const baselineSchema = schemas.find(
      (s) => s.type === baseline.type && s.version === baseline.schemaVersion,
    )
    if (!baselineSchema) return new Set<string>()
    return projectIdentityPaths(
      baselineSchema.contentSchema,
      (baseline.properties ?? {}) as Record<string, unknown>,
    )
  }, [
    draftState.baselineEdgeIds,
    draftState.baselineEdges,
    schemas,
    selectedEdgeId,
  ])

  useEffect(() => {
    let cancelled = false
    async function load() {
      if (!selectedEntityType || !selectedEntityVersion) {
        setEditSchema(null)
        return
      }
      try {
        const schema = await getSchema(selectedEntityType, selectedEntityVersion)
        if (!cancelled) setEditSchema(schema)
      } catch {
        if (!cancelled) setEditSchema(null)
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [selectedEntityId, selectedEntityType, selectedEntityVersion])

  useEffect(() => {
    let cancelled = false
    async function load() {
      if (!selectedEdgeType || !selectedEdgeVersion) {
        setEdgePropSchema(null)
        return
      }
      try {
        const schema = await getSchema(selectedEdgeType, selectedEdgeVersion)
        if (!cancelled) setEdgePropSchema(schema)
      } catch {
        if (!cancelled) setEdgePropSchema(null)
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [selectedEdgeId, selectedEdgeType, selectedEdgeVersion])

  const versionsForAddType = useMemo(
    () => versionsForEntityType(schemas, addType),
    [addType, schemas],
  )

  const linkCreateType = useMemo(() => {
    if (!linkOptionKey) return null
    const option = linkOptions.find((o) => o.key === linkOptionKey)
    if (!option || option.createType === '*') return null
    return option.createType
  }, [linkOptionKey, linkOptions])

  const versionsForLinkType = useMemo(
    () => versionsForEntityType(schemas, linkCreateType),
    [linkCreateType, schemas],
  )

  useEffect(() => {
    if (!addType) {
      setAddVersion(null)
      return
    }
    const latest = entitySchemaOptions.find((s) => s.type === addType)
    setAddVersion(latest?.version ?? versionsForAddType[0] ?? null)
  }, [addType, entitySchemaOptions, versionsForAddType])

  useEffect(() => {
    if (!linkCreateType) {
      setLinkVersion(null)
      return
    }
    const latest = entitySchemaOptions.find((s) => s.type === linkCreateType)
    setLinkVersion(latest?.version ?? versionsForLinkType[0] ?? null)
  }, [entitySchemaOptions, linkCreateType, versionsForLinkType])

  const annotationsMenuEnabled =
    pairIds.length === 1 &&
    !!selectedEntity &&
    !draftState.pendingDeleteEntityIds.has(selectedEntity.id)
  const annotationPasteEnabled = annotationsMenuEnabled && annotationBuffer != null

  function copySelectedAnnotations() {
    if (!selectedEntity) return
    setAnnotationBuffer({ ...(selectedEntity.annotations ?? {}) })
  }

  function pasteSelectedAnnotations() {
    if (!selectedEntity || annotationBuffer == null) return
    onUpsertEntity({ ...selectedEntity, annotations: { ...annotationBuffer } })
  }

  function pasteMergeSelectedAnnotations() {
    if (!selectedEntity || annotationBuffer == null) return
    onUpsertEntity({
      ...selectedEntity,
      annotations: mergeAnnotations(selectedEntity.annotations ?? {}, annotationBuffer),
    })
  }

  const closeCanvasMenu = useCallback(() => setCanvasMenu(null), [])

  const openCanvasMenuAt = useCallback(
    (
      event: { clientX: number; clientY: number; preventDefault: () => void },
      snapshot: { pairCount: number; hasEdge: boolean; entityDeleted: boolean },
    ) => {
      event.preventDefault()
      setCanvasMenu({
        x: event.clientX,
        y: event.clientY,
        ...snapshot,
      })
    },
    [],
  )

  const onCanvasNodeContextMenu = useCallback(
    (event: ReactMouseEvent, node: GraphNode) => {
      const entity = document.entities.find((e) => e.id === node.id)
      if (!entity) return
      const inPair = pairIds.includes(node.id)
      let pairCount: number
      if (!inPair) {
        selectEntityAlone(entity)
        pairCount = 1
      } else {
        pairCount = pairIds.length
      }
      openCanvasMenuAt(event, {
        pairCount,
        hasEdge: false,
        entityDeleted: draftState.pendingDeleteEntityIds.has(entity.id),
      })
    },
    [
      document.entities,
      draftState.pendingDeleteEntityIds,
      openCanvasMenuAt,
      pairIds,
      selectEntityAlone,
    ],
  )

  const onCanvasEdgeContextMenu = useCallback(
    (event: ReactMouseEvent, edge: GraphLink) => {
      clearTypeHighlight()
      setPairIds([])
      onSelect({ kind: 'edge', edge })
      openCanvasMenuAt(event, {
        pairCount: 0,
        hasEdge: true,
        entityDeleted: false,
      })
    },
    [clearTypeHighlight, onSelect, openCanvasMenuAt],
  )

  const onCanvasPaneContextMenu = useCallback(
    (event: ReactMouseEvent | MouseEvent) => {
      const entityDeleted =
        pairIds.length === 1 &&
        !!pairIds[0] &&
        draftState.pendingDeleteEntityIds.has(pairIds[0])
      openCanvasMenuAt(event, {
        pairCount: pairIds.length,
        hasEdge: selection?.kind === 'edge',
        entityDeleted,
      })
    },
    [draftState.pendingDeleteEntityIds, openCanvasMenuAt, pairIds, selection],
  )

  const canvasMenuCanCreateLinked =
    canvasMenu != null && canvasMenu.pairCount === 1 && !canvasMenu.entityDeleted
  const canvasMenuCanConnect =
    canvasMenu != null &&
    canvasMenu.pairCount === 2 &&
    !pairIds.some((id) => draftState.pendingDeleteEntityIds.has(id))
  const canvasMenuCanDelete =
    canvasMenu != null && (canvasMenu.pairCount > 0 || canvasMenu.hasEdge)
  const canvasMenuCanAnnotations = canvasMenuCanCreateLinked
  const canvasMenuCanPaste = canvasMenuCanAnnotations && annotationBuffer != null

  const openLink = useCallback(async () => {
    if (!selectedEntity) return
    if (draftState.pendingDeleteEntityIds.has(selectedEntity.id)) return
    setLinkOpen(true)
    setLinkOptionKey(null)
    setLinkVersion(null)
    setLinkOptions([])
    try {
      const edges = await getTypeEdges(selectedEntity.type)
      setLinkOptions(
        buildLinkOptions(
          selectedEntity,
          edges.outgoing,
          edges.incoming,
          liveDocument.edges,
          liveDocument.entities,
        ),
      )
    } catch {
      setLinkOptions([])
    }
  }, [draftState.pendingDeleteEntityIds, liveDocument.edges, liveDocument.entities, selectedEntity])

  const openConnect = useCallback(async () => {
    if (pairIds.length !== 2) return
    if (pairIds.some((id) => draftState.pendingDeleteEntityIds.has(id))) return
    const a = liveDocument.entities.find((e) => e.id === pairIds[0])
    const b = liveDocument.entities.find((e) => e.id === pairIds[1])
    if (!a || !b) return
    setConnectOpen(true)
    setConnectOptionKey(null)
    setConnectOptions([])
    setConnectBusy(true)
    try {
      const [edgesA, edgesB] = await Promise.all([getTypeEdges(a.type), getTypeEdges(b.type)])
      setConnectOptions(
        buildConnectOptions(a, b, edgesA.outgoing, edgesB.outgoing, liveDocument.edges),
      )
    } catch {
      setConnectOptions([])
    } finally {
      setConnectBusy(false)
    }
  }, [draftState.pendingDeleteEntityIds, liveDocument.edges, liveDocument.entities, pairIds])

  useImperativeHandle(
    ref,
    () => ({
      focusNode: (nodeId: string) => {
        graphRef.current?.focusNode(nodeId)
      },
      openNew: () => setAddOpen(true),
      openNewLinked: () => {
        void openLink()
      },
      openLink: () => {
        void openConnect()
      },
    }),
    [openConnect, openLink],
  )

  const canNewLinked =
    pairIds.length === 1 &&
    !!selectedEntity &&
    !draftState.pendingDeleteEntityIds.has(selectedEntity.id)
  const canLink =
    pairIds.length === 2 &&
    !pairIds.some((id) => draftState.pendingDeleteEntityIds.has(id))
  const newLinkedTooltip = canNewLinked
    ? undefined
    : 'Select exactly one non-deleted object'
  const linkTooltip = canLink
    ? undefined
    : 'Select exactly two non-deleted objects (Ctrl+click)'

  async function confirmAdd() {
    if (!addType || !addVersion) return
    const schema = await getSchema(addType, addVersion)
    const payload = defaultValueForSchema(schema.contentSchema) as Record<string, unknown>
    const entity: BoMEntity = {
      id: newEntityId(),
      type: addType,
      schemaVersion: addVersion,
      payload,
      annotations: { ...graphAnnotations },
    }
    onUpsertEntity(entity)
    selectEntityAlone(entity)
    setAddOpen(false)
  }

  function applySchemaMigrate(
    entity: BoMEntity,
    schema: BoMSchema,
    payload: Record<string, unknown>,
  ) {
    onUpsertEntity({
      ...entity,
      type: schema.type,
      schemaVersion: schema.version,
      payload,
    })
    setEditSchema(schema)
    setMigrateConfirm(null)
  }

  async function pickEntitySchema(type: string, version: string) {
    if (!selectedEntity) return
    if (
      selectedEntity.type === type &&
      (selectedEntity.schemaVersion ?? '1.0.0') === version
    ) {
      return
    }
    setSchemaMigrateBusy(true)
    try {
      const schema = await getSchema(type, version)
      const source = (selectedEntity.payload ?? {}) as Record<string, unknown>
      const migrated = migratePayloadByKey(source, schema.contentSchema)
      if (migrateNeedsConfirm(migrated)) {
        setMigrateConfirm({
          type: schema.type,
          version: schema.version,
          schema,
          payload: migrated.payload,
          copied: migrated.copied,
          dropped: migrated.dropped,
        })
        return
      }
      applySchemaMigrate(selectedEntity, schema, migrated.payload)
    } finally {
      setSchemaMigrateBusy(false)
    }
  }

  async function confirmLinked() {
    if (!selectedEntity || !linkOptionKey || !linkVersion) return
    const option = linkOptions.find((o) => o.key === linkOptionKey)
    if (!option || option.createType === '*') return
    if (linkOptionOccupied(option, selectedEntity, liveDocument.edges, liveDocument.entities)) {
      return
    }
    const schema = await getSchema(option.createType, linkVersion)
    const createdId = newEntityId()
    const entity: BoMEntity = {
      id: createdId,
      type: option.createType,
      schemaVersion: linkVersion,
      payload: defaultValueForSchema(schema.contentSchema) as Record<string, unknown>,
      annotations: { ...(selectedEntity.annotations ?? {}) },
    }
    const edge: BoMEdge = {
      id: newEntityId(),
      source: option.direction === 'out' ? selectedEntity.id : createdId,
      target: option.direction === 'out' ? createdId : selectedEntity.id,
      role: option.rule.role,
      type: option.rule.propertiesSchemaType ?? undefined,
      schemaVersion: option.rule.propertiesSchemaVersion ?? undefined,
      properties: {},
    }
    onUpsertEntity(entity)
    onUpsertEdge(edge)
    selectEntityAlone(entity)
    setLinkOpen(false)
  }

  function confirmConnect() {
    if (!connectOptionKey) return
    const option = connectOptions.find((o) => o.key === connectOptionKey)
    if (!option) return
    const alreadyExists = liveDocument.edges.some(
      (e) =>
        e.source === option.sourceId &&
        e.target === option.targetId &&
        e.role === option.rule.role,
    )
    if (alreadyExists) return
    const target = liveDocument.entities.find((e) => e.id === option.targetId)
    onUpsertEdge({
      id: newEntityId(),
      source: option.sourceId,
      target: option.targetId,
      role: option.rule.role,
      type: option.rule.propertiesSchemaType ?? undefined,
      schemaVersion: option.rule.propertiesSchemaVersion ?? undefined,
      properties: {},
    })
    if (target) selectEntityAlone(target)
    setConnectOpen(false)
  }

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group justify="space-between" align="center" wrap="wrap" style={{ flexShrink: 0 }}>
        <Group gap="xs" wrap="wrap">
        <Group gap={0} wrap="nowrap" style={{ display: 'inline-flex' }}>
          <Button
            size="xs"
            onClick={() => setAddOpen(true)}
            style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
          >
            New
          </Button>
          <Menu shadow="md" width={180} position="bottom-end" withinPortal>
            <Menu.Target>
              <Button
                size="xs"
                aria-label="New options"
                px="xs"
                style={{
                  borderTopLeftRadius: 0,
                  borderBottomLeftRadius: 0,
                  borderLeft: '1px solid var(--mantine-color-default-border)',
                }}
              >
                ▾
              </Button>
            </Menu.Target>
            <Menu.Dropdown>
              <Menu.Item onClick={() => setAddOpen(true)}>New</Menu.Item>
              <Menu.Item
                disabled={!canNewLinked}
                title={newLinkedTooltip}
                onClick={() => void openLink()}
              >
                New linked
              </Menu.Item>
            </Menu.Dropdown>
          </Menu>
        </Group>
        <Tooltip label={linkTooltip} disabled={!linkTooltip} withArrow>
          <span style={{ display: 'inline-flex' }}>
            <Button
              size="xs"
              variant="light"
              disabled={!canLink}
              onClick={() => void openConnect()}
            >
              Link
            </Button>
          </span>
        </Tooltip>
        {onToggleAddObjects && (
          <Button size="xs" variant="light" onClick={onToggleAddObjects}>
            {addObjectsOpen ? 'Hide add objects' : 'Add objects…'}
          </Button>
        )}
        <Button
          size="xs"
          variant="light"
          disabled={!canExcludeFromDraft}
          onClick={excludeSelection}
        >
          Remove from draft
        </Button>
        <Button
          size="xs"
          variant="light"
          color="red"
          disabled={pairIds.length === 0 && !selectedEdge}
          onClick={deleteSelection}
        >
          Delete
        </Button>
        <Menu shadow="md" width={180} position="bottom-end" withinPortal>
          <Menu.Target>
            <Group gap={0} wrap="nowrap" style={{ display: 'inline-flex' }}>
              <Button
                size="xs"
                variant="light"
                disabled={!annotationsMenuEnabled}
                style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
              >
                Annotations
              </Button>
              <Button
                size="xs"
                variant="light"
                disabled={!annotationsMenuEnabled}
                aria-label="Annotation actions"
                px="xs"
                style={{
                  borderTopLeftRadius: 0,
                  borderBottomLeftRadius: 0,
                  borderLeft: '1px solid var(--mantine-color-default-border)',
                }}
              >
                ▾
              </Button>
            </Group>
          </Menu.Target>
          <Menu.Dropdown>
            <Menu.Item
              disabled={!annotationsMenuEnabled}
              onClick={copySelectedAnnotations}
            >
              Copy
            </Menu.Item>
            <Menu.Item
              disabled={!annotationPasteEnabled}
              onClick={pasteSelectedAnnotations}
            >
              Paste
            </Menu.Item>
            <Menu.Item
              disabled={!annotationPasteEnabled}
              onClick={pasteMergeSelectedAnnotations}
            >
              Paste Merge
            </Menu.Item>
          </Menu.Dropdown>
        </Menu>
        <Group gap={0}>
          <Button
            size="xs"
            variant="light"
            disabled={displayGraph.nodes.length === 0}
            onClick={() => graphRef.current?.applyLayout(layout)}
            style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
          >
            Apply layout
          </Button>
          <Menu position="bottom-end" withinPortal>
            <Menu.Target>
              <Button
                size="xs"
                variant="light"
                disabled={displayGraph.nodes.length === 0}
                aria-label="Choose graph layout"
                px="xs"
                style={{
                  borderTopLeftRadius: 0,
                  borderBottomLeftRadius: 0,
                  borderLeft: '1px solid var(--mantine-color-default-border)',
                }}
              >
                ▾
              </Button>
            </Menu.Target>
            <Menu.Dropdown>
              <Menu.Label>Layout direction</Menu.Label>
              {GRAPH_LAYOUTS.map((option) => (
                <Menu.Item
                  key={option.value}
                  onClick={() => {
                    setLayout(option.value)
                    if (option.value === layout) {
                      graphRef.current?.applyLayout(option.value)
                    } else {
                      requestAnimationFrame(() => graphRef.current?.applyLayout(option.value))
                    }
                  }}
                >
                  {option.value === layout ? '✓ ' : ''}
                  {option.label}
                </Menu.Item>
              ))}
            </Menu.Dropdown>
          </Menu>
        </Group>
        {pairIds.length === 2 && (
          <Text size="xs" c="dimmed">
            2 selected (Ctrl+click to adjust)
          </Text>
        )}
        </Group>
        <Group gap="xs" wrap="wrap" align="center" style={{ flexShrink: 0 }}>
          <Badge variant="light" size="sm">
            {document.entities.length} on canvas
          </Badge>
          {addObjectsStats != null && (
            <Badge variant="outline" size="sm">
              last search {addObjectsStats.nodes} nodes
            </Badge>
          )}
          <Switch
            size="xs"
            label="Changes only"
            checked={changesOnly}
            onChange={(e) => setChangesOnly(e.currentTarget.checked)}
          />
        </Group>
      </Group>

      {graphView.nodes.length > 0 && (
        <Group gap="xs" wrap="wrap">
          <Text size="xs" c="dimmed">
            {graphView.nodes.length} nodes / {graphView.links.length} edges
          </Text>
          {types.map(([type, color]) => {
            const active = highlightedTypes.has(type)
            const filtering = highlightedTypes.size > 0
            return (
              <Badge
                key={type}
                size="sm"
                variant={active ? 'filled' : 'outline'}
                color="gray"
                leftSection={
                  <span style={{ color: active ? '#fff' : color, lineHeight: 1 }}>●</span>
                }
                onClick={() => toggleTypeHighlight(type)}
                style={{
                  cursor: 'pointer',
                  background: active ? color : undefined,
                  borderColor: color,
                  color: active ? '#fff' : undefined,
                  opacity: filtering && !active ? 0.45 : 1,
                  userSelect: 'none',
                }}
              >
                {type}
              </Badge>
            )
          })}
          {highlightedTypes.size > 0 && (
            <Tooltip label="Clear type highlight" withArrow>
              <ActionIcon
                size="sm"
                variant="subtle"
                color="gray"
                aria-label="Clear type highlight"
                onClick={clearTypeHighlight}
              >
                <IconX size={14} />
              </ActionIcon>
            </Tooltip>
          )}
        </Group>
      )}

      {schemasError && (
        <Alert color="red" title="Cannot load schemas">
          {schemasError}
        </Alert>
      )}

      <Group
        ref={splitHostRef}
        align="stretch"
        gap={0}
        wrap="nowrap"
        style={{ flex: 1, minHeight: 0 }}
      >
        <Paper
          withBorder
          style={{ flex: 1, minWidth: 0, minHeight: 0, overflow: 'hidden', position: 'relative' }}
          onContextMenu={
            displayGraph.nodes.length === 0 ? onCanvasPaneContextMenu : undefined
          }
        >
          {canvasLoading && (
            <Stack
              align="center"
              justify="center"
              gap="sm"
              style={{
                position: 'absolute',
                inset: 0,
                zIndex: 5,
                background: 'color-mix(in srgb, var(--mantine-color-body) 82%, transparent)',
              }}
            >
              <Loader size="md" />
              <Text size="sm" c="dimmed">
                Loading graph…
              </Text>
            </Stack>
          )}
          {displayGraph.nodes.length === 0 && !canvasLoading ? (
            <Stack p="md" gap="xs">
              <Text size="sm" c="dimmed">
                Draft has no entities. Use New or Add objects….
              </Text>
            </Stack>
          ) : displayGraph.nodes.length === 0 ? null : (
            <GraphCanvas
              ref={graphRef}
              nodes={displayGraph.nodes}
              links={displayGraph.links}
              selection={selection}
              onSelect={handleSelect}
              layout={layout}
              autoLayoutOnDataChange={false}
              highlightedNodeIds={pairIds}
              onNodeContextMenu={onCanvasNodeContextMenu}
              onEdgeContextMenu={onCanvasEdgeContextMenu}
              onPaneContextMenu={onCanvasPaneContextMenu}
            />
          )}
        </Paper>

        <Box
          role="separator"
          aria-orientation="vertical"
          aria-label="Resize side pane"
          onPointerDown={onSplitterPointerDown}
          onPointerMove={onSplitterPointerMove}
          onPointerUp={onSplitterPointerUp}
          onPointerCancel={onSplitterPointerUp}
          style={{
            width: SPLITTER_WIDTH,
            flexShrink: 0,
            cursor: 'col-resize',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            touchAction: 'none',
            userSelect: 'none',
          }}
        >
          <Box
            style={{
              width: 3,
              height: 48,
              borderRadius: 2,
              background: 'var(--mantine-color-default-border)',
            }}
          />
        </Box>

        <Menu
          opened={canvasMenu != null}
          onChange={(opened) => {
            if (!opened) closeCanvasMenu()
          }}
          position="bottom-start"
          offset={4}
          withinPortal
          shadow="md"
          width={200}
        >
          <Menu.Target>
            <div
              style={{
                position: 'fixed',
                left: canvasMenu?.x ?? 0,
                top: canvasMenu?.y ?? 0,
                width: 0,
                height: 0,
                pointerEvents: 'none',
              }}
            />
          </Menu.Target>
          <Menu.Dropdown>
            <Menu.Item
              onClick={() => {
                closeCanvasMenu()
                setAddOpen(true)
              }}
            >
              New
            </Menu.Item>
            {canvasMenuCanCreateLinked && (
              <Menu.Item
                onClick={() => {
                  closeCanvasMenu()
                  void openLink()
                }}
              >
                New linked
              </Menu.Item>
            )}
            {canvasMenuCanConnect && (
              <Menu.Item
                onClick={() => {
                  closeCanvasMenu()
                  void openConnect()
                }}
              >
                Link
              </Menu.Item>
            )}
            {canExcludeFromDraft && (
              <Menu.Item
                onClick={() => {
                  closeCanvasMenu()
                  excludeSelection()
                }}
              >
                Remove from draft
              </Menu.Item>
            )}
            {canvasMenuCanDelete && (
              <Menu.Item
                color="red"
                onClick={() => {
                  closeCanvasMenu()
                  deleteSelection()
                }}
              >
                Delete
              </Menu.Item>
            )}
            {canvasMenuCanAnnotations && (
              <>
                <Menu.Divider />
                <Menu.Label>Annotations</Menu.Label>
                <Menu.Item
                  onClick={() => {
                    copySelectedAnnotations()
                    closeCanvasMenu()
                  }}
                >
                  Copy annotations
                </Menu.Item>
                <Menu.Item
                  disabled={!canvasMenuCanPaste}
                  onClick={() => {
                    pasteSelectedAnnotations()
                    closeCanvasMenu()
                  }}
                >
                  Paste annotations
                </Menu.Item>
                <Menu.Item
                  disabled={!canvasMenuCanPaste}
                  onClick={() => {
                    pasteMergeSelectedAnnotations()
                    closeCanvasMenu()
                  }}
                >
                  Paste merge annotations
                </Menu.Item>
              </>
            )}
            {displayGraph.nodes.length > 0 && (
              <>
                <Menu.Divider />
                <Menu.Item
                  onClick={() => {
                    closeCanvasMenu()
                    graphRef.current?.applyLayout(layout)
                  }}
                >
                  Apply layout
                </Menu.Item>
              </>
            )}
          </Menu.Dropdown>
        </Menu>

        <Paper
          withBorder
          p="sm"
          style={{
            width: sidePaneWidth,
            flexShrink: 0,
            minHeight: 0,
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
          }}
        >
          {addObjectsOpen && onCloseAddObjects && onMergeEntities && onMergeEdges ? (
            <AddObjectsPanel
              active={addObjectsOpen}
              graphId={graphId}
              onClose={onCloseAddObjects}
              matcher={addObjectsMatcher}
              autoSearch={addObjectsAutoSearch}
              autoAddAllResults={addObjectsAutoAddAll}
              draftEntityIds={resolvedDraftEntityIds}
              baselineEntityIds={draftState.baselineEntityIds}
              onMergeEntities={onMergeEntities}
              onExcludeEntity={onExcludeEntity}
              onMergeEdges={onMergeEdges}
              onSearchSuccess={onAddObjectsSearchSuccess}
            />
          ) : pairIds.length > 1 ? (
            <Stack gap="sm">
              <Group justify="space-between" align="flex-start">
                <Text fw={600}>Multiple selection</Text>
                <Button size="compact-xs" variant="subtle" onClick={() => handleSelect(null)}>
                  Clear
                </Button>
              </Group>
              <Text size="sm" c="dimmed">
                Editing is disabled while two objects are selected. Use Link or Delete in the
                toolbar, or click a single object to edit.
              </Text>
              <Stack gap={4}>
                {pairIds.map((id) => {
                  const entity = document.entities.find((e) => e.id === id)
                  return (
                    <Text key={id} size="sm">
                      <Code>{entity?.type ?? '?'}</Code> {id.slice(0, 8)}…
                    </Text>
                  )
                })}
              </Stack>
            </Stack>
          ) : !selectedEntity && !selectedEdge ? (
            <Stack gap="sm">
              <Text fw={600} size="sm">
                Graph annotations
              </Text>
              <Text size="xs" c="dimmed">
                No object or edge selected. Edit this graph&apos;s header annotations, or select a
                node/edge to edit.
              </Text>
              {onGraphAnnotationsChange ? (
                <AnnotationsEditor
                  value={graphAnnotations}
                  onChange={onGraphAnnotationsChange}
                  compact
                />
              ) : (
                <Text size="sm" c="dimmed">
                  Select a node or edge to edit. Ctrl+click a second node to link two objects.
                </Text>
              )}
            </Stack>
          ) : (
            <ScrollArea style={{ flex: 1, minHeight: 0 }} offsetScrollbars>
              {selectedEntity && (
                <Stack gap="xs">
                  <Group justify="space-between" align="flex-start" gap="xs">
                    <div>
                      <Group gap={6} align="center" wrap="wrap">
                        <Text fw={600} size="sm">
                          Edit {selectedEntity.type}
                        </Text>
                        <Menu shadow="md" width={260} position="bottom-start" withinPortal>
                          <Menu.Target>
                            <Button
                              size="compact-xs"
                              variant="light"
                              loading={schemaMigrateBusy}
                              disabled={schemaMenuOptions.length <= 1}
                              title={
                                schemaMenuOptions.length <= 1
                                  ? 'No other versions of this type'
                                  : 'Migrate to another schema version'
                              }
                            >
                              Schema ▾
                            </Button>
                          </Menu.Target>
                          <Menu.Dropdown>
                            <Menu.Label>Versions of {selectedEntity.type}</Menu.Label>
                            <ScrollArea.Autosize mah={280}>
                              {schemaMenuOptions.map((s) => {
                                const current =
                                  s.version === (selectedEntity.schemaVersion ?? '1.0.0')
                                return (
                                  <Menu.Item
                                    key={`${s.type}@${s.version}`}
                                    disabled={current}
                                    onClick={() => void pickEntitySchema(s.type, s.version)}
                                  >
                                    {s.version}
                                    {current ? ' (current)' : ''}
                                  </Menu.Item>
                                )
                              })}
                            </ScrollArea.Autosize>
                          </Menu.Dropdown>
                        </Menu>
                      </Group>
                      <Text size="xs" mt={4}>
                        <Text span fw={600}>
                          type:{' '}
                        </Text>
                        <Anchor
                          component={Link}
                          to={schemaDetailPath(
                            selectedEntity.type,
                            selectedEntity.schemaVersion ?? '1.0.0',
                          )}
                          target="_blank"
                          rel="noopener noreferrer"
                          size="xs"
                        >
                          <Badge
                            size="sm"
                            variant="light"
                            style={{
                              color: colorForType(selectedEntity.type),
                              cursor: 'pointer',
                            }}
                          >
                            {selectedEntity.type}
                          </Badge>
                        </Anchor>
                        <Text span c="dimmed" ml={6}>
                          schema {selectedEntity.schemaVersion ?? '—'}
                        </Text>
                      </Text>
                    </div>
                    <Button size="compact-xs" variant="subtle" onClick={() => handleSelect(null)}>
                      Close
                    </Button>
                  </Group>
                  <Group gap={6}>
                    <Text size="xs">
                      <Code>{selectedEntity.id}</Code>
                    </Text>
                    {entityStatus(draftState, selectedEntity.id) === 'new' && (
                      <Badge size="xs" color="teal" variant="filled">
                        new
                      </Badge>
                    )}
                    {entityStatus(draftState, selectedEntity.id) === 'modified' && (
                      <Badge size="xs" color="orange" variant="filled">
                        modified
                      </Badge>
                    )}
                    {entityStatus(draftState, selectedEntity.id) === 'deleted' && (
                      <Badge size="xs" color="red" variant="filled">
                        deleted
                      </Badge>
                    )}
                  </Group>
                  {entityStatus(draftState, selectedEntity.id) === 'deleted' ? (
                    <Stack gap="xs">
                      <Text size="sm" c="dimmed">
                        This loaded object is marked for delete. It stays on the canvas until Apply,
                        or you can undo the delete.
                      </Text>
                      <Button
                        size="xs"
                        variant="light"
                        onClick={() => onRestoreDeletedEntity(selectedEntity.id)}
                      >
                        Undo delete
                      </Button>
                    </Stack>
                  ) : (
                    <Stack gap="xs">
                      {entityStatus(draftState, selectedEntity.id) === 'modified' && (
                        <Button
                          size="xs"
                          variant="light"
                          onClick={() => onRevertEntityChanges(selectedEntity.id)}
                        >
                          Undo modifications
                        </Button>
                      )}
                      <Tabs defaultValue="payload" keepMounted={false} variant="outline">
                        <Tabs.List>
                          <Tabs.Tab value="payload">Payload</Tabs.Tab>
                          <Tabs.Tab value="annotations">Annotations</Tabs.Tab>
                        </Tabs.List>
                        <Tabs.Panel value="payload" pt="xs">
                          {editSchema ? (
                            <PayloadInspector
                              key={selectedEntity.id}
                              schema={editSchema.contentSchema}
                              value={(selectedEntity.payload ?? {}) as Record<string, unknown>}
                              onChange={(payload) =>
                                onUpsertEntity({ ...selectedEntity, payload })
                              }
                              hideChrome
                              allowFieldDelete
                              lockedIdentifierPaths={lockedEntityIdentifierPaths}
                            />
                          ) : (
                            <Text size="xs" c="dimmed">
                              Schema not available for form editing.
                            </Text>
                          )}
                        </Tabs.Panel>
                        <Tabs.Panel value="annotations" pt="xs">
                          <AnnotationsEditor
                            value={selectedEntity.annotations ?? {}}
                            onChange={(annotations) =>
                              onUpsertEntity({ ...selectedEntity, annotations })
                            }
                            compact
                            hideChrome
                          />
                        </Tabs.Panel>
                      </Tabs>
                    </Stack>
                  )}
                </Stack>
              )}
              {selectedEdge && !selectedEntity && (
                <Stack gap="xs">
                  <Group justify="space-between" align="flex-start" gap="xs">
                    <Text fw={600} size="sm">
                      Edge {selectedEdge.role}
                    </Text>
                    <Button size="compact-xs" variant="subtle" onClick={() => handleSelect(null)}>
                      Close
                    </Button>
                  </Group>
                  {selectedEdge.type && (
                    <Text size="xs">
                      <Text span fw={600}>
                        type:{' '}
                      </Text>
                      <Anchor
                        component={Link}
                        to={schemaDetailPath(
                          selectedEdge.type,
                          selectedEdge.schemaVersion ?? '1.0.0',
                        )}
                        target="_blank"
                        rel="noopener noreferrer"
                        size="xs"
                      >
                        <Badge size="sm" variant="light" style={{ cursor: 'pointer' }}>
                          {selectedEdge.type}
                        </Badge>
                      </Anchor>
                      {selectedEdge.schemaVersion && (
                        <Text span c="dimmed" ml={6}>
                          schema {selectedEdge.schemaVersion}
                        </Text>
                      )}
                    </Text>
                  )}
                  <Group gap={6}>
                    <Text size="xs">
                      <Code>{selectedEdge.id}</Code>
                    </Text>
                    {selectedEdge.id && edgeStatus(draftState, selectedEdge.id) === 'new' && (
                      <Badge size="xs" color="teal" variant="filled">
                        new
                      </Badge>
                    )}
                    {selectedEdge.id && edgeStatus(draftState, selectedEdge.id) === 'modified' && (
                      <Badge size="xs" color="orange" variant="filled">
                        modified
                      </Badge>
                    )}
                    {selectedEdge.id && edgeStatus(draftState, selectedEdge.id) === 'deleted' && (
                      <Badge size="xs" color="red" variant="filled">
                        deleted
                      </Badge>
                    )}
                  </Group>
                  <Text size="xs" c="dimmed">
                    endpoints
                  </Text>
                  <Stack gap={2}>
                    <Group gap={6} wrap="nowrap" align="flex-start">
                      <Text size="xs" fw={600} style={{ flexShrink: 0 }}>
                        source:
                      </Text>
                      <Anchor
                        component="button"
                        type="button"
                        size="xs"
                        ta="left"
                        style={{ wordBreak: 'break-all' }}
                        onClick={() => selectAndFocusEntity(selectedEdge.source)}
                      >
                        {endpointLabel(selectedEdge.source)}
                      </Anchor>
                    </Group>
                    <Group gap={6} wrap="nowrap" align="flex-start">
                      <Text size="xs" fw={600} style={{ flexShrink: 0 }}>
                        target:
                      </Text>
                      <Anchor
                        component="button"
                        type="button"
                        size="xs"
                        ta="left"
                        style={{ wordBreak: 'break-all' }}
                        onClick={() => selectAndFocusEntity(selectedEdge.target)}
                      >
                        {endpointLabel(selectedEdge.target)}
                      </Anchor>
                    </Group>
                  </Stack>
                  <Text size="xs" c="dimmed">
                    role: {selectedEdge.role}
                  </Text>
                  {selectedEdge.id && edgeStatus(draftState, selectedEdge.id) === 'deleted' ? (
                    <Button
                      size="xs"
                      variant="light"
                      onClick={() => onRestoreDeletedEdge(selectedEdge.id!)}
                    >
                      Undo delete
                    </Button>
                  ) : (
                    <>
                      {selectedEdge.id &&
                        edgeStatus(draftState, selectedEdge.id) === 'modified' && (
                          <Button
                            size="xs"
                            variant="light"
                            onClick={() => onRevertEdgeChanges(selectedEdge.id!)}
                          >
                            Undo modifications
                          </Button>
                        )}
                      {edgePropSchema ? (
                        <SchemaInstanceForm
                          schema={edgePropSchema.contentSchema}
                          value={(selectedEdge.properties ?? {}) as Record<string, unknown>}
                          onChange={(properties) =>
                            onUpsertEdge({ ...selectedEdge, properties })
                          }
                          compact
                          lockedIdentifierPaths={lockedEdgeIdentifierPaths}
                        />
                      ) : (
                        <Text size="xs" c="dimmed">
                          No property schema for this edge (NONE policy or schema missing).
                        </Text>
                      )}
                    </>
                  )}
                </Stack>
              )}
            </ScrollArea>
          )}
        </Paper>
      </Group>

      <Modal opened={addOpen} onClose={() => setAddOpen(false)} title="New object">
        <Stack>
          <Select
            label="Type"
            searchable
            data={entitySchemaOptions.map((s) => s.type)}
            value={addType}
            onChange={setAddType}
          />
          <Select
            label="Schema version"
            data={versionsForAddType}
            value={addVersion}
            onChange={setAddVersion}
            disabled={!addType}
          />
          <Button disabled={!addType || !addVersion} onClick={() => void confirmAdd()}>
            Create
          </Button>
        </Stack>
      </Modal>

      <Modal
        opened={linkOpen}
        onClose={() => {
          setLinkOpen(false)
          setLinkOptionKey(null)
          setLinkVersion(null)
        }}
        title="New linked object"
      >
        <Stack>
          {linkOptions.length === 0 ? (
            <Alert color="orange" title="No available relations">
              No allow-listed incoming/outgoing relations left for this object (or all matching
              relations already exist).
            </Alert>
          ) : (
            <>
              <Select
                label="Allowed relation"
                data={linkOptions.map((o) => ({
                  value: o.key,
                  label: `${o.label} (${o.rule.cardinality ?? 'UNSPECIFIED'})`,
                }))}
                value={linkOptionKey}
                onChange={setLinkOptionKey}
                searchable
              />
              <Select
                label="Schema version"
                data={versionsForLinkType}
                value={linkVersion}
                onChange={setLinkVersion}
                disabled={!linkCreateType}
              />
              <Text size="xs" c="dimmed">
                Annotations are copied from the selected object (including when empty).
              </Text>
              <Button
                disabled={!linkOptionKey || !linkVersion}
                onClick={() => void confirmLinked()}
              >
                New linked
              </Button>
            </>
          )}
        </Stack>
      </Modal>

      <Modal opened={connectOpen} onClose={() => setConnectOpen(false)} title="Link objects">
        <Stack>
          {connectBusy ? (
            <Text size="sm" c="dimmed">
              Loading allowed edges…
            </Text>
          ) : connectOptions.length === 0 ? (
            <Alert color="orange" title="Connection not possible">
              No defined edges between the selected objects.
            </Alert>
          ) : (
            <>
              <Select
                label="Allowed relation"
                data={connectOptions.map((o) => ({
                  value: o.key,
                  label: `${o.label} (${o.rule.cardinality ?? 'UNSPECIFIED'})`,
                }))}
                value={connectOptionKey}
                onChange={setConnectOptionKey}
                searchable
              />
              <Button disabled={!connectOptionKey} onClick={confirmConnect}>
                Link
              </Button>
            </>
          )}
        </Stack>
      </Modal>
      <Modal
        opened={migrateConfirm != null}
        onClose={() => setMigrateConfirm(null)}
        title="Migrate payload?"
      >
        {migrateConfirm && (
          <Stack gap="sm">
            <Text size="sm">
              Change schema to{' '}
              <Code>
                {migrateConfirm.type}@{migrateConfirm.version}
              </Code>
              .
            </Text>
            <Text size="sm" c="dimmed">
              {migrateConfirm.copied === 0
                ? 'No fields could be copied to the new schema (unmatched keys will be dropped).'
                : `${migrateConfirm.copied} field(s) copied, ${migrateConfirm.dropped} dropped.`}
            </Text>
            <Group justify="flex-end" gap="xs">
              <Button variant="default" onClick={() => setMigrateConfirm(null)}>
                Cancel
              </Button>
              <Button
                onClick={() => {
                  if (!selectedEntity || !migrateConfirm) return
                  applySchemaMigrate(
                    selectedEntity,
                    migrateConfirm.schema,
                    migrateConfirm.payload,
                  )
                }}
              >
                Confirm
              </Button>
            </Group>
          </Stack>
        )}
      </Modal>
    </Stack>
  )
  },
)

function ruleKey(rule: BoMAllowedEdgeRule): string {
  return `${rule.sourceType}|${rule.role}|${rule.targetType}`
}

function ruleMatchesEndpoint(ruleType: string, entityType: string): boolean {
  return ruleType === '*' || ruleType === entityType
}

type LinkOption = {
  key: string
  direction: 'out' | 'in'
  rule: BoMAllowedEdgeRule
  /** Entity type to create (other end of the relation). */
  createType: string
  label: string
}

function edgeExists(
  edges: BoMEdge[],
  sourceId: string,
  targetId: string,
  role: string,
): boolean {
  return edges.some((e) => e.source === sourceId && e.target === targetId && e.role === role)
}

function entityTypeById(entities: BoMEntity[], id: string): string | undefined {
  return entities.find((e) => e.id === id)?.type
}

/**
 * Hide Create-linked options when a 1:1 slot from/to the selected entity is already taken
 * for this rule's peer type. Exact (source, target, role) uniqueness does not apply here
 * because create-linked always allocates a new peer id; Connect existing handles that case.
 */
function linkOptionOccupied(
  option: LinkOption,
  selected: BoMEntity,
  existingEdges: BoMEdge[],
  entities: BoMEntity[],
): boolean {
  if (option.rule.cardinality !== '1:1') return false
  if (option.direction === 'out') {
    return existingEdges.some(
      (e) =>
        e.source === selected.id &&
        e.role === option.rule.role &&
        ruleMatchesEndpoint(option.rule.targetType, entityTypeById(entities, e.target) ?? ''),
    )
  }
  return existingEdges.some(
    (e) =>
      e.target === selected.id &&
      e.role === option.rule.role &&
      ruleMatchesEndpoint(option.rule.sourceType, entityTypeById(entities, e.source) ?? ''),
  )
}

function buildLinkOptions(
  selected: BoMEntity,
  outgoing: BoMAllowedEdgeRule[],
  incoming: BoMAllowedEdgeRule[],
  existingEdges: BoMEdge[],
  entities: BoMEntity[],
): LinkOption[] {
  const options: LinkOption[] = []
  const seen = new Set<string>()

  for (const rule of outgoing) {
    if (!ruleMatchesEndpoint(rule.sourceType, selected.type)) continue
    if (rule.targetType === '*') continue
    const key = `out|${ruleKey(rule)}`
    if (seen.has(key)) continue
    seen.add(key)
    options.push({
      key,
      direction: 'out',
      rule,
      createType: rule.targetType,
      label: `${selected.type} —${rule.role}→ ${rule.targetType}`,
    })
  }

  for (const rule of incoming) {
    if (!ruleMatchesEndpoint(rule.targetType, selected.type)) continue
    if (rule.sourceType === '*') continue
    const key = `in|${ruleKey(rule)}`
    if (seen.has(key)) continue
    seen.add(key)
    options.push({
      key,
      direction: 'in',
      rule,
      createType: rule.sourceType,
      label: `${rule.sourceType} —${rule.role}→ ${selected.type}`,
    })
  }

  return options.filter((opt) => !linkOptionOccupied(opt, selected, existingEdges, entities))
}

type ConnectOption = {
  key: string
  rule: BoMAllowedEdgeRule
  sourceId: string
  targetId: string
  label: string
}

function buildConnectOptions(
  a: BoMEntity,
  b: BoMEntity,
  outgoingA: BoMAllowedEdgeRule[],
  outgoingB: BoMAllowedEdgeRule[],
  existingEdges: BoMEdge[],
): ConnectOption[] {
  const options: ConnectOption[] = []
  for (const rule of outgoingA) {
    if (!ruleMatchesEndpoint(rule.sourceType, a.type)) continue
    if (!ruleMatchesEndpoint(rule.targetType, b.type)) continue
    options.push({
      key: `ab|${ruleKey(rule)}`,
      rule,
      sourceId: a.id,
      targetId: b.id,
      label: `${a.type} —${rule.role}→ ${b.type}`,
    })
  }
  for (const rule of outgoingB) {
    if (!ruleMatchesEndpoint(rule.sourceType, b.type)) continue
    if (!ruleMatchesEndpoint(rule.targetType, a.type)) continue
    const key = `ba|${ruleKey(rule)}`
    if (options.some((o) => o.key === key)) continue
    options.push({
      key,
      rule,
      sourceId: b.id,
      targetId: a.id,
      label: `${b.type} —${rule.role}→ ${a.type}`,
    })
  }
  return options.filter(
    (opt) => !edgeExists(existingEdges, opt.sourceId, opt.targetId, opt.rule.role),
  )
}

function advancePairSelection(prev: string[], id: string, additive: boolean): string[] {
  if (!additive) return [id]
  if (prev.includes(id)) return prev.filter((x) => x !== id)
  if (prev.length === 0) return [id]
  if (prev.length === 1) return [prev[0], id]
  return [prev[1], id]
}
