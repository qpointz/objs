import { memo } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import type { GraphNode } from './types'

export type EntityCardData = {
  entity: GraphNode
  selected: boolean
}

function compact(value: unknown, max = 90): string {
  const raw = JSON.stringify(value) ?? ''
  return raw.length <= max ? raw : `${raw.slice(0, max - 1)}…`
}

function EntityCardNodeComponent({ data }: NodeProps) {
  const { entity, selected } = data as EntityCardData
  return (
    <div
      style={{
        width: 180,
        border: `3px solid ${selected ? '#228be6' : entity.color}`,
        borderRadius: 6,
        background: '#fff',
        overflow: 'hidden',
        fontFamily: 'system-ui, sans-serif',
        boxShadow: selected
          ? '0 0 0 3px rgba(34, 139, 230, 0.35)'
          : '0 1px 3px rgba(0,0,0,0.12)',
        outline: selected ? '1px solid #228be6' : 'none',
      }}
    >
      <Handle type="target" position={Position.Top} style={{ opacity: 0.35 }} />
      <div
        style={{
          background: entity.color,
          color: '#fff',
          padding: '4px 6px',
          fontSize: 11,
          fontWeight: 700,
          lineHeight: 1.2,
        }}
      >
        {entity.type}: {entity.schemaVersion}
      </div>
      <div style={{ borderTop: '1px solid #dee2e6', padding: '3px 6px' }}>
        <div style={{ fontSize: 8, color: '#868e96', fontWeight: 700 }}>annotations</div>
        <div
          style={{
            fontSize: 8,
            fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
            color: '#212529',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
            lineHeight: 1.25,
          }}
        >
          {compact(entity.annotations)}
        </div>
      </div>
      <div style={{ borderTop: '1px solid #dee2e6', padding: '3px 6px' }}>
        <div style={{ fontSize: 8, color: '#868e96', fontWeight: 700 }}>payload</div>
        <div
          style={{
            fontSize: 8,
            fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
            color: '#212529',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
            lineHeight: 1.25,
          }}
        >
          {compact(entity.payload, 110)}
        </div>
      </div>
      <Handle type="source" position={Position.Bottom} style={{ opacity: 0.35 }} />
    </div>
  )
}

export const EntityCardNode = memo(EntityCardNodeComponent)
