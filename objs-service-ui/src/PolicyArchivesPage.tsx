import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Accordion,
  Alert,
  Badge,
  Box,
  Button,
  Group,
  Modal,
  Paper,
  ScrollArea,
  Stack,
  Table,
  Tabs,
  Text,
  Title,
  Tooltip,
} from '@mantine/core'
import {
  deleteEvaluation,
  fetchPolicyCapabilities,
  listEvaluations,
  listPolicies,
  loadEvaluation,
} from './policyApi'
import {
  PolicyGraphOutputColumn,
  type PolicyGraphOutputColumnHandle,
} from './PolicyGraphOutputColumn'
import { PolicyModeTabs, type PolicyWorkbenchMode } from './PolicyModeTabs'
import {
  PolicyEvaluationTable,
  SuiteEvaluationTree,
} from './SuiteEvaluationTree'
import { SyntaxCodeEditor } from './SyntaxCodeEditor'
import type {
  EvaluationArchiveDocument,
  EvaluationArchiveSummary,
  Finding,
  PersistContentAxes,
  Policy,
  PolicyOutcome,
} from './policyTypes'
import type { GraphSelection } from './types'
import { VIEW_ACTION_BUTTON_SIZE, VIEW_ACTION_VARIANT, VIEW_TITLE_PROPS } from './viewActionButtons'
import { clamp, maxSidePaneWidth } from './sidePaneSplit'

const LEFT_WIDTH_KEY = 'objs.ui.policy.archives.leftPaneWidth'
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
    localStorage.setItem(key, String(Math.round(value)))
  } catch {
    /* ignore */
  }
}

function formatEvaluatedAt(epochMs: number): string {
  try {
    return new Date(epochMs).toLocaleString()
  } catch {
    return String(epochMs)
  }
}

function axisPresent(
  doc: EvaluationArchiveDocument,
  axis: 'results' | 'executionContext' | 'input',
): boolean {
  if (axis === 'results') {
    return (
      doc.axes.results &&
      ((doc.tree != null && doc.tree !== undefined) || (doc.outcomes?.length ?? 0) > 0)
    )
  }
  if (axis === 'executionContext') {
    return doc.axes.executionContext && doc.executionContext != null
  }
  return doc.axes.input && doc.input != null
}

function AxisChip({
  label,
  claimed,
  present,
}: {
  label: string
  claimed: boolean
  present: boolean
}) {
  if (!claimed) return null
  return (
    <Badge size="sm" variant={present ? 'light' : 'outline'} color={present ? 'teal' : 'gray'}>
      {label}
      {!present ? ' (empty)' : ''}
    </Badge>
  )
}

/** Single-letter claimed-axis pills for the archive list. */
function AxisLetterPills({ axes }: { axes: PersistContentAxes }) {
  const pills: { key: string; letter: string; tip: string }[] = []
  if (axes.results) pills.push({ key: 'r', letter: 'R', tip: 'Results' })
  if (axes.executionContext) pills.push({ key: 'c', letter: 'C', tip: 'Policies / execution context' })
  if (axes.input) pills.push({ key: 'i', letter: 'I', tip: 'Input fragment' })
  if (pills.length === 0) return null
  return (
    <Group gap={4} wrap="nowrap">
      {pills.map((p) => (
        <Tooltip key={p.key} label={p.tip} withArrow>
          <Badge size="xs" variant="light" color="gray" style={{ minWidth: 18, paddingInline: 4 }}>
            {p.letter}
          </Badge>
        </Tooltip>
      ))}
    </Group>
  )
}

