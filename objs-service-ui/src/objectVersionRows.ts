import type { BoMInstanceVersionSummary } from './types'

/** Version list row; `version: null` = live Latest (HEAD). */
export type ObjectVersionRow = {
  id: string
  version: number | null
  createdAt: string
  updatedAt?: string
  annotations?: Record<string, string>
}

export function instanceToVersionRow(row: BoMInstanceVersionSummary): ObjectVersionRow {
  return {
    id: row.id,
    version: row.version,
    createdAt: row.createdAt,
    updatedAt: row.updatedAt,
    annotations: row.annotations,
  }
}

/** Synthetic Latest row for graph (and optionally entity/edge) version lists. */
export function latestVersionRow(
  id: string,
  annotations: Record<string, string> = {},
  createdAt: string = new Date().toISOString(),
): ObjectVersionRow {
  return {
    id,
    version: null,
    createdAt,
    updatedAt: createdAt,
    annotations,
  }
}

/**
 * When viewing a pinned (non-latest) version, ensure Latest is first in the preview list.
 * Deep rows keep newest-first order after Latest.
 */
export function withLatestInRecent(
  deepRows: ObjectVersionRow[],
  opts: {
    includeLatest: boolean
    id: string
    annotations?: Record<string, string>
    createdAt?: string
    recentN: number
  },
): ObjectVersionRow[] {
  const deep = deepRows.slice(0, opts.recentN)
  if (!opts.includeLatest) return deep
  return [
    latestVersionRow(opts.id, opts.annotations ?? {}, opts.createdAt),
    ...deep,
  ]
}

/** Always put Latest first in the full browser list when requested. */
export function withLatestInBrowser(
  deepRows: ObjectVersionRow[],
  opts: {
    includeLatest: boolean
    id: string
    annotations?: Record<string, string>
    createdAt?: string
  },
): ObjectVersionRow[] {
  if (!opts.includeLatest) return deepRows
  const rest = deepRows.filter((r) => r.version != null)
  return [latestVersionRow(opts.id, opts.annotations ?? {}, opts.createdAt), ...rest]
}

export function formatVersionRowLabel(version: number | null): string {
  return version == null ? 'LATEST' : formatVersionIdForList(version)
}

/**
 * Compact version id for lists: last 6 digits as `...643179`.
 * Shorter than 6 digits → full string (future shorter schemes).
 */
export function formatVersionIdForList(version: number): string {
  const raw = String(version)
  if (raw.length <= 6) return raw
  return `...${raw.slice(-6)}`
}
