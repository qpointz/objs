import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type PointerEvent as ReactPointerEvent,
} from 'react'
import {
  ActionIcon,
  Alert,
  Badge,
  Box,
  Button,
  Code,
  Group,
  NumberInput,
  Paper,
  Popover,
  Stack,
  Tabs,
  Text,
  Title,
} from '@mantine/core'
import { IconSettings } from '@tabler/icons-react'
import {
  GraphGoToContextMenu,
  buildGraphNeighborIndex,
  type GraphGoToTarget,
} from './graphGoToNav'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  listSchemas,
  toGraphData,
  traverseGremlin,
  type BoMGremlinResult,
} from './api'
import { GraphContextBar } from './GraphContextBar'
import { useGraphContext } from './GraphContextProvider'
import { GraphCanvas, type GraphCanvasHandle } from './GraphCanvas'
import { ObjectInspectPane } from './ObjectInspectPane'
import { formatObjectCell, scalarPayloadColumns } from './ObjectResultsTable'
import { formatQueryDuration } from './queryExecStats'
import { SyntaxCodeEditor, type SyntaxCodeEditorHandle } from './SyntaxCodeEditor'
import type {
  BoMEdge,
  BoMEntity,
  BoMGraphContents,
  BoMSchema,
  GraphLink,
  GraphNode,
  GraphSelection,
  PayloadFieldKind,
} from './types'
import { matcherFromGraphContext } from './queryGraphContext'
import { resolveQueryResultTab } from './queryResultTabs'
import { DEFAULT_QUERY_SCRIPT, QUERY_SCRIPT_STORAGE_KEY } from './queryScriptDefaults'
import {
  graphContentsFromResult,
  hasOpenInComposerGraph,
  resolveStructuredMode,
  structuredEdgeRows,
  structuredVertexRows,
} from './queryStructuredModel'
import {
  QueryResultGrid,
  QueryStructuredStack,
  QueryTableAlikeGrid,
} from './QueryResultGrid'
import {
  IdLink,
  QUERY_STRUCT_EDGE_ROLE_COL_WIDTH,
  QUERY_STRUCT_EDGE_SOURCE_COL_WIDTH,
  QUERY_STRUCT_ID_COL_WIDTH,
  QUERY_STRUCT_TYPE_COL_WIDTH,
} from './QueryStructColumns'
import { payloadFieldKindsByTypeVersion } from './payloadFieldKinds'
import { EXPLORER_NODE_CAP } from './graphContextVersions'
import { clamp, maxSidePaneWidth } from './sidePaneSplit'
import { VIEW_ACTION_BUTTON_SIZE } from './viewActionButtons'
import { objectDisplayTitle } from './objectViewerTitle'

const SCRIPT_STORAGE_KEY = QUERY_SCRIPT_STORAGE_KEY
const OPTIONS_STORAGE_KEY = 'objs.ui.query.options'
const TOP_PANE_HEIGHT_KEY = 'objs.ui.query.topPaneHeight'
const INSPECT_PANE_WIDTH_KEY = 'objs.ui.query.inspectPaneWidth.v2'

const DEFAULT_TIMEOUT_SECONDS = 60
const DEFAULT_TOP_PANE_HEIGHT = 280
const MIN_TOP_PANE_HEIGHT = 160
const MIN_BOTTOM_PANE_HEIGHT = 160
const SPLITTER_HEIGHT = 8
/** Default inspect pane share when nothing stored (Graph + Structured share one width). */
const DEFAULT_INSPECT_RATIO = 0.3
const MIN_INSPECT_WIDTH = 240
const INSPECT_SPLITTER_WIDTH = 8

type QueryNavState = {
  graphContents?: BoMGraphContents
  graphId?: string
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
  return DEFAULT_QUERY_SCRIPT
}

