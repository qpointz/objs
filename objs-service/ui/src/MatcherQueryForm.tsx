import {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useState,
  type ReactNode,
} from 'react'
import {
  ActionIcon,
  Alert,
  Button,
  Collapse,
  Group,
  Paper,
  Select,
  SegmentedControl,
  Stack,
  Text,
  Textarea,
  TextInput,
  Tooltip,
} from '@mantine/core'
import {
  IconArrowDown,
  IconArrowUp,
  IconChevronDown,
  IconChevronUp,
  IconPlus,
  IconTrash,
} from '@tabler/icons-react'
import { formatQueryExecStats, type QueryExecStats } from './queryExecStats'

export type MatcherMode = 'anno' | 'anno-expr' | 'obj-expr' | 'chained'
export type AnnoRow = { key: string; value: string }

type ChainStageKind = 'anno' | 'anno-expr' | 'obj-expr'
type ChainStage =
  | { kind: 'anno'; rows: AnnoRow[] }
  | { kind: 'anno-expr'; expr: string }
  | { kind: 'obj-expr'; expr: string }

const SAMPLE_ANNO_ROWS: AnnoRow[] = [
  { key: 'app', value: 'payments-api' },
  { key: 'appVersion', value: '2.3.1' },
]

const SAMPLE_EXPR = "app == 'payments-api' && appVersion == '2.3.1'"
const SAMPLE_OBJ_EXPR = "type == 'Product' && a.app == 'payments-api'"

const EMPTY_ANNO_ROWS: AnnoRow[] = [{ key: '', value: '' }]
const EMPTY_EXPR = ''

const MODE_OPTIONS = [
  { value: 'anno', label: 'anno' },
  { value: 'anno-expr', label: 'anno-expr' },
  { value: 'obj-expr', label: 'obj-expr' },
  { value: 'chained', label: 'chained' },
]

const STAGE_KIND_OPTIONS = [
  { value: 'anno', label: 'anno' },
  { value: 'anno-expr', label: 'anno-expr' },
  { value: 'obj-expr', label: 'obj-expr' },
]

const inputMono = { input: { fontFamily: 'var(--mantine-font-family-monospace)', fontSize: 12 } }

export type MatcherFormState = {
  mode: MatcherMode
  annoRows: AnnoRow[]
  exprText: string
  objExprText: string
  chainStages: ChainStage[]
  chainedJson: string
  chainView: 'visual' | 'json'
}

function emptyStage(kind: ChainStageKind = 'anno'): ChainStage {
  if (kind === 'anno') return { kind: 'anno', rows: [{ key: '', value: '' }] }
  if (kind === 'anno-expr') return { kind: 'anno-expr', expr: '' }
  return { kind: 'obj-expr', expr: '' }
}

function rowsToAnnoFilter(rows: AnnoRow[]): Record<string, string> {
  const filter: Record<string, string> = {}
  for (const row of rows) {
    const key = row.key.trim()
    if (!key) continue
    filter[key] = row.value
  }
  return filter
}

function stageToMatcher(stage: ChainStage): unknown {
  if (stage.kind === 'anno') {
    const filter = rowsToAnnoFilter(stage.rows)
    if (Object.keys(filter).length === 0) {
      throw new Error('Each anno stage needs at least one annotation key/value')
    }
    return { anno: filter }
  }
  const expression = stage.expr.trim()
  if (!expression) {
    throw new Error(`Provide a non-blank ${stage.kind} expression`)
  }
  return stage.kind === 'anno-expr' ? { 'anno-expr': expression } : { 'obj-expr': expression }
}

