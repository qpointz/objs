import { useRef, useState } from 'react'
import { Button, Stack } from '@mantine/core'
import { MatcherQueryForm, type MatcherQueryFormHandle } from './MatcherQueryForm'

export { buildMatcherBody, hydrateFromMatcher } from './MatcherQueryForm'
export type { MatcherMode, AnnoRow } from './MatcherQueryForm'

type Props = {
  loading?: boolean
  /** Hydrate the form from a previously used matcher body. */
  matcher?: unknown | null
  onLoad: (matcherBody: unknown) => Promise<void> | void
}

export function MatcherLoadPanel({ loading, matcher, onLoad }: Props) {
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
        action={
          <Button size="xs" loading={loading} onClick={() => void handleLoad()}>
            Load into draft
          </Button>
        }
      />
    </Stack>
  )
}
