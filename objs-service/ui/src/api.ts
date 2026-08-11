import type {
  BoMSchema,
  BoMSchemaUsage,
  BoMGraphContents,
  BoMAllowedEdgeRule,
  BoMGraphListItem,
  BoMGraphResponse,
  BoMGraphSearchResponse,
  EdgeRelationRequest,
  GraphLink,
  GraphNode,
  GraphValidationResult,
  SchemaDefinitionRequest,
  SchemaLintResponse,
  SeedImportResult,
  TypeEdgesResponse,
} from './types'
import type { MatcherMode } from './MatcherQueryForm'
import { colorForType, nodeLabel } from './color'

export function toGraphData(contents: BoMGraphContents): { nodes: GraphNode[]; links: GraphLink[] } {
  const nodes: GraphNode[] = (contents.entities ?? []).map((e) => ({
    id: e.id,
    name: nodeLabel(e.payload, e.id),
    type: e.type,
    schemaVersion: e.schemaVersion ?? '?',
    color: colorForType(e.type),
    payload: e.payload ?? {},
    annotations: e.annotations ?? {},
  }))

  const idSet = new Set(nodes.map((n) => n.id))
  const links: GraphLink[] = (contents.edges ?? [])
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

/** Rebuild a BoMGraphContents from Explorer/Query canvas state (Open in Composer handoff). */
export function graphContentsFromGraphView(nodes: GraphNode[], links: GraphLink[]): BoMGraphContents {
  return {
    entities: nodes.map((n) => ({
      id: n.id,
      type: n.type,
      schemaVersion: n.schemaVersion === '?' ? undefined : n.schemaVersion,
      payload: n.payload ?? {},
      annotations: n.annotations ?? {},
    })),
    edges: links.map((l) => ({
      id: l.id,
      source: l.source,
      target: l.target,
      role: l.role,
      type: l.type ?? undefined,
      schemaVersion: l.schemaVersion ?? undefined,
      properties: l.properties ?? {},
    })),
  }
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
    if (body != null && typeof body === 'object' && typeof (body as { error?: unknown }).error === 'string') {
      throw new Error((body as { error: string }).error)
    }
    if (typeof body === 'string' && body.length > 0) {
      throw new Error(body)
    }
    throw new Error(`HTTP ${res.status}`)
  }
  return body as T
}

export type BoMGremlinTable = {
  columns: string[]
  rows: unknown[][]
}

export type BoMGremlinViews = {
  graph?: BoMGraphContents | null
  table?: BoMGremlinTable | null
  scalar?: unknown
}

export type BoMGremlinGraphStats = {
  entities: number
  edges: number
}

export type BoMGremlinMeta = {
  strategy: string
  language: string
  subgraph1Stats: BoMGremlinGraphStats
  subgraph2Stats?: BoMGremlinGraphStats | null
  resultCount: number
  durationMs: number
}

export type BoMGremlinItem = {
  kind: string
  value: unknown
}

export type BoMGremlinResult = {
  primary: string
  items: BoMGremlinItem[]
  contents?: BoMGraphContents | null
  views: BoMGremlinViews
  meta: BoMGremlinMeta
}

export type TraverseGremlinRequest = {
  matcher: unknown
  script: string
  strategy?: string
  traversalOptions?: {
    timeoutSeconds?: number
    language?: string
  }
}

