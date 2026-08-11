import { useRef, useState, type ReactNode } from 'react'
import { Button, Stack } from '@mantine/core'
import { MatcherQueryForm, type MatcherQueryFormHandle } from './MatcherQueryForm'
import type { QueryExecStats } from './queryExecStats'

export { buildMatcherBody, hydrateFromMatcher } from './MatcherQueryForm'
export type { MatcherMode } from './MatcherQueryForm'

type Props = {
  loading?: boolean
  /** Hydrate the form from a previously used matcher body. */
  matcher?: unknown | null
  /** Last successful load wall time + retrieved counts (API only). */
  stats?: QueryExecStats | null
  onLoad: (matcherBody: unknown) => Promise<void> | void
  actionLabel?: ReactNode
}

export function MatcherLoadPanel({
  loading,
  matcher,
  stats,
  onLoad,
  actionLabel = 'Load into draft',
}: Props) {
  const formRef = useRef<MatcherQueryFormHandle>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleLoad() {
    setError(null)
    try {
      const body = formRef.current?.build()
      if (body === undefined) {
        throw new Error('Matcher form is not ready')
      }
      await onLoad(body)
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    }
  }

  return (
    <Stack gap="xs">
      <MatcherQueryForm
        ref={formRef}
        matcher={matcher}
        error={error}
        stats={stats}
        action={
          <Button size="xs" loading={loading} onClick={() => void handleLoad()}>
            {actionLabel}
          </Button>
        }
      />
    </Stack>
  )
}
