export type BoMEntity = {
  id: string
  type: string
  schemaVersion?: string
  payload?: Record<string, unknown>
  annotations?: Record<string, string>
  createdAt?: string
  updatedAt?: string
  /** Last deep-capture version; absent/null → LATEST in UI. */
  headVersion?: number | null
}

export type BoMEdge = {
  id?: string
  source: string
  target: string
  role: string
  type?: string
  schemaVersion?: string
  properties?: Record<string, unknown>
  headVersion?: number | null
}

export type BoMGraphContents = {
  entities: BoMEntity[]
  edges: BoMEdge[]
}

/** Graph header (`bom_graph`) + resolved member entities and graph-local edges. */
export type BoMGraphResponse = {
  id: string
  annotations: Record<string, string>
  graph: BoMGraphContents
}

/** `GET /api/v1/objs/graphs/{id}/versions` row. */
export type BoMGraphVersionSummary = {
  graphId: string
  version: number
  createdAt: string
  annotations: Record<string, string>
}

/** Entity/edge history list row. */
export type BoMInstanceVersionSummary = {
  id: string
  version: number
  createdAt: string
  updatedAt: string
  annotations?: Record<string, string>
}

/** `GET …/versions/stats` — total + newest recent N. */
export type BoMInstanceVersionStats = {
  total: number
  recent: BoMInstanceVersionSummary[]
}

/** Header-only row from open-graph search (`GET …/graphs/search`). */
export type BoMGraphHeader = {
  id: string
  annotations: Record<string, string>
}

/** `GET /api/v1/objs/graphs/search` envelope (G-U10; additive fields ignored by UI). */
export type BoMGraphSearchResponse = {
  items: BoMGraphHeader[]
}

/** `GET /api/v1/objs/graphs` list row with membership counts. */
export type BoMGraphListItem = {
  id: string
  annotations: Record<string, string>
  entityCount: number
  edgeCount: number
}

export type GraphNode = {
  id: string
  name: string
  type: string
  schemaVersion: string
  color: string
  payload: Record<string, unknown>
  annotations: Record<string, string>
  /** Instance deep-capture version; null/undefined → LATEST. */
  headVersion?: number | null
  /** Top-level payload field kinds from entity schema (for card rendering). */
  payloadFieldKinds?: Record<string, PayloadFieldKind>
  x?: number
  y?: number
  draftStatus?: 'new' | 'modified' | 'deleted' | 'unchanged'
  /** Dimmed in Composer “Changes only” mode (unchanged items stay visible for stable layout). */
  dimmed?: boolean
  /** Failing latest Validate — red blink until result cleared/revalidated. */
  validationError?: boolean
}

/** How a top-level payload field should render on graph cards. */
export type PayloadFieldKind =
  | 'STRING'
  | 'ENUM'
  | 'NUMBER'
  | 'INTEGER'
  | 'BOOLEAN'
  | 'ARRAY'
  | 'OBJECT'
  | 'OTHER'

export type GraphLink = {
  id: string
  source: string
  target: string
  role: string
  type: string | null
  schemaVersion: string | null
  properties: Record<string, unknown>
  headVersion?: number | null
  draftStatus?: 'new' | 'modified' | 'deleted' | 'unchanged'
  /** Dimmed in Composer “Changes only” mode. */
  dimmed?: boolean
  /** Failing latest Validate — red blink until result cleared/revalidated. */
  validationError?: boolean
}

export type GraphSelection =
  | { kind: 'node'; node: GraphNode }
  | { kind: 'edge'; edge: GraphLink }

export type BoMSchemaUsage = 'ENTITY' | 'EDGE_PROPERTIES'

export type BoMSchemaType =
  | 'OBJECT'
  | 'ARRAY'
  | 'STRING'
  | 'NUMBER'
  | 'INTEGER'
  | 'BOOLEAN'
  | 'ENUM'

