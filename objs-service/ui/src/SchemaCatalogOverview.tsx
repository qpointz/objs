import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Button,
  Group,
  Loader,
  Menu,
  Paper,
  SegmentedControl,
  Select,
  Stack,
  Switch,
  Table,
  Tabs,
  Text,
  Title,
} from '@mantine/core'
import {
  Background,
  Controls,
  Handle,
  Position,
  ReactFlow,
  ReactFlowProvider,
  useEdgesState,
  useNodesState,
  useReactFlow,
  type Edge,
  type Node,
  type NodeProps,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { useNavigate } from 'react-router-dom'
import {
  DEFAULT_JSON_SCHEMA_EXPORT_OPTIONS,
  exportCatalog,
  importCatalogSeed,
  listEdges,
  schemaDetailPath,
  type CatalogExportFormat,
  type JsonSchemaExportOptions,
} from './api'
import {
  catalogSeedContainsGraph,
  catalogSeedObjectTypes,
  catalogSeedTypeRevealQuery,
  jsonSchemaDefKeys,
  jsonSchemaDefRevealQuery,
  schemaCatalogElements,
  type CatalogLayout,
  type CatalogNodeData,
  type CatalogTypeNode,
} from './catalogOverviewModel'
import { SyntaxCodeEditor, type SyntaxCodeEditorHandle } from './SyntaxCodeEditor'
import type { BoMAllowedEdgeRule, BoMSchema, SeedImportResult } from './types'

const FULL_SCHEMA_LAYOUT_KEY = 'objs.ui.fullSchema.layout'

const CATALOG_LAYOUTS: { value: CatalogLayout; label: string }[] = [
  { value: 'TB', label: 'Top to bottom' },
  { value: 'LR', label: 'Left to right' },
  { value: 'BT', label: 'Bottom to top' },
  { value: 'RL', label: 'Right to left' },
]

type StoredCatalogLayout = {
  direction: CatalogLayout
  positions: Record<string, { x: number; y: number }>
}

function isCatalogLayout(value: unknown): value is CatalogLayout {
  return value === 'TB' || value === 'LR' || value === 'BT' || value === 'RL'
}

function loadCatalogLayout(): StoredCatalogLayout {
  try {
    const raw = localStorage.getItem(FULL_SCHEMA_LAYOUT_KEY)
    if (!raw) return { direction: 'LR', positions: {} }
    const parsed = JSON.parse(raw) as unknown
    // Legacy: bare id → {x,y} map
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed) && !('direction' in parsed)) {
      const positions: StoredCatalogLayout['positions'] = {}
      for (const [id, pos] of Object.entries(parsed as Record<string, unknown>)) {
        if (
          pos &&
          typeof pos === 'object' &&
          typeof (pos as { x?: unknown }).x === 'number' &&
          typeof (pos as { y?: unknown }).y === 'number'
        ) {
          positions[id] = { x: (pos as { x: number }).x, y: (pos as { y: number }).y }
        }
      }
      return { direction: 'LR', positions }
    }
    const session = parsed as Partial<StoredCatalogLayout>
    const direction = isCatalogLayout(session.direction) ? session.direction : 'LR'
    const positions: StoredCatalogLayout['positions'] = {}
    if (session.positions && typeof session.positions === 'object') {
      for (const [id, pos] of Object.entries(session.positions)) {
        if (
          pos &&
          typeof pos === 'object' &&
          typeof pos.x === 'number' &&
          typeof pos.y === 'number'
        ) {
          positions[id] = { x: pos.x, y: pos.y }
        }
      }
    }
    return { direction, positions }
  } catch {
    return { direction: 'LR', positions: {} }
  }
}

function saveCatalogLayout(session: StoredCatalogLayout) {
  try {
    localStorage.setItem(FULL_SCHEMA_LAYOUT_KEY, JSON.stringify(session))
  } catch {
    // ignore quota / private mode
  }
}

function saveCatalogPositions(nodes: Node[], direction: CatalogLayout) {
  saveCatalogLayout({
    direction,
    positions: Object.fromEntries(nodes.map((n) => [n.id, { x: n.position.x, y: n.position.y }])),
  })
}

function clearCatalogPositions(direction: CatalogLayout) {
  saveCatalogLayout({ direction, positions: {} })
}

function applyCatalogPositions(
  nodes: Node[],
  stored: Record<string, { x: number; y: number }>,
): Node[] {
  if (Object.keys(stored).length === 0) return nodes
  return nodes.map((n) => {
    const pos = stored[n.id]
    return pos ? { ...n, position: { x: pos.x, y: pos.y } } : n
  })
}

