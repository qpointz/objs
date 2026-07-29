import { useMemo, useRef, useState } from 'react'
import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Code,
  Group,
  Menu,
  Paper,
  ScrollArea,
  SegmentedControl,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
  Anchor,
} from '@mantine/core'
import { Link } from 'react-router-dom'
import {
  GraphCanvas,
  type GraphCanvasHandle,
  type GraphLayout,
} from './GraphCanvas'
import { queryGraph, schemaDetailPath, toGraphData } from './api'
import { colorForType } from './color'
import type { GraphLink, GraphNode, GraphSelection } from './types'

type MatcherMode = 'anno' | 'anno-expr' | 'chained'

type AnnoRow = { key: string; value: string }

const DEFAULT_ANNO_ROWS: AnnoRow[] = [
  { key: 'app', value: 'payments-api' },
  { key: 'appVersion', value: '2.3.1' },
]

const DEFAULT_EXPR = "app == 'payments-api' && appVersion == '2.3.1'"

const DEFAULT_CHAINED = `[
  { "anno": { "app": "payments-api" } },
  { "anno-expr": "appVersion == '2.3.1'" }
]`

const GRAPH_LAYOUTS: { value: GraphLayout; label: string }[] = [
  { value: 'TB', label: 'Top to bottom' },
  { value: 'LR', label: 'Left to right' },
  { value: 'BT', label: 'Bottom to top' },
  { value: 'RL', label: 'Right to left' },
]

export function GraphExplorerPage() {
  const graphRef = useRef<GraphCanvasHandle>(null)
  const [mode, setMode] = useState<MatcherMode>('anno')
  const [annoRows, setAnnoRows] = useState<AnnoRow[]>(DEFAULT_ANNO_ROWS)
  const [exprText, setExprText] = useState(DEFAULT_EXPR)
  const [chainedText, setChainedText] = useState(DEFAULT_CHAINED)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [nodes, setNodes] = useState<GraphNode[]>([])
  const [links, setLinks] = useState<GraphLink[]>([])
  const [selection, setSelection] = useState<GraphSelection | null>(null)
  const [layout, setLayout] = useState<GraphLayout>('TB')

  const types = useMemo(() => {
    const set = new Map<string, string>()
    for (const n of nodes) {
      if (!set.has(n.type)) set.set(n.type, colorForType(n.type))
    }
    return [...set.entries()].sort(([a], [b]) => a.localeCompare(b))
  }, [nodes])

  function buildMatcherBody(): unknown {
    if (mode === 'anno') {
      const filter: Record<string, string> = {}
      for (const row of annoRows) {
        const key = row.key.trim()
        if (!key) continue
        filter[key] = row.value
      }
      if (Object.keys(filter).length === 0) {
        throw new Error('Provide at least one annotation key/value')
      }
      return { anno: filter }
    }
    if (mode === 'anno-expr') {
      const expression = exprText.trim()
      if (!expression) {
        throw new Error('Provide a non-blank anno-expr expression')
      }
      return { 'anno-expr': expression }
    }
    let parsed: unknown
    try {
      parsed = JSON.parse(chainedText)
    } catch {
      throw new Error('Chained matcher must be valid JSON')
    }
    if (!Array.isArray(parsed) || parsed.length === 0) {
      throw new Error('Chained matcher must be a non-empty JSON array')
    }
    return parsed
  }

  async function onExec() {
    setLoading(true)
    setError(null)
    setSelection(null)
    try {
      const body = buildMatcherBody()
      const subgraph = await queryGraph(body)
      const graph = toGraphData(subgraph)
      setNodes(graph.nodes)
      setLinks(graph.links)
    } catch (e) {
      setNodes([])
      setLinks([])
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
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
        <Group>
          <Button onClick={onExec} loading={loading}>
            Exec
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
                        setLayout(option.value)
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

      <SegmentedControl
        value={mode}
        onChange={(value) => setMode(value as MatcherMode)}
        data={[
          { label: 'anno', value: 'anno' },
          { label: 'anno-expr', value: 'anno-expr' },
          { label: 'chained', value: 'chained' },
        ]}
      />

      {mode === 'anno' && (
        <Stack gap="xs">
          {annoRows.map((row, index) => (
            <Group key={index} align="flex-end" wrap="nowrap">
              <TextInput
                label={index === 0 ? 'Key' : undefined}
                value={row.key}
                onChange={(e) => {
                  const next = [...annoRows]
                  next[index] = { ...next[index], key: e.currentTarget.value }
                  setAnnoRows(next)
                }}
                style={{ flex: 1 }}
              />
              <TextInput
                label={index === 0 ? 'Value' : undefined}
                value={row.value}
                onChange={(e) => {
                  const next = [...annoRows]
                  next[index] = { ...next[index], value: e.currentTarget.value }
                  setAnnoRows(next)
                }}
                style={{ flex: 1 }}
              />
              <ActionIcon
                variant="subtle"
                color="red"
                aria-label="Remove annotation row"
                onClick={() => setAnnoRows(annoRows.filter((_, i) => i !== index))}
                disabled={annoRows.length <= 1}
              >
                ×
              </ActionIcon>
            </Group>
          ))}
          <Button
            variant="light"
            size="xs"
            w="fit-content"
            onClick={() => setAnnoRows([...annoRows, { key: '', value: '' }])}
          >
            Add key/value
          </Button>
        </Stack>
      )}

      {mode === 'anno-expr' && (
        <TextInput
          label="Expression"
          description="Direct annotation variables, e.g. app == 'payments-api'"
          value={exprText}
          onChange={(e) => setExprText(e.currentTarget.value)}
          styles={{ input: { fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace' } }}
        />
      )}

      {mode === 'chained' && (
        <Textarea
          label="Chained matchers (JSON array)"
          autosize
          minRows={4}
          maxRows={10}
          value={chainedText}
          onChange={(e) => setChainedText(e.currentTarget.value)}
          styles={{ input: { fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace' } }}
        />
      )}

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
              to={`/schemas/${encodeURIComponent(type)}`}
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
          {nodes.length === 0 && !loading ? (
            <Text c="dimmed" p="md">
              Press Exec to load a subgraph.
            </Text>
          ) : (
            <GraphCanvas
              ref={graphRef}
              nodes={nodes}
              links={links}
              selection={selection}
              onSelect={setSelection}
              layout={layout}
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
