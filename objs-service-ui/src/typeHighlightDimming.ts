import type { GraphLink, GraphNode } from './types'

type TypeHighlightOptions = {
  /**
   * When true, combine with existing `dimmed` flags (e.g. Composer “Changes only”)
   * instead of replacing them. Empty filter leaves nodes/links unchanged.
   */
  compose?: boolean
  /**
   * Edge visibility when a type filter is active (Note 4 Explorer):
   * - `either-end` (default): edge stays full opacity if **at least one** endpoint is a selected type
   * - `both-ends`: edge dims unless **both** endpoints are selected types
   */
  edgeKeepIf?: 'either-end' | 'both-ends'
}

/** Dim nodes/edges not matching the selected type filter. Empty set = no filter. */
export function applyTypeHighlightDimming(
  nodes: GraphNode[],
  links: GraphLink[],
  highlightedTypes: ReadonlySet<string>,
  options?: TypeHighlightOptions,
): { nodes: GraphNode[]; links: GraphLink[] } {
  const compose = options?.compose === true
  const edgeKeepIf = options?.edgeKeepIf ?? 'either-end'
  if (highlightedTypes.size === 0) {
    if (compose) return { nodes, links }
    return {
      nodes: nodes.map((n) => (n.dimmed ? { ...n, dimmed: false } : n)),
      links: links.map((l) => (l.dimmed ? { ...l, dimmed: false } : l)),
    }
  }
  const highlightedIds = new Set(
    nodes.filter((n) => highlightedTypes.has(n.type)).map((n) => n.id),
  )
  return {
    nodes: nodes.map((n) => ({
      ...n,
      dimmed: (compose && n.dimmed === true) || !highlightedTypes.has(n.type),
    })),
    links: links.map((l) => {
      const sourceOk = highlightedIds.has(l.source)
      const targetOk = highlightedIds.has(l.target)
      const typeDim =
        edgeKeepIf === 'both-ends' ? !sourceOk || !targetOk : !sourceOk && !targetOk
      return {
        ...l,
        dimmed: (compose && l.dimmed === true) || typeDim,
      }
    }),
  }
}

export function toggleTypeInSet(prev: ReadonlySet<string>, type: string): Set<string> {
  const next = new Set(prev)
  if (next.has(type)) next.delete(type)
  else next.add(type)
  return next
}
