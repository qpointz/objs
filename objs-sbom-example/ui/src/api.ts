import type {
  BoMSchema,
  BoMSchemaUsage,
  BoMSubgraph,
  BoMAllowedEdgeRule,
  EdgeRelationRequest,
  GraphLink,
  GraphNode,
  GraphValidationResult,
  SchemaDefinitionRequest,
  SchemaLintResponse,
  SeedImportResult,
  TypeEdgesResponse,
} from './types'
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

async function parseResponse<T>(res: Response): Promise<T> {
  const body = await res.json().catch(() => null)
  if (!res.ok) {
    const issues = body?.issues
    if (Array.isArray(issues) && issues.length > 0) {
      throw new Error(issues.map((i: { message?: string }) => i.message ?? JSON.stringify(i)).join('; '))
    }
    throw new Error(typeof body === 'string' ? body : `HTTP ${res.status}`)
  }
  return body as T
}

export async function queryGraph(matcherBody: unknown): Promise<BoMSubgraph> {
  const res = await fetch('/api/v1/objs/graph/query', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(matcherBody),
  })
  return parseResponse<BoMSubgraph>(res)
}

/** @deprecated Prefer [queryGraph] with an `anno` matcher body. */
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
  const filter = Object.fromEntries(
    Object.entries(parsed as Record<string, unknown>)
      .filter(([, value]) => value !== null && value !== undefined)
      .map(([key, value]) => [key, String(value)]),
  )
  if (Object.keys(filter).length === 0) {
    throw new Error('Provide at least one annotation key/value')
  }
  return queryGraph({ anno: filter })
}

export async function validateGraphDraft(graph: unknown): Promise<GraphValidationResult> {
  const res = await fetch('/api/v1/objs/graph/validate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(graph),
  })
  return parseResponse<GraphValidationResult>(res)
}

export async function listSchemas(usage?: BoMSchemaUsage): Promise<BoMSchema[]> {
  const query = usage ? `?usage=${encodeURIComponent(usage)}` : ''
  const res = await fetch(`/api/v1/objs/registry/schemas${query}`)
  return parseResponse<BoMSchema[]>(res)
}

export async function listSchemasByType(type: string): Promise<BoMSchema[]> {
  const res = await fetch(`/api/v1/objs/registry/schemas/${encodeURIComponent(type)}`)
  return parseResponse<BoMSchema[]>(res)
}

export async function getSchema(type: string, version: string): Promise<BoMSchema> {
  const res = await fetch(
    `/api/v1/objs/registry/schemas/${encodeURIComponent(type)}/${encodeURIComponent(version)}`,
  )
  return parseResponse<BoMSchema>(res)
}

export async function getJsonSchema(type: string, version: string): Promise<Record<string, unknown>> {
  const res = await fetch(
    `/api/v1/objs/registry/schemas/${encodeURIComponent(type)}/${encodeURIComponent(version)}/json-schema`,
  )
  return parseResponse<Record<string, unknown>>(res)
}

export async function getTypeEdges(type: string): Promise<TypeEdgesResponse> {
  const res = await fetch(`/api/v1/objs/registry/types/${encodeURIComponent(type)}/edges`)
  return parseResponse<TypeEdgesResponse>(res)
}

export async function getSchemaEdges(type: string, version: string): Promise<BoMAllowedEdgeRule[]> {
  const res = await fetch(
    `/api/v1/objs/registry/schemas/${encodeURIComponent(type)}/${encodeURIComponent(version)}/edges`,
  )
  return parseResponse<BoMAllowedEdgeRule[]>(res)
}

export async function replaceSchemaEdges(
  type: string,
  version: string,
  relations: EdgeRelationRequest[],
): Promise<BoMAllowedEdgeRule[]> {
  const res = await fetch(
    `/api/v1/objs/registry/schemas/${encodeURIComponent(type)}/${encodeURIComponent(version)}/edges`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(relations),
    },
  )
  return parseResponse<BoMAllowedEdgeRule[]>(res)
}

