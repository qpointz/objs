/** Shared right-pane splitter limits for Explorer / Objects / Query / Composer. */

export function clamp(n: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, n))
}

/** Right pane may grow up to half the split-host width (at least `minWidth`). */
export function maxSidePaneWidth(hostWidth: number, minWidth: number): number {
  if (!Number.isFinite(hostWidth) || hostWidth <= 0) return minWidth
  return Math.max(minWidth, Math.floor(hostWidth * 0.5))
}
