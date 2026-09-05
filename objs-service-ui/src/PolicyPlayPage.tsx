import { useCallback, useEffect, useMemo, useRef, useState, memo, type PointerEvent as ReactPointerEvent } from 'react'
import {
  Alert,
  Badge,
  Box,
  Button,
  Code,
  Group,
  Menu,
  Modal,
  MultiSelect,
  Paper,
  ScrollArea,
  Select,
  Stack,
  Tabs,
  TagsInput,
  Text,
  Textarea,
  TextInput,
  Title,
  Tooltip,
} from '@mantine/core'
import { IconChevronDown, IconChevronRight, IconPlus } from '@tabler/icons-react'
import { getGraph, getGraphVersion, listSchemas, queryAddObjects, toGraphData } from './api'
import { GraphCanvas, type GraphCanvasHandle, type GraphLayout } from './GraphCanvas'
import { GraphContextBar } from './GraphContextBar'
import { useGraphContext } from './GraphContextProvider'
import {
  GraphGoToContextMenu,
  buildGraphNeighborIndex,
  type GraphGoToTarget,
} from './graphGoToNav'
import { ObjectInspectPane } from './ObjectInspectPane'
import {
  checkPolicy,
  createCategory,
  createPolicy,
  deleteCategory,
  deletePolicy,
  evaluatePolicy,
  fetchPolicyCapabilities,
  listCategories,
  listPolicies,
  updatePolicy,
} from './policyApi'
import {
  findingRuleName,
  maxSeverity,
  formatPolicyVersion,
  severityRank,
  type Category,
  type EvaluationResult,
  type Finding,
  type Policy,
  type PolicyCheckResult,
} from './policyTypes'
import { KeyValueRowsEditor, rowsToStringMap, stringMapToRows, type KeyValueRow } from './KeyValueRowsEditor'
import { formatObjectCell, scalarPayloadColumns } from './ObjectResultsTable'
import { objectDisplayTitle } from './objectViewerTitle'
import { payloadFieldKindsByTypeVersion } from './payloadFieldKinds'
import { policyFragmentMatcher } from './queryGraphContext'
import { EXPLORER_NODE_CAP } from './graphContextVersions'
import { formatQueryDuration, type QueryExecStats } from './queryExecStats'
import { QueryResultGrid } from './QueryResultGrid'
import {
  IdLink,
  QUERY_STRUCT_EDGE_ROLE_COL_WIDTH,
  QUERY_STRUCT_EDGE_SOURCE_COL_WIDTH,
  QUERY_STRUCT_ID_COL_WIDTH,
  QUERY_STRUCT_TYPE_COL_WIDTH,
} from './QueryStructColumns'
import {
  structuredEdgeRows,
  structuredVertexRows,
} from './queryStructuredModel'
import { clamp, maxSidePaneWidth } from './sidePaneSplit'
import { SyntaxCodeEditor, type SyntaxCodeEditorHandle } from './SyntaxCodeEditor'
import type {
  BoMEdge,
  BoMEntity,
  BoMGraphContents,
  BoMSchema,
  GraphLink,
  GraphNode,
  GraphSelection,
} from './types'
import { VIEW_ACTION_BUTTON_SIZE } from './viewActionButtons'

const LEFT_WIDTH_KEY = 'objs.ui.policy.leftPaneWidth'
const RIGHT_WIDTH_KEY = 'objs.ui.policy.rightPaneWidth'
const EDITOR_FRAC_KEY = 'objs.ui.policy.editorFrac'
const TASKS_HEIGHT_KEY = 'objs.ui.policy.tasksHeight'
const SPLITTER = 8
/** Absolute floor; effective min is max(this, host/8). */
const MIN_SIDE_ABS = 160
const MIN_TASKS_ABS = 140

function sidePaneMin(hostWidth: number): number {
  return Math.max(MIN_SIDE_ABS, Math.floor(hostWidth / 8))
}

function tasksPaneMin(hostHeight: number): number {
  return Math.max(MIN_TASKS_ABS, Math.floor(hostHeight / 8))
}

