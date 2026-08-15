import { Button, Checkbox, Group, Modal, ScrollArea, Stack, Table, Text } from '@mantine/core'
import { useEffect, useState } from 'react'
import type { AssetView, RelationView } from './api/types'

export type ImpactOrphan = {
  asset: AssetView
  delete: boolean
}

export type BomImpactPlan = {
  title: string
  message: string
  confirmLabel: string
  relations: { rel: RelationView; fromLabel: string; toLabel: string }[]
  deleteAssets: AssetView[]
  orphans: ImpactOrphan[]
}

export function BomImpactDialog({
  plan,
  onClose,
  onConfirm,
}: {
  plan: BomImpactPlan | null
  onClose: () => void
  onConfirm: (plan: BomImpactPlan) => void
}) {
  const [orphans, setOrphans] = useState<ImpactOrphan[]>([])

  useEffect(() => {
    setOrphans(plan?.orphans ?? [])
  }, [plan])

  return (
    <Modal opened={!!plan} onClose={onClose} title={plan?.title} centered size="lg">
      {plan && (
        <Stack>
          <Text size="sm">{plan.message}</Text>
          {plan.deleteAssets.length > 0 && (
            <div>
              <Text size="xs" fw={700} c="dimmed" mb={4}>
                Assets to delete
              </Text>
              {plan.deleteAssets.map((a) => (
                <Text key={a.id} size="sm">
                  {a.label}{' '}
                  <Text span size="xs" c="dimmed">
                    {a.type}
                  </Text>
                </Text>
              ))}
            </div>
          )}
          {plan.relations.length > 0 && (
            <div>
              <Text size="xs" fw={700} c="dimmed" mb={4}>
                Relations to remove
              </Text>
              <ScrollArea mah={180} type="auto">
                <Table striped layout="fixed" withRowBorders={false}>
                  <Table.Tbody>
                    {plan.relations.map((row) => (
                      <Table.Tr key={row.rel.id}>
                        <Table.Td>
                          <Text size="sm">
                            {row.fromLabel} —[{row.rel.label || row.rel.role}]→ {row.toLabel}
                          </Text>
                        </Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </ScrollArea>
            </div>
          )}
          {orphans.length > 0 && (
            <div>
              <Text size="xs" fw={700} c="dimmed" mb={4}>
                Assets left with no relations
              </Text>
              <Text size="xs" c="dimmed" mb={6}>
                Keep is checked by default. Uncheck Keep to delete the asset from the application.
              </Text>
              <Table striped layout="fixed" withRowBorders={false}>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Asset</Table.Th>
                    <Table.Th w={72}>Keep</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {orphans.map((row) => (
                    <Table.Tr key={row.asset.id}>
                      <Table.Td>
                        <Text size="sm" fw={600}>
                          {row.asset.label}
                        </Text>
                        <Text size="xs" c="dimmed">
                          {row.asset.type}
                        </Text>
                      </Table.Td>
                      <Table.Td>
                        <Checkbox
                          aria-label={`Keep ${row.asset.label}`}
                          checked={!row.delete}
                          onChange={(e) => {
                            const keep = e.currentTarget.checked
                            setOrphans((prev) =>
                              prev.map((item) =>
                                item.asset.id === row.asset.id ? { ...item, delete: !keep } : item,
                              ),
                            )
                          }}
                        />
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </div>
          )}
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>
              Cancel
            </Button>
            <Button color="red" onClick={() => onConfirm({ ...plan, orphans })}>
              {plan.confirmLabel}
            </Button>
          </Group>
        </Stack>
      )}
    </Modal>
  )
}
