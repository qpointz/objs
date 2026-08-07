import { describe, expect, it } from 'vitest'
import {
  applyMutationDocument,
  buildMutationDocument,
  deleteEntityFromDraft,
  draftFromSubgraph,
  edgeStatus,
  emptyDraftState,
  entityStatus,
  excludeEntityFromDraft,
  graphShapeError,
  mergeEdgesIntoDraft,
  mergeEntitiesIntoDraft,
  mutationShapeError,
  normalizeGraphDocument,
  normalizeGraphMutation,
  replaceDocument,
  undoDeleteEntity,
  undoEntityModifications,
  visualDocument,
} from './graphDraft'

describe('Object linter graph shape', () => {
  it('accepts a graph batch envelope', () => {
    expect(graphShapeError({ entities: [], edges: [] })).toBeNull()
  })

  it('requires entity and edge arrays', () => {
    expect(graphShapeError({ edges: [] })).toBe('Graph document must contain an entities array')
    expect(graphShapeError({ entities: [] })).toBe('Graph document must contain an edges array')
  })

  it('rejects non-object documents', () => {
    expect(graphShapeError([])).toBe('Graph document must be an object')
  })

  it('accepts optional delete id arrays on mutations', () => {
    expect(
      mutationShapeError({
        upsert: { entities: [], edges: [] },
        delete: { entities: ['a'], edges: [] },
      }),
    ).toBeNull()
  })
})

