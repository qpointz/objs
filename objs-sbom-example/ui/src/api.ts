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
  return parseResponse<BoMSubgraph>(res)
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

export function schemaDetailPath(type: string, version: string): string {
  return `/schemas/${encodeURIComponent(type)}/${encodeURIComponent(version)}`
}

export function schemaLinterPath(type: string, version: string, mode: 'edit' | 'create-version' = 'edit'): string {
  const base = `/schemas/${encodeURIComponent(type)}/${encodeURIComponent(version)}/lint`
  return mode === 'create-version' ? `${base}?mode=create-version` : base
}
