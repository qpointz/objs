import type { BoMEdge, BoMEntity, BoMGraphContents } from './types'

export type GraphDraftDocument = {
  entities: BoMEntity[]
  edges: BoMEdge[]
}

export type DraftItemStatus = 'new' | 'modified' | 'deleted' | 'unchanged'

export type GraphDraftState = {
  /** Live working graph (baseline ∪ upserts − soft-deletes). Visual uses visualDocument(). */
  document: GraphDraftDocument
  baselineEntityIds: Set<string>
  baselineEdgeIds: Set<string>
  /** Snapshot at last Load / Apply — used for modified detection and undo. */
  baselineEntities: Map<string, BoMEntity>
  baselineEdges: Map<string, BoMEdge>
  /** Soft-deleted loaded items kept for the visual canvas only. */
  softDeletedEntities: Map<string, BoMEntity>
  softDeletedEdges: Map<string, BoMEdge>
  pendingDeleteEntityIds: Set<string>
  pendingDeleteEdgeIds: Set<string>
}

/** Text / UI / API mutation shape (BoMGraphMutation) — kind-first set/unset. */
export type GraphMutationDocument = {
  entities: {
    set: BoMEntity[]
    unset: string[]
  }
  edges: {
    set: BoMEdge[]
    unset: string[]
  }
}

export const EMPTY_MUTATION: GraphMutationDocument = {
  entities: { set: [], unset: [] },
  edges: { set: [], unset: [] },
}

export const EMPTY_GRAPH: GraphDraftDocument = {
  entities: [],
  edges: [],
}

export const EXAMPLE_GRAPH: GraphDraftDocument = {
  entities: [
    {
      id: '11111111-1111-4111-8111-111111111111',
      type: 'Product',
      schemaVersion: '1.0.0',
      payload: {
        name: 'Payments API',
        version: '2.3.1',
      },
      annotations: {
        app: 'payments-api',
        appVersion: '2.3.1',
      },
    },
    {
      id: '22222222-2222-4222-8222-222222222222',
      type: 'Component',
      schemaVersion: '1.0.0',
      payload: {
        name: 'payment-core',
        version: '2.3.1',
        ecosystem: 'maven',
        kind: 'library',
      },
      annotations: {
        app: 'payments-api',
        appVersion: '2.3.1',
      },
    },
  ],
  edges: [
    {
      id: '33333333-3333-4333-8333-333333333333',
      source: '11111111-1111-4111-8111-111111111111',
      target: '22222222-2222-4222-8222-222222222222',
      role: 'CONTAINS',
      type: 'CanonicalEdge',
      schemaVersion: '1.0.0',
      properties: {},
    },
  ],
}

export function graphShapeError(value: unknown): string | null {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return 'Graph document must be an object'
  }
  const graph = value as Record<string, unknown>
  if (!Array.isArray(graph.entities)) return 'Graph document must contain an entities array'
  if (!Array.isArray(graph.edges)) return 'Graph document must contain an edges array'
  return null
}

export function mutationShapeError(value: unknown): string | null {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return 'Mutation must be an object'
  }
  const root = value as Record<string, unknown>
  const entities = root.entities
  const edges = root.edges
  if (!entities || typeof entities !== 'object' || Array.isArray(entities)) {
    return 'Mutation must contain an entities object'
  }
  if (!edges || typeof edges !== 'object' || Array.isArray(edges)) {
    return 'Mutation must contain an edges object'
  }
  const e = entities as Record<string, unknown>
  const g = edges as Record<string, unknown>
  if (!Array.isArray(e.set)) return 'entities.set must be an array'
  if (!Array.isArray(e.unset)) return 'entities.unset must be an array of ids'
  if (!Array.isArray(g.set)) return 'edges.set must be an array'
  if (!Array.isArray(g.unset)) return 'edges.unset must be an array of ids'
  return null
}