export type BoMEnumValue = {
  value: string
  description: string
  /** Short UI label; omit to show [value] in editors. */
  caption?: string | null
}

/** Dropdown / read-view label: caption if set, otherwise stored value. */
export function enumCaption(entry: { value: string; caption?: string | null }): string {
  const caption = entry.caption?.trim()
  return caption || entry.value
}

export type BoMSchemaField = {
  name: string
  schema: BoMSchemaNode
  required?: boolean
  identifier?: boolean
  searchable?: boolean
  stereotype?: string[]
  tags?: string[]
  attributes?: Record<string, string>
}

export type BoMSchemaNode = {
  type: BoMSchemaType
  title: string
  description: string
  fields?: BoMSchemaField[]
  items?: BoMSchemaNode
  values?: BoMEnumValue[]
  format?: string | null
  default?: unknown
}

export type BoMSchema = {
  type: string
  version: string
  contentSchema: BoMSchemaNode
  usage: BoMSchemaUsage
  tags?: string[]
  attributes?: Record<string, string>
}

export type BoMEdgeCardinality = 'UNSPECIFIED' | '1:1' | '1:*'

export type BoMAllowedEdgeRule = {
  sourceType: string
  role: string
  targetType: string
  propertiesPolicy: 'NONE' | 'SCHEMA'
  emptyPropertiesAllowed: boolean
  propertiesSchemaType?: string | null
  propertiesSchemaVersion?: string | null
  cardinality?: BoMEdgeCardinality
  description?: string | null
  sourceVerb?: string | null
  targetVerb?: string | null
  tags?: string[]
  attributes?: Record<string, string>
}

export type EdgeRelationRequest = {
  sourceType: string
  role: string
  targetType: string
  emptyPropertiesAllowed: boolean
  cardinality?: BoMEdgeCardinality
}

export type TypeEdgesResponse = {
  incoming: BoMAllowedEdgeRule[]
  outgoing: BoMAllowedEdgeRule[]
}

export type BoMValidationIssue = {
  code: string
  message: string
  path?: string | null
}

export type SchemaDefinitionRequest = {
  contentSchema: BoMSchemaNode
  usage?: BoMSchemaUsage
  tags?: string[]
  attributes?: Record<string, string>
}

export type SchemaLintResponse = {
  valid: boolean
  issues: BoMValidationIssue[]
  schema?: BoMSchema | null
  jsonSchema?: Record<string, unknown> | null
}

export type GraphValidationResult = {
  issues: BoMValidationIssue[]
  isValid?: boolean
  valid?: boolean
}

export type SeedDocumentResult = {
  index: number
  kind?: string | null
  apiVersion?: string | null
  identity?: string | null
  applied: boolean
  skipped: boolean
  errors: BoMValidationIssue[]
  warnings: string[]
}

export type SeedImportResult = {
  documents: SeedDocumentResult[]
  warnings: string[]
}

/** `GET /api/v1/objs/graph/algorithms/capabilities` */
export type GraphAlgorithmCapabilities = {
  algorithms: GraphAlgorithmCapability[]
}

export type GraphAlgorithmCapability = {
  id: string
  materializationModes: string[]
}

/** `POST /api/v1/objs/graph/algorithms/cycles` request */
export type GraphCycleAnalysisRequest = {
  matcher: unknown
  graphId?: string
  graphVersion?: number
  algorithm?: string
  materialization?: string
}

/** Directed SCC cycle-region analysis result */
export type GraphCycleAnalysis = {
  algorithm: string
  components: GraphCycleComponent[]
  stats: GraphAnalysisStats
  diagnostics?: GraphFragmentDiagnostic[]
}

export type GraphCycleComponent = {
  id: string
  entityIds: string[]
  edgeIds: string[]
}

export type GraphAnalysisStats = {
  entityCount: number
  edgeCount: number
  cyclicComponentCount: number
}

export type GraphFragmentDiagnostic = {
  code: string
  message: string
  entityId?: string | null
  edgeId?: string | null
}
