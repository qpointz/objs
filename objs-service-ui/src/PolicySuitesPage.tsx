import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
} from 'react'
import {
  Alert,
  Badge,
  Box,
  Button,
  Checkbox,
  Group,
  Menu,
  Modal,
  Paper,
  ScrollArea,
  Select,
  Stack,
  Switch,
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
  createSuite,
  deleteSuite,
  evaluateSuite,
  fetchPolicyCapabilities,
  fetchSuiteSelection,
  listPolicies,
  listSuites,
  persistSuiteEvaluation,
  updateSuite,
} from './policyApi'
import type {
  Finding,
  Policy,
  PolicySuite,
  SuiteEvaluationResult,
  SuiteFolder,
  SuiteMatcher,
  SuiteSelectionResult,
} from './policyTypes'
import { formatPolicyVersion } from './policyTypes'
import { KeyValueRowsEditor, rowsToStringMap, stringMapToRows, type KeyValueRow } from './KeyValueRowsEditor'
import { ObjectInspectPane } from './ObjectInspectPane'
import { payloadFieldKindsByTypeVersion } from './payloadFieldKinds'
import { policyFragmentMatcher } from './queryGraphContext'
import {
  PolicyGraphOutputColumn,
  type PolicyGraphModel,
  type PolicyGraphOutputColumnHandle,
} from './PolicyGraphOutputColumn'
import { PolicyModeTabs, type PolicyWorkbenchMode } from './PolicyModeTabs'
import { clamp, maxSidePaneWidth } from './sidePaneSplit'
import {
  countSuiteTreeFindings,
  pruneSuiteEvalTree,
  SuiteEvaluationTree,
} from './SuiteEvaluationTree'
import type { BoMSchema, GraphSelection } from './types'
import { VIEW_ACTION_BUTTON_SIZE, VIEW_ACTION_VARIANT, VIEW_TITLE_PROPS } from './viewActionButtons'

const LEFT_WIDTH_KEY = 'objs.ui.policy.leftPaneWidth'
const EDITOR_FRAC_KEY = 'objs.ui.policy.editorFrac'
const SPLITTER = 8
const MIN_SIDE_ABS = 160

function sidePaneMin(hostWidth: number): number {
  return Math.max(MIN_SIDE_ABS, Math.floor(hostWidth / 8))
}

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
    localStorage.setItem(key, String(value))
  } catch {
    /* ignore */
  }
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

type NavSel =
  | { kind: 'suite'; id: string }
  | { kind: 'folder'; suiteId: string; folderId: string }

function newId(): string {
  return crypto.randomUUID()
}

function emptySuite(): PolicySuite {
  const rootId = newId()
  return {
    key: `suite-${Date.now()}`,
    name: 'New suite',
    rollUpStrategyKind: 'BUILTIN',
    executionStrategyKind: 'DEDUPE',
    folders: [
      {
        id: rootId,
        key: 'root',
        name: 'Root',
        participation: 'ENABLED',
        rollUpMode: 'ALL_PASS',
        matchers: [],
      },
    ],
    tags: [],
    annotations: {},
  }
}

function cloneSuite(s: PolicySuite): PolicySuite {
  return JSON.parse(JSON.stringify(s)) as PolicySuite
}