export function normalizeGraphDocument(value: unknown): GraphDraftDocument | null {
  if (graphShapeError(value)) return null
  const raw = value as { entities: unknown[]; edges: unknown[] }
  const entities: BoMEntity[] = raw.entities.map((item, index) => {
    const e = (item ?? {}) as Record<string, unknown>
    return {
      id: typeof e.id === 'string' && e.id ? e.id : `draft-entity-${index}`,
      type: String(e.type ?? ''),
      schemaVersion: e.schemaVersion != null ? String(e.schemaVersion) : undefined,
      payload: (e.payload as Record<string, unknown>) ?? {},
      annotations: normalizeAnnotations(e.annotations),
    }
  })
  const edges: BoMEdge[] = raw.edges.map((item, index) => {
    const edge = (item ?? {}) as Record<string, unknown>
    return {
      id: typeof edge.id === 'string' && edge.id ? edge.id : `draft-edge-${index}`,
      source: String(edge.source ?? ''),
      target: String(edge.target ?? ''),
      role: String(edge.role ?? ''),
      type: edge.type != null ? String(edge.type) : undefined,
      schemaVersion: edge.schemaVersion != null ? String(edge.schemaVersion) : undefined,
      properties: (edge.properties as Record<string, unknown>) ?? {},
    }
  })
  return { entities, edges }
}

function normalizeIdList(value: unknown): string[] {
  if (!Array.isArray(value)) return []
  return value.map((id) => String(id)).filter(Boolean)
}

export function normalizeGraphMutation(value: unknown): GraphMutationDocument | null {
  if (mutationShapeError(value)) return null
  const root = value as {
    entities: { set: unknown[]; unset: unknown[] }
    edges: { set: unknown[]; unset: unknown[] }
  }
  const setDoc = normalizeGraphDocument({
    entities: root.entities.set,
    edges: root.edges.set,
  })
  if (!setDoc) return null
  return {
    entities: {
      set: setDoc.entities,
      unset: normalizeIdList(root.entities.unset),
    },
    edges: {
      set: setDoc.edges,
      unset: normalizeIdList(root.edges.unset),
    },
  }
}

/** Sets + unsets for Text / Validate / Apply — excludes unchanged loaded items. */
export function buildMutationDocument(state: GraphDraftState): GraphMutationDocument {
  const entities = state.document.entities
    .filter((e) => {
      const status = entityStatus(state, e.id)
      return status === 'new' || status === 'modified'
    })
    .map(cloneEntity)
  const edges = state.document.edges
    .filter((e) => {
      if (!e.id) return true
      const status = edgeStatus(state, e.id)
      return status === 'new' || status === 'modified'
    })
    .map(cloneEdge)
  return {
    entities: {
      set: entities,
      unset: [...state.pendingDeleteEntityIds].sort(),
    },
    edges: {
      set: edges,
      unset: [...state.pendingDeleteEdgeIds].sort(),
    },
  }
}

/**
 * Rebuild live document from baseline + mutation text.
 * Unchanged baseline items stay on the canvas but are not part of the mutation.
 */
export function applyMutationDocument(
  state: GraphDraftState,
  mutation: GraphMutationDocument,
): GraphDraftState {
  const pendingDeleteEntityIds = new Set(
    mutation.entities.unset.filter((id) => state.baselineEntityIds.has(id)),
  )
  const pendingDeleteEdgeIds = new Set(
    mutation.edges.unset.filter((id) => state.baselineEdgeIds.has(id)),
  )

  const softDeletedEntities = new Map<string, BoMEntity>()
  const softDeletedEdges = new Map<string, BoMEdge>()
  const setById = new Map(mutation.entities.set.map((e) => [e.id, cloneEntity(e)]))

  const liveEntities: BoMEntity[] = []
  for (const id of state.baselineEntityIds) {
    if (pendingDeleteEntityIds.has(id)) {
      softDeletedEntities.set(
        id,
        cloneEntity(state.baselineEntities.get(id) ?? state.softDeletedEntities.get(id)!),
      )
      continue
    }
    const setEntity = setById.get(id)
    if (setEntity) {
      liveEntities.push(setEntity)
      setById.delete(id)
    } else {
      liveEntities.push(cloneEntity(state.baselineEntities.get(id)!))
    }
  }
  for (const entity of setById.values()) {
    liveEntities.push(entity)
  }

  const mutationEdgeById = new Map(
    mutation.edges.set.filter((e) => e.id).map((e) => [e.id as string, cloneEdge(e)]),
  )
  const usedMutationEdgeIds = new Set<string>()
  const liveEdges: BoMEdge[] = []

  for (const id of state.baselineEdgeIds) {
    const baselineEdge = state.baselineEdges.get(id)!
    if (
      pendingDeleteEdgeIds.has(id) ||
      pendingDeleteEntityIds.has(baselineEdge.source) ||
      pendingDeleteEntityIds.has(baselineEdge.target)
    ) {
      pendingDeleteEdgeIds.add(id)
      softDeletedEdges.set(
        id,
        cloneEdge(state.baselineEdges.get(id) ?? state.softDeletedEdges.get(id)!),
      )
      continue
    }
    const setEdge = mutationEdgeById.get(id)
    if (setEdge) {
      liveEdges.push(setEdge)
      usedMutationEdgeIds.add(id)
    } else {
      liveEdges.push(cloneEdge(baselineEdge))
    }
  }
  for (const edge of mutation.edges.set) {
    if (edge.id && usedMutationEdgeIds.has(edge.id)) continue
    if (edge.id && pendingDeleteEdgeIds.has(edge.id)) continue
    if (pendingDeleteEntityIds.has(edge.source) || pendingDeleteEntityIds.has(edge.target)) {
      continue
    }
    liveEdges.push(cloneEdge(edge))
  }

  return {
    ...state,
    document: { entities: liveEntities, edges: liveEdges },
    softDeletedEntities,
    softDeletedEdges,
    pendingDeleteEntityIds,
    pendingDeleteEdgeIds,
  }
}