function FindingDetailPanel({
  finding,
  findingId,
  outcome,
  onClear,
}: {
  finding: Finding
  findingId: string
  outcome?: PolicyOutcome | null
  onClear: () => void
}) {
  return (
    <Paper withBorder p="xs" mt="xs">
      <Group justify="space-between" mb={6} wrap="nowrap">
        <Text size="sm" fw={600}>
          Finding detail
        </Text>
        <Button size="compact-xs" variant="subtle" onClick={onClear}>
          Clear
        </Button>
      </Group>
      <Stack gap={6}>
        <Text size="sm" style={{ wordBreak: 'break-word' }}>
          {finding.message}
        </Text>
        <Group gap="xs" wrap="wrap">
          {finding.severity && (
            <Badge size="sm" variant="light">
              {finding.severity}
            </Badge>
          )}
          {finding.code && (
            <Badge size="sm" variant="outline">
              {finding.code}
            </Badge>
          )}
          {outcome && (
            <Text size="xs" c="dimmed">
              {outcome.policyName} · serial {outcome.policySerial} · {outcome.status}
            </Text>
          )}
          <Text size="xs" c="dimmed">
            id {findingId}
          </Text>
        </Group>
        {(finding.entities?.length ?? 0) > 0 && (
          <Box>
            <Text size="xs" c="dimmed" mb={2}>
              Entities
            </Text>
            <Text size="xs" ff="monospace" style={{ wordBreak: 'break-all' }}>
              {(finding.entities ?? []).join(', ')}
            </Text>
          </Box>
        )}
        {(finding.edges?.length ?? 0) > 0 && (
          <Box>
            <Text size="xs" c="dimmed" mb={2}>
              Edges
            </Text>
            <Text size="xs" ff="monospace" style={{ wordBreak: 'break-all' }}>
              {(finding.edges ?? []).join(', ')}
            </Text>
          </Box>
        )}
        {finding.extras != null && Object.keys(finding.extras).length > 0 && (
          <Stack gap={4}>
            <Text size="xs" c="dimmed">
              Extras
            </Text>
            <SyntaxCodeEditor
              language="json"
              value={JSON.stringify(finding.extras, null, 2)}
              readOnly
              fillHeight
              minHeight={100}
            />
          </Stack>
        )}
      </Stack>
    </Paper>
  )
}

function resolveFindingOutcome(
  findingId: string,
  outcomes: PolicyOutcome[],
): PolicyOutcome | null {
  // SuiteEvaluationTree / PolicyEvaluationTable ids: `f-{outcomeIndex}-{fi}-…`
  const m = /^f-(\d+)-/.exec(findingId)
  if (m) {
    const idx = Number(m[1])
    if (Number.isFinite(idx) && outcomes[idx]) return outcomes[idx]
  }
  return null
}