export function PolicySuitesPage({
  mode = 'suites',
  onModeChange,
}: {
  mode?: PolicyWorkbenchMode
  onModeChange?: (mode: PolicyWorkbenchMode) => void
}) {
  const { context } = useGraphContext()
  const [capable, setCapable] = useState<boolean | null>(null)
  const [archiveCapable, setArchiveCapable] = useState(false)
  const [suites, setSuites] = useState<PolicySuite[]>([])
  const [policies, setPolicies] = useState<Policy[]>([])
  const [nav, setNav] = useState<NavSel | null>(null)
  const [draft, setDraft] = useState<PolicySuite | null>(null)
  const [dirty, setDirty] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [expanded, setExpanded] = useState<Set<string>>(() => new Set())
  const [selection, setSelection] = useState<SuiteSelectionResult | null>(null)
  const [evalResult, setEvalResult] = useState<SuiteEvaluationResult | null>(null)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [persistOpen, setPersistOpen] = useState(false)
  const [persistName, setPersistName] = useState('')
  const [persistDescription, setPersistDescription] = useState('')
  const [persistTags, setPersistTags] = useState<string[]>([])
  const [persistAnnoRows, setPersistAnnoRows] = useState<KeyValueRow[]>([])
  const [persistAxisResults, setPersistAxisResults] = useState(true)
  const [persistAxisContext, setPersistAxisContext] = useState(true)
  const [persistAxisInput, setPersistAxisInput] = useState(false)
  const [persistSavedId, setPersistSavedId] = useState<string | null>(null)
  const [leftWidth, setLeftWidth] = useState(() => loadNum(LEFT_WIDTH_KEY, 240))
  const [editorFrac, setEditorFrac] = useState(() => {
    const v = loadNum(EDITOR_FRAC_KEY, 50)
    return Math.min(80, Math.max(20, v)) / 100
  })
  const [outputTab, setOutputTab] = useState<string | null>('selection')
  const [graphSelection, setGraphSelection] = useState<GraphSelection | null>(null)
  const [focusedFindingId, setFocusedFindingId] = useState<string | null>(null)
  const [graphModel, setGraphModel] = useState<PolicyGraphModel | null>(null)
  const [schemas, setSchemas] = useState<BoMSchema[]>([])
  const graphRef = useRef<PolicyGraphOutputColumnHandle>(null)
  const splitHostRef = useRef<HTMLDivElement>(null)

  const onGraphModel = useCallback((model: PolicyGraphModel) => {
    setGraphModel(model)
  }, [])

  const onGraphSelectionChange = useCallback((sel: GraphSelection | null, openObject = true) => {
    setGraphSelection(sel)
    if (sel != null && openObject) setOutputTab('object')
  }, [])

  const onFocusFinding = useCallback(
    (payload: { id: string; finding: Finding }) => {
      setFocusedFindingId(payload.id)
      const entityId = payload.finding.entities?.[0]
      const edgeId = payload.finding.edges?.[0]
      if (entityId) {
        const node = graphModel?.annotatedNodes.find((n) => n.id === entityId)
        if (node) {
          setGraphSelection({ kind: 'node', node })
          graphRef.current?.focusNode(node.id)
        }
      } else if (edgeId) {
        const edge = graphModel?.annotatedLinks.find((l) => l.id === edgeId)
        if (edge) setGraphSelection({ kind: 'edge', edge })
      }
    },
    [graphModel],
  )

  useEffect(() => {
    const el = splitHostRef.current
    if (!el) return
    const apply = () => {
      const hostW = el.clientWidth
      if (hostW < 200) return
      const min = sidePaneMin(hostW)
      setLeftWidth((w) => {
        const next = Math.max(w, min)
        if (next !== w) saveNum(LEFT_WIDTH_KEY, next)
        return next
      })
    }
    apply()
    const ro = new ResizeObserver(apply)
    ro.observe(el)
    return () => ro.disconnect()
  }, [])

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

  const refresh = useCallback(async () => {
    const caps = await fetchPolicyCapabilities()
    setCapable(caps != null && (caps.operations?.includes('suites') ?? false))
    setArchiveCapable(caps != null && (caps.operations?.includes('archive') ?? false))
    if (!caps) return
    const [s, p] = await Promise.all([listSuites(), listPolicies()])
    setSuites(s)
    setPolicies(p)
  }, [])

  useEffect(() => {
    void refresh().catch((e: unknown) =>
      setError(e instanceof Error ? e.message : String(e)),
    )
  }, [refresh])

  useEffect(() => {
    void listSchemas().then(setSchemas).catch(() => setSchemas([]))
  }, [])

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

  const selectedSuiteId =
    nav?.kind === 'suite' ? nav.id : nav?.kind === 'folder' ? nav.suiteId : null
  const selectedSuite =
    draft?.id && draft.id === selectedSuiteId
      ? draft
      : suites.find((s) => s.id === selectedSuiteId) ?? draft

  const selectedFolder =
    nav?.kind === 'folder' && selectedSuite
      ? selectedSuite.folders?.find((f) => f.id === nav.folderId) ?? null
      : null

  const prunedEvalTree = useMemo(() => {
    if (!evalResult?.tree) return null
    return pruneSuiteEvalTree(evalResult.tree, evalResult.outcomes, graphSelection)
  }, [evalResult, graphSelection])

  const filteredFindingCount = useMemo(
    () =>
      evalResult?.tree
        ? countSuiteTreeFindings(evalResult.tree, evalResult.outcomes, graphSelection)
        : 0,
    [evalResult, graphSelection],
  )

  const evalFiltered = graphSelection != null && (evalResult?.outcomes.length ?? 0) > 0

  function applyNav(next: NavSel | null, suiteOverride?: PolicySuite | null) {
    setNav(next)
    setSelection(null)
    setEvalResult(null)
    setGraphSelection(null)
    setFocusedFindingId(null)
    if (next?.kind === 'suite' || next?.kind === 'folder') {
      const sid = next.kind === 'suite' ? next.id : next.suiteId
      const s = suiteOverride ?? suites.find((x) => x.id === sid) ?? null
      setDraft(s ? cloneSuite(s) : null)
      setDirty(false)
      if (s?.id) setExpanded((prev) => new Set(prev).add(s.id!))
    } else {
      setDraft(null)
      setDirty(false)
    }
  }

  function markDirty(next: PolicySuite) {
    setDraft(next)
    setDirty(true)
  }

  async function onAddSuite() {
    setBusy(true)
    setError(null)
    try {
      const created = await createSuite(emptySuite())
      await refresh()
      applyNav({ kind: 'suite', id: created.id! }, created)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  function onAddFolder() {
    if (!draft?.id) return
    const root = draft.folders?.find((f) => !f.parentId)
    const parentId =
      nav?.kind === 'folder' ? nav.folderId : root?.id ?? null
    if (!parentId) return
    const id = newId()
    const folder: SuiteFolder = {
      id,
      key: `folder-${(draft.folders?.length ?? 0) + 1}`,
      name: 'New folder',
      parentId,
      participation: 'ENABLED',
      rollUpMode: 'ALL_PASS',
      matchers: [],
    }
    markDirty({ ...draft, folders: [...(draft.folders ?? []), folder] })
    setExpanded((prev) => new Set(prev).add(draft.id!).add(parentId))
    setNav({ kind: 'folder', suiteId: draft.id, folderId: id })
  }

  async function onSave() {
    if (!draft?.id) return
    setBusy(true)
    setError(null)
    try {
      const updated = await updateSuite(draft.id, draft)
      await refresh()
      applyNav(
        nav?.kind === 'folder'
          ? { kind: 'folder', suiteId: updated.id!, folderId: nav.folderId }
          : { kind: 'suite', id: updated.id! },
        updated,
      )
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  async function onDelete() {
    if (!draft?.id) return
    setBusy(true)
    setError(null)
    try {
      if (nav?.kind === 'folder') {
        const drop = new Set<string>([nav.folderId])
        let changed = true
        while (changed) {
          changed = false
          for (const f of draft.folders ?? []) {
            if (f.parentId && drop.has(f.parentId) && f.id && !drop.has(f.id)) {
              drop.add(f.id)
              changed = true
            }
          }
        }
        const nextFolders = (draft.folders ?? []).filter((f) => !f.id || !drop.has(f.id))
        if (!nextFolders.some((f) => !f.parentId)) {
          setError('Cannot delete the only root folder')
          return
        }
        markDirty({ ...draft, folders: nextFolders })
        setNav({ kind: 'suite', id: draft.id })
      } else {
        await deleteSuite(draft.id)
        await refresh()
        applyNav(null)
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setConfirmDelete(false)
      setBusy(false)
    }
  }

  async function onExamine() {
    if (!draft?.id) return
    setBusy(true)
    setError(null)
    try {
      // Persist draft first so server selection matches editor
      const saved = dirty ? await updateSuite(draft.id, draft) : draft
      if (dirty) {
        await refresh()
        setDraft(cloneSuite(saved))
        setDirty(false)
      }
      const scope = nav?.kind === 'folder' ? 'SUBFOLDER' : 'FULL'
      const result = await fetchSuiteSelection({
        suiteId: saved.id!,
        scope,
        folderId: nav?.kind === 'folder' ? nav.folderId : null,
      })
      setSelection(result)
      setOutputTab('selection')
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  async function onEvaluate() {
    if (!draft?.id) return
    if (context.kind === 'empty') {
      setError('Open a graph or matcher (Matcher / All) in the shared context before Evaluate')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const saved = dirty ? await updateSuite(draft.id, draft) : draft
      if (dirty) {
        await refresh()
        setDraft(cloneSuite(saved))
        setDirty(false)
      }
      const scope = policyFragmentMatcher(context)
      const result = await evaluateSuite({
        suiteId: saved.id!,
        ...scope,
        scope: nav?.kind === 'folder' ? 'SUBFOLDER' : 'FULL',
        folderId: nav?.kind === 'folder' ? nav.folderId : null,
      })
      setEvalResult(result)
      setPersistSavedId(null)
      setOutputTab('evaluation')
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  function openPersistDialog() {
    if (!evalResult) return
    setPersistName(draft?.name ? `${draft.name} run` : 'Suite evaluation')
    setPersistDescription('')
    setPersistTags([...(evalResult.meta.tags ?? [])])
    setPersistAnnoRows(stringMapToRows(evalResult.meta.annotations ?? {}))
    setPersistAxisResults(true)
    setPersistAxisContext(true)
    setPersistAxisInput(false)
    setPersistOpen(true)
  }

  async function onPersistSave() {
    if (!evalResult) return
    if (!persistAxisResults && !persistAxisContext && !persistAxisInput) {
      setError('Select at least one persist axis')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const scope = policyFragmentMatcher(context)
      const saved = await persistSuiteEvaluation({
        result: evalResult,
        name: persistName.trim() || null,
        description: persistDescription.trim() || null,
        tags: persistTags.map((t) => t.trim()).filter(Boolean),
        annotations: rowsToStringMap(persistAnnoRows),
        axes: {
          results: persistAxisResults,
          executionContext: persistAxisContext,
          input: persistAxisInput,
        },
        graphId: scope.graphId,
        graphVersion: scope.graphVersion,
        matcher: scope.matcher,
      })
      setPersistSavedId(saved.evaluationId)
      setPersistOpen(false)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const folderChildren = useMemo(() => {
    const map = new Map<string | null, SuiteFolder[]>()
    for (const f of draft?.folders ?? []) {
      const p = f.parentId ?? null
      const list = map.get(p) ?? []
      list.push(f)
      map.set(p, list)
    }
    for (const list of map.values()) {
      list.sort((a, b) => a.name.localeCompare(b.name))
    }
    return map
  }, [draft?.folders])

  function renderFolderTree(parentId: string | null, suiteId: string, depth: number): ReactNode {
    const kids = folderChildren.get(parentId) ?? []
    return kids.map((f) => {
      const fid = f.id!
      const hasKids = (folderChildren.get(fid) ?? []).length > 0
      const isOpen = expanded.has(fid)
      const selected = nav?.kind === 'folder' && nav.folderId === fid
      return (
        <Box key={fid}>
          <Group
            gap={4}
            wrap="nowrap"
            style={{
              paddingLeft: 8 + depth * 12,
              cursor: 'pointer',
              background: selected ? 'var(--mantine-color-default-hover)' : undefined,
              borderRadius: 4,
            }}
            onClick={() => applyNav({ kind: 'folder', suiteId, folderId: fid }, draft)}
          >
            <Box
              style={{ width: 16, flexShrink: 0 }}
              onClick={(e) => {
                e.stopPropagation()
                if (!hasKids) return
                setExpanded((prev) => {
                  const next = new Set(prev)
                  if (next.has(fid)) next.delete(fid)
                  else next.add(fid)
                  return next
                })
              }}
            >
              {hasKids ? (
                isOpen ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />
              ) : null}
            </Box>
            <Text size="xs" truncate style={{ flex: 1 }}>
              {f.name}
              <Text span size="xs" c="dimmed">
                {' '}
                ({f.participation ?? 'ENABLED'})
              </Text>
            </Text>
          </Group>
          {isOpen ? renderFolderTree(fid, suiteId, depth + 1) : null}
        </Box>
      )
    })
  }

  function updateSelectedFolder(patch: Partial<SuiteFolder>) {
    if (!draft || nav?.kind !== 'folder') return
    markDirty({
      ...draft,
      folders: (draft.folders ?? []).map((f) =>
        f.id === nav.folderId ? { ...f, ...patch } : f,
      ),
    })
  }

  function updateMatchers(matchers: SuiteMatcher[]) {
    updateSelectedFolder({ matchers })
  }

  if (capable === false) {
    return (
      <Alert color="yellow">
        `:objs-policy-service` suite APIs are not available. Suites is disabled.
      </Alert>
    )
  }

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0 }}>
      <Group align="center" wrap="nowrap" gap="sm" style={{ flexShrink: 0 }}>
        <Title {...VIEW_TITLE_PROPS}>Policy</Title>
        <Group
          justify="flex-end"
          align="center"
          wrap="wrap"
          gap="xs"
          style={{ flex: 1, minWidth: 0 }}
          data-tour="suite-view-actions"
        >
        <Box style={{ flex: 1, minWidth: 0 }} aria-hidden />
        <Group gap={6} wrap="nowrap" align="center">
          {dirty ? (
            <Text size="xs" c="dimmed" style={{ alignSelf: 'center' }}>
              unsaved
            </Text>
          ) : null}
          <Group gap={0}>
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              leftSection={<IconPlus size={14} />}
              onClick={() => void onAddSuite()}
              disabled={!capable || busy}
              style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
            >
              Add
            </Button>
            <Menu position="bottom-end" withinPortal>
              <Menu.Target>
                <Button
                  size={VIEW_ACTION_BUTTON_SIZE}
                  disabled={!capable || busy || !draft}
                  aria-label="Add suite or folder"
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
                <Menu.Item onClick={() => void onAddSuite()}>Suite</Menu.Item>
                <Menu.Item disabled={!draft} onClick={() => onAddFolder()}>
                  Folder
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            variant={VIEW_ACTION_VARIANT}
            color="red"
            disabled={!capable || !nav || busy}
            onClick={() => setConfirmDelete(true)}
          >
            Delete
          </Button>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            disabled={!capable || !draft?.id || !dirty || busy}
            onClick={() => void onSave()}
          >
            Save
          </Button>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            variant={VIEW_ACTION_VARIANT}
            disabled={!capable || !draft?.id || busy}
            onClick={() => void onExamine()}
          >
            Examine selection
          </Button>
          <Tooltip
            label={
              context.kind === 'empty'
                ? 'Open a graph or matcher (Matcher / All) first'
                : nav?.kind === 'folder'
                  ? 'Evaluate selected folder subtree'
                  : 'Evaluate suite against current graph context'
            }
          >
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              loading={busy}
              disabled={!capable || !draft?.id || context.kind === 'empty'}
              onClick={() => void onEvaluate()}
            >
              Evaluate
            </Button>
          </Tooltip>
          <Tooltip
            label={
              !evalResult
                ? 'Evaluate first'
                : !archiveCapable
                  ? 'Archive persistence not available on this server'
                  : 'Persist evaluation result'
            }
          >
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              variant={VIEW_ACTION_VARIANT}
              disabled={!capable || !archiveCapable || !evalResult || busy}
              onClick={() => openPersistDialog()}
            >
              Persist result
            </Button>
          </Tooltip>
        </Group>
        </Group>
      </Group>

      {error && (
        <Alert color="red" withCloseButton onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Group
        ref={splitHostRef}
        align="stretch"
        gap={0}
        wrap="nowrap"
        style={{ flex: 1, minHeight: 0, minWidth: 0 }}
      >
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
          <ScrollArea style={{ flex: 1, minHeight: 0 }} p={0}>
            {suites.length === 0 && !draft && (
              <Text size="xs" c="dimmed">
                No suites yet. Add a suite to begin.
              </Text>
            )}
            {suites.map((s) => {
              const sid = s.id!
              const open = expanded.has(sid)
              const selected = nav?.kind === 'suite' && nav.id === sid
              const showing = draft?.id === sid ? draft : s
              return (
                <Box key={sid} mb={4}>
                  <Group
                    gap={4}
                    wrap="nowrap"
                    style={{
                      cursor: 'pointer',
                      background: selected ? 'var(--mantine-color-default-hover)' : undefined,
                      borderRadius: 4,
                      paddingLeft: 4,
                    }}
                    onClick={() => {
                      applyNav({ kind: 'suite', id: sid }, showing)
                      setExpanded((prev) => new Set(prev).add(sid))
                    }}
                  >
                    <Box
                      style={{ width: 16 }}
                      onClick={(e) => {
                        e.stopPropagation()
                        setExpanded((prev) => {
                          const next = new Set(prev)
                          if (next.has(sid)) next.delete(sid)
                          else next.add(sid)
                          return next
                        })
                      }}
                    >
                      {open ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />}
                    </Box>
                    <Text size="xs" fw={600} truncate>
                      {showing.name}
                    </Text>
                  </Group>
                  {open && draft?.id === sid
                    ? renderFolderTree(null, sid, 1)
                    : open
                      ? (s.folders ?? [])
                          .filter((f) => !f.parentId)
                          .map((f) => (
                            <Text
                              key={f.id}
                              size="xs"
                              c="dimmed"
                              style={{ paddingLeft: 28, cursor: 'pointer' }}
                              onClick={() => applyNav({ kind: 'folder', suiteId: sid, folderId: f.id! }, s)}
                            >
                              {f.name}
                            </Text>
                          ))
                      : null}
                </Box>
              )
            })}
          </ScrollArea>
        </Paper>

        <Box
          role="separator"
          aria-orientation="vertical"
          onPointerDown={dragLeft}
          style={{ width: SPLITTER, cursor: 'col-resize', flexShrink: 0 }}
        />

        <Group
          align="stretch"
          gap={0}
          wrap="nowrap"
          style={{ flex: 1, minWidth: 0, minHeight: 0, overflow: 'hidden' }}
        >
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
          <ScrollArea style={{ flex: 1, minHeight: 0 }}>
            {!selectedSuite ? (
              <Text size="sm" c="dimmed" p="md">
                Select a suite or folder from the tree.
              </Text>
            ) : nav?.kind === 'suite' ? (
              <Stack gap="xs">
                <TextInput
                  size="xs"
                  label="Key"
                  value={draft?.key ?? ''}
                  onChange={(e) =>
                    draft && markDirty({ ...draft, key: e.currentTarget.value.trim().toLowerCase() })
                  }
                />
                <TextInput
                  size="xs"
                  label="Name"
                  value={draft?.name ?? ''}
                  onChange={(e) => draft && markDirty({ ...draft, name: e.currentTarget.value })}
                />
                <Select
                  size="xs"
                  label="Roll-up strategy (engine)"
                  data={[{ value: 'BUILTIN', label: 'BUILTIN' }]}
                  value={draft?.rollUpStrategyKind ?? 'BUILTIN'}
                  onChange={(v) =>
                    draft && markDirty({ ...draft, rollUpStrategyKind: v ?? 'BUILTIN' })
                  }
                />
                <Select
                  size="xs"
                  label="Execution strategy"
                  data={[
                    { value: 'DEDUPE', label: 'DEDUPE' },
                    { value: 'NO_DEDUPE', label: 'NO_DEDUPE' },
                  ]}
                  value={draft?.executionStrategyKind ?? 'DEDUPE'}
                  onChange={(v) =>
                    draft && markDirty({ ...draft, executionStrategyKind: v ?? 'DEDUPE' })
                  }
                />
              </Stack>
            ) : selectedFolder ? (
              <Stack gap="xs">
                <TextInput
                  size="xs"
                  label="Key"
                  value={selectedFolder.key}
                  onChange={(e) => updateSelectedFolder({ key: e.currentTarget.value })}
                />
                <TextInput
                  size="xs"
                  label="Name"
                  value={selectedFolder.name}
                  onChange={(e) => updateSelectedFolder({ name: e.currentTarget.value })}
                />
                <Select
                  size="xs"
                  label="Participation"
                  data={[
                    { value: 'ENABLED', label: 'ENABLED' },
                    { value: 'DISABLED', label: 'DISABLED' },
                    { value: 'IGNORED', label: 'IGNORED' },
                  ]}
                  value={(selectedFolder.participation as string) ?? 'ENABLED'}
                  onChange={(v) => updateSelectedFolder({ participation: v ?? 'ENABLED' })}
                />
                <Select
                  size="xs"
                  label="Roll-up mode"
                  data={[
                    { value: 'ALL_PASS', label: 'ALL_PASS' },
                    { value: 'ANY_PASS', label: 'ANY_PASS' },
                  ]}
                  value={selectedFolder.rollUpMode ?? 'ALL_PASS'}
                  onChange={(v) => updateSelectedFolder({ rollUpMode: v ?? 'ALL_PASS' })}
                />
                <TextInput
                  size="xs"
                  label="Severity config (optional)"
                  placeholder="e.g. HIGH"
                  value={selectedFolder.severityConfig ?? ''}
                  onChange={(e) =>
                    updateSelectedFolder({
                      severityConfig: e.currentTarget.value.trim() || null,
                    })
                  }
                />
                <Group justify="space-between" align="center">
                  <Text size="xs" fw={600}>
                    Matchers
                  </Text>
                  <Menu withinPortal>
                    <Menu.Target>
                      <Button size="compact-xs" variant="light">
                        Add matcher
                      </Button>
                    </Menu.Target>
                    <Menu.Dropdown>
                      <Menu.Item
                        onClick={() =>
                          updateMatchers([
                            ...(selectedFolder.matchers ?? []),
                            { kind: 'STATIC_LIST', mode: 'INCLUDE', entries: [] },
                          ])
                        }
                      >
                        STATIC_LIST
                      </Menu.Item>
                      <Menu.Item
                        onClick={() =>
                          updateMatchers([
                            ...(selectedFolder.matchers ?? []),
                            {
                              kind: 'METADATA',
                              mode: 'INCLUDE',
                              categoryKey: 'general',
                              tags: [],
                              annotations: {},
                            },
                          ])
                        }
                      >
                        METADATA
                      </Menu.Item>
                    </Menu.Dropdown>
                  </Menu>
                </Group>
                {(selectedFolder.matchers ?? []).map((m, idx) => (
                  <Paper key={idx} withBorder p="xs">
                    <Group justify="space-between" mb={4}>
                      <Text size="xs" fw={600}>
                        {m.kind}
                      </Text>
                      <Button
                        size="compact-xs"
                        variant="subtle"
                        color="red"
                        onClick={() =>
                          updateMatchers((selectedFolder.matchers ?? []).filter((_, i) => i !== idx))
                        }
                      >
                        Remove
                      </Button>
                    </Group>
                    <Select
                      size="xs"
                      label="Mode"
                      mb={4}
                      data={[
                        { value: 'INCLUDE', label: 'INCLUDE' },
                        { value: 'EXCLUDE', label: 'EXCLUDE' },
                      ]}
                      value={(m.mode as string) ?? 'INCLUDE'}
                      onChange={(v) => {
                        const next = [...(selectedFolder.matchers ?? [])]
                        next[idx] = { ...m, mode: v ?? 'INCLUDE' }
                        updateMatchers(next)
                      }}
                    />
                    {m.kind === 'STATIC_LIST' && (
                      <>
                        <Switch
                          size="xs"
                          label="Skip missing"
                          checked={!!m.skipMissing}
                          onChange={(e) => {
                            const next = [...(selectedFolder.matchers ?? [])]
                            next[idx] = { ...m, skipMissing: e.currentTarget.checked }
                            updateMatchers(next)
                          }}
                          mb={4}
                        />
                        <Select
                          size="xs"
                          label="Add policy"
                          searchable
                          clearable
                          data={policies.map((p) => ({
                            value: p.id,
                            label: `${p.name} (${formatPolicyVersion(p)})`,
                          }))}
                          value={null}
                          onChange={(pid) => {
                            if (!pid) return
                            const next = [...(selectedFolder.matchers ?? [])]
                            const entries = [...(m.entries ?? [])]
                            if (!entries.some((e) => e.policyId === pid)) {
                              entries.push({ policyId: pid })
                            }
                            next[idx] = { ...m, entries }
                            updateMatchers(next)
                          }}
                        />
                        <Stack gap={2} mt={4}>
                          {(m.entries ?? []).map((e) => {
                            const p = policies.find((x) => x.id === e.policyId)
                            return (
                              <Group key={e.policyId} justify="space-between" gap={4}>
                                <Text size="xs" truncate style={{ flex: 1 }}>
                                  {p ? `${p.name} · ${formatPolicyVersion(p)}` : e.policyId}
                                </Text>
                                <Button
                                  size="compact-xs"
                                  variant="subtle"
                                  color="red"
                                  onClick={() => {
                                    const next = [...(selectedFolder.matchers ?? [])]
                                    next[idx] = {
                                      ...m,
                                      entries: (m.entries ?? []).filter(
                                        (x) => x.policyId !== e.policyId,
                                      ),
                                    }
                                    updateMatchers(next)
                                  }}
                                >
                                  ×
                                </Button>
                              </Group>
                            )
                          })}
                        </Stack>
                      </>
                    )}
                    {m.kind === 'METADATA' && (
                      <>
                        <TextInput
                          size="xs"
                          label="Category key"
                          value={m.categoryKey ?? ''}
                          onChange={(e) => {
                            const next = [...(selectedFolder.matchers ?? [])]
                            next[idx] = {
                              ...m,
                              categoryKey: e.currentTarget.value.trim() || null,
                            }
                            updateMatchers(next)
                          }}
                          mb={4}
                        />
                        <TextInput
                          size="xs"
                          label="Tags (comma-separated)"
                          value={(m.tags ?? []).join(', ')}
                          onChange={(e) => {
                            const tags = e.currentTarget.value
                              .split(',')
                              .map((t) => t.trim())
                              .filter(Boolean)
                            const next = [...(selectedFolder.matchers ?? [])]
                            next[idx] = { ...m, tags }
                            updateMatchers(next)
                          }}
                        />
                      </>
                    )}
                  </Paper>
                ))}
              </Stack>
            ) : (
              <Text size="sm" c="dimmed" p="md">
                Select a folder to edit matchers.
              </Text>
            )}
          </ScrollArea>
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
            outcomes={evalResult?.outcomes}
            selection={graphSelection}
            onSelectionChange={onGraphSelectionChange}
            onGraphModel={onGraphModel}
            output={
              <Tabs
                value={outputTab}
                onChange={setOutputTab}
                style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
              >
                <Tabs.List>
                  <Tabs.Tab value="selection">Selection</Tabs.Tab>
                  <Tabs.Tab value="evaluation">
                    Evaluations
                    {evalFiltered ? ` (${filteredFindingCount})` : ''}
                  </Tabs.Tab>
                  <Tabs.Tab value="object">Object</Tabs.Tab>
                </Tabs.List>
                <Tabs.Panel value="selection" style={{ flex: 1, minHeight: 0, overflow: 'auto' }} p="xs">
                  {!selection ? (
                    <Text size="sm" c="dimmed">
                      Examine selection to list effective policies.
                    </Text>
                  ) : (
                    <Stack gap={4}>
                      <Text size="sm" fw={600}>
                        Effective policies ({selection.policies.length})
                      </Text>
                      {selection.policies.length === 0 && (
                        <Text size="sm" c="dimmed">
                          Empty selection.
                        </Text>
                      )}
                      {selection.policies.map((p) => (
                        <Text key={p.id} size="sm">
                          {p.name} · {formatPolicyVersion(p)}
                        </Text>
                      ))}
                    </Stack>
                  )}
                </Tabs.Panel>
                <Tabs.Panel value="evaluation" style={{ flex: 1, minHeight: 0, overflow: 'auto' }} p="xs">
                  {evalFiltered && (
                    <Group gap="xs" mb="xs" wrap="nowrap">
                      <Text size="xs" c="dimmed" style={{ flex: 1, minWidth: 0 }}>
                        Filtered to current graph selection
                      </Text>
                      <Button
                        size="compact-xs"
                        variant="subtle"
                        onClick={() => {
                          onGraphSelectionChange(null, false)
                          setFocusedFindingId(null)
                        }}
                      >
                        Reset filter
                      </Button>
                    </Group>
                  )}
                  {!evalResult ? (
                    <Text size="sm" c="dimmed">
                      Run Evaluate to see the suite result tree.
                    </Text>
                  ) : evalFiltered && prunedEvalTree == null ? (
                    <Text size="sm" c="dimmed">
                      No findings for the current selection.
                    </Text>
                  ) : (
                    <Stack gap="sm">
                      {persistSavedId && (
                        <Text size="xs" c="teal">
                          Archived: {persistSavedId}
                        </Text>
                      )}
                      <Group gap="xs" wrap="wrap">
                        <Badge
                          size="sm"
                          variant="light"
                          color={severityBadgeColor(
                            evalResult.meta.overallSeverity ?? evalResult.meta.overallStatus,
                          )}
                        >
                          {evalResult.meta.overallStatus ?? '—'}
                          {evalResult.meta.overallSeverity
                            ? ` @ ${evalResult.meta.overallSeverity}`
                            : ''}
                        </Badge>
                        <Text size="xs" c="dimmed">
                          {evalResult.meta.evaluationId}
                        </Text>
                      </Group>
                      {prunedEvalTree ? (
                        <SuiteEvaluationTree
                          node={prunedEvalTree}
                          depth={0}
                          outcomes={evalResult.outcomes}
                          graphSelection={graphSelection}
                          focusedFindingId={focusedFindingId}
                          onFocusFinding={onFocusFinding}
                        />
                      ) : (
                        <Text size="sm" c="dimmed">
                          Suite result has no folder tree.
                        </Text>
                      )}
                    </Stack>
                  )}
                </Tabs.Panel>
                <Tabs.Panel value="object" style={{ flex: 1, minHeight: 0, overflow: 'auto' }} p="xs">
                  <ObjectInspectPane
                    selection={graphSelection}
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
                      if (node) onGraphSelectionChange({ kind: 'node', node })
                    }}
                    onClearSelection={() => onGraphSelectionChange(null, false)}
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
        opened={confirmDelete}
        onClose={() => setConfirmDelete(false)}
        title={nav?.kind === 'folder' ? 'Delete folder?' : 'Delete suite?'}
        size="sm"
      >
        <Text size="sm" mb="md">
          {nav?.kind === 'folder'
            ? `Delete folder "${selectedFolder?.name}" and its descendants from the draft? Save to persist.`
            : `Delete suite "${draft?.name}"? This cannot be undone.`}
        </Text>
        <Group justify="flex-end">
          <Button size="xs" variant="default" onClick={() => setConfirmDelete(false)}>
            Cancel
          </Button>
          <Button size="xs" color="red" onClick={() => void onDelete()}>
            Delete
          </Button>
        </Group>
      </Modal>

      <Modal
        opened={persistOpen}
        onClose={() => setPersistOpen(false)}
        title="Persist evaluation result"
        size="md"
      >
        <Stack gap="sm">
          <Text size="xs" c="dimmed">
            evaluationId: {evalResult?.meta.evaluationId ?? '—'}
          </Text>
          <TextInput
            size="xs"
            label="Name"
            value={persistName}
            onChange={(e) => setPersistName(e.currentTarget.value)}
          />
          <Textarea
            size="xs"
            label="Description"
            minRows={2}
            value={persistDescription}
            onChange={(e) => setPersistDescription(e.currentTarget.value)}
          />
          <TagsInput
            size="xs"
            label="Tags"
            value={persistTags}
            onChange={setPersistTags}
            placeholder="Add tag"
          />
          <Box>
            <Text size="xs" fw={500} mb={4}>
              Annotations
            </Text>
            <KeyValueRowsEditor rows={persistAnnoRows} onChange={setPersistAnnoRows} />
          </Box>
          <Stack gap={6}>
            <Text size="xs" fw={500}>
              Persist axes
            </Text>
            <Checkbox
              size="xs"
              label="Results (outcomes, findings, suite tree)"
              checked={persistAxisResults}
              onChange={(e) => setPersistAxisResults(e.currentTarget.checked)}
            />
            <Checkbox
              size="xs"
              label="Execution context (policy/config snapshot at T₀)"
              checked={persistAxisContext}
              onChange={(e) => setPersistAxisContext(e.currentTarget.checked)}
            />
            <Checkbox
              size="xs"
              label="Input (frozen graph fragment)"
              checked={persistAxisInput}
              onChange={(e) => setPersistAxisInput(e.currentTarget.checked)}
              disabled={context.kind === 'empty'}
            />
          </Stack>
          <Group justify="flex-end" mt="xs">
            <Button size="xs" variant="default" onClick={() => setPersistOpen(false)}>
              Cancel
            </Button>
            <Button
              size="xs"
              loading={busy}
              disabled={
                busy || (!persistAxisResults && !persistAxisContext && !persistAxisInput)
              }
              onClick={() => void onPersistSave()}
            >
              Save
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}