export async function traverseGremlin(body: TraverseGremlinRequest): Promise<BoMGremlinResult> {
  const res = await fetch('/api/v1/objs/graph/traverse/gremlin', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  return parseResponse<BoMGremlinResult>(res)
}

/**
 * Graph lifecycle (`/api/v1/objs/graphs`, WI-004/WI-005): header CRUD, membership, graph-scoped
 * mutate/query, and optional clone.
 */
export async function listGraphs(): Promise<BoMGraphListItem[]> {
  const res = await fetch('/api/v1/objs/graphs')
  return parseResponse<BoMGraphListItem[]>(res)
}

/** Build `GET /api/v1/objs/graphs/search` query string (G-U10). */
export function graphSearchQuery(params: {
  q?: string
  expr?: string
  limit?: number
}): string {
  const sp = new URLSearchParams()
  const q = params.q?.trim()
  const expr = params.expr?.trim()
  if (q) sp.set('q', q)
  if (expr) sp.set('expr', expr)
  sp.set('limit', String(params.limit ?? 15))
  return sp.toString()
}

/**
 * Open-graph search (WI-007 / G-U10). Empty `q` without `expr` returns `{ items: [] }` —
 * never the full catalog. Response shape is additive-extendable; callers should ignore unknowns.
 */
export async function searchGraphs(params: {
  q?: string
  expr?: string
  limit?: number
} = {}): Promise<BoMGraphSearchResponse> {
  const res = await fetch(`/api/v1/objs/graphs/search?${graphSearchQuery(params)}`)
  return parseResponse<BoMGraphSearchResponse>(res)
}

export async function getGraph(id: string): Promise<BoMGraphResponse> {
  const res = await fetch(`/api/v1/objs/graphs/${encodeURIComponent(id)}`)
  return parseResponse<BoMGraphResponse>(res)
}

export async function createGraph(body: {
  id?: string
  annotations?: Record<string, string>
  entityIds?: string[]
}): Promise<BoMGraphResponse> {
  const res = await fetch('/api/v1/objs/graphs', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      id: body.id,
      annotations: body.annotations ?? {},
      entityIds: body.entityIds ?? [],
    }),
  })
  return parseResponse<BoMGraphResponse>(res)
}

export async function deleteGraph(id: string): Promise<void> {
  const res = await fetch(`/api/v1/objs/graphs/${encodeURIComponent(id)}`, { method: 'DELETE' })
  if (res.status === 204) return
  await parseResponse(res)
}

export async function cloneGraph(
  id: string,
  annotations: Record<string, string> = {},
): Promise<BoMGraphResponse> {
  const res = await fetch(`/api/v1/objs/graphs/${encodeURIComponent(id)}/clone`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ annotations }),
  })
  return parseResponse<BoMGraphResponse>(res)
}

/** Matcher DSL (`obj-expr` / chained) scoped to this graph's stored members. */
export async function queryInGraph(id: string, matcherBody: unknown): Promise<BoMGraphContents> {
  const res = await fetch(`/api/v1/objs/graphs/${encodeURIComponent(id)}/query`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(matcherBody),
  })
  return parseResponse<BoMGraphContents>(res)
}

/** Matcher DSL over graph headers (stage-0 `all` or `graph-expr`); union of matching graphs. */
export async function queryGraphs(matcherBody: unknown): Promise<BoMGraphContents> {
  const res = await fetch('/api/v1/objs/graphs/query', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(matcherBody),
  })
  return parseResponse<BoMGraphContents>(res)
}

/**
 * Route a built matcher body to the right graph-scoped query endpoint (WI-005). `obj-expr`
 * requires a current graph and is scoped through [queryInGraph]; `all` / `graph-expr` / `chained`
 * select graph(s) by header through [queryGraphs] (backend requires stage-0 `all` or `graph-expr`).
 */
export async function execMatcher(
  mode: MatcherMode,
  body: unknown,
  currentGraphId: string | null,
): Promise<BoMGraphContents> {
  if (mode === 'obj-expr') {
    if (!currentGraphId) {
      throw new Error(
        'Select or create a current graph before running a bare obj-expr matcher (or use graph-expr to open one).',
      )
    }
    return queryInGraph(currentGraphId, body)
  }
  return queryGraphs(body)
}

/**
 * Wrap a built matcher body with a `graph-expr` stage scoping it to [currentGraphId] when needed
 * (WI-005 Query page: `POST /graph/traverse/gremlin` takes a matcher, not a graph id path param).
 */
