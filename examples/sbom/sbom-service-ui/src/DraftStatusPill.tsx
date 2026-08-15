import type { DraftKind } from './bomDraft'

export const DRAFT_STATUS_COLOR = {
  new: '#40c057',
  deleted: '#fa5252',
  modified: '#fd7e14',
} as const

export function draftStatusColor(kind: DraftKind | undefined): string | null {
  if (kind === 'new' || kind === 'deleted' || kind === 'modified') return DRAFT_STATUS_COLOR[kind]
  return null
}

export function DraftStatusPill({
  status,
  ml,
}: {
  status: DraftKind | undefined
  ml?: number | string
}) {
  if (!status || status === 'unchanged') return null
  const bg = draftStatusColor(status) ?? '#495057'
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        marginLeft: ml ?? 0,
        background: bg,
        color: '#fff',
        borderRadius: 999,
        padding: '1px 6px',
        fontSize: 9,
        fontWeight: 800,
        textTransform: 'uppercase',
        verticalAlign: 'middle',
        flexShrink: 0,
        lineHeight: 1.4,
      }}
    >
      {status}
    </span>
  )
}
