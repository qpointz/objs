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
import { IconX } from '@tabler/icons-react'
import { ObjectVersionRowContent } from './ObjectVersionRowContent'
import { filterGraphVersionsByTime } from './graphContextVersions'
import { OBJECT_VERSION_BROWSER_DEFAULT } from './objectViewerTitle'
import type { ObjectVersionRow } from './objectVersionRows'

function parseDatetimeLocal(raw: string): Date | null {
  const t = raw.trim()
  if (!t) return null
  const d = new Date(t)
  return Number.isNaN(d.getTime()) ? null : d
}

function matchesQuery(row: ObjectVersionRow, q: string): boolean {
  if (!q) return true
  if (row.version == null) {
    if ('latest'.startsWith(q) || q === 'head') return true
  } else if (String(row.version).includes(q)) {
    return true
  }
  return Object.entries(row.annotations ?? {}).some(
    ([k, v]) => k.toLowerCase().includes(q) || v.toLowerCase().includes(q),
  )
}

type Props = {
  title?: string
  rows: ObjectVersionRow[]
  loading?: boolean
  error?: string | null
  /**
   * Selected deep version, or null for Latest.
   * `undefined` = nothing selected yet (no highlight).
   */
  selectedVersion?: number | null
  onSelect: (version: number | null) => void
  onClose: () => void
}

/**
 * Inline version list (right of object viewer) — date → annotations → version (G-UX-over).
 */
export function ObjectVersionBrowser({
  title = 'Versions',
  rows,
  loading,
  error,
  selectedVersion,
  onSelect,
  onClose,
}: Props) {
  const [fromRaw, setFromRaw] = useState('')
  const [toRaw, setToRaw] = useState('')
  const [query, setQuery] = useState('')

  useEffect(() => {
    setFromRaw('')
    setToRaw('')
    setQuery('')
  }, [rows])

  const filtered = useMemo(() => {
    const latest = rows.filter((r) => r.version == null)
    const deep = rows.filter((r) => r.version != null)
    const timed = filterGraphVersionsByTime(
      deep.map((r) => ({
        graphId: r.id,
        version: r.version as number,
        createdAt: r.createdAt,
        annotations: r.annotations ?? {},
      })),
      {
        from: parseDatetimeLocal(fromRaw),
        to: parseDatetimeLocal(toRaw),
      },
    ).map(
      (r): ObjectVersionRow => ({
        id: r.graphId,
        version: r.version,
        createdAt: r.createdAt,
        annotations: r.annotations,
      }),
    )
    const q = query.trim().toLowerCase()
    const latestShown = latest.filter((r) => matchesQuery(r, q))
    const deepShown = timed.filter((r) => matchesQuery(r, q))
    const noFilter = !fromRaw.trim() && !toRaw.trim() && !q
    if (noFilter) {
      return [...latestShown, ...deepShown.slice(0, OBJECT_VERSION_BROWSER_DEFAULT)]
    }
    return [...latestShown, ...deepShown]
  }, [rows, fromRaw, toRaw, query])

  return (
    <Stack gap="xs" h="100%" style={{ minWidth: 0 }} data-tour="object-version-browser">
      <Group justify="space-between" wrap="nowrap" align="center">
        <Text size="sm" fw={600}>
          {title}
        </Text>
        <ActionIcon size="sm" variant="subtle" aria-label="Close versions" onClick={onClose}>
          <IconX size={14} />
        </ActionIcon>
      </Group>
      <TextInput
        size="xs"
        placeholder="Filter"
        value={query}
        onChange={(e) => setQuery(e.currentTarget.value)}
      />
      <Group gap={6} wrap="nowrap" grow>
        <TextInput
          size="xs"
          type="datetime-local"
          label="From"
          value={fromRaw}
          onChange={(e) => setFromRaw(e.currentTarget.value)}
          styles={{
            label: { fontSize: 11 },
            input: { fontVariantNumeric: 'tabular-nums' },
          }}
        />
        <TextInput
          size="xs"
          type="datetime-local"
          label="To"
          value={toRaw}
          onChange={(e) => setToRaw(e.currentTarget.value)}
          styles={{
            label: { fontSize: 11 },
            input: { fontVariantNumeric: 'tabular-nums' },
          }}
        />
      </Group>
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
              {rows.length === 0 ? 'No versions yet' : 'No versions in range'}
            </Text>
          )}
          {filtered.map((row) => {
            const current =
              selectedVersion !== undefined &&
              (row.version == null
                ? selectedVersion === null
                : selectedVersion === row.version)
            return (
              <UnstyledButton
                key={row.version == null ? 'latest' : String(row.version)}
                onClick={() => onSelect(row.version)}
                style={{
                  display: 'block',
                  width: '100%',
                  padding: '6px 8px',
                  borderRadius: 4,
                  background: current ? 'var(--mantine-color-blue-light)' : undefined,
                }}
              >
                <ObjectVersionRowContent row={row} selected={current} density="browser" />
              </UnstyledButton>
            )
          })}
        </Stack>
      </ScrollArea>
    </Stack>
  )
}
