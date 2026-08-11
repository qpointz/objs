import { useEffect, useState } from 'react'
import { Alert, Button, Modal, Stack, Text, Table } from '@mantine/core'
import { getGraph, listGraphs } from './api'
import type { BoMGraphListItem, BoMGraphResponse } from './types'

type Props = {
  opened: boolean
  onClose: () => void
  /** Called with the opened graph's id + resolved header/members. */
  onOpen: (graphId: string, resolved: BoMGraphResponse) => void
}

/** "Open graph…" (WI-005): lists `bom_graph` headers and resolves the chosen one. */
export function OpenGraphModal({ opened, onClose, onOpen }: Props) {
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [items, setItems] = useState<BoMGraphListItem[]>([])

  async function refreshList() {
    const listed = await listGraphs()
    setItems(listed)
  }

  useEffect(() => {
    if (!opened) return
    setError(null)
    void refreshList().catch((e: unknown) => setError(e instanceof Error ? e.message : String(e)))
  }, [opened])

  async function onOpenRow(id: string) {
    setBusy(true)
    setError(null)
    try {
      const resolved = await getGraph(id)
      onOpen(id, resolved)
      onClose()
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal opened={opened} onClose={onClose} title="Open graph" size="lg">
      <Stack gap="sm">
        {error && (
          <Alert color="red" title="Error">
            {error}
          </Alert>
        )}
        <Text size="sm" c="dimmed">
          Opens with replace (draft baselines reset to this graph's members). Create a graph via
          New graph, or Save ▾ → Clone.
        </Text>
        <Table striped highlightOnHover withTableBorder>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Id</Table.Th>
              <Table.Th>Annotations</Table.Th>
              <Table.Th>Members</Table.Th>
              <Table.Th />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {items.map((item) => (
              <Table.Tr key={item.id}>
                <Table.Td>
                  <Text size="xs" ff="monospace">
                    {item.id}
                  </Text>
                </Table.Td>
                <Table.Td>
                  <Text size="xs">{JSON.stringify(item.annotations)}</Text>
                </Table.Td>
                <Table.Td>
                  {item.entityCount}e / {item.edgeCount}E
                </Table.Td>
                <Table.Td>
                  <Button size="xs" variant="light" loading={busy} onClick={() => void onOpenRow(item.id)}>
                    Open
                  </Button>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
        {items.length === 0 && (
          <Text size="sm" c="dimmed">
            No graphs yet.
          </Text>
        )}
      </Stack>
    </Modal>
  )
}
