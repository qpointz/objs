import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Collapse,
  Group,
  Modal,
  Stack,
  Table,
  Text,
  Textarea,
  TextInput,
  UnstyledButton,
} from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { IconChevronDown, IconChevronRight } from '@tabler/icons-react'
import { getGraph, searchGraphs } from './api'
import type { BoMGraphHeader, BoMGraphResponse } from './types'

type Props = {
  opened: boolean
  onClose: () => void
  /** Called with the opened graph's id + resolved header/members. */
  onOpen: (graphId: string, resolved: BoMGraphResponse) => void
}

const SEARCH_LIMIT = 15
const DEBOUNCE_MS = 250

/** Shared "Open graph…" dialog (WI-007 / G-U10): debounced search, never lists the full catalog. */
export function OpenGraphModal({ opened, onClose, onOpen }: Props) {
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [searching, setSearching] = useState(false)
  const [q, setQ] = useState('')
  const [expr, setExpr] = useState('')
  const [exprOpen, setExprOpen] = useState(false)
  const [items, setItems] = useState<BoMGraphHeader[]>([])
  const [debouncedQ] = useDebouncedValue(q, DEBOUNCE_MS)
  const [debouncedExpr] = useDebouncedValue(expr, DEBOUNCE_MS)

  useEffect(() => {
    if (!opened) return
    setError(null)
    setQ('')
    setExpr('')
    setExprOpen(false)
    setItems([])
  }, [opened])

  useEffect(() => {
    if (!opened) return
    const trimmedQ = debouncedQ.trim()
    const trimmedExpr = debouncedExpr.trim()
    if (!trimmedQ && !trimmedExpr) {
      setItems([])
      setSearching(false)
      return
    }
    let cancelled = false
    setSearching(true)
    setError(null)
    void searchGraphs({ q: trimmedQ || undefined, expr: trimmedExpr || undefined, limit: SEARCH_LIMIT })
      .then((res) => {
        if (!cancelled) setItems(res.items)
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setItems([])
          setError(e instanceof Error ? e.message : String(e))
        }
      })
      .finally(() => {
        if (!cancelled) setSearching(false)
      })
    return () => {
      cancelled = true
    }
  }, [opened, debouncedQ, debouncedExpr])

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

  const hasCriteria = q.trim().length > 0 || expr.trim().length > 0

  return (
    <Modal opened={opened} onClose={onClose} title="Open graph" size="lg">
      <Stack gap="sm">
        {error && (
          <Alert color="red" title="Error">
            {error}
          </Alert>
        )}
        <Text size="sm" c="dimmed">
          Search by graph id (UUID / prefix) or annotation text. Optional expression narrows with
          graph-expr. Empty query does not list all graphs.
        </Text>
        <TextInput
          label="Search"
          placeholder="Id, UUID prefix, or annotation text…"
          value={q}
          onChange={(e) => setQ(e.currentTarget.value)}
          autoFocus
        />
        <div>
          <UnstyledButton onClick={() => setExprOpen((o) => !o)} aria-expanded={exprOpen}>
            <Group gap={4}>
              {exprOpen ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />}
              <Text size="sm">Expression (graph-expr)</Text>
            </Group>
          </UnstyledButton>
          <Collapse in={exprOpen}>
            <Textarea
              mt="xs"
              minRows={2}
              placeholder={"e.g. a.env == 'prod'"}
              value={expr}
              onChange={(e) => setExpr(e.currentTarget.value)}
              autosize
            />
          </Collapse>
        </div>
        {!hasCriteria && (
          <Text size="sm" c="dimmed">
            Type to search…
          </Text>
        )}
        {hasCriteria && searching && (
          <Text size="sm" c="dimmed">
            Searching…
          </Text>
        )}
        {hasCriteria && !searching && items.length === 0 && !error && (
          <Text size="sm" c="dimmed">
            No matching graphs.
          </Text>
        )}
        {items.length > 0 && (
          <Table striped highlightOnHover withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Id</Table.Th>
                <Table.Th>Annotations</Table.Th>
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
                    <Button size="xs" variant="light" loading={busy} onClick={() => void onOpenRow(item.id)}>
                      Open
                    </Button>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        )}
      </Stack>
    </Modal>
  )
}
