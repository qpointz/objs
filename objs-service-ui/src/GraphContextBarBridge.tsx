import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  type ReactNode,
} from 'react'
import type { BoMGraphResponse } from './types'

export type GraphContextBarExplorerHandlers = {
  onMatcherApplied?: (
    contents: { entities: unknown[]; edges: unknown[] },
    body: unknown,
  ) => void
  onGraphOpened?: (graphId: string, resolved: BoMGraphResponse) => void
}

type BridgeApi = {
  getHandlers: () => GraphContextBarExplorerHandlers | null
  setHandlers: (next: GraphContextBarExplorerHandlers | null) => void
}

const BridgeContext = createContext<BridgeApi | null>(null)

/** Lets Explorer register canvas apply hooks for the header GraphContextBar. */
export function GraphContextBarBridgeProvider({ children }: { children: ReactNode }) {
  const handlersRef = useRef<GraphContextBarExplorerHandlers | null>(null)
  const getHandlers = useCallback(() => handlersRef.current, [])
  const setHandlers = useCallback((next: GraphContextBarExplorerHandlers | null) => {
    handlersRef.current = next
  }, [])
  return (
    <BridgeContext.Provider value={{ getHandlers, setHandlers }}>{children}</BridgeContext.Provider>
  )
}

export function useGraphContextBarBridge(): BridgeApi {
  const api = useContext(BridgeContext)
  if (api == null) {
    throw new Error('useGraphContextBarBridge requires GraphContextBarBridgeProvider')
  }
  return api
}

/** Register Explorer canvas handlers while this page is mounted. */
export function useRegisterGraphContextBarExplorerHandlers(
  handlers: GraphContextBarExplorerHandlers,
) {
  const { setHandlers } = useGraphContextBarBridge()
  const onMatcherApplied = handlers.onMatcherApplied
  const onGraphOpened = handlers.onGraphOpened
  useEffect(() => {
    setHandlers({ onMatcherApplied, onGraphOpened })
    return () => setHandlers(null)
  }, [setHandlers, onMatcherApplied, onGraphOpened])
}
