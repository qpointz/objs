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

/** Matcher modes: `all` / `graph-expr` / `obj-expr` / chained. */
export type MatcherMode = 'all' | 'graph-expr' | 'obj-expr' | 'chained'

type ChainStageKind = 'all' | 'graph-expr' | 'obj-expr'
type ChainStage =
  | { kind: 'all' }
  | { kind: 'graph-expr'; expr: string }
  | { kind: 'obj-expr'; expr: string }

const SAMPLE_GRAPH_EXPR = "a.env == 'prod'"
const SAMPLE_OBJ_EXPR = "type == 'Product' && a.app == 'payments-api'"

const EMPTY_EXPR = ''

const MODE_OPTIONS = [
  { value: 'all', label: 'all' },
  { value: 'graph-expr', label: 'graph-expr' },
  { value: 'obj-expr', label: 'obj-expr' },
  { value: 'chained', label: 'chained' },
]

const STAGE_KIND_OPTIONS = [
  { value: 'all', label: 'all' },
  { value: 'graph-expr', label: 'graph-expr' },
  { value: 'obj-expr', label: 'obj-expr' },
]

const inputMono = { input: { fontFamily: 'var(--mantine-font-family-monospace)', fontSize: 12 } }

export type MatcherFormState = {
  mode: MatcherMode
  graphExprText: string
  objExprText: string
  chainStages: ChainStage[]
  chainedJson: string
  chainView: 'visual' | 'json'
}

function emptyStage(kind: ChainStageKind = 'graph-expr'): ChainStage {
  if (kind === 'all') return { kind: 'all' }
  if (kind === 'graph-expr') return { kind: 'graph-expr', expr: '' }
  return { kind: 'obj-expr', expr: '' }
}

function stageToMatcher(stage: ChainStage): unknown {
  if (stage.kind === 'all') return { all: true }
  const expression = stage.expr.trim()
  if (!expression) {
    throw new Error(`Provide a non-blank ${stage.kind} expression`)
  }
  return stage.kind === 'graph-expr' ? { 'graph-expr': expression } : { 'obj-expr': expression }
}

