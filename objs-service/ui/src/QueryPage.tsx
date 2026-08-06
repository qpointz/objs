import { useCallback, useEffect, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import {
  Alert,
  Box,
  Button,
  Code,
  Group,
  NumberInput,
  Paper,
  ScrollArea,
  Stack,
  Table,
  Tabs,
  Text,
  Title,
} from '@mantine/core'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  type BoMGremlinResult,
  toGraphData,
  traverseGremlin,
} from './api'
import { GraphCanvas, type GraphCanvasHandle } from './GraphCanvas'
import {
  MatcherQueryForm,
  type MatcherQueryFormHandle,
} from './MatcherQueryForm'
import { formatQueryDuration } from './queryExecStats'
import { SyntaxCodeEditor } from './SyntaxCodeEditor'
import type { GraphLink, GraphNode } from './types'

const SCRIPT_STORAGE_KEY = 'objs.ui.query.script'
const MATCHER_STORAGE_KEY = 'objs.ui.query.matcher'
const OPTIONS_STORAGE_KEY = 'objs.ui.query.options'
const TOP_PANE_HEIGHT_KEY = 'objs.ui.query.topPaneHeight'

const DEFAULT_SCRIPT = `g.V().hasLabel('Service', 'Policy').project('type', 'name', 'protocol')
  .by(label)
  .by(values('payload').select('name'))
  .by(coalesce(values('payload').select('protocol'), constant('')))`

const DEFAULT_TIMEOUT_SECONDS = 60
const DEFAULT_TOP_PANE_HEIGHT = 280
const MIN_TOP_PANE_HEIGHT = 160
const MIN_BOTTOM_PANE_HEIGHT = 160
const SPLITTER_HEIGHT = 8

type QueryNavState = {
  matcher?: unknown
}

type QueryOptions = {
  timeoutSeconds: number
}

function loadStoredScript(): string {
  try {
    const raw = localStorage.getItem(SCRIPT_STORAGE_KEY)
    if (raw != null && raw.length > 0) return raw
  } catch {
    // ignore
  }
  return DEFAULT_SCRIPT
}

function saveStoredScript(script: string) {
  try {
    localStorage.setItem(SCRIPT_STORAGE_KEY, script)
  } catch {
    // ignore
  }
}

function loadStoredMatcher(): unknown | null {
  try {
    const raw = localStorage.getItem(MATCHER_STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw) as unknown
  } catch {
    return null
  }
}

function saveStoredMatcher(body: unknown) {
  try {
    localStorage.setItem(MATCHER_STORAGE_KEY, JSON.stringify(body))
  } catch {
    // ignore
  }
}

function loadStoredOptions(): QueryOptions {
  try {
    const raw = localStorage.getItem(OPTIONS_STORAGE_KEY)
    if (!raw) return { timeoutSeconds: DEFAULT_TIMEOUT_SECONDS }
    const parsed = JSON.parse(raw) as Partial<QueryOptions>
    const timeout =
      typeof parsed.timeoutSeconds === 'number' && parsed.timeoutSeconds > 0
        ? parsed.timeoutSeconds
        : DEFAULT_TIMEOUT_SECONDS
    return { timeoutSeconds: timeout }
  } catch {
    return { timeoutSeconds: DEFAULT_TIMEOUT_SECONDS }
  }
}

function saveStoredOptions(options: QueryOptions) {
  try {
    localStorage.setItem(OPTIONS_STORAGE_KEY, JSON.stringify(options))
  } catch {
    // ignore
  }
}

function loadTopPaneHeight(): number {
  try {
    const raw = localStorage.getItem(TOP_PANE_HEIGHT_KEY)
    if (!raw) return DEFAULT_TOP_PANE_HEIGHT
    const n = Number(raw)
    return Number.isFinite(n) && n >= MIN_TOP_PANE_HEIGHT ? n : DEFAULT_TOP_PANE_HEIGHT
  } catch {
    return DEFAULT_TOP_PANE_HEIGHT
  }
}

