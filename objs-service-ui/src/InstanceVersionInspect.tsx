import { useEffect, useState } from 'react'
import {
  Anchor,
  Button,
  Group,
  Modal,
  ScrollArea,
  Stack,
  Text,
} from '@mantine/core'
import {
  getEdgeVersion,
  getEntityVersion,
  listEdgeVersions,
  listEntityVersions,
} from './api'
import {
  EntityAnnotationsView,
  EntityPayloadView,
} from './EntityCardNode'
import { formatInstanceVersionLabel } from './objectViewerTitle'
import { formatVersionIdForList } from './objectVersionRows'
import type { BoMEdge, BoMEntity, BoMInstanceVersionSummary } from './types'

export { formatInstanceVersionLabel }

type Kind = 'entity' | 'edge'

type Props = {
  kind: Kind
  id: string
  /** Current selection's headVersion (null → LATEST). */
  headVersion?: number | null
  /** Schema field kinds for payload rendering when inspecting a version. */
  fieldKinds?: Record<string, import('./types').PayloadFieldKind>
}

/**
 * Node/edge version readout + on-demand versions dialog (G-UX-objver).
 * Does not fetch the version list until the user opens the dialog.
 */
export function InstanceVersionInspect({ kind, id, headVersion, fieldKinds }: Props) {
  const [opened, setOpened] = useState(false)
  const [rows, setRows] = useState<BoMInstanceVersionSummary[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [inspect, setInspect] = useState<BoMEntity | BoMEdge | null>(null)

  useEffect(() => {
    if (!opened) return
    let cancelled = false
    setBusy(true)
    setError(null)
    setInspect(null)
    const load = kind === 'entity' ? listEntityVersions(id) : listEdgeVersions(id)
    load
      .then((list) => {
        if (!cancelled) setRows(list)
      })
      .catch((e) => {
        if (!cancelled) {
          setRows([])
          setError(e instanceof Error ? e.message : String(e))
        }
      })
      .finally(() => {
        if (!cancelled) setBusy(false)
      })
    return () => {
      cancelled = true
    }
  }, [opened, kind, id])

  async function openVersion(version: number) {
    setBusy(true)
    setError(null)
    try {
      const row =
        kind === 'entity' ? await getEntityVersion(id, version) : await getEdgeVersion(id, version)
      setInspect(row)
    } catch (e) {
      setInspect(null)
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <Group gap={6} wrap="wrap" align="center">
        <Text size="sm">
          <Text span fw={600}>
            version:{' '}
          </Text>
          {formatInstanceVersionLabel(headVersion)}
        </Text>
        <Anchor
          component="button"
          type="button"
          size="sm"
          onClick={() => setOpened(true)}
          data-tour="instance-versions"
        >
          versions
        </Anchor>
      </Group>

      <Modal
        opened={opened}
        onClose={() => {
          setOpened(false)
          setInspect(null)
        }}
        title={kind === 'entity' ? 'Object versions' : 'Edge versions'}
        size="lg"
      >
        <Stack gap="sm">
          {error && (
            <Text size="sm" c="red">
              {error}
            </Text>
          )}
          {busy && !inspect && (
            <Text size="sm" c="dimmed">
              Loading…
            </Text>
          )}
          {!busy && rows.length === 0 && !error && (
            <Text size="sm" c="dimmed">
              No deep-capture versions yet (create a graph Snapshot first).
            </Text>
          )}
          <ScrollArea.Autosize mah={220}>
            <Stack gap={6}>
              {rows.map((row) => (
                <Group key={row.version} justify="space-between" wrap="nowrap">
                  <div>
                    <Text size="xs" ff="monospace" title={String(row.version)}>
                      {formatVersionIdForList(row.version)}
                    </Text>
                    <Text size="xs" c="dimmed">
                      {new Date(row.createdAt).toLocaleString()}
                    </Text>
                  </div>
                  <Button size="compact-xs" variant="light" onClick={() => void openVersion(row.version)}>
                    Inspect
                  </Button>
                </Group>
              ))}
            </Stack>
          </ScrollArea.Autosize>
          {inspect && (
            <Stack gap="xs">
              <Text size="sm" fw={600}>
                Inspecting {formatInstanceVersionLabel(inspect.headVersion)}
              </Text>
              {'payload' in inspect ? (
                <EntityPayloadView
                  payload={inspect.payload ?? {}}
                  fieldKinds={fieldKinds}
                  size="panel"
                  showLabel
                />
              ) : (
                <EntityPayloadView
                  payload={(inspect as BoMEdge).properties ?? {}}
                  fieldKinds={fieldKinds}
                  size="panel"
                  showLabel
                  label="properties"
                />
              )}
              {'annotations' in inspect && inspect.annotations && (
                <EntityAnnotationsView
                  annotations={inspect.annotations}
                  size="panel"
                  showLabel
                />
              )}
            </Stack>
          )}
        </Stack>
      </Modal>
    </>
  )
}
