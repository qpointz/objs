import { Anchor } from '@mantine/core'

/** Shared Id column: 8-char truncate + `...`; same width on vertices and edges. */
export const QUERY_STRUCT_ID_MAX_LEN = 8
/** Fits `abcdefgh...` in monospace xs. */
export const QUERY_STRUCT_ID_COL_WIDTH = '11ch'
/** Same Type column width on vertices and edges tables. */
export const QUERY_STRUCT_TYPE_COL_WIDTH = '18ch'
/** Edges: Source name. */
export const QUERY_STRUCT_EDGE_SOURCE_COL_WIDTH = '40ch'
/** Edges: Role. */
export const QUERY_STRUCT_EDGE_ROLE_COL_WIDTH = '40ch'

export function truncateQueryId(id: string, max = QUERY_STRUCT_ID_MAX_LEN): string {
  if (id.length <= max) return id
  return `${id.slice(0, max)}...`
}

export function IdLink({
  id,
  onOpen,
}: {
  id: string
  onOpen: () => void
}) {
  const label = truncateQueryId(id)
  return (
    <Anchor
      component="button"
      type="button"
      size="xs"
      title={id}
      onClick={(e) => {
        e.stopPropagation()
        onOpen()
      }}
      style={{
        fontFamily: 'var(--mantine-font-family-monospace)',
        display: 'block',
        maxWidth: '100%',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
      }}
    >
      {label}
    </Anchor>
  )
}