function CatalogTypeNodeView({ data, sourcePosition, targetPosition }: NodeProps) {
  const node = data as CatalogNodeData
  const wildcard = node.kind === 'wildcard'
  return (
    <div
      style={{
        width: 168,
        borderRadius: 8,
        border: wildcard
          ? '1px dashed var(--mantine-color-gray-5)'
          : '2px solid var(--mantine-color-blue-6)',
        background: 'var(--mantine-color-body)',
        padding: '8px 10px',
        fontFamily: 'system-ui, sans-serif',
        cursor: wildcard ? 'default' : 'pointer',
      }}
    >
      <Handle
        type="target"
        position={targetPosition ?? Position.Left}
        style={{ background: '#1971c2' }}
      />
      <Handle
        type="source"
        position={sourcePosition ?? Position.Right}
        style={{ background: '#1971c2' }}
      />
      <div style={{ fontSize: 11, fontWeight: 700, opacity: 0.65 }}>
        {wildcard ? 'ANY TYPE' : 'OBJECT'}
      </div>
      <div style={{ fontSize: 14, fontWeight: 700 }}>{wildcard ? 'Any type (*)' : node.type}</div>
      {node.version && (
        <div style={{ fontSize: 11, opacity: 0.75 }}>latest {node.version}</div>
      )}
    </div>
  )
}

const nodeTypes = { catalogType: memo(CatalogTypeNodeView) }

