import type {
  AssessmentMatrixRow,
  CategoryAssetRow,
  PortfolioAppRef,
  PortfolioTreeView,
  SubjectAreaView,
} from './api/types'

export type ResultLayout = 'flat' | 'tree'

export type CategorySection<T> = {
  key: string
  title: string
  titleFull: string
  items: T[]
}

type SectionMeta = {
  key: string
  nodeId: string | null
  title: string
  titleFull: string
}

function findArea(areas: SubjectAreaView[], id: string): SubjectAreaView | null {
  for (const a of areas) {
    if (a.id === id) return a
    const nested = findArea(a.children, id)
    if (nested) return nested
  }
  return null
}

function pathNamesToNode(areas: SubjectAreaView[], targetId: string, acc: string[] = []): string[] | null {
  for (const a of areas) {
    const next = [...acc, a.name]
    if (a.id === targetId) return next
    const found = pathNamesToNode(a.children, targetId, next)
    if (found) return found
  }
  return null
}

function relativeTitle(
  areas: SubjectAreaView[],
  currentLevel: string,
  nodeId: string,
): { display: string; full: string } {
  const fullPath = pathNamesToNode(areas, nodeId) ?? [nodeId]
  let relative = fullPath
  if (currentLevel !== 'root') {
    const currentPath = pathNamesToNode(areas, currentLevel)
    if (currentPath && fullPath.length >= currentPath.length) {
      relative = fullPath.slice(currentPath.length)
      if (relative.length === 0) relative = [fullPath[fullPath.length - 1]!]
    }
  }
  const full = relative.join(' / ')
  const display = relative.length <= 2 ? full : `… / ${relative[relative.length - 1]}`
  return { display, full }
}

function pushDfs(areas: SubjectAreaView[], into: SubjectAreaView[]) {
  for (const a of areas) {
    into.push(a)
    pushDfs(a.children, into)
  }
}

/** Ordered category buckets under the selected level (for By category layout). */
export function categorySectionMetas(
  tree: PortfolioTreeView,
  level: string,
  includeSubcategories: boolean,
): SectionMeta[] {
  const areas = tree.subjectAreas
  const out: SectionMeta[] = []

  if (level === 'root') {
    out.push({
      key: 'root',
      nodeId: null,
      title: 'Portfolio root',
      titleFull: 'Portfolio root',
    })
    const ordered: SubjectAreaView[] = []
    if (includeSubcategories) {
      pushDfs(areas, ordered)
    } else {
      ordered.push(...areas)
    }
    for (const a of ordered) {
      const titles = relativeTitle(areas, 'root', a.id)
      out.push({ key: a.id, nodeId: a.id, title: titles.display, titleFull: titles.full })
    }
    return out
  }

  const selected = findArea(areas, level)
  if (!selected) return out

  if (!includeSubcategories) {
    const titles = relativeTitle(areas, level, selected.id)
    out.push({
      key: selected.id,
      nodeId: selected.id,
      title: selected.name,
      titleFull: titles.full || selected.name,
    })
    return out
  }

  const ordered: SubjectAreaView[] = []
  ordered.push(selected)
  pushDfs(selected.children, ordered)
  for (const a of ordered) {
    const titles = relativeTitle(areas, level, a.id)
    out.push({
      key: a.id,
      nodeId: a.id,
      title: a.id === selected.id ? selected.name : titles.display,
      titleFull: a.id === selected.id ? selected.name : titles.full,
    })
  }
  return out
}

/** applicationId → placement nodeId from the full tree (not just the current page). */
export function appPlacementNodeIds(tree: PortfolioTreeView): Map<string, string | null> {
  const map = new Map<string, string | null>()
  for (const app of tree.rootApplications) {
    map.set(app.applicationId, app.nodeId ?? null)
  }
  const walk = (areas: SubjectAreaView[]) => {
    for (const a of areas) {
      for (const app of a.applications) {
        map.set(app.applicationId, app.nodeId ?? a.id)
      }
      walk(a.children)
    }
  }
  walk(tree.subjectAreas)
  return map
}

function bucketByNodeId<T>(
  metas: SectionMeta[],
  items: T[],
  nodeIdOf: (item: T) => string | null | undefined,
): CategorySection<T>[] {
  const buckets = new Map<string, T[]>()
  for (const m of metas) buckets.set(m.key, [])
  const uncategorized: T[] = []

  for (const item of items) {
    const nodeId = nodeIdOf(item)
    const key = nodeId == null ? 'root' : nodeId
    const bucket = buckets.get(key)
    if (bucket) bucket.push(item)
    else uncategorized.push(item)
  }

  const sections: CategorySection<T>[] = []
  for (const m of metas) {
    const itemsIn = buckets.get(m.key) ?? []
    if (itemsIn.length === 0) continue
    sections.push({
      key: m.key,
      title: m.title,
      titleFull: m.titleFull,
      items: itemsIn,
    })
  }
  if (uncategorized.length > 0) {
    sections.push({
      key: 'uncategorized',
      title: 'Uncategorized',
      titleFull: 'Uncategorized',
      items: uncategorized,
    })
  }
  return sections
}

export function groupAppsByCategory(
  tree: PortfolioTreeView,
  level: string,
  includeSubcategories: boolean,
  apps: PortfolioAppRef[],
): CategorySection<PortfolioAppRef>[] {
  const metas = categorySectionMetas(tree, level, includeSubcategories)
  return bucketByNodeId(metas, apps, (app) => app.nodeId ?? null)
}

export function groupAssessmentRowsByCategory(
  tree: PortfolioTreeView,
  level: string,
  includeSubcategories: boolean,
  rows: AssessmentMatrixRow[],
): CategorySection<AssessmentMatrixRow>[] {
  const placements = appPlacementNodeIds(tree)
  const metas = categorySectionMetas(tree, level, includeSubcategories)
  return bucketByNodeId(metas, rows, (row) => placements.get(row.applicationId) ?? null)
}

/**
 * Assets may appear under multiple category sections when used by apps in different areas.
 */
export function groupAssetsByCategory(
  tree: PortfolioTreeView,
  level: string,
  includeSubcategories: boolean,
  assets: CategoryAssetRow[],
): CategorySection<CategoryAssetRow>[] {
  const placements = appPlacementNodeIds(tree)
  const metas = categorySectionMetas(tree, level, includeSubcategories)
  const metaKeys = new Set(metas.map((m) => m.key))
  const buckets = new Map<string, CategoryAssetRow[]>()
  for (const m of metas) buckets.set(m.key, [])
  const uncategorized: CategoryAssetRow[] = []

  for (const asset of assets) {
    const keys = new Set<string>()
    for (const appId of asset.usedInApplicationIds) {
      const nodeId = placements.get(appId)
      const key = nodeId == null ? 'root' : nodeId
      if (metaKeys.has(key)) keys.add(key)
    }
    if (keys.size === 0) {
      uncategorized.push(asset)
      continue
    }
    for (const key of keys) {
      buckets.get(key)!.push(asset)
    }
  }

  const sections: CategorySection<CategoryAssetRow>[] = []
  for (const m of metas) {
    const items = buckets.get(m.key) ?? []
    if (items.length === 0) continue
    sections.push({
      key: m.key,
      title: m.title,
      titleFull: m.titleFull,
      items,
    })
  }
  if (uncategorized.length > 0) {
    sections.push({
      key: 'uncategorized',
      title: 'Uncategorized',
      titleFull: 'Uncategorized',
      items: uncategorized,
    })
  }
  return sections
}

export function parseResultLayout(raw: string | null): ResultLayout {
  return raw === 'tree' ? 'tree' : 'flat'
}