function matcherToStage(body: unknown): ChainStage | null {
  if (!body || typeof body !== 'object' || Array.isArray(body)) return null
  const obj = body as Record<string, unknown>
  if (obj.anno != null && typeof obj.anno === 'object' && !Array.isArray(obj.anno)) {
    const entries = Object.entries(obj.anno as Record<string, unknown>).map(([key, value]) => ({
      key,
      value: String(value ?? ''),
    }))
    return { kind: 'anno', rows: entries.length > 0 ? entries : [{ key: '', value: '' }] }
  }
  if (typeof obj['anno-expr'] === 'string') {
    return { kind: 'anno-expr', expr: obj['anno-expr'] }
  }
  if (typeof obj['obj-expr'] === 'string') {
    return { kind: 'obj-expr', expr: obj['obj-expr'] }
  }
  return null
}

function truncate(text: string, max = 56): string {
  const t = text.trim().replace(/\s+/g, ' ')
  if (t.length <= max) return t
  return `${t.slice(0, max - 1)}…`
}

function matcherSummary(
  mode: MatcherMode,
  annoRows: AnnoRow[],
  exprText: string,
  objExprText: string,
  chainStages: ChainStage[],
  chainedJson: string,
  chainView: 'visual' | 'json',
): string {
  if (mode === 'anno') {
    const n = Object.keys(rowsToAnnoFilter(annoRows)).length
    return n > 0 ? `anno · ${n} key${n === 1 ? '' : 's'}` : 'anno'
  }
  if (mode === 'anno-expr') {
    const e = exprText.trim()
    return e ? `anno-expr · ${truncate(e)}` : 'anno-expr'
  }
  if (mode === 'obj-expr') {
    const e = objExprText.trim()
    return e ? `obj-expr · ${truncate(e)}` : 'obj-expr'
  }
  if (chainView === 'json') {
    try {
      const parsed = JSON.parse(chainedJson) as unknown
      if (Array.isArray(parsed)) return `chained · ${parsed.length} stage${parsed.length === 1 ? '' : 's'} (JSON)`
    } catch {
      return 'chained · JSON'
    }
  }
  return `chained · ${chainStages.length} stage${chainStages.length === 1 ? '' : 's'}`
}

export function buildMatcherBody(
  mode: MatcherMode,
  annoRows: AnnoRow[],
  exprText: string,
  objExprText: string,
  chainStages: ChainStage[],
  chainedJson: string,
  chainView: 'visual' | 'json',
): unknown {
  if (mode === 'anno') {
    const filter = rowsToAnnoFilter(annoRows)
    if (Object.keys(filter).length === 0) {
      throw new Error('Provide at least one annotation key/value')
    }
    return { anno: filter }
  }
  if (mode === 'anno-expr') {
    const expression = exprText.trim()
    if (!expression) throw new Error('Provide a non-blank anno-expr expression')
    return { 'anno-expr': expression }
  }
  if (mode === 'obj-expr') {
    const expression = objExprText.trim()
    if (!expression) throw new Error('Provide a non-blank obj-expr expression')
    return { 'obj-expr': expression }
  }
  if (chainView === 'json') {
    let parsed: unknown
    try {
      parsed = JSON.parse(chainedJson)
    } catch {
      throw new Error('Chained matcher must be valid JSON')
    }
    if (!Array.isArray(parsed) || parsed.length === 0) {
      throw new Error('Chained matcher must be a non-empty JSON array')
    }
    return parsed
  }
  if (chainStages.length === 0) {
    throw new Error('Add at least one chain stage')
  }
  return chainStages.map(stageToMatcher)
}

