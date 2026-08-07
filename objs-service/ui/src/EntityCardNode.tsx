import { memo } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import { IconMinus, IconPencil, IconPlus } from '@tabler/icons-react'
import { colorForType } from './color'
import type { GraphNode, PayloadFieldKind } from './types'
import './validationHighlight.css'

export type EntityCardData = {
  entity: GraphNode
  selected: boolean
}

/** Visual density for card vs inspector panel. */
export type EntityPayloadViewSize = 'card' | 'panel'

const STATUS_PILL: Record<
  Exclude<NonNullable<GraphNode['draftStatus']>, 'unchanged'>,
  { bg: string; label: string; Icon: typeof IconPlus }
> = {
  new: { bg: '#12b886', label: 'new', Icon: IconPlus },
  modified: { bg: '#fd7e14', label: 'edit', Icon: IconPencil },
  deleted: { bg: '#fa5252', label: 'del', Icon: IconMinus },
}

const HANDLE_STYLE = {
  opacity: 0,
  width: 8,
  height: 8,
  border: 'none',
  background: 'transparent',
} as const

const VIEW_SIZE = {
  card: {
    pillFont: 7,
    pillKeyMax: 56,
    pillValMax: 64,
    fieldFont: 8,
    valueFont: 8,
    listChipFont: 7,
    listChipMax: 72,
    valueMax: 28,
    listItemMax: 16,
    maxAnnotations: 6,
    maxPayloadRows: 8,
    maxListItems: 6,
    gap: 2,
    rowGap: 2,
    emptyFont: 8,
    keyCol: 'minmax(48px, 38%)',
    sectionLabelFont: 7,
  },
  panel: {
    pillFont: 11,
    pillKeyMax: 140,
    pillValMax: 220,
    fieldFont: 12,
    valueFont: 13,
    listChipFont: 11,
    listChipMax: 180,
    valueMax: 120,
    listItemMax: 48,
    maxAnnotations: 100,
    maxPayloadRows: 200,
    maxListItems: 40,
    gap: 6,
    rowGap: 6,
    emptyFont: 12,
    keyCol: 'minmax(72px, 34%)',
    sectionLabelFont: 10,
  },
} as const

type ViewTokens = (typeof VIEW_SIZE)[EntityPayloadViewSize]

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

function SectionLabel({
  children,
  size = 'card',
}: {
  children: string
  size?: EntityPayloadViewSize
}) {
  const t = VIEW_SIZE[size]
  return (
    <div
      style={{
        fontSize: t.sectionLabelFont,
        color: '#adb5bd',
        fontWeight: 600,
        letterSpacing: '0.06em',
        textTransform: 'uppercase',
        marginBottom: size === 'panel' ? 6 : 2,
      }}
    >
      {children}
    </div>
  )
}

/** Split key|value chip for annotations. Color is derived from the key. */
export function AnnotationSplitPill({
  k,
  v,
  size = 'card',
}: {
  k: string
  v: string
  size?: EntityPayloadViewSize
}) {
  const color = colorForType(k)
  const t = VIEW_SIZE[size]
  const padY = size === 'panel' ? 3 : 1
  const padX = size === 'panel' ? 8 : 4
  return (
    <span
      title={`${k}=${v}`}
      style={{
        display: 'inline-flex',
        alignItems: 'stretch',
        maxWidth: '100%',
        borderRadius: 999,
        overflow: 'hidden',
        border: `1px solid ${color}`,
        fontSize: t.pillFont,
        lineHeight: 1.2,
        background: '#fff',
      }}
    >
      <span
        style={{
          padding: `${padY}px ${padX}px`,
          background: color,
          color: '#fff',
          fontWeight: 700,
          fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
          maxWidth: t.pillKeyMax,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}
      >
        {k}
      </span>
      <span
        style={{
          padding: `${padY}px ${padX}px`,
          color: '#212529',
          fontWeight: 600,
          background: `color-mix(in srgb, ${color} 12%, #fff)`,
          maxWidth: t.pillValMax,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}
      >
        {v || '∅'}
      </span>
    </span>
  )
}

export function formatPayloadCell(value: unknown, max: number = VIEW_SIZE.card.valueMax): string {
  if (value == null) return '—'
  if (typeof value === 'boolean') return value ? 'true' : 'false'
  if (typeof value === 'number') return String(value)
  if (typeof value === 'string') {
    return value.length <= max ? value : `${value.slice(0, max - 1)}…`
  }
  if (Array.isArray(value)) {
    if (value.length === 0) return '[]'
    return `[${value.length}]`
  }
  if (typeof value === 'object') {
    const keys = Object.keys(value as object)
    if (keys.length === 0) return '{}'
    return `{${keys.length}}`
  }
  const raw = String(value)
  return raw.length <= max ? raw : `${raw.slice(0, max - 1)}…`
}

export function isScalarList(value: unknown): value is unknown[] {
  return (
    Array.isArray(value) &&
    value.every(
      (item) =>
        item == null ||
        typeof item === 'string' ||
        typeof item === 'number' ||
        typeof item === 'boolean',
    )
  )
}

export type PayloadDisplayRow = {
  key: string
  value: unknown
  kind?: PayloadFieldKind
  overflow?: string
}

export function flattenPayloadRows(
  payload: Record<string, unknown> | undefined | null,
  fieldKinds?: Record<string, PayloadFieldKind>,
  maxRows: number = VIEW_SIZE.card.maxPayloadRows,
): PayloadDisplayRow[] {
  const entries = Object.entries(payload ?? {})
  const shown = entries.slice(0, maxRows)
  const rows: PayloadDisplayRow[] = shown.map(([key, value]) => ({
    key,
    value,
    kind: fieldKinds?.[key],
  }))
  if (entries.length > maxRows) {
    rows.push({ key: '…', value: `+${entries.length - maxRows} more`, overflow: 'more' })
  }
  return rows
}

function EnumValuePill({ value, t }: { value: string; t: ViewTokens }) {
  const panel = t === VIEW_SIZE.panel
  return (
    <span
      title={value}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        maxWidth: '100%',
        boxSizing: 'border-box',
        height: panel ? 20 : 14,
        padding: panel ? '0 7px' : '0 5px',
        borderRadius: 999,
        border: '1px solid color-mix(in srgb, #7950f2 55%, #ced4da)',
        background: 'color-mix(in srgb, #7950f2 10%, #fff)',
        color: '#5f3dc4',
        fontSize: t.valueFont,
        fontWeight: 600,
        lineHeight: 1,
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
      }}
    >
      {value || '∅'}
    </span>
  )
}

