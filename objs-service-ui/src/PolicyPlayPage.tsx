import { useCallback, useEffect, useMemo, useRef, useState, memo, type PointerEvent as ReactPointerEvent } from 'react'
import {
  Alert,
  Box,
  Button,
  Group,
  Menu,
  Modal,
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
import { listSchemas } from './api'
import { useGraphContext } from './GraphContextProvider'
import {
  checkPolicy,
  createCategory,
  createPolicy,
  deleteCategory,
  deletePolicy,
  evaluatePolicy,
  exportPolicyCatalogSeeds,
  fetchPolicyCapabilities,
  listCategories,
  listPolicies,
  updatePolicy,
} from './policyApi'
import { ObjectInspectPane } from './ObjectInspectPane'
import { PolicyModeTabs, type PolicyWorkbenchMode } from './PolicyModeTabs'
import {
  formatPolicyVersion,
  type Category,
  type EvaluationResult,
  type Finding,
  type Policy,
  type PolicyCheckResult,
} from './policyTypes'
import { KeyValueRowsEditor, rowsToStringMap, stringMapToRows, type KeyValueRow } from './KeyValueRowsEditor'
import { payloadFieldKindsByTypeVersion } from './payloadFieldKinds'
import { policyFragmentMatcher } from './queryGraphContext'
import { formatQueryDuration, type QueryExecStats } from './queryExecStats'
import {
  PolicyGraphOutputColumn,
  type PolicyGraphModel,
  type PolicyGraphOutputColumnHandle,
} from './PolicyGraphOutputColumn'
import { PolicyEvaluationTable } from './SuiteEvaluationTree'
import { clamp, maxSidePaneWidth } from './sidePaneSplit'
import { SyntaxCodeEditor, type SyntaxCodeEditorHandle } from './SyntaxCodeEditor'
import type { BoMSchema, GraphSelection } from './types'
import { VIEW_ACTION_BUTTON_SIZE, VIEW_ACTION_VARIANT, VIEW_TITLE_PROPS } from './viewActionButtons'

const LEFT_WIDTH_KEY = 'objs.ui.policy.leftPaneWidth'
const EDITOR_FRAC_KEY = 'objs.ui.policy.editorFrac'
const SPLITTER = 8
/** Absolute floor; effective min is max(this, host/8). */
const MIN_SIDE_ABS = 160

function sidePaneMin(hostWidth: number): number {
  return Math.max(MIN_SIDE_ABS, Math.floor(hostWidth / 8))
}

type PolicyEvalStats = QueryExecStats & { findings: number }

function formatPolicyEvalStats(stats: PolicyEvalStats): string {
  return `${formatQueryDuration(stats.durationMs)} · ${stats.findings} finding${
    stats.findings === 1 ? '' : 's'
  } · ${stats.nodes} nodes · ${stats.edges} edges`
}

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
        finding: { message: o.message || o.status, severity: undefined },
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

export function PolicyPlayPage({
  mode = 'policies',
  onModeChange,
}: {
  mode?: PolicyWorkbenchMode
  onModeChange?: (mode: PolicyWorkbenchMode) => void
}) {
  const { context } = useGraphContext()
  const graphRef = useRef<PolicyGraphOutputColumnHandle>(null)
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
  const [filterName, setFilterName] = useState('')
  const [catName, setCatName] = useState('')
  const [catKey, setCatKey] = useState('')
  const [addCategoryOpen, setAddCategoryOpen] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [confirmKind, setConfirmKind] = useState<'discard' | 'delete-policy' | 'delete-category'>('discard')
  const [pendingNav, setPendingNav] = useState<NavSel | null>(null)
  const [dirty, setDirty] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [schemas, setSchemas] = useState<BoMSchema[]>([])
  const [selection, setSelection] = useState<GraphSelection | null>(null)
  const [graphModel, setGraphModel] = useState<PolicyGraphModel | null>(null)

  const [checkResult, setCheckResult] = useState<PolicyCheckResult | null>(null)
  const [evalResult, setEvalResult] = useState<EvaluationResult | null>(null)
  const [evalStats, setEvalStats] = useState<PolicyEvalStats | null>(null)
  const [tasksTab, setTasksTab] = useState<string | null>('policy')
  const [focusedTaskId, setFocusedTaskId] = useState<string | null>(null)

  const [leftWidth, setLeftWidth] = useState(() => loadNum(LEFT_WIDTH_KEY, 240))
  const [editorFrac, setEditorFrac] = useState(() => {
    const v = loadNum(EDITOR_FRAC_KEY, 50)
    return Math.min(80, Math.max(20, v)) / 100
  })

  const onGraphModel = useCallback((model: PolicyGraphModel) => {
    setGraphModel(model)
  }, [])

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
    return categories.map((c) => ({
      category: c,
      policies: policies.filter((p) => p.categoryId === c.id),
    }))
  }, [categories, policies])

  const markEditorDirty = useCallback(() => setDirty(true), [])

  const liveEditorBody = useCallback(
    () => editorRef.current?.getValue() ?? editorBody,
    [editorBody],
  )

  // Enforce ≥ 1/8 host for side panes (Note1): fix tiny first-open / stale localStorage.
  useEffect(() => {
    const el = splitHostRef.current
    if (!el) return
    const apply = () => {
      const hostW = el.clientWidth
      if (hostW >= 200) {
        const min = sidePaneMin(hostW)
        setLeftWidth((w) => {
          const next = Math.max(w, min)
          if (next !== w) saveNum(LEFT_WIDTH_KEY, next)
          return next
        })
      }
    }
    apply()
    const ro = new ResizeObserver(apply)
    ro.observe(el)
    return () => ro.disconnect()
  }, [])

  const refreshCategories = useCallback(async () => {
    const rows = await listCategories()
    setCategories(rows)
    return rows
  }, [])

  const refreshPolicies = useCallback(async () => {
    const rows = await listPolicies({
      key: filterName.trim() || null,
    })
    setPolicies(rows)
    return rows
  }, [filterName])

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
    void listSchemas().then(setSchemas).catch(() => setSchemas([]))
  }, [])

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

  const fieldKindsByTypeVersion = useMemo(
    () => payloadFieldKindsByTypeVersion(schemas),
    [schemas],
  )

  const fragmentContents = graphModel?.fragmentContents ?? null
  const inspectNodes = useMemo(() => {
    if (graphModel?.annotatedNodes && graphModel.annotatedNodes.length > 0) {
      return graphModel.annotatedNodes
    }
    return []
  }, [graphModel])

  const selectGraphSelection = useCallback((next: GraphSelection | null, openObject = true) => {
    setSelection(next)
    if (next != null && openObject) setTasksTab('object')
  }, [])

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

  const evalListRows = selection ? selectionTaskRows : evalRows

  async function onExportCatalog() {
    if (!capable) return
    setBusy(true)
    setError(null)
    try {
      const blob = await exportPolicyCatalogSeeds()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'policy-catalog-seeds.yaml'
      a.click()
      URL.revokeObjectURL(url)
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : String(ex))
    } finally {
      setBusy(false)
    }
  }

  async function onAddPolicy() {
    if (!capable) return
    const categoryId =
      (nav?.kind === 'category' ? nav.id : null) ??
      (nav?.kind === 'policy' ? selectedPolicy?.categoryId : null) ??
      categories[0]?.id ??
      null
    if (categoryId == null) {
      setError('Create a category first (Add → Category), then add a policy.')
      setCatName('')
      setCatKey('')
      setAddCategoryOpen(true)
      return
    }
    setBusy(true)
    setError(null)
    try {
      const created = await createPolicy({
        key: `policy-${policies.length + 1}`,
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
    const key = (selectedPolicy.key || name).trim().toLowerCase()
    if (!key) {
      setError('Policy key is required')
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
        key,
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
        nodes: fragmentContents?.entities?.length ?? graphModel?.nodes.length ?? 0,
        edges: fragmentContents?.edges?.length ?? graphModel?.links.length ?? 0,
      })
      setTasksTab('evaluations')
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
    const name = catName.trim()
    const key = catKey.trim().toLowerCase()
    if (!name || !key) {
      setError('Category name and key are required')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const created = await createCategory({ name, key })
      setCatName('')
      setCatKey('')
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
    setTasksTab('evaluations')
    if (row.kind !== 'finding') return
    const entityId = row.finding.entities?.[0]
    const edgeId = row.finding.edges?.[0]
    if (entityId) {
      const node = graphModel?.annotatedNodes.find((n) => n.id === entityId)
      if (node) {
        selectGraphSelection({ kind: 'node', node }, false)
        graphRef.current?.focusNode(node.id)
      }
    } else if (edgeId) {
      const edge = graphModel?.annotatedLinks.find((l) => l.id === edgeId)
      if (edge) selectGraphSelection({ kind: 'edge', edge }, false)
    }
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

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group align="center" wrap="nowrap" gap="sm" style={{ flexShrink: 0 }}>
        <Title {...VIEW_TITLE_PROPS}>Policy</Title>
        <Group
          justify="flex-end"
          align="center"
          wrap="wrap"
          gap="xs"
          style={{ flex: 1, minWidth: 0 }}
          data-tour="policy-view-actions"
        >
        <Box style={{ flex: 1, minWidth: 0 }} aria-hidden />
        <Group gap={6} wrap="nowrap" style={{ flexShrink: 0 }} align="center">
          <Text size="xs" c="dimmed" style={{ alignSelf: 'center' }}>
            {evalStats != null ? formatPolicyEvalStats(evalStats) : dirty ? 'unsaved' : '\u00a0'}
          </Text>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            variant={VIEW_ACTION_VARIANT}
            disabled={!capable || busy}
            onClick={() => void onExportCatalog()}
          >
            Export
          </Button>
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
                    setCatName('')
                    setCatKey('')
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
            variant={VIEW_ACTION_VARIANT}
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
            variant={VIEW_ACTION_VARIANT}
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
        </Group>
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
          {onModeChange && <PolicyModeTabs mode={mode} onModeChange={onModeChange} />}
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
                          {c.name}
                        </Text>
                        <Text size="xs" c="dimmed" truncate>
                          {c.key} · {kids.length}
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

        <Group align="stretch" gap={0} wrap="nowrap" style={{ flex: 1, minWidth: 0, minHeight: 0, overflow: 'hidden' }}>
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
                    {selectedCategory.name}
                  </Text>
                  <Text size="xs" c="dimmed" mb="xs">
                    {selectedCategory.key} · {policiesInSelectedCategory.length} polic
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
                          data={categories.map((c) => ({ value: c.id, label: c.name }))}
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

            <Box
              style={{
                flex: 1 - editorFrac,
                minWidth: 0,
                minHeight: 0,
                display: 'flex',
                overflow: 'hidden',
              }}
            >
              <PolicyGraphOutputColumn
                ref={graphRef}
                selection={selection}
                onSelectionChange={selectGraphSelection}
                outcomes={evalResult?.outcomes}
                onGraphModel={onGraphModel}
                output={
                  <Tabs
                    value={tasksTab}
                    onChange={setTasksTab}
                    style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
                  >
                    <Tabs.List>
                      <Tabs.Tab value="policy">Policy</Tabs.Tab>
                      <Tabs.Tab value="evaluations">
                        Evaluations
                        {selection ? ` (${selectionTaskRows.length})` : ''}
                      </Tabs.Tab>
                      <Tabs.Tab value="object">Object</Tabs.Tab>
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
                              fw={focusedTaskId === row.id ? 600 : undefined}
                              onClick={() => {
                                setFocusedTaskId(row.id)
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
                    <Tabs.Panel
                      value="evaluations"
                      style={{ flex: 1, minHeight: 0, overflow: 'auto' }}
                      p="xs"
                    >
                      {selection != null && evalRows.length > 0 && (
                        <Group gap="xs" mb="xs" wrap="nowrap">
                          <Text size="xs" c="dimmed" style={{ flex: 1, minWidth: 0 }}>
                            Filtered to current graph selection
                          </Text>
                          <Button
                            size="compact-xs"
                            variant="subtle"
                            onClick={() => {
                              selectGraphSelection(null, false)
                              setFocusedTaskId(null)
                            }}
                          >
                            Reset filter
                          </Button>
                        </Group>
                      )}
                      {evalListRows.length === 0 ? (
                        <Text size="sm" c="dimmed">
                          {evalRows.length === 0
                            ? 'Run Evaluate to list outcomes and findings.'
                            : selection
                              ? 'No findings for the current selection.'
                              : 'No findings.'}
                        </Text>
                      ) : (
                        <PolicyEvaluationTable
                          result={evalResult!}
                          graphSelection={selection}
                          focusedFindingId={focusedTaskId}
                          onFocusFinding={({ id, finding }) => {
                            const row = evalRows.find((r) => r.kind === 'finding' && r.id === id)
                            onTaskClick(
                              row ?? {
                                kind: 'finding',
                                id,
                                finding,
                                status: '',
                                policyName: '',
                              },
                            )
                          }}
                          rootLabel={
                            editorName.trim() ||
                            selectedPolicy?.name ||
                            'Policy evaluation'
                          }
                          durationMs={evalStats?.durationMs}
                        />
                      )}
                    </Tabs.Panel>
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
                                  fragmentContents?.edges?.length ??
                                  graphModel?.annotatedLinks.length ??
                                  0,
                              }
                            : null
                        }
                        fieldKindsByTypeVersion={fieldKindsByTypeVersion}
                        onSelectNode={(nodeId) => {
                          const node = inspectNodes.find((n) => n.id === nodeId)
                          if (node) selectGraphSelection({ kind: 'node', node })
                        }}
                        onClearSelection={() => selectGraphSelection(null, false)}
                        endpointLabel={(nodeId) => {
                          const node = inspectNodes.find((n) => n.id === nodeId)
                          return node ? `${node.name} (${node.type})` : nodeId
                        }}
                      />
                    </Tabs.Panel>
                  </Tabs>
                }
              />
            </Box>
        </Group>
      </Group>

      <Modal
        opened={addCategoryOpen}
        onClose={() => setAddCategoryOpen(false)}
        title="Add category"
        centered
      >
        <Stack gap="sm">
          <TextInput
            label="Name"
            value={catName}
            onChange={(e) => setCatName(e.currentTarget.value)}
            disabled={busy}
          />
          <TextInput
            label="Key"
            description="Lowercase letters only"
            value={catKey}
            onChange={(e) => setCatKey(e.currentTarget.value)}
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
          Delete category &quot;{selectedCategory?.name}&quot;? Categories with policies cannot
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