function normalizeAnnotations(value: unknown): Record<string, string> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return {}
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>).map(([k, v]) => [k, v == null ? '' : String(v)]),
  )
}

export function cloneEntity(entity: BoMEntity): BoMEntity {
  return {
    ...entity,
    payload: { ...(entity.payload ?? {}) },
    annotations: { ...(entity.annotations ?? {}) },
  }
}

export function cloneEdge(edge: BoMEdge): BoMEdge {
  return {
    ...edge,
    properties: { ...(edge.properties ?? {}) },
  }
}

export function cloneDocument(doc: GraphDraftDocument): GraphDraftDocument {
  return {
    entities: doc.entities.map(cloneEntity),
    edges: doc.edges.map(cloneEdge),
  }
}

function snapshotMaps(doc: GraphDraftDocument): {
  baselineEntities: Map<string, BoMEntity>
  baselineEdges: Map<string, BoMEdge>
  baselineEntityIds: Set<string>
  baselineEdgeIds: Set<string>
} {
  const baselineEntities = new Map(doc.entities.map((e) => [e.id, cloneEntity(e)]))
  const baselineEdges = new Map(
    doc.edges.filter((e) => e.id).map((e) => [e.id as string, cloneEdge(e)]),
  )
  return {
    baselineEntities,
    baselineEdges,
    baselineEntityIds: new Set(baselineEntities.keys()),
    baselineEdgeIds: new Set(baselineEdges.keys()),
  }
}

export function emptyDraftState(document: GraphDraftDocument = EMPTY_GRAPH): GraphDraftState {
  return {
    document: cloneDocument(document),
    baselineEntityIds: new Set(),
    baselineEdgeIds: new Set(),
    baselineEntities: new Map(),
    baselineEdges: new Map(),
    softDeletedEntities: new Map(),
    softDeletedEdges: new Map(),
    pendingDeleteEntityIds: new Set(),
    pendingDeleteEdgeIds: new Set(),
  }
}

export function draftFromGraphContents(contents: BoMGraphContents): GraphDraftState {
  const document: GraphDraftDocument = {
    entities: (contents.entities ?? []).map(cloneEntity),
    edges: (contents.edges ?? []).map((edge) =>
      cloneEdge({
        ...edge,
        id: edge.id ?? crypto.randomUUID(),
      }),
    ),
  }
  const snap = snapshotMaps(document)
  return {
    document,
    ...snap,
    softDeletedEntities: new Map(),
    softDeletedEdges: new Map(),
    pendingDeleteEntityIds: new Set(),
    pendingDeleteEdgeIds: new Set(),
  }
}

export function stableStringify(value: unknown): string {
  return JSON.stringify(value, (_, v) => {
    if (v && typeof v === 'object' && !Array.isArray(v)) {
      return Object.fromEntries(Object.entries(v as Record<string, unknown>).sort(([a], [b]) => a.localeCompare(b)))
    }
    return v
  })
}

export function entityEquals(a: BoMEntity, b: BoMEntity): boolean {
  return (
    a.id === b.id &&
    a.type === b.type &&
    (a.schemaVersion ?? '') === (b.schemaVersion ?? '') &&
    stableStringify(a.payload ?? {}) === stableStringify(b.payload ?? {}) &&
    stableStringify(a.annotations ?? {}) === stableStringify(b.annotations ?? {})
  )
}

