import { memo, useMemo } from 'react'
import {
  Background,
  Controls,
  Handle,
  MarkerType,
  Position,
  ReactFlow,
  ReactFlowProvider,
  type Edge,
  type Node,
  type NodeProps,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { useNavigate } from 'react-router-dom'
import type {
  BoMAllowedEdgeRule,
  BoMSchema,
  BoMSchemaField,
  BoMSchemaNode,
  TypeEdgesResponse,
} from './types'

type PropertyRow = {
  key: string
  name: string
  type: string
  depth: number
  required: boolean
}

type SchemaGraphNodeData = {
  kind: 'selected' | 'related'
  type: string
  version?: string
  properties?: PropertyRow[]
  direction?: 'incoming' | 'outgoing'
  schemaKind?: 'ENTITY SCHEMA' | 'EDGE SCHEMA'
}

const SELECTED_WIDTH = 340
const RELATED_WIDTH = 180
const COLUMN_GAP = 260
const RELATED_HEIGHT = 64
const RELATED_GAP = 36

function compactType(node: BoMSchemaNode): string {
  switch (node.type) {
    case 'ARRAY':
      return `ARRAY<${node.items ? compactType(node.items) : '?'}>`
    case 'ENUM':
      return `ENUM {${(node.values ?? []).map((value) => value.value).join(' | ')}}`
    default:
      return node.type
  }
}

function appendFieldRows(
  rows: PropertyRow[],
  fields: BoMSchemaField[],
  depth: number,
  parentKey: string,
) {
  fields.forEach((field, index) => {
    const key = `${parentKey}/${index}:${field.name}`
    rows.push({
      key,
      name: field.name,
      type: compactType(field.schema),
      depth,
      required: field.required !== false,
    })

    if (field.schema.type === 'OBJECT') {
      appendFieldRows(rows, field.schema.fields ?? [], depth + 1, key)
    } else if (field.schema.type === 'ARRAY' && field.schema.items?.type === 'OBJECT') {
      appendFieldRows(rows, field.schema.items.fields ?? [], depth + 1, `${key}/items`)
    }
  })
}

/** Flatten a recursive DSL into compact UML-style property rows. */
export function schemaPropertyRows(root: BoMSchemaNode): PropertyRow[] {
  const rows: PropertyRow[] = []
  appendFieldRows(rows, root.fields ?? [], 0, 'root')
  return rows
}

function SelectedSchemaNode({ data }: NodeProps) {
  const node = data as SchemaGraphNodeData
  return (
    <div
      style={{
        width: SELECTED_WIDTH,
        border: '2px solid var(--mantine-color-blue-7)',
        borderRadius: 8,
        background: 'var(--mantine-color-body)',
        overflow: 'hidden',
        boxShadow: '0 4px 16px rgba(0, 0, 0, 0.14)',
        fontFamily: 'system-ui, sans-serif',
      }}
    >
      <Handle type="target" position={Position.Left} style={{ background: '#1971c2' }} />
      <div
        style={{
          padding: '10px 12px',
          background: 'var(--mantine-color-blue-7)',
          color: 'white',
        }}
      >
        <div style={{ fontSize: 11, fontWeight: 700, opacity: 0.8 }}>{node.schemaKind}</div>
        <div style={{ fontSize: 16, fontWeight: 700 }}>{node.type}</div>
        <div style={{ fontSize: 11, opacity: 0.85 }}>version {node.version}</div>
      </div>
      <div
        className="nowheel"
        style={{
          maxHeight: 310,
          overflowY: 'auto',
          background: 'var(--mantine-color-body)',
        }}
      >
        {(node.properties ?? []).length === 0 ? (
          <div style={{ padding: '10px 12px', fontSize: 11, color: '#868e96' }}>
            No declared properties
          </div>
        ) : (
          node.properties?.map((property) => (
            <div
              key={property.key}
              style={{
                display: 'grid',
                gridTemplateColumns: 'minmax(100px, 1fr) minmax(90px, auto)',
                gap: 8,
                padding: `5px 10px 5px ${12 + property.depth * 16}px`,
                borderTop: '1px solid var(--mantine-color-default-border)',
                fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
                fontSize: 10,
                lineHeight: 1.25,
              }}
            >
              <span style={{ minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {property.depth > 0 ? '↳ ' : ''}
                {property.name}
                {property.required ? <strong style={{ color: '#e03131' }}> *</strong> : null}
              </span>
              <span
                style={{
                  color: 'var(--mantine-color-dimmed)',
                  textAlign: 'right',
                  wordBreak: 'break-word',
                }}
              >
                {property.type}
              </span>
            </div>
          ))
        )}
      </div>
      <Handle type="source" position={Position.Right} style={{ background: '#1971c2' }} />
    </div>
  )
}

function RelatedSchemaNode({ data }: NodeProps) {
  const node = data as SchemaGraphNodeData
  const incoming = node.direction === 'incoming'
  const wildcard = node.type === '*'
  return (
    <div
      style={{
        width: RELATED_WIDTH,
        minHeight: RELATED_HEIGHT,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '8px 12px',
        border: `1px solid ${wildcard ? '#adb5bd' : '#748ffc'}`,
        borderRadius: 7,
        background: wildcard ? '#f1f3f5' : 'var(--mantine-color-body)',
        boxShadow: '0 1px 4px rgba(0, 0, 0, 0.1)',
        fontFamily: 'system-ui, sans-serif',
        cursor: wildcard ? 'default' : 'pointer',
        textAlign: 'center',
      }}
    >
      {incoming && <Handle type="source" position={Position.Right} style={{ background: '#5c7cfa' }} />}
      <div>
        <div style={{ fontSize: 10, color: '#868e96', fontWeight: 700 }}>
          {wildcard ? 'ANY ENTITY' : 'ENTITY'}
        </div>
        <div style={{ fontSize: 13, fontWeight: 700 }}>{wildcard ? 'Any type (*)' : node.type}</div>
        {!wildcard && <div style={{ fontSize: 9, color: '#868e96' }}>click to inspect</div>}
      </div>
      {!incoming && <Handle type="target" position={Position.Left} style={{ background: '#5c7cfa' }} />}
    </div>
  )
}

const nodeTypes = {
  selectedSchema: memo(SelectedSchemaNode),
  relatedSchema: memo(RelatedSchemaNode),
}

function uniqueRelatedTypes(rules: BoMAllowedEdgeRule[], direction: 'incoming' | 'outgoing') {
  const types: string[] = []
  const seen = new Set<string>()
  for (const rule of rules) {
    const type = direction === 'incoming' ? rule.sourceType : rule.targetType
    if (!seen.has(type)) {
      seen.add(type)
      types.push(type)
    }
  }
  return types.sort((a, b) => a.localeCompare(b))
}

function relatedY(index: number, count: number): number {
  const totalHeight = count * RELATED_HEIGHT + Math.max(0, count - 1) * RELATED_GAP
  return -totalHeight / 2 + index * (RELATED_HEIGHT + RELATED_GAP)
}

export function schemaRelationshipElements(
  schema: BoMSchema,
  relationships: TypeEdgesResponse,
): { nodes: Node[]; edges: Edge[] } {
  const incomingTypes = uniqueRelatedTypes(relationships.incoming, 'incoming')
  const outgoingTypes = uniqueRelatedTypes(relationships.outgoing, 'outgoing')
  const nodes: Node[] = [
    {
      id: 'selected',
      type: 'selectedSchema',
      position: { x: 0, y: -150 },
      data: {
        kind: 'selected',
        type: schema.type,
        version: schema.version,
        properties: schemaPropertyRows(schema.contentSchema),
        schemaKind:
          schema.usages.includes('EDGE_PROPERTIES') && !schema.usages.includes('ENTITY')
            ? 'EDGE SCHEMA'
            : 'ENTITY SCHEMA',
      } satisfies SchemaGraphNodeData,
      draggable: false,
      selectable: false,
    },
    ...incomingTypes.map((type, index) => ({
      id: `incoming:${type}`,
      type: 'relatedSchema',
      position: { x: -COLUMN_GAP, y: relatedY(index, incomingTypes.length) },
      data: {
        kind: 'related' as const,
        type,
        direction: 'incoming' as const,
      } satisfies SchemaGraphNodeData,
      draggable: false,
    })),
    ...outgoingTypes.map((type, index) => ({
      id: `outgoing:${type}`,
      type: 'relatedSchema',
      position: { x: SELECTED_WIDTH + COLUMN_GAP - RELATED_WIDTH, y: relatedY(index, outgoingTypes.length) },
      data: {
        kind: 'related' as const,
        type,
        direction: 'outgoing' as const,
      } satisfies SchemaGraphNodeData,
      draggable: false,
    })),
  ]

  const edgeStyle = { stroke: '#5c7cfa', strokeWidth: 1.5 }
  const markerEnd = { type: MarkerType.ArrowClosed, color: '#5c7cfa', width: 14, height: 14 }
  const edges: Edge[] = [
    ...relationships.incoming.map((rule, index) => ({
      id: `incoming-edge:${index}:${rule.sourceType}:${rule.role}`,
      source: `incoming:${rule.sourceType}`,
      target: 'selected',
      label: rule.role,
      type: 'smoothstep',
      style: edgeStyle,
      markerEnd,
      labelStyle: { fontSize: 10, fontWeight: 700, fill: '#364fc7' },
      labelBgStyle: { fill: '#fff', fillOpacity: 0.92 },
      labelBgPadding: [4, 2] as [number, number],
    })),
    ...relationships.outgoing.map((rule, index) => ({
      id: `outgoing-edge:${index}:${rule.targetType}:${rule.role}`,
      source: 'selected',
      target: `outgoing:${rule.targetType}`,
      label: rule.role,
      type: 'smoothstep',
      style: edgeStyle,
      markerEnd,
      labelStyle: { fontSize: 10, fontWeight: 700, fill: '#364fc7' },
      labelBgStyle: { fill: '#fff', fillOpacity: 0.92 },
      labelBgPadding: [4, 2] as [number, number],
    })),
  ]

  return { nodes, edges }
}

function SchemaRelationshipGraphInner({
  schema,
  relationships,
}: {
  schema: BoMSchema
  relationships: TypeEdgesResponse
}) {
  const navigate = useNavigate()
  const elements = useMemo(
    () => schemaRelationshipElements(schema, relationships),
    [schema, relationships],
  )

  return (
    <ReactFlow
      nodes={elements.nodes}
      edges={elements.edges}
      nodeTypes={nodeTypes}
      fitView
      fitViewOptions={{ padding: 0.18, maxZoom: 1 }}
      minZoom={0.35}
      maxZoom={1.4}
      nodesDraggable={false}
      nodesConnectable={false}
      elementsSelectable
      onNodeClick={(_, node) => {
        const data = node.data as SchemaGraphNodeData
        if (data.kind === 'related' && data.type !== '*') {
          navigate(`/schemas/${encodeURIComponent(data.type)}`)
        }
      }}
      proOptions={{ hideAttribution: true }}
    >
      <Background gap={18} size={1} />
      <Controls showInteractive={false} />
    </ReactFlow>
  )
}

export function SchemaRelationshipGraph({
  schema,
  relationships,
}: {
  schema: BoMSchema
  relationships: TypeEdgesResponse
}) {
  return (
    <div style={{ height: 560, border: '1px solid var(--mantine-color-default-border)', borderRadius: 6 }}>
      <ReactFlowProvider>
        <SchemaRelationshipGraphInner schema={schema} relationships={relationships} />
      </ReactFlowProvider>
    </div>
  )
}