function severityBadgeColor(raw: string | null | undefined): string {
  switch ((raw ?? '').trim().toUpperCase()) {
    case 'ERROR':
    case 'FAIL':
      return 'red'
    case 'WARN':
    case 'WARNING':
      return 'orange'
    case 'OK':
    case 'PASS':
      return 'green'
    case 'INFO':
      return 'cyan'
    default:
      return 'gray'
  }
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

const DATA_SEVERITY_NONE = { value: 'NONE', label: 'None' } as const

type PolicyEvalStats = QueryExecStats & { findings: number }

function formatPolicyEvalStats(stats: PolicyEvalStats): string {
  return `${formatQueryDuration(stats.durationMs)} · ${stats.findings} finding${
    stats.findings === 1 ? '' : 's'
  } · ${stats.nodes} nodes · ${stats.edges} edges`
}

const GRAPH_LAYOUTS: { value: GraphLayout; label: string }[] = [
  { value: 'TB', label: 'Top to bottom' },
  { value: 'LR', label: 'Left to right' },
  { value: 'BT', label: 'Bottom to top' },
  { value: 'RL', label: 'Right to left' },
]

const DEFAULT_DRL = `package org.poc.objs.policy.playground
import org.poc.objs.policy.drools.DroolsEvaluationScratch;
global DroolsEvaluationScratch scratch;
rule "playground-pass"
when
then
end
`

function loadNum(key: string, fallback: number): number {
  try {
    const n = Number(localStorage.getItem(key))
    return Number.isFinite(n) ? n : fallback
  } catch {
    return fallback
  }
}

function saveNum(key: string, value: number) {
  try {
    localStorage.setItem(key, String(Math.round(value)))
  } catch {
    /* ignore */
  }
}

type TaskRow =
  | { kind: 'check'; id: string; message: string; line?: number; column?: number }
  | { kind: 'finding'; id: string; finding: Finding; status: string; policyName: string }

function findingsFromResult(result: EvaluationResult | null): TaskRow[] {
  if (!result) return []
  const rows: TaskRow[] = []
  result.outcomes.forEach((o, oi) => {
    ;(o.findings ?? []).forEach((f, fi) => {
      rows.push({
        kind: 'finding',
        id: `f-${oi}-${fi}-${f.message}`,
        finding: f,
        status: o.status,
        policyName: o.policyName,
      })
    })
    if ((o.findings ?? []).length === 0) {
      rows.push({
        kind: 'finding',
        id: `o-${oi}`,
        finding: { message: o.message || o.status, severity: o.status === 'PASS' ? 'OK' : o.status },
        status: o.status,
        policyName: o.policyName,
      })
    }
  })
  return rows
}

/**
 * Owns DRL text locally so keystrokes do not re-render the heavy Policy page
 * (graph / data grids). Parent only seeds body when the selection changes.
 */
const PolicyDrlEditor = memo(function PolicyDrlEditor({
  policyKey,
  body,
  readOnly,
  onDirty,
  editorRef,
}: {
  policyKey: string | null
  body: string
  readOnly: boolean
  onDirty: () => void
  editorRef: React.Ref<SyntaxCodeEditorHandle>
}) {
  const [value, setValue] = useState(body)

  useEffect(() => {
    setValue(body)
  }, [policyKey, body])

  const onChange = useCallback(
    (next: string) => {
      setValue(next)
      onDirty()
    },
    [onDirty],
  )

  return (
    <SyntaxCodeEditor
      ref={editorRef}
      language="drools"
      value={value}
      onChange={onChange}
      fillHeight
      minHeight={120}
      readOnly={readOnly}
    />
  )
})

export function PolicyPlayPage() {
  const { context } = useGraphContext()
  const canvasRef = useRef<GraphCanvasHandle>(null)
  const editorRef = useRef<SyntaxCodeEditorHandle>(null)
  const splitHostRef = useRef<HTMLDivElement>(null)

  const [capable, setCapable] = useState<boolean | null>(null)
  const [policies, setPolicies] = useState<Policy[]>([])
  type NavSel = { kind: 'category'; id: string } | { kind: 'policy'; id: string }
  const [nav, setNav] = useState<NavSel | null>(null)
  const [expandedCats, setExpandedCats] = useState<Set<string>>(() => new Set())
  const [editorName, setEditorName] = useState('')
  const [editorBody, setEditorBody] = useState('')
  const [editorDescription, setEditorDescription] = useState('')
  const [editorCategoryId, setEditorCategoryId] = useState<string | null>(null)
  const [editorTags, setEditorTags] = useState<string[]>([])
  const [editorAnnoRows, setEditorAnnoRows] = useState<KeyValueRow[]>([])
  const [editorVersion, setEditorVersion] = useState('0.1')
  const [editorTab, setEditorTab] = useState<string | null>('general')
  const [categories, setCategories] = useState<Category[]>([])
  const [filterCategoryId, setFilterCategoryId] = useState<string | null>(null)
  const [filterTags, setFilterTags] = useState<string[]>([])
  const [filterName, setFilterName] = useState('')
  const [catDisplayName, setCatDisplayName] = useState('')
  const [catSlug, setCatSlug] = useState('')
  const [addCategoryOpen, setAddCategoryOpen] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [confirmKind, setConfirmKind] = useState<'discard' | 'delete-policy' | 'delete-category'>('discard')
  const [pendingNav, setPendingNav] = useState<NavSel | null>(null)
  const [dirty, setDirty] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [nodes, setNodes] = useState<GraphNode[]>([])
  const [links, setLinks] = useState<GraphLink[]>([])
  /** Full fragment for Data tab (kept even when Visual canvas is cleared for over-cap). */
  const [fragmentContents, setFragmentContents] = useState<BoMGraphContents | null>(null)
  /** Entity count of last loaded fragment (even when canvas is cleared for over-cap). */
  const [fragmentNodeCount, setFragmentNodeCount] = useState(0)
  const [schemas, setSchemas] = useState<BoMSchema[]>([])
  const [selection, setSelection] = useState<GraphSelection | null>(null)
  const [layout, setLayout] = useState<GraphLayout>('TB')
  const [goToMenu, setGoToMenu] = useState<{ x: number; y: number; target: GraphGoToTarget } | null>(
    null,
  )
  const [graphViewTab, setGraphViewTab] = useState<string | null>('visual')
  const [structVeTab, setStructVeTab] = useState<string | null>('vertices')

  const [checkResult, setCheckResult] = useState<PolicyCheckResult | null>(null)
  const [evalResult, setEvalResult] = useState<EvaluationResult | null>(null)
  const [evalStats, setEvalStats] = useState<PolicyEvalStats | null>(null)
  const [tasksTab, setTasksTab] = useState<string | null>('policy')
  const [inspectTab, setInspectTab] = useState<string | null>('object')
  const [focusedTaskId, setFocusedTaskId] = useState<string | null>(null)
  const [severityFilter, setSeverityFilter] = useState<Set<string>>(() => new Set())

  const [leftWidth, setLeftWidth] = useState(() => loadNum(LEFT_WIDTH_KEY, 240))
  const [rightWidth, setRightWidth] = useState(() => loadNum(RIGHT_WIDTH_KEY, 300))
  const [editorFrac, setEditorFrac] = useState(() => {
    const v = loadNum(EDITOR_FRAC_KEY, 50)
    return Math.min(80, Math.max(20, v)) / 100
  })
  const [tasksHeight, setTasksHeight] = useState(() =>
    Math.max(MIN_TASKS_ABS, loadNum(TASKS_HEIGHT_KEY, 180)),
  )

  const selectedPolicy =
    nav?.kind === 'policy' ? (policies.find((p) => p.id === nav.id) ?? null) : null
  const selectedCategory =
    nav?.kind === 'category' ? (categories.find((c) => c.id === nav.id) ?? null) : null
  const selectedId = nav?.kind === 'policy' ? nav.id : null

  const policiesInSelectedCategory = useMemo(() => {
    if (!selectedCategory) return []
    return policies.filter((p) => p.categoryId === selectedCategory.id)
  }, [policies, selectedCategory])

  const treeCategories = useMemo(() => {
    const cats = filterCategoryId
      ? categories.filter((c) => c.id === filterCategoryId)
      : categories
    return cats.map((c) => ({
      category: c,
      policies: policies.filter((p) => p.categoryId === c.id),
    }))
  }, [categories, policies, filterCategoryId])

  const markEditorDirty = useCallback(() => setDirty(true), [])

  const liveEditorBody = useCallback(
    () => editorRef.current?.getValue() ?? editorBody,
    [editorBody],
  )

  // Enforce ≥ 1/8 host for side panes + tasks (Note1): fix tiny first-open / stale localStorage.
  useEffect(() => {
    const el = splitHostRef.current
    if (!el) return
    const apply = () => {
      const hostW = el.clientWidth
      const hostH = el.clientHeight
      if (hostW >= 200) {
        const min = sidePaneMin(hostW)
        setLeftWidth((w) => {
          const next = Math.max(w, min)
          if (next !== w) saveNum(LEFT_WIDTH_KEY, next)
          return next
        })
        setRightWidth((w) => {
          const next = Math.max(w, min)
          if (next !== w) saveNum(RIGHT_WIDTH_KEY, next)
          return next
        })
      }
      if (hostH >= 200) {
        const minH = tasksPaneMin(hostH)
        setTasksHeight((h) => {
          const next = Math.max(h, minH)
          if (next !== h) saveNum(TASKS_HEIGHT_KEY, next)
          return next
        })
      }
    }
    apply()
    const ro = new ResizeObserver(apply)
    ro.observe(el)
    return () => ro.disconnect()
  }, [])

  const fieldKindsByTypeVersion = useMemo(
    () => payloadFieldKindsByTypeVersion(schemas),
    [schemas],
  )

  const refreshCategories = useCallback(async () => {
    const rows = await listCategories()
    setCategories(rows)
    return rows
  }, [])

  const refreshPolicies = useCallback(async () => {
    const rows = await listPolicies({
      tags: filterTags,
      name: filterName.trim() || null,
    })
    setPolicies(rows)
    return rows
  }, [filterTags, filterName])

  function seedEditor(p: Policy | null) {
    if (!p) {
      setEditorName('')
      setEditorBody('')
      setEditorDescription('')
      setEditorCategoryId(null)
      setEditorTags([])
      setEditorAnnoRows([])
      setEditorVersion('0.1')
      return
    }
    setEditorName(p.name)
    setEditorBody(p.body)
    setEditorDescription(p.description ?? '')
    setEditorCategoryId(p.categoryId)
    setEditorTags([...(p.tags ?? [])])
    setEditorAnnoRows(stringMapToRows(p.annotations ?? {}))
    setEditorVersion(p.version ?? '0.1')
  }

  function toggleCatExpanded(id: string) {
    setExpandedCats((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  function requestNav(next: NavSel | null) {
    if (dirty) {
      setPendingNav(next)
      setConfirmKind('discard')
      setConfirmOpen(true)
      return
    }
    applyNav(next)
  }

  function applyNav(next: NavSel | null, policyOverride?: Policy | null) {
    setNav(next)
    if (next?.kind === 'policy') {
      const p =
        policyOverride ?? policies.find((x) => x.id === next.id) ?? null
      seedEditor(p)
      if (p) {
        setExpandedCats((prev) => new Set(prev).add(p.categoryId))
      }
    } else {
      seedEditor(null)
      if (next?.kind === 'category') {
        setExpandedCats((prev) => new Set(prev).add(next.id))
      }
    }
    setDirty(false)
  }

  useEffect(() => {
    void (async () => {
      const caps = await fetchPolicyCapabilities()
      setCapable(caps != null)
      if (!caps) return
      try {
        await refreshCategories()
        await refreshPolicies()
      } catch (ex) {
        setError(ex instanceof Error ? ex.message : String(ex))
      }
    })()
  }, [refreshPolicies, refreshCategories])

  useEffect(() => {
    if (!capable) return
    void refreshPolicies().catch((ex) => setError(ex instanceof Error ? ex.message : String(ex)))
  }, [capable, refreshPolicies])

  useEffect(() => {
    if (!filterCategoryId) return
    if (dirty) return
    applyNav({ kind: 'category', id: filterCategoryId })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterCategoryId])

  useEffect(() => {
    void listSchemas().then(setSchemas).catch(() => setSchemas([]))
  }, [])

  const loadCanvas = useCallback(async () => {
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
      setSelection(null)
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
  }, [context, schemas])

  useEffect(() => {
    void loadCanvas()
  }, [loadCanvas])

  const checkRows: TaskRow[] = useMemo(() => {
    const issues = checkResult?.issues
    if (issues && issues.length > 0) {
      return issues.map((issue, i) => ({
        kind: 'check' as const,
        id: `c-${i}`,
        message:
          issue.line != null
            ? `line ${issue.line}${issue.column != null ? `:${issue.column}` : ''}: ${issue.message}`
            : issue.message,
        line: issue.line ?? undefined,
        column: issue.column ?? undefined,
      }))
    }
    return (checkResult?.messages ?? []).map((message, i) => ({
      kind: 'check' as const,
      id: `c-${i}`,
      message,
    }))
  }, [checkResult])
  const evalRows = useMemo(() => findingsFromResult(evalResult), [evalResult])

  const severitiesPresent = useMemo(() => {
    const s = new Set<string>()
    evalRows.forEach((r) => {
      if (r.kind === 'finding' && r.finding.severity) s.add(r.finding.severity.toUpperCase())
    })
    return [...s].sort((a, b) => severityRank(b) - severityRank(a))
  }, [evalRows])

  const dataSeverityOptions = useMemo(() => {
    const options = severitiesPresent.map((sev) => ({ value: sev, label: sev }))
    options.push({ value: DATA_SEVERITY_NONE.value, label: DATA_SEVERITY_NONE.label })
    return options
  }, [severitiesPresent])

  const findingSeverityMaps = useMemo(() => {
    const sevByEntity = new Map<string, string>()
    const sevByEdge = new Map<string, string>()
    evalRows.forEach((r) => {
      if (r.kind !== 'finding') return
      const sev = r.finding.severity?.toUpperCase()
      ;(r.finding.entities ?? []).forEach((id) => {
        sevByEntity.set(id, maxSeverity(sevByEntity.get(id), sev) ?? sev ?? 'OK')
      })
      ;(r.finding.edges ?? []).forEach((id) => {
        sevByEdge.set(id, maxSeverity(sevByEdge.get(id), sev) ?? sev ?? 'OK')
      })
    })
    return { sevByEntity, sevByEdge }
  }, [evalRows])

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
    return { nodes: nodesOut, links: linksOut }
  }, [findingSeverityMaps, links, nodes, severityFilter])

  const annotatedVertexRows = useMemo(() => {
    if (fragmentContents == null) return []
    return structuredVertexRows(fragmentContents)
      .map((row) => ({
        ...row,
        findingSeverity: findingSeverityMaps.sevByEntity.get(row.id),
      }))
      .filter((row) => passesSeverityFilter(row.findingSeverity, severityFilter))
  }, [findingSeverityMaps, fragmentContents, severityFilter])

  const annotatedEdgeRows = useMemo(() => {
    if (fragmentContents == null) return []
    return structuredEdgeRows(fragmentContents)
      .map((row) => ({
        ...row,
        findingSeverity: findingSeverityMaps.sevByEdge.get(row.id),
      }))
      .filter((row) => passesSeverityFilter(row.findingSeverity, severityFilter))
  }, [findingSeverityMaps, fragmentContents, severityFilter])

  const vertexPayloadCols = useMemo(
    () => scalarPayloadColumns(annotatedVertexRows.map((r) => r.entity)),
    [annotatedVertexRows],
  )

  const inspectNodes = useMemo(() => {
    if (annotatedGraph.nodes.length > 0) return annotatedGraph.nodes
    if (fragmentContents?.entities == null) return []
    return fragmentContents.entities.map((entity) => {
      const node = entityToGraphNode(entity)
      const findingSeverity = findingSeverityMaps.sevByEntity.get(entity.id)
      return { ...node, findingSeverity }
    })
  }, [annotatedGraph.nodes, findingSeverityMaps, fragmentContents])

  const neighborIndex = useMemo(
    () => buildGraphNeighborIndex(annotatedGraph.nodes, annotatedGraph.links),
    [annotatedGraph.nodes, annotatedGraph.links],
  )

  const canvasOverCap =
    fragmentNodeCount > EXPLORER_NODE_CAP ||
    context.nodeCount > EXPLORER_NODE_CAP ||
    nodes.length > EXPLORER_NODE_CAP
  const canvasNodeTotal = Math.max(fragmentNodeCount, context.nodeCount, nodes.length)
  const canvasNonEmpty = !canvasOverCap && (annotatedGraph.nodes.length > 0 || annotatedGraph.links.length > 0)
  const dataNonEmpty =
    (fragmentContents?.entities?.length ?? 0) > 0 || (fragmentContents?.edges?.length ?? 0) > 0

  const selectFromDataNode = useCallback(
    (entity: BoMEntity) => {
      const node = entityToGraphNode(entity)
      const findingSeverity = findingSeverityMaps.sevByEntity.get(entity.id)
      setSelection({ kind: 'node', node: { ...node, findingSeverity } })
      setInspectTab('object')
    },
    [findingSeverityMaps],
  )

  const selectFromDataEdge = useCallback(
    (edge: BoMEdge, index: number) => {
      const link = edgeToGraphLink(edge, index)
      const findingSeverity = findingSeverityMaps.sevByEdge.get(link.id)
      setSelection({ kind: 'edge', edge: { ...link, findingSeverity } })
      setInspectTab('object')
    },
    [findingSeverityMaps],
  )

  const selectionTaskRows = useMemo(() => {
    if (!selection) return evalRows
    if (selection.kind === 'node') {
      return evalRows.filter(
        (r) => r.kind === 'finding' && (r.finding.entities ?? []).includes(selection.node.id),
      )
    }
    return evalRows.filter(
      (r) => r.kind === 'finding' && (r.finding.edges ?? []).includes(selection.edge.id),
    )
  }, [evalRows, selection])

  const focusedTask = useMemo(() => {
    const all = [...checkRows, ...evalRows]
    return all.find((r) => r.id === focusedTaskId) ?? null
  }, [checkRows, evalRows, focusedTaskId])

  async function onAddPolicy() {
    if (!capable) return
    const categoryId =
      (nav?.kind === 'category' ? nav.id : null) ??
      (nav?.kind === 'policy' ? selectedPolicy?.categoryId : null) ??
      filterCategoryId ??
      categories[0]?.id ??
      null
    if (categoryId == null) {
      setError('Create a category first (Add → Category), then add a policy.')
      setCatDisplayName('')
      setCatSlug('')
      setAddCategoryOpen(true)
      return
    }
    setBusy(true)
    setError(null)
    try {
      const created = await createPolicy({
        name: `policy-${policies.length + 1}`,
        engineKind: 'DROOLS',
        body: DEFAULT_DRL,
        applicabilityKind: 'ALWAYS_APPLY',
        categoryId,
        tags: ['new'],
        version: '0.1',
        annotations: {},
        description: '',
      })
      await refreshPolicies()
      applyNav({ kind: 'policy', id: created.id }, created)
      setEditorTab('code')
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : String(ex))
    } finally {
      setBusy(false)
    }
  }

  function onDeleteClick() {
    if (!capable || !nav) return
    if (nav.kind === 'category') {
      setConfirmKind('delete-category')
      setConfirmOpen(true)
    } else {
      setConfirmKind('delete-policy')
      setConfirmOpen(true)
    }
  }

  async function confirmDeletePolicy() {
    if (!selectedPolicy) return
    setBusy(true)
    setError(null)
    setConfirmOpen(false)
    try {
      const catId = selectedPolicy.categoryId
      await deletePolicy(selectedPolicy.id)
      await refreshPolicies()
      applyNav({ kind: 'category', id: catId })
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : String(ex))
    } finally {
      setBusy(false)
    }
  }

  async function confirmDeleteCategory() {
    if (!selectedCategory) return
    setBusy(true)
    setError(null)
    setConfirmOpen(false)
    try {
      await deleteCategory(selectedCategory.id)
      if (filterCategoryId === selectedCategory.id) setFilterCategoryId(null)
      await refreshCategories()
      await refreshPolicies()
      applyNav(null)
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : String(ex))
    } finally {
      setBusy(false)
    }
  }

  function confirmDiscard() {
    setConfirmOpen(false)
    const next = pendingNav
    setPendingNav(null)
    applyNav(next)
  }

  async function onSave() {
    if (!selectedPolicy || !capable) return
    const name = editorName.trim()
    if (!name) {
      setError('Policy name is required')
      return
    }
    setBusy(true)
    setError(null)
    try {
      if (!editorCategoryId) {
        setError('Category is required')
        setBusy(false)
        return
      }
      const tags = editorTags.map((t) => t.trim().toLowerCase()).filter(Boolean)
      if (tags.length === 0) {
        setError('At least one tag is required')
        setBusy(false)
        return
      }
      const updated = await updatePolicy(selectedPolicy.id, {
        name,
        engineKind: selectedPolicy.engineKind || 'DROOLS',
        body: liveEditorBody(),
        applicabilityKind: selectedPolicy.applicabilityKind ?? 'ALWAYS_APPLY',
        categoryId: editorCategoryId,
        tags,
        annotations: rowsToStringMap(editorAnnoRows),
        version: editorVersion.trim() || '0.1',
        description: editorDescription,
      })
      await refreshPolicies()
      applyNav({ kind: 'policy', id: updated.id }, updated)
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : String(ex))
    } finally {
      setBusy(false)
    }
  }

  async function onCheck() {
    if (!capable) return
    setBusy(true)
    setError(null)
    try {
      const result = await checkPolicy(liveEditorBody())
      setCheckResult(result)
      setTasksTab('policy')
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : String(ex))
    } finally {
      setBusy(false)
    }
  }

  async function onEvaluate() {
    if (!capable || !selectedPolicy) return
    if (context.kind === 'empty') {
      setError('Open a graph or matcher (Matcher / All) in the shared context before Evaluate')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const scope = policyFragmentMatcher(context)
      const started = performance.now()
      const result = await evaluatePolicy({
        matcher: scope.matcher,
        graphId: scope.graphId,
        graphVersion: scope.graphVersion,
        policyId: selectedPolicy.id,
        body: liveEditorBody(),
        engineKind: 'DROOLS',
        policyName: editorName.trim() || selectedPolicy.name,
      })
      const durationMs = performance.now() - started
      const findings = result.outcomes.reduce((n, o) => n + (o.findings?.length ?? 0), 0)
      setEvalResult(result)
      setEvalStats({
        durationMs,
        findings,
        nodes: fragmentContents?.entities?.length ?? fragmentNodeCount,
        edges: fragmentContents?.edges?.length ?? 0,
      })
      setTasksTab('evaluations')
      setSeverityFilter(new Set())
      await loadCanvas()
    } catch (ex) {
      setEvalStats(null)
      setError(ex instanceof Error ? ex.message : String(ex))
    } finally {
      setBusy(false)
    }
  }

  function selectPolicy(p: Policy) {
    requestNav({ kind: 'policy', id: p.id })
  }

  function selectCategory(c: Category) {
    requestNav({ kind: 'category', id: c.id })
  }

  async function onCreateCategory() {
    const displayName = catDisplayName.trim()
    const slug = catSlug.trim().toLowerCase()
    if (!displayName || !slug) {
      setError('Category display name and slug are required')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const created = await createCategory({ displayName, slug })
      setCatDisplayName('')
      setCatSlug('')
      setAddCategoryOpen(false)
      await refreshCategories()
      applyNav({ kind: 'category', id: created.id })
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : String(ex))
    } finally {
      setBusy(false)
    }
  }


  function onTaskClick(row: TaskRow) {
    setFocusedTaskId(row.id)
    setInspectTab('tasks')
    if (row.kind !== 'finding') return
    const entityId = row.finding.entities?.[0]
    const edgeId = row.finding.edges?.[0]
    if (entityId) {
      const node = annotatedGraph.nodes.find((n) => n.id === entityId)
      if (node) {
        setSelection({ kind: 'node', node })
        canvasRef.current?.focusNode?.(node.id)
      }
    } else if (edgeId) {
      const edge = annotatedGraph.links.find((l) => l.id === edgeId)
      if (edge) setSelection({ kind: 'edge', edge })
    }
  }

  function changeLayout(next: GraphLayout) {
    setLayout(next)
    canvasRef.current?.applyLayout(next)
  }

  function selectNodeFromCanvas(nodeId: string) {
    const node = annotatedGraph.nodes.find((n) => n.id === nodeId)
    if (!node) return
    setSelection({ kind: 'node', node })
    requestAnimationFrame(() => canvasRef.current?.focusNode?.(nodeId))
  }

  function onCanvasNodeContextMenu(
    event: { preventDefault: () => void; clientX: number; clientY: number },
    node: GraphNode,
  ) {
    event.preventDefault()
    setSelection({ kind: 'node', node })
    setGoToMenu({ x: event.clientX, y: event.clientY, target: { kind: 'node', nodeId: node.id } })
  }

  function onCanvasEdgeContextMenu(
    event: { preventDefault: () => void; clientX: number; clientY: number },
    edge: GraphLink,
  ) {
    event.preventDefault()
    setSelection({ kind: 'edge', edge })
    setGoToMenu({
      x: event.clientX,
      y: event.clientY,
      target: { kind: 'edge', sourceId: edge.source, targetId: edge.target },
    })
  }

  const dragLeft = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      e.currentTarget.setPointerCapture(e.pointerId)
      const startX = e.clientX
      const startW = leftWidth
      let latest = startW
      const onMove = (ev: PointerEvent) => {
        const host = splitHostRef.current?.clientWidth ?? 1200
        const min = sidePaneMin(host)
        latest = clamp(startW + (ev.clientX - startX), min, maxSidePaneWidth(host, min))
        setLeftWidth(latest)
      }
      const onUp = () => {
        window.removeEventListener('pointermove', onMove)
        window.removeEventListener('pointerup', onUp)
        saveNum(LEFT_WIDTH_KEY, latest)
      }
      window.addEventListener('pointermove', onMove)
      window.addEventListener('pointerup', onUp)
    },
    [leftWidth],
  )

  // Simplified: use frac drag via pointer on mid splitter
  const onEditorGraphSplit = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      e.currentTarget.setPointerCapture(e.pointerId)
      const host = e.currentTarget.parentElement
      if (!host) return
      const rect = host.getBoundingClientRect()
      let latest = editorFrac
      const onMove = (ev: PointerEvent) => {
        const x = ev.clientX - rect.left
        latest = clamp(x / rect.width, 0.2, 0.8)
        setEditorFrac(latest)
      }
      const onUp = () => {
        window.removeEventListener('pointermove', onMove)
        window.removeEventListener('pointerup', onUp)
        saveNum(EDITOR_FRAC_KEY, latest * 100)
      }
      window.addEventListener('pointermove', onMove)
      window.addEventListener('pointerup', onUp)
    },
    [editorFrac],
  )

  const onTasksSplit = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      e.currentTarget.setPointerCapture(e.pointerId)
      const startY = e.clientY
      const startH = tasksHeight
      let latest = startH
      const onMove = (ev: PointerEvent) => {
        const host = splitHostRef.current?.clientHeight ?? 600
        const min = tasksPaneMin(host)
        const max = Math.max(min + 40, Math.floor(host * 0.55))
        latest = clamp(startH - (ev.clientY - startY), min, max)
        setTasksHeight(latest)
      }
      const onUp = () => {
        window.removeEventListener('pointermove', onMove)
        window.removeEventListener('pointerup', onUp)
        saveNum(TASKS_HEIGHT_KEY, latest)
      }
      window.addEventListener('pointermove', onMove)
      window.addEventListener('pointerup', onUp)
    },
    [tasksHeight],
  )

  const onRightSplit = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      e.currentTarget.setPointerCapture(e.pointerId)
      const startX = e.clientX
      const startW = rightWidth
      let latest = startW
      const onMove = (ev: PointerEvent) => {
        const host = splitHostRef.current?.clientWidth ?? 1200
        const min = sidePaneMin(host)
        latest = clamp(startW - (ev.clientX - startX), min, maxSidePaneWidth(host, min))
        setRightWidth(latest)
      }
      const onUp = () => {
        window.removeEventListener('pointermove', onMove)
        window.removeEventListener('pointerup', onUp)
        saveNum(RIGHT_WIDTH_KEY, latest)
      }
      window.addEventListener('pointermove', onMove)
      window.addEventListener('pointerup', onUp)
    },
    [rightWidth],
  )

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group align="center" wrap="nowrap" gap="md" style={{ flexShrink: 0 }}>
        <Title order={3} style={{ flexShrink: 0 }}>
          Policy
        </Title>
        <Box style={{ flex: 1, minWidth: 0 }}>
          <GraphContextBar />
        </Box>
      </Group>

      <Group
        justify="space-between"
        align="center"
        wrap="wrap"
        gap="xs"
        style={{ flexShrink: 0 }}
        data-tour="policy-view-actions"
      >
        <Group gap={6} wrap="wrap" style={{ flex: 1, minWidth: 0 }} align="center">
          <Select
            size={VIEW_ACTION_BUTTON_SIZE}
            clearable
            placeholder="Categories"
            data={categories.map((c) => ({ value: c.id, label: c.displayName }))}
            value={filterCategoryId}
            onChange={setFilterCategoryId}
            disabled={!capable}
            w={180}
          />
          <MultiSelect
            size={VIEW_ACTION_BUTTON_SIZE}
            clearable
            searchable
            placeholder="Tags"
            data={Array.from(
              new Set(policies.flatMap((p) => p.tags ?? []).concat(filterTags)),
            ).map((t) => ({ value: t, label: t }))}
            value={filterTags}
            onChange={setFilterTags}
            disabled={!capable}
            w={220}
          />
          <Text size="xs" c="dimmed" style={{ alignSelf: 'center' }}>
            {evalStats != null ? formatPolicyEvalStats(evalStats) : dirty ? 'unsaved' : '\u00a0'}
          </Text>
          {severitiesPresent.map((sev) => {
            const active = severityFilter.has(sev)
            const color = severityBadgeColor(sev)
            return (
              <Badge
                key={sev}
                variant={active ? 'filled' : 'outline'}
                color={color}
                style={{ cursor: 'pointer' }}
                onClick={() =>
                  setSeverityFilter((prev) => {
                    const next = new Set(prev)
                    if (next.has(sev)) next.delete(sev)
                    else next.add(sev)
                    return next
                  })
                }
              >
                {sev}
              </Badge>
            )
          })}
          {severityFilter.size > 0 && (
            <Button size="compact-xs" variant="subtle" onClick={() => setSeverityFilter(new Set())}>
              Clear
            </Button>
          )}
        </Group>
        <Group gap={6} wrap="nowrap" style={{ flexShrink: 0 }}>
          <Group gap={0}>
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              leftSection={<IconPlus size={14} />}
              onClick={() => void onAddPolicy()}
              disabled={!capable || busy}
              style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
            >
              Add
            </Button>
            <Menu position="bottom-end" withinPortal>
              <Menu.Target>
                <Button
                  size={VIEW_ACTION_BUTTON_SIZE}
                  disabled={!capable || busy}
                  aria-label="Add policy or category"
                  px="xs"
                  style={{
                    borderTopLeftRadius: 0,
                    borderBottomLeftRadius: 0,
                    borderLeft: '1px solid var(--mantine-color-default-border)',
                  }}
                >
                  <IconChevronDown size={14} />
                </Button>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item onClick={() => void onAddPolicy()}>Policy</Menu.Item>
                <Menu.Item
                  onClick={() => {
                    setCatDisplayName('')
                    setCatSlug('')
                    setAddCategoryOpen(true)
                  }}
                >
                  Category
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            variant="light"
            color="red"
            onClick={() => onDeleteClick()}
            disabled={!capable || !nav || busy}
          >
            Delete
          </Button>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            onClick={() => void onSave()}
            disabled={!capable || nav?.kind !== 'policy' || !selectedPolicy || busy || !dirty}
          >
            Save
          </Button>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            variant="light"
            loading={busy}
            disabled={!capable}
            onClick={() => void onCheck()}
          >
            Check
          </Button>
          <Tooltip
            label={
              selectedPolicy
                ? context.kind === 'empty'
                  ? 'Open a graph or matcher (Matcher / All) first'
                  : 'Evaluate against current graph context'
                : 'Select a policy'
            }
          >
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              loading={busy}
              disabled={!capable || !selectedPolicy || context.kind === 'empty'}
              onClick={() => void onEvaluate()}
            >
              Evaluate
            </Button>
          </Tooltip>
          <Group gap={0}>
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              variant="light"
              disabled={!canvasNonEmpty || graphViewTab !== 'visual'}
              onClick={() => canvasRef.current?.applyLayout()}
              style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
            >
              Apply layout
            </Button>
            <Menu position="bottom-end" withinPortal>
              <Menu.Target>
                <Button
                  size={VIEW_ACTION_BUTTON_SIZE}
                  variant="light"
                  disabled={!canvasNonEmpty || graphViewTab !== 'visual'}
                  aria-label="Choose graph layout"
                  px="xs"
                  style={{
                    borderTopLeftRadius: 0,
                    borderBottomLeftRadius: 0,
                    borderLeft: '1px solid var(--mantine-color-default-border)',
                  }}
                >
                  <IconChevronDown size={14} />
                </Button>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Label>Layout direction</Menu.Label>
                {GRAPH_LAYOUTS.map((option) => (
                  <Menu.Item
                    key={option.value}
                    onClick={() => {
                      if (option.value === layout) {
                        canvasRef.current?.applyLayout()
                      } else {
                        changeLayout(option.value)
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
          {evalResult?.overall && (
            <Badge variant="light" color={severityBadgeColor(evalResult.overall)}>
              {evalResult.overall}
            </Badge>
          )}
        </Group>
      </Group>

      {capable === false && (
        <Alert color="orange" title="Policy service unavailable">
          `:objs-policy-service` is not on the classpath. Check and Evaluate are disabled.
        </Alert>
      )}
      {error && (
        <Alert color="red" title="Error" onClose={() => setError(null)} withCloseButton>
          {error}
        </Alert>
      )}

      <Group ref={splitHostRef} align="stretch" gap={0} wrap="nowrap" style={{ flex: 1, minHeight: 0, minWidth: 0 }}>
        <Paper
          withBorder
          p="xs"
          style={{
            width: leftWidth,
            flexShrink: 0,
            display: 'flex',
            flexDirection: 'column',
            minHeight: 0,
            overflow: 'hidden',
          }}
        >
          <Text size="sm" fw={600} mb="xs">
            Policies
          </Text>
          <TextInput
            size="xs"
            placeholder="Search name"
            value={filterName}
            onChange={(e) => setFilterName(e.currentTarget.value)}
            disabled={!capable}
            mb="xs"
          />
          <ScrollArea style={{ flex: 1, minHeight: 0 }}>
            <Stack gap={2}>
              {treeCategories.map(({ category: c, policies: kids }) => {
                const expanded = expandedCats.has(c.id)
                const catSelected = nav?.kind === 'category' && nav.id === c.id
                return (
                  <Stack key={c.id} gap={2}>
                    <Group
                      gap={4}
                      wrap="nowrap"
                      p={6}
                      style={{
                        borderRadius: 6,
                        cursor: 'pointer',
                        background: catSelected
                          ? 'color-mix(in srgb, var(--mantine-color-blue-filled) 18%, transparent)'
                          : undefined,
                      }}
                      onClick={() => selectCategory(c)}
                    >
                      <Button
                        size="compact-xs"
                        variant="subtle"
                        px={4}
                        onClick={(e) => {
                          e.stopPropagation()
                          toggleCatExpanded(c.id)
                        }}
                        aria-label={expanded ? 'Collapse' : 'Expand'}
                      >
                        {expanded ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />}
                      </Button>
                      <Stack gap={0} style={{ flex: 1, minWidth: 0 }}>
                        <Text size="sm" fw={600} truncate>
                          {c.displayName}
                        </Text>
                        <Text size="xs" c="dimmed" truncate>
                          {c.slug} · {kids.length}
                        </Text>
                      </Stack>
                    </Group>
                    {expanded &&
                      kids.map((p) => (
                        <Group
                          key={p.id}
                          gap={4}
                          wrap="nowrap"
                          p={6}
                          pl={28}
                          style={{
                            borderRadius: 6,
                            cursor: 'pointer',
                            background:
                              nav?.kind === 'policy' && nav.id === p.id
                                ? 'color-mix(in srgb, var(--mantine-color-blue-filled) 18%, transparent)'
                                : undefined,
                          }}
                          onClick={() => selectPolicy(p)}
                        >
                          <Stack gap={0} style={{ flex: 1, minWidth: 0 }}>
                            <Text size="sm" truncate>
                              {p.name}
                            </Text>
                            <Text size="xs" c="dimmed" truncate>
                              {formatPolicyVersion(p)}
                            </Text>
                          </Stack>
                        </Group>
                      ))}
                  </Stack>
                )
              })}
              {treeCategories.length === 0 && (
                <Text size="sm" c="dimmed">
                  No categories yet.
                </Text>
              )}
            </Stack>
          </ScrollArea>
        </Paper>

        <Box
          role="separator"
          aria-orientation="vertical"
          onPointerDown={dragLeft}
          style={{ width: SPLITTER, cursor: 'col-resize', flexShrink: 0 }}
        />

        <Stack gap={0} style={{ flex: 1, minWidth: 0, minHeight: 0, overflow: 'hidden' }}>
          <Group align="stretch" gap={0} wrap="nowrap" style={{ flex: 1, minHeight: 0, minWidth: 0, overflow: 'hidden' }}>
            <Paper
              withBorder
              p="xs"
              style={{
                flex: editorFrac,
                minWidth: 0,
                minHeight: 0,
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
              }}
            >
              {nav?.kind === 'category' && selectedCategory ? (
                <>
                  <Text size="sm" fw={600} mb={2}>
                    {selectedCategory.displayName}
                  </Text>
                  <Text size="xs" c="dimmed" mb="xs">
                    {selectedCategory.slug} · {policiesInSelectedCategory.length} polic
                    {policiesInSelectedCategory.length === 1 ? 'y' : 'ies'}
                  </Text>
                  <ScrollArea style={{ flex: 1, minHeight: 0 }}>
                    {policiesInSelectedCategory.length === 0 ? (
                      <Text size="sm" c="dimmed">
                        No policies in this category.
                      </Text>
                    ) : (
                      <Box
                        component="table"
                        style={{
                          width: '100%',
                          borderCollapse: 'collapse',
                          tableLayout: 'fixed',
                        }}
                      >
                        <Box component="thead">
                          <Box component="tr">
                            <Box
                              component="th"
                              style={{
                                textAlign: 'left',
                                fontWeight: 600,
                                fontSize: 'var(--mantine-font-size-xs)',
                                color: 'var(--mantine-color-dimmed)',
                                padding: '2px 6px 6px 0',
                                width: '32%',
                              }}
                            >
                              Name
                            </Box>
                            <Box
                              component="th"
                              style={{
                                textAlign: 'left',
                                fontWeight: 600,
                                fontSize: 'var(--mantine-font-size-xs)',
                                color: 'var(--mantine-color-dimmed)',
                                padding: '2px 6px 6px',
                                width: '18%',
                              }}
                            >
                              Version
                            </Box>
                            <Box
                              component="th"
                              style={{
                                textAlign: 'left',
                                fontWeight: 600,
                                fontSize: 'var(--mantine-font-size-xs)',
                                color: 'var(--mantine-color-dimmed)',
                                padding: '2px 0 6px 6px',
                              }}
                            >
                              Description
                            </Box>
                          </Box>
                        </Box>
                        <Box component="tbody">
                          {policiesInSelectedCategory.map((p) => (
                            <Box
                              component="tr"
                              key={p.id}
                              onClick={() => selectPolicy(p)}
                              style={{ cursor: 'pointer' }}
                              onMouseEnter={(e) => {
                                e.currentTarget.style.background =
                                  'color-mix(in srgb, var(--mantine-color-default-hover) 55%, transparent)'
                              }}
                              onMouseLeave={(e) => {
                                e.currentTarget.style.background = 'transparent'
                              }}
                            >
                              <Box
                                component="td"
                                style={{
                                  padding: '3px 6px 3px 0',
                                  verticalAlign: 'top',
                                  overflow: 'hidden',
                                }}
                              >
                                <Text size="sm" truncate>
                                  {p.name}
                                </Text>
                              </Box>
                              <Box
                                component="td"
                                style={{
                                  padding: '3px 6px',
                                  verticalAlign: 'top',
                                  overflow: 'hidden',
                                }}
                                title={formatPolicyVersion(p)}
                              >
                                <Text size="xs" c="dimmed" truncate>
                                  {p.version}
                                </Text>
                              </Box>
                              <Box
                                component="td"
                                style={{
                                  padding: '3px 0 3px 6px',
                                  verticalAlign: 'top',
                                  overflow: 'hidden',
                                }}
                                title={p.description || undefined}
                              >
                                <Text size="xs" c="dimmed" truncate>
                                  {p.description || '—'}
                                </Text>
                              </Box>
                            </Box>
                          ))}
                        </Box>
                      </Box>
                    )}
                  </ScrollArea>
                </>
              ) : nav?.kind === 'policy' && selectedPolicy ? (
                <>
                  <Text size="sm" fw={600} mb="xs">
                    Policy editor
                  </Text>
                  <Tabs
                    value={editorTab}
                    onChange={setEditorTab}
                    style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
                  >
                    <Tabs.List>
                      <Tabs.Tab value="general">General</Tabs.Tab>
                      <Tabs.Tab value="code">Code</Tabs.Tab>
                    </Tabs.List>
                    <Tabs.Panel value="general" pt="xs" style={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
                      <Stack gap="sm">
                        <Select
                          size="xs"
                          label="Category"
                          data={categories.map((c) => ({ value: c.id, label: c.displayName }))}
                          value={editorCategoryId}
                          onChange={(v) => {
                            setEditorCategoryId(v)
                            setDirty(true)
                          }}
                          disabled={!capable}
                        />
                        <TextInput
                          size="xs"
                          label="Name"
                          value={editorName}
                          onChange={(e) => {
                            setEditorName(e.currentTarget.value)
                            setDirty(true)
                          }}
                          disabled={!capable}
                        />
                        <Textarea
                          size="xs"
                          label="Description"
                          placeholder="Human-readable summary"
                          minRows={2}
                          autosize
                          maxRows={6}
                          value={editorDescription}
                          onChange={(e) => {
                            setEditorDescription(e.currentTarget.value)
                            setDirty(true)
                          }}
                          disabled={!capable}
                        />
                        <Group grow align="flex-end">
                          <TextInput
                            size="xs"
                            label="Version"
                            description="major.minor (e.g. 1.2)"
                            value={editorVersion}
                            onChange={(e) => {
                              setEditorVersion(e.currentTarget.value)
                              setDirty(true)
                            }}
                            disabled={!capable}
                          />
                          <Text size="xs" c="dimmed" pb={6}>
                            serial {selectedPolicy.serial}
                          </Text>
                        </Group>
                        <TagsInput
                          size="xs"
                          label="Tags"
                          value={editorTags}
                          onChange={(v) => {
                            setEditorTags(v)
                            setDirty(true)
                          }}
                          disabled={!capable}
                        />
                        <Text size="xs" fw={600}>
                          Annotations
                        </Text>
                        <KeyValueRowsEditor
                          rows={editorAnnoRows}
                          onChange={(rows) => {
                            setEditorAnnoRows(rows)
                            setDirty(true)
                          }}
                          disabled={!capable}
                        />
                      </Stack>
                    </Tabs.Panel>
                    <Tabs.Panel
                      value="code"
                      pt="xs"
                      style={{
                        flex: 1,
                        minHeight: 0,
                        height: 0,
                        overflow: 'hidden',
                        display: 'flex',
                        flexDirection: 'column',
                      }}
                    >
                      <PolicyDrlEditor
                        editorRef={editorRef}
                        policyKey={selectedId}
                        body={editorBody}
                        onDirty={markEditorDirty}
                        readOnly={!capable}
                      />
                    </Tabs.Panel>
                  </Tabs>
                </>
              ) : (
                <Text size="sm" c="dimmed">
                  Select a category or policy from the tree.
                </Text>
              )}
            </Paper>

            <Box
              role="separator"
              aria-orientation="vertical"
              onPointerDown={onEditorGraphSplit}
              style={{ width: SPLITTER, cursor: 'col-resize', flexShrink: 0 }}
            />

            <Paper
              withBorder
              p="xs"
              style={{
                flex: 1 - editorFrac,
                minWidth: 0,
                minHeight: 0,
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
              }}
            >
              <Tabs
                value={graphViewTab}
                onChange={setGraphViewTab}
                style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
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
                        This context has {canvasNodeTotal} nodes (cap {EXPLORER_NODE_CAP}). Use the
                        Data tab to browse objects and edges. Check and Evaluate still run against
                        the full fragment.
                      </Alert>
                    </Stack>
                  ) : annotatedGraph.nodes.length === 0 ? (
                    <Text size="sm" c="dimmed" p="md">
                      Open a graph or matcher (Matcher / All) in the shared context to preview
                      findings.
                    </Text>
                  ) : (
                    <GraphCanvas
                      ref={canvasRef}
                      nodes={annotatedGraph.nodes}
                      links={annotatedGraph.links}
                      selection={selection}
                      onSelect={setSelection}
                      onNodeContextMenu={onCanvasNodeContextMenu}
                      onEdgeContextMenu={onCanvasEdgeContextMenu}
                      layout={layout}
                      autoLayoutOnDataChange={false}
                    />
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
                      Open a graph or matcher (Matcher / All) in the shared context to browse
                      objects and edges.
                    </Text>
                  ) : (
                    <Stack gap="xs" style={{ flex: 1, minHeight: 0, overflow: 'hidden' }}>
                      <MultiSelect
                        size="xs"
                        label="Severity"
                        placeholder={
                          severitiesPresent.length === 0
                            ? 'Evaluate to annotate, or filter None'
                            : 'All severities'
                        }
                        data={dataSeverityOptions}
                        value={[...severityFilter]}
                        onChange={(vals) => setSeverityFilter(new Set(vals.map((v) => v.toUpperCase())))}
                        clearable
                        searchable={false}
                        comboboxProps={{ withinPortal: true }}
                        style={{ flexShrink: 0, maxWidth: 360 }}
                      />
                      <Tabs
                        value={structVeTab}
                        onChange={(v) => {
                          setStructVeTab(v)
                          setSelection(null)
                        }}
                        style={{
                          flex: 1,
                          minHeight: 0,
                          display: 'flex',
                          flexDirection: 'column',
                        }}
                      >
                      <Tabs.List style={{ flexShrink: 0, alignSelf: 'flex-start' }}>
                        <Tabs.Tab
                          value="vertices"
                          style={{ fontSize: 'var(--mantine-font-size-xs)' }}
                        >
                          Vertices ({annotatedVertexRows.length})
                        </Tabs.Tab>
                        <Tabs.Tab
                          value="edges"
                          style={{ fontSize: 'var(--mantine-font-size-xs)' }}
                        >
                          Edges ({annotatedEdgeRows.length})
                        </Tabs.Tab>
                      </Tabs.List>
                      <Tabs.Panel
                        value="vertices"
                        pt="xs"
                        style={{
                          flex: 1,
                          minHeight: 0,
                          display: 'flex',
                          flexDirection: 'column',
                        }}
                      >
                        <QueryResultGrid
                          rows={annotatedVertexRows}
                          rowKey={(r) => r.id}
                          selectedKey={
                            selection?.kind === 'node' ? selection.node.id : null
                          }
                          onRowSelect={(row) => selectFromDataNode(row.entity)}
                          empty={
                            <Text size="sm" c="dimmed">
                              {severityFilter.size > 0
                                ? 'No vertices match the severity filter.'
                                : 'No vertices in this context.'}
                            </Text>
                          }
                          columns={[
                            {
                              key: 'severity',
                              header: 'Severity',
                              width: '10ch',
                              render: (row) =>
                                row.findingSeverity ? (
                                  <Badge
                                    size="xs"
                                    color={severityBadgeColor(row.findingSeverity)}
                                  >
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
                                <IdLink
                                  id={row.id}
                                  onOpen={() => selectFromDataNode(row.entity)}
                                />
                              ),
                            },
                            {
                              key: 'type',
                              header: 'Type',
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
                        style={{
                          flex: 1,
                          minHeight: 0,
                          display: 'flex',
                          flexDirection: 'column',
                        }}
                      >
                        <QueryResultGrid
                          rows={annotatedEdgeRows}
                          rowKey={(r) => r.id}
                          selectedKey={
                            selection?.kind === 'edge' ? selection.edge.id : null
                          }
                          onRowSelect={(row) => selectFromDataEdge(row.edge, 0)}
                          empty={
                            <Text size="sm" c="dimmed">
                              {severityFilter.size > 0
                                ? 'No edges match the severity filter.'
                                : 'No edges in this context.'}
                            </Text>
                          }
                          columns={[
                            {
                              key: 'severity',
                              header: 'Severity',
                              width: '10ch',
                              render: (row) =>
                                row.findingSeverity ? (
                                  <Badge
                                    size="xs"
                                    color={severityBadgeColor(row.findingSeverity)}
                                  >
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
                                <IdLink
                                  id={row.id}
                                  onOpen={() => selectFromDataEdge(row.edge, 0)}
                                />
                              ),
                            },
                            {
                              key: 'type',
                              header: 'Type',
                              width: QUERY_STRUCT_TYPE_COL_WIDTH,
                              render: (row) => (
                                <Text size="xs" truncate title={row.type}>
                                  {row.type}
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
                              header: 'Role',
                              width: QUERY_STRUCT_EDGE_ROLE_COL_WIDTH,
                              render: (row) => (
                                <Text size="xs" truncate title={row.role}>
                                  {row.role}
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

            <Box
              role="separator"
              aria-orientation="vertical"
              onPointerDown={onRightSplit}
              style={{ width: SPLITTER, cursor: 'col-resize', flexShrink: 0 }}
            />

            <Paper
              withBorder
              style={{
                width: rightWidth,
                flexShrink: 0,
                minHeight: 0,
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
              }}
            >
              <Tabs value={inspectTab} onChange={setInspectTab} style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
                <Tabs.List>
                  <Tabs.Tab value="object">Object</Tabs.Tab>
                  <Tabs.Tab value="tasks">
                    <Text
                      span
                      fw={selectionTaskRows.length > 0 ? 800 : 500}
                      size="sm"
                    >
                      Tasks ({selectionTaskRows.length})
                    </Text>
                  </Tabs.Tab>
                </Tabs.List>
                <Tabs.Panel value="object" style={{ flex: 1, minHeight: 0, overflow: 'auto' }} p="xs">
                  <ObjectInspectPane
                    selection={selection}
                    nodes={inspectNodes}
                    graphContext={
                      context.kind === 'graph' && context.graphId
                        ? {
                            graphId: context.graphId,
                            graphVersion: context.graphVersion,
                            annotations: context.annotations ?? {},
                            entityCount:
                              fragmentContents?.entities?.length ?? inspectNodes.length,
                            edgeCount:
                              fragmentContents?.edges?.length ?? annotatedGraph.links.length,
                          }
                        : null
                    }
                    fieldKindsByTypeVersion={fieldKindsByTypeVersion}
                    onSelectNode={(nodeId) => {
                      const node = inspectNodes.find((n) => n.id === nodeId)
                      if (node) setSelection({ kind: 'node', node })
                    }}
                    onClearSelection={() => setSelection(null)}
                    endpointLabel={(nodeId) => {
                      const node = inspectNodes.find((n) => n.id === nodeId)
                      return node ? `${node.name} (${node.type})` : nodeId
                    }}
                  />
                </Tabs.Panel>
                <Tabs.Panel value="tasks" style={{ flex: 1, minHeight: 0, overflow: 'auto' }} p="xs">
                  {focusedTask?.kind === 'finding' ? (
                    <Stack gap="xs">
                      <Group gap="xs">
                        <Badge
                          size="sm"
                          color={severityBadgeColor(focusedTask.finding.severity ?? focusedTask.status)}
                        >
                          {focusedTask.finding.severity ?? focusedTask.status}
                        </Badge>
                        {findingRuleName(focusedTask.finding) && (
                          <Badge size="sm" variant="outline">
                            {findingRuleName(focusedTask.finding)}
                          </Badge>
                        )}
                      </Group>
                      <Text size="sm" fw={600}>
                        {focusedTask.policyName}
                      </Text>
                      <Text size="sm" style={{ wordBreak: 'break-word' }}>
                        {focusedTask.finding.message}
                      </Text>
                      {focusedTask.finding.code && <Code>{focusedTask.finding.code}</Code>}
                      <Text size="xs" c="dimmed">
                        entities: {(focusedTask.finding.entities ?? []).join(', ') || '—'}
                      </Text>
                      <Text size="xs" c="dimmed">
                        edges: {(focusedTask.finding.edges ?? []).join(', ') || '—'}
                      </Text>
                    </Stack>
                  ) : selectionTaskRows.length === 0 ? (
                    <Text size="sm" c="dimmed">
                      No tasks for the current selection.
                    </Text>
                  ) : (
                    <Stack gap={6}>
                      {selectionTaskRows.map((row) =>
                        row.kind === 'finding' ? (
                          <Paper
                            key={row.id}
                            withBorder
                            p="xs"
                            style={{ cursor: 'pointer' }}
                            onClick={() => onTaskClick(row)}
                          >
                            <Group gap={6} wrap="nowrap">
                              <Badge
                                size="xs"
                                color={severityBadgeColor(row.finding.severity ?? row.status)}
                              >
                                {row.finding.severity ?? row.status}
                              </Badge>
                              <Text size="xs" lineClamp={2} style={{ minWidth: 0 }}>
                                {row.finding.message}
                              </Text>
                            </Group>
                          </Paper>
                        ) : null,
                      )}
                    </Stack>
                  )}
                </Tabs.Panel>
              </Tabs>
            </Paper>
          </Group>

          <Box
            role="separator"
            aria-orientation="horizontal"
            onPointerDown={onTasksSplit}
            style={{ height: SPLITTER, cursor: 'row-resize', flexShrink: 0 }}
          />

          <Paper
            withBorder
            style={{
              height: tasksHeight,
              minHeight: tasksHeight,
              flexShrink: 0,
              flexGrow: 0,
              display: 'flex',
              flexDirection: 'column',
              overflow: 'hidden',
            }}
          >
            <Tabs value={tasksTab} onChange={setTasksTab} style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
              <Tabs.List>
                <Tabs.Tab value="policy">Policy</Tabs.Tab>
                <Tabs.Tab value="evaluations">Evaluations</Tabs.Tab>
              </Tabs.List>
              <Tabs.Panel value="policy" style={{ flex: 1, minHeight: 0, overflow: 'auto' }} p="xs">
                {checkRows.length === 0 ? (
                  <Text size="sm" c="dimmed">
                    Run Check to list compile/validation messages.
                  </Text>
                ) : (
                  <Stack gap={4}>
                    {checkRows.map((row) => (
                      <Text
                        key={row.id}
                        size="sm"
                        style={{ cursor: 'pointer', wordBreak: 'break-word' }}
                        c={checkResult?.ok ? undefined : 'red'}
                        onClick={() => {
                          setFocusedTaskId(row.id)
                          setInspectTab('tasks')
                          if (row.kind === 'check' && row.line != null) {
                            editorRef.current?.revealLine(row.line, row.column)
                          }
                        }}
                      >
                        {row.kind === 'check' ? row.message : row.finding.message}
                      </Text>
                    ))}
                  </Stack>
                )}
              </Tabs.Panel>
              <Tabs.Panel value="evaluations" style={{ flex: 1, minHeight: 0, overflow: 'auto' }} p="xs">
                {evalRows.length === 0 ? (
                  <Text size="sm" c="dimmed">
                    Run Evaluate to list outcomes and findings.
                  </Text>
                ) : (
                  <Stack gap={4}>
                    {evalRows.map((row) =>
                      row.kind === 'finding' ? (
                        <Group
                          key={row.id}
                          gap={8}
                          wrap="nowrap"
                          style={{ cursor: 'pointer' }}
                          onClick={() => onTaskClick(row)}
                        >
                          <Badge
                            size="xs"
                            color={severityBadgeColor(row.finding.severity ?? row.status)}
                          >
                            {row.finding.severity ?? row.status}
                          </Badge>
                          <Text size="sm" lineClamp={2} style={{ minWidth: 0 }}>
                            {row.policyName}: {row.finding.message}
                          </Text>
                        </Group>
                      ) : null,
                    )}
                  </Stack>
                )}
              </Tabs.Panel>
            </Tabs>
          </Paper>
        </Stack>
      </Group>

      <Modal
        opened={addCategoryOpen}
        onClose={() => setAddCategoryOpen(false)}
        title="Add category"
        centered
      >
        <Stack gap="sm">
          <TextInput
            label="Display name"
            value={catDisplayName}
            onChange={(e) => setCatDisplayName(e.currentTarget.value)}
            disabled={busy}
          />
          <TextInput
            label="Slug"
            description="Lowercase letters only"
            value={catSlug}
            onChange={(e) => setCatSlug(e.currentTarget.value)}
            disabled={busy}
          />
          <Group justify="flex-end" gap="xs">
            <Button variant="default" onClick={() => setAddCategoryOpen(false)} disabled={busy}>
              Cancel
            </Button>
            <Button onClick={() => void onCreateCategory()} disabled={busy}>
              Create
            </Button>
          </Group>
        </Stack>
      </Modal>

      <Modal
        opened={confirmOpen && confirmKind === 'discard'}
        onClose={() => {
          setConfirmOpen(false)
          setPendingNav(null)
        }}
        title="Discard changes?"
        centered
      >
        <Text size="sm" mb="md">
          You have unsaved edits. Discard them and continue?
        </Text>
        <Group justify="flex-end" gap="xs">
          <Button
            variant="default"
            onClick={() => {
              setConfirmOpen(false)
              setPendingNav(null)
            }}
          >
            Cancel
          </Button>
          <Button color="red" onClick={confirmDiscard}>
            Discard
          </Button>
        </Group>
      </Modal>

      <Modal
        opened={confirmOpen && confirmKind === 'delete-policy'}
        onClose={() => setConfirmOpen(false)}
        title="Delete policy?"
        centered
      >
        <Text size="sm" mb="md">
          Delete policy &quot;{editorName || selectedPolicy?.name}&quot;? This cannot be undone.
        </Text>
        <Group justify="flex-end" gap="xs">
          <Button variant="default" onClick={() => setConfirmOpen(false)} disabled={busy}>
            Cancel
          </Button>
          <Button color="red" onClick={() => void confirmDeletePolicy()} disabled={busy}>
            Delete
          </Button>
        </Group>
      </Modal>

      <Modal
        opened={confirmOpen && confirmKind === 'delete-category'}
        onClose={() => setConfirmOpen(false)}
        title="Delete category?"
        centered
      >
        <Text size="sm" mb="md">
          Delete category &quot;{selectedCategory?.displayName}&quot;? Categories with policies cannot
          be deleted.
        </Text>
        <Group justify="flex-end" gap="xs">
          <Button variant="default" onClick={() => setConfirmOpen(false)} disabled={busy}>
            Cancel
          </Button>
          <Button color="red" onClick={() => void confirmDeleteCategory()} disabled={busy}>
            Delete
          </Button>
        </Group>
      </Modal>
    </Stack>
  )
}