export function edgeEquals(a: BoMEdge, b: BoMEdge): boolean {
  return (
    (a.id ?? '') === (b.id ?? '') &&
    a.source === b.source &&
    a.target === b.target &&
    a.role === b.role &&
    (a.type ?? '') === (b.type ?? '') &&
    (a.schemaVersion ?? '') === (b.schemaVersion ?? '') &&
    stableStringify(a.properties ?? {}) === stableStringify(b.properties ?? {})
  )
}

export function entityStatus(state: GraphDraftState, entityId: string): DraftItemStatus {
  if (state.pendingDeleteEntityIds.has(entityId)) return 'deleted'
  if (!state.baselineEntityIds.has(entityId)) return 'new'
  const current = state.document.entities.find((e) => e.id === entityId)
  const baseline = state.baselineEntities.get(entityId)
  if (!current || !baseline) return 'unchanged'
  return entityEquals(current, baseline) ? 'unchanged' : 'modified'
}

export function edgeStatus(state: GraphDraftState, edgeId: string): DraftItemStatus {
  if (state.pendingDeleteEdgeIds.has(edgeId)) return 'deleted'
  if (!state.baselineEdgeIds.has(edgeId)) return 'new'
  const current = state.document.edges.find((e) => e.id === edgeId)
  const baseline = state.baselineEdges.get(edgeId)
  if (!current || !baseline) return 'unchanged'
  return edgeEquals(current, baseline) ? 'unchanged' : 'modified'
}

/** Entities/edges shown on the canvas (live + soft-deleted). */
export function visualDocument(state: GraphDraftState): GraphDraftDocument {
  return {
    entities: [...state.document.entities, ...state.softDeletedEntities.values()],
    edges: [...state.document.edges, ...state.softDeletedEdges.values()],
  }
}

export function replaceDocument(
  state: GraphDraftState,
  document: GraphDraftDocument,
): GraphDraftState {
  const next = cloneDocument(document)
  const entityIds = new Set(next.entities.map((e) => e.id))

  const softDeletedEntities = new Map(state.softDeletedEntities)
  const softDeletedEdges = new Map(state.softDeletedEdges)
  const pendingDeleteEntityIds = new Set<string>()
  const pendingDeleteEdgeIds = new Set<string>()

  for (const id of state.baselineEntityIds) {
    if (!entityIds.has(id)) {
      pendingDeleteEntityIds.add(id)
      softDeletedEntities.set(
        id,
        cloneEntity(state.baselineEntities.get(id) ?? softDeletedEntities.get(id)!),
      )
    } else {
      softDeletedEntities.delete(id)
    }
  }

  // Never keep a live edge whose endpoint is pending delete (e.g. stale text sync
  // re-introduces the edge after a cascaded soft-delete of the other node).
  const liveEdges: BoMEdge[] = []
  for (const edge of next.edges) {
    if (pendingDeleteEntityIds.has(edge.source) || pendingDeleteEntityIds.has(edge.target)) {
      if (edge.id && state.baselineEdgeIds.has(edge.id)) {
        pendingDeleteEdgeIds.add(edge.id)
        softDeletedEdges.set(edge.id, cloneEdge(edge))
      }
      continue
    }
    liveEdges.push(edge)
  }
  next.edges = liveEdges

  const edgeIds = new Set(next.edges.map((e) => e.id).filter((id): id is string => Boolean(id)))
  for (const id of state.baselineEdgeIds) {
    if (!edgeIds.has(id)) {
      pendingDeleteEdgeIds.add(id)
      softDeletedEdges.set(
        id,
        cloneEdge(state.baselineEdges.get(id) ?? softDeletedEdges.get(id)!),
      )
    } else {
      softDeletedEdges.delete(id)
    }
  }

  // Drop soft entries for ids no longer pending
  for (const id of [...softDeletedEntities.keys()]) {
    if (!pendingDeleteEntityIds.has(id)) softDeletedEntities.delete(id)
  }
  for (const id of [...softDeletedEdges.keys()]) {
    if (!pendingDeleteEdgeIds.has(id)) softDeletedEdges.delete(id)
  }

  return {
    ...state,
    document: next,
    softDeletedEntities,
    softDeletedEdges,
    pendingDeleteEntityIds,
    pendingDeleteEdgeIds,
  }
}