function ListValueChips({ items, t }: { items: unknown[]; t: ViewTokens }) {
  if (items.length === 0) {
    return <span style={{ fontSize: t.emptyFont, color: '#adb5bd' }}>[]</span>
  }
  const shown = items.slice(0, t.maxListItems)
  const rest = items.length - shown.length
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: t.gap, alignItems: 'center' }}>
      {shown.map((item, i) => {
        const label = item == null ? '∅' : formatPayloadCell(item, t.listItemMax)
        return (
          <span
            key={`${i}:${label}`}
            title={String(item)}
            style={{
              display: 'inline-block',
              maxWidth: t.listChipMax,
              padding: t === VIEW_SIZE.panel ? '2px 8px' : '0 4px',
              borderRadius: t === VIEW_SIZE.panel ? 4 : 3,
              border: '1px solid #dee2e6',
              background: '#f8f9fa',
              color: '#495057',
              fontSize: t.listChipFont,
              fontWeight: 600,
              lineHeight: 1.4,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
          >
            {label}
          </span>
        )
      })}
      {rest > 0 && (
        <span style={{ fontSize: t.listChipFont, color: '#868e96', fontWeight: 700 }}>
          +{rest}
        </span>
      )}
    </div>
  )
}

function PayloadValueCell({
  value,
  kind,
  t,
}: {
  value: unknown
  kind?: PayloadFieldKind
  t: ViewTokens
}) {
  if (typeof value === 'string' && kind === 'ENUM') {
    return <EnumValuePill value={value} t={t} />
  }
  if (isScalarList(value)) {
    return <ListValueChips items={value} t={t} />
  }
  const text = formatPayloadCell(value, t.valueMax)
  const wrap = t === VIEW_SIZE.panel && typeof value === 'string' && value.length > 40
  return (
    <span
      title={typeof value === 'string' ? value : text}
      style={{
        fontSize: t.valueFont,
        fontWeight: 600,
        color: '#212529',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: wrap ? 'normal' : 'nowrap',
        wordBreak: wrap ? 'break-word' : undefined,
        display: 'block',
        lineHeight: t === VIEW_SIZE.panel ? '20px' : '14px',
      }}
    >
      {text}
    </span>
  )
}

/** Annotation chips — same look as graph cards (`card`) or larger inspector (`panel`). */
export function EntityAnnotationsView({
  annotations,
  size = 'card',
  showLabel = false,
}: {
  annotations: Record<string, string>
  size?: EntityPayloadViewSize
  showLabel?: boolean
}) {
  const t = VIEW_SIZE[size]
  const entries = Object.entries(annotations)
  return (
    <div>
      {showLabel && <SectionLabel size={size}>annotations</SectionLabel>}
      {entries.length === 0 ? (
        <div style={{ fontSize: t.emptyFont, color: '#adb5bd', fontStyle: 'italic' }}>none</div>
      ) : (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: t.gap }}>
          {entries.slice(0, t.maxAnnotations).map(([k, v]) => (
            <AnnotationSplitPill key={k} k={k} v={v} size={size} />
          ))}
          {entries.length > t.maxAnnotations && (
            <span
              style={{
                fontSize: t.emptyFont,
                color: '#868e96',
                fontWeight: 700,
                padding: '2px 5px',
                alignSelf: 'center',
              }}
            >
              +{entries.length - t.maxAnnotations}
            </span>
          )}
        </div>
      )}
    </div>
  )
}

