import type { GraphLink, GraphNode } from './types'

type TypeHighlightOptions = {
  /**
   * When true, combine with existing `dimmed` flags (e.g. Composer “Changes only”)
   * instead of replacing them. Empty filter leaves nodes/links unchanged.
   */
  compose?: boolean
}

/** Dim nodes/edges not matching the selected type filter. Empty set = no filter. */
export function applyTypeHighlightDimming(
  nodes: GraphNode[],
  links: GraphLink[],
  highlightedTypes: ReadonlySet<string>,
  options?: TypeHighlightOptions,
): { nodes: GraphNode[]; links: GraphLink[] } {
  const compose = options?.compose === true
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
    links: links.map((l) => ({
      ...l,
      dimmed:
        (compose && l.dimmed === true) ||
        !highlightedIds.has(l.source) ||
        !highlightedIds.has(l.target),
    })),
  }
}

export function toggleTypeInSet(prev: ReadonlySet<string>, type: string): Set<string> {
  const next = new Set(prev)
  if (next.has(type)) next.delete(type)
  else next.add(type)
  return next
}
