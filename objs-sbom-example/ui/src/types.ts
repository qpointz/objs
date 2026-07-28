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