/** Payload field/value grid — same look as graph cards (`card`) or larger inspector (`panel`). */
export function EntityPayloadView({
  payload,
  fieldKinds,
  size = 'card',
  showLabel = false,
  label = 'payload',
}: {
  payload: Record<string, unknown>
  fieldKinds?: Record<string, PayloadFieldKind>
  size?: EntityPayloadViewSize
  showLabel?: boolean
  label?: string
}) {
  const t = VIEW_SIZE[size]
  const rows = flattenPayloadRows(payload, fieldKinds, t.maxPayloadRows)
  return (
    <div>
      {showLabel && <SectionLabel size={size}>{label}</SectionLabel>}
      {rows.length === 0 ? (
        <div style={{ fontSize: t.emptyFont, color: '#adb5bd', fontStyle: 'italic' }}>empty</div>
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: `${t.keyCol} 1fr`,
            columnGap: size === 'panel' ? 10 : 4,
            rowGap: t.rowGap,
            alignItems: 'center',
          }}
        >
          {rows.map((row) => (
            <div key={row.key} style={{ display: 'contents' }}>
              <div
                style={{
                  fontSize: t.fieldFont,
                  fontWeight: 700,
                  color: '#868e96',
                  fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  lineHeight: size === 'panel' ? '20px' : '14px',
                }}
                title={row.key}
              >
                {row.key}
              </div>
              <div
                style={{
                  minWidth: 0,
                  display: 'flex',
                  alignItems: 'center',
                  minHeight: size === 'panel' ? 20 : 14,
                }}
              >
                {row.overflow === 'more' ? (
                  <span style={{ fontSize: t.fieldFont, color: '#868e96', fontWeight: 700 }}>
                    {String(row.value)}
                  </span>
                ) : (
                  <PayloadValueCell value={row.value} kind={row.kind} t={t} />
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function EntityCardNodeComponent({ data }: NodeProps) {
  const { entity, selected } = data as EntityCardData
  const deleted = entity.draftStatus === 'deleted'
  const dimmed = entity.dimmed === true
  const validationError = entity.validationError === true
  const borderColor = validationError
    ? '#fa5252'
    : deleted
      ? '#fa5252'
      : selected
        ? '#228be6'
        : entity.color

  return (
    <div
      className={validationError ? 'objs-validation-error' : undefined}
      style={{
        width: 180,
        border: `3px solid ${borderColor}`,
        borderRadius: 6,
        background: deleted ? '#fff5f5' : '#fff',
        overflow: 'hidden',
        fontFamily: 'system-ui, sans-serif',
        opacity: dimmed ? 0.25 : deleted ? 0.72 : 1,
        boxShadow: selected && !validationError
          ? '0 0 0 3px rgba(34, 139, 230, 0.35)'
          : validationError
            ? undefined
            : '0 1px 3px rgba(0,0,0,0.12)',
        outline: selected && !validationError ? '1px solid #228be6' : 'none',
      }}
    >
      <Handle type="target" id="top-target" position={Position.Top} isConnectable={false} style={HANDLE_STYLE} />
      <Handle type="source" id="top-source" position={Position.Top} isConnectable={false} style={HANDLE_STYLE} />
      <Handle type="target" id="left-target" position={Position.Left} isConnectable={false} style={HANDLE_STYLE} />
      <Handle type="source" id="left-source" position={Position.Left} isConnectable={false} style={HANDLE_STYLE} />
      <Handle type="target" id="right-target" position={Position.Right} isConnectable={false} style={HANDLE_STYLE} />
      <Handle type="source" id="right-source" position={Position.Right} isConnectable={false} style={HANDLE_STYLE} />

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
      <div style={{ borderTop: '1px solid #dee2e6', padding: '4px 6px' }}>
        <EntityPayloadView payload={entity.payload ?? {}} fieldKinds={entity.payloadFieldKinds} />
      </div>
      <div style={{ borderTop: '1px solid #dee2e6', padding: '4px 6px' }}>
        <EntityAnnotationsView annotations={entity.annotations ?? {}} showLabel />
      </div>

      <Handle type="target" id="bottom-target" position={Position.Bottom} isConnectable={false} style={HANDLE_STYLE} />
      <Handle type="source" id="bottom-source" position={Position.Bottom} isConnectable={false} style={HANDLE_STYLE} />
    </div>
  )
}

export const EntityCardNode = memo(EntityCardNodeComponent)
