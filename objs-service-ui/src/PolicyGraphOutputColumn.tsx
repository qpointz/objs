import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
} from 'react'
import { Alert, Badge, Box, Paper, Stack, Tabs, Text } from '@mantine/core'
import { getGraph, getGraphVersion, listSchemas, queryAddObjects, toGraphData } from './api'
import { ColumnFilterHeader } from './ColumnFilterHeader'
import { GraphCanvas, type GraphCanvasHandle, type GraphLayout } from './GraphCanvas'
import { applyGraphCanvasFilters, toggleTypeInSet, uniqueSortedRoles } from './graphFilterDimming'
import { GraphFilterToolbar } from './GraphFilterToolbar'
import { useGraphContext } from './GraphContextProvider'
import {
  GraphGoToContextMenu,
  buildGraphNeighborIndex,
  type GraphGoToTarget,
} from './graphGoToNav'
import { EXPLORER_NODE_CAP } from './graphContextVersions'
import { policyFragmentMatcher } from './queryGraphContext'
import { structuredEdgeRows, structuredVertexRows } from './queryStructuredModel'
import { QueryResultGrid } from './QueryResultGrid'
import {
  IdLink,
  QUERY_STRUCT_EDGE_ROLE_COL_WIDTH,
  QUERY_STRUCT_EDGE_SOURCE_COL_WIDTH,
  QUERY_STRUCT_ID_COL_WIDTH,
  QUERY_STRUCT_TYPE_COL_WIDTH,
} from './QueryStructColumns'
import { formatObjectCell, scalarPayloadColumns } from './ObjectResultsTable'
import { objectDisplayTitle } from './objectViewerTitle'
import { maxSeverity, severityRank, type PolicyOutcome } from './policyTypes'
import type {
  BoMEdge,
  BoMEntity,
  BoMGraphContents,
  BoMSchema,
  GraphLink,
  GraphNode,
  GraphSelection,
} from './types'
import { clamp } from './sidePaneSplit'

export type PolicyGraphOutputColumnHandle = {
  applyLayout: (layout?: GraphLayout) => void
  changeLayout: (layout: GraphLayout) => void
  focusNode: (nodeId: string) => void
  canvasNonEmpty: boolean
  graphViewTab: string | null
  layout: GraphLayout
}

export type PolicyGraphModel = {
  fragmentContents: BoMGraphContents | null
  nodes: GraphNode[]
  links: GraphLink[]
  annotatedNodes: GraphNode[]
  annotatedLinks: GraphLink[]
}

const DATA_SEVERITY_NONE = { value: 'NONE', label: 'None' } as const

function severityBadgeColor(raw: string | null | undefined): string {
  switch ((raw ?? '').trim().toUpperCase()) {
    case 'CRITICAL':
    case 'HIGH':
    case 'EXEC_ERROR':
    case 'FAIL':
      return 'red'
    case 'MEDIUM':
      return 'orange'
    case 'LOW':
      return 'yellow'
    case 'PASS':
      return 'green'
    case 'INFO':
      return 'cyan'
    default:
      return 'gray'
  }
}

function passesTypeFilter(type: string | null | undefined, typeFilter: Set<string>): boolean {
  if (typeFilter.size === 0) return true
  if (type == null || type === '') return false
  return typeFilter.has(type)
}

function passesSeverityFilter(
  findingSeverity: string | undefined,
  severityFilter: Set<string>,
): boolean {
  if (severityFilter.size === 0) return true
  if (findingSeverity == null || findingSeverity === '') {
    return severityFilter.has('NONE')
  }
  return severityFilter.has(findingSeverity.toUpperCase())
}

function entityToGraphNode(entity: BoMEntity): GraphNode {
  const name =
    entity.payload != null && typeof entity.payload.name === 'string'
      ? entity.payload.name
      : null
  return {
    id: entity.id,
    name: objectDisplayTitle(name, entity.type, entity.id),
    type: entity.type,
    schemaVersion: entity.schemaVersion ?? '?',
    color: '#868e96',
    payload: entity.payload ?? {},
    annotations: entity.annotations ?? {},
    headVersion: entity.headVersion ?? null,
  }
}

function edgeToGraphLink(edge: BoMEdge, index: number): GraphLink {
  return {
    id: edge.id ?? `e-${edge.source}-${edge.target}-${edge.role}-${index}`,
    source: edge.source,
    target: edge.target,
    role: edge.role,
    type: edge.type ?? null,
    schemaVersion: edge.schemaVersion ?? null,
    properties: edge.properties ?? {},
    headVersion: edge.headVersion ?? null,
  }
}

const SPLITTER = 6
const OUTPUT_HEIGHT_KEY = 'objs.ui.policy.outputHeight'
const MIN_OUTPUT_ABS = 120

function loadNum(key: string, fallback: number): number {
  try {
    const raw = window.localStorage.getItem(key)
    if (raw == null) return fallback
    const n = Number(raw)
    return Number.isFinite(n) ? n : fallback
  } catch {
    return fallback
  }
}

