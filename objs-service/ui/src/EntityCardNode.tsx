import { memo } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import { IconMinus, IconPencil, IconPlus } from '@tabler/icons-react'
import type { GraphNode } from './types'

export type EntityCardData = {
  entity: GraphNode
  selected: boolean
}

function compact(value: unknown, max = 90): string {
  const raw = JSON.stringify(value) ?? ''
  return raw.length <= max ? raw : `${raw.slice(0, max - 1)}…`
}

const STATUS_PILL: Record<
  Exclude<NonNullable<GraphNode['draftStatus']>, 'unchanged'>,
  { bg: string; label: string; Icon: typeof IconPlus }
> = {
  new: { bg: '#12b886', label: 'new', Icon: IconPlus },
  modified: { bg: '#fd7e14', label: 'edit', Icon: IconPencil },
  deleted: { bg: '#fa5252', label: 'del', Icon: IconMinus },
}

function StatusPill({ status }: { status: GraphNode['draftStatus'] }) {
  if (!status || status === 'unchanged') return null
  const cfg = STATUS_PILL[status]
  if (!cfg) return null
  const { bg, label, Icon } = cfg
  return (
    <span
      title={status}
      aria-label={status}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 2,
        flexShrink: 0,
        background: bg,
        color: '#fff',
        borderRadius: 999,
        padding: '2px 6px',
        fontSize: 9,
        fontWeight: 800,
        letterSpacing: 0.02,
        lineHeight: 1,
        textTransform: 'uppercase',
        boxShadow: '0 1px 2px rgba(0,0,0,0.25)',
      }}
    >
      <Icon size={10} stroke={3} />
      {label}
    </span>
  )
}

function EntityCardNodeComponent({ data }: NodeProps) {
  const { entity, selected } = data as EntityCardData
  const deleted = entity.draftStatus === 'deleted'
  const dimmed = entity.dimmed === true
  const borderColor = deleted ? '#fa5252' : selected ? '#228be6' : entity.color

  return (
    <div
      style={{
        width: 180,
        border: `3px solid ${borderColor}`,
        borderRadius: 6,
        background: deleted ? '#fff5f5' : '#fff',
        overflow: 'hidden',
        fontFamily: 'system-ui, sans-serif',
        opacity: dimmed ? 0.25 : deleted ? 0.72 : 1,
        boxShadow: selected
          ? '0 0 0 3px rgba(34, 139, 230, 0.35)'
          : '0 1px 3px rgba(0,0,0,0.12)',
        outline: selected ? '1px solid #228be6' : 'none',
      }}
    >
      <Handle type="target" position={Position.Top} style={{ opacity: 0.35 }} />
      <div
        style={{
          background: deleted ? '#fa5252' : entity.color,
          color: '#fff',
          padding: '4px 6px',
          fontSize: 11,
          fontWeight: 700,
          lineHeight: 1.2,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 6,
          textDecoration: deleted ? 'line-through' : 'none',
        }}
      >
        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {entity.type}: {entity.schemaVersion}
        </span>
        <StatusPill status={entity.draftStatus} />
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
