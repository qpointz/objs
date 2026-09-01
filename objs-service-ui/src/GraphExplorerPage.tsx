import { useCallback, useEffect, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import {
  ActionIcon,
  Alert,
  Badge,
  Box,
  Button,
  Group,
  Loader,
  Menu,
  Paper,
  Stack,
  Text,
  Title,
  Tooltip,
} from '@mantine/core'
import { IconX } from '@tabler/icons-react'
import {
  GraphGoToContextMenu,
  buildGraphNeighborIndex,
  type GraphGoToTarget,
} from './graphGoToNav'
import { useNavigate } from 'react-router-dom'
import {
  GraphCanvas,
  type GraphCanvasHandle,
  type GraphLayout,
  type GraphNodePositions,
} from './GraphCanvas'
import {
  analyzeCycles,
  fetchGraphAlgorithmCapabilities,
  getGraph,
  getGraphVersion,
  graphContentsFromGraphView,
  listSchemas,
  toGraphData,
} from './api'
import { GraphContextBar } from './GraphContextBar'
import { useGraphContext } from './GraphContextProvider'
import { EXPLORER_NODE_CAP } from './graphContextVersions'
import { ObjectInspectPane } from './ObjectInspectPane'
import { payloadFieldKindsByTypeVersion } from './payloadFieldKinds'
import type {
  BoMGraphContents,
  BoMGraphResponse,
  BoMSchema,
  GraphCycleAnalysis,
  GraphAlgorithmCapabilities,
  GraphLink,
  GraphNode,
  GraphSelection,
} from './types'
import { applyTypeHighlightDimming, toggleTypeInSet } from './typeHighlightDimming'
import { clamp, maxSidePaneWidth } from './sidePaneSplit'
import { newGraphQueryId, useGraphSelectionHistory } from './useGraphSelectionHistory'
import { VIEW_ACTION_BUTTON_SIZE } from './viewActionButtons'
import {
  cycleAnalysisHighlights,
  supportsGenericCycleAnalysis,
} from './analysisHighlight'
import { matcherFromGraphContext } from './queryGraphContext'

const GRAPH_SESSION_STORAGE_KEY = 'objs.ui.graphExplorer.session'
const SIDE_PANE_WIDTH_KEY = 'objs.ui.explorer.sidePaneWidth'
const DEFAULT_SIDE_WIDTH = 340
const MIN_SIDE_WIDTH = 240
const SPLITTER_WIDTH = 8

type ExploreMode = 'graph' | 'selection'

const GRAPH_LAYOUTS: { value: GraphLayout; label: string }[] = [
  { value: 'TB', label: 'Top to bottom' },
  { value: 'LR', label: 'Left to right' },
  { value: 'BT', label: 'Bottom to top' },
  { value: 'RL', label: 'Right to left' },
]

type StoredGraphSession = {
  nodes: GraphNode[]
  links: GraphLink[]
  layout: GraphLayout
  queryId?: string
}

function loadSideWidth(): number {
  try {
    const raw = localStorage.getItem(SIDE_PANE_WIDTH_KEY)
    if (!raw) return DEFAULT_SIDE_WIDTH
    const n = Number(raw)
    return Number.isFinite(n) && n >= MIN_SIDE_WIDTH ? n : DEFAULT_SIDE_WIDTH
  } catch {
    return DEFAULT_SIDE_WIDTH
  }
}

function saveSideWidth(width: number) {
  try {
    localStorage.setItem(SIDE_PANE_WIDTH_KEY, String(width))
  } catch {
    // ignore
  }
}

function loadStoredGraphSession(): StoredGraphSession | null {
  try {
    const raw = localStorage.getItem(GRAPH_SESSION_STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<StoredGraphSession>
    if (!Array.isArray(parsed.nodes) || !Array.isArray(parsed.links)) return null
    const layout =
      parsed.layout === 'TB' ||
      parsed.layout === 'LR' ||
      parsed.layout === 'BT' ||
      parsed.layout === 'RL'
        ? parsed.layout
        : 'TB'
    const queryId =
      typeof parsed.queryId === 'string' && parsed.queryId.length > 0 ? parsed.queryId : undefined
    return { nodes: parsed.nodes, links: parsed.links, layout, queryId }
  } catch {
    return null
  }
}

function initialExplorerQueryId(session: StoredGraphSession | null): string | null {
  try {
    const fromUrl = new URLSearchParams(window.location.search).get('qid')
    if (fromUrl) return fromUrl
  } catch {
    // ignore
  }
  if (session?.queryId) return session.queryId
  if (session && (session.nodes.length > 0 || session.links.length > 0)) {
    return newGraphQueryId()
  }
  return null
}

function saveStoredGraphSession(session: StoredGraphSession) {
  try {
    localStorage.setItem(GRAPH_SESSION_STORAGE_KEY, JSON.stringify(session))
  } catch {
    // ignore quota / private mode
  }
}

function clearStoredGraphSession() {
  try {
    localStorage.removeItem(GRAPH_SESSION_STORAGE_KEY)
  } catch {
    // ignore
  }
}

export function GraphExplorerPage() {
  const navigate = useNavigate()
  const graphRef = useRef<GraphCanvasHandle>(null)
  const { context, setCounts, setAnnotations, setGraphVersion, clear } = useGraphContext()
  const exploreMode: ExploreMode = context.kind === 'matcher' ? 'selection' : 'graph'
  const currentGraphId = context.kind === 'graph' ? context.graphId : null
  const viewingVersion = context.kind === 'graph' ? context.graphVersion : null
  const [storedSession] = useState(() => loadStoredGraphSession())
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [nodes, setNodes] = useState<GraphNode[]>(() => storedSession?.nodes ?? [])
  const [links, setLinks] = useState<GraphLink[]>(() => storedSession?.links ?? [])
  const [layout, setLayout] = useState<GraphLayout>(() => storedSession?.layout ?? 'TB')
  const [canvasEpoch, setCanvasEpoch] = useState(0)
  const canvasOverCap =
    context.nodeCount > EXPLORER_NODE_CAP || nodes.length > EXPLORER_NODE_CAP
  const [goToMenu, setGoToMenu] = useState<{ x: number; y: number; target: GraphGoToTarget } | null>(
    null,
  )
  const linksRef = useRef(links)
  linksRef.current = links
  const nodesRef = useRef(nodes)
  nodesRef.current = nodes
  const layoutRef = useRef(layout)
  layoutRef.current = layout
  const queryIdRef = useRef<string | null>(null)
  const loadedGraphIdRef = useRef<string | null>(null)

  const [initialQueryId] = useState(() => initialExplorerQueryId(storedSession))
  const onFocusNode = useCallback((nodeId: string) => {
    graphRef.current?.focusNode(nodeId)
  }, [])

  const { selection, select, beginQueryResult, queryId } = useGraphSelectionHistory({
    nodes,
    links,
    onFocusNode,
    initialQueryId,
  })
  queryIdRef.current = queryId

  const [highlightedTypes, setHighlightedTypes] = useState<Set<string>>(() => new Set())
  const [schemas, setSchemas] = useState<BoMSchema[]>([])
  const [sideWidth, setSideWidth] = useState(loadSideWidth)
  const [algorithmCapabilities, setAlgorithmCapabilities] = useState<GraphAlgorithmCapabilities | null>(
    null,
  )
  const [cycleAnalysis, setCycleAnalysis] = useState<GraphCycleAnalysis | null>(null)
  const [cycleAnalysisLoading, setCycleAnalysisLoading] = useState(false)
  const [cycleAnalysisError, setCycleAnalysisError] = useState<string | null>(null)
  const [cycleAnalysisMessage, setCycleAnalysisMessage] = useState<string | null>(null)
  const splitHostRef = useRef<HTMLDivElement>(null)
  const dragRef = useRef<{ startX: number; startWidth: number } | null>(null)

  useEffect(() => {
    const host = splitHostRef.current
    if (host == null) return
    const clampToHost = () => {
      const max = maxSidePaneWidth(host.clientWidth, MIN_SIDE_WIDTH)
      setSideWidth((w) => {
        const next = clamp(w, MIN_SIDE_WIDTH, max)
        return next === w ? w : next
      })
    }
    clampToHost()
    const ro = new ResizeObserver(clampToHost)
    ro.observe(host)
    return () => ro.disconnect()
  }, [])

  const onSplitterPointerDown = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      e.currentTarget.setPointerCapture(e.pointerId)
      dragRef.current = { startX: e.clientX, startWidth: sideWidth }
    },
    [sideWidth],
  )

  const onSplitterPointerMove = useCallback((e: ReactPointerEvent<HTMLDivElement>) => {
    const drag = dragRef.current
    if (!drag) return
    const host = splitHostRef.current
    if (host == null) return
    const max = maxSidePaneWidth(host.clientWidth, MIN_SIDE_WIDTH)
    const next = clamp(drag.startWidth - (e.clientX - drag.startX), MIN_SIDE_WIDTH, max)
    setSideWidth(next)
  }, [])

  const onSplitterPointerUp = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      const drag = dragRef.current
      dragRef.current = null
      if (e.currentTarget.hasPointerCapture(e.pointerId)) {
        e.currentTarget.releasePointerCapture(e.pointerId)
      }
      if (drag != null) {
        saveSideWidth(sideWidth)
      }
    },
    [sideWidth],
  )

  useEffect(() => {
    let cancelled = false
    listSchemas()
      .then((list) => {
        if (!cancelled) setSchemas(list)
      })
      .catch(() => {
        /* inspector still works without enum pills */
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    fetchGraphAlgorithmCapabilities()
      .then((caps) => {
        if (!cancelled) setAlgorithmCapabilities(caps)
      })
      .catch(() => {
        if (!cancelled) setAlgorithmCapabilities(null)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const clearCycleAnalysis = useCallback(() => {
    setCycleAnalysis(null)
    setCycleAnalysisError(null)
    setCycleAnalysisMessage(null)
  }, [])

  useEffect(() => {
    clearCycleAnalysis()
  }, [
    clearCycleAnalysis,
    context.kind,
    context.graphId,
    context.graphVersion,
    context.matcherBody,
  ])

  useEffect(() => {
    // Keep canvas in sync with shared graph context. Never leave a previous graph
    // painted when context changed (esp. over-cap graphs / matcher handoff).
    let cancelled = false
    setError(null)

    if (context.nodeCount > EXPLORER_NODE_CAP) {
      setNodes([])
      setLinks([])
      setHighlightedTypes(new Set())
      clearStoredGraphSession()
      loadedGraphIdRef.current = exploreMode === 'graph' ? currentGraphId : null
      setLoading(false)
      return () => {
        cancelled = true
      }
    }

    if (exploreMode !== 'graph' || !currentGraphId) {
      // Leaving graph mode: drop prior graph canvas (matcher results may replace it via onApplied).
      if (loadedGraphIdRef.current != null) {
        setNodes([])
        setLinks([])
        setHighlightedTypes(new Set())
        clearStoredGraphSession()
        loadedGraphIdRef.current = null
      }
      setLoading(false)
      return () => {
        cancelled = true
      }
    }

    const keepLayout =
      loadedGraphIdRef.current === currentGraphId && nodesRef.current.length > 0
    if (!keepLayout) {
      setNodes([])
      setLinks([])
      setLoading(true)
    }

    const load =
      viewingVersion == null
        ? getGraph(currentGraphId)
        : getGraphVersion(currentGraphId, viewingVersion)
    load
      .then((resolved) => {
        if (cancelled) return
        setAnnotations(resolved.annotations ?? {})
        const graph = toGraphData(resolved.graph, schemas)
        const entityCount = graph.nodes.length
        setCounts(entityCount, graph.links.length)

        if (entityCount > EXPLORER_NODE_CAP) {
          setNodes([])
          setLinks([])
          setHighlightedTypes(new Set())
          clearStoredGraphSession()
          loadedGraphIdRef.current = currentGraphId
          setCanvasEpoch((n) => n + 1)
          return
        }

        const prev = nodesRef.current
        const nextNodes = keepLayout
          ? graph.nodes.map((n) => {
              const p = prev.find((x) => x.id === n.id)
              return p != null && p.x != null && p.y != null ? { ...n, x: p.x, y: p.y } : n
            })
          : graph.nodes
        setNodes(nextNodes)
        setLinks(graph.links)
        loadedGraphIdRef.current = currentGraphId
        if (keepLayout) {
          persistSession(nextNodes, graph.links, layoutRef.current)
          return
        }
        setHighlightedTypes(new Set())
        const qid = beginQueryResult()
        persistSession(nextNodes, graph.links, layoutRef.current, qid)
        setCanvasEpoch((n) => n + 1)
      })
      .catch(() => {
        if (cancelled) return
        if (viewingVersion != null) {
          setGraphVersion(null)
          return
        }
        loadedGraphIdRef.current = null
        clear()
        setNodes([])
        setLinks([])
        clearStoredGraphSession()
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    currentGraphId,
    exploreMode,
    clear,
    setAnnotations,
    setCounts,
    setGraphVersion,
    viewingVersion,
    context.nodeCount,
  ])

  const fieldKindsByTypeVersion = useMemo(
    () => payloadFieldKindsByTypeVersion(schemas),
    [schemas],
  )

  const types = useMemo(() => {
    const set = new Map<string, string>()
    for (const n of nodes) {
      if (!set.has(n.type)) set.set(n.type, n.color)
    }
    return [...set.entries()].sort(([a], [b]) => a.localeCompare(b))
  }, [nodes])

  const nodesWithKinds = useMemo(
    () =>
      nodes.map((n) => ({
        ...n,
        payloadFieldKinds: fieldKindsByTypeVersion.get(`${n.type}@${n.schemaVersion}`),
      })),
    [fieldKindsByTypeVersion, nodes],
  )

  const displayGraph = useMemo(
    () => applyTypeHighlightDimming(nodesWithKinds, links, highlightedTypes),
    [highlightedTypes, links, nodesWithKinds],
  )

  const neighborIndex = useMemo(
    () => buildGraphNeighborIndex(displayGraph.nodes, displayGraph.links),
    [displayGraph],
  )

  const typeHighlightNodeIds = useMemo(() => {
    if (highlightedTypes.size === 0) return undefined
    return displayGraph.nodes.filter((n) => highlightedTypes.has(n.type)).map((n) => n.id)
  }, [highlightedTypes, displayGraph.nodes])

  const cycleHighlights = useMemo(
    () => (cycleAnalysis ? cycleAnalysisHighlights(cycleAnalysis) : { nodeIds: [], edgeIds: [] }),
    [cycleAnalysis],
  )

  const canvasNonEmpty = nodes.length > 0 || links.length > 0

  const cycleAnalysisAvailable = supportsGenericCycleAnalysis(algorithmCapabilities)
  const canAnalyzeCycles =
    cycleAnalysisAvailable &&
    canvasNonEmpty &&
    !canvasOverCap &&
    context.kind !== 'empty'

  const clearTypeHighlight = useCallback(() => {
    setHighlightedTypes((prev) => (prev.size === 0 ? prev : new Set()))
  }, [])

  async function onAnalyzeCycles() {
    setCycleAnalysisError(null)
    setCycleAnalysisMessage(null)
    setCycleAnalysisLoading(true)
    try {
      let matcherBody: unknown
      try {
        matcherBody = matcherFromGraphContext(context)
      } catch (e) {
        setCycleAnalysisError(e instanceof Error ? e.message : String(e))
        return
      }
      const result = await analyzeCycles({
        matcher: matcherBody,
        materialization: 'GENERIC',
        ...(context.kind === 'graph' && context.graphId
          ? {
              graphId: context.graphId,
              ...(context.graphVersion != null ? { graphVersion: context.graphVersion } : {}),
            }
          : {}),
      })
      setCycleAnalysis(result)
      if (result.components.length === 0) {
        setCycleAnalysisMessage('No directed cycle regions found.')
      } else {
        setCycleAnalysisMessage(
          `${result.stats.cyclicComponentCount} cycle region(s); ${result.stats.entityCount} entities and ${result.stats.edgeCount} edges highlighted.`,
        )
      }
    } catch (e) {
      setCycleAnalysis(null)
      setCycleAnalysisError(e instanceof Error ? e.message : String(e))
    } finally {
      setCycleAnalysisLoading(false)
    }
  }

  const handleSelect = useCallback(
    (next: GraphSelection | null) => {
      // Type filter stays until Clear / graph reload — selection must not reset it (Note 4).
      select(next)
    },
    [select],
  )

  function toggleTypeHighlight(type: string) {
    setHighlightedTypes((prev) => toggleTypeInSet(prev, type))
  }

  function persistSession(
    nextNodes: GraphNode[],
    nextLinks: GraphLink[],
    nextLayout: GraphLayout,
    nextQueryId: string | null = queryIdRef.current,
  ) {
    if (nextNodes.length === 0 && nextLinks.length === 0) {
      clearStoredGraphSession()
      return
    }
    saveStoredGraphSession({
      nodes: nextNodes,
      links: nextLinks,
      layout: nextLayout,
      ...(nextQueryId ? { queryId: nextQueryId } : {}),
    })
  }

  useEffect(() => {
    if (!queryId) return
    if (nodes.length === 0 && links.length === 0) return
    persistSession(nodes, links, layout, queryId)
    // Seed session with qid only; positions persist via onPositionsChange.
    // Intentionally omit nodes/links/layout deps.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queryId])

  function onPositionsChange(positions: GraphNodePositions) {
    setNodes((prev) => {
      const next = prev.map((n) => {
        const p = positions[n.id]
        return p ? { ...n, x: p.x, y: p.y } : n
      })
      persistSession(next, linksRef.current, layoutRef.current)
      return next
    })
  }

  async function onMatcherApplied(raw: { entities: unknown[]; edges: unknown[] }, _body: unknown) {
    setError(null)
    setLoading(true)
    loadedGraphIdRef.current = null
    try {
      const contents: BoMGraphContents = {
        entities: (raw.entities ?? []) as BoMGraphContents['entities'],
        edges: (raw.edges ?? []) as BoMGraphContents['edges'],
      }
      const graph = toGraphData(contents, schemas)
      if (graph.nodes.length > EXPLORER_NODE_CAP) {
        setNodes([])
        setLinks([])
        setHighlightedTypes(new Set())
        clearStoredGraphSession()
        setCanvasEpoch((n) => n + 1)
        return
      }
      const qid = beginQueryResult()
      setNodes(graph.nodes)
      setLinks(graph.links)
      setHighlightedTypes(new Set())
      clearCycleAnalysis()
      clearStoredGraphSession()
      persistSession(graph.nodes, graph.links, layout, qid)
      setCanvasEpoch((n) => n + 1)
    } catch (e) {
      setNodes([])
      setLinks([])
      clearStoredGraphSession()
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  function onOpenInComposer() {
    if (exploreMode !== 'graph' || !currentGraphId) return
    navigate('/composer', {
      state: { graphId: currentGraphId },
    })
  }

  function onNewGraphFromSelection() {
    if (exploreMode !== 'selection') return
    if (!canvasNonEmpty) return
    navigate('/composer', {
      state: {
        graphId: null,
        replaceDraft: true,
        graphContents: graphContentsFromGraphView(nodes, links),
      },
    })
  }

  function onGraphOpened(id: string, resolved: BoMGraphResponse) {
    const graph = toGraphData(resolved.graph, schemas)
    setNodes(graph.nodes)
    setLinks(graph.links)
    setHighlightedTypes(new Set())
    clearCycleAnalysis()
    clearStoredGraphSession()
    const qid = beginQueryResult()
    persistSession(graph.nodes, graph.links, layout, qid)
    setCanvasEpoch((n) => n + 1)
    loadedGraphIdRef.current = id
  }

  function changeLayout(next: GraphLayout) {
    setLayout(next)
    layoutRef.current = next
    graphRef.current?.applyLayout(next)
  }

  function selectNodeFromCanvas(nodeId: string) {
    const node = nodes.find((n) => n.id === nodeId)
    if (!node) return
    handleSelect({ kind: 'node', node })
    requestAnimationFrame(() => graphRef.current?.focusNode(nodeId))
  }

  function onCanvasNodeContextMenu(event: { preventDefault: () => void; clientX: number; clientY: number }, node: GraphNode) {
    event.preventDefault()
    handleSelect({ kind: 'node', node })
    setGoToMenu({ x: event.clientX, y: event.clientY, target: { kind: 'node', nodeId: node.id } })
  }

  function onCanvasEdgeContextMenu(event: { preventDefault: () => void; clientX: number; clientY: number }, edge: GraphLink) {
    event.preventDefault()
    handleSelect({ kind: 'edge', edge })
    setGoToMenu({
      x: event.clientX,
      y: event.clientY,
      target: { kind: 'edge', sourceId: edge.source, targetId: edge.target },
    })
  }

  function endpointLabel(nodeId: string): string {
    const node = nodes.find((n) => n.id === nodeId)
    return node ? `${node.name} (${node.type})` : nodeId
  }

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0 }}>
      <Group align="center" wrap="nowrap" gap="md" style={{ flexShrink: 0 }}>
        <Title order={3} style={{ flexShrink: 0 }}>
          Explorer
        </Title>
        <Box style={{ flex: 1, minWidth: 0 }}>
          <GraphContextBar onGraphOpened={onGraphOpened} onMatcherApplied={onMatcherApplied} />
        </Box>
      </Group>

      <Group
        align="flex-start"
        wrap="nowrap"
        gap="md"
        style={{ flexShrink: 0 }}
        data-tour="explorer-type-actions"
      >
        <Group gap="xs" wrap="wrap" style={{ flex: 1, minWidth: 0 }}>
          {!error &&
            canvasNonEmpty &&
            types.map(([type, color]) => {
              const active = highlightedTypes.has(type)
              return (
                <Badge
                  key={type}
                  variant={active ? 'filled' : 'outline'}
                  color="gray"
                  leftSection={
                    active ? undefined : (
                      <span style={{ color, lineHeight: 1, fontWeight: 700 }}>+</span>
                    )
                  }
                  onClick={() => toggleTypeHighlight(type)}
                  style={{
                    cursor: 'pointer',
                    background: active ? color : undefined,
                    borderColor: color,
                    color: active ? '#fff' : color,
                    userSelect: 'none',
                  }}
                >
                  {type}
                </Badge>
              )
            })}
          {!error && canvasNonEmpty && highlightedTypes.size > 0 && (
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
        <Group gap={6} wrap="nowrap" data-tour="explorer-view-actions" style={{ flexShrink: 0 }}>
          {canAnalyzeCycles && (
            <>
              <Button
                size={VIEW_ACTION_BUTTON_SIZE}
                variant="light"
                color="violet"
                loading={cycleAnalysisLoading}
                onClick={onAnalyzeCycles}
              >
                Analyze cycles
              </Button>
              {cycleAnalysis != null && (
                <Tooltip label="Clear cycle analysis highlight" withArrow>
                  <ActionIcon
                    size="sm"
                    variant="subtle"
                    color="violet"
                    aria-label="Clear cycle analysis highlight"
                    onClick={clearCycleAnalysis}
                  >
                    <IconX size={14} />
                  </ActionIcon>
                </Tooltip>
              )}
            </>
          )}
          {exploreMode === 'graph' ? (
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              variant="light"
              disabled={!currentGraphId}
              onClick={onOpenInComposer}
            >
              Open in Composer
            </Button>
          ) : (
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              variant="light"
              disabled={!canvasNonEmpty}
              onClick={onNewGraphFromSelection}
            >
              New graph from selection
            </Button>
          )}
          <Group gap={0}>
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              variant="light"
              disabled={nodes.length === 0}
              onClick={() => graphRef.current?.applyLayout()}
              style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
            >
              Apply layout
            </Button>
            <Menu position="bottom-end" withinPortal>
              <Menu.Target>
                <Button
                  size={VIEW_ACTION_BUTTON_SIZE}
                  variant="light"
                  disabled={nodes.length === 0}
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
                      if (option.value === layout) {
                        graphRef.current?.applyLayout()
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
        </Group>
      </Group>

      {error && (
        <Alert color="red" title="Query failed">
          {error}
        </Alert>
      )}

      {cycleAnalysisError && (
        <Alert color="red" title="Cycle analysis failed" onClose={() => setCycleAnalysisError(null)} withCloseButton>
          {cycleAnalysisError}
        </Alert>
      )}

      {cycleAnalysisMessage && !cycleAnalysisError && (
        <Alert color="violet" title="Cycle analysis" onClose={() => setCycleAnalysisMessage(null)} withCloseButton>
          {cycleAnalysisMessage}
        </Alert>
      )}

      <Group
        ref={splitHostRef}
        align="stretch"
        wrap="nowrap"
        preventGrowOverflow={false}
        style={{ flex: 1, minHeight: 0 }}
        gap={0}
      >
        <Paper withBorder style={{ flex: 1, minWidth: 0, minHeight: 280, overflow: 'hidden', position: 'relative' }}>
          {loading && (
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
          {canvasOverCap && !loading ? (
            <Stack align="center" justify="center" gap="sm" p="md" h="100%">
              <Alert color="yellow" title="Graph canvas disabled">
                This context has{' '}
                {Math.max(context.nodeCount, nodes.length)} nodes (cap {EXPLORER_NODE_CAP}). Narrow
                the graph context or open a smaller graph to use the canvas.
              </Alert>
            </Stack>
          ) : nodes.length === 0 && !loading ? (
            <Text c="dimmed" p="md">
              Open a graph or press Exec to load a selection.
            </Text>
          ) : !canvasOverCap ? (
            <GraphCanvas
              key={canvasEpoch}
              ref={graphRef}
              nodes={displayGraph.nodes}
              links={displayGraph.links}
              selection={selection}
              onSelect={handleSelect}
              onNodeContextMenu={onCanvasNodeContextMenu}
              onEdgeContextMenu={onCanvasEdgeContextMenu}
              layout={layout}
              autoLayoutOnDataChange={false}
              onPositionsChange={onPositionsChange}
              highlightedNodeIds={typeHighlightNodeIds}
              analysisHighlightedNodeIds={cycleHighlights.nodeIds}
              analysisHighlightedEdgeIds={cycleHighlights.edgeIds}
            />
          ) : null}
          <GraphGoToContextMenu
            opened={goToMenu != null}
            x={goToMenu?.x ?? 0}
            y={goToMenu?.y ?? 0}
            onClose={() => setGoToMenu(null)}
            target={goToMenu?.target ?? null}
            nodes={displayGraph.nodes}
            index={neighborIndex}
            onGoTo={selectNodeFromCanvas}
          />
        </Paper>

        <Box
          role="separator"
          aria-orientation="vertical"
          aria-label="Resize Explorer inspect pane"
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

        <Paper
          withBorder
          p="md"
          style={{
            width: sideWidth,
            flexShrink: 0,
            minHeight: 280,
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column',
          }}
          data-tour="object-inspect"
        >
          <ObjectInspectPane
            selection={selection}
            nodes={nodes}
            graphContext={
              exploreMode === 'graph' && currentGraphId
                ? {
                    graphId: currentGraphId,
                    graphVersion: viewingVersion,
                    annotations:
                      context.kind === 'graph' ? context.annotations ?? {} : {},
                    entityCount: nodes.length,
                    edgeCount: links.length,
                  }
                : null
            }
            fieldKindsByTypeVersion={fieldKindsByTypeVersion}
            onSelectNode={selectNodeFromCanvas}
            onClearSelection={() => handleSelect(null)}
            endpointLabel={endpointLabel}
          />
        </Paper>
      </Group>
    </Stack>
  )
}