/** Remove entity: hard-delete if new; soft-delete (+ cascade) if loaded. */
export function deleteEntityFromDraft(state: GraphDraftState, entityId: string): GraphDraftState {
  const entity =
    state.document.entities.find((e) => e.id === entityId) ??
    state.softDeletedEntities.get(entityId)
  if (!entity) return state

  const incident = [
    ...state.document.edges.filter((e) => e.source === entityId || e.target === entityId),
    ...[...state.softDeletedEdges.values()].filter((e) => e.source === entityId || e.target === entityId),
  ]

  if (!state.baselineEntityIds.has(entityId)) {
    const remainingEdges = state.document.edges.filter((e) => e.source !== entityId && e.target !== entityId)
    const remainingEntities = state.document.entities.filter((e) => e.id !== entityId)
    const pendingDeleteEdgeIds = new Set(state.pendingDeleteEdgeIds)
    const softDeletedEdges = new Map(state.softDeletedEdges)
    for (const edge of incident) {
      if (!edge.id) continue
      if (state.baselineEdgeIds.has(edge.id)) {
        pendingDeleteEdgeIds.add(edge.id)
        softDeletedEdges.set(edge.id, cloneEdge(edge))
      }
    }
    return {
      ...state,
      document: { entities: remainingEntities, edges: remainingEdges },
      softDeletedEdges,
      pendingDeleteEdgeIds,
    }
  }

  const softDeletedEntities = new Map(state.softDeletedEntities)
  softDeletedEntities.set(entityId, cloneEntity(entity))
  const pendingDeleteEntityIds = new Set(state.pendingDeleteEntityIds)
  pendingDeleteEntityIds.add(entityId)

  const softDeletedEdges = new Map(state.softDeletedEdges)
  const pendingDeleteEdgeIds = new Set(state.pendingDeleteEdgeIds)
  const remainingEdges: BoMEdge[] = []
  for (const edge of state.document.edges) {
    if (edge.source === entityId || edge.target === entityId) {
      if (edge.id && state.baselineEdgeIds.has(edge.id)) {
        pendingDeleteEdgeIds.add(edge.id)
        softDeletedEdges.set(edge.id, cloneEdge(edge))
      }
      // new incident edges are dropped
    } else {
      remainingEdges.push(edge)
    }
  }

  return {
    ...state,
    document: {
      entities: state.document.entities.filter((e) => e.id !== entityId),
      edges: remainingEdges,
    },
    softDeletedEntities,
    softDeletedEdges,
    pendingDeleteEntityIds,
    pendingDeleteEdgeIds,
  }
}

export function deleteEdgeFromDraft(state: GraphDraftState, edgeId: string): GraphDraftState {
  const edge =
    state.document.edges.find((e) => e.id === edgeId) ?? state.softDeletedEdges.get(edgeId)
  if (!edge) return state

  if (!state.baselineEdgeIds.has(edgeId)) {
    return {
      ...state,
      document: {
        ...state.document,
        edges: state.document.edges.filter((e) => e.id !== edgeId),
      },
    }
  }

  const softDeletedEdges = new Map(state.softDeletedEdges)
  softDeletedEdges.set(edgeId, cloneEdge(edge))
  const pendingDeleteEdgeIds = new Set(state.pendingDeleteEdgeIds)
  pendingDeleteEdgeIds.add(edgeId)

  return {
    ...state,
    document: {
      ...state.document,
      edges: state.document.edges.filter((e) => e.id !== edgeId),
    },
    softDeletedEdges,
    pendingDeleteEdgeIds,
  }
}

export function undoDeleteEntity(state: GraphDraftState, entityId: string): GraphDraftState {
  if (!state.pendingDeleteEntityIds.has(entityId)) return state
  const entity = state.softDeletedEntities.get(entityId) ?? state.baselineEntities.get(entityId)
  if (!entity) return state

  const softDeletedEntities = new Map(state.softDeletedEntities)
  softDeletedEntities.delete(entityId)
  const pendingDeleteEntityIds = new Set(state.pendingDeleteEntityIds)
  pendingDeleteEntityIds.delete(entityId)

  const softDeletedEdges = new Map(state.softDeletedEdges)
  const pendingDeleteEdgeIds = new Set(state.pendingDeleteEdgeIds)
  const restoredEdges = [...state.document.edges]
  for (const [edgeId, edge] of [...softDeletedEdges.entries()]) {
    if (edge.source !== entityId && edge.target !== entityId) continue
    // Only restore the edge when the other endpoint is also live.
    const otherId = edge.source === entityId ? edge.target : edge.source
    if (pendingDeleteEntityIds.has(otherId)) continue
    softDeletedEdges.delete(edgeId)
    pendingDeleteEdgeIds.delete(edgeId)
    if (!restoredEdges.some((e) => e.id === edgeId)) {
      restoredEdges.push(cloneEdge(edge))
    }
  }

  const entities = state.document.entities.some((e) => e.id === entityId)
    ? state.document.entities
    : [...state.document.entities, cloneEntity(entity)]

  return {
    ...state,
    document: { entities, edges: restoredEdges },
    softDeletedEntities,
    softDeletedEdges,
    pendingDeleteEntityIds,
    pendingDeleteEdgeIds,
  }
}

