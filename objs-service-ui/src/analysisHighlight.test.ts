import { describe, expect, it } from 'vitest'
import {
  cycleAnalysisHighlights,
  DIRECTED_CYCLE_REGIONS_ALGORITHM_ID,
  GENERIC_MATERIALIZATION,
  supportsGenericCycleAnalysis,
} from './analysisHighlight'
import type { GraphAlgorithmCapabilities, GraphCycleAnalysis } from './types'

describe('supportsGenericCycleAnalysis', () => {
  it('shouldReturnTrueWhenDirectedCycleRegionsAdvertisesGeneric', () => {
    const caps: GraphAlgorithmCapabilities = {
      algorithms: [
        {
          id: DIRECTED_CYCLE_REGIONS_ALGORITHM_ID,
          materializationModes: [GENERIC_MATERIALIZATION],
        },
      ],
    }
    expect(supportsGenericCycleAnalysis(caps)).toBe(true)
  })

  it('shouldReturnFalseWhenCapabilitiesMissing', () => {
    expect(supportsGenericCycleAnalysis(null)).toBe(false)
    expect(supportsGenericCycleAnalysis({ algorithms: [] })).toBe(false)
  })

  it('shouldReturnFalseWhenOnlyTypedMaterialization', () => {
    const caps: GraphAlgorithmCapabilities = {
      algorithms: [
        {
          id: DIRECTED_CYCLE_REGIONS_ALGORITHM_ID,
          materializationModes: ['TYPED'],
        },
      ],
    }
    expect(supportsGenericCycleAnalysis(caps)).toBe(false)
  })
})

describe('cycleAnalysisHighlights', () => {
  it('shouldUnionEntityAndEdgeIdsAcrossComponents', () => {
    const analysis: GraphCycleAnalysis = {
      algorithm: DIRECTED_CYCLE_REGIONS_ALGORITHM_ID,
      components: [
        {
          id: '00000000-0000-0000-0000-000000000001',
          entityIds: ['a', 'b'],
          edgeIds: ['e1'],
        },
        {
          id: '00000000-0000-0000-0000-000000000002',
          entityIds: ['c'],
          edgeIds: ['e2', 'e3'],
        },
      ],
      stats: { entityCount: 3, edgeCount: 3, cyclicComponentCount: 2 },
    }
    const { nodeIds, edgeIds } = cycleAnalysisHighlights(analysis)
    expect(nodeIds.sort()).toEqual(['a', 'b', 'c'])
    expect(edgeIds.sort()).toEqual(['e1', 'e2', 'e3'])
  })

  it('shouldReturnEmptyWhenNoComponents', () => {
    const analysis: GraphCycleAnalysis = {
      algorithm: DIRECTED_CYCLE_REGIONS_ALGORITHM_ID,
      components: [],
      stats: { entityCount: 0, edgeCount: 0, cyclicComponentCount: 0 },
    }
    expect(cycleAnalysisHighlights(analysis)).toEqual({ nodeIds: [], edgeIds: [] })
  })
})
