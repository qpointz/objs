import type { BoMSubgraph, GraphLink, GraphNode } from './types'
import { colorForType, nodeLabel } from './color'

export function toGraphData(subgraph: BoMSubgraph): { nodes: GraphNode[]; links: GraphLink[] } {
  const nodes: GraphNode[] = (subgraph.entities ?? []).map((e) => ({
    id: e.id,
    name: nodeLabel(e.payload, e.id),
    type: e.type,
    schemaVersion: e.schemaVersion ?? '?',
    color: colorForType(e.type),
    payload: e.payload ?? {},
    annotations: e.annotations ?? {},
  }))

  const idSet = new Set(nodes.map((n) => n.id))
  const links: GraphLink[] = (subgraph.edges ?? [])
    .filter((edge) => idSet.has(edge.source) && idSet.has(edge.target))
    .map((edge, i) => ({
      id: edge.id ?? `e-${edge.source}-${edge.target}-${edge.role}-${i}`,
      source: edge.source,
      target: edge.target,
      role: edge.role,
      type: edge.type ?? null,
      schemaVersion: edge.schemaVersion ?? null,
      properties: edge.properties ?? {},
    }))

  return { nodes, links }
}

export function annotationsToQuery(obj: Record<string, unknown>): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(obj)) {
    if (value === null || value === undefined) continue
    params.set(key, String(value))
  }
  return params.toString()
}

export async function fetchGraph(annotationJson: string): Promise<BoMSubgraph> {
  let parsed: unknown
  try {
    parsed = JSON.parse(annotationJson)
  } catch {
    throw new Error('Annotation box must be valid JSON')
  }
  if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error('Annotation JSON must be an object, e.g. {"app":"payments-api"}')
  }
  const query = annotationsToQuery(parsed as Record<string, unknown>)
  if (!query) {
    throw new Error('Provide at least one annotation key/value')
  }
  const res = await fetch(`/api/v1/objs/graph?${query}`)
  const body = await res.json().catch(() => null)
  if (!res.ok) {
    const issues = body?.issues
    if (Array.isArray(issues) && issues.length > 0) {
      throw new Error(issues.map((i: { message?: string }) => i.message ?? JSON.stringify(i)).join('; '))
    }
    throw new Error(typeof body === 'string' ? body : `HTTP ${res.status}`)
  }
  return body as BoMSubgraph
}