export function undoDeleteEdge(state: GraphDraftState, edgeId: string): GraphDraftState {
  if (!state.pendingDeleteEdgeIds.has(edgeId)) return state
  const edge = state.softDeletedEdges.get(edgeId) ?? state.baselineEdges.get(edgeId)
  if (!edge) return state
  // Endpoints must not be soft-deleted
  if (state.pendingDeleteEntityIds.has(edge.source) || state.pendingDeleteEntityIds.has(edge.target)) {
    return state
  }

  const softDeletedEdges = new Map(state.softDeletedEdges)
  softDeletedEdges.delete(edgeId)
  const pendingDeleteEdgeIds = new Set(state.pendingDeleteEdgeIds)
  pendingDeleteEdgeIds.delete(edgeId)

  const edges = state.document.edges.some((e) => e.id === edgeId)
    ? state.document.edges
    : [...state.document.edges, cloneEdge(edge)]

  return {
    ...state,
    document: { ...state.document, edges },
    softDeletedEdges,
    pendingDeleteEdgeIds,
  }
}

export function undoEntityModifications(state: GraphDraftState, entityId: string): GraphDraftState {
  const baseline = state.baselineEntities.get(entityId)
  if (!baseline || state.pendingDeleteEntityIds.has(entityId)) return state
  const entities = state.document.entities.map((e) => (e.id === entityId ? cloneEntity(baseline) : e))
  if (!entities.some((e) => e.id === entityId)) {
    entities.push(cloneEntity(baseline))
  }
  // Revert payload/annotations only — never touch edges or pending deletes.
  return {
    ...state,
    document: { entities, edges: state.document.edges },
  }
}

export function undoEdgeModifications(state: GraphDraftState, edgeId: string): GraphDraftState {
  const baseline = state.baselineEdges.get(edgeId)
  if (!baseline || state.pendingDeleteEdgeIds.has(edgeId)) return state
  const edges = state.document.edges.map((e) => (e.id === edgeId ? cloneEdge(baseline) : e))
  if (!edges.some((e) => e.id === edgeId)) {
    edges.push(cloneEdge(baseline))
  }
  return { ...state, document: { ...state.document, edges } }
}

export function clearPendingDeletes(state: GraphDraftState): GraphDraftState {
  const snap = snapshotMaps(state.document)
  return {
    ...state,
    ...snap,
    softDeletedEntities: new Map(),
    softDeletedEdges: new Map(),
    pendingDeleteEntityIds: new Set(),
    pendingDeleteEdgeIds: new Set(),
  }
}

/**
 * Append store entities into the draft. Existing ids are kept (not overwritten).
 * New ids are recorded as baseline (store-backed).
 */
export function mergeEntitiesIntoDraft(
  state: GraphDraftState,
  entities: BoMEntity[],
): GraphDraftState {
  const existing = new Set(state.document.entities.map((e) => e.id))
  const added: BoMEntity[] = []
  for (const entity of entities) {
    if (!entity.id || existing.has(entity.id)) continue
    if (state.pendingDeleteEntityIds.has(entity.id)) continue
    added.push(cloneEntity(entity))
    existing.add(entity.id)
  }
  if (added.length === 0) return state

  const baselineEntityIds = new Set(state.baselineEntityIds)
  const baselineEntities = new Map(state.baselineEntities)
  for (const entity of added) {
    baselineEntityIds.add(entity.id)
    baselineEntities.set(entity.id, cloneEntity(entity))
  }
  return {
    ...state,
    document: {
      entities: [...state.document.entities, ...added],
      edges: state.document.edges,
    },
    baselineEntityIds,
    baselineEntities,
  }
}

