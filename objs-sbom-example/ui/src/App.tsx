import { useMemo, useRef, useState } from 'react'
import {
  Alert,
  Badge,
  Box,
  Button,
  Code,
  Group,
  Paper,
  ScrollArea,
  Stack,
  Text,
  Textarea,
  Title,
} from '@mantine/core'
import { GraphCanvas, type GraphCanvasHandle } from './GraphCanvas'
import { fetchGraph, toGraphData } from './api'
import { colorForType } from './color'
import type { GraphLink, GraphNode, GraphSelection } from './types'

const DEFAULT_ANNOTATIONS = `{
  "app": "payments-api",
  "appVersion": "2.3.1"
}`

export default function App() {
  const graphRef = useRef<GraphCanvasHandle>(null)
  const [annotationText, setAnnotationText] = useState(DEFAULT_ANNOTATIONS)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [nodes, setNodes] = useState<GraphNode[]>([])
  const [links, setLinks] = useState<GraphLink[]>([])
  const [selection, setSelection] = useState<GraphSelection | null>(null)

  const types = useMemo(() => {
    const set = new Map<string, string>()
    for (const n of nodes) {
      if (!set.has(n.type)) set.set(n.type, colorForType(n.type))
    }
    return [...set.entries()].sort(([a], [b]) => a.localeCompare(b))
  }, [nodes])

  async function onExec() {
    setLoading(true)
    setError(null)
    setSelection(null)
    try {
      const subgraph = await fetchGraph(annotationText)
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
    <Box style={{ height: '100vh', display: 'flex', flexDirection: 'column' }} p="md">
      <Stack gap="sm" style={{ flex: '0 0 auto' }}>
        <Title order={3}>Graph explorer</Title>
        <Text size="sm" c="dimmed">
          Query foundation <Code>GET /api/v1/objs/graph</Code> with a match-all annotation object.
        </Text>
        <Group align="flex-end" wrap="nowrap" gap="sm">
          <Textarea
            style={{ flex: 1 }}
            label="Annotations (JSON object)"
            autosize
            minRows={2}
            maxRows={12}
            value={annotationText}
            onChange={(e) => setAnnotationText(e.currentTarget.value)}
            styles={{ input: { fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace' } }}
          />
          <Button onClick={onExec} loading={loading} size="md">
            Exec
          </Button>
          <Button
            variant="light"
            size="md"
            disabled={nodes.length === 0}
            onClick={() => graphRef.current?.applyLayout()}
          >
            Apply layout
          </Button>
        </Group>
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
              <Badge key={type} color="gray" variant="outline" leftSection={<span style={{ color }}>●</span>}>
                {type}
              </Badge>
            ))}
          </Group>
        )}
      </Stack>

      <Group
        align="stretch"
        grow
        preventGrowOverflow={false}
        style={{ flex: 1, minHeight: 0, marginTop: 12 }}
        gap="md"
      >
        <Paper withBorder style={{ flex: 2, minHeight: 360, overflow: 'hidden', position: 'relative' }}>
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
            />
          )}
        </Paper>

        <Paper withBorder p="md" style={{ flex: 1, minWidth: 280, maxWidth: 420, overflow: 'hidden' }}>
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
                  <Badge variant="light" style={{ color: selection.node.color }}>
                    {selection.node.type}
                  </Badge>
                  <Text span size="sm" c="dimmed" ml="xs">
                    schema {selection.node.schemaVersion}
                  </Text>
                </Text>
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
                  {selection.edge.type ?? '—'}
                  {selection.edge.schemaVersion && (
                    <Text span size="sm" c="dimmed" ml="xs">
                      schema {selection.edge.schemaVersion}
                    </Text>
                  )}
                </Text>
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
    </Box>
  )
}
