import { useCallback, useMemo, useState } from 'react'
import type { BoMEntity } from './types'

export const OBJECT_SHELF_STORAGE_KEY = 'objs.ui.objects.shelf'

function loadShelf(): BoMEntity[] {
  try {
    const raw = window.localStorage.getItem(OBJECT_SHELF_STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) return []
    return parsed.filter(
      (e): e is BoMEntity =>
        e != null &&
        typeof e === 'object' &&
        typeof (e as BoMEntity).id === 'string' &&
        typeof (e as BoMEntity).type === 'string',
    )
  } catch {
    return []
  }
}

function persistShelf(entities: BoMEntity[]) {
  window.localStorage.setItem(OBJECT_SHELF_STORAGE_KEY, JSON.stringify(entities))
}

export function useObjectShelf() {
  const [entities, setEntities] = useState<BoMEntity[]>(() =>
    typeof window === 'undefined' ? [] : loadShelf(),
  )

  const ids = useMemo(() => new Set(entities.map((e) => e.id)), [entities])

  const replace = useCallback((next: BoMEntity[]) => {
    setEntities(next)
    persistShelf(next)
  }, [])

  const add = useCallback((toAdd: BoMEntity[]) => {
    setEntities((prev) => {
      const byId = new Map(prev.map((e) => [e.id, e]))
      for (const entity of toAdd) {
        if (entity.id) byId.set(entity.id, entity)
      }
      const next = [...byId.values()]
      persistShelf(next)
      return next
    })
  }, [])

  const remove = useCallback((removeIds: string[]) => {
    const drop = new Set(removeIds)
    setEntities((prev) => {
      const next = prev.filter((e) => !drop.has(e.id))
      persistShelf(next)
      return next
    })
  }, [])

  const clear = useCallback(() => {
    setEntities([])
    persistShelf([])
  }, [])

  const toggle = useCallback((entity: BoMEntity) => {
    setEntities((prev) => {
      const exists = prev.some((e) => e.id === entity.id)
      const next = exists ? prev.filter((e) => e.id !== entity.id) : [...prev, entity]
      persistShelf(next)
      return next
    })
  }, [])

  return { entities, ids, add, remove, clear, toggle, replace }
}

/** Navigate state for Composer: replace draft with shelf entities (no edges). */
export function shelfToComposerNavState(entities: BoMEntity[]) {
  return {
    graphId: null as null,
    replaceDraft: true as const,
    graphContents: { entities, edges: [] as const },
  }
}
