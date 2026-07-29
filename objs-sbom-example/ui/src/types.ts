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

export type GraphNode = {
  id: string
  name: string
  type: string
  schemaVersion: string
  color: string
  payload: Record<string, unknown>
  annotations: Record<string, string>
  x?: number
  y?: number
}

export type GraphLink = {
  id: string
  source: string
  target: string
  role: string
  type: string | null
  schemaVersion: string | null
  properties: Record<string, unknown>
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

export type BoMAllowedEdgeRule = {
  sourceType: string
  role: string
  targetType: string
  propertiesPolicy: 'NONE' | 'SCHEMA'
  emptyPropertiesAllowed: boolean
  propertiesSchemaType?: string | null
  propertiesSchemaVersion?: string | null
}

export type EdgeRelationRequest = {
  sourceType: string
  role: string
  targetType: string
  emptyPropertiesAllowed: boolean
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
