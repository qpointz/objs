import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Code,
  Group,
  Loader,
  Menu,
  Paper,
  ScrollArea,
  Stack,
  Text,
  Title,
  Anchor,
} from '@mantine/core'
import { Link, useNavigate } from 'react-router-dom'
import {
  GraphCanvas,
  type GraphCanvasHandle,
  type GraphLayout,
  type GraphNodePositions,
} from './GraphCanvas'
import { queryGraph, schemaDetailPath, toGraphData } from './api'
import { colorForType } from './color'
import {
  MatcherQueryForm,
  type MatcherQueryFormHandle,
} from './MatcherQueryForm'
import type { QueryExecStats } from './queryExecStats'
import type { GraphLink, GraphNode } from './types'
import { newGraphQueryId, useGraphSelectionHistory } from './useGraphSelectionHistory'

const GRAPH_MATCHER_STORAGE_KEY = 'objs.ui.graphExplorer.matcher'
const GRAPH_SESSION_STORAGE_KEY = 'objs.ui.graphExplorer.session'

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

function loadStoredGraphMatcher(): unknown | null {
  try {
    const raw = localStorage.getItem(GRAPH_MATCHER_STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw) as unknown
  } catch {
    return null
  }
}

function saveStoredGraphMatcher(body: unknown) {
  try {
    localStorage.setItem(GRAPH_MATCHER_STORAGE_KEY, JSON.stringify(body))
  } catch {
    // ignore quota / private mode
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
  const matcherRef = useRef<MatcherQueryFormHandle>(null)
  const [storedMatcher] = useState(() => loadStoredGraphMatcher())
  const [storedSession] = useState(() => loadStoredGraphSession())
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [execStats, setExecStats] = useState<QueryExecStats | null>(null)
  const [nodes, setNodes] = useState<GraphNode[]>(() => storedSession?.nodes ?? [])
  const [links, setLinks] = useState<GraphLink[]>(() => storedSession?.links ?? [])
  const [layout, setLayout] = useState<GraphLayout>(() => storedSession?.layout ?? 'TB')
  const [lastMatcher, setLastMatcher] = useState<unknown>(() => storedMatcher)
  const [canvasEpoch, setCanvasEpoch] = useState(0)
  const linksRef = useRef(links)
  linksRef.current = links
  const layoutRef = useRef(layout)
  layoutRef.current = layout
  const queryIdRef = useRef<string | null>(null)

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

  const types = useMemo(() => {
    const set = new Map<string, string>()
    for (const n of nodes) {
      if (!set.has(n.type)) set.set(n.type, colorForType(n.type))
    }
    return [...set.entries()].sort(([a], [b]) => a.localeCompare(b))
  }, [nodes])

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

  async function onExec() {
    setLoading(true)
    setError(null)
    setFormError(null)
    try {
      const body = matcherRef.current?.build()
      if (body === undefined) {
        throw new Error('Matcher form is not ready')
      }
      const started = performance.now()
      const subgraph = await queryGraph(body)
      const durationMs = performance.now() - started
      const graph = toGraphData(subgraph)
      const qid = beginQueryResult()
      setExecStats({
        durationMs,
        nodes: graph.nodes.length,
        edges: graph.links.length,
      })
      // Drop prior canvas coordinates so Exec starts from a fresh layout.
      setNodes(graph.nodes)
      setLinks(graph.links)
      setLastMatcher(body)
      saveStoredGraphMatcher(body)
      clearStoredGraphSession()
      persistSession(graph.nodes, graph.links, layout, qid)
      setCanvasEpoch((n) => n + 1)
    } catch (e) {
      setNodes([])
      setLinks([])
      setExecStats(null)
      clearStoredGraphSession()
      const message = e instanceof Error ? e.message : String(e)
      if (
        message.includes('annotation') ||
        message.includes('anno-expr') ||
        message.includes('Chained') ||
        message.includes('JSON') ||
        message.includes('Matcher form') ||
        message.includes('Provide')
      ) {
        setFormError(message)
      } else {
        setError(message)
      }
    } finally {
      setLoading(false)
    }
  }

  function onEditInLinter() {
    if (lastMatcher == null) return
    navigate('/composer', { state: { matcher: lastMatcher } })
  }

  function changeLayout(next: GraphLayout) {
    setLayout(next)
    layoutRef.current = next
    graphRef.current?.applyLayout(next)
  }

  function selectNodeFromCanvas(nodeId: string) {
    const node = nodes.find((n) => n.id === nodeId)
    if (!node) return
    select({ kind: 'node', node })
    requestAnimationFrame(() => graphRef.current?.focusNode(nodeId))
  }

  function endpointLabel(nodeId: string): string {
    const node = nodes.find((n) => n.id === nodeId)
    return node ? `${node.name} (${node.type})` : nodeId
  }

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0 }}>
      <Group justify="space-between" align="flex-end" wrap="wrap">
        <div>
          <Title order={3}>Graph explorer</Title>
          <Text size="sm" c="dimmed">
            Query <Code>POST /api/v1/objs/graph/query</Code>. Open a type to inspect its object model in Schema
            explorer.
          </Text>
        </div>
        <Group gap="xs">
          <Button
            variant="light"
            disabled={lastMatcher == null}
            onClick={onEditInLinter}
          >
            Edit in linter
          </Button>
          <Group gap={0}>
            <Button
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

      <Paper withBorder p="sm">
        <MatcherQueryForm
          ref={matcherRef}
          emptyDefaults
          matcher={storedMatcher}
          error={formError}
          stats={execStats}
          action={
            <Button size="xs" onClick={() => void onExec()} loading={loading}>
              Exec
            </Button>
          }
        />
      </Paper>

      {error && (
        <Alert color="red" title="Query failed">
          {error}
        </Alert>
      )}

      {!error && (nodes.length > 0 || links.length > 0) && (
        <Group gap="xs">
          <Text size="sm">
            {nodes.length} nodes / {links.length} edges
          </Text>
          {types.map(([type, color]) => (
            <Badge
              key={type}
              component={Link}
              to={`/model/${encodeURIComponent(type)}`}
              color="gray"
              variant="outline"
              leftSection={<span style={{ color }}>●</span>}
              style={{ cursor: 'pointer', textDecoration: 'none' }}
            >
              {type}
            </Badge>
          ))}
        </Group>
      )}

      <Group align="stretch" grow preventGrowOverflow={false} style={{ flex: 1, minHeight: 0 }} gap="md">
        <Paper withBorder style={{ flex: 2, minHeight: 280, overflow: 'hidden', position: 'relative' }}>
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
                Fetching subgraph…
              </Text>
            </Stack>
          )}
          {nodes.length === 0 && !loading ? (
            <Text c="dimmed" p="md">
              Press Exec to load a subgraph.
            </Text>
          ) : (
            <GraphCanvas
              key={canvasEpoch}
              ref={graphRef}
              nodes={nodes}
              links={links}
              selection={selection}
              onSelect={select}
              layout={layout}
              autoLayoutOnDataChange={false}
              onPositionsChange={onPositionsChange}
            />
          )}
        </Paper>

        <Paper withBorder p="md" style={{ flex: 1, minWidth: 260, maxWidth: 420, overflow: 'hidden' }}>
          <ScrollArea h="100%">
            {selection?.kind === 'node' ? (
              <Stack gap="sm">
                <Badge color="blue" variant="light" w="fit-content">
                  node
                </Badge>
                <Title order={5}>{selection.node.name}</Title>
                <Text size="sm">
                  <Text span fw={600}>
                    type:{' '}
                  </Text>
                  <Anchor
                    component={Link}
                    to={schemaDetailPath(selection.node.type, selection.node.schemaVersion)}
                    size="sm"
                  >
                    <Badge variant="light" style={{ color: selection.node.color, cursor: 'pointer' }}>
                      {selection.node.type}
                    </Badge>
                  </Anchor>
                  <Text span size="sm" c="dimmed" ml="xs">
                    schema {selection.node.schemaVersion}
                  </Text>
                </Text>
                <Anchor
                  component={Link}
                  to={schemaDetailPath(selection.node.type, selection.node.schemaVersion)}
                  size="sm"
                >
                  Open object model in Schema explorer
                </Anchor>
                <Text size="xs" c="dimmed" style={{ wordBreak: 'break-all' }}>
                  id: {selection.node.id}
                </Text>
                <div>
                  <Text fw={600} size="sm" mb={4}>
                    annotations
                  </Text>
                  <Code block>{JSON.stringify(selection.node.annotations, null, 2)}</Code>
                </div>
                <div>
                  <Text fw={600} size="sm" mb={4}>
                    payload
                  </Text>
                  <Code block>{JSON.stringify(selection.node.payload, null, 2)}</Code>
                </div>
              </Stack>
            ) : selection?.kind === 'edge' ? (
              <Stack gap="sm">
                <Badge color="blue" variant="light" w="fit-content">
                  edge
                </Badge>
                <Title order={5}>{selection.edge.role}</Title>
                <Text size="sm">
                  <Text span fw={600}>
                    type:{' '}
                  </Text>
                  {selection.edge.type ? (
                    <Anchor
                      component={Link}
                      to={schemaDetailPath(
                        selection.edge.type,
                        selection.edge.schemaVersion ?? '1.0.0',
                      )}
                      size="sm"
                    >
                      {selection.edge.type}
                    </Anchor>
                  ) : (
                    '—'
                  )}
                  {selection.edge.schemaVersion && (
                    <Text span size="sm" c="dimmed" ml="xs">
                      schema {selection.edge.schemaVersion}
                    </Text>
                  )}
                </Text>
                {selection.edge.type && (
                  <Anchor
                    component={Link}
                    to={schemaDetailPath(
                      selection.edge.type,
                      selection.edge.schemaVersion ?? '1.0.0',
                    )}
                    size="sm"
                  >
                    Open edge property schema
                  </Anchor>
                )}
                <Text size="xs" c="dimmed" style={{ wordBreak: 'break-all' }}>
                  id: {selection.edge.id}
                </Text>
                <div>
                  <Text fw={600} size="sm" mb={4}>
                    endpoints
                  </Text>
                  <Stack gap={4}>
                    <Group gap={6} wrap="nowrap" align="flex-start">
                      <Text size="sm" fw={600} style={{ flexShrink: 0 }}>
                        source:
                      </Text>
                      <Anchor
                        component="button"
                        type="button"
                        size="sm"
                        ta="left"
                        style={{ wordBreak: 'break-all' }}
                        onClick={() => selectNodeFromCanvas(selection.edge.source)}
                      >
                        {endpointLabel(selection.edge.source)}
                      </Anchor>
                    </Group>
                    <Group gap={6} wrap="nowrap" align="flex-start">
                      <Text size="sm" fw={600} style={{ flexShrink: 0 }}>
                        target:
                      </Text>
                      <Anchor
                        component="button"
                        type="button"
                        size="sm"
                        ta="left"
                        style={{ wordBreak: 'break-all' }}
                        onClick={() => selectNodeFromCanvas(selection.edge.target)}
                      >
                        {endpointLabel(selection.edge.target)}
                      </Anchor>
                    </Group>
                  </Stack>
                </div>
                <div>
                  <Text fw={600} size="sm" mb={4}>
                    edge JSON
                  </Text>
                  <Code block>
                    {JSON.stringify(
                      {
                        id: selection.edge.id,
                        source: selection.edge.source,
                        target: selection.edge.target,
                        role: selection.edge.role,
                        type: selection.edge.type,
                        schemaVersion: selection.edge.schemaVersion,
                        properties: selection.edge.properties,
                      },
                      null,
                      2,
                    )}
                  </Code>
                </div>
              </Stack>
            ) : (
              <Text c="dimmed" size="sm">
                Select a node or edge to inspect JSON.
              </Text>
            )}
          </ScrollArea>
        </Paper>
      </Group>
    </Stack>
  )
}
