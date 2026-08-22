import type { CSSProperties } from 'react'

/** Shared key/value column layout for ObjectViewer sections (aligned across Node / Payload / …). */
export const OBJECT_VIEWER_KEY_COL = '7.5rem'
export const OBJECT_VIEWER_KV_GAP = 8

export const OBJECT_VIEWER_KV_COLUMNS = `${OBJECT_VIEWER_KEY_COL} minmax(0, 1fr)`

export const objectViewerKvGridStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: OBJECT_VIEWER_KV_COLUMNS,
  columnGap: OBJECT_VIEWER_KV_GAP,
  alignItems: 'center',
}
