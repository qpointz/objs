import { useEffect, useState } from 'react'
import { Alert, Button, Modal, Stack, Text, Table } from '@mantine/core'
import { getSoftLinkSubgraph, listSoftLinkSubgraphs } from './api'
import type { BoMSubgraph, SoftLinkSubgraphListItem } from './types'

type Props = {
  opened: boolean
  onClose: () => void
  onOpenPack: (subgraph: BoMSubgraph) => void
}

export function SubgraphPacksModal({ opened, onClose, onOpenPack }: Props) {
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [items, setItems] = useState<SoftLinkSubgraphListItem[]>([])

  async function refreshList() {
    const listed = await listSoftLinkSubgraphs()
    setItems(listed)
  }

  useEffect(() => {
    if (!opened) return
    setError(null)
    void refreshList().catch((e: unknown) => setError(e instanceof Error ? e.message : String(e)))
  }, [opened])

  async function onOpen(id: string) {
    setBusy(true)
    setError(null)
    try {
      const pack = await getSoftLinkSubgraph(id)
      onOpenPack(pack.subgraph)
      onClose()
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal opened={opened} onClose={onClose} title="Open subgraph pack" size="lg">
      <Stack gap="sm">
        {error && (
          <Alert color="red" title="Error">
            {error}
          </Alert>
        )}
        <Text size="sm" c="dimmed">
          Opens with replace (draft baselines reset to pack members). Create packs via Save →
          Subgraph / Snapshot.
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
                  <Button size="xs" variant="light" loading={busy} onClick={() => void onOpen(item.id)}>
                    Open
                  </Button>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
        {items.length === 0 && (
          <Text size="sm" c="dimmed">
            No packs yet.
          </Text>
        )}
      </Stack>
    </Modal>
  )
}