function saveNum(key: string, value: number) {
  try {
    window.localStorage.setItem(key, String(value))
  } catch {
    /* ignore */
  }
}

function tasksPaneMin(hostH: number): number {
  return Math.max(MIN_OUTPUT_ABS, Math.floor(hostH / 8))
}

type PolicyGraphOutputColumnProps = {
  output: ReactNode
  layout?: GraphLayout
  onLayoutReady?: (api: {
    applyLayout: (l?: GraphLayout) => void
    changeLayout: (layout: GraphLayout) => void
    canvasNonEmpty: boolean
    graphViewTab: string | null
    layout: GraphLayout
  }) => void
  selection?: GraphSelection | null
  onSelectionChange?: (selection: GraphSelection | null) => void
  outcomes?: PolicyOutcome[]
  severityFilter?: Set<string>
  onSeverityFilterChange?: (next: Set<string>) => void
  onSeveritiesPresent?: (sevs: string[]) => void
  onGraphModel?: (model: PolicyGraphModel) => void
  /**
   * When set, Visual/Data use this fragment instead of the shared graph context
   * (Archives Input — does not mutate context).
   */
  frozenFragment?: BoMGraphContents | null
  /** Hide the bottom output pane (Archives Input raw/detail can own the chrome). */
  hideOutput?: boolean
}

/** Shared Visual | Data + Output column for Policy Policies/Suites. */
export const PolicyGraphOutputColumn = forwardRef<
  PolicyGraphOutputColumnHandle,
  PolicyGraphOutputColumnProps
