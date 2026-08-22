import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import {
  EMPTY_GRAPH_CONTEXT,
  loadGraphContextSnapshot,
  saveGraphContextSnapshot,
  type GraphContextSnapshot,
} from './graphContext'

export type GraphContextApi = {
  context: GraphContextSnapshot
  /** Open / switch to a graph (clears matcher; resets version pin to Latest). */
  setGraph: (
    graphId: string,
    annotations?: Record<string, string>,
    counts?: { nodeCount: number; edgeCount: number },
  ) => void
  /** Apply a matcher selection as context (clears opened graph id and version pin). */
  setMatcher: (
    body: unknown,
    line: string,
    counts: { nodeCount: number; edgeCount: number },
  ) => void
  /** Pin a deep graph version (null = Latest). Graph mode only. */
  setGraphVersion: (
    version: number | null,
    versionAnnotations?: Record<string, string>,
    createdAt?: string | null,
  ) => void
  /** Update node/edge stats without changing kind. */
  setCounts: (nodeCount: number, edgeCount: number) => void
  /** Update graph annotations while in graph mode. */
  setAnnotations: (annotations: Record<string, string>) => void
  clear: () => void
}

const GraphContextReact = createContext<GraphContextApi | null>(null)

export function GraphContextProvider({ children }: { children: ReactNode }) {
  const [context, setContext] = useState<GraphContextSnapshot>(() => loadGraphContextSnapshot())

  const persist = useCallback((next: GraphContextSnapshot) => {
    setContext(next)
    saveGraphContextSnapshot(next)
  }, [])

  const setGraph = useCallback(
    (
      graphId: string,
      annotations: Record<string, string> = {},
      counts?: { nodeCount: number; edgeCount: number },
    ) => {
      persist({
        kind: 'graph',
        graphId,
        graphVersion: null,
        graphVersionAnnotations: {},
        graphVersionCreatedAt: null,
        annotations,
        matcherBody: null,
        matcherLine: null,
        nodeCount: counts?.nodeCount ?? 0,
        edgeCount: counts?.edgeCount ?? 0,
      })
    },
    [persist],
  )

  const setMatcher = useCallback(
    (body: unknown, line: string, counts: { nodeCount: number; edgeCount: number }) => {
      persist({
        kind: 'matcher',
        graphId: null,
        graphVersion: null,
        graphVersionAnnotations: {},
        graphVersionCreatedAt: null,
        annotations: {},
        matcherBody: body,
        matcherLine: line,
        nodeCount: counts.nodeCount,
        edgeCount: counts.edgeCount,
      })
    },
    [persist],
  )

  const setGraphVersion = useCallback(
    (
      version: number | null,
      versionAnnotations: Record<string, string> = {},
      createdAt: string | null = null,
    ) => {
      setContext((prev) => {
        if (prev.kind !== 'graph' || !prev.graphId) return prev
        const pinned =
          version == null || !Number.isFinite(version) ? null : Math.trunc(version)
        const next = {
          ...prev,
          graphVersion: pinned,
          graphVersionAnnotations:
            pinned == null
              ? {}
              : Object.fromEntries(
                  Object.entries(versionAnnotations).filter(
                    ([k, v]) => k.trim().length > 0 && v.trim().length > 0,
                  ),
                ),
          graphVersionCreatedAt:
            pinned == null
              ? null
              : typeof createdAt === 'string' && createdAt.length > 0
                ? createdAt
                : null,
        }
        saveGraphContextSnapshot(next)
        return next
      })
    },
    [],
  )

  const setCounts = useCallback(
    (nodeCount: number, edgeCount: number) => {
      setContext((prev) => {
        const next = { ...prev, nodeCount, edgeCount }
        saveGraphContextSnapshot(next)
        return next
      })
    },
    [],
  )

  const setAnnotations = useCallback((annotations: Record<string, string>) => {
    setContext((prev) => {
      if (prev.kind !== 'graph') return prev
      const next = { ...prev, annotations }
      saveGraphContextSnapshot(next)
      return next
    })
  }, [])

  const clear = useCallback(() => {
    persist({ ...EMPTY_GRAPH_CONTEXT })
  }, [persist])

  const api = useMemo(
    () => ({
      context,
      setGraph,
      setMatcher,
      setGraphVersion,
      setCounts,
      setAnnotations,
      clear,
    }),
    [context, setGraph, setMatcher, setGraphVersion, setCounts, setAnnotations, clear],
  )

  return <GraphContextReact.Provider value={api}>{children}</GraphContextReact.Provider>
}

export function useGraphContext(): GraphContextApi {
  const api = useContext(GraphContextReact)
  if (api == null) {
    throw new Error('useGraphContext requires GraphContextProvider')
  }
  return api
}
