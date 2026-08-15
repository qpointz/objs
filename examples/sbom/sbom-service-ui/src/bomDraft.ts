import type { AssetView, RelationView } from './api/types'

export type DraftKind = 'new' | 'modified' | 'deleted' | 'unchanged'

export type BomDraft = {
  assetStatus: Map<string, DraftKind>
  relationStatus: Map<string, DraftKind>
  graphAssets: AssetView[]
  graphRelations: RelationView[]
}

function payloadEqual(a: Record<string, unknown>, b: Record<string, unknown>): boolean {
  return JSON.stringify(a) === JSON.stringify(b)
}

export function computeBomDraft(
  baselineAssets: AssetView[],
  baselineRels: RelationView[],
  workingAssets: AssetView[],
  workingRels: RelationView[],
): BomDraft {
  const baseAssets = new Map(baselineAssets.map((a) => [a.id, a]))
  const workAssets = new Map(workingAssets.map((a) => [a.id, a]))
  const baseRels = new Map(baselineRels.map((r) => [r.id, r]))
  const workRels = new Map(workingRels.map((r) => [r.id, r]))

  const assetStatus = new Map<string, DraftKind>()
  const relationStatus = new Map<string, DraftKind>()

  for (const a of workingAssets) {
    const prev = baseAssets.get(a.id)
    if (!prev) assetStatus.set(a.id, 'new')
    else if (!payloadEqual(prev.payload, a.payload)) assetStatus.set(a.id, 'modified')
    else assetStatus.set(a.id, 'unchanged')
  }
  for (const a of baselineAssets) {
    if (!workAssets.has(a.id)) assetStatus.set(a.id, 'deleted')
  }

  for (const r of workingRels) {
    relationStatus.set(r.id, baseRels.has(r.id) ? 'unchanged' : 'new')
  }
  for (const r of baselineRels) {
    if (!workRels.has(r.id)) relationStatus.set(r.id, 'deleted')
  }

  const bump = (id: string) => {
    const current = assetStatus.get(id)
    if (current === 'unchanged') assetStatus.set(id, 'modified')
  }
  for (const r of [...workingRels, ...baselineRels]) {
    const status = relationStatus.get(r.id)
    if (status === 'new' || status === 'deleted') {
      bump(r.fromAssetId)
      bump(r.toAssetId)
    }
  }

  const graphAssets = [
    ...workingAssets,
    ...baselineAssets.filter((a) => !workAssets.has(a.id)),
  ]
  const graphRelations = [
    ...workingRels,
    ...baselineRels.filter((r) => !workRels.has(r.id)),
  ]
  return { assetStatus, relationStatus, graphAssets, graphRelations }
}

export function isChanged(kind: DraftKind | undefined): boolean {
  return kind === 'new' || kind === 'modified' || kind === 'deleted'
}