>(function PolicyGraphOutputColumn(
  {
    output,
    layout = 'TB',
    onLayoutReady,
    selection: controlledSelection,
    onSelectionChange,
    outcomes,
    severityFilter: controlledSeverityFilter,
    onSeverityFilterChange,
    onSeveritiesPresent,
    onGraphModel,
    frozenFragment = null,
    hideOutput = false,
  },
  ref,
) {
  const { context } = useGraphContext()
  const canvasRef = useRef<GraphCanvasHandle>(null)
  const columnRef = useRef<HTMLDivElement>(null)
  const [schemas, setSchemas] = useState<BoMSchema[]>([])
  const [nodes, setNodes] = useState<GraphNode[]>([])
  const [links, setLinks] = useState<GraphLink[]>([])
  const [fragmentContents, setFragmentContents] = useState<BoMGraphContents | null>(null)
  const [fragmentNodeCount, setFragmentNodeCount] = useState(0)
  const [uncontrolledSelection, setUncontrolledSelection] = useState<GraphSelection | null>(null)
  const selection = onSelectionChange ? (controlledSelection ?? null) : uncontrolledSelection
  // Keep setSelection identity stable so parents' inline onSelectionChange do not
  // retrigger effects (e.g. canvas reload) and clear the selection on mouse-up.
  const onSelectionChangeRef = useRef(onSelectionChange)
  onSelectionChangeRef.current = onSelectionChange
  const setSelection = useCallback((next: GraphSelection | null) => {
    const notify = onSelectionChangeRef.current
    if (notify) notify(next)
    else setUncontrolledSelection(next)
  }, [])
  const [graphViewTab, setGraphViewTab] = useState<string | null>('visual')
  const [structVeTab, setStructVeTab] = useState<string | null>('vertices')
  const [graphLayout, setGraphLayout] = useState<GraphLayout>(layout)
  const [error, setError] = useState<string | null>(null)
  const [goToMenu, setGoToMenu] = useState<{ x: number; y: number; target: GraphGoToTarget } | null>(
    null,
  )
  const [internalSeverityFilter, setInternalSeverityFilter] = useState<Set<string>>(() => new Set())
  const [highlightedTypes, setHighlightedTypes] = useState<Set<string>>(() => new Set())
  const [highlightedEdgeRoles, setHighlightedEdgeRoles] = useState<Set<string>>(() => new Set())
  /** Data → Edges column funnels (Type = edge schema type, not entity type). */
  const [dataEdgeTypes, setDataEdgeTypes] = useState<Set<string>>(() => new Set())
  const [dataEdgeSourceTypes, setDataEdgeSourceTypes] = useState<Set<string>>(() => new Set())
  const [dataEdgeTargetTypes, setDataEdgeTargetTypes] = useState<Set<string>>(() => new Set())
  const severityControlled = onSeverityFilterChange != null
  const severityFilter = severityControlled
    ? (controlledSeverityFilter ?? new Set<string>())
    : internalSeverityFilter
  const setSeverityFilter = useCallback(
    (next: Set<string>) => {
      if (onSeverityFilterChange) onSeverityFilterChange(next)
      else setInternalSeverityFilter(next)
    },
    [onSeverityFilterChange],
  )
  const [outputHeight, setOutputHeight] = useState(() =>
    Math.max(MIN_OUTPUT_ABS, loadNum(OUTPUT_HEIGHT_KEY, 180)),
  )

  useEffect(() => {
    setGraphLayout(layout)
  }, [layout])

  useEffect(() => {
    void listSchemas().then(setSchemas).catch(() => setSchemas([]))
  }, [])

  const loadCanvas = useCallback(async () => {
    if (frozenFragment != null) {
      const contents = frozenFragment
      const entityCount = contents.entities?.length ?? 0
      setFragmentContents(contents)
      setFragmentNodeCount(entityCount)
      if (entityCount > EXPLORER_NODE_CAP) {
        setNodes([])
        setLinks([])
        setGraphViewTab('data')
        return
      }
      const data = toGraphData(contents, schemas)
      setNodes(data.nodes)
      setLinks(data.links)
      return
    }
    if (context.kind === 'empty') {
      setNodes([])
      setLinks([])
      setFragmentContents(null)
      setFragmentNodeCount(0)
      return
    }
    try {
      let contents: BoMGraphContents
      if (context.kind === 'graph' && context.graphId) {
        const res =
          context.graphVersion != null
            ? await getGraphVersion(context.graphId, context.graphVersion)
            : await getGraph(context.graphId)
        contents = res.graph
      } else if (context.kind === 'matcher' && context.matcherBody != null) {
        const { matcher } = policyFragmentMatcher(context)
        contents = await queryAddObjects(matcher, null)
      } else {
        setNodes([])
        setLinks([])
        setFragmentContents(null)
        setFragmentNodeCount(0)
        return
      }
      const entityCount = contents.entities?.length ?? 0
      setFragmentContents(contents)
      setFragmentNodeCount(entityCount)
      if (entityCount > EXPLORER_NODE_CAP) {
        setNodes([])
        setLinks([])
        setGraphViewTab('data')
        return
      }
      const data = toGraphData(contents, schemas)
      setNodes(data.nodes)
      setLinks(data.links)
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : String(ex))
      setNodes([])
      setLinks([])
      setFragmentContents(null)
      setFragmentNodeCount(0)
    }
  }, [context, frozenFragment, schemas])

  useEffect(() => {
    void loadCanvas()
  }, [loadCanvas])

  // Clear selection only when the shared graph context actually changes — not when
  // parent re-renders with a new onSelectionChange callback identity.
  // Frozen fragment: clear when the frozen pack identity changes.
  const contextKey =
    frozenFragment != null
      ? `frozen:${frozenFragment.entities?.length ?? 0}:${frozenFragment.edges?.length ?? 0}:${
          frozenFragment.entities?.[0]?.id ?? ''
        }`
      : context.kind === 'graph'
        ? `graph:${context.graphId}:${context.graphVersion ?? 'head'}`
        : context.kind === 'matcher'
          ? `matcher:${JSON.stringify(context.matcherBody)}`
          : 'empty'

  useEffect(() => {
    setSelection(null)
    // eslint-disable-next-line react-hooks/exhaustive-deps -- only when contextKey changes
  }, [contextKey])

  const findingSeverityMaps = useMemo(() => {
    const sevByEntity = new Map<string, string>()
    const sevByEdge = new Map<string, string>()
    ;(outcomes ?? []).forEach((o) => {
      ;(o.findings ?? []).forEach((f) => {
        const sev = f.severity?.toUpperCase()
        if (!sev) return
        ;(f.entities ?? []).forEach((id) => {
          const next = maxSeverity(sevByEntity.get(id), sev) ?? sev
          sevByEntity.set(id, next)
        })
        ;(f.edges ?? []).forEach((id) => {
          const next = maxSeverity(sevByEdge.get(id), sev) ?? sev
          sevByEdge.set(id, next)
        })
      })
    })
    return { sevByEntity, sevByEdge }
  }, [outcomes])

  const severitiesPresent = useMemo(() => {
    const s = new Set<string>()
    ;(outcomes ?? []).forEach((o) => {
      ;(o.findings ?? []).forEach((f) => {
        if (f.severity) s.add(f.severity.toUpperCase())
      })
    })
    return [...s].sort((a, b) => severityRank(b) - severityRank(a))
  }, [outcomes])

  useEffect(() => {
    onSeveritiesPresent?.(severitiesPresent)
  }, [onSeveritiesPresent, severitiesPresent])

  const annotatedGraph = useMemo(() => {
    const { sevByEntity, sevByEdge } = findingSeverityMaps
    const filtering = severityFilter.size > 0
    const nodesOut = nodes.map((n) => {
      const findingSeverity = sevByEntity.get(n.id)
      const dimmed = filtering && !passesSeverityFilter(findingSeverity, severityFilter)
      return { ...n, findingSeverity, dimmed }
    })
    const linksOut = links.map((l) => {
      const findingSeverity = sevByEdge.get(l.id)
      const dimmed = filtering && !passesSeverityFilter(findingSeverity, severityFilter)
      return { ...l, findingSeverity, dimmed }
    })
    return applyGraphCanvasFilters(
      nodesOut,
      linksOut,
      { types: highlightedTypes, edgeRoles: highlightedEdgeRoles },
      { compose: true },
    )
  }, [
    findingSeverityMaps,
    highlightedEdgeRoles,
    highlightedTypes,
    links,
    nodes,
    severityFilter,
  ])

  const typeFilterOptions = useMemo(() => {
    const set = new Map<string, string>()
    for (const n of nodes) {
      if (!set.has(n.type)) set.set(n.type, n.color)
    }
    // Data tab still has entities when Visual is over the node cap (nodes cleared).
    for (const e of fragmentContents?.entities ?? []) {
      if (e.type && !set.has(e.type)) set.set(e.type, '#868e96')
    }
    return [...set.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([value, color]) => ({ value, label: value, color }))
  }, [fragmentContents, nodes])

  const edgeRoleOptions = useMemo(() => uniqueSortedRoles(links), [links])

  const severityFilterOptions = useMemo(() => {
    const options = severitiesPresent.map((sev) => ({
      value: sev,
      label: sev,
      color: severityBadgeColor(sev),
    }))
    options.push({
      value: DATA_SEVERITY_NONE.value,
      label: DATA_SEVERITY_NONE.label,
      color: 'gray',
    })
    return options
  }, [severitiesPresent])

  const clearCanvasFilters = useCallback(() => {
    setHighlightedTypes((prev) => (prev.size === 0 ? prev : new Set()))
    setHighlightedEdgeRoles((prev) => (prev.size === 0 ? prev : new Set()))
    setDataEdgeTypes((prev) => (prev.size === 0 ? prev : new Set()))
    setDataEdgeSourceTypes((prev) => (prev.size === 0 ? prev : new Set()))
    setDataEdgeTargetTypes((prev) => (prev.size === 0 ? prev : new Set()))
    setSeverityFilter(new Set())
  }, [setSeverityFilter])

  const annotatedVertexRows = useMemo(() => {
    if (fragmentContents == null) return []
    return structuredVertexRows(fragmentContents)
      .map((row) => ({
        ...row,
        findingSeverity: findingSeverityMaps.sevByEntity.get(row.id),
      }))
      .filter(
        (row) =>
          passesSeverityFilter(row.findingSeverity, severityFilter) &&
          passesTypeFilter(row.type, highlightedTypes),
      )
  }, [findingSeverityMaps, fragmentContents, highlightedTypes, severityFilter])

  const entitiesById = useMemo(() => {
    const map = new Map<string, BoMEntity>()
    for (const e of fragmentContents?.entities ?? []) map.set(e.id, e)
    return map
  }, [fragmentContents])

  const annotatedEdgeRows = useMemo(() => {
    if (fragmentContents == null) return []
    return structuredEdgeRows(fragmentContents)
      .map((row) => ({
        ...row,
        findingSeverity: findingSeverityMaps.sevByEdge.get(row.id),
      }))
      .filter((row) => {
        if (!passesSeverityFilter(row.findingSeverity, severityFilter)) return false
        if (!passesTypeFilter(row.type === '—' ? null : row.type, dataEdgeTypes)) return false
        if (!passesTypeFilter(row.sourceType === '—' ? null : row.sourceType, dataEdgeSourceTypes)) {
          return false
        }
        if (!passesTypeFilter(row.targetType === '—' ? null : row.targetType, dataEdgeTargetTypes)) {
          return false
        }
        if (!passesTypeFilter(row.role, highlightedEdgeRoles)) return false
        return true
      })
  }, [
    dataEdgeSourceTypes,
    dataEdgeTargetTypes,
    dataEdgeTypes,
    findingSeverityMaps,
    fragmentContents,
    highlightedEdgeRoles,
    severityFilter,
  ])

  const vertexPayloadCols = useMemo(
    () => scalarPayloadColumns(annotatedVertexRows.map((r) => r.entity)),
    [annotatedVertexRows],
  )

  const neighborIndex = useMemo(
    () => buildGraphNeighborIndex(annotatedGraph.nodes, annotatedGraph.links),
    [annotatedGraph.nodes, annotatedGraph.links],
  )

  const dataTypeOptions = useMemo(
    () => typeFilterOptions.map((t) => ({ value: t.value, label: t.label })),
    [typeFilterOptions],
  )

  const dataEdgeTypeOptions = useMemo(() => {
    const set = new Set<string>()
    for (const e of fragmentContents?.edges ?? []) {
      if (e.type) set.add(e.type)
    }
    return [...set].sort((a, b) => a.localeCompare(b)).map((value) => ({ value, label: value }))
  }, [fragmentContents])

  const dataEdgeSourceTypeOptions = useMemo(() => {
    const set = new Set<string>()
    for (const e of fragmentContents?.edges ?? []) {
      const t = entitiesById.get(e.source)?.type
      if (t) set.add(t)
    }
    return [...set].sort((a, b) => a.localeCompare(b)).map((value) => ({ value, label: value }))
  }, [entitiesById, fragmentContents])

  const dataEdgeTargetTypeOptions = useMemo(() => {
    const set = new Set<string>()
    for (const e of fragmentContents?.edges ?? []) {
      const t = entitiesById.get(e.target)?.type
      if (t) set.add(t)
    }
    return [...set].sort((a, b) => a.localeCompare(b)).map((value) => ({ value, label: value }))
  }, [entitiesById, fragmentContents])

  const dataEdgeRoleOptions = useMemo(() => {
    const set = new Set<string>()
    for (const e of fragmentContents?.edges ?? []) {
      if (e.role) set.add(e.role)
    }
    for (const role of edgeRoleOptions) set.add(role)
    return [...set].sort((a, b) => a.localeCompare(b)).map((value) => ({ value, label: value }))
  }, [edgeRoleOptions, fragmentContents])

  const edgeDataFiltersActive =
    dataEdgeTypes.size > 0 ||
    dataEdgeSourceTypes.size > 0 ||
    dataEdgeTargetTypes.size > 0 ||
    highlightedEdgeRoles.size > 0

  const dataSeverityFilterOptions = useMemo(() => {
    const options = severitiesPresent.map((sev) => ({ value: sev, label: sev }))
    options.push({ value: DATA_SEVERITY_NONE.value, label: DATA_SEVERITY_NONE.label })
    return options
  }, [severitiesPresent])

  const canvasOverCap =
    fragmentNodeCount > EXPLORER_NODE_CAP ||
    context.nodeCount > EXPLORER_NODE_CAP ||
    nodes.length > EXPLORER_NODE_CAP
  const canvasNodeTotal = Math.max(fragmentNodeCount, context.nodeCount, nodes.length)
  const canvasNonEmpty =
    !canvasOverCap && (annotatedGraph.nodes.length > 0 || annotatedGraph.links.length > 0)
  const dataNonEmpty =
    (fragmentContents?.entities?.length ?? 0) > 0 || (fragmentContents?.edges?.length ?? 0) > 0

  const applyLayout = useCallback(
    (l?: GraphLayout) => {
      if (l) setGraphLayout(l)
      canvasRef.current?.applyLayout(l)
    },
    [],
  )

  const changeLayout = useCallback((next: GraphLayout) => {
    setGraphLayout(next)
    canvasRef.current?.applyLayout(next)
  }, [])

  const focusNode = useCallback((nodeId: string) => {
    canvasRef.current?.focusNode?.(nodeId)
  }, [])

  useImperativeHandle(
    ref,
    () => ({
      applyLayout,
      changeLayout,
      focusNode,
      canvasNonEmpty,
      graphViewTab,
      layout: graphLayout,
    }),
    [applyLayout, canvasNonEmpty, changeLayout, focusNode, graphLayout, graphViewTab],
  )

  useEffect(() => {
    onLayoutReady?.({
      applyLayout,
      changeLayout,
      canvasNonEmpty,
      graphViewTab,
      layout: graphLayout,
    })
  }, [applyLayout, canvasNonEmpty, changeLayout, graphLayout, graphViewTab, onLayoutReady])

  const inspectAnnotatedNodes = useMemo(() => {
    if (annotatedGraph.nodes.length > 0) return annotatedGraph.nodes
    if (fragmentContents?.entities == null) return []
    return fragmentContents.entities.map((entity) => {
      const node = entityToGraphNode(entity)
      const findingSeverity = findingSeverityMaps.sevByEntity.get(entity.id)
      return { ...node, findingSeverity }
    })
  }, [annotatedGraph.nodes, findingSeverityMaps, fragmentContents])

  useEffect(() => {
    onGraphModel?.({
      fragmentContents,
      nodes,
      links,
      annotatedNodes: inspectAnnotatedNodes,
      annotatedLinks: annotatedGraph.links,
    })
  }, [
    annotatedGraph.links,
    fragmentContents,
    inspectAnnotatedNodes,
    links,
    nodes,
    onGraphModel,
  ])

  useEffect(() => {
    const el = columnRef.current
    if (!el) return
    const apply = () => {
      const hostH = el.clientHeight
      if (hostH < 200) return
      const minH = tasksPaneMin(hostH)
      setOutputHeight((h) => {
        const next = Math.max(h, minH)
        if (next !== h) saveNum(OUTPUT_HEIGHT_KEY, next)
        return next
      })
    }
    apply()
    const ro = new ResizeObserver(apply)
    ro.observe(el)
    return () => ro.disconnect()
  }, [])

  const onOutputSplit = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      e.preventDefault()
      const startY = e.clientY
      const startH = outputHeight
      let latest = startH
      const onMove = (ev: PointerEvent) => {
        const host = columnRef.current?.clientHeight ?? 600
        const min = tasksPaneMin(host)
        const max = Math.max(min, host - 160)
        latest = clamp(startH + (startY - ev.clientY), min, max)
        setOutputHeight(latest)
      }
      const onUp = () => {
        window.removeEventListener('pointermove', onMove)
        window.removeEventListener('pointerup', onUp)
        saveNum(OUTPUT_HEIGHT_KEY, latest)
      }
      window.addEventListener('pointermove', onMove)
      window.addEventListener('pointerup', onUp)
    },
    [outputHeight],
  )

  const selectFromDataNode = useCallback(
    (entity: BoMEntity) => {
      const node = entityToGraphNode(entity)
      const findingSeverity = findingSeverityMaps.sevByEntity.get(entity.id)
      setSelection({ kind: 'node', node: { ...node, findingSeverity } })
    },
    [findingSeverityMaps, setSelection],
  )

  const selectFromDataEdge = useCallback(
    (edge: BoMEdge, index: number) => {
      const link = edgeToGraphLink(edge, index)
      const findingSeverity = findingSeverityMaps.sevByEdge.get(link.id)
      setSelection({ kind: 'edge', edge: { ...link, findingSeverity } })
    },
    [findingSeverityMaps, setSelection],
  )

  const selectNodeFromCanvas = useCallback(
    (nodeId: string) => {
      const node = annotatedGraph.nodes.find((n) => n.id === nodeId)
      if (!node) return
      setSelection({ kind: 'node', node })
      requestAnimationFrame(() => canvasRef.current?.focusNode?.(nodeId))
    },
    [annotatedGraph.nodes, setSelection],
  )

  const onCanvasNodeContextMenu = useCallback(
    (
      event: { preventDefault: () => void; clientX: number; clientY: number },
      node: GraphNode,
    ) => {
      event.preventDefault()
      setSelection({ kind: 'node', node })
      setGoToMenu({ x: event.clientX, y: event.clientY, target: { kind: 'node', nodeId: node.id } })
    },
    [setSelection],
  )

  const onCanvasEdgeContextMenu = useCallback(
    (
      event: { preventDefault: () => void; clientX: number; clientY: number },
      edge: GraphLink,
    ) => {
      event.preventDefault()
      setSelection({ kind: 'edge', edge })
      setGoToMenu({
        x: event.clientX,
        y: event.clientY,
        target: { kind: 'edge', sourceId: edge.source, targetId: edge.target },
      })
    },
    [setSelection],
  )

  return (
    <Stack ref={columnRef} gap={0} style={{ flex: 1, minWidth: 0, minHeight: 0, overflow: 'hidden' }}>
      <Paper
        withBorder
        p="xs"
        style={{
          flex: 1,
          minHeight: 0,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
        }}
      >
        {error && (
          <Alert color="red" mb="xs" onClose={() => setError(null)} withCloseButton>
            {error}
          </Alert>
        )}
        <Tabs
          value={graphViewTab}
          onChange={setGraphViewTab}
          style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
          styles={{
            panel: {
              flex: 1,
              minHeight: 0,
              display: 'flex',
              flexDirection: 'column',
              overflow: 'hidden',
            },
          }}
        >
          <Tabs.List style={{ flexShrink: 0 }}>
            <Tabs.Tab value="visual">Visual</Tabs.Tab>
            <Tabs.Tab value="data">Data</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel
            value="visual"
            pt="xs"
            style={{ flex: 1, minHeight: 0, overflow: 'hidden', position: 'relative' }}
          >
            {canvasOverCap ? (
              <Stack align="center" justify="center" gap="sm" p="md" h="100%">
                <Alert color="yellow" title="Graph canvas disabled">
                  This context has {canvasNodeTotal} nodes (cap {EXPLORER_NODE_CAP}). Use the Data
                  tab to browse objects and edges. Check and Evaluate still run against the full
                  fragment.
                </Alert>
              </Stack>
            ) : annotatedGraph.nodes.length === 0 ? (
              <Text size="sm" c="dimmed" p="md">
                {frozenFragment != null
                  ? 'No entities in this archived fragment.'
                  : 'Open a graph or matcher (Matcher / All) in the shared context to preview findings.'}
              </Text>
            ) : (
              <>
                <GraphCanvas
                  ref={canvasRef}
                  nodes={annotatedGraph.nodes}
                  links={annotatedGraph.links}
                  selection={selection}
                  onSelect={setSelection}
                  onNodeContextMenu={onCanvasNodeContextMenu}
                  onEdgeContextMenu={onCanvasEdgeContextMenu}
                  layout={graphLayout}
                  onLayoutChange={setGraphLayout}
                  autoLayoutOnDataChange={false}
                />
                <GraphFilterToolbar
                  types={typeFilterOptions}
                  selectedTypes={highlightedTypes}
                  onToggleType={(type) =>
                    setHighlightedTypes((prev) => toggleTypeInSet(prev, type))
                  }
                  edgeRoles={edgeRoleOptions}
                  selectedEdgeRoles={highlightedEdgeRoles}
                  onToggleEdgeRole={(role) =>
                    setHighlightedEdgeRoles((prev) => toggleTypeInSet(prev, role))
                  }
                  severities={severityFilterOptions}
                  selectedSeverities={severityFilter}
                  onToggleSeverity={(sev) =>
                    setSeverityFilter(toggleTypeInSet(severityFilter, sev))
                  }
                  onReset={clearCanvasFilters}
                />
              </>
            )}
            {!canvasOverCap && (
              <GraphGoToContextMenu
                opened={goToMenu != null}
                x={goToMenu?.x ?? 0}
                y={goToMenu?.y ?? 0}
                onClose={() => setGoToMenu(null)}
                target={goToMenu?.target ?? null}
                nodes={annotatedGraph.nodes}
                index={neighborIndex}
                onGoTo={selectNodeFromCanvas}
              />
            )}
          </Tabs.Panel>
          <Tabs.Panel
            value="data"
            pt="xs"
            style={{
              flex: 1,
              minHeight: 0,
              display: 'flex',
              flexDirection: 'column',
              overflow: 'hidden',
            }}
          >
            {!dataNonEmpty ? (
              <Text size="sm" c="dimmed" p="md">
                {frozenFragment != null
                  ? 'No objects or edges in this archived fragment.'
                  : 'Open a graph or matcher (Matcher / All) in the shared context to browse objects and edges.'}
              </Text>
            ) : (
              <Stack gap="xs" style={{ flex: 1, minHeight: 0, overflow: 'hidden' }}>
                <Tabs
                  value={structVeTab}
                  onChange={(v) => {
                    setStructVeTab(v)
                    setSelection(null)
                  }}
                  style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
                  styles={{
                    panel: {
                      flex: 1,
                      minHeight: 0,
                      display: 'flex',
                      flexDirection: 'column',
                      overflow: 'hidden',
                    },
                  }}
                >
                  <Tabs.List style={{ flexShrink: 0, alignSelf: 'flex-start' }}>
                    <Tabs.Tab value="vertices" style={{ fontSize: 'var(--mantine-font-size-xs)' }}>
                      Vertices ({annotatedVertexRows.length})
                    </Tabs.Tab>
                    <Tabs.Tab value="edges" style={{ fontSize: 'var(--mantine-font-size-xs)' }}>
                      Edges ({annotatedEdgeRows.length})
                    </Tabs.Tab>
                  </Tabs.List>
                  <Tabs.Panel
                    value="vertices"
                    pt="xs"
                    style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
                  >
                    <QueryResultGrid
                      rows={annotatedVertexRows}
                      rowKey={(r) => r.id}
                      selectedKey={selection?.kind === 'node' ? selection.node.id : null}
                      onRowSelect={(row) => selectFromDataNode(row.entity)}
                      empty={
                        <Text size="sm" c="dimmed">
                          {severityFilter.size > 0 || highlightedTypes.size > 0
                            ? 'No vertices match the current filters.'
                            : 'No vertices in this context.'}
                        </Text>
                      }
                      columns={[
                        {
                          key: 'severity',
                          header: (
                            <ColumnFilterHeader
                              label="Severity"
                              options={dataSeverityFilterOptions}
                              selected={severityFilter}
                              onToggle={(v) =>
                                setSeverityFilter(toggleTypeInSet(severityFilter, v))
                              }
                              onClear={() => setSeverityFilter(new Set())}
                              emptyMessage={
                                severitiesPresent.length === 0
                                  ? 'Evaluate to annotate severities'
                                  : 'No values'
                              }
                            />
                          ),
                          width: '12ch',
                          render: (row) =>
                            row.findingSeverity ? (
                              <Badge size="xs" color={severityBadgeColor(row.findingSeverity)}>
                                {row.findingSeverity}
                              </Badge>
                            ) : (
                              <Text size="xs" c="dimmed">
                                —
                              </Text>
                            ),
                        },
                        {
                          key: 'id',
                          header: 'Id',
                          width: QUERY_STRUCT_ID_COL_WIDTH,
                          render: (row) => (
                            <IdLink id={row.id} onOpen={() => selectFromDataNode(row.entity)} />
                          ),
                        },
                        {
                          key: 'type',
                          header: (
                            <ColumnFilterHeader
                              label="Type"
                              options={dataTypeOptions}
                              selected={highlightedTypes}
                              onToggle={(type) =>
                                setHighlightedTypes((prev) => toggleTypeInSet(prev, type))
                              }
                              onClear={() => setHighlightedTypes(new Set())}
                              menuWidth={280}
                            />
                          ),
                          width: QUERY_STRUCT_TYPE_COL_WIDTH,
                          render: (row) => (
                            <Text size="xs" truncate title={row.type}>
                              {row.type}
                            </Text>
                          ),
                        },
                        ...vertexPayloadCols.map((col) => ({
                          key: `payload:${col}`,
                          header: col,
                          render: (row: (typeof annotatedVertexRows)[number]) =>
                            formatObjectCell(row.entity.payload?.[col]),
                        })),
                      ]}
                    />
                  </Tabs.Panel>
                  <Tabs.Panel
                    value="edges"
                    pt="xs"
                    style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
                  >
                    <QueryResultGrid
                      rows={annotatedEdgeRows}
                      rowKey={(r) => r.id}
                      selectedKey={selection?.kind === 'edge' ? selection.edge.id : null}
                      onRowSelect={(row) => selectFromDataEdge(row.edge, 0)}
                      empty={
                        <Text size="sm" c="dimmed">
                          {severityFilter.size > 0 || edgeDataFiltersActive
                            ? 'No edges match the current filters.'
                            : 'No edges in this context.'}
                        </Text>
                      }
                      columns={[
                        {
                          key: 'severity',
                          header: (
                            <ColumnFilterHeader
                              label="Severity"
                              options={dataSeverityFilterOptions}
                              selected={severityFilter}
                              onToggle={(v) =>
                                setSeverityFilter(toggleTypeInSet(severityFilter, v))
                              }
                              onClear={() => setSeverityFilter(new Set())}
                              emptyMessage={
                                severitiesPresent.length === 0
                                  ? 'Evaluate to annotate severities'
                                  : 'No values'
                              }
                            />
                          ),
                          width: '12ch',
                          render: (row) =>
                            row.findingSeverity ? (
                              <Badge size="xs" color={severityBadgeColor(row.findingSeverity)}>
                                {row.findingSeverity}
                              </Badge>
                            ) : (
                              <Text size="xs" c="dimmed">
                                —
                              </Text>
                            ),
                        },
                        {
                          key: 'id',
                          header: 'Id',
                          width: QUERY_STRUCT_ID_COL_WIDTH,
                          render: (row) => (
                            <IdLink id={row.id} onOpen={() => selectFromDataEdge(row.edge, 0)} />
                          ),
                        },
                        {
                          key: 'type',
                          header: (
                            <ColumnFilterHeader
                              label="Type"
                              options={dataEdgeTypeOptions}
                              selected={dataEdgeTypes}
                              onToggle={(type) =>
                                setDataEdgeTypes((prev) => toggleTypeInSet(prev, type))
                              }
                              onClear={() => setDataEdgeTypes(new Set())}
                              menuWidth={280}
                              emptyMessage="No edge types"
                            />
                          ),
                          width: QUERY_STRUCT_TYPE_COL_WIDTH,
                          render: (row) => (
                            <Text size="xs" truncate title={row.type}>
                              {row.type}
                            </Text>
                          ),
                        },
                        {
                          key: 'sourceType',
                          header: (
                            <ColumnFilterHeader
                              label="Source Type"
                              options={dataEdgeSourceTypeOptions}
                              selected={dataEdgeSourceTypes}
                              onToggle={(type) =>
                                setDataEdgeSourceTypes((prev) => toggleTypeInSet(prev, type))
                              }
                              onClear={() => setDataEdgeSourceTypes(new Set())}
                              menuWidth={280}
                            />
                          ),
                          width: QUERY_STRUCT_TYPE_COL_WIDTH,
                          render: (row) => (
                            <Text size="xs" truncate title={row.sourceType}>
                              {row.sourceType}
                            </Text>
                          ),
                        },
                        {
                          key: 'sourceName',
                          header: 'Source name',
                          width: QUERY_STRUCT_EDGE_SOURCE_COL_WIDTH,
                          render: (row) => (
                            <Text size="xs" truncate title={row.sourceName}>
                              {row.sourceName}
                            </Text>
                          ),
                        },
                        {
                          key: 'role',
                          header: (
                            <ColumnFilterHeader
                              label="Role"
                              options={dataEdgeRoleOptions}
                              selected={highlightedEdgeRoles}
                              onToggle={(role) =>
                                setHighlightedEdgeRoles((prev) => toggleTypeInSet(prev, role))
                              }
                              onClear={() => setHighlightedEdgeRoles(new Set())}
                              menuWidth={280}
                            />
                          ),
                          width: QUERY_STRUCT_EDGE_ROLE_COL_WIDTH,
                          render: (row) => (
                            <Text size="xs" truncate title={row.role}>
                              {row.role}
                            </Text>
                          ),
                        },
                        {
                          key: 'targetType',
                          header: (
                            <ColumnFilterHeader
                              label="Target Type"
                              options={dataEdgeTargetTypeOptions}
                              selected={dataEdgeTargetTypes}
                              onToggle={(type) =>
                                setDataEdgeTargetTypes((prev) => toggleTypeInSet(prev, type))
                              }
                              onClear={() => setDataEdgeTargetTypes(new Set())}
                              menuWidth={280}
                            />
                          ),
                          width: QUERY_STRUCT_TYPE_COL_WIDTH,
                          render: (row) => (
                            <Text size="xs" truncate title={row.targetType}>
                              {row.targetType}
                            </Text>
                          ),
                        },
                        {
                          key: 'targetName',
                          header: 'Target name',
                          render: (row) => row.targetName,
                        },
                      ]}
                    />
                  </Tabs.Panel>
                </Tabs>
              </Stack>
            )}
          </Tabs.Panel>
        </Tabs>
      </Paper>

      {!hideOutput && (
        <>
          <Box
            role="separator"
            aria-orientation="horizontal"
            onPointerDown={onOutputSplit}
            style={{ height: SPLITTER, cursor: 'row-resize', flexShrink: 0 }}
          />

          <Paper
            withBorder
            style={{
              height: outputHeight,
              minHeight: outputHeight,
              flexShrink: 0,
              flexGrow: 0,
              display: 'flex',
              flexDirection: 'column',
              overflow: 'hidden',
            }}
            data-tour="policy-output"
          >
            {output}
          </Paper>
        </>
      )}
    </Stack>
  )
})