function saveStoredScript(script: string) {
  try {
    localStorage.setItem(SCRIPT_STORAGE_KEY, script)
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

/** Stored px width, or `null` → use {@link DEFAULT_INSPECT_RATIO} of the split host. */
function loadInspectWidth(): number | null {
  try {
    const raw = localStorage.getItem(INSPECT_PANE_WIDTH_KEY)
    if (!raw) return null
    const n = Number(raw)
    return Number.isFinite(n) && n >= MIN_INSPECT_WIDTH ? n : null
  } catch {
    return null
  }
}

function saveInspectWidth(width: number) {
  try {
    localStorage.setItem(INSPECT_PANE_WIDTH_KEY, String(Math.round(width)))
  } catch {
    // ignore
  }
}

function defaultInspectWidth(hostWidth: number): number {
  return Math.max(MIN_INSPECT_WIDTH, Math.round(hostWidth * DEFAULT_INSPECT_RATIO))
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

type InspectSplitProps = {
  selection: GraphSelection | null
  nodes: GraphNode[]
  fieldKindsByTypeVersion: Map<string, Record<string, PayloadFieldKind>>
  /** Shared Graph/Structured width; `null` until host measured → 30% default. */
  inspectWidth: number | null
  onInspectWidth: (w: number) => void
  onSelect: (sel: GraphSelection | null) => void
  children: React.ReactNode
}

function QueryTabInspectSplit({
  selection,
  nodes,
  fieldKindsByTypeVersion,
  inspectWidth,
  onInspectWidth,
  onSelect,
  children,
}: InspectSplitProps) {
  const hostRef = useRef<HTMLDivElement>(null)
  const dragRef = useRef<{ startX: number; startWidth: number } | null>(null)
  const showInspect = selection != null
  const [hostWidth, setHostWidth] = useState(0)

  const resolvedWidth = useMemo(() => {
    const host = hostWidth > 0 ? hostWidth : 800
    const preferred = inspectWidth ?? defaultInspectWidth(host)
    const max = maxSidePaneWidth(host, MIN_INSPECT_WIDTH)
    return clamp(preferred, MIN_INSPECT_WIDTH, max)
  }, [hostWidth, inspectWidth])

  const onPointerDown = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      e.preventDefault()
      dragRef.current = { startX: e.clientX, startWidth: resolvedWidth }
      e.currentTarget.setPointerCapture(e.pointerId)
    },
    [resolvedWidth],
  )

  const onPointerMove = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      const drag = dragRef.current
      if (drag == null) return
      const host = hostRef.current
      if (host == null) return
      const max = maxSidePaneWidth(host.clientWidth, MIN_INSPECT_WIDTH)
      const next = clamp(drag.startWidth - (e.clientX - drag.startX), MIN_INSPECT_WIDTH, max)
      onInspectWidth(next)
    },
    [onInspectWidth],
  )

  const onPointerUp = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      dragRef.current = null
      if (e.currentTarget.hasPointerCapture(e.pointerId)) {
        e.currentTarget.releasePointerCapture(e.pointerId)
      }
      saveInspectWidth(resolvedWidth)
    },
    [resolvedWidth],
  )

  useEffect(() => {
    const host = hostRef.current
    if (host == null) return
    const sync = () => {
      const w = host.clientWidth
      setHostWidth(w)
      if (w <= 0) return
      if (inspectWidth == null) {
        onInspectWidth(defaultInspectWidth(w))
        return
      }
      const max = maxSidePaneWidth(w, MIN_INSPECT_WIDTH)
      const next = clamp(inspectWidth, MIN_INSPECT_WIDTH, max)
      if (next !== inspectWidth) onInspectWidth(next)
    }
    sync()
    const ro = new ResizeObserver(sync)
    ro.observe(host)
    return () => ro.disconnect()
  }, [inspectWidth, onInspectWidth, showInspect])

  function endpointLabel(nodeId: string): string {
    const node = nodes.find((n) => n.id === nodeId)
    return node ? `${node.name} (${node.type})` : nodeId
  }

  return (
    <Group
      ref={hostRef}
      align="stretch"
      gap={0}
      wrap="nowrap"
      style={{ flex: 1, minHeight: 0, overflow: 'hidden' }}
    >
      <Box style={{ flex: 1, minWidth: 0, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
        {children}
      </Box>
      {showInspect && (
        <>
          <Box
            role="separator"
            aria-orientation="vertical"
            aria-label="Resize object viewer"
            onPointerDown={onPointerDown}
            onPointerMove={onPointerMove}
            onPointerUp={onPointerUp}
            onPointerCancel={onPointerUp}
            style={{
              width: INSPECT_SPLITTER_WIDTH,
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
          <Paper
            withBorder
            p="xs"
            data-tour="query-object-inspect"
            style={{
              width: resolvedWidth,
              flexShrink: 0,
              minHeight: 0,
              overflow: 'hidden',
              display: 'flex',
              flexDirection: 'column',
            }}
          >
            <ObjectInspectPane
              selection={selection}
              nodes={nodes}
              graphContext={null}
              fieldKindsByTypeVersion={fieldKindsByTypeVersion}
              onSelectNode={(id) => {
                const node = nodes.find((n) => n.id === id)
                if (node) onSelect({ kind: 'node', node })
              }}
              onClearSelection={() => onSelect(null)}
              endpointLabel={endpointLabel}
            />
          </Paper>
        </>
      )}
    </Group>
  )
}

export function QueryPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const graphRef = useRef<GraphCanvasHandle>(null)
  const scriptEditorRef = useRef<SyntaxCodeEditorHandle>(null)
  const splitHostRef = useRef<HTMLDivElement>(null)
  const dragRef = useRef<{ startY: number; startHeight: number } | null>(null)

  const [script, setScript] = useState(loadStoredScript)
  const [options, setOptions] = useState<QueryOptions>(loadStoredOptions)
  const [topPaneHeight, setTopPaneHeight] = useState(loadTopPaneHeight)
  const [inspectWidth, setInspectWidth] = useState<number | null>(loadInspectWidth)
  const [optionsOpen, setOptionsOpen] = useState(false)
  const [resultTab, setResultTab] = useState<string | null>('graph')
  const [structVeTab, setStructVeTab] = useState<string | null>('vertices')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [composerError, setComposerError] = useState<string | null>(null)
  const [result, setResult] = useState<BoMGremlinResult | null>(null)
  const [nodes, setNodes] = useState<GraphNode[]>([])
  const [links, setLinks] = useState<GraphLink[]>([])
  const graphOverCap = nodes.length > EXPLORER_NODE_CAP
  const [querySelection, setQuerySelection] = useState<GraphSelection | null>(null)
  const [goToMenu, setGoToMenu] = useState<{ x: number; y: number; target: GraphGoToTarget } | null>(
    null,
  )
  const [schemas, setSchemas] = useState<BoMSchema[]>([])
  const neighborIndex = useMemo(() => buildGraphNeighborIndex(nodes, links), [nodes, links])
  const { context, setGraph } = useGraphContext()

  const fieldKindsByTypeVersion = useMemo(
    () => payloadFieldKindsByTypeVersion(schemas),
    [schemas],
  )

  useEffect(() => {
    listSchemas()
      .then(setSchemas)
      .catch(() => setSchemas([]))
  }, [])

  useEffect(() => {
    const navState = location.state as QueryNavState | null
    if (navState == null || typeof navState !== 'object') return
    const hasGraphContents =
      navState.graphContents != null && typeof navState.graphContents === 'object'
    const hasGraphId = typeof navState.graphId === 'string' && navState.graphId.length > 0
    if (!hasGraphContents && !hasGraphId) return

    navigate('.', { replace: true, state: null })

    if (hasGraphId) {
      setGraph(navState.graphId!)
    }

    if (hasGraphContents) {
      const graph = toGraphData(navState.graphContents!, schemas)
      setNodes(graph.nodes)
      setLinks(graph.links)
      setResult(null)
      setResultTab('graph')
      setQuerySelection(null)
    }
  }, [location.state, navigate, setGraph, schemas])

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

  const table = result?.views.table ?? null
  const scalar = result?.views.scalar
  const items = result?.items ?? []
  const structuredMode = resolveStructuredMode(result)
  const structContents = result != null ? graphContentsFromResult(result) : null
  const vertexRows = useMemo(
    () => (structContents != null ? structuredVertexRows(structContents) : []),
    [structContents],
  )
  const edgeRows = useMemo(
    () => (structContents != null ? structuredEdgeRows(structContents) : []),
    [structContents],
  )
  /** Same scalar payload-column pick as Objects (`scalarPayloadColumns`). */
  const vertexPayloadCols = useMemo(
    () => scalarPayloadColumns(vertexRows.map((r) => r.entity)),
    [vertexRows],
  )

  const canOpenInComposer = hasOpenInComposerGraph(result)

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
    setComposerError(null)
    setLoading(true)
    try {
      let matcherBody: unknown
      try {
        matcherBody = matcherFromGraphContext(context)
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e))
        return
      }
      const fromEditor = scriptEditorRef.current?.getValue()
      const liveScript = fromEditor ?? script
      if (fromEditor != null && fromEditor !== script) {
        setScript(fromEditor)
      }
      const trimmed = liveScript.trim()
      if (!trimmed) {
        setError('Script must not be blank')
        return
      }

      const next = await traverseGremlin({
        matcher: matcherBody,
        script: trimmed,
        traversalOptions: {
          timeoutSeconds: options.timeoutSeconds,
          language: 'gremlin-lang',
        },
        ...(context.kind === 'graph' && context.graphId
          ? {
              graphId: context.graphId,
              ...(context.graphVersion != null ? { graphVersion: context.graphVersion } : {}),
            }
          : {}),
      })
      setResult(next)
      setQuerySelection(null)
      setStructVeTab('vertices')

      const sg = next.contents ?? next.views.graph ?? null
      const graph =
        sg != null ? toGraphData(sg, schemas) : { nodes: [] as GraphNode[], links: [] as GraphLink[] }
      setNodes(graph.nodes)
      setLinks(graph.links)

      const nextTable = next.views.table ?? null
      const nextHasStructured =
        resolveStructuredMode(next) !== 'empty' ||
        next.views.scalar != null ||
        (nextTable != null && nextTable.rows.length > 0) ||
        (next.items?.length ?? 0) > 0
      const nextTab = resolveQueryResultTab(resultTab, {
        hasGraph: graph.nodes.length > 0 || graph.links.length > 0,
        hasStructured: nextHasStructured,
        hasRaw: true,
      })
      if (nextTab !== resultTab) {
        setResultTab(nextTab)
      }
    } catch (e) {
      setResult(null)
      setNodes([])
      setLinks([])
      setQuerySelection(null)
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  const onExecRef = useRef(onExec)
  onExecRef.current = onExec
  const runExecFromEditor = useCallback(() => {
    void onExecRef.current()
  }, [])

  function onOpenInComposer() {
    setComposerError(null)
    if (!canOpenInComposer || result == null) return
    const contents = graphContentsFromResult(result)
    if (contents == null) return
    if ((contents.entities?.length ?? 0) > EXPLORER_NODE_CAP) {
      setComposerError(
        `Result has ${contents.entities.length} nodes (cap ${EXPLORER_NODE_CAP}). Narrow the query before Open in Composer.`,
      )
      return
    }
    navigate('/composer', {
      state: {
        graphId: null,
        replaceDraft: true,
        graphContents: contents,
      },
    })
  }

  const prevResultTabRef = useRef(resultTab)
  useEffect(() => {
    const switchedToGraph = prevResultTabRef.current !== 'graph' && resultTab === 'graph'
    prevResultTabRef.current = resultTab
    if (!switchedToGraph || graphOverCap || (nodes.length === 0 && links.length === 0)) return
    requestAnimationFrame(() => graphRef.current?.applyLayout())
  }, [resultTab, graphOverCap, nodes.length, links.length])

  const inspectNodes =
    resultTab === 'structured' && structContents != null
      ? toGraphData(structContents, schemas).nodes
      : nodes

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group align="center" wrap="nowrap" gap="md" style={{ flexShrink: 0 }}>
        <Title order={3} style={{ flexShrink: 0 }}>
          Query
        </Title>
        <Box style={{ flex: 1, minWidth: 0 }}>
          <GraphContextBar />
        </Box>
      </Group>

      <Group
        justify="space-between"
        align="center"
        wrap="wrap"
        style={{ flexShrink: 0 }}
        gap="xs"
        data-tour="query-view-actions"
      >
        <Text size="xs" c="dimmed" style={{ alignSelf: 'center' }}>
          {result != null ? formatGremlinStats(result) : '\u00a0'}
        </Text>
        <Group gap="xs" wrap="nowrap">
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            variant="default"
            disabled={!canOpenInComposer}
            onClick={onOpenInComposer}
            data-tour="query-open-composer"
          >
            Open in Composer
          </Button>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            loading={loading}
            onClick={() => void onExec()}
            data-tour="query-exec"
          >
            Exec
          </Button>
          <Popover
            opened={optionsOpen}
            onChange={setOptionsOpen}
            position="bottom-end"
            withArrow
            shadow="md"
          >
            <Popover.Target>
              <ActionIcon
                size={VIEW_ACTION_BUTTON_SIZE}
                variant="default"
                aria-label="Query options"
                data-tour="query-options"
                onClick={() => setOptionsOpen((o) => !o)}
              >
                <IconSettings size={16} />
              </ActionIcon>
            </Popover.Target>
            <Popover.Dropdown>
              <Stack gap="sm" w={260}>
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
            </Popover.Dropdown>
          </Popover>
        </Group>
      </Group>

      <Box
        ref={splitHostRef}
        style={{
          flex: 1,
          minWidth: 0,
          minHeight: 0,
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Paper
          withBorder
          p="sm"
          data-tour="query-script"
          style={{
            height: topPaneHeight,
            flexShrink: 0,
            display: 'flex',
            flexDirection: 'column',
            minHeight: 0,
            overflow: 'hidden',
          }}
        >
          <SyntaxCodeEditor
            ref={scriptEditorRef}
            language="groovy"
            value={script}
            onChange={setScript}
            fillHeight
            minHeight={120}
            onModEnter={runExecFromEditor}
          />
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

        <Paper
          withBorder
          p="sm"
          style={{
            flex: 1,
            minHeight: 0,
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
          }}
        >
          {error && (
            <Alert color="red" title="Exec failed" mb="xs" withCloseButton onClose={() => setError(null)}>
              {error}
            </Alert>
          )}
          {composerError && (
            <Alert
              color="yellow"
              title="Open in Composer refused"
              mb="xs"
              withCloseButton
              onClose={() => setComposerError(null)}
            >
              {composerError}
            </Alert>
          )}
          <Tabs
            value={resultTab}
            onChange={(v) => {
              setResultTab(v)
              setQuerySelection(null)
            }}
            style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
          >
            <Tabs.List style={{ flexShrink: 0 }}>
              <Tabs.Tab value="graph">Visual</Tabs.Tab>
              <Tabs.Tab value="structured">Data</Tabs.Tab>
              <Tabs.Tab value="raw">Raw</Tabs.Tab>
            </Tabs.List>

            <Tabs.Panel
              value="graph"
              pt="sm"
              style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
            >
              <QueryTabInspectSplit
                selection={querySelection}
                nodes={nodes}
                fieldKindsByTypeVersion={fieldKindsByTypeVersion}
                inspectWidth={inspectWidth}
                onInspectWidth={setInspectWidth}
                onSelect={setQuerySelection}
              >
                {nodes.length === 0 && links.length === 0 ? (
                  <Text size="sm" c="dimmed">
                    {result != null && result.meta.resultCount === 0
                      ? result.meta.subgraph1Stats.entities > 0
                        ? `Traversal returned no elements (seed had ${result.meta.subgraph1Stats.entities} entities). Try g.V().`
                        : 'Seed subgraph is empty — open a graph with members, then Exec.'
                      : 'No graph view in the last result.'}
                  </Text>
                ) : graphOverCap ? (
                  <Alert color="yellow" title="Graph canvas disabled">
                    Result has {nodes.length} nodes (cap {EXPLORER_NODE_CAP}). Narrow the query or
                    open Data / Raw instead.
                  </Alert>
                ) : (
                  <GraphCanvas
                    ref={graphRef}
                    nodes={nodes}
                    links={links}
                    selection={querySelection}
                    onSelect={setQuerySelection}
                    layout="TB"
                    autoLayoutOnDataChange={resultTab === 'graph'}
                    onNodeContextMenu={(event, node) => {
                      event.preventDefault()
                      setQuerySelection({ kind: 'node', node })
                      setGoToMenu({
                        x: event.clientX,
                        y: event.clientY,
                        target: { kind: 'node', nodeId: node.id },
                      })
                    }}
                    onEdgeContextMenu={(event, edge) => {
                      event.preventDefault()
                      setQuerySelection({ kind: 'edge', edge })
                      setGoToMenu({
                        x: event.clientX,
                        y: event.clientY,
                        target: {
                          kind: 'edge',
                          sourceId: edge.source,
                          targetId: edge.target,
                        },
                      })
                    }}
                  />
                )}
              </QueryTabInspectSplit>
            </Tabs.Panel>

            <Tabs.Panel
              value="structured"
              pt="sm"
              style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
            >
              <QueryTabInspectSplit
                selection={querySelection}
                nodes={inspectNodes}
                fieldKindsByTypeVersion={fieldKindsByTypeVersion}
                inspectWidth={inspectWidth}
                onInspectWidth={setInspectWidth}
                onSelect={setQuerySelection}
              >
                {result == null ? (
                  !error && (
                    <Text size="sm" c="dimmed">
                      Run Exec to see results.
                    </Text>
                  )
                ) : structuredMode === 'empty' ? (
                  <Text size="sm" c="dimmed">
                    {result.meta.resultCount === 0 && result.meta.subgraph1Stats.entities > 0
                      ? `Traversal returned no elements (seed had ${result.meta.subgraph1Stats.entities} entities). Try g.V().`
                      : 'No structured projection for this result (see Raw).'}
                  </Text>
                ) : structuredMode === 'table' ? (
                  <QueryStructuredStack>
                    {scalar != null && (
                      <Text size="sm">
                        Scalar: <Code>{String(scalar)}</Code>
                      </Text>
                    )}
                    {table != null && table.rows.length > 0 && (
                      <Box style={{ flex: 1, minHeight: 120, display: 'flex', flexDirection: 'column' }}>
                        <QueryTableAlikeGrid columns={table.columns} rows={table.rows} />
                      </Box>
                    )}
                    {items.length > 0 && (table == null || table.rows.length === 0) && scalar == null && (
                      <Stack gap="xs">
                        {items.map((item, i) => (
                          <Paper key={i} withBorder p="xs" radius="sm">
                            <Group gap="xs" mb={4}>
                              <Badge size="sm" variant="light">
                                {item.kind}
                              </Badge>
                            </Group>
                            <Code
                              block
                              style={{
                                whiteSpace: 'pre-wrap',
                                fontSize: 12,
                                maxHeight: 200,
                                overflow: 'auto',
                              }}
                            >
                              {JSON.stringify(item.value, null, 2)}
                            </Code>
                          </Paper>
                        ))}
                      </Stack>
                    )}
                  </QueryStructuredStack>
                ) : (
                  <Tabs
                    value={structVeTab}
                    onChange={(v) => {
                      setStructVeTab(v)
                      setQuerySelection(null)
                    }}
                    style={{
                      flex: 1,
                      minHeight: 0,
                      display: 'flex',
                      flexDirection: 'column',
                    }}
                  >
                    <Tabs.List style={{ flexShrink: 0, alignSelf: 'flex-start' }}>
                      <Tabs.Tab value="vertices" style={{ fontSize: 'var(--mantine-font-size-xs)' }}>
                        Vertices ({vertexRows.length})
                      </Tabs.Tab>
                      <Tabs.Tab value="edges" style={{ fontSize: 'var(--mantine-font-size-xs)' }}>
                        Edges ({edgeRows.length})
                      </Tabs.Tab>
                    </Tabs.List>
                    <Tabs.Panel
                      value="vertices"
                      pt="xs"
                      style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
                    >
                      <QueryResultGrid
                        rows={vertexRows}
                        rowKey={(r) => r.id}
                        selectedKey={
                          querySelection?.kind === 'node' ? querySelection.node.id : null
                        }
                        onRowSelect={(row) =>
                          setQuerySelection({ kind: 'node', node: entityToGraphNode(row.entity) })
                        }
                        empty={
                          <Text size="sm" c="dimmed">
                            No vertices in this result.
                          </Text>
                        }
                        columns={[
                          {
                            key: 'id',
                            header: 'Id',
                            width: QUERY_STRUCT_ID_COL_WIDTH,
                            render: (row) => (
                              <IdLink
                                id={row.id}
                                onOpen={() =>
                                  setQuerySelection({
                                    kind: 'node',
                                    node: entityToGraphNode(row.entity),
                                  })
                                }
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
                            render: (row: (typeof vertexRows)[number]) =>
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
                        rows={edgeRows}
                        rowKey={(r) => r.id}
                        selectedKey={
                          querySelection?.kind === 'edge' ? querySelection.edge.id : null
                        }
                        onRowSelect={(row) =>
                          setQuerySelection({
                            kind: 'edge',
                            edge: edgeToGraphLink(row.edge, 0),
                          })
                        }
                        empty={
                          <Text size="sm" c="dimmed">
                            No edges in this result.
                          </Text>
                        }
                        columns={[
                          {
                            key: 'id',
                            header: 'Id',
                            width: QUERY_STRUCT_ID_COL_WIDTH,
                            render: (row) => (
                              <IdLink
                                id={row.id}
                                onOpen={() =>
                                  setQuerySelection({
                                    kind: 'edge',
                                    edge: edgeToGraphLink(row.edge, 0),
                                  })
                                }
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
                )}
              </QueryTabInspectSplit>
            </Tabs.Panel>

            <Tabs.Panel
              value="raw"
              pt="sm"
              style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
            >
              <SyntaxCodeEditor language="json" value={rawJson} readOnly fillHeight minHeight={120} />
            </Tabs.Panel>
          </Tabs>
        </Paper>
      </Box>

      <GraphGoToContextMenu
        opened={goToMenu != null}
        x={goToMenu?.x ?? 0}
        y={goToMenu?.y ?? 0}
        onClose={() => setGoToMenu(null)}
        target={goToMenu?.target ?? null}
        nodes={nodes}
        index={neighborIndex}
        onGoTo={(id) => {
          const node = nodes.find((n) => n.id === id)
          if (node) setQuerySelection({ kind: 'node', node })
          requestAnimationFrame(() => graphRef.current?.focusNode(id))
        }}
      />
    </Stack>
  )
}
