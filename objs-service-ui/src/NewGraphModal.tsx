import { useEffect, useState } from 'react'
import { Alert, Button, Group, Modal, Stack, Text } from '@mantine/core'
import { cloneGraph, createGraph, createGraphVersion, getGraph } from './api'
import {
  EMPTY_KEY_VALUE_ROWS,
  KeyValueRowsEditor,
  rowsToStringMap,
  type KeyValueRow,
} from './KeyValueRowsEditor'
import type { BoMGraphResponse } from './types'

/** `new` empty header; `clone` new-id deep copy; `snapshot` freeze on the same graph id. */
export type NewGraphMode = 'new' | 'clone' | 'snapshot'

type Props = {
  opened: boolean
  mode: NewGraphMode
  /** Required for `clone` / `snapshot` — the current graph being copied. */
  cloneSourceGraphId?: string | null
  onClose: () => void
  onCreated: (graphId: string, resolved: BoMGraphResponse) => void
}

export function NewGraphModal({
  opened,
  mode,
  cloneSourceGraphId,
  onClose,
  onCreated,
}: Props) {
  const [rows, setRows] = useState<KeyValueRow[]>(() =>
    EMPTY_KEY_VALUE_ROWS.map((row) => ({ ...row })),
  )
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!opened) return
    setError(null)
    setBusy(false)
    setRows(EMPTY_KEY_VALUE_ROWS.map((row) => ({ ...row })))
  }, [opened, mode])

  async function onSubmit() {
    setBusy(true)
    setError(null)
    try {
      const annotations = rowsToStringMap(rows)
      if (!cloneSourceGraphId && mode !== 'new') {
        throw new Error('No current graph selected')
      }
      const resolved =
        mode === 'new'
          ? await createGraph({ annotations })
          : mode === 'clone'
            ? await cloneGraph(cloneSourceGraphId!, annotations)
            : await (async () => {
                await createGraphVersion(cloneSourceGraphId!, annotations)
                return getGraph(cloneSourceGraphId!)
              })()
      onCreated(resolved.id, resolved)
      onClose()
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const title = mode === 'new' ? 'New graph' : mode === 'clone' ? 'Clone graph' : 'Create version'
  const submitLabel =
    mode === 'new' ? 'Create graph' : mode === 'clone' ? 'Clone' : 'Create version'

  return (
    <Modal opened={opened} onClose={onClose} title={title} size="md">
      <Stack gap="sm">
        {error && (
          <Alert color="red" title="Error">
            {error}
          </Alert>
        )}
        <Text size="sm" c="dimmed">
          {mode === 'new'
            ? 'Creates a new, empty graph header. Set as the current graph after creation.'
            : mode === 'clone'
              ? 'Deep-copies the current graph into a new graph with new object ids and an empty history. Switches Composer to the clone.'
              : 'Creates a version of the current graph (same id). Composer stays here. Explorer can open that freeze later.'}
        </Text>
        <Text size="sm" fw={500}>
          Header annotations
        </Text>
        <KeyValueRowsEditor rows={rows} onChange={setRows} />
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose} disabled={busy}>
            Cancel
          </Button>
          <Button loading={busy} onClick={() => void onSubmit()}>
            {submitLabel}
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