export async function lintSchema(
  type: string,
  version: string,
  body: SchemaDefinitionRequest,
): Promise<SchemaLintResponse> {
  const res = await fetch(
    `/api/v1/objs/registry/schemas/${encodeURIComponent(type)}/${encodeURIComponent(version)}/lint`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    },
  )
  return parseResponse<SchemaLintResponse>(res)
}

export async function updateSchema(
  type: string,
  version: string,
  body: SchemaDefinitionRequest,
): Promise<BoMSchema> {
  const res = await fetch(
    `/api/v1/objs/registry/schemas/${encodeURIComponent(type)}/${encodeURIComponent(version)}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    },
  )
  return parseResponse<BoMSchema>(res)
}

export async function createNextMajorSchema(
  type: string,
  body: SchemaDefinitionRequest,
): Promise<BoMSchema> {
  const res = await fetch(`/api/v1/objs/registry/schemas/${encodeURIComponent(type)}/versions/next-major`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  return parseResponse<BoMSchema>(res)
}

export async function putEdge(rule: {
  sourceType: string
  role: string
  targetType: string
  propertiesPolicy?: 'NONE' | 'SCHEMA'
  emptyPropertiesAllowed?: boolean
  propertiesSchemaType?: string | null
  propertiesSchemaVersion?: string | null
  cardinality?: string
}): Promise<BoMAllowedEdgeRule> {
  const res = await fetch('/api/v1/objs/registry/edges', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(rule),
  })
  return parseResponse<BoMAllowedEdgeRule>(res)
}

export async function deleteEdge(
  sourceType: string,
  role: string,
  targetType: string,
): Promise<void> {
  const params = new URLSearchParams({ sourceType, role, targetType })
  const res = await fetch(`/api/v1/objs/registry/edges?${params}`, { method: 'DELETE' })
  if (res.status === 204) return
  await parseResponse(res)
}

export async function listEdges(): Promise<BoMAllowedEdgeRule[]> {
  const res = await fetch('/api/v1/objs/registry/edges')
  return parseResponse<BoMAllowedEdgeRule[]>(res)
}

/** Catalog-only seed YAML (ObjectSchema + AllowedEdgeRule). */
export type CatalogExportFormat = 'seeds' | 'json-schema'

export async function exportCatalog(format: CatalogExportFormat): Promise<string> {
  const res = await fetch(`/api/v1/objs/registry/export?format=${encodeURIComponent(format)}`)
  if (!res.ok) {
    const body = await res.json().catch(() => null)
    throw new Error(typeof body === 'string' ? body : `HTTP ${res.status}`)
  }
  const text = await res.text()
  if (format === 'json-schema') {
    try {
      return JSON.stringify(JSON.parse(text), null, 2)
    } catch {
      return text
    }
  }
  return text
}

/** @deprecated Prefer exportCatalog('seeds') */
export async function exportCatalogSeed(): Promise<string> {
  return exportCatalog('seeds')
}

export async function importCatalogSeed(file: File): Promise<SeedImportResult> {
  const body = new FormData()
  body.append('file', file)
  const res = await fetch('/api/v1/objs/registry/import?format=seeds', { method: 'POST', body })
  return parseResponse<SeedImportResult>(res)
}

export function schemaDetailPath(type: string, version: string): string {
  return `/schemas/${encodeURIComponent(type)}/${encodeURIComponent(version)}`
}

export function schemaCreatePath(kind: 'object' | 'edge'): string {
  return `/schemas/new?kind=${kind}`
}

/** @deprecated Use in-place Schemas editing; kept for redirect helpers. */
export function schemaLinterPath(type: string, version: string, mode: 'edit' | 'create-version' = 'edit'): string {
  const base = schemaDetailPath(type, version)
  return mode === 'create-version' ? `${base}?mode=create-version` : base
}
