export type QueryExecStats = {
  /** Wall time of the graph query HTTP call only (excludes layout / canvas work). */
  durationMs: number
  nodes: number
  edges: number
}

export function formatQueryDuration(ms: number): string {
  if (ms >= 1000) {
    const seconds = ms / 1000
    return `${seconds >= 10 ? seconds.toFixed(1) : seconds.toFixed(2)}s`
  }
  return `${Math.round(ms)}ms`
}

export function formatQueryExecStats(stats: QueryExecStats): string {
  return `${formatQueryDuration(stats.durationMs)} · ${stats.nodes} nodes · ${stats.edges} edges`
}