function matcherToStage(body: unknown): ChainStage | null {
  if (!body || typeof body !== 'object' || Array.isArray(body)) return null
  const obj = body as Record<string, unknown>
  if (obj.all === true) return { kind: 'all' }
  if (typeof obj['graph-expr'] === 'string') {
    return { kind: 'graph-expr', expr: obj['graph-expr'] }
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

export function matcherSummary(
  mode: MatcherMode,
  graphExprText: string,
  objExprText: string,
  chainStages: ChainStage[],
  chainedJson: string,
  chainView: 'visual' | 'json',
): string {
  if (mode === 'all') return 'all · every graph'
  if (mode === 'graph-expr') {
    const e = graphExprText.trim()
    return e ? `graph-expr · ${truncate(e)}` : 'graph-expr'
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

/** One-line matcher description for Explore-scope Selection summary. */
export function matcherBodyOneLiner(body: unknown | null | undefined): string {
  if (body == null) return 'no matcher'
  const hydrated = hydrateFromMatcher(body)
  if (!hydrated) {
    try {
      return truncate(JSON.stringify(body), 72)
    } catch {
      return 'matcher'
    }
  }
  return matcherSummary(
    hydrated.mode,
    hydrated.graphExprText,
    hydrated.objExprText,
    hydrated.chainStages,
    hydrated.chainedJson,
    hydrated.chainView,
  )
}

export function buildMatcherBody(
  mode: MatcherMode,
  graphExprText: string,
  objExprText: string,
  chainStages: ChainStage[],
  chainedJson: string,
  chainView: 'visual' | 'json',
): unknown {
  if (mode === 'all') {
    return { all: true }
  }
  if (mode === 'graph-expr') {
    const expression = graphExprText.trim()
    if (!expression) throw new Error('Provide a non-blank graph-expr expression')
    return { 'graph-expr': expression }
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
        graphExprText: EMPTY_EXPR,
        objExprText: EMPTY_EXPR,
        chainStages: [emptyStage()],
        chainedJson: JSON.stringify(body, null, 2),
        chainView: 'json',
      }
    }
    return {
      mode: 'chained',
      graphExprText: EMPTY_EXPR,
      objExprText: EMPTY_EXPR,
      chainStages: stages as ChainStage[],
      chainedJson: JSON.stringify(body, null, 2),
      chainView: 'visual',
    }
  }
  if (!body || typeof body !== 'object') return null
  const obj = body as Record<string, unknown>
  if (obj.all === true) {
    return {
      mode: 'all',
      graphExprText: EMPTY_EXPR,
      objExprText: EMPTY_EXPR,
      chainStages: [emptyStage()],
      chainedJson: '[]',
      chainView: 'visual',
    }
  }
  if (typeof obj['graph-expr'] === 'string') {
    return {
      mode: 'graph-expr',
      graphExprText: obj['graph-expr'],
      objExprText: EMPTY_EXPR,
      chainStages: [emptyStage()],
      chainedJson: '[]',
      chainView: 'visual',
    }
  }
  if (typeof obj['obj-expr'] === 'string') {
    return {
      mode: 'obj-expr',
      graphExprText: EMPTY_EXPR,
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
  getMode: () => MatcherMode
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
      defaultMode = 'obj-expr',
      collapsible = false,
      defaultCollapsed = false,
      collapseStorageKey,
    },
    ref,
  ) {
    const [mode, setMode] = useState<MatcherMode>(defaultMode)
    const [graphExprText, setGraphExprText] = useState(() =>
      emptyDefaults ? EMPTY_EXPR : SAMPLE_GRAPH_EXPR,
    )
    const [objExprText, setObjExprText] = useState(() =>
      emptyDefaults ? EMPTY_EXPR : SAMPLE_OBJ_EXPR,
    )
    const [chainStages, setChainStages] = useState<ChainStage[]>(() => [emptyStage('graph-expr')])
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
      setGraphExprText(hydrated.graphExprText)
      setObjExprText(hydrated.objExprText)
      setChainStages(hydrated.chainStages)
      setChainedJson(hydrated.chainedJson)
      setChainView(hydrated.chainView)
      setChainJsonError(null)
    }, [matcher])

    useImperativeHandle(
      ref,
      () => ({
        build: () => buildMatcherBody(mode, graphExprText, objExprText, chainStages, chainedJson, chainView),
        getMode: () => mode,
      }),
      [mode, graphExprText, objExprText, chainStages, chainedJson, chainView],
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
          setChainJsonError('JSON chain stages must be all / graph-expr / obj-expr objects')
          return
        }
        setChainStages(stages as ChainStage[])
        setChainJsonError(null)
        setChainView('visual')
      } catch {
        setChainJsonError('Chained matcher must be valid JSON')
      }
    }

    const summary = matcherSummary(mode, graphExprText, objExprText, chainStages, chainedJson, chainView)

    const editors = (
      <Stack gap={6}>
        {mode === 'all' && (
          <Text size="xs" c="dimmed">
            Selects every graph; returns the union of members and graph-local edges (distinct by id).
            Orphan pool entities are not included.
          </Text>
        )}

        {mode === 'graph-expr' && (
          <ExprEditor
            placeholder="graph-expr (id, a.* graph header annotations)"
            value={graphExprText}
            onChange={setGraphExprText}
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
                        w={120}
                        allowDeselect={false}
                        aria-label={`Stage ${index + 1} kind`}
                        data={STAGE_KIND_OPTIONS}
                        value={stage.kind}
                        onChange={(v) => {
                          const kind = (v as ChainStageKind) ?? 'graph-expr'
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
                    {stage.kind === 'all' && (
                      <Text size="xs" c="dimmed">
                        all graphs (union, distinct by id)
                      </Text>
                    )}
                    {stage.kind === 'graph-expr' && (
                      <ExprEditor
                        placeholder="graph-expr"
                        value={stage.expr}
                        onChange={(expr) => {
                          const next = [...chainStages]
                          next[index] = { kind: 'graph-expr', expr }
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
                    const next = [...chainStages, emptyStage('obj-expr')]
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
            w={130}
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
