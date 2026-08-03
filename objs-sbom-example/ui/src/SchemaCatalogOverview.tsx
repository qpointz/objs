import { memo, useEffect, useMemo, useRef, useState } from 'react'
import { Alert, Button, Group, Loader, Paper, Stack, Table, Text, Title } from '@mantine/core'
import {
  Background,
  Controls,
  Handle,
  Position,
  ReactFlow,
  ReactFlowProvider,
  type NodeProps,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { useNavigate } from 'react-router-dom'
import {
  exportCatalogSeed,
  importCatalogSeed,
  listEdges,
  schemaDetailPath,
} from './api'
import {
  catalogSeedContainsGraph,
  schemaCatalogElements,
  type CatalogNodeData,
  type CatalogTypeNode,
} from './catalogOverviewModel'
import type { BoMAllowedEdgeRule, BoMSchema, SeedImportResult } from './types'

function CatalogTypeNodeView({ data }: NodeProps) {
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
      <Handle type="target" position={Position.Left} style={{ background: '#1971c2' }} />
      <Handle type="source" position={Position.Right} style={{ background: '#1971c2' }} />
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
  const fileRef = useRef<HTMLInputElement>(null)
  const [ioError, setIoError] = useState<string | null>(null)
  const [importResult, setImportResult] = useState<SeedImportResult | null>(null)
  const [ioBusy, setIoBusy] = useState(false)

  const elements = useMemo(
    () => schemaCatalogElements(entityTypes, rules),
    [entityTypes, rules],
  )

  async function onExport() {
    setIoError(null)
    setIoBusy(true)
    try {
      const yaml = await exportCatalogSeed()
      const blob = new Blob([yaml], { type: 'application/yaml' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'objs-catalog.yaml'
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setIoError(e instanceof Error ? e.message : String(e))
    } finally {
      setIoBusy(false)
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
      }
    } catch (e) {
      setIoError(e instanceof Error ? e.message : String(e))
    } finally {
      setIoBusy(false)
    }
  }

  return (
    <Stack gap="sm" style={{ height: '100%', minHeight: 0 }}>
      <Group justify="space-between" align="flex-start">
        <Stack gap={2}>
          <Title order={3}>Full schema</Title>
          <Text size="sm" c="dimmed">
            Catalog overview of object types and allowed edges. Open a type to edit. Import is MERGE
            only (no deletes).
          </Text>
        </Stack>
        <Group gap="xs">
          <Button size="sm" variant="light" loading={busy || ioBusy} onClick={() => void onRefresh()}>
            Refresh
          </Button>
          <Button size="sm" variant="light" loading={ioBusy} onClick={() => void onExport()}>
            Export
          </Button>
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
        <Alert color="red" title="Catalog I/O">
          {ioError}
        </Alert>
      )}

      {importResult && (
        <Paper withBorder p="sm">
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

      <div
        style={{
          flex: 1,
          minWidth: 0,
          minHeight: 520,
          height: 560,
          border: '1px solid var(--mantine-color-default-border)',
          borderRadius: 6,
        }}
      >
        <ReactFlow
          nodes={elements.nodes}
          edges={elements.edges}
          nodeTypes={nodeTypes}
          fitView
          fitViewOptions={{ padding: 0.18, maxZoom: 1 }}
          minZoom={0.2}
          maxZoom={1.4}
          nodesDraggable={false}
          nodesConnectable={false}
          elementsSelectable
          panOnDrag
          panOnScroll={false}
          zoomOnScroll={false}
          zoomOnPinch
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
    <Stack gap="sm" style={{ height: '100%' }}>
      {error && (
        <Alert color="red" title="Failed to load edges">
          {error}
        </Alert>
      )}
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
    </Stack>
  )
}
