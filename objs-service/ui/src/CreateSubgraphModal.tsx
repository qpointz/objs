import { useEffect, useState } from 'react'
import { Alert, Button, Group, Modal, Stack, Text } from '@mantine/core'
import { createSoftLinkSubgraph, deleteSoftLinkSubgraph, snapshotSoftLinkSubgraph } from './api'
import {
  EMPTY_KEY_VALUE_ROWS,
  KeyValueRowsEditor,
  rowsToStringMap,
  type KeyValueRow,
} from './KeyValueRowsEditor'
import type { BoMSubgraph, SoftLinkSubgraph } from './types'

export type CreateSubgraphMode = 'soft' | 'hard'

type Props = {
  opened: boolean
  mode: CreateSubgraphMode
  draftSubgraph: BoMSubgraph
  onClose: () => void
  /** Called after hard snapshot with the clone pack members (replace draft). Soft create skips this. */
  onHardCreated?: (subgraph: BoMSubgraph) => void
  onCreated?: (pack: SoftLinkSubgraph) => void
}

function draftMemberIds(draft: BoMSubgraph): { entityIds: string[]; edgeIds: string[] } {
  const entityIds = (draft.entities ?? []).map((e) => e.id).filter(Boolean)
  const edgeIds = (draft.edges ?? [])
    .map((e) => e.id)
    .filter((id): id is string => typeof id === 'string' && id.length > 0)
  return { entityIds, edgeIds }
}

export function CreateSubgraphModal({
  opened,
  mode,
  draftSubgraph,
  onClose,
  onHardCreated,
  onCreated,
}: Props) {
  const [rows, setRows] = useState<KeyValueRow[]>(() =>
    EMPTY_KEY_VALUE_ROWS.map((row) => ({ ...row })),
  )
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [createdId, setCreatedId] = useState<string | null>(null)

  useEffect(() => {
    if (!opened) return
    setError(null)
    setCreatedId(null)
    setBusy(false)
    setRows(EMPTY_KEY_VALUE_ROWS.map((row) => ({ ...row })))
  }, [opened, mode])

  async function onSubmit() {
    setBusy(true)
    setError(null)
    setCreatedId(null)
    try {
      const { entityIds, edgeIds } = draftMemberIds(draftSubgraph)
      if (entityIds.length === 0 && edgeIds.length === 0) {
        throw new Error('Draft has no entities or edges to pack')
      }
      const annotations = rowsToStringMap(rows)
      if (mode === 'hard' && Object.keys(annotations).length === 0) {
        throw new Error('Snapshot requires at least one header annotation key/value')
      }
      const soft = await createSoftLinkSubgraph({ annotations, entityIds, edgeIds })
      if (mode === 'soft') {
        setCreatedId(soft.id)
        onCreated?.(soft)
        return
      }
      const hard = await snapshotSoftLinkSubgraph(soft.id, annotations)
      try {
        await deleteSoftLinkSubgraph(soft.id)
      } catch {
        /* intermediate soft pack cleanup is best-effort */
      }
      setCreatedId(hard.id)
      onCreated?.(hard)
      onHardCreated?.(hard.subgraph)
      onClose()
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const title = mode === 'soft' ? 'Create soft subgraph' : 'Create hard snapshot'
  const submitLabel = mode === 'soft' ? 'Create soft-link pack' : 'Snapshot clones'

  return (
    <Modal opened={opened} onClose={onClose} title={title} size="md">
      <Stack gap="sm">
        {error && (
          <Alert color="red" title="Error">
            {error}
          </Alert>
        )}
        {createdId && mode === 'soft' && (
          <Alert color="green" title="Created">
            Soft-link pack id: <code>{createdId}</code>
          </Alert>
        )}
        <Text size="sm" c="dimmed">
          {mode === 'soft'
            ? 'Soft-links the current draft entity/edge ids (no copy). Free-form header annotations.'
            : 'Hard-clones draft members under a new pack (temporary soft pack is cleaned up). Annotations stamp the new header and clones.'}
        </Text>
        <Text size="sm" fw={500}>
          Header annotations
        </Text>
        <KeyValueRowsEditor rows={rows} onChange={setRows} />
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose} disabled={busy}>
            {createdId && mode === 'soft' ? 'Close' : 'Cancel'}
          </Button>
          {!(createdId && mode === 'soft') && (
            <Button loading={busy} onClick={() => void onSubmit()}>
              {submitLabel}
            </Button>
          )}
        </Group>
      </Stack>
    </Modal>
  )
}
