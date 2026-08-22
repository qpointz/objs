import { useRef, useState } from 'react'
import { Alert, Button, Group, Modal, Stack, Text } from '@mantine/core'
import { execMatcher } from './api'
import {
  MatcherQueryForm,
  matcherBodyOneLiner,
  type MatcherQueryFormHandle,
} from './MatcherQueryForm'
import { useGraphContext } from './GraphContextProvider'

type Props = {
  opened: boolean
  onClose: () => void
  /**
   * Optional: after context is set, let the caller apply canvas contents
   * (Explorer). When omitted, only shared context is updated.
   */
  onApplied?: (contents: { entities: unknown[]; edges: unknown[] }, body: unknown) => void
  /** Scope matcher to this graph when set (Explorer Graph → Matcher handoff). */
  scopeGraphId?: string | null
  /** When false, do not update shared graph context (Composer New → Matcher). Default true. */
  bindSharedContext?: boolean
  title?: string
  description?: string
}

/** Open menu → Matcher: run a matcher and store it as shared graph context. */
export function OpenMatcherModal({
  opened,
  onClose,
  onApplied,
  scopeGraphId = null,
  bindSharedContext = true,
  title = 'Open matcher',
  description = 'Run a matcher and use the result as the shared graph context (Selection) across Explorer, Objects, and Query.',
}: Props) {
  const { setMatcher } = useGraphContext()
  const formRef = useRef<MatcherQueryFormHandle>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function onApply() {
    setError(null)
    try {
      const mode = formRef.current?.getMode()
      const body = formRef.current?.build()
      if (body === undefined || mode === undefined) {
        throw new Error('Matcher form is not ready')
      }
      setBusy(true)
      const contents = await execMatcher(mode, body, scopeGraphId)
      const entities = contents.entities ?? []
      const edges = contents.edges ?? []
      const line = matcherBodyOneLiner(body)
      if (bindSharedContext) {
        setMatcher(body, line, { nodeCount: entities.length, edgeCount: edges.length })
      }
      onApplied?.({ entities, edges }, body)
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal opened={opened} onClose={onClose} title={title} size="lg">
      <Stack gap="sm">
        <Text size="sm" c="dimmed">
          {description}
        </Text>
        {error && (
          <Alert color="red" title="Matcher failed" withCloseButton onClose={() => setError(null)}>
            {error}
          </Alert>
        )}
        <MatcherQueryForm ref={formRef} defaultMode="obj-expr" />
        <Group justify="flex-end" gap="xs">
          <Button variant="default" onClick={onClose} disabled={busy}>
            Cancel
          </Button>
          <Button onClick={() => void onApply()} loading={busy}>
            Apply
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
