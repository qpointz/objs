import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'
import { Box } from '@mantine/core'
import { useLocation } from 'react-router-dom'
import { ComposerGraphBar, type ComposerGraphBarProps } from './ComposerGraphBar'

type SlotApi = {
  setProps: (next: ComposerGraphBarProps | null) => void
}

const SlotContext = createContext<SlotApi | null>(null)
const PropsContext = createContext<ComposerGraphBarProps | null>(null)

/** Holds Composer draft bar props so AppLayout can render it in the header. */
export function ComposerGraphBarHeaderProvider({ children }: { children: ReactNode }) {
  const [props, setPropsState] = useState<ComposerGraphBarProps | null>(null)
  const setProps = useCallback((next: ComposerGraphBarProps | null) => {
    setPropsState(next)
  }, [])
  return (
    <SlotContext.Provider value={{ setProps }}>
      <PropsContext.Provider value={props}>{children}</PropsContext.Provider>
    </SlotContext.Provider>
  )
}

/** Publish ComposerGraphBar props while Composer is mounted. */
export function useRegisterComposerGraphBar(props: ComposerGraphBarProps) {
  const slot = useContext(SlotContext)
  if (slot == null) {
    throw new Error('useRegisterComposerGraphBar requires ComposerGraphBarHeaderProvider')
  }
  const { setProps } = slot
  const { graphId, annotations, versionLabel, nodeCount, edgeCount, onOpenGraph } = props
  useEffect(() => {
    setProps({
      graphId,
      annotations,
      versionLabel,
      nodeCount,
      edgeCount,
      onOpenGraph,
    })
    return () => setProps(null)
  }, [setProps, graphId, annotations, versionLabel, nodeCount, edgeCount, onOpenGraph])
}

/** Header slot: show ComposerGraphBar on /composer when props are registered. */
export function HeaderComposerGraphBar() {
  const location = useLocation()
  const onComposer =
    location.pathname === '/composer' || location.pathname.startsWith('/composer/')
  const props = useContext(PropsContext)
  if (!onComposer || props == null) return null
  return (
    <Box style={{ flexShrink: 0, maxWidth: 'min(720px, 48vw)' }}>
      <ComposerGraphBar {...props} />
    </Box>
  )
}
