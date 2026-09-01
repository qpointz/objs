import type { GraphAlgorithmCapabilities, GraphCycleAnalysis } from './types'

export const DIRECTED_CYCLE_REGIONS_ALGORITHM_ID = 'directed-cycle-regions'
export const GENERIC_MATERIALIZATION = 'GENERIC'

/** True when the optional algorithm service advertises generic directed cycle analysis. */
export function supportsGenericCycleAnalysis(
  capabilities: GraphAlgorithmCapabilities | null | undefined,
): boolean {
  if (!capabilities?.algorithms?.length) return false
  return capabilities.algorithms.some(
    (algorithm) =>
      algorithm.id === DIRECTED_CYCLE_REGIONS_ALGORITHM_ID &&
      algorithm.materializationModes.includes(GENERIC_MATERIALIZATION),
  )
}

/** Map cycle analysis components to canvas node and edge ids (string UUIDs from JSON). */
export function cycleAnalysisHighlights(analysis: GraphCycleAnalysis): {
  nodeIds: string[]
  edgeIds: string[]
} {
  const nodeIds = new Set<string>()
  const edgeIds = new Set<string>()
  for (const component of analysis.components) {
    for (const id of component.entityIds) nodeIds.add(String(id))
    for (const id of component.edgeIds) edgeIds.add(String(id))
  }
  return { nodeIds: [...nodeIds], edgeIds: [...edgeIds] }
}
