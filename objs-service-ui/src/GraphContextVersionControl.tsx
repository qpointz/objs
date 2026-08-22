import { useEffect, useMemo, useRef, useState } from 'react'
import {
  Badge,
  Button,
  Group,
  Menu,
  ScrollArea,
  Text,
  TextInput,
} from '@mantine/core'
import { listGraphVersions } from './api'
import { AnnotationSplitPill } from './EntityCardNode'
import { useGraphContext } from './GraphContextProvider'
import { formatVersionIdForList } from './objectVersionRows'
import {
  filterGraphVersionsByTime,
  graphVersionPageCount,
  GRAPH_VERSION_PAGE_SIZE,
  pageGraphVersions,
  parseGraphVersionTime,
} from './graphContextVersions'
import type { BoMGraphVersionSummary } from './types'

function parseDatetimeLocal(raw: string): Date | null {
  const t = raw.trim()
  if (!t) return null
  const d = new Date(t)
  return Number.isNaN(d.getTime()) ? null : d
}

export function nonEmptyAnnotations(
  raw: Record<string, string> | undefined,
): Record<string, string> {
  return Object.fromEntries(
    Object.entries(raw ?? {}).filter(([k, v]) => k.trim().length > 0 && v.trim().length > 0),
  )
}

/**
 * Light graph-version dropdown for shared graph-context bar (Note 2 / G-UX-gver).
 * Graph mode only — caller should not render otherwise.
 * Version annotation pills are rendered by the parent bar next to this control.
 */