describe('graphDraft helpers', () => {
  it('normalizes a parsed document', () => {
    const doc = normalizeGraphDocument({
      entities: [{ id: 'a', type: 'Product', payload: { name: 'x' } }],
      edges: [{ id: 'e1', source: 'a', target: 'a', role: 'SELF' }],
    })
    expect(doc?.entities[0].type).toBe('Product')
    expect(doc?.edges[0].role).toBe('SELF')
  })

  it('cascades incident edges when deleting an entity from a loaded draft', () => {
    const loaded = draftFromSubgraph({
      entities: [
        { id: 'a', type: 'A', annotations: {} },
        { id: 'b', type: 'B', annotations: {} },
      ],
      edges: [{ id: 'e1', source: 'a', target: 'b', role: 'R' }],
    })
    const next = deleteEntityFromDraft(loaded, 'a')
    expect(next.document.entities.map((e) => e.id)).toEqual(['b'])
    expect(next.document.edges).toEqual([])
    expect([...next.pendingDeleteEntityIds]).toEqual(['a'])
    expect([...next.pendingDeleteEdgeIds]).toEqual(['e1'])
    expect(next.softDeletedEntities.has('a')).toBe(true)
    expect(next.softDeletedEdges.has('e1')).toBe(true)
    expect(visualDocument(next).entities.map((e) => e.id).sort()).toEqual(['a', 'b'])
  })

  it('hard-removes entities that were not loaded', () => {
    const state = emptyDraftState({
      entities: [{ id: 'n1', type: 'A', annotations: {} }],
      edges: [],
    })
    const next = deleteEntityFromDraft(state, 'n1')
    expect(next.document.entities).toEqual([])
    expect(next.pendingDeleteEntityIds.size).toBe(0)
    expect(visualDocument(next).entities).toEqual([])
  })

  it('undoes soft-delete and modifications for loaded entities', () => {
    const loaded = draftFromSubgraph({
      entities: [{ id: 'a', type: 'A', annotations: {}, payload: { name: 'orig' } }],
      edges: [],
    })
    const modified = replaceDocument(loaded, {
      entities: [{ id: 'a', type: 'A', annotations: {}, payload: { name: 'changed' } }],
      edges: [],
    })
    expect(entityStatus(modified, 'a')).toBe('modified')
    const reverted = undoEntityModifications(modified, 'a')
    expect(entityStatus(reverted, 'a')).toBe('unchanged')
    expect(reverted.document.entities[0].payload).toEqual({ name: 'orig' })

    const deleted = deleteEntityFromDraft(reverted, 'a')
    expect(entityStatus(deleted, 'a')).toBe('deleted')
    const restored = undoDeleteEntity(deleted, 'a')
    expect(entityStatus(restored, 'a')).toBe('unchanged')
    expect(restored.pendingDeleteEntityIds.size).toBe(0)
    expect(restored.document.entities.map((e) => e.id)).toEqual(['a'])
  })

  it('does not resurrect a cascaded soft-deleted edge when reverting the other endpoint', () => {
    const loaded = draftFromSubgraph({
      entities: [
        { id: 'a', type: 'A', annotations: {}, payload: { name: 'orig' } },
        { id: 'b', type: 'B', annotations: {}, payload: {} },
      ],
      edges: [{ id: 'e1', source: 'a', target: 'b', role: 'R' }],
    })
    let s = deleteEntityFromDraft(loaded, 'b')
    expect(edgeStatus(s, 'e1')).toBe('deleted')

    s = replaceDocument(s, {
      entities: [{ id: 'a', type: 'A', annotations: {}, payload: { name: 'changed' } }],
      edges: s.document.edges,
    })
    expect(entityStatus(s, 'a')).toBe('modified')

    s = replaceDocument(s, {
      entities: [{ id: 'a', type: 'A', annotations: {}, payload: { name: 'changed' } }],
      edges: [{ id: 'e1', source: 'a', target: 'b', role: 'R' }],
    })
    expect(entityStatus(s, 'b')).toBe('deleted')
    expect(edgeStatus(s, 'e1')).toBe('deleted')
    expect(s.document.edges.map((e) => e.id)).toEqual([])

    s = undoEntityModifications(s, 'a')
    expect(entityStatus(s, 'a')).toBe('unchanged')
    expect(entityStatus(s, 'b')).toBe('deleted')
    expect(edgeStatus(s, 'e1')).toBe('deleted')
    expect(s.document.edges).toEqual([])
  })

  it('tracks pending deletes when text replaces a loaded document', () => {
    const loaded = draftFromSubgraph({
      entities: [{ id: 'a', type: 'A', annotations: {} }],
      edges: [],
    })
    const next = replaceDocument(loaded, { entities: [], edges: [] })
    expect([...next.pendingDeleteEntityIds]).toEqual(['a'])
  })

  it('starts with an empty draft by default', () => {
    const state = emptyDraftState()
    expect(state.pendingDeleteEntityIds.size).toBe(0)
    expect(state.document.entities).toEqual([])
    expect(state.document.edges).toEqual([])
  })

  it('builds an empty mutation right after load', () => {
    const loaded = draftFromSubgraph({
      entities: [
        { id: 'a', type: 'A', annotations: {}, payload: { name: 'x' } },
        { id: 'b', type: 'B', annotations: {}, payload: {} },
      ],
      edges: [{ id: 'e1', source: 'a', target: 'b', role: 'R' }],
    })
    expect(buildMutationDocument(loaded)).toEqual({
      upsert: { entities: [], edges: [] },
      delete: { entities: [], edges: [] },
    })
  })

  it('includes only created, modified, and deleted items in the mutation', () => {
    const loaded = draftFromSubgraph({
      entities: [
        { id: 'a', type: 'A', annotations: {}, payload: { name: 'orig' } },
        { id: 'b', type: 'B', annotations: {}, payload: {} },
      ],
      edges: [{ id: 'e1', source: 'a', target: 'b', role: 'R' }],
    })
    let s = replaceDocument(loaded, {
      entities: [
        { id: 'a', type: 'A', annotations: {}, payload: { name: 'changed' } },
        { id: 'b', type: 'B', annotations: {}, payload: {} },
        { id: 'c', type: 'C', annotations: {}, payload: {} },
      ],
      edges: [{ id: 'e1', source: 'a', target: 'b', role: 'R' }],
    })
    s = deleteEntityFromDraft(s, 'b')
    const mutation = buildMutationDocument(s)
    expect(mutation.upsert.entities.map((e) => e.id).sort()).toEqual(['a', 'c'])
    expect(mutation.upsert.edges).toEqual([])
    expect(mutation.delete.entities).toEqual(['b'])
    expect(mutation.delete.edges).toEqual(['e1'])
  })

  it('applies a mutation onto baseline without wiping unchanged loaded items', () => {
    const loaded = draftFromSubgraph({
      entities: [
        { id: 'a', type: 'A', annotations: {}, payload: { name: 'orig' } },
        { id: 'b', type: 'B', annotations: {}, payload: {} },
      ],
      edges: [{ id: 'e1', source: 'a', target: 'b', role: 'R' }],
    })
    const mutation = normalizeGraphMutation({
      upsert: {
        entities: [{ id: 'a', type: 'A', annotations: {}, payload: { name: 'changed' } }],
        edges: [],
      },
      delete: { entities: [], edges: [] },
    })!
    const next = applyMutationDocument(loaded, mutation)
    expect(next.document.entities.map((e) => e.id).sort()).toEqual(['a', 'b'])
    expect(next.document.entities.find((e) => e.id === 'a')?.payload).toEqual({ name: 'changed' })
    expect(entityStatus(next, 'a')).toBe('modified')
    expect(entityStatus(next, 'b')).toBe('unchanged')
    expect(buildMutationDocument(next).upsert.entities.map((e) => e.id)).toEqual(['a'])
  })

  it('merges entities without overwriting and excludes without pending deletes', () => {
    const loaded = draftFromSubgraph({
      entities: [{ id: 'a', type: 'A', annotations: {}, payload: { name: 'keep' } }],
      edges: [],
    })
    const merged = mergeEntitiesIntoDraft(loaded, [
      { id: 'a', type: 'A', annotations: {}, payload: { name: 'store' } },
      { id: 'b', type: 'B', annotations: {}, payload: {} },
    ])
    expect(merged.document.entities.find((e) => e.id === 'a')?.payload).toEqual({ name: 'keep' })
    expect(merged.document.entities.map((e) => e.id).sort()).toEqual(['a', 'b'])
    expect(merged.baselineEntityIds.has('b')).toBe(true)

    const withEdge = mergeEdgesIntoDraft(merged, [
      { id: 'e1', source: 'a', target: 'b', role: 'R' },
    ])
    expect(withEdge.document.edges.map((e) => e.id)).toEqual(['e1'])

    const excluded = excludeEntityFromDraft(withEdge, 'b')
    expect(excluded.document.entities.map((e) => e.id)).toEqual(['a'])
    expect(excluded.document.edges).toEqual([])
    expect(excluded.pendingDeleteEntityIds.size).toBe(0)
    expect(excluded.pendingDeleteEdgeIds.size).toBe(0)
    expect(excluded.baselineEntityIds.has('b')).toBe(false)
  })
})
