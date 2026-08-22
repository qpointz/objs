import { describe, expect, it } from 'vitest'
import { EMPTY_GRAPH_CONTEXT } from './graphContext'
import { MATCH_ALL_OBJ_EXPR } from './queryGraphContext'
import { scopeObjectsSearch } from './objectsSearchScope'

describe('scopeObjectsSearch', () => {
  const obj = { 'obj-expr': "type == 'API'" }

  it('scopes to opened graph', () => {
    const scoped = scopeObjectsSearch(obj, {
      ...EMPTY_GRAPH_CONTEXT,
      kind: 'graph',
      graphId: 'g1',
    })
    expect(scoped).toEqual({ graphId: 'g1', graphVersion: null, body: obj })
  })

  it('passes pinned graph version', () => {
    const scoped = scopeObjectsSearch(obj, {
      ...EMPTY_GRAPH_CONTEXT,
      kind: 'graph',
      graphId: 'g1',
      graphVersion: 42,
    })
    expect(scoped).toEqual({ graphId: 'g1', graphVersion: 42, body: obj })
  })

  it('lists all members when filter is blank', () => {
    const scoped = scopeObjectsSearch({ 'obj-expr': '' }, {
      ...EMPTY_GRAPH_CONTEXT,
      kind: 'graph',
      graphId: 'g1',
    })
    expect(scoped.body).toEqual(MATCH_ALL_OBJ_EXPR)
  })

  it('chains into matcher context', () => {
    const scoped = scopeObjectsSearch(obj, {
      ...EMPTY_GRAPH_CONTEXT,
      kind: 'matcher',
      matcherBody: { 'obj-expr': "type == 'Component'" },
      matcherLine: '…',
    })
    expect(scoped.graphId).toBeNull()
    expect(scoped.graphVersion).toBeNull()
    expect(scoped.body).toEqual([{ 'obj-expr': "type == 'Component'" }, obj])
  })

  it('reuses matcher body when filter is match-all', () => {
    const body = { 'graph-expr': "id == 'g'" }
    const scoped = scopeObjectsSearch(MATCH_ALL_OBJ_EXPR, {
      ...EMPTY_GRAPH_CONTEXT,
      kind: 'matcher',
      matcherBody: body,
      matcherLine: '…',
    })
    expect(scoped.body).toBe(body)
  })

  it('throws when context is empty', () => {
    expect(() => scopeObjectsSearch(obj, EMPTY_GRAPH_CONTEXT)).toThrow(/graph context/i)
  })
})
