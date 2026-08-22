import type { BoMGraphVersionSummary } from './types'

export const GRAPH_VERSION_PAGE_SIZE = 10

export type GraphVersionTimeFilter = {
  /** Inclusive lower bound (local datetime → Date). */
  from: Date | null
  /** Inclusive upper bound (local datetime → Date). */
  to: Date | null
}

/** Parse API createdAt (or epoch-ish version) into a Date. */
export function parseGraphVersionTime(
  createdAt: string | undefined,
  version: number,
): Date | null {
  if (createdAt) {
    const d = new Date(createdAt)
    if (!Number.isNaN(d.getTime())) return d
  }
  if (Number.isFinite(version) && version > 1_000_000_000_000) {
    const fromVersion = new Date(version)
    if (!Number.isNaN(fromVersion.getTime())) return fromVersion
  }
  return null
}

/** Newest-first list filtered by optional from/to (inclusive by calendar instant). */
export function filterGraphVersionsByTime(
  rows: BoMGraphVersionSummary[],
  filter: GraphVersionTimeFilter,
): BoMGraphVersionSummary[] {
  const { from, to } = filter
  if (from == null && to == null) return rows
  return rows.filter((row) => {
    const t = parseGraphVersionTime(row.createdAt, row.version)
    if (t == null) return false
    if (from != null && t.getTime() < from.getTime()) return false
    if (to != null && t.getTime() > to.getTime()) return false
    return true
  })
}

export function pageGraphVersions(
  rows: BoMGraphVersionSummary[],
  pageIndex: number,
  pageSize: number = GRAPH_VERSION_PAGE_SIZE,
): BoMGraphVersionSummary[] {
  const start = Math.max(0, pageIndex) * pageSize
  return rows.slice(start, start + pageSize)
}

export function graphVersionPageCount(
  total: number,
  pageSize: number = GRAPH_VERSION_PAGE_SIZE,
): number {
  if (total <= 0) return 1
  return Math.max(1, Math.ceil(total / pageSize))
}

export const EXPLORER_NODE_CAP = 300