/** Restore form fields from a matcher DSL body. */
export function hydrateFromMatcher(body: unknown): MatcherFormState | null {
  if (Array.isArray(body)) {
    const stages = body.map(matcherToStage)
    if (stages.some((s) => s == null)) {
      return {
        mode: 'chained',
        annoRows: EMPTY_ANNO_ROWS.map((row) => ({ ...row })),
        exprText: EMPTY_EXPR,
        objExprText: EMPTY_EXPR,
        chainStages: [emptyStage()],
        chainedJson: JSON.stringify(body, null, 2),
        chainView: 'json',
      }
    }
    return {
      mode: 'chained',
      annoRows: EMPTY_ANNO_ROWS.map((row) => ({ ...row })),
      exprText: EMPTY_EXPR,
      objExprText: EMPTY_EXPR,
      chainStages: stages as ChainStage[],
      chainedJson: JSON.stringify(body, null, 2),
      chainView: 'visual',
    }
  }
  if (!body || typeof body !== 'object') return null
  const obj = body as Record<string, unknown>
  if (obj.anno != null && typeof obj.anno === 'object' && !Array.isArray(obj.anno)) {
    const entries = Object.entries(obj.anno as Record<string, unknown>).map(([key, value]) => ({
      key,
      value: String(value ?? ''),
    }))
    return {
      mode: 'anno',
      annoRows: entries.length > 0 ? entries : [{ key: '', value: '' }],
      exprText: EMPTY_EXPR,
      objExprText: EMPTY_EXPR,
      chainStages: [emptyStage()],
      chainedJson: '[]',
      chainView: 'visual',
    }
  }
  if (typeof obj['anno-expr'] === 'string') {
    return {
      mode: 'anno-expr',
      annoRows: EMPTY_ANNO_ROWS.map((row) => ({ ...row })),
      exprText: obj['anno-expr'],
      objExprText: EMPTY_EXPR,
      chainStages: [emptyStage()],
      chainedJson: '[]',
      chainView: 'visual',
    }
  }
  if (typeof obj['obj-expr'] === 'string') {
    return {
      mode: 'obj-expr',
      annoRows: EMPTY_ANNO_ROWS.map((row) => ({ ...row })),
      exprText: EMPTY_EXPR,
      objExprText: obj['obj-expr'],
      chainStages: [emptyStage()],
      chainedJson: '[]',
      chainView: 'visual',
    }
  }
  return null
}

export type MatcherQueryFormHandle = {
  build: () => unknown
}

type MatcherQueryFormProps = {
  action?: ReactNode
  stats?: QueryExecStats | null
  error?: string | null
  matcher?: unknown | null
  emptyDefaults?: boolean
  /** Initial mode when not hydrating from matcher (Composer Add objects uses obj-expr). */
  defaultMode?: MatcherMode
  /**
   * Show expand/collapse control (Explorer and other permanently visible surfaces).
   * Collapsed keeps mode + action + a one-line summary.
   */
  collapsible?: boolean
  /** Initial collapsed state when [collapsible] (ignored if [collapseStorageKey] restores a value). */
  defaultCollapsed?: boolean
  /** Persist collapsed preference in localStorage. */
  collapseStorageKey?: string
}

function readCollapsed(key: string | undefined, fallback: boolean): boolean {
  if (!key || typeof window === 'undefined') return fallback
  try {
    const raw = window.localStorage.getItem(key)
    if (raw === '1') return true
    if (raw === '0') return false
  } catch {
    /* ignore */
  }
  return fallback
}

function writeCollapsed(key: string | undefined, collapsed: boolean) {
  if (!key || typeof window === 'undefined') return
  try {
    window.localStorage.setItem(key, collapsed ? '1' : '0')
  } catch {
    /* ignore */
  }
}

function AnnoRowsEditor({
  rows,
  onChange,
}: {
  rows: AnnoRow[]
  onChange: (rows: AnnoRow[]) => void
}) {
  return (
    <Stack gap={4}>
      {rows.map((row, index) => (
        <Group key={index} align="center" wrap="nowrap" gap={4}>
          <TextInput
            size="xs"
            placeholder="key"
            aria-label={index === 0 ? 'Annotation key' : undefined}
            value={row.key}
            onChange={(e) => {
              const next = [...rows]
              next[index] = { ...row, key: e.currentTarget.value }
              onChange(next)
            }}
            style={{ flex: 1, minWidth: 0 }}
          />
          <TextInput
            size="xs"
            placeholder="value"
            aria-label={index === 0 ? 'Annotation value' : undefined}
            value={row.value}
            onChange={(e) => {
              const next = [...rows]
              next[index] = { ...row, value: e.currentTarget.value }
              onChange(next)
            }}
            style={{ flex: 1, minWidth: 0 }}
          />
          <ActionIcon
            size="xs"
            variant="subtle"
            color="red"
            aria-label="Remove annotation row"
            onClick={() => onChange(rows.filter((_, i) => i !== index))}
            disabled={rows.length <= 1}
          >
            <IconTrash size={12} />
          </ActionIcon>
        </Group>
      ))}
      <Button
        variant="subtle"
        size="compact-xs"
        w="fit-content"
        px={4}
        leftSection={<IconPlus size={11} />}
        onClick={() => onChange([...rows, { key: '', value: '' }])}
      >
        Add key/value
      </Button>
    </Stack>
  )
}