/**
 * Append store edges into the draft when both endpoints exist. Existing edge ids are kept.
 */
export function mergeEdgesIntoDraft(state: GraphDraftState, edges: BoMEdge[]): GraphDraftState {
  const entityIds = new Set(state.document.entities.map((e) => e.id))
  const existingEdgeIds = new Set(
    state.document.edges.map((e) => e.id).filter((id): id is string => !!id),
  )
  const added: BoMEdge[] = []
  for (const edge of edges) {
    const id = edge.id
    if (!id || existingEdgeIds.has(id)) continue
    if (state.pendingDeleteEdgeIds.has(id)) continue
    if (!entityIds.has(edge.source) || !entityIds.has(edge.target)) continue
    added.push(cloneEdge({ ...edge, id }))
    existingEdgeIds.add(id)
  }
  if (added.length === 0) return state

  const baselineEdgeIds = new Set(state.baselineEdgeIds)
  const baselineEdges = new Map(state.baselineEdges)
  for (const edge of added) {
    if (!edge.id) continue
    baselineEdgeIds.add(edge.id)
    baselineEdges.set(edge.id, cloneEdge(edge))
  }
  return {
    ...state,
    document: {
      entities: state.document.entities,
      edges: [...state.document.edges, ...added],
    },
    baselineEdgeIds,
    baselineEdges,
  }
}

/**
 * Remove an entity from the draft without Apply-delete. Drops baseline membership;
 * cascades exclude on incident edges (not pending-delete).
 */
export function excludeEntityFromDraft(state: GraphDraftState, entityId: string): GraphDraftState {
  const inDoc = state.document.entities.some((e) => e.id === entityId)
  const soft = state.softDeletedEntities.get(entityId)
  if (!inDoc && !soft) return state

  const softDeletedEntities = new Map(state.softDeletedEntities)
  softDeletedEntities.delete(entityId)
  const pendingDeleteEntityIds = new Set(state.pendingDeleteEntityIds)
  pendingDeleteEntityIds.delete(entityId)

  const baselineEntityIds = new Set(state.baselineEntityIds)
  baselineEntityIds.delete(entityId)
  const baselineEntities = new Map(state.baselineEntities)
  baselineEntities.delete(entityId)

  let next: GraphDraftState = {
    ...state,
    document: {
      entities: state.document.entities.filter((e) => e.id !== entityId),
      edges: state.document.edges,
    },
    softDeletedEntities,
    pendingDeleteEntityIds,
    baselineEntityIds,
    baselineEntities,
  }

  const incidentIds = [
    ...next.document.edges.filter((e) => e.source === entityId || e.target === entityId),
    ...[...next.softDeletedEdges.values()].filter((e) => e.source === entityId || e.target === entityId),
  ]
    .map((e) => e.id)
    .filter((id): id is string => !!id)

  for (const edgeId of incidentIds) {
    next = excludeEdgeFromDraft(next, edgeId)
  }
  // Drop any remaining incident edges that had no id
  next = {
    ...next,
    document: {
      entities: next.document.entities,
      edges: next.document.edges.filter((e) => e.source !== entityId && e.target !== entityId),
    },
  }
  return next
}

/** Remove an edge from the draft without Apply-delete. */
export function excludeEdgeFromDraft(state: GraphDraftState, edgeId: string): GraphDraftState {
  const inDoc = state.document.edges.some((e) => e.id === edgeId)
  const soft = state.softDeletedEdges.get(edgeId)
  if (!inDoc && !soft) return state

  const softDeletedEdges = new Map(state.softDeletedEdges)
  softDeletedEdges.delete(edgeId)
  const pendingDeleteEdgeIds = new Set(state.pendingDeleteEdgeIds)
  pendingDeleteEdgeIds.delete(edgeId)
  const baselineEdgeIds = new Set(state.baselineEdgeIds)
  baselineEdgeIds.delete(edgeId)
  const baselineEdges = new Map(state.baselineEdges)
  baselineEdges.delete(edgeId)

  return {
    ...state,
    document: {
      entities: state.document.entities,
      edges: state.document.edges.filter((e) => e.id !== edgeId),
    },
    softDeletedEdges,
    pendingDeleteEdgeIds,
    baselineEdgeIds,
    baselineEdges,
  }
}

export function newEntityId(): string {
  return crypto.randomUUID()
}