export function PolicyArchivesPage({
  mode = 'archives',
  onModeChange,
  initialArchiveId = null,
  onInitialArchiveConsumed,
}: {
  mode?: PolicyWorkbenchMode
  onModeChange?: (mode: PolicyWorkbenchMode) => void
  initialArchiveId?: string | null
  onInitialArchiveConsumed?: () => void
}) {
  const [archiveCapable, setArchiveCapable] = useState(false)
  const [capableLoaded, setCapableLoaded] = useState(false)
  const [items, setItems] = useState<EvaluationArchiveSummary[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [doc, setDoc] = useState<EvaluationArchiveDocument | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [inspectorTab, setInspectorTab] = useState<string | null>('results')
  const [inputSubTab, setInputSubTab] = useState<string | null>('graph')
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [leftWidth, setLeftWidth] = useState(() => loadNum(LEFT_WIDTH_KEY, 280))
  const [graphSelection, setGraphSelection] = useState<GraphSelection | null>(null)
  const [focusedFindingId, setFocusedFindingId] = useState<string | null>(null)
  const [focusedFinding, setFocusedFinding] = useState<Finding | null>(null)
  const [catalogPolicies, setCatalogPolicies] = useState<Policy[]>([])
  const splitHostRef = useRef<HTMLDivElement>(null)
  const graphRef = useRef<PolicyGraphOutputColumnHandle>(null)

  const refreshList = useCallback(async () => {
    if (!archiveCapable) {
      setItems([])
      return
    }
    setBusy(true)
    setError(null)
    try {
      const res = await listEvaluations({ limit: 100 })
      setItems(res.items ?? [])
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
      setItems([])
    } finally {
      setBusy(false)
    }
  }, [archiveCapable])

  useEffect(() => {
    let cancelled = false
    void (async () => {
      const caps = await fetchPolicyCapabilities()
      if (cancelled) return
      setArchiveCapable(caps != null && (caps.operations?.includes('archive') ?? false))
      setCapableLoaded(true)
      try {
        const pols = await listPolicies()
        if (!cancelled) setCatalogPolicies(pols)
      } catch {
        if (!cancelled) setCatalogPolicies([])
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (capableLoaded) void refreshList()
  }, [capableLoaded, refreshList])

  const openArchive = useCallback(async (id: string) => {
    setSelectedId(id)
    setBusy(true)
    setError(null)
    setGraphSelection(null)
    setFocusedFindingId(null)
    setFocusedFinding(null)
    try {
      const loaded = await loadEvaluation(id)
      setDoc(loaded)
      const first =
        axisPresent(loaded, 'results')
          ? 'results'
          : axisPresent(loaded, 'executionContext')
            ? 'policies'
            : axisPresent(loaded, 'input')
              ? 'input'
              : null
      setInspectorTab(first)
    } catch (e) {
      setDoc(null)
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }, [])

  useEffect(() => {
    if (!initialArchiveId) return
    void openArchive(initialArchiveId)
    onInitialArchiveConsumed?.()
  }, [initialArchiveId, openArchive, onInitialArchiveConsumed])

  useEffect(() => {
    const el = splitHostRef.current
    if (!el) return
    const apply = () => {
      const host = el.clientWidth
      const min = sidePaneMin(host)
      const max = maxSidePaneWidth(host, SPLITTER)
      setLeftWidth((w) => clamp(w, min, max))
    }
    apply()
    const ro = new ResizeObserver(apply)
    ro.observe(el)
    return () => ro.disconnect()
  }, [])

  const onSplitterPointerDown = (e: React.PointerEvent) => {
    e.preventDefault()
    const host = splitHostRef.current
    if (!host) return
    const startX = e.clientX
    const startW = leftWidth
    const onMove = (ev: PointerEvent) => {
      const hostW = host.clientWidth
      const min = sidePaneMin(hostW)
      const max = maxSidePaneWidth(hostW, SPLITTER)
      const next = clamp(startW + (ev.clientX - startX), min, max)
      setLeftWidth(next)
      saveNum(LEFT_WIDTH_KEY, next)
    }
    const onUp = () => {
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerup', onUp)
    }
    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
  }

  const onFocusFinding = useCallback((payload: { id: string; finding: Finding }) => {
    setFocusedFindingId(payload.id)
    setFocusedFinding(payload.finding)
    const entityId = payload.finding.entities?.[0]
    if (entityId) {
      setGraphSelection({
        kind: 'node',
        node: {
          id: entityId,
          name: entityId,
          type: '?',
          schemaVersion: '?',
          color: '#868e96',
          payload: {},
          annotations: {},
        },
      })
      graphRef.current?.focusNode(entityId)
    }
  }, [])

  const showResults = doc != null && axisPresent(doc, 'results')
  const showPolicies = doc != null && axisPresent(doc, 'executionContext')
  const showInput = doc != null && axisPresent(doc, 'input')

  const ctx = doc?.executionContext ?? null
  const ctxPolicies = Array.isArray(ctx?.policies) ? (ctx!.policies as Record<string, unknown>[]) : []
  const knownCtxKeys = new Set([
    'source',
    'kind',
    'suiteId',
    'suiteName',
    'executionStrategyKind',
    'rollUpStrategyKind',
    'policies',
  ])
  const extraCtx =
    ctx == null
      ? null
      : Object.fromEntries(Object.entries(ctx).filter(([k]) => !knownCtxKeys.has(k)))

  const focusedOutcome = useMemo(() => {
    if (!doc || !focusedFindingId) return null
    return resolveFindingOutcome(focusedFindingId, doc.outcomes)
  }, [doc, focusedFindingId])

  function catalogMatch(name: string, serial: number): Policy | null {
    const matches = catalogPolicies.filter(
      (p) =>
        (p.name === name || p.key === name) &&
        (serial === 0 || p.serial === serial),
    )
    if (matches.length === 0) {
      return catalogPolicies.find((p) => p.name === name || p.key === name) ?? null
    }
    return matches.sort((a, b) => b.serial - a.serial)[0] ?? null
  }

  async function onDeleteConfirmed() {
    if (!selectedId) return
    setBusy(true)
    setError(null)
    try {
      await deleteEvaluation(selectedId)
      setConfirmDelete(false)
      setDoc(null)
      setSelectedId(null)
      await refreshList()
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group justify="space-between" align="flex-start" wrap="nowrap">
        <Group gap="sm" align="center" wrap="nowrap" style={{ minWidth: 0 }}>
          <Title {...VIEW_TITLE_PROPS}>Policy</Title>
        </Group>
        <Group gap="xs" wrap="nowrap">
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            variant={VIEW_ACTION_VARIANT}
            disabled={!archiveCapable || busy}
            onClick={() => void refreshList()}
          >
            Refresh
          </Button>
          <Tooltip label={!selectedId ? 'Select an archive' : 'Delete this archive'}>
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              variant={VIEW_ACTION_VARIANT}
              color="red"
              disabled={!archiveCapable || !selectedId || busy}
              onClick={() => setConfirmDelete(true)}
            >
              Delete
            </Button>
          </Tooltip>
        </Group>
      </Group>

      {error && (
        <Alert color="red" withCloseButton onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {!capableLoaded ? null : !archiveCapable ? (
        <Alert color="yellow">
          Evaluation archives are not available on this server (archive capability missing).
        </Alert>
      ) : null}

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
          <ScrollArea style={{ flex: 1, minHeight: 0 }}>
            <Stack gap={2}>
              {items.length === 0 ? (
                <Text size="sm" c="dimmed">
                  {archiveCapable ? 'No archives yet.' : '—'}
                </Text>
              ) : (
                items.map((row) => {
                  const selected = selectedId === row.evaluationId
                  return (
                    <Box
                      key={row.evaluationId}
                      p={6}
                      style={{
                        borderRadius: 6,
                        cursor: 'pointer',
                        background: selected
                          ? 'color-mix(in srgb, var(--mantine-color-blue-filled) 18%, transparent)'
                          : undefined,
                      }}
                      onClick={() => void openArchive(row.evaluationId)}
                    >
                      <Group justify="space-between" gap={6} wrap="nowrap" mb={2}>
                        <Text size="sm" fw={500} lineClamp={1} style={{ flex: 1, minWidth: 0 }}>
                          {row.name?.trim() || row.evaluationId}
                        </Text>
                        <AxisLetterPills axes={row.axes} />
                      </Group>
                      <Text size="xs" c="dimmed" lineClamp={1}>
                        {row.kind}
                        {row.overallStatus ? ` · ${row.overallStatus}` : ''}
                        {` · ${formatEvaluatedAt(row.evaluatedAtEpochMs)}`}
                      </Text>
                    </Box>
                  )
                })
              )}
            </Stack>
          </ScrollArea>
        </Paper>

        <Box
          onPointerDown={onSplitterPointerDown}
          style={{
            width: SPLITTER,
            flexShrink: 0,
            cursor: 'col-resize',
            alignSelf: 'stretch',
          }}
        />

        <Paper
          withBorder
          p="xs"
          style={{
            flex: 1,
            minWidth: 0,
            minHeight: 0,
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
          }}
        >
          {!doc ? (
            <Text size="sm" c="dimmed" p="xs">
              Select an archive to inspect.
            </Text>
          ) : (
            <Stack gap="xs" style={{ flex: 1, minHeight: 0 }}>
              <Group gap="xs" wrap="wrap">
                <Text size="sm" fw={600}>
                  {doc.name?.trim() || doc.evaluationId}
                </Text>
                <Badge size="sm" variant="outline">
                  {doc.kind}
                </Badge>
                {doc.overall != null || doc.meta.overallStatus != null ? (
                  <Badge size="sm" variant="light">
                    {doc.overall ?? doc.meta.overallStatus}
                  </Badge>
                ) : null}
                {doc.presetName ? (
                  <Badge size="sm" variant="outline">
                    {doc.presetName}
                  </Badge>
                ) : null}
                <Text size="xs" c="dimmed">
                  {formatEvaluatedAt(doc.meta.evaluatedAtEpochMs)}
                </Text>
              </Group>
              <Group gap={6} wrap="wrap">
                <AxisChip
                  label="results"
                  claimed={doc.axes.results}
                  present={axisPresent(doc, 'results')}
                />
                <AxisChip
                  label="executionContext"
                  claimed={doc.axes.executionContext}
                  present={axisPresent(doc, 'executionContext')}
                />
                <AxisChip
                  label="input"
                  claimed={doc.axes.input}
                  present={axisPresent(doc, 'input')}
                />
              </Group>

              {!showResults && !showPolicies && !showInput ? (
                <Text size="sm" c="dimmed">
                  No durable axes in this pack.
                </Text>
              ) : (
                <Tabs
                  value={inspectorTab}
                  onChange={setInspectorTab}
                  style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
                >
                  <Tabs.List>
                    {showResults && <Tabs.Tab value="results">Results</Tabs.Tab>}
                    {showPolicies && <Tabs.Tab value="policies">Policies</Tabs.Tab>}
                    {showInput && <Tabs.Tab value="input">Input</Tabs.Tab>}
                  </Tabs.List>
                  {showResults && (
                    <Tabs.Panel
                      value="results"
                      style={{ flex: 1, minHeight: 0, overflow: 'auto' }}
                      pt="xs"
                    >
                      <Stack gap="xs">
                        {doc.tree != null ? (
                          <SuiteEvaluationTree
                            node={doc.tree}
                            outcomes={doc.outcomes}
                            graphSelection={graphSelection}
                            focusedFindingId={focusedFindingId}
                            onFocusFinding={onFocusFinding}
                            rootLabel={doc.meta.suiteName?.trim() || doc.name || 'Suite evaluation'}
                            rootStatus={doc.meta.overallStatus}
                            rootSeverity={doc.meta.overallSeverity}
                            durationMs={doc.durationMs}
                          />
                        ) : (
                          <PolicyEvaluationTable
                            result={{
                              outcomes: doc.outcomes,
                              overall: doc.overall ?? doc.meta.overallStatus,
                            }}
                            graphSelection={graphSelection}
                            focusedFindingId={focusedFindingId}
                            onFocusFinding={onFocusFinding}
                            rootLabel={doc.name ?? 'Flat evaluation'}
                            durationMs={doc.durationMs}
                          />
                        )}
                        {focusedFinding && focusedFindingId && (
                          <FindingDetailPanel
                            finding={focusedFinding}
                            findingId={focusedFindingId}
                            outcome={focusedOutcome}
                            onClear={() => {
                              setFocusedFinding(null)
                              setFocusedFindingId(null)
                            }}
                          />
                        )}
                      </Stack>
                    </Tabs.Panel>
                  )}
                  {showPolicies && (
                    <Tabs.Panel
                      value="policies"
                      style={{ flex: 1, minHeight: 0, overflow: 'auto' }}
                      pt="xs"
                    >
                      <Stack gap="sm">
                        <Alert color="gray" variant="light">
                          Archives store a thin execution-context snapshot (identity + status), not
                          full policy bodies. When the policy still exists in the catalog, its
                          current body is shown below for convenience — it may differ from T₀.
                        </Alert>
                        <Table withTableBorder withColumnBorders>
                          <Table.Tbody>
                            {(
                              [
                                ['source', ctx?.source],
                                ['kind', ctx?.kind],
                                ['suiteId', ctx?.suiteId],
                                ['suiteName', ctx?.suiteName],
                                ['executionStrategyKind', ctx?.executionStrategyKind],
                                ['rollUpStrategyKind', ctx?.rollUpStrategyKind],
                              ] as const
                            ).map(([k, v]) =>
                              v == null || v === '' ? null : (
                                <Table.Tr key={k}>
                                  <Table.Td>
                                    <Text size="xs" c="dimmed">
                                      {k}
                                    </Text>
                                  </Table.Td>
                                  <Table.Td>
                                    <Text size="sm">{String(v)}</Text>
                                  </Table.Td>
                                </Table.Tr>
                              ),
                            )}
                          </Table.Tbody>
                        </Table>
                        {ctxPolicies.length > 0 && (
                          <Accordion variant="separated" multiple>
                            {ctxPolicies.map((p, i) => {
                              const name = String(p.name ?? '')
                              const serial = Number(p.serial ?? 0)
                              const live = catalogMatch(name, serial)
                              const bodyFromSnap =
                                typeof p.body === 'string'
                                  ? p.body
                                  : typeof p.code === 'string'
                                    ? p.code
                                    : null
                              const body = bodyFromSnap ?? live?.body ?? null
                              return (
                                <Accordion.Item key={i} value={`p-${i}`}>
                                  <Accordion.Control>
                                    <Group gap="xs" wrap="wrap">
                                      <Text size="sm" fw={500}>
                                        {name || `policy ${i + 1}`}
                                      </Text>
                                      <Badge size="xs" variant="outline">
                                        serial {String(p.serial ?? '—')}
                                      </Badge>
                                      <Badge size="xs" variant="light">
                                        {String(p.engineKind ?? '—')}
                                      </Badge>
                                      <Badge size="xs" variant="light">
                                        {String(p.status ?? '—')}
                                      </Badge>
                                      {body ? (
                                        <Badge size="xs" color="teal" variant="light">
                                          {bodyFromSnap ? 'body in snapshot' : 'catalog body'}
                                        </Badge>
                                      ) : (
                                        <Badge size="xs" color="gray" variant="outline">
                                          no body
                                        </Badge>
                                      )}
                                    </Group>
                                  </Accordion.Control>
                                  <Accordion.Panel>
                                    {body ? (
                                      <SyntaxCodeEditor
                                        language={
                                          String(p.engineKind ?? live?.engineKind ?? '')
                                            .toUpperCase()
                                            .includes('DROOLS')
                                            ? 'drools'
                                            : 'json'
                                        }
                                        value={body}
                                        readOnly
                                        fillHeight
                                        minHeight={160}
                                      />
                                    ) : (
                                      <Text size="sm" c="dimmed">
                                        No policy body in this archive snapshot, and no matching
                                        catalog policy ({name}
                                        {serial ? ` @ serial ${serial}` : ''}). Enriching persist to
                                        freeze full T₀ bodies is a follow-up.
                                      </Text>
                                    )}
                                  </Accordion.Panel>
                                </Accordion.Item>
                              )
                            })}
                          </Accordion>
                        )}
                        {extraCtx != null && Object.keys(extraCtx).length > 0 && (
                          <Stack gap={4}>
                            <Text size="sm" fw={500}>
                              Raw extras
                            </Text>
                            <SyntaxCodeEditor
                              language="json"
                              value={JSON.stringify(extraCtx, null, 2)}
                              readOnly
                              fillHeight
                              minHeight={120}
                            />
                          </Stack>
                        )}
                        <Stack gap={4}>
                          <Text size="sm" fw={500}>
                            Full executionContext JSON
                          </Text>
                          <SyntaxCodeEditor
                            language="json"
                            value={JSON.stringify(ctx, null, 2)}
                            readOnly
                            fillHeight
                            minHeight={120}
                          />
                        </Stack>
                      </Stack>
                    </Tabs.Panel>
                  )}
                  {showInput && doc.input && (
                    <Tabs.Panel
                      value="input"
                      style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
                      pt="xs"
                    >
                      <Tabs
                        value={inputSubTab}
                        onChange={setInputSubTab}
                        style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
                      >
                        <Tabs.List>
                          <Tabs.Tab value="graph">Visual / Data</Tabs.Tab>
                          <Tabs.Tab value="raw">Raw</Tabs.Tab>
                        </Tabs.List>
                        <Tabs.Panel
                          value="graph"
                          style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
                          pt="xs"
                        >
                          <PolicyGraphOutputColumn
                            ref={graphRef}
                            frozenFragment={doc.input}
                            outcomes={doc.outcomes}
                            selection={graphSelection}
                            onSelectionChange={setGraphSelection}
                            hideOutput
                            output={null}
                          />
                        </Tabs.Panel>
                        <Tabs.Panel value="raw" style={{ flex: 1, minHeight: 0 }} pt="xs">
                          <SyntaxCodeEditor
                            language="json"
                            value={JSON.stringify(doc.input, null, 2)}
                            readOnly
                            fillHeight
                            minHeight={160}
                          />
                        </Tabs.Panel>
                      </Tabs>
                    </Tabs.Panel>
                  )}
                </Tabs>
              )}
            </Stack>
          )}
        </Paper>
      </Group>

      <Modal
        opened={confirmDelete}
        onClose={() => setConfirmDelete(false)}
        title="Delete archive"
        centered
      >
        <Stack gap="sm">
          <Text size="sm">
            Delete archive {selectedId}? This cannot be undone.
          </Text>
          <Group justify="flex-end" gap="xs">
            <Button size="xs" variant="default" onClick={() => setConfirmDelete(false)}>
              Cancel
            </Button>
            <Button size="xs" color="red" loading={busy} onClick={() => void onDeleteConfirmed()}>
              Delete
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}
