import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  execMatcher,
  graphSearchQuery,
  scopeAddObjectsMatcher,
  scopeMatcherToGraph,
  searchGraphs,
} from './api'

describe('execMatcher (WI-005 graph-scoped routing)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('routes obj-expr to the graph-scoped query endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ entities: [], edges: [] }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await execMatcher('obj-expr', { 'obj-expr': "type == 'X'" }, 'graph-1')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/objs/graphs/graph-1/query',
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('wraps bare obj-expr with all when no current graph', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ entities: [], edges: [] }),
    })
    vi.stubGlobal('fetch', fetchMock)

    const body = { 'obj-expr': "type == 'X'" }
    await execMatcher('obj-expr', body, null)

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/objs/graphs/query',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify([{ all: true }, body]),
      }),
    )
  })

  it('routes graph-expr / all / chained to the header query endpoint regardless of current graph', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ entities: [], edges: [] }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await execMatcher('graph-expr', { 'graph-expr': "a.env == 'prod'" }, null)
    await execMatcher('all', { all: true }, null)

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/objs/graphs/query',
      expect.objectContaining({ method: 'POST' }),
    )
  })
})

describe('scopeAddObjectsMatcher (Composer Add objects)', () => {
  it('scopes to the current graph when one is selected', () => {
    const body = { 'obj-expr': "type == 'X'" }
    expect(scopeAddObjectsMatcher(body, 'graph-1')).toEqual({
      kind: 'in-graph',
      graphId: 'graph-1',
      body,
    })
  })

  it('wraps bare obj-expr with all when no current graph', () => {
    const body = { 'obj-expr': "type == 'Component'" }
    expect(scopeAddObjectsMatcher(body, null)).toEqual({
      kind: 'graphs',
      body: [{ all: true }, body],
    })
  })

  it('passes graph-scoped matchers through to graphs/query when no current graph', () => {
    const body = { all: true }
    expect(scopeAddObjectsMatcher(body, null)).toEqual({ kind: 'graphs', body })
  })
})

describe('scopeMatcherToGraph (Query page obj-expr scoping)', () => {
  it('leaves graph-expr / all / chained matchers untouched', () => {
    const body = { 'graph-expr': "a.env == 'prod'" }
    expect(scopeMatcherToGraph('graph-expr', body, null)).toBe(body)
    expect(scopeMatcherToGraph('all', { all: true }, null)).toEqual({ all: true })
  })

  it('wraps a bare obj-expr with a graph-expr stage for the current graph', () => {
    const body = { 'obj-expr': "type == 'X'" }
    expect(scopeMatcherToGraph('obj-expr', body, 'graph-1')).toEqual([
      { 'graph-expr': "id == 'graph-1'" },
      body,
    ])
  })

  it('rejects a bare obj-expr without a current graph', () => {
    expect(() => scopeMatcherToGraph('obj-expr', { 'obj-expr': "type == 'X'" }, null)).toThrow(
      /current graph/,
    )
  })
})

describe('searchGraphs (WI-007 / G-U10)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('builds query with q, limit, and optional expr', () => {
    expect(graphSearchQuery({ q: 'prod', limit: 15 })).toBe('q=prod&limit=15')
    expect(graphSearchQuery({ q: 'acme', expr: "a.env == 'prod'", limit: 10 })).toBe(
      'q=acme&expr=a.env+%3D%3D+%27prod%27&limit=10',
    )
    expect(graphSearchQuery({})).toBe('limit=15')
  })

  it('GETs /graphs/search and returns items envelope', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        items: [{ id: 'g-1', annotations: { env: 'prod' }, score: 0.9 }],
      }),
    })
    vi.stubGlobal('fetch', fetchMock)

    const res = await searchGraphs({ q: 'prod', limit: 15 })
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/objs/graphs/search?q=prod&limit=15')
    expect(res.items).toEqual([{ id: 'g-1', annotations: { env: 'prod' }, score: 0.9 }])
  })
})