export function GraphContextVersionControl() {
  const { context, setGraphVersion } = useGraphContext()
  const graphId = context.kind === 'graph' ? context.graphId : null
  const pinned = context.kind === 'graph' ? context.graphVersion : null
  const pinnedAnn =
    context.kind === 'graph' ? nonEmptyAnnotations(context.graphVersionAnnotations) : {}

  const [opened, setOpened] = useState(false)
  const [rows, setRows] = useState<BoMGraphVersionSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [pageIndex, setPageIndex] = useState(0)
  const [fromRaw, setFromRaw] = useState('')
  const [toRaw, setToRaw] = useState('')
  const hydratedPinRef = useRef<string | null>(null)

  useEffect(() => {
    if (!graphId || pinned == null) {
      hydratedPinRef.current = null
      return
    }
    const key = `${graphId}@${pinned}`
    if (hydratedPinRef.current === key) return
    if (Object.keys(pinnedAnn).length > 0 && context.graphVersionCreatedAt) {
      hydratedPinRef.current = key
      return
    }
    let cancelled = false
    listGraphVersions(graphId)
      .then((list) => {
        if (cancelled) return
        hydratedPinRef.current = key
        const row = list.find((r) => r.version === pinned)
        if (row) {
          setGraphVersion(
            row.version,
            nonEmptyAnnotations(row.annotations),
            row.createdAt ?? null,
          )
        }
      })
      .catch(() => {
        if (!cancelled) hydratedPinRef.current = key
      })
    return () => {
      cancelled = true
    }
  }, [graphId, pinned, pinnedAnn, context.graphVersionCreatedAt, setGraphVersion])

  useEffect(() => {
    if (!opened || !graphId) return
    let cancelled = false
    setLoading(true)
    listGraphVersions(graphId)
      .then((list) => {
        if (!cancelled) setRows(list)
      })
      .catch(() => {
        if (!cancelled) setRows([])
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [opened, graphId])

  useEffect(() => {
    setPageIndex(0)
  }, [graphId, fromRaw, toRaw])

  const filtered = useMemo(
    () =>
      filterGraphVersionsByTime(rows, {
        from: parseDatetimeLocal(fromRaw),
        to: parseDatetimeLocal(toRaw),
      }),
    [rows, fromRaw, toRaw],
  )
  const pageCount = graphVersionPageCount(filtered.length)
  const safePage = Math.min(pageIndex, pageCount - 1)
  const pageRows = pageGraphVersions(filtered, safePage)

  const label = pinned == null ? 'Latest' : String(pinned)

  return (
    <Menu
      shadow="md"
      width={460}
      position="bottom-start"
      withinPortal
      opened={opened}
      onChange={setOpened}
    >
      <Menu.Target>
        <Button
          size="compact-xs"
          variant="light"
          data-tour="graph-context-version"
          title="Pin graph version for Explorer, Objects, and Query"
          style={{ flexShrink: 0 }}
        >
          {label} ▾
        </Button>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>Graph versions</Menu.Label>
        <Menu.Item disabled={pinned == null} onClick={() => setGraphVersion(null)}>
          <Group gap={6} wrap="nowrap" align="center">
            <Text size="xs">Latest</Text>
            {pinned == null && (
              <Badge size="xs" variant="light" color="blue">
                current
              </Badge>
            )}
          </Group>
        </Menu.Item>
        <Group gap={6} px="sm" py={6} wrap="nowrap" grow>
          <TextInput
            size="xs"
            type="datetime-local"
            label="From"
            value={fromRaw}
            onChange={(e) => setFromRaw(e.currentTarget.value)}
            styles={{
              label: { fontSize: 11 },
              input: { minWidth: 200, fontVariantNumeric: 'tabular-nums' },
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
              input: { minWidth: 200, fontVariantNumeric: 'tabular-nums' },
            }}
          />
        </Group>
        <ScrollArea.Autosize mah={300}>
          {loading && (
            <Text size="xs" c="dimmed" px="sm" py="xs">
              Loading…
            </Text>
          )}
          {!loading && pageRows.length === 0 && (
            <Text size="xs" c="dimmed" px="sm" py="xs">
              {rows.length === 0 ? 'No versions yet' : 'No versions in range'}
            </Text>
          )}
          {pageRows.map((row) => {
            const current = pinned === row.version
            const created = parseGraphVersionTime(row.createdAt, row.version)
            const annotations = nonEmptyAnnotations(row.annotations)
            const annEntries = Object.entries(annotations)
            return (
              <Menu.Item
                key={row.version}
                disabled={current}
                onClick={() => setGraphVersion(row.version, annotations, row.createdAt ?? null)}
              >
                <Group gap="sm" wrap="nowrap" justify="space-between" align="center">
                  <Group gap={6} wrap="nowrap" align="center" style={{ flexShrink: 0 }}>
                    <Text size="xs" ff="monospace" lh={1.3} title={String(row.version)}>
                      {formatVersionIdForList(row.version)}
                    </Text>
                    {current && (
                      <Badge size="xs" variant="light" color="blue">
                        current
                      </Badge>
                    )}
                  </Group>
                  {created && (
                    <Text size="xs" c="dimmed" style={{ whiteSpace: 'nowrap' }}>
                      {created.toLocaleDateString()} {created.toLocaleTimeString()}
                    </Text>
                  )}
                </Group>
                {annEntries.length > 0 && (
                  <Group gap={3} wrap="wrap" mt={4}>
                    {annEntries.map(([k, v]) => (
                      <AnnotationSplitPill key={k} k={k} v={v} size="bar" />
                    ))}
                  </Group>
                )}
              </Menu.Item>
            )
          })}
        </ScrollArea.Autosize>
        {filtered.length > GRAPH_VERSION_PAGE_SIZE && (
          <Group justify="space-between" px="sm" py={6}>
            <Button
              size="compact-xs"
              variant="subtle"
              disabled={safePage <= 0}
              onClick={() => setPageIndex((p) => Math.max(0, p - 1))}
            >
              Newer
            </Button>
            <Text size="xs" c="dimmed">
              {safePage + 1} / {pageCount}
            </Text>
            <Button
              size="compact-xs"
              variant="subtle"
              disabled={safePage >= pageCount - 1}
              onClick={() => setPageIndex((p) => Math.min(pageCount - 1, p + 1))}
            >
              Older
            </Button>
          </Group>
        )}
      </Menu.Dropdown>
    </Menu>
  )
}

function datetimeLocalValue(d: Date | null): string {
  if (d == null || Number.isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** Exported for tests / datetime helpers reuse. */
export { datetimeLocalValue, parseDatetimeLocal }
