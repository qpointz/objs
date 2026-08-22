import { Button, Checkbox, Group, Text } from '@mantine/core'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  IdLink,
  QUERY_STRUCT_ID_COL_WIDTH,
  QUERY_STRUCT_TYPE_COL_WIDTH,
} from './QueryStructColumns'
import { QUERY_STRUCT_PAGE_SIZE, QueryResultGrid } from './QueryResultGrid'
import type { BoMEntity } from './types'

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
  /** Optional summary left of bulk actions (e.g. Composer Add Objects panel). */
  summary?: string
  statusColumnLabel: string
  memberButtonLabel: string
  nonMemberButtonLabel: string
  addSelectedLabel?: string
  removeSelectedLabel?: string
  onToggleMember: (entity: BoMEntity) => void
  onAddSelected: (entities: BoMEntity[]) => void
  onRemoveSelected: (ids: string[]) => void
  /** When set, Id column is a link that opens the object viewer (Objects view). */
  onOpenId?: (entity: BoMEntity) => void
  /** Controlled row selection (checkbox column). */
  selectedIds?: ReadonlySet<string>
  onSelectedIdsChange?: (ids: Set<string>) => void
  /** Hide inline bulk-action row (actions live in view chrome instead). */
  hideBulkActions?: boolean
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
  onOpenId,
  selectedIds: selectedIdsProp,
  onSelectedIdsChange,
  hideBulkActions = false,
}: ObjectResultsTableProps) {
  const [selectedIdsInternal, setSelectedIdsInternal] = useState<Set<string>>(() => new Set())
  const selectedIds = selectedIdsProp ?? selectedIdsInternal

  const applySelectedIds = useCallback(
    (update: Set<string> | ((prev: Set<string>) => Set<string>)) => {
      const prev = new Set(selectedIds)
      const next = typeof update === 'function' ? update(prev) : update
      if (
        next.size === selectedIds.size &&
        [...next].every((id) => selectedIds.has(id))
      ) {
        return
      }
      if (onSelectedIdsChange) onSelectedIdsChange(next)
      else setSelectedIdsInternal(next)
    },
    [onSelectedIdsChange, selectedIds],
  )
  const [pageRows, setPageRows] = useState<BoMEntity[]>([])
  const payloadCols = useMemo(() => scalarPayloadColumns(results), [results])

  useEffect(() => {
    applySelectedIds((prev) => {
      const valid = new Set(results.map((e) => e.id))
      const next = new Set([...prev].filter((id) => valid.has(id)))
      return next.size === prev.size ? prev : next
    })
  }, [results, applySelectedIds])

  const onPageRowsChange = useCallback((rows: BoMEntity[]) => {
    setPageRows(rows)
  }, [])

  if (results.length === 0) {
    return null
  }

  const allPageSelected =
    pageRows.length > 0 && pageRows.every((e) => selectedIds.has(e.id))
  const somePageSelected = pageRows.some((e) => selectedIds.has(e.id))

  function toggleRowSelected(id: string, checked: boolean) {
    applySelectedIds((prev) => {
      const next = new Set(prev)
      if (checked) next.add(id)
      else next.delete(id)
      return next
    })
  }

  return (
    <Group
      gap="xs"
      align="stretch"
      wrap="nowrap"
      style={{ flex: 1, minWidth: 0, minHeight: 0, flexDirection: 'column' }}
    >
      {!hideBulkActions && (
        <Group justify={summary != null ? 'space-between' : 'flex-end'} wrap="wrap" gap={4}>
          {summary != null && (
            <Text size="xs" c="dimmed">
              {summary}
            </Text>
          )}
          <Group gap={4}>
            <Button
              size="compact-xs"
              variant="light"
              disabled={
                selectedIds.size === 0 || [...selectedIds].every((id) => memberIds.has(id))
              }
              onClick={() => {
                const entities = results.filter(
                  (e) => selectedIds.has(e.id) && !memberIds.has(e.id),
                )
                if (entities.length > 0) onAddSelected(entities)
              }}
            >
              {addSelectedLabel}
            </Button>
            <Button
              size="compact-xs"
              variant="default"
              disabled={
                selectedIds.size === 0 || [...selectedIds].every((id) => !memberIds.has(id))
              }
              onClick={() => {
                const ids = [...selectedIds].filter((id) => memberIds.has(id))
                if (ids.length > 0) onRemoveSelected(ids)
              }}
            >
              {removeSelectedLabel}
            </Button>
          </Group>
        </Group>
      )}

      <QueryResultGrid
        rows={results}
        pageSize={QUERY_STRUCT_PAGE_SIZE}
        rowKey={(entity) => entity.id}
        onPageRowsChange={onPageRowsChange}
        columns={[
          {
            key: 'select',
            header: (
              <Checkbox
                size="xs"
                aria-label="Select page"
                checked={allPageSelected}
                indeterminate={!allPageSelected && somePageSelected}
                onChange={(e) => {
                  const checked = e.currentTarget.checked
                  applySelectedIds((prev) => {
                    const next = new Set(prev)
                    for (const row of pageRows) {
                      if (checked) next.add(row.id)
                      else next.delete(row.id)
                    }
                    return next
                  })
                }}
              />
            ),
            width: 32,
            render: (entity) => (
              <Checkbox
                size="xs"
                aria-label={`Select ${entity.id}`}
                checked={selectedIds.has(entity.id)}
                onChange={(e) => toggleRowSelected(entity.id, e.currentTarget.checked)}
                onClick={(e) => e.stopPropagation()}
              />
            ),
          },
          {
            key: 'id',
            header: 'Id',
            width: QUERY_STRUCT_ID_COL_WIDTH,
            render: (entity) =>
              onOpenId != null ? (
                <IdLink id={entity.id} onOpen={() => onOpenId(entity)} />
              ) : (
                <Text size="xs" ff="monospace" title={entity.id} truncate>
                  {entity.id.length > 12 ? `${entity.id.slice(0, 8)}…` : entity.id}
                </Text>
              ),
          },
          {
            key: 'type',
            header: 'Type',
            width: QUERY_STRUCT_TYPE_COL_WIDTH,
            render: (entity) => entity.type,
          },
          ...payloadCols.map((col) => ({
            key: col,
            header: col,
            render: (entity: BoMEntity) => {
              const payload = entity.payload ?? {}
              return formatObjectCell(
                payload && typeof payload === 'object'
                  ? (payload as Record<string, unknown>)[col]
                  : undefined,
              )
            },
          })),
          {
            key: 'shelf',
            header: statusColumnLabel,
            width: 88,
            render: (entity) => {
              const inMember = memberIds.has(entity.id)
              return (
                <Button
                  size="compact-xs"
                  variant={inMember ? 'filled' : 'light'}
                  color={inMember ? 'teal' : 'blue'}
                  onClick={(e) => {
                    e.stopPropagation()
                    onToggleMember(entity)
                  }}
                >
                  {inMember ? memberButtonLabel : nonMemberButtonLabel}
                </Button>
              )
            },
          },
        ]}
      />
    </Group>
  )
}
