export type BoMEntity = {
  id: string
  type: string
  schemaVersion?: string
  payload?: Record<string, unknown>
  annotations?: Record<string, string>
}

export type BoMEdge = {
  id?: string
  source: string
  target: string
  role: string
  type?: string
  schemaVersion?: string
  properties?: Record<string, unknown>
}

export type BoMSubgraph = {
  entities: BoMEntity[]
  edges: BoMEdge[]
}

/** Soft-link / evidence-pack subgraph header + resolved members. */
export type SoftLinkSubgraph = {
  id: string
  annotations: Record<string, string>
  subgraph: BoMSubgraph
}

export type SoftLinkSubgraphListItem = {
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
}

export type BoMSchemaField = {
  name: string
  schema: BoMSchemaNode
  required?: boolean
  stereotype?: string[]
}

export type BoMSchemaNode = {
  type: BoMSchemaType
  title: string
  description: string
  fields?: BoMSchemaField[]
  items?: BoMSchemaNode
  values?: BoMEnumValue[]
  format?: string | null
  required?: string[]
  default?: unknown
}

export type BoMSchema = {
  type: string
  version: string
  contentSchema: BoMSchemaNode
  usages: BoMSchemaUsage[]
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
  usages?: BoMSchemaUsage[]
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
