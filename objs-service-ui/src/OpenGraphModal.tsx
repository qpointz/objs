import { useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Group,
  Loader,
  Modal,
  Stack,
  Tabs,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { IconAffiliate, IconCode, IconSearch } from '@tabler/icons-react'
import { getGraph, listRecentGraphHeaders, searchGraphs } from './api'
import { GRAPH_HEADER_COMPACT_ROW_HEIGHT, GraphHeaderReadout } from './GraphHeaderReadout'
import type { BoMGraphHeader, BoMGraphResponse } from './types'

type Props = {
  opened: boolean
  onClose: () => void
  /** Called with the opened graph's id + resolved header/members. */
  onOpen: (graphId: string, resolved: BoMGraphResponse) => void
}

type SearchTab = 'search' | 'expression'

const SEARCH_LIMIT = 15
const DEBOUNCE_MS = 250
const HIT_GAP = 6
const HIT_PAD = 6
/** Tall enough for all SEARCH_LIMIT compact rows without scrolling. */
const RESULTS_PANE_HEIGHT =
  SEARCH_LIMIT * GRAPH_HEADER_COMPACT_ROW_HEIGHT + (SEARCH_LIMIT - 1) * HIT_GAP + HIT_PAD * 2

/** Shared "Open graph…" dialog: recent when empty; search / graph-expr when filled (Note2). */
export function OpenGraphModal({ opened, onClose, onOpen }: Props) {
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [searching, setSearching] = useState(false)
  const [tab, setTab] = useState<SearchTab>('search')
  const [q, setQ] = useState('')
  const [expr, setExpr] = useState('')
  const [items, setItems] = useState<BoMGraphHeader[]>([])
  const [debouncedQ] = useDebouncedValue(q, DEBOUNCE_MS)
  const [debouncedExpr] = useDebouncedValue(expr, DEBOUNCE_MS)

  useEffect(() => {
    if (!opened) return
    setError(null)
    setTab('search')
    setQ('')
    setExpr('')
    setItems([])
  }, [opened])

  useEffect(() => {
    if (!opened) return
    const trimmedQ = tab === 'search' ? debouncedQ.trim() : ''
    const trimmedExpr = tab === 'expression' ? debouncedExpr.trim() : ''
    let cancelled = false
    setSearching(true)
    setError(null)

    const load =
      !trimmedQ && !trimmedExpr
        ? listRecentGraphHeaders(SEARCH_LIMIT)
        : searchGraphs({
            q: trimmedQ || undefined,
            expr: trimmedExpr || undefined,
            limit: SEARCH_LIMIT,
          }).then((res) => res.items)

    void load
      .then((rows) => {
        if (!cancelled) setItems(rows)
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
  }, [opened, tab, debouncedQ, debouncedExpr])

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

  const hasFilter =
    (tab === 'search' && q.trim().length > 0) ||
    (tab === 'expression' && expr.trim().length > 0)

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      size="960px"
      radius="md"
      padding="md"
      title={
        <Group gap="xs" wrap="nowrap">
          <Box
            aria-hidden
            style={{
              width: 28,
              height: 28,
              borderRadius: 8,
              display: 'grid',
              placeItems: 'center',
              background:
                'color-mix(in srgb, var(--mantine-color-blue-filled) 16%, var(--mantine-color-body))',
              color: 'var(--mantine-color-blue-filled)',
              border:
                '1px solid color-mix(in srgb, var(--mantine-color-blue-filled) 28%, transparent)',
            }}
          >
            <IconAffiliate size={16} stroke={1.6} />
          </Box>
          <Stack gap={0}>
            <Title order={5} style={{ lineHeight: 1.2 }}>
              Open graph
            </Title>
            <Text size="xs" c="dimmed">
              Recent graphs when empty; search or graph-expr to filter
            </Text>
          </Stack>
        </Group>
      }
    >
      <Stack gap="sm">
        {error && (
          <Alert color="red" title="Error" py="xs">
            {error}
          </Alert>
        )}

        <Tabs
          value={tab}
          onChange={(v) => setTab((v as SearchTab) ?? 'search')}
          variant="pills"
          radius="md"
        >
          <Tabs.List grow mb="xs">
            <Tabs.Tab value="search" leftSection={<IconSearch size={14} />}>
              Search
            </Tabs.Tab>
            <Tabs.Tab value="expression" leftSection={<IconCode size={14} />}>
              Expression
            </Tabs.Tab>
          </Tabs.List>

          <Tabs.Panel value="search">
            <TextInput
              label="Search"
              description="Graph id, UUID prefix, or annotation text"
              placeholder="prod, a1b2c3d4, env…"
              value={q}
              onChange={(e) => setQ(e.currentTarget.value)}
              autoFocus
              leftSection={<IconSearch size={14} />}
              size="sm"
              radius="md"
            />
          </Tabs.Panel>

          <Tabs.Panel value="expression">
            <Textarea
              label="graph-expr"
              description="JEXL over graph header id and a.* annotations"
              placeholder={"a.env == 'prod' && id != null"}
              value={expr}
              onChange={(e) => setExpr(e.currentTarget.value)}
              minRows={2}
              maxRows={4}
              autosize
              radius="md"
              size="sm"
              styles={{
                input: {
                  fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
                  fontSize: 12,
                },
              }}
            />
          </Tabs.Panel>
        </Tabs>

        <Box
          style={{
            borderRadius: 10,
            border: '1px solid var(--mantine-color-default-border)',
            background:
              'color-mix(in srgb, var(--mantine-color-default-hover) 55%, var(--mantine-color-body))',
            height: RESULTS_PANE_HEIGHT,
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          {searching && (
            <Stack align="center" justify="center" gap="xs" style={{ flex: 1 }}>
              <Loader size="sm" />
              <Text size="sm" c="dimmed">
                {hasFilter ? 'Searching…' : 'Loading recent graphs…'}
              </Text>
            </Stack>
          )}

          {!searching && items.length === 0 && !error && (
            <Stack align="center" justify="center" gap={4} style={{ flex: 1 }} px="md">
              <Text size="sm" c="dimmed">
                {hasFilter ? 'No matching graphs.' : 'No graphs yet.'}
              </Text>
              {hasFilter && (
                <Text size="xs" c="dimmed">
                  Try a shorter UUID prefix or a different annotation fragment.
                </Text>
              )}
            </Stack>
          )}

          {!searching && items.length > 0 && (
            <Stack
              gap={HIT_GAP}
              p={HIT_PAD}
              style={{
                flex: 1,
                minHeight: 0,
                overflowY: 'auto',
                overflowX: 'hidden',
              }}
            >
              {items.map((item) => (
                <GraphHeaderReadout
                  key={item.id}
                  graphId={item.id}
                  annotations={item.annotations}
                  compactId
                  density="compact"
                  interactive
                  onClick={() => {
                    if (!busy) void onOpenRow(item.id)
                  }}
                />
              ))}
            </Stack>
          )}
        </Box>
      </Stack>
    </Modal>
  )
}
