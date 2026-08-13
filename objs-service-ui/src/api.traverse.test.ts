import { afterEach, describe, expect, it, vi } from 'vitest'
import { traverseGremlin } from './api'

describe('traverseGremlin', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('shouldPostMatcherAndScript', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        primary: 'table',
        items: [],
        contents: null,
        views: { graph: null, table: { columns: ['name'], rows: [['a']] }, scalar: null },
        meta: {
          strategy: 'envelope',
          language: 'gremlin-lang',
          subgraph1Stats: { entities: 1, edges: 0 },
          subgraph2Stats: null,
          resultCount: 1,
          durationMs: 5,
        },
      }),
    })
    vi.stubGlobal('fetch', fetchMock)

    const result = await traverseGremlin({
      matcher: { anno: { app: 'app-00001' } },
      script: "g.V().hasLabel('Service')",
    })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/objs/graph/traverse/gremlin',
      expect.objectContaining({
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    const body = JSON.parse(fetchMock.mock.calls[0][1].body as string)
    expect(body.matcher).toEqual({ anno: { app: 'app-00001' } })
    expect(body.script).toBe("g.V().hasLabel('Service')")
    expect(result.primary).toBe('table')
    expect(result.views.table?.columns).toEqual(['name'])
  })

  it('shouldSurfaceErrorFieldOn400', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        json: async () => ({ error: 'script must not be blank' }),
      }),
    )

    await expect(
      traverseGremlin({ matcher: { anno: { a: 'b' } }, script: ' ' }),
    ).rejects.toThrow('script must not be blank')
  })
})
