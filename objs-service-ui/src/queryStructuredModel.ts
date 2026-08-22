import type { BoMGremlinResult } from './api'
import type { BoMEdge, BoMEntity, BoMGraphContents } from './types'
import { objectDisplayTitle } from './objectViewerTitle'

/** Virtualize Structured tables when row count exceeds this (G-UX-qstruct). */
export const QUERY_STRUCT_VIRTUALIZE_THRESHOLD = 200

export type QueryStructuredMode = 'table' | 'graph' | 'empty'

export function hasTableAlikeProjection(result: BoMGremlinResult): boolean {
  const table = result.views.table
  if (result.views.scalar != null) return true
  if (table != null && table.rows.length > 0) return true
  return false
}

export function graphContentsFromResult(result: BoMGremlinResult): BoMGraphContents | null {
  const c = result.contents
  if (c == null) return null
  const entities = c.entities ?? []
  const edges = c.edges ?? []
  if (entities.length === 0 && edges.length === 0) return null
  return c
}

/** Table-alike wins when both projection and subgraph are present (G-UX-qstruct). */
export function resolveStructuredMode(result: BoMGremlinResult | null): QueryStructuredMode {
  if (result == null) return 'empty'
  if (hasTableAlikeProjection(result)) return 'table'
  if (graphContentsFromResult(result) != null) return 'graph'
  if ((result.items?.length ?? 0) > 0) return 'table'
  return 'empty'
}

export function entityDisplayName(entity: BoMEntity): string {
  const payloadName =
    entity.payload != null && typeof entity.payload.name === 'string'
      ? entity.payload.name
      : null
  return objectDisplayTitle(payloadName, entity.type, entity.id)
}

export function endpointDisplayName(
  id: string,
  entitiesById: Map<string, BoMEntity>,
): string {
  const entity = entitiesById.get(id)
  if (entity) return entityDisplayName(entity)
  return id
}

export type StructuredVertexRow = {
  id: string
  type: string
  name: string
  entity: BoMEntity
}

export type StructuredEdgeRow = {
  id: string
  type: string
  sourceName: string
  role: string
  targetName: string
  edge: BoMEdge
}

export function structuredVertexRows(contents: BoMGraphContents): StructuredVertexRow[] {
  return (contents.entities ?? []).map((entity) => ({
    id: entity.id,
    type: entity.type,
    name: entityDisplayName(entity),
    entity,
  }))
}

export function structuredEdgeRows(contents: BoMGraphContents): StructuredEdgeRow[] {
  const byId = new Map((contents.entities ?? []).map((e) => [e.id, e]))
  return (contents.edges ?? []).map((edge, i) => ({
    id: edge.id ?? `e-${edge.source}-${edge.target}-${edge.role}-${i}`,
    type: edge.type ?? '—',
    sourceName: endpointDisplayName(edge.source, byId),
    role: edge.role,
    targetName: endpointDisplayName(edge.target, byId),
    edge,
  }))
}

export function hasOpenInComposerGraph(result: BoMGremlinResult | null): boolean {
  if (result == null) return false
  return graphContentsFromResult(result) != null
}
