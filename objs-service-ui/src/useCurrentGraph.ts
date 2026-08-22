import { useCallback, useState } from 'react'

/**
 * Composer current-graph id (unbound from Explorer/Objects/Query graph context).
 * Persisted so navigating away from Composer keeps the draft graph.
 */
export const CURRENT_GRAPH_STORAGE_KEY = 'objs.ui.currentGraphId'

export function loadCurrentGraphId(): string | null {
  try {
    const raw = window.localStorage.getItem(CURRENT_GRAPH_STORAGE_KEY)
    return raw && raw.length > 0 ? raw : null
  } catch {
    return null
  }
}

export function saveCurrentGraphId(id: string | null) {
  try {
    if (id) {
      window.localStorage.setItem(CURRENT_GRAPH_STORAGE_KEY, id)
    } else {
      window.localStorage.removeItem(CURRENT_GRAPH_STORAGE_KEY)
    }
  } catch {
    // ignore quota / private mode
  }
}

export function useCurrentGraphId(): [string | null, (id: string | null) => void] {
  const [graphId, setGraphIdState] = useState<string | null>(() => loadCurrentGraphId())
  const setGraphId = useCallback((next: string | null) => {
    setGraphIdState(next)
    saveCurrentGraphId(next)
  }, [])
  return [graphId, setGraphId]
}

/** Short id for compact chrome (e.g. `a1b2c3d4…`). */
export function shortGraphId(id: string): string {
  return id.length > 12 ? `${id.slice(0, 8)}…` : id
}
