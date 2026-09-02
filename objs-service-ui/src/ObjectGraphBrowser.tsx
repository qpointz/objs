import { useEffect, useMemo, useState } from 'react'
import {
  ActionIcon,
  Group,
  ScrollArea,
  Stack,
  Text,
  TextInput,
  UnstyledButton,
} from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { IconX } from '@tabler/icons-react'
import { ObjectGraphRowContent } from './ObjectGraphRowContent'
import { OBJECT_VERSION_BROWSER_DEFAULT } from './objectViewerTitle'
import type { BoMGraphHeader } from './types'

const DEBOUNCE_MS = 250

function matchesGraphSearch(header: BoMGraphHeader, q: string): boolean {
  if (!q) return true
  const idStr = header.id
  if (idStr.toLowerCase().includes(q) || idStr.toLowerCase().startsWith(q)) {
    return true
  }
  return Object.entries(header.annotations ?? {}).some(
    ([k, v]) => k.toLowerCase().includes(q) || v.toLowerCase().includes(q),
  )
}

type Props = {
  rows: BoMGraphHeader[]
  loading?: boolean
  error?: string | null
  /** Current shared-context graph id, if any. */
  selectedGraphId?: string | null
  onSelect: (graphId: string) => void
  onClose: () => void
}

/**
 * Inline live-graphs list (right of object viewer) — search like Open graph, no date range.
 */
export function ObjectGraphBrowser({
  rows,
  loading,
  error,
  selectedGraphId,
  onSelect,
  onClose,
}: Props) {
  const [query, setQuery] = useState('')
  const [debouncedQ] = useDebouncedValue(query, DEBOUNCE_MS)

  useEffect(() => {
    setQuery('')
  }, [rows])

  const filtered = useMemo(() => {
    const q = debouncedQ.trim().toLowerCase()
    const matched = rows.filter((r) => matchesGraphSearch(r, q))
    if (!q) {
      return matched.slice(0, OBJECT_VERSION_BROWSER_DEFAULT)
    }
    return matched
  }, [rows, debouncedQ])

  return (
    <Stack gap="xs" h="100%" style={{ minWidth: 0 }} data-tour="object-graph-browser">
      <Group justify="space-between" wrap="nowrap" align="center">
        <Text size="sm" fw={600}>
          Graphs
        </Text>
        <ActionIcon size="sm" variant="subtle" aria-label="Close graphs" onClick={onClose}>
          <IconX size={14} />
        </ActionIcon>
      </Group>
      <TextInput
        size="xs"
        placeholder="Search"
        value={query}
        onChange={(e) => setQuery(e.currentTarget.value)}
        data-tour="object-graph-browser-search"
      />
      <ScrollArea style={{ flex: 1, minHeight: 0 }} offsetScrollbars type="auto">
        <Stack gap={4} pb="xs">
          {error && (
            <Text size="xs" c="red">
              {error}
            </Text>
          )}
          {loading && (
            <Text size="xs" c="dimmed">
              Loading…
            </Text>
          )}
          {!loading && filtered.length === 0 && (
            <Text size="xs" c="dimmed">
              {rows.length === 0 ? 'No graphs' : 'No matching graphs'}
            </Text>
          )}
          {filtered.map((header) => {
            const current = selectedGraphId != null && selectedGraphId === header.id
            return (
              <UnstyledButton
                key={header.id}
                onClick={() => onSelect(header.id)}
                data-tour="object-graph-browser-row"
                style={{
                  display: 'block',
                  width: '100%',
                  padding: '6px 8px',
                  borderRadius: 4,
                  background: current ? 'var(--mantine-color-blue-light)' : undefined,
                }}
              >
                <ObjectGraphRowContent header={header} selected={current} density="browser" />
              </UnstyledButton>
            )
          })}
        </Stack>
      </ScrollArea>
    </Stack>
  )
}