function saveTopPaneHeight(height: number) {
  try {
    localStorage.setItem(TOP_PANE_HEIGHT_KEY, String(height))
  } catch {
    // ignore
  }
}

function formatGremlinStats(result: BoMGremlinResult): string {
  const s1 = result.meta.subgraph1Stats
  const s2 = result.meta.subgraph2Stats
  const parts = [
    formatQueryDuration(result.meta.durationMs),
    `${result.meta.resultCount} results`,
    `sg1 ${s1.entities}/${s1.edges}`,
  ]
  if (s2 != null) {
    parts.push(`sg2 ${s2.entities}/${s2.edges}`)
  }
  return parts.join(' · ')
}

function clamp(n: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, n))
}

export function QueryPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const matcherRef = useRef<MatcherQueryFormHandle>(null)
  const graphRef = useRef<GraphCanvasHandle>(null)
  const splitHostRef = useRef<HTMLDivElement>(null)
  const dragRef = useRef<{ startY: number; startHeight: number } | null>(null)

  const [script, setScript] = useState(loadStoredScript)
  const [matcher, setMatcher] = useState<unknown | null>(() => loadStoredMatcher())
  const [options, setOptions] = useState<QueryOptions>(loadStoredOptions)
  const [topPaneHeight, setTopPaneHeight] = useState(loadTopPaneHeight)
  const [topTab, setTopTab] = useState<string | null>('query')
  const [resultTab, setResultTab] = useState<string | null>('structured')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [matcherError, setMatcherError] = useState<string | null>(null)
  const [result, setResult] = useState<BoMGremlinResult | null>(null)
  const [nodes, setNodes] = useState<GraphNode[]>([])
  const [links, setLinks] = useState<GraphLink[]>([])

  useEffect(() => {
    const navState = location.state as QueryNavState | null
    if (navState == null || typeof navState !== 'object' || !('matcher' in navState)) return
    if (navState.matcher === undefined) return
    setMatcher(navState.matcher)
    saveStoredMatcher(navState.matcher)
    setTopTab('matcher')
    navigate('.', { replace: true, state: null })
  }, [location.state, navigate])

  useEffect(() => {
    saveStoredScript(script)
  }, [script])

  useEffect(() => {
    saveStoredOptions(options)
  }, [options])

  useEffect(() => {
    saveTopPaneHeight(topPaneHeight)
  }, [topPaneHeight])

  const rawJson = useMemo(
    () => (result == null ? '' : JSON.stringify(result, null, 2)),
    [result],
  )

  const graphSubgraph = result?.subgraph ?? result?.views.graph ?? null
  const table = result?.views.table ?? null
  const scalar = result?.views.scalar

  const onSplitterPointerDown = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      e.preventDefault()
      dragRef.current = { startY: e.clientY, startHeight: topPaneHeight }
      e.currentTarget.setPointerCapture(e.pointerId)
    },
    [topPaneHeight],
  )

  const onSplitterPointerMove = useCallback((e: ReactPointerEvent<HTMLDivElement>) => {
    const drag = dragRef.current
    if (drag == null) return
    const host = splitHostRef.current
    if (host == null) return
    const hostHeight = host.clientHeight
    const maxTop = Math.max(
      MIN_TOP_PANE_HEIGHT,
      hostHeight - MIN_BOTTOM_PANE_HEIGHT - SPLITTER_HEIGHT,
    )
    const next = clamp(drag.startHeight + (e.clientY - drag.startY), MIN_TOP_PANE_HEIGHT, maxTop)
    setTopPaneHeight(next)
  }, [])

  const onSplitterPointerUp = useCallback((e: ReactPointerEvent<HTMLDivElement>) => {
    dragRef.current = null
    if (e.currentTarget.hasPointerCapture(e.pointerId)) {
      e.currentTarget.releasePointerCapture(e.pointerId)
    }
  }, [])

  async function onExec() {
    setError(null)
    setMatcherError(null)
    setLoading(true)
    try {
      let matcherBody: unknown
      try {
        matcherBody = matcherRef.current?.build()
      } catch (e) {
        const message = e instanceof Error ? e.message : String(e)
        setMatcherError(message)
        setTopTab('matcher')
        return
      }
      if (matcherBody === undefined) {
        setMatcherError('Matcher is not ready')
        setTopTab('matcher')
        return
      }
      const trimmed = script.trim()
      if (!trimmed) {
        setError('Script must not be blank')
        setTopTab('query')
        return
      }

      saveStoredMatcher(matcherBody)
      setMatcher(matcherBody)

      const next = await traverseGremlin({
        matcher: matcherBody,
        script: trimmed,
        traversalOptions: {
          timeoutSeconds: options.timeoutSeconds,
          language: 'gremlin-lang',
        },
      })
      setResult(next)
      setResultTab('structured')

      const sg = next.subgraph ?? next.views.graph ?? null
      if (sg != null) {
        const graph = toGraphData(sg)
        setNodes(graph.nodes)
        setLinks(graph.links)
      } else {
        setNodes([])
        setLinks([])
      }
    } catch (e) {
      setResult(null)
      setNodes([])
      setLinks([])
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group justify="space-between" align="flex-end" wrap="wrap" style={{ flexShrink: 0 }}>
        <div>
          <Title order={3}>Query</Title>
          <Text size="sm" c="dimmed">
            Matcher selects a subgraph; gremlin-lang script runs via{' '}
            <Code>POST /api/v1/objs/graph/traverse/gremlin</Code>.
          </Text>
        </div>
        <Group gap="xs">
          {result != null && (
            <Text size="xs" c="dimmed">
              {formatGremlinStats(result)}
            </Text>
          )}
          <Button loading={loading} onClick={() => void onExec()}>
            Exec
          </Button>
        </Group>
      </Group>

      <Box
        ref={splitHostRef}
        style={{
          flex: 1,
          minHeight: 0,
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Paper
          withBorder
          p="sm"
          style={{
            height: topPaneHeight,
            flexShrink: 0,
            display: 'flex',
            flexDirection: 'column',
            minHeight: 0,
            overflow: 'hidden',
          }}
        >
          <Tabs
            value={topTab}
            onChange={setTopTab}
            style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
          >
            <Tabs.List style={{ flexShrink: 0 }}>
              <Tabs.Tab value="query">Query</Tabs.Tab>
              <Tabs.Tab value="matcher">Matcher</Tabs.Tab>
              <Tabs.Tab value="options">Options</Tabs.Tab>
            </Tabs.List>

            <Tabs.Panel
              value="query"
              pt="sm"
              style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
            >
              <SyntaxCodeEditor
                language="groovy"
                value={script}
                onChange={setScript}
                fillHeight
                minHeight={120}
              />
            </Tabs.Panel>

            <Tabs.Panel value="matcher" pt="sm" style={{ flex: 1, minHeight: 0 }}>
              <ScrollArea h="100%">
                <MatcherQueryForm
                  ref={matcherRef}
                  matcher={matcher}
                  emptyDefaults
                  error={matcherError}
                />
              </ScrollArea>
            </Tabs.Panel>

            <Tabs.Panel value="options" pt="sm" style={{ flex: 1, minHeight: 0 }}>
              <ScrollArea h="100%">
                <Stack gap="sm" maw={320}>
                  <NumberInput
                    label="Timeout (seconds)"
                    description="Eval timeout sent as traversalOptions.timeoutSeconds"
                    min={1}
                    max={3600}
                    value={options.timeoutSeconds}
                    onChange={(v) =>
                      setOptions({
                        timeoutSeconds:
                          typeof v === 'number' && v > 0 ? v : DEFAULT_TIMEOUT_SECONDS,
                      })
                    }
                  />
                  <Text size="xs" c="dimmed">
                    Language is fixed to <Code>gremlin-lang</Code> for this release.
                  </Text>
                </Stack>
              </ScrollArea>
            </Tabs.Panel>
          </Tabs>
        </Paper>

        <Box
          role="separator"
          aria-orientation="horizontal"
          aria-label="Resize query editor"
          onPointerDown={onSplitterPointerDown}
          onPointerMove={onSplitterPointerMove}
          onPointerUp={onSplitterPointerUp}
          onPointerCancel={onSplitterPointerUp}
          style={{
            height: SPLITTER_HEIGHT,
            flexShrink: 0,
            cursor: 'row-resize',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            touchAction: 'none',
            userSelect: 'none',
          }}
        >
          <Box
            style={{
              width: 48,
              height: 3,
              borderRadius: 2,
              background: 'var(--mantine-color-default-border)',
            }}
          />
        </Box>

        {error && (
          <Alert color="red" title="Exec failed" style={{ flexShrink: 0, marginBottom: 8 }}>
            {error}
          </Alert>
        )}

        <Paper
          withBorder
          p="sm"
          style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
        >
          <Tabs
            value={resultTab}
            onChange={setResultTab}
            style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
          >
            <Tabs.List style={{ flexShrink: 0 }}>
              <Tabs.Tab value="structured">Structured</Tabs.Tab>
              <Tabs.Tab value="raw">Raw</Tabs.Tab>
            </Tabs.List>

            <Tabs.Panel
              value="structured"
              pt="sm"
              style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
            >
              {result == null ? (
                <Text size="sm" c="dimmed">
                  Run Exec to see a tactical structured view (graph / table / scalar). Result UX is
                  demo-grade for now.
                </Text>
              ) : graphSubgraph != null && nodes.length > 0 ? (
                <div style={{ flex: 1, minHeight: 0 }}>
                  <GraphCanvas
                    ref={graphRef}
                    nodes={nodes}
                    links={links}
                    selection={null}
                    onSelect={() => undefined}
                    layout="TB"
                  />
                </div>
              ) : table != null ? (
                <ScrollArea style={{ flex: 1, minHeight: 0 }}>
                  <Table striped highlightOnHover withTableBorder withColumnBorders>
                    <Table.Thead>
                      <Table.Tr>
                        {table.columns.map((col) => (
                          <Table.Th key={col}>{col}</Table.Th>
                        ))}
                      </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                      {table.rows.map((row, ri) => (
                        <Table.Tr key={ri}>
                          {table.columns.map((_, ci) => (
                            <Table.Td key={ci}>
                              <Code style={{ whiteSpace: 'pre-wrap' }}>
                                {row[ci] == null
                                  ? ''
                                  : typeof row[ci] === 'string'
                                    ? (row[ci] as string)
                                    : JSON.stringify(row[ci])}
                              </Code>
                            </Table.Td>
                          ))}
                        </Table.Tr>
                      ))}
                    </Table.Tbody>
                  </Table>
                </ScrollArea>
              ) : scalar !== undefined && scalar !== null ? (
                <Code block>
                  {typeof scalar === 'string' ? scalar : JSON.stringify(scalar, null, 2)}
                </Code>
              ) : (
                <Text size="sm" c="dimmed">
                  primary={result.primary}, {result.meta.resultCount} item(s) — open Raw for full
                  JSON.
                </Text>
              )}
            </Tabs.Panel>

            <Tabs.Panel
              value="raw"
              pt="sm"
              style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
            >
              {result == null ? (
                <Text size="sm" c="dimmed">
                  No result yet.
                </Text>
              ) : (
                <SyntaxCodeEditor
                  language="json"
                  value={rawJson}
                  readOnly
                  fillHeight
                  minHeight={120}
                />
              )}
            </Tabs.Panel>
          </Tabs>
        </Paper>
      </Box>
    </Stack>
  )
}