export function scopeMatcherToGraph(
  mode: MatcherMode,
  body: unknown,
  currentGraphId: string | null,
): unknown {
  if (mode !== 'obj-expr') return body
  if (!currentGraphId) {
    throw new Error(
      'Select or create a current graph before running a bare obj-expr matcher (or use graph-expr to open one).',
    )
  }
  return [{ 'graph-expr': `id == '${currentGraphId}'` }, body]
}

export async function validateGraphDraft(graph: unknown): Promise<GraphValidationResult> {
  const res = await fetch('/api/v1/objs/graph/validate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(graph),
  })
  const body = await res.json().catch(() => null)
  if (!res.ok) {
    const issues = body?.issues
    if (Array.isArray(issues) && issues.length > 0) {
      return body as GraphValidationResult
    }
    throw new Error(typeof body === 'string' ? body : `HTTP ${res.status}`)
  }
  return body as GraphValidationResult
}

export type GraphMutationBody = {
  upsert: {
    entities: unknown
    edges: unknown
  }
  delete: {
    entities: string[]
    edges: string[]
  }
}

export async function validateGraphMutation(mutation: GraphMutationBody): Promise<GraphValidationResult> {
  return validateGraphDraft(mutation)
}

/** Mutate [graphId] in one transaction: upsert lands in the pool + this graph's membership. */
export async function putGraphMutation(
  graphId: string,
  mutation: GraphMutationBody,
): Promise<BoMGraphResponse> {
  const res = await fetch(`/api/v1/objs/graphs/${encodeURIComponent(graphId)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(mutation),
  })
  return parseResponse<BoMGraphResponse>(res)
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

export async function deleteSchemaVersion(type: string, version: string): Promise<void> {
  const res = await fetch(
    `/api/v1/objs/registry/schemas/${encodeURIComponent(type)}/${encodeURIComponent(version)}`,
    { method: 'DELETE' },
  )
  if (res.status === 204) return
  await parseResponse(res)
}

export async function deleteSchemaType(type: string): Promise<void> {
  const res = await fetch(`/api/v1/objs/registry/schemas/${encodeURIComponent(type)}`, {
    method: 'DELETE',
  })
  if (res.status === 204) return
  await parseResponse(res)
}

export async function listEdges(): Promise<BoMAllowedEdgeRule[]> {
  const res = await fetch('/api/v1/objs/registry/edges')
  return parseResponse<BoMAllowedEdgeRule[]>(res)
}

/** Catalog-only seed YAML (ObjectSchema + AllowedEdgeRule). */
export type CatalogExportFormat = 'seeds' | 'json-schema'

export type JsonSchemaExportOptions = {
  dialect?: '2020-12'
  includeEdges?: 'none' | 'outbound' | 'linked'
  includeEdgePropertySchemas?: boolean
}

export const DEFAULT_JSON_SCHEMA_EXPORT_OPTIONS: Required<JsonSchemaExportOptions> = {
  dialect: '2020-12',
  includeEdges: 'outbound',
  includeEdgePropertySchemas: true,
}

export async function exportCatalog(
  format: CatalogExportFormat,
  options?: JsonSchemaExportOptions,
): Promise<string> {
  const params = new URLSearchParams({ format })
  if (format === 'json-schema' && options) {
    if (options.dialect) params.set('dialect', options.dialect)
    if (options.includeEdges) params.set('includeEdges', options.includeEdges)
    if (options.includeEdgePropertySchemas != null) {
      params.set('includeEdgePropertySchemas', String(options.includeEdgePropertySchemas))
    }
  }
  const res = await fetch(`/api/v1/objs/registry/export?${params.toString()}`)
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
  return `/model/${encodeURIComponent(type)}/${encodeURIComponent(version)}`
}

export function schemaCreatePath(kind: 'object' | 'edge'): string {
  return `/model/new?kind=${kind}`
}

/** @deprecated Use in-place Schemas editing; kept for redirect helpers. */
export function schemaLinterPath(type: string, version: string, mode: 'edit' | 'create-version' = 'edit'): string {
  const base = schemaDetailPath(type, version)
  return mode === 'create-version' ? `${base}?mode=create-version` : base
}
