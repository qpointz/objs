import type {
  BoMSchema,
  BoMSchemaUsage,
  BoMGraphContents,
  BoMAllowedEdgeRule,
  BoMGraphListItem,
  BoMGraphResponse,
  BoMGraphSearchResponse,
  BoMGraphVersionSummary,
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

export function toGraphData(
  contents: BoMGraphContents,
  schemas?: { type: string; attributes?: Record<string, string> }[],
): { nodes: GraphNode[]; links: GraphLink[] } {
  const colorByType = new Map<string, string>()
  for (const schema of schemas ?? []) {
    colorByType.set(schema.type, colorForType(schema.type, schema.attributes))
  }
  const nodes: GraphNode[] = (contents.entities ?? []).map((e) => ({
    id: e.id,
    name: nodeLabel(e.payload, e.id),
    type: e.type,
    schemaVersion: e.schemaVersion ?? '?',
    color: colorByType.get(e.type) ?? colorForType(e.type),
    payload: e.payload ?? {},
    annotations: e.annotations ?? {},
    headVersion: e.headVersion ?? null,
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
      headVersion: edge.headVersion ?? null,
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
  /** When set with graphVersion, traverse a reconstructed pin. */
  graphId?: string
  graphVersion?: number
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

export async function putGraphAnnotations(
  id: string,
  annotations: Record<string, string>,
): Promise<BoMGraphResponse> {
  const res = await fetch(`/api/v1/objs/graphs/${encodeURIComponent(id)}/annotations`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ annotations }),
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

export async function createGraphVersion(
  id: string,
  annotations: Record<string, string> = {},
): Promise<BoMGraphVersionSummary> {
  const res = await fetch(`/api/v1/objs/graphs/${encodeURIComponent(id)}/versions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ annotations }),
  })
  return parseResponse(res)
}

export async function listGraphVersions(
  id: string,
): Promise<BoMGraphVersionSummary[]> {
  const res = await fetch(`/api/v1/objs/graphs/${encodeURIComponent(id)}/versions`)
  return parseResponse(res)
}

export async function getGraphVersion(
  id: string,
  version: number,
): Promise<BoMGraphResponse> {
  const res = await fetch(
    `/api/v1/objs/graphs/${encodeURIComponent(id)}/versions/${encodeURIComponent(String(version))}`,
  )
  return parseResponse<BoMGraphResponse>(res)
}

/** Matcher DSL scoped to a reconstructed deep graph version. */
export async function queryInGraphVersion(
  id: string,
  version: number,
  matcherBody: unknown,
): Promise<BoMGraphContents> {
  const res = await fetch(
    `/api/v1/objs/graphs/${encodeURIComponent(id)}/versions/${encodeURIComponent(String(version))}/query`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(matcherBody),
    },
  )
  return parseResponse<BoMGraphContents>(res)
}

export async function listEntityVersions(id: string): Promise<import('./types').BoMInstanceVersionSummary[]> {
  const res = await fetch(`/api/v1/objs/entities/${encodeURIComponent(id)}/versions`)
  return parseResponse(res)
}

export async function entityVersionStats(
  id: string,
  recent = 5,
): Promise<import('./types').BoMInstanceVersionStats> {
  const res = await fetch(
    `/api/v1/objs/entities/${encodeURIComponent(id)}/versions/stats?recent=${encodeURIComponent(String(recent))}`,
  )
  return parseResponse(res)
}

export async function getEntityVersion(
  id: string,
  version: number,
): Promise<import('./types').BoMEntity> {
  const res = await fetch(
    `/api/v1/objs/entities/${encodeURIComponent(id)}/versions/${encodeURIComponent(String(version))}`,
  )
  return parseResponse(res)
}

export async function listEdgeVersions(id: string): Promise<import('./types').BoMInstanceVersionSummary[]> {
  const res = await fetch(`/api/v1/objs/edges/${encodeURIComponent(id)}/versions`)
  return parseResponse(res)
}

export async function edgeVersionStats(
  id: string,
  recent = 5,
): Promise<import('./types').BoMInstanceVersionStats> {
  const res = await fetch(
    `/api/v1/objs/edges/${encodeURIComponent(id)}/versions/stats?recent=${encodeURIComponent(String(recent))}`,
  )
  return parseResponse(res)
}

export async function getEdgeVersion(
  id: string,
  version: number,
): Promise<import('./types').BoMEdge> {
  const res = await fetch(
    `/api/v1/objs/edges/${encodeURIComponent(id)}/versions/${encodeURIComponent(String(version))}`,
  )
  return parseResponse(res)
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

/** True when [body] is a bare `{ "obj-expr": "..." }` object (not a chain). */
export function isBareObjExprMatcher(body: unknown): boolean {
  return (
    body != null &&
    typeof body === 'object' &&
    !Array.isArray(body) &&
    typeof (body as Record<string, unknown>)['obj-expr'] === 'string'
  )
}

/**
 * Add-objects / Objects-page Search routing when [graphId] is supplied.
 * With [graphId]: `POST …/graphs/{id}/query` (Objects page in graph context). Without: bare
 * `obj-expr` (or obj-expr-only chain) hits `POST …/entities/query` (whole pool). `all` /
 * `graph-expr` use `POST …/graphs/query`. Composer Add objects passes null [graphId] so Search
 * always uses pool / cross-graph endpoints.
 */
export function scopeAddObjectsMatcher(body: unknown, graphId: string | null): {
  kind: 'in-graph' | 'graphs' | 'pool'
  graphId?: string
  body: unknown
} {
  if (graphId) {
    return { kind: 'in-graph', graphId, body }
  }
  if (isPoolObjExprMatcher(body)) {
    return { kind: 'pool', body }
  }
  return { kind: 'graphs', body }
}

/** True when [body] is bare obj-expr or a non-empty chain of only obj-expr stages. */
export function isPoolObjExprMatcher(body: unknown): boolean {
  if (isBareObjExprMatcher(body)) return true
  if (!Array.isArray(body) || body.length === 0) return false
  return body.every(
    (stage) =>
      stage != null &&
      typeof stage === 'object' &&
      !Array.isArray(stage) &&
      typeof (stage as Record<string, unknown>)['obj-expr'] === 'string' &&
      Object.keys(stage as object).length === 1,
  )
}

/** Matcher DSL over the entity pool (orphans included); edges always empty. */
export async function queryEntities(matcherBody: unknown): Promise<BoMGraphContents> {
  const res = await fetch('/api/v1/objs/entities/query?page=1&size=100', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(matcherBody),
  })
  return parseResponse<BoMGraphContents>(res)
}

/** Run Add objects / Objects Search — pool, in-graph, or cross-graph per [graphId] and matcher. */
export async function queryAddObjects(
  body: unknown,
  graphId: string | null,
  graphVersion: number | null = null,
): Promise<BoMGraphContents> {
  const scoped = scopeAddObjectsMatcher(body, graphId)
  if (scoped.kind === 'in-graph') {
    if (graphVersion != null) {
      return queryInGraphVersion(scoped.graphId!, graphVersion, scoped.body)
    }
    return queryInGraph(scoped.graphId!, scoped.body)
  }
  if (scoped.kind === 'pool') {
    return queryEntities(scoped.body)
  }
  return queryGraphs(scoped.body)
}

/**
 * Route a built matcher body to the right query endpoint (Explorer / Query helpers).
 * With a current graph, bare `obj-expr` uses `POST …/graphs/{id}/query`. Without one, bare
 * `obj-expr` uses `POST …/entities/query` (pool, orphans included) — same as Composer Add objects.
 */
export async function execMatcher(
  mode: MatcherMode,
  body: unknown,
  currentGraphId: string | null,
): Promise<BoMGraphContents> {
  if (mode === 'obj-expr') {
    if (currentGraphId) {
      return queryInGraph(currentGraphId, body)
    }
    return queryEntities(body)
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
  entities: {
    set: unknown
    unset: string[]
  }
  edges: {
    set: unknown
    unset: string[]
  }
}

export type GraphMutateMode = 'merge' | 'replace'

/** Pool-scoped dry-run (MERGE). Prefer graph-scoped validate when a graph id is known. */
export async function validateGraphMutation(
  mutation: GraphMutationBody,
  options?: { graphId?: string; mode?: GraphMutateMode },
): Promise<GraphValidationResult> {
  if (options?.graphId) {
    const mode = options.mode ?? 'merge'
    const method = mode === 'replace' ? 'PUT' : 'PATCH'
    const res = await fetch(
      `/api/v1/objs/graphs/${encodeURIComponent(options.graphId)}/validate`,
      {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(mutation),
      },
    )
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
  return validateGraphDraft(mutation)
}

/** MERGE mutate: set + unset; omission keeps. */
export async function patchGraphMutation(
  graphId: string,
  mutation: GraphMutationBody,
): Promise<BoMGraphResponse> {
  const res = await fetch(`/api/v1/objs/graphs/${encodeURIComponent(graphId)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(mutation),
  })
  return parseResponse<BoMGraphResponse>(res)
}

/** REPLACE mutate: set is full desired membership + edges. */
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
  description?: string | null
  sourceVerb?: string | null
  targetVerb?: string | null
  tags?: string[]
  attributes?: Record<string, string>
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
export type CatalogExportFormat = 'seeds' | 'json-schema' | 'json-schema-codegen'

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

export function isJsonSchemaCatalogFormat(
  format: CatalogExportFormat,
): format is 'json-schema' | 'json-schema-codegen' {
  return format === 'json-schema' || format === 'json-schema-codegen'
}

export async function exportCatalog(
  format: CatalogExportFormat,
  options?: JsonSchemaExportOptions,
): Promise<string> {
  const params = new URLSearchParams({ format })
  if (isJsonSchemaCatalogFormat(format) && options) {
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
  if (isJsonSchemaCatalogFormat(format)) {
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
