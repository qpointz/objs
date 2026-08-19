import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { Button, Group, Paper, Text, Title } from '@mantine/core'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  WORKBENCH_TOUR_STEPS,
  WORKBENCH_TOUR_STORAGE_KEY,
  type WorkbenchTourStep,
} from './workbenchTourSteps'

type TourApi = {
  active: boolean
  start: () => void
  stop: () => void
}

const TourContext = createContext<TourApi>({
  active: false,
  start: () => {},
  stop: () => {},
})

export function useWorkbenchTour(): TourApi {
  return useContext(TourContext)
}

function tourDone(): boolean {
  try {
    return window.localStorage.getItem(WORKBENCH_TOUR_STORAGE_KEY) === 'done'
  } catch {
    return true
  }
}

function markTourDone() {
  try {
    window.localStorage.setItem(WORKBENCH_TOUR_STORAGE_KEY, 'done')
  } catch {
    /* ignore quota / private mode */
  }
}

function waitForEl(selector: string, tries = 40): Promise<HTMLElement | null> {
  return new Promise((resolve) => {
    let n = 0
    const tick = () => {
      const el = document.querySelector(selector)
      if (el instanceof HTMLElement) {
        resolve(el)
        return
      }
      n += 1
      if (n >= tries) {
        resolve(null)
        return
      }
      window.setTimeout(tick, 50)
    }
    tick()
  })
}

export function WorkbenchTourProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const location = useLocation()
  const [index, setIndex] = useState<number | null>(null)
  const [target, setTarget] = useState<DOMRect | null>(null)
  const active = index != null

  const step: WorkbenchTourStep | null = index == null ? null : (WORKBENCH_TOUR_STEPS[index] ?? null)

  const stop = useCallback(() => {
    setIndex(null)
    setTarget(null)
    markTourDone()
  }, [])

  const go = useCallback(
    (nextIndex: number) => {
      const run = async (i: number): Promise<void> => {
        if (i < 0) return
        if (i >= WORKBENCH_TOUR_STEPS.length) {
          stop()
          return
        }
        const next = WORKBENCH_TOUR_STEPS[i]
        if (next.route && location.pathname !== next.route && !location.pathname.startsWith(`${next.route}/`)) {
          navigate(next.route)
        }
        if (!next.selector) {
          setIndex(i)
          setTarget(null)
          return
        }
        const el = await waitForEl(next.selector)
        if (!el) {
          await run(i + 1)
          return
        }
        el.scrollIntoView({ block: 'nearest', inline: 'nearest' })
        setIndex(i)
        setTarget(el.getBoundingClientRect())
      }
      return run(nextIndex)
    },
    [location.pathname, navigate, stop],
  )

  const start = useCallback(() => {
    void go(0)
  }, [go])

  useEffect(() => {
    if (index == null) return
    const current = WORKBENCH_TOUR_STEPS[index]
    if (!current?.selector) {
      setTarget(null)
      return
    }
    void waitForEl(current.selector).then((el) => {
      if (el) setTarget(el.getBoundingClientRect())
    })
  }, [location.pathname, index])

  useEffect(() => {
    if (tourDone()) return
    const t = window.setTimeout(() => start(), 700)
    return () => window.clearTimeout(t)
  }, [start])

  useEffect(() => {
    if (index == null || !step?.selector) return
    const measure = () => {
      const el = document.querySelector(step.selector!)
      if (el instanceof HTMLElement) setTarget(el.getBoundingClientRect())
    }
    window.addEventListener('resize', measure)
    window.addEventListener('scroll', measure, true)
    return () => {
      window.removeEventListener('resize', measure)
      window.removeEventListener('scroll', measure, true)
    }
  }, [index, step?.selector])

  const api = useMemo(() => ({ active, start, stop }), [active, start, stop])

  const pad = 8
  const hole = target
    ? {
        top: Math.max(8, target.top - pad),
        left: Math.max(8, target.left - pad),
        width: target.width + pad * 2,
        height: target.height + pad * 2,
      }
    : null

  const cardW = 340
  const cardH = 200
  let cardTop = 80
  let cardLeft = 24
  if (hole) {
    cardTop = hole.top + hole.height + 12
    cardLeft = hole.left
    if (cardTop + cardH > window.innerHeight - 16) {
      cardTop = Math.max(16, hole.top - cardH - 12)
    }
    if (cardLeft + cardW > window.innerWidth - 16) {
      cardLeft = Math.max(16, window.innerWidth - cardW - 16)
    }
  }

  return (
    <TourContext.Provider value={api}>
      {children}
      {active && step && (
        <>
          <div
            aria-hidden
            onClick={(e) => e.stopPropagation()}
            style={{
              position: 'fixed',
              inset: 0,
              zIndex: 390,
              background: hole ? 'transparent' : 'rgba(15, 23, 36, 0.45)',
            }}
          />
          {hole && (
            <div
              aria-hidden
              style={{
                position: 'fixed',
                top: hole.top,
                left: hole.left,
                width: hole.width,
                height: hole.height,
                borderRadius: 10,
                boxShadow: '0 0 0 9999px rgba(15, 23, 36, 0.48)',
                pointerEvents: 'none',
                zIndex: 391,
                transition: 'top 160ms ease, left 160ms ease, width 160ms ease, height 160ms ease',
              }}
            />
          )}
          <Paper
            withBorder
            shadow="lg"
            p="md"
            radius="md"
            role="dialog"
            aria-label={step.title}
            style={{
              position: 'fixed',
              top: cardTop,
              left: cardLeft,
              width: cardW,
              zIndex: 392,
            }}
          >
            <Text size="xs" c="dimmed" mb={4}>
              {index! + 1} / {WORKBENCH_TOUR_STEPS.length}
            </Text>
            <Title order={5} mb={6}>
              {step.title}
            </Title>
            <Text size="sm" c="dimmed" mb="md">
              {step.body}
            </Text>
            <Group justify="space-between">
              <Button variant="subtle" size="xs" color="gray" onClick={stop}>
                Skip
              </Button>
              <Group gap="xs">
                <Button
                  variant="default"
                  size="xs"
                  disabled={index === 0}
                  onClick={() => void go(index! - 1)}
                >
                  Back
                </Button>
                <Button size="xs" onClick={() => void go(index! + 1)}>
                  {index === WORKBENCH_TOUR_STEPS.length - 1 ? 'Done' : 'Next'}
                </Button>
              </Group>
            </Group>
          </Paper>
        </>
      )}
    </TourContext.Provider>
  )
}
