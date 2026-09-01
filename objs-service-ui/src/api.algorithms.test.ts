import { afterEach, describe, expect, it, vi } from 'vitest'
import { analyzeCycles, fetchGraphAlgorithmCapabilities } from './api'

describe('fetchGraphAlgorithmCapabilities', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('shouldReturnNullOn404', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
        json: async () => ({ error: 'not found' }),
      }),
    )

    await expect(fetchGraphAlgorithmCapabilities()).resolves.toBeNull()
  })

  it('shouldReturnCapabilitiesOn200', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          algorithms: [{ id: 'directed-cycle-regions', materializationModes: ['GENERIC'] }],
        }),
      }),
    )

    const caps = await fetchGraphAlgorithmCapabilities()
    expect(caps?.algorithms[0]?.id).toBe('directed-cycle-regions')
  })

  it('shouldReturnNullOnNetworkFailure', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))

    await expect(fetchGraphAlgorithmCapabilities()).resolves.toBeNull()
  })
})

describe('analyzeCycles', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('shouldPostMatcherWithGenericMaterialization', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        algorithm: 'directed-cycle-regions',
        components: [],
        stats: { entityCount: 0, edgeCount: 0, cyclicComponentCount: 0 },
      }),
    })
    vi.stubGlobal('fetch', fetchMock)

    const result = await analyzeCycles({
      matcher: { 'obj-expr': 'true' },
      graphId: 'g-1',
      materialization: 'GENERIC',
    })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/objs/graph/algorithms/cycles',
      expect.objectContaining({ method: 'POST' }),
    )
    const body = JSON.parse(fetchMock.mock.calls[0][1].body as string)
    expect(body.matcher).toEqual({ 'obj-expr': 'true' })
    expect(body.graphId).toBe('g-1')
    expect(body.materialization).toBe('GENERIC')
    expect(result.stats.cyclicComponentCount).toBe(0)
  })

  it('shouldSurfaceErrorFieldOn400', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        json: async () => ({ error: 'invalid matcher' }),
      }),
    )

    await expect(analyzeCycles({ matcher: {}, materialization: 'GENERIC' })).rejects.toThrow(
      'invalid matcher',
    )
  })
})
