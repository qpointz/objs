const API = '/api/v1/asset-repository'

export type CollectionType = {
  id: string
  objectType: string
  metadata: string | null
}

export type Collection = {
  id: string
  name: string
  description: string | null
  owner: string
  ownerEmail: string | null
  supportEmail: string | null
  sla: string | null
  objectWriteMode: string
  graphId: string
  types: CollectionType[]
}

export type CollectionStatistics = {
  collectionId: string
  objectCount: number
  lastUpdated: string | null
}

export type ArObject = {
  id: string
  type: string
  schemaVersion: string
  payload: Record<string, unknown>
}

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
  identifier?: boolean
  searchable?: boolean
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
  usage?: string
}

export type CollectionRef = {
  id: string
  name: string
}

export type SchemaCatalogEntry = {
  type: string
  latestVersion: string
  versions: string[]
  title: string
  description: string
  usage: string
  usedIn: CollectionRef[]
}

async function json<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = await res.text()
    throw new Error(body || res.statusText)
  }
  if (res.status === 204) {
    return undefined as T
  }
  return res.json() as Promise<T>
}

export function listCollections(params?: {
  name?: string
  owner?: string
  acceptedType?: string
}): Promise<Collection[]> {
  const q = new URLSearchParams()
  if (params?.name) q.set('name', params.name)
  if (params?.owner) q.set('owner', params.owner)
  if (params?.acceptedType) q.set('acceptedType', params.acceptedType)
  const qs = q.toString()
  return fetch(`${API}/collections${qs ? `?${qs}` : ''}`).then((r) => json(r))
}

export function getCollection(id: string): Promise<Collection> {
  return fetch(`${API}/collections/${id}`).then((r) => json(r))
}

export function getCollectionStatistics(id: string): Promise<CollectionStatistics> {
  return fetch(`${API}/collections/${id}/statistics`).then((r) => json(r))
}

export function createCollection(body: unknown): Promise<Collection> {
  return fetch(`${API}/collections`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then((r) => json(r))
}

export function patchCollection(id: string, body: unknown): Promise<Collection> {
  return fetch(`${API}/collections/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then((r) => json(r))
}

export function listObjects(collectionId: string): Promise<ArObject[]> {
  return fetch(`${API}/collections/${collectionId}/objects`).then((r) => json(r))
}

export function getObject(collectionId: string, objectId: string): Promise<ArObject> {
  return fetch(`${API}/collections/${collectionId}/objects/${objectId}`).then((r) => json(r))
}

export type ObjectRelation = {
  edgeId: string
  role: string
  direction: 'OUTGOING' | 'INCOMING'
  related: ArObject
}

export function listObjectRelations(collectionId: string, objectId: string): Promise<ObjectRelation[]> {
  return fetch(`${API}/collections/${collectionId}/objects/${objectId}/relations`).then((r) => json(r))
}

export function writeObject(collectionId: string, body: unknown): Promise<ArObject> {
  return fetch(`${API}/collections/${collectionId}/objects`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then((r) => json(r))
}

export type CompositionRelation = {
  sourceKey: string
  role: string
  targetKey: string
}

export type CompositionRequest = {
  objects: unknown[]
  relations?: CompositionRelation[]
}

export function writeComposition(collectionId: string, body: CompositionRequest): Promise<ArObject[]> {
  return fetch(`${API}/collections/${collectionId}/compositions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then((r) => json(r))
}

export function searchObjects(
  collectionId: string,
  body: { filters?: Record<string, string>; matcherExpr?: string },
): Promise<ArObject[]> {
  return fetch(`${API}/collections/${collectionId}/objects/search`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then((r) => json(r))
}

export function listSchemas(type?: string): Promise<BoMSchema[]> {
  const q = type ? `?type=${encodeURIComponent(type)}` : ''
  return fetch(`${API}/schemas${q}`).then((r) => json(r))
}

export function listSchemasByType(type: string): Promise<BoMSchema[]> {
  return fetch(`${API}/schemas/${encodeURIComponent(type)}`).then((r) => json(r))
}

export function getSchema(type: string, version: string): Promise<BoMSchema> {
  return fetch(
    `${API}/schemas/${encodeURIComponent(type)}/${encodeURIComponent(version)}`,
  ).then((r) => json(r))
}

export function listCollectionSchemas(collectionId: string): Promise<BoMSchema[]> {
  return fetch(`${API}/collections/${collectionId}/schemas`).then((r) => json(r))
}

export function listSchemaCatalog(): Promise<SchemaCatalogEntry[]> {
  return fetch(`${API}/schema-catalog`).then((r) => json(r))
}
