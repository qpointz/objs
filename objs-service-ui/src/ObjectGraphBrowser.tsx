import { useEffect, useMemo, useState } from 'react'
import { ActionIcon, Box, Group, ScrollArea, Stack, Text, TextInput } from '@mantine/core'
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
  onOpenAsContext: (graphId: string) => void
  onOpenInExplorer: (graphId: string) => void
  onEditInComposer: (graphId: string) => void
  onClose: () => void
}

/**
 * Graphs list pane (Note1): search + rows only. No pane-level graph id/⋮, no row selection —
 * left viewer stays on the object; per-row ⋮ for Open / Open in Explorer / Edit.
 */
export function ObjectGraphBrowser({
  rows,
  loading,
  error,
  onOpenAsContext,
  onOpenInExplorer,
  onEditInComposer,
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
          {filtered.map((header) => (
            <Box
              key={header.id}
              data-tour="object-graph-browser-row"
              style={{
                display: 'block',
                width: '100%',
                padding: '6px 8px',
                borderRadius: 4,
              }}
            >
              <ObjectGraphRowContent
                header={header}
                density="browser"
                onOpenAsContext={onOpenAsContext}
                onOpenInExplorer={onOpenInExplorer}
                onEditInComposer={onEditInComposer}
              />
            </Box>
          ))}
        </Stack>
      </ScrollArea>
    </Stack>
  )
}