function ExprEditor({
  placeholder,
  value,
  onChange,
}: {
  placeholder: string
  value: string
  onChange: (v: string) => void
}) {
  return (
    <Textarea
      size="xs"
      placeholder={placeholder}
      minRows={1}
      maxRows={3}
      autosize
      value={value}
      onChange={(e) => onChange(e.currentTarget.value)}
      styles={inputMono}
    />
  )
}

export const MatcherQueryForm = forwardRef<MatcherQueryFormHandle, MatcherQueryFormProps>(
  function MatcherQueryForm(
    {
      action,
      stats,
      error,
      matcher,
      emptyDefaults = false,
      defaultMode = 'anno',
      collapsible = false,
      defaultCollapsed = false,
      collapseStorageKey,
    },
    ref,
  ) {
    const [mode, setMode] = useState<MatcherMode>(defaultMode)
    const [annoRows, setAnnoRows] = useState<AnnoRow[]>(() =>
      emptyDefaults
        ? EMPTY_ANNO_ROWS.map((row) => ({ ...row }))
        : SAMPLE_ANNO_ROWS.map((row) => ({ ...row })),
    )
    const [exprText, setExprText] = useState(() => (emptyDefaults ? EMPTY_EXPR : SAMPLE_EXPR))
    const [objExprText, setObjExprText] = useState(() =>
      emptyDefaults ? EMPTY_EXPR : SAMPLE_OBJ_EXPR,
    )
    const [chainStages, setChainStages] = useState<ChainStage[]>(() => [emptyStage('anno')])
    const [chainedJson, setChainedJson] = useState('[]')
    const [chainView, setChainView] = useState<'visual' | 'json'>('visual')
    const [chainJsonError, setChainJsonError] = useState<string | null>(null)
    const [collapsed, setCollapsed] = useState(() =>
      collapsible ? readCollapsed(collapseStorageKey, defaultCollapsed) : false,
    )

    useEffect(() => {
      if (matcher === undefined || matcher === null) return
      const hydrated = hydrateFromMatcher(matcher)
      if (!hydrated) return
      setMode(hydrated.mode)
      setAnnoRows(hydrated.annoRows)
      setExprText(hydrated.exprText)
      setObjExprText(hydrated.objExprText)
      setChainStages(hydrated.chainStages)
      setChainedJson(hydrated.chainedJson)
      setChainView(hydrated.chainView)
      setChainJsonError(null)
    }, [matcher])

    useImperativeHandle(
      ref,
      () => ({
        build: () =>
          buildMatcherBody(mode, annoRows, exprText, objExprText, chainStages, chainedJson, chainView),
      }),
      [mode, annoRows, exprText, objExprText, chainStages, chainedJson, chainView],
    )

    function setCollapsedPersist(next: boolean) {
      setCollapsed(next)
      writeCollapsed(collapseStorageKey, next)
    }

    function syncJsonFromVisual(stages: ChainStage[]) {
      try {
        const body = stages.map(stageToMatcher)
        setChainedJson(JSON.stringify(body, null, 2))
        setChainJsonError(null)
      } catch {
        /* incomplete stages — leave JSON as-is until build */
      }
    }

    function switchChainView(next: 'visual' | 'json') {
      if (next === chainView) return
      if (next === 'json') {
        try {
          const body = chainStages.map(stageToMatcher)
          setChainedJson(JSON.stringify(body, null, 2))
          setChainJsonError(null)
          setChainView('json')
        } catch (e) {
          setChainJsonError(e instanceof Error ? e.message : String(e))
        }
        return
      }
      try {
        const parsed = JSON.parse(chainedJson) as unknown
        if (!Array.isArray(parsed) || parsed.length === 0) {
          setChainJsonError('Chained matcher must be a non-empty JSON array')
          return
        }
        const stages = parsed.map(matcherToStage)
        if (stages.some((s) => s == null)) {
          setChainJsonError('JSON chain stages must be anno / anno-expr / obj-expr objects')
          return
        }
        setChainStages(stages as ChainStage[])
        setChainJsonError(null)
        setChainView('visual')
      } catch {
        setChainJsonError('Chained matcher must be valid JSON')
      }
    }

    const summary = matcherSummary(
      mode,
      annoRows,
      exprText,
      objExprText,
      chainStages,
      chainedJson,
      chainView,
    )

    const editors = (
      <Stack gap={6}>
        {mode === 'anno' && <AnnoRowsEditor rows={annoRows} onChange={setAnnoRows} />}

        {mode === 'anno-expr' && (
          <ExprEditor
            placeholder="anno-expr (e.g. app == 'x')"
            value={exprText}
            onChange={setExprText}
          />
        )}

        {mode === 'obj-expr' && (
          <ExprEditor
            placeholder="obj-expr (id, type, schemaVersion, a.*, p.*)"
            value={objExprText}
            onChange={setObjExprText}
          />
        )}

        {mode === 'chained' && (
          <Stack gap={6}>
            <SegmentedControl
              size="xs"
              value={chainView}
              onChange={(v) => switchChainView(v as 'visual' | 'json')}
              data={[
                { label: 'Visual', value: 'visual' },
                { label: 'JSON', value: 'json' },
              ]}
              styles={{ root: { alignSelf: 'flex-start' } }}
            />
            {chainView === 'visual' ? (
              <Stack
                gap={4}
                style={{
                  maxHeight: 220,
                  overflowY: 'auto',
                  paddingRight: 2,
                }}
              >
                {chainStages.map((stage, index) => (
                  <Paper key={index} withBorder p={6} radius="xs">
                    <Group justify="space-between" mb={4} wrap="nowrap" gap={4}>
                      <Select
                        size="xs"
                        w={110}
                        allowDeselect={false}
                        aria-label={`Stage ${index + 1} kind`}
                        data={STAGE_KIND_OPTIONS}
                        value={stage.kind}
                        onChange={(v) => {
                          const kind = (v as ChainStageKind) ?? 'anno'
                          const next = [...chainStages]
                          next[index] = emptyStage(kind)
                          setChainStages(next)
                          syncJsonFromVisual(next)
                        }}
                      />
                      <Group gap={2} wrap="nowrap">
                        <ActionIcon
                          size="xs"
                          variant="subtle"
                          aria-label="Move stage up"
                          disabled={index === 0}
                          onClick={() => {
                            const next = [...chainStages]
                            ;[next[index - 1], next[index]] = [next[index], next[index - 1]]
                            setChainStages(next)
                            syncJsonFromVisual(next)
                          }}
                        >
                          <IconArrowUp size={12} />
                        </ActionIcon>
                        <ActionIcon
                          size="xs"
                          variant="subtle"
                          aria-label="Move stage down"
                          disabled={index === chainStages.length - 1}
                          onClick={() => {
                            const next = [...chainStages]
                            ;[next[index], next[index + 1]] = [next[index + 1], next[index]]
                            setChainStages(next)
                            syncJsonFromVisual(next)
                          }}
                        >
                          <IconArrowDown size={12} />
                        </ActionIcon>
                        <ActionIcon
                          size="xs"
                          variant="subtle"
                          color="red"
                          aria-label="Remove stage"
                          disabled={chainStages.length <= 1}
                          onClick={() => {
                            const next = chainStages.filter((_, i) => i !== index)
                            setChainStages(next)
                            syncJsonFromVisual(next)
                          }}
                        >
                          <IconTrash size={12} />
                        </ActionIcon>
                      </Group>
                    </Group>
                    {stage.kind === 'anno' && (
                      <AnnoRowsEditor
                        rows={stage.rows}
                        onChange={(rows) => {
                          const next = [...chainStages]
                          next[index] = { kind: 'anno', rows }
                          setChainStages(next)
                        }}
                      />
                    )}
                    {stage.kind === 'anno-expr' && (
                      <ExprEditor
                        placeholder="anno-expr"
                        value={stage.expr}
                        onChange={(expr) => {
                          const next = [...chainStages]
                          next[index] = { kind: 'anno-expr', expr }
                          setChainStages(next)
                        }}
                      />
                    )}
                    {stage.kind === 'obj-expr' && (
                      <ExprEditor
                        placeholder="obj-expr"
                        value={stage.expr}
                        onChange={(expr) => {
                          const next = [...chainStages]
                          next[index] = { kind: 'obj-expr', expr }
                          setChainStages(next)
                        }}
                      />
                    )}
                  </Paper>
                ))}
                <Button
                  variant="subtle"
                  size="compact-xs"
                  w="fit-content"
                  px={4}
                  leftSection={<IconPlus size={11} />}
                  onClick={() => {
                    const next = [...chainStages, emptyStage('anno')]
                    setChainStages(next)
                  }}
                >
                  Add stage
                </Button>
              </Stack>
            ) : (
              <Textarea
                size="xs"
                placeholder="[{…}, {…}]"
                minRows={3}
                maxRows={8}
                autosize
                value={chainedJson}
                onChange={(e) => {
                  setChainedJson(e.currentTarget.value)
                  setChainJsonError(null)
                }}
                styles={inputMono}
              />
            )}
            {chainJsonError && (
              <Alert color="red" p="xs" title="Chain editor">
                {chainJsonError}
              </Alert>
            )}
          </Stack>
        )}

        {error && (
          <Alert color="red" p="xs" title="Matcher error">
            {error}
          </Alert>
        )}
      </Stack>
    )

    return (
      <Stack gap={6}>
        <Group align="center" gap="xs" wrap="nowrap">
          {collapsible && (
            <Tooltip label={collapsed ? 'Expand matcher' : 'Collapse matcher'} withArrow>
              <ActionIcon
                size="sm"
                variant="subtle"
                aria-label={collapsed ? 'Expand matcher' : 'Collapse matcher'}
                aria-expanded={!collapsed}
                onClick={() => setCollapsedPersist(!collapsed)}
              >
                {collapsed ? <IconChevronDown size={16} /> : <IconChevronUp size={16} />}
              </ActionIcon>
            </Tooltip>
          )}
          <Text size="sm" fw={500} style={{ flexShrink: 0 }}>
            Matcher
          </Text>
          <Select
            size="xs"
            w={120}
            allowDeselect={false}
            aria-label="Matcher mode"
            data={MODE_OPTIONS}
            value={mode}
            onChange={(v) => setMode((v as MatcherMode) ?? defaultMode)}
          />
          {action}
          {stats != null && (
            <Text size="xs" c="dimmed" style={{ flexShrink: 0 }}>
              {formatQueryExecStats(stats)}
            </Text>
          )}
          {collapsible && collapsed && (
            <Text
              size="xs"
              c="dimmed"
              style={{
                flex: 1,
                minWidth: 0,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
              title={summary}
            >
              {summary}
            </Text>
          )}
        </Group>

        {collapsible ? (
          <Collapse in={!collapsed}>{editors}</Collapse>
        ) : (
          editors
        )}
      </Stack>
    )
  },
)
