import { useEffect, useMemo, useRef, useState, type ReactNode, type UIEvent } from 'react'
import { Box, Code, Group, Pagination, Stack, Table, Text } from '@mantine/core'
import { QUERY_STRUCT_VIRTUALIZE_THRESHOLD } from './queryStructuredModel'

const ROW_HEIGHT = 28
const OVERSCAN = 8

/** Default page size for Structured Vertices / Edges grids. */
export const QUERY_STRUCT_PAGE_SIZE = 25

export const queryResultTableProps = {
  striped: true,
  highlightOnHover: true,
  withTableBorder: true,
  withColumnBorders: true,
  horizontalSpacing: 6 as const,
  verticalSpacing: 3 as const,
  style: { fontSize: 'var(--mantine-font-size-xs)' },
}

type Col<T> = {
  key: string
  header: ReactNode
  width?: number | string
  render: (row: T, index: number) => ReactNode
}

type Props<T> = {
  rows: T[]
  columns: Col<T>[]
  selectedKey?: string | null
  rowKey: (row: T) => string
  onRowSelect?: (row: T) => void
  empty?: ReactNode
  /** Rows per page; default {@link QUERY_STRUCT_PAGE_SIZE}. Pass `0` to disable paging. */
  pageSize?: number
  /** Notified when the visible page slice changes (for page-scoped bulk actions). */
  onPageRowsChange?: (pageRows: T[]) => void
}

/**
 * Shared Structured / Objects-like grid chrome.
 * Pages by default (25); virtualizes only when paging is off and rows > 200.
 */
export function QueryResultGrid<T>({
  rows,
  columns,
  selectedKey,
  rowKey,
  onRowSelect,
  empty,
  pageSize = QUERY_STRUCT_PAGE_SIZE,
  onPageRowsChange,
}: Props<T>) {
  const scrollRef = useRef<HTMLDivElement>(null)
  const [scrollTop, setScrollTop] = useState(0)
  const [page, setPage] = useState(1)
  const paging = pageSize > 0
  const pageCount = paging ? Math.max(1, Math.ceil(rows.length / pageSize)) : 1
  const safePage = Math.min(page, pageCount)

  useEffect(() => {
    setPage(1)
    setScrollTop(0)
    if (scrollRef.current) scrollRef.current.scrollTop = 0
  }, [rows, pageSize])

  const pagedRows = useMemo(() => {
    if (!paging) return rows
    const start = (safePage - 1) * pageSize
    return rows.slice(start, start + pageSize)
  }, [rows, paging, safePage, pageSize])

  useEffect(() => {
    onPageRowsChange?.(pagedRows)
  }, [onPageRowsChange, pagedRows])

  const virtualize = !paging && pagedRows.length > QUERY_STRUCT_VIRTUALIZE_THRESHOLD

  const { start, end, padTop, padBottom } = useMemo(() => {
    if (!virtualize) {
      return { start: 0, end: pagedRows.length, padTop: 0, padBottom: 0 }
    }
    const viewport = scrollRef.current?.clientHeight ?? 320
    const startIdx = Math.max(0, Math.floor(scrollTop / ROW_HEIGHT) - OVERSCAN)
    const visible = Math.ceil(viewport / ROW_HEIGHT) + OVERSCAN * 2
    const endIdx = Math.min(pagedRows.length, startIdx + visible)
    return {
      start: startIdx,
      end: endIdx,
      padTop: startIdx * ROW_HEIGHT,
      padBottom: Math.max(0, (pagedRows.length - endIdx) * ROW_HEIGHT),
    }
  }, [pagedRows.length, scrollTop, virtualize])

  if (rows.length === 0) {
    return empty ?? null
  }

  const slice = pagedRows.slice(start, end)

  function onScroll(e: UIEvent<HTMLDivElement>) {
    setScrollTop(e.currentTarget.scrollTop)
  }

  return (
    <Stack gap="xs" style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
      <Box
        ref={scrollRef}
        onScroll={virtualize ? onScroll : undefined}
        style={{
          flex: 1,
          minHeight: 0,
          overflow: 'auto',
          maxHeight: '100%',
        }}
      >
        <Table.ScrollContainer minWidth={280}>
          <Table {...queryResultTableProps}>
            <Table.Thead>
              <Table.Tr>
                {columns.map((col) => (
                  <Table.Th
                    key={col.key}
                    w={col.width}
                    style={
                      col.width != null
                        ? { width: col.width, maxWidth: col.width, minWidth: col.width }
                        : undefined
                    }
                  >
                    {col.header}
                  </Table.Th>
                ))}
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {virtualize && padTop > 0 && (
                <Table.Tr aria-hidden>
                  <Table.Td colSpan={columns.length} p={0} style={{ height: padTop, border: 0 }} />
                </Table.Tr>
              )}
              {slice.map((row, i) => {
                const key = rowKey(row)
                const selected = selectedKey != null && selectedKey === key
                const absIndex = (paging ? (safePage - 1) * pageSize : 0) + start + i
                return (
                  <Table.Tr
                    key={key}
                    style={{
                      cursor: onRowSelect ? 'pointer' : undefined,
                      background: selected ? 'var(--mantine-color-blue-light)' : undefined,
                      height: virtualize ? ROW_HEIGHT : undefined,
                    }}
                    onClick={onRowSelect ? () => onRowSelect(row) : undefined}
                  >
                    {columns.map((col) => (
                      <Table.Td
                        key={col.key}
                        style={
                          col.width != null
                            ? { width: col.width, maxWidth: col.width, minWidth: col.width }
                            : undefined
                        }
                      >
                        {col.render(row, absIndex)}
                      </Table.Td>
                    ))}
                  </Table.Tr>
                )
              })}
              {virtualize && padBottom > 0 && (
                <Table.Tr aria-hidden>
                  <Table.Td
                    colSpan={columns.length}
                    p={0}
                    style={{ height: padBottom, border: 0 }}
                  />
                </Table.Tr>
              )}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      </Box>
      {paging && pageCount > 1 && (
        <Group justify="flex-start" style={{ flexShrink: 0 }}>
          <Pagination size="sm" value={safePage} onChange={setPage} total={pageCount} />
        </Group>
      )}
    </Stack>
  )
}

export function QueryTableAlikeGrid({
  columns,
  rows,
}: {
  columns: string[]
  rows: unknown[][]
}) {
  type Row = { row: unknown[]; i: number }
  const data: Row[] = rows.map((row, i) => ({ row, i }))
  return (
    <QueryResultGrid<Row>
      rows={data}
      rowKey={(r) => String(r.i)}
      columns={columns.map((col, ci) => ({
        key: col,
        header: col,
        render: (r) => {
          const cell = r.row[ci]
          return (
            <Code style={{ whiteSpace: 'pre-wrap', fontSize: 11 }}>
              {cell == null ? '' : typeof cell === 'string' ? cell : JSON.stringify(cell)}
            </Code>
          )
        },
      }))}
    />
  )
}

export function QuerySectionLabel({ children }: { children: ReactNode }) {
  return (
    <Text size="xs" c="dimmed" fw={500} mb={4}>
      {children}
    </Text>
  )
}

export function QueryStructuredStack({ children }: { children: ReactNode }) {
  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
      {children}
    </Stack>
  )
}
