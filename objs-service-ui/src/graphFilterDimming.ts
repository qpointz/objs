import type { GraphLink, GraphNode } from './types'
import { applyTypeHighlightDimming, toggleTypeInSet } from './typeHighlightDimming'

export { toggleTypeInSet }

/** Dim by edge `role` (verb). Empty set = no edge-role filter. */
export function applyEdgeRoleDimming(
  nodes: GraphNode[],
  links: GraphLink[],
  selectedRoles: ReadonlySet<string>,
  options?: { compose?: boolean },
): { nodes: GraphNode[]; links: GraphLink[] } {
  const compose = options?.compose === true
  if (selectedRoles.size === 0) {
    if (compose) return { nodes, links }
    return {
      nodes: nodes.map((n) => (n.dimmed ? { ...n, dimmed: false } : n)),
      links: links.map((l) => (l.dimmed ? { ...l, dimmed: false } : l)),
    }
  }
  const keptIds = new Set<string>()
  for (const l of links) {
    if (selectedRoles.has(l.role)) {
      keptIds.add(l.source)
      keptIds.add(l.target)
    }
  }
  return {
    nodes: nodes.map((n) => ({
      ...n,
      dimmed: (compose && n.dimmed === true) || !keptIds.has(n.id),
    })),
    links: links.map((l) => ({
      ...l,
      dimmed: (compose && l.dimmed === true) || !selectedRoles.has(l.role),
    })),
  }
}

/**
 * Combine type + edge-role filters.
 * - One dimension active → that dimension alone (existing behaviour).
 * - Both active → **union**: keep if type matches **or** incident to a selected-role edge
 *   (same for edges: role matches **or** either endpoint type matches).
 * Empty sets skip that dimension. `compose` preserves prior dimming (Changes only / severity).
 */
export function applyGraphCanvasFilters(
  nodes: GraphNode[],
  links: GraphLink[],
  filters: {
    types?: ReadonlySet<string>
    edgeRoles?: ReadonlySet<string>
  },
  options?: { compose?: boolean; edgeKeepIf?: 'either-end' | 'both-ends' },
): { nodes: GraphNode[]; links: GraphLink[] } {
  const types = filters.types ?? new Set<string>()
  const edgeRoles = filters.edgeRoles ?? new Set<string>()
  const hasTypes = types.size > 0
  const hasRoles = edgeRoles.size > 0
  const compose = options?.compose === true

  if (!hasTypes && !hasRoles) {
    return applyTypeHighlightDimming(nodes, links, types, {
      compose,
      edgeKeepIf: options?.edgeKeepIf,
    })
  }

  if (hasTypes && !hasRoles) {
    return applyTypeHighlightDimming(nodes, links, types, {
      compose,
      edgeKeepIf: options?.edgeKeepIf,
    })
  }

  if (!hasTypes && hasRoles) {
    return applyEdgeRoleDimming(nodes, links, edgeRoles, { compose })
  }

  // Both active — union of matches (not intersection).
  const onRoleEdge = new Set<string>()
  for (const l of links) {
    if (edgeRoles.has(l.role)) {
      onRoleEdge.add(l.source)
      onRoleEdge.add(l.target)
    }
  }
  const typeById = new Map(nodes.map((n) => [n.id, n.type]))

  return {
    nodes: nodes.map((n) => {
      const typeOk = types.has(n.type)
      const roleOk = onRoleEdge.has(n.id)
      const dimmed = !typeOk && !roleOk
      return {
        ...n,
        dimmed: (compose && n.dimmed === true) || dimmed,
      }
    }),
    links: links.map((l) => {
      const roleOk = edgeRoles.has(l.role)
      const srcType = typeById.get(l.source)
      const tgtType = typeById.get(l.target)
      const typeOk =
        (srcType != null && types.has(srcType)) || (tgtType != null && types.has(tgtType))
      const dimmed = !roleOk && !typeOk
      return {
        ...l,
        dimmed: (compose && l.dimmed === true) || dimmed,
      }
    }),
  }
}

export function uniqueSortedRoles(links: GraphLink[]): string[] {
  const roles = new Set<string>()
  for (const l of links) {
    if (l.role) roles.add(l.role)
  }
  return [...roles].sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
}
