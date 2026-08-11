import { Button, Checkbox, Group, Pagination, Table, Text } from '@mantine/core'
import { useMemo, useState } from 'react'
import type { BoMEntity } from './types'

const PAGE_SIZE = 20
const MAX_PAYLOAD_COLS = 6

function isScalar(value: unknown): boolean {
  return (
    typeof value === 'string' ||
    typeof value === 'number' ||
    typeof value === 'boolean'
  )
}

export function formatObjectCell(value: unknown): string {
  if (value == null) return '—'
  if (typeof value === 'boolean') return value ? 'true' : 'false'
  const text = String(value)
  return text.length > 48 ? `${text.slice(0, 45)}…` : text
}

/** Prefer frequent top-level scalar payload keys across the result set. */
export function scalarPayloadColumns(entities: BoMEntity[], max = MAX_PAYLOAD_COLS): string[] {
  const counts = new Map<string, number>()
  for (const entity of entities) {
    const payload = entity.payload
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) continue
    for (const [key, value] of Object.entries(payload)) {
      if (!isScalar(value)) continue
      counts.set(key, (counts.get(key) ?? 0) + 1)
    }
  }
  return [...counts.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
    .slice(0, max)
    .map(([key]) => key)
}

export type ObjectResultsTableProps = {
  results: BoMEntity[]
  /** Ids currently in the target collection (draft or shelf). */
  memberIds: ReadonlySet<string>
  /** Summary line left of bulk actions (e.g. result count + stats). */
  summary?: string
  statusColumnLabel: string
  memberButtonLabel: string
  nonMemberButtonLabel: string
  addSelectedLabel?: string
  removeSelectedLabel?: string
  onToggleMember: (entity: BoMEntity) => void
  onAddSelected: (entities: BoMEntity[]) => void
  onRemoveSelected: (ids: string[]) => void
}

export function ObjectResultsTable({
  results,
  memberIds,
  summary,
  statusColumnLabel,
  memberButtonLabel,
  nonMemberButtonLabel,
  addSelectedLabel = 'Add selected',
  removeSelectedLabel = 'Remove selected',
  onToggleMember,
  onAddSelected,
  onRemoveSelected,
}: ObjectResultsTableProps) {
  const [page, setPage] = useState(1)
  const [selectedIds, setSelectedIds] = useState<Set<string>>(() => new Set())

  const payloadCols = useMemo(() => scalarPayloadColumns(results), [results])
  const pageCount = Math.max(1, Math.ceil(results.length / PAGE_SIZE))
  const safePage = Math.min(page, pageCount)
  const pageRows = useMemo(() => {
    const start = (safePage - 1) * PAGE_SIZE
    return results.slice(start, start + PAGE_SIZE)
  }, [results, safePage])

  const allPageSelected =
    pageRows.length > 0 && pageRows.every((e) => selectedIds.has(e.id))
  const somePageSelected = pageRows.some((e) => selectedIds.has(e.id))

  function toggleRowSelected(id: string, checked: boolean) {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (checked) next.add(id)
      else next.delete(id)
      return next
    })
  }

  if (results.length === 0) {
    return null
  }

  return (
    <>
      <Group justify="space-between" wrap="wrap" gap={4}>
        <Text size="xs" c="dimmed">
          {summary ?? `${results.length} result${results.length === 1 ? '' : 's'}`}
        </Text>
        <Group gap={4}>
          <Button
            size="compact-xs"
            variant="light"
            disabled={[...selectedIds].every((id) => memberIds.has(id))}
            onClick={() => {
              const entities = results.filter((e) => selectedIds.has(e.id) && !memberIds.has(e.id))
              if (entities.length > 0) onAddSelected(entities)
            }}
          >
            {addSelectedLabel}
          </Button>
          <Button
            size="compact-xs"
            variant="default"
            disabled={[...selectedIds].every((id) => !memberIds.has(id))}
            onClick={() => {
              const ids = [...selectedIds].filter((id) => memberIds.has(id))
              if (ids.length > 0) onRemoveSelected(ids)
            }}
          >
            {removeSelectedLabel}
          </Button>
        </Group>
      </Group>

      <Table.ScrollContainer minWidth={280}>
        <Table
          striped
          highlightOnHover
          withTableBorder
          withColumnBorders
          horizontalSpacing={6}
          verticalSpacing={3}
          style={{ fontSize: 'var(--mantine-font-size-xs)' }}
        >
          <Table.Thead>
            <Table.Tr>
              <Table.Th w={32}>
                <Checkbox
                  size="xs"
                  aria-label="Select page"
                  checked={allPageSelected}
                  indeterminate={!allPageSelected && somePageSelected}
                  onChange={(e) => {
                    const checked = e.currentTarget.checked
                    setSelectedIds((prev) => {
                      const next = new Set(prev)
                      for (const row of pageRows) {
                        if (checked) next.add(row.id)
                        else next.delete(row.id)
                      }
                      return next
                    })
                  }}
                />
              </Table.Th>
              <Table.Th>Id</Table.Th>
              <Table.Th>Type</Table.Th>
              {payloadCols.map((col) => (
                <Table.Th key={col}>{col}</Table.Th>
              ))}
              <Table.Th w={88}>{statusColumnLabel}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {pageRows.map((entity) => {
              const inMember = memberIds.has(entity.id)
              const payload = entity.payload ?? {}
              return (
                <Table.Tr key={entity.id}>
                  <Table.Td>
                    <Checkbox
                      size="xs"
                      aria-label={`Select ${entity.id}`}
                      checked={selectedIds.has(entity.id)}
                      onChange={(e) => toggleRowSelected(entity.id, e.currentTarget.checked)}
                    />
                  </Table.Td>
                  <Table.Td title={entity.id} style={{ wordBreak: 'break-all', maxWidth: 96 }}>
                    {entity.id.length > 12 ? `${entity.id.slice(0, 8)}…` : entity.id}
                  </Table.Td>
                  <Table.Td>{entity.type}</Table.Td>
                  {payloadCols.map((col) => (
                    <Table.Td key={col}>
                      {formatObjectCell(
                        payload && typeof payload === 'object'
                          ? (payload as Record<string, unknown>)[col]
                          : undefined,
                      )}
                    </Table.Td>
                  ))}
                  <Table.Td>
                    <Button
                      size="compact-xs"
                      variant={inMember ? 'filled' : 'light'}
                      color={inMember ? 'teal' : 'blue'}
                      onClick={() => onToggleMember(entity)}
                    >
                      {inMember ? memberButtonLabel : nonMemberButtonLabel}
                    </Button>
                  </Table.Td>
                </Table.Tr>
              )
            })}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>

      {pageCount > 1 && (
        <Pagination size="sm" value={safePage} onChange={setPage} total={pageCount} />
      )}
    </>
  )
}