function SchemaCatalogOverviewInner({
  entityTypes,
  rules,
  busy,
  onRefresh,
  onImported,
}: {
  entityTypes: CatalogTypeNode[]
  rules: BoMAllowedEdgeRule[]
  busy?: boolean
  onRefresh: () => Promise<void>
  onImported: () => Promise<void>
}) {
  const navigate = useNavigate()
  const { fitView } = useReactFlow()
  const fileRef = useRef<HTMLInputElement>(null)
  const [ioError, setIoError] = useState<string | null>(null)
  const [importResult, setImportResult] = useState<SeedImportResult | null>(null)
  const [ioBusy, setIoBusy] = useState(false)
  const [overviewTab, setOverviewTab] = useState<'visual' | 'text'>('visual')
  const [textFormat, setTextFormat] = useState<CatalogExportFormat>('json-schema')
  const [jsonSchemaOptions, setJsonSchemaOptions] = useState<Required<JsonSchemaExportOptions>>(
    () => ({ ...DEFAULT_JSON_SCHEMA_EXPORT_OPTIONS }),
  )
  const [textBody, setTextBody] = useState('')
  const [textBusy, setTextBusy] = useState(false)
  const [textError, setTextError] = useState<string | null>(null)
  const [textEpoch, setTextEpoch] = useState(0)
  const textEditorRef = useRef<SyntaxCodeEditorHandle>(null)
  const [jumpTo, setJumpTo] = useState<string | null>(null)
  const fittedOnceRef = useRef(false)
  const [storedSession] = useState(() => loadCatalogLayout())
  const [layout, setLayout] = useState<CatalogLayout>(() => storedSession.direction)
  const layoutRef = useRef(layout)
  layoutRef.current = layout

  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])

  useEffect(() => {
    const session = loadCatalogLayout()
    const next = schemaCatalogElements(entityTypes, rules, session.direction)
    const laidOut = applyCatalogPositions(next.nodes, session.positions)
    setLayout(session.direction)
    setNodes(laidOut)
    setEdges(next.edges)
    const restored = laidOut.some((n) => session.positions[n.id] != null)
    if (!fittedOnceRef.current || !restored) {
      fittedOnceRef.current = true
      requestAnimationFrame(() => fitView({ padding: 0.18, maxZoom: 1, duration: 200 }))
    }
  }, [entityTypes, rules, setNodes, setEdges, fitView])

  const persistLayout = useCallback((rfNodes: Node[]) => {
    saveCatalogPositions(rfNodes, layoutRef.current)
  }, [])

  const applyLayout = useCallback(
    (nextLayout: CatalogLayout = layout) => {
      clearCatalogPositions(nextLayout)
      setLayout(nextLayout)
      layoutRef.current = nextLayout
      const next = schemaCatalogElements(entityTypes, rules, nextLayout)
      setNodes(next.nodes)
      setEdges(next.edges)
      requestAnimationFrame(() => fitView({ padding: 0.18, maxZoom: 1, duration: 300 }))
    },
    [entityTypes, rules, layout, setNodes, setEdges, fitView],
  )

  const loadTextView = useCallback(
    async (format: CatalogExportFormat, options: Required<JsonSchemaExportOptions>) => {
      setTextBusy(true)
      setTextError(null)
      try {
        setTextBody(await exportCatalog(format, format === 'json-schema' ? options : undefined))
      } catch (e) {
        setTextBody('')
        setTextError(e instanceof Error ? e.message : String(e))
      } finally {
        setTextBusy(false)
      }
    },
    [],
  )

  useEffect(() => {
    if (overviewTab !== 'text') return
    setJumpTo(null)
    void loadTextView(textFormat, jsonSchemaOptions)
  }, [overviewTab, textFormat, jsonSchemaOptions, textEpoch, loadTextView, entityTypes, rules])

  async function onExport(format: CatalogExportFormat) {
    setIoError(null)
    setIoBusy(true)
    try {
      const body = await exportCatalog(format, format === 'json-schema' ? jsonSchemaOptions : undefined)
      const blob = new Blob([body], {
        type: format === 'json-schema' ? 'application/schema+json' : 'application/yaml',
      })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = format === 'json-schema' ? 'objs-catalog.schema.json' : 'objs-catalog.yaml'
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setIoError(e instanceof Error ? e.message : String(e))
    } finally {
      setIoBusy(false)
    }
  }

  const jsonSchemaHint = `${jsonSchemaOptions.includeEdges} · ${jsonSchemaOptions.dialect}`

  const jumpTargets = useMemo(() => {
    if (!textBody.trim()) return [] as { value: string; label: string }[]
    if (textFormat === 'json-schema') {
      return jsonSchemaDefKeys(textBody).map((k) => ({ value: k, label: k }))
    }
    return catalogSeedObjectTypes(textBody).map((t) => ({ value: t, label: t }))
  }, [textBody, textFormat])

  function onJumpTo(key: string | null) {
    setJumpTo(key)
    if (!key) return
    const query =
      textFormat === 'json-schema' ? jsonSchemaDefRevealQuery(key) : catalogSeedTypeRevealQuery(key)
    const ok = textEditorRef.current?.revealText(query)
    if (!ok && textFormat === 'seeds') {
      // Unquoted YAML type lines
      textEditorRef.current?.revealText(`type: ${key}`)
    }
  }

  async function onImportFile(file: File) {
    setIoError(null)
    setImportResult(null)
    setIoBusy(true)
    try {
      const text = await file.text()
      if (catalogSeedContainsGraph(text)) {
        setIoError(
          'This file contains Graph seed documents. Overview import accepts catalog definitions only (ObjectSchema and AllowedEdgeRule).',
        )
        return
      }
      const result = await importCatalogSeed(file)
      setImportResult(result)
      if (result.documents.some((d) => (d.errors?.length ?? 0) > 0)) {
        setIoError('Import finished with errors (MERGE only; nothing was deleted).')
      } else {
        await onImported()
        setTextEpoch((n) => n + 1)
      }
    } catch (e) {
      setIoError(e instanceof Error ? e.message : String(e))
    } finally {
      setIoBusy(false)
    }
  }

  return (
    <Stack gap="sm" style={{ height: '100%', minHeight: 0, flex: 1 }}>
      <Group justify="space-between" align="flex-start" style={{ flexShrink: 0 }}>
        <Stack gap={2}>
          <Title order={3}>Full schema</Title>
          <Text size="sm" c="dimmed">
            Catalog overview of object types and allowed edges. Open a type to edit. Import is MERGE
            only (no deletes).
          </Text>
        </Stack>
        <Group gap="xs">
          <Group gap={0}>
            <Button
              size="sm"
              variant="light"
              disabled={nodes.length === 0}
              onClick={() => applyLayout()}
              style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
            >
              Apply layout
            </Button>
            <Menu position="bottom-end" withinPortal>
              <Menu.Target>
                <Button
                  size="sm"
                  variant="light"
                  disabled={nodes.length === 0}
                  aria-label="Choose catalog layout"
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
                {CATALOG_LAYOUTS.map((option) => (
                  <Menu.Item
                    key={option.value}
                    onClick={() => applyLayout(option.value)}
                  >
                    {option.value === layout ? '✓ ' : ''}
                    {option.label}
                  </Menu.Item>
                ))}
              </Menu.Dropdown>
            </Menu>
          </Group>
          <Button
            size="sm"
            variant="light"
            loading={busy || ioBusy}
            onClick={() => {
              void onRefresh().then(() => setTextEpoch((n) => n + 1))
            }}
          >
            Refresh
          </Button>
          <Menu position="bottom-end">
            <Menu.Target>
              <Button size="sm" variant="light" loading={ioBusy}>
                Export
              </Button>
            </Menu.Target>
            <Menu.Dropdown>
              <Menu.Label>Format</Menu.Label>
              <Menu.Item onClick={() => void onExport('seeds')}>Seeds (YAML)</Menu.Item>
              <Menu.Item onClick={() => void onExport('json-schema')}>
                JSON Schema
                <Text span size="xs" c="dimmed" ml={8}>
                  {jsonSchemaHint}
                </Text>
              </Menu.Item>
            </Menu.Dropdown>
          </Menu>
          <Button size="sm" loading={ioBusy} onClick={() => fileRef.current?.click()}>
            Import
          </Button>
          <input
            ref={fileRef}
            type="file"
            accept=".yaml,.yml,text/yaml,application/yaml"
            style={{ display: 'none' }}
            onChange={(e) => {
              const file = e.currentTarget.files?.[0]
              e.currentTarget.value = ''
              if (file) void onImportFile(file)
            }}
          />
        </Group>
      </Group>

      {ioError && (
        <Alert color="red" title="Catalog I/O" style={{ flexShrink: 0 }}>
          {ioError}
        </Alert>
      )}

      {importResult && (
        <Paper withBorder p="sm" style={{ flexShrink: 0, maxHeight: 220, overflow: 'auto' }}>
          <Text size="sm" fw={600} mb="xs">
            Import result
          </Text>
          <Table striped withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>#</Table.Th>
                <Table.Th>Kind</Table.Th>
                <Table.Th>Identity</Table.Th>
                <Table.Th>Status</Table.Th>
                <Table.Th>Errors</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {importResult.documents.map((doc) => (
                <Table.Tr key={doc.index}>
                  <Table.Td>{doc.index}</Table.Td>
                  <Table.Td>{doc.kind ?? '—'}</Table.Td>
                  <Table.Td>{doc.identity ?? '—'}</Table.Td>
                  <Table.Td>
                    {doc.applied ? 'applied' : doc.skipped ? 'skipped' : 'failed'}
                  </Table.Td>
                  <Table.Td>
                    {(doc.errors ?? []).map((err) => err.message).join('; ') || '—'}
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Paper>
      )}

      <Tabs
        value={overviewTab}
        onChange={(v) => setOverviewTab((v as 'visual' | 'text') ?? 'visual')}
        style={{
          flex: 1,
          minHeight: 0,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
        }}
      >
        <Tabs.List style={{ flexShrink: 0 }}>
          <Tabs.Tab value="visual">Visual</Tabs.Tab>
          <Tabs.Tab value="text">Text</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel
          value="visual"
          pt="sm"
          style={{
            flex: 1,
            minHeight: 0,
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
          }}
        >
          <div
            style={{
              flex: 1,
              minHeight: 0,
              height: '100%',
              border: '1px solid var(--mantine-color-default-border)',
              borderRadius: 6,
              overflow: 'hidden',
            }}
          >
            <ReactFlow
              nodes={nodes}
              edges={edges}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
              nodeTypes={nodeTypes}
              fitViewOptions={{ padding: 0.18, maxZoom: 1 }}
              minZoom={0.2}
              maxZoom={1.4}
              nodesDraggable
              nodesConnectable={false}
              elementsSelectable
              panOnDrag
              panOnScroll={false}
              zoomOnScroll={false}
              zoomOnPinch
              onNodeDragStop={(_event, _node, dragged) => persistLayout(dragged)}
              onNodeClick={(_, node) => {
                const data = node.data as CatalogNodeData
                if (data.kind === 'entity' && data.version) {
                  navigate(schemaDetailPath(data.type, data.version))
                }
              }}
              proOptions={{ hideAttribution: true }}
            >
              <Background gap={18} size={1} />
              <Controls showInteractive={false} />
            </ReactFlow>
          </div>
        </Tabs.Panel>

        <Tabs.Panel
          value="text"
          pt="sm"
          style={{
            flex: 1,
            minHeight: 0,
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
          }}
        >
          <Stack gap="xs" style={{ flex: 1, minHeight: 0, height: '100%' }}>
            <Group justify="space-between" style={{ flexShrink: 0 }} wrap="wrap">
              <Group gap="sm" wrap="wrap">
                <SegmentedControl
                  size="xs"
                  value={textFormat}
                  onChange={(v) => setTextFormat(v as CatalogExportFormat)}
                  data={[
                    { label: 'JSON Schema', value: 'json-schema' },
                    { label: 'Seeds', value: 'seeds' },
                  ]}
                />
                {textFormat === 'json-schema' && (
                  <>
                    <SegmentedControl
                      size="xs"
                      value={jsonSchemaOptions.includeEdges}
                      onChange={(v) =>
                        setJsonSchemaOptions((prev) => ({
                          ...prev,
                          includeEdges: v as Required<JsonSchemaExportOptions>['includeEdges'],
                        }))
                      }
                      data={[
                        { label: 'None', value: 'none' },
                        { label: 'Outbound', value: 'outbound' },
                        { label: 'Linked', value: 'linked' },
                      ]}
                    />
                    <Switch
                      size="xs"
                      label="Edge property schemas"
                      checked={jsonSchemaOptions.includeEdgePropertySchemas}
                      disabled={jsonSchemaOptions.includeEdges === 'none'}
                      onChange={(e) =>
                        setJsonSchemaOptions((prev) => ({
                          ...prev,
                          includeEdgePropertySchemas: e.currentTarget.checked,
                        }))
                      }
                    />
                    <Select
                      size="xs"
                      w={110}
                      label=""
                      aria-label="JSON Schema dialect"
                      data={[{ value: '2020-12', label: '2020-12' }]}
                      value={jsonSchemaOptions.dialect}
                      onChange={(v) =>
                        setJsonSchemaOptions((prev) => ({
                          ...prev,
                          dialect: (v as '2020-12') ?? '2020-12',
                        }))
                      }
                      allowDeselect={false}
                    />
                  </>
                )}
              </Group>
              <Group gap="xs" wrap="nowrap" style={{ flexShrink: 0 }}>
                {jumpTargets.length > 0 && (
                  <Select
                    size="xs"
                    w={200}
                    searchable
                    clearable
                    placeholder="Go to type…"
                    data={jumpTargets}
                    value={jumpTo}
                    onChange={onJumpTo}
                    nothingFoundMessage="No match"
                  />
                )}
                {textBusy && <Loader size="xs" />}
              </Group>
            </Group>
            {textError && (
              <Alert color="red" title="Text view" style={{ flexShrink: 0 }}>
                {textError}
              </Alert>
            )}
            <Text size="xs" c="dimmed" style={{ flexShrink: 0 }}>
              Ctrl/Cmd+F to search in the document
            </Text>
            <div
              style={{
                flex: 1,
                minHeight: 0,
                display: 'flex',
                flexDirection: 'column',
              }}
            >
              <SyntaxCodeEditor
                ref={textEditorRef}
                language={textFormat === 'json-schema' ? 'json' : 'yaml'}
                readOnly
                minHeight={240}
                fillHeight
                value={textBody}
              />
            </div>
          </Stack>
        </Tabs.Panel>
      </Tabs>
    </Stack>
  )
}

export function SchemaCatalogOverview({
  schemas,
  onCatalogChanged,
}: {
  schemas: BoMSchema[]
  onCatalogChanged: () => Promise<void>
}) {
  const [rules, setRules] = useState<BoMAllowedEdgeRule[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const entityTypes = useMemo(() => {
    const byType = new Map<string, string[]>()
    for (const schema of schemas) {
      if (!schema.usages.includes('ENTITY')) continue
      const list = byType.get(schema.type) ?? []
      list.push(schema.version)
      byType.set(schema.type, list)
    }
    return [...byType.entries()]
      .map(([type, versions]) => ({
        type,
        version: [...versions].sort().at(-1) ?? '1.0.0',
      }))
      .sort((a, b) => a.type.localeCompare(b.type))
  }, [schemas])

  async function reloadRules() {
    setLoading(true)
    setError(null)
    try {
      setRules(await listEdges())
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
      setRules([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reloadRules()
  }, [schemas])

  if (loading && !rules) {
    return <Loader size="sm" />
  }

  return (
    <Stack gap="sm" style={{ height: '100%', minHeight: 0, flex: 1 }}>
      {error && (
        <Alert color="red" title="Failed to load edges" style={{ flexShrink: 0 }}>
          {error}
        </Alert>
      )}
      <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
        <ReactFlowProvider>
          <SchemaCatalogOverviewInner
            entityTypes={entityTypes}
            rules={rules ?? []}
            busy={loading}
            onRefresh={async () => {
              await onCatalogChanged()
              await reloadRules()
            }}
            onImported={async () => {
              await onCatalogChanged()
              await reloadRules()
            }}
          />
        </ReactFlowProvider>
      </div>
    </Stack>
  )
}
