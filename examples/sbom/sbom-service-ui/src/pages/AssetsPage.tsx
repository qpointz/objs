import {
  ActionIcon,
  Alert,
  Anchor,
  Box,
  Button,
  Group,
  Pagination,
  Paper,
  ScrollArea,
  Select,
  Skeleton,
  Stack,
  Table,
  Tabs,
  Text,
  TextInput,
  Textarea,
  Tooltip,
  UnstyledButton,
} from '@mantine/core'
import { IconCopy, IconChevronDown, IconRefresh, IconSearch } from '@tabler/icons-react'
import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent, type PointerEvent as ReactPointerEvent, Fragment } from 'react'
import { Link, Outlet, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import { SearchInput } from '../SearchInput'
import { ObjectTree } from '../ObjectTree'
import type { AssetTypeDetail, AssetTypeStatistics, AssetTypeSummary, AssetView } from '../api/types'

const PAGE_SIZE_OPTIONS = ['10', '20', '50']
const DEFAULT_PAGE_SIZE = 20
const ALL_TYPES = ''
const DEFAULT_RIGHT_PANE = 280
const MIN_RIGHT_PANE = 200
const MIN_CONTENT_PANE = 280
const SPLITTER_WIDTH = 8

function payloadFieldValue(payload: Record<string, unknown>, path: string): string {
  const parts = path.split('.')
  let cur: unknown = payload
  for (const part of parts) {
    if (cur == null || typeof cur !== 'object' || Array.isArray(cur)) return '—'
    cur = (cur as Record<string, unknown>)[part]
  }
  if (cur == null || cur === '') return '—'
  if (typeof cur === 'object') return JSON.stringify(cur)
  return String(cur)
}

function formatExecDuration(ms: number): string {
  if (ms >= 1000) {
    const seconds = ms / 1000
    return `${seconds >= 10 ? seconds.toFixed(1) : seconds.toFixed(2)}s`
  }
  return `${Math.round(ms)}ms`
}

type AssetsWorkspace = {
  filters: Record<string, string>
  objExpr: string
  results: AssetView[]
  dupesNote: string | null
  page: number
  pageSize: number
  exec: { records: number; durationMs: number } | null
  detail: AssetTypeDetail | null
}

const workspaceByType = new Map<string, AssetsWorkspace>()
let lastTypeFilter = ''
let lastRightWidth = DEFAULT_RIGHT_PANE

function emptyWorkspace(): AssetsWorkspace {
  return {
    filters: {},
    objExpr: '',
    results: [],
    dupesNote: null,
    page: 1,
    pageSize: DEFAULT_PAGE_SIZE,
    exec: null,
    detail: null,
  }
}

export function AssetsPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { assetId } = useParams()
  const [types, setTypes] = useState<AssetTypeSummary[]>([])
  const [typeFilter, setTypeFilter] = useState(lastTypeFilter)
  const [statsByType, setStatsByType] = useState<Record<string, AssetTypeStatistics>>({})
  const [statsPending, setStatsPending] = useState<Set<string>>(new Set())
  const [typesBusy, setTypesBusy] = useState(false)
  const selectedType = searchParams.get('type') ?? ALL_TYPES
  const listQuery = searchParams.toString()
  const assetTo = (id: string) =>
    listQuery ? `/applications/assets/${id}?${listQuery}` : `/applications/assets/${id}`
  const schemaLatestTo = (type: string) => `/schemas/${encodeURIComponent(type)}`
  const schemaVersionTo = (type: string, version: string) =>
    `/schemas/${encodeURIComponent(type)}/${encodeURIComponent(version)}`
  const restored = workspaceByType.get(selectedType)

  function selectType(type: string) {
    const search = type ? `?type=${encodeURIComponent(type)}` : ''
    navigate(`/applications/assets${search}`)
  }
  const [detail, setDetail] = useState<AssetTypeDetail | null>(restored?.detail ?? null)
  const [filters, setFilters] = useState<Record<string, string>>(restored?.filters ?? {})
  const [objExpr, setObjExpr] = useState(restored?.objExpr ?? '')
  const [searchTab, setSearchTab] = useState<'form' | 'expr'>(selectedType === ALL_TYPES ? 'expr' : 'form')
  const [results, setResults] = useState<AssetView[]>(restored?.results ?? [])
  const [dupesNote, setDupesNote] = useState<string | null>(restored?.dupesNote ?? null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [page, setPage] = useState(restored?.page ?? 1)
  const [pageSize, setPageSize] = useState(restored?.pageSize ?? DEFAULT_PAGE_SIZE)
  const [exec, setExec] = useState<{ records: number; durationMs: number } | null>(restored?.exec ?? null)
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set())
  const [rightWidth, setRightWidth] = useState(lastRightWidth)
  const splitHostRef = useRef<HTMLDivElement | null>(null)
  const dragRef = useRef<{ startX: number; startWidth: number } | null>(null)
  const typeRef = useRef(selectedType)
  const snapshotRef = useRef<AssetsWorkspace>(emptyWorkspace())

  const allTypes = selectedType === ALL_TYPES
  const activeSearchTab = allTypes ? 'expr' : searchTab

  useEffect(() => {
    if (allTypes) setSearchTab('expr')
  }, [allTypes])

  snapshotRef.current = {
    filters,
    objExpr,
    results,
    dupesNote,
    page,
    pageSize,
    exec,
    detail,
  }

  useEffect(() => {
    lastTypeFilter = typeFilter
  }, [typeFilter])

  useEffect(() => {
    lastRightWidth = rightWidth
  }, [rightWidth])

  async function loadTypes() {
    setTypesBusy(true)
    try {
      setTypes(await api.listAssetTypes())
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not load asset types')
    } finally {
      setTypesBusy(false)
    }
  }

  useEffect(() => {
    void loadTypes()
  }, [])

  useEffect(() => {
    if (types.length === 0) {
      setStatsByType({})
      setStatsPending(new Set())
      return
    }
    let cancelled = false
    const pending = new Set(types.map((t) => t.type))
    setStatsByType({})
    setStatsPending(pending)
    void (async () => {
      for (const t of types) {
        if (cancelled) return
        try {
          const stats = await api.getAssetTypeStatistics(t.type)
          if (cancelled) return
          setStatsByType((prev) => ({ ...prev, [t.type]: stats }))
        } catch {
          if (cancelled) return
        } finally {
          if (!cancelled) {
            setStatsPending((prev) => {
              const next = new Set(prev)
              next.delete(t.type)
              return next
            })
          }
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [types])

  useEffect(() => {
    if (typeRef.current !== selectedType) return
    workspaceByType.set(selectedType, snapshotRef.current)
    return () => {
      workspaceByType.set(typeRef.current, snapshotRef.current)
    }
  })

  useEffect(() => {
    const prev = typeRef.current
    if (prev === selectedType) return
    workspaceByType.set(prev, snapshotRef.current)
    typeRef.current = selectedType
    const cached = workspaceByType.get(selectedType)
    if (cached) {
      setDetail(cached.detail)
      setFilters(cached.filters)
      setObjExpr(cached.objExpr)
      setResults(cached.results)
      setDupesNote(cached.dupesNote)
      setPage(cached.page)
      setPageSize(cached.pageSize)
      setExec(cached.exec)
      return
    }
    setFilters({})
    setDupesNote(null)
    setResults([])
    setExec(null)
    setPage(1)
  }, [selectedType])

  useEffect(() => {
    if (assetId) return
    const cached = workspaceByType.get(selectedType)
    if (cached?.exec != null) return
    void runList()
  }, [selectedType, assetId])

  useEffect(() => {
    if (allTypes) {
      setDetail(null)
      return
    }
    let cancelled = false
    void (async () => {
      try {
        const next = await api.getAssetType(selectedType)
        if (!cancelled) setDetail(next)
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Could not load type form')
      }
    })()
    return () => {
      cancelled = true
    }
  }, [selectedType, allTypes])

  const visibleTypes = useMemo(() => {
    const q = typeFilter.trim().toLowerCase()
    if (!q) return types
    return types.filter((t) => `${t.title} ${t.type}`.toLowerCase().includes(q))
  }, [types, typeFilter])

  const allObjectCount = useMemo(() => {
    if (types.length === 0) return null
    if (types.some((t) => statsPending.has(t.type) || statsByType[t.type] == null)) return null
    return types.reduce((sum, t) => sum + (statsByType[t.type]?.objectCount ?? 0), 0)
  }, [types, statsByType, statsPending])

  async function timedSearch(fn: () => Promise<AssetView[]>, note: string | null = null) {
    setBusy(true)
    setError(null)
    setDupesNote(note)
    try {
      const started = performance.now()
      const rows = await fn()
      setExec({ records: rows.length, durationMs: performance.now() - started })
      setResults(rows)
      setPage(1)
    } catch (err) {
      setExec(null)
      setResults([])
      setError(err instanceof Error ? err.message : 'Search failed')
    } finally {
      setBusy(false)
    }
  }

  async function runList() {
    await timedSearch(() =>
      allTypes ? api.searchAssets({}) : api.searchAssets({ type: selectedType }),
    )
  }

  async function onSearch(e: FormEvent) {
    e.preventDefault()
    if (activeSearchTab === 'expr') {
      await timedSearch(() =>
        allTypes
          ? api.searchAssets({ objExpr: objExpr.trim() || undefined })
          : api.searchAssets({ type: selectedType, objExpr: objExpr.trim() || undefined }),
      )
      return
    }
    const cleaned = Object.fromEntries(Object.entries(filters).filter(([, v]) => v.trim().length > 0))
    await timedSearch(() => api.searchAssets({ type: selectedType, filters: cleaned }))
  }

  async function onExec() {
    await timedSearch(() =>
      allTypes
        ? api.searchAssets({ objExpr: objExpr.trim() || undefined })
        : api.searchAssets({ type: selectedType, objExpr: objExpr.trim() || undefined }),
    )
  }

  async function findDupes() {
    if (allTypes) return
    await findDupesForType(selectedType)
  }

  async function findDupesForType(type: string, assetIdFilter?: string) {
    setBusy(true)
    setError(null)
    try {
      const started = performance.now()
      const groups = await api.findDuplicates(type)
      const matching =
        assetIdFilter == null ? groups : groups.filter((g) => g.assets.some((a) => a.id === assetIdFilter))
      const seen = new Set<string>()
      const rows: AssetView[] = []
      for (const g of matching) {
        for (const a of g.assets) {
          if (seen.has(a.id)) continue
          seen.add(a.id)
          rows.push(a)
        }
      }
      if (assetIdFilter != null && rows.length === 0) {
        const self = results.find((a) => a.id === assetIdFilter)
        if (self) rows.push(self)
      }
      setExec({ records: rows.length, durationMs: performance.now() - started })
      setResults(rows)
      setPage(1)
      setExpandedIds(new Set())
      if (assetIdFilter != null) {
        setDupesNote(
          rows.length <= 1
            ? 'No duplicates for this object.'
            : `${rows.length} duplicate${rows.length === 1 ? '' : 's'} of this object`,
        )
      } else {
        setDupesNote(
          matching.length === 0
            ? 'No duplicate groups for this type.'
            : `${matching.length} duplicate group${matching.length === 1 ? '' : 's'}`,
        )
      }
    } catch (err) {
      setExec(null)
      setError(err instanceof Error ? err.message : 'Could not find duplicates')
    } finally {
      setBusy(false)
    }
  }

  function toggleExpanded(id: string) {
    setExpandedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const pageCount = Math.max(1, Math.ceil(results.length / pageSize))
  const safePage = Math.min(page, pageCount)
  const pageRows = results.slice((safePage - 1) * pageSize, safePage * pageSize)
  const selectedTypeMeta = types.find((t) => t.type === selectedType)
  const selectedTitle = allTypes ? 'All types' : (selectedTypeMeta?.title ?? selectedType)
  const selectedDescription = allTypes
    ? null
    : (detail?.description || selectedTypeMeta?.description || '').trim() || null
  const extraColumns =
    allTypes || detail == null
      ? []
      : detail.searchableFields.length > 0
        ? detail.searchableFields
        : (detail.firstLevelScalarFields ?? [])
  const tableColSpan = 4 + extraColumns.length

  const onSplitterPointerDown = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      e.preventDefault()
      dragRef.current = { startX: e.clientX, startWidth: rightWidth }
      e.currentTarget.setPointerCapture(e.pointerId)
    },
    [rightWidth],
  )

  const onSplitterPointerMove = useCallback((e: ReactPointerEvent<HTMLDivElement>) => {
    const drag = dragRef.current
    if (drag == null) return
    const host = splitHostRef.current
    if (host == null) return
    const maxRight = Math.max(MIN_RIGHT_PANE, host.clientWidth - MIN_CONTENT_PANE - SPLITTER_WIDTH)
    const next = Math.min(maxRight, Math.max(MIN_RIGHT_PANE, drag.startWidth - (e.clientX - drag.startX)))
    setRightWidth(next)
  }, [])

  const onSplitterPointerUp = useCallback((e: ReactPointerEvent<HTMLDivElement>) => {
    dragRef.current = null
    if (e.currentTarget.hasPointerCapture(e.pointerId)) {
      e.currentTarget.releasePointerCapture(e.pointerId)
    }
  }, [])

  return (
    <>
    <div
      style={{
        flex: 1,
        minHeight: 0,
        height: '100%',
        display: 'flex',
        gap: 'var(--mantine-spacing-sm)',
        overflow: 'hidden',
      }}
    >
      <Paper
        withBorder
        p="sm"
        style={{
          flex: '0 0 240px',
          width: 240,
          minHeight: 0,
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Group justify="space-between" mb="xs" wrap="nowrap">
          <Text size="sm" fw={650}>
            Asset types
          </Text>
          <Tooltip label="Refresh types and statistics" withArrow>
            <ActionIcon
              variant="subtle"
              size="sm"
              aria-label="Refresh types and statistics"
              loading={typesBusy}
              onClick={() => void loadTypes()}
            >
              <IconRefresh size={14} />
            </ActionIcon>
          </Tooltip>
        </Group>
        <SearchInput
          size="xs"
          placeholder="Filter types"
          value={typeFilter}
          onValueChange={setTypeFilter}
          mb="xs"
        />
        <div style={{ flex: 1, minHeight: 0, position: 'relative' }}>
          <ScrollArea type="auto" offsetScrollbars style={{ position: 'absolute', inset: 0 }}>
            <Stack gap={0}>
              <UnstyledButton
                px={6}
                py={6}
                onClick={() => selectType(ALL_TYPES)}
                style={{
                  borderRadius: 4,
                  background: allTypes ? 'var(--mantine-color-blue-light)' : undefined,
                }}
              >
                <Text size="xs" fw={allTypes ? 700 : 500}>
                  All types
                </Text>
                {allObjectCount == null ? (
                  <Skeleton height={10} width={72} mt={4} />
                ) : (
                  <Text size="xs" c="dimmed">
                    {allObjectCount} asset{allObjectCount === 1 ? '' : 's'}
                  </Text>
                )}
              </UnstyledButton>
              {visibleTypes.map((t) => {
                const active = selectedType === t.type
                const count = statsByType[t.type]?.objectCount
                return (
                  <UnstyledButton
                    key={`${t.type}@${t.version}`}
                    px={6}
                    py={6}
                    onClick={() => selectType(t.type)}
                    style={{
                      width: '100%',
                      borderRadius: 4,
                      background: active ? 'var(--mantine-color-blue-light)' : undefined,
                    }}
                  >
                    <Text size="xs" fw={active ? 700 : 500} truncate>
                      {t.title}
                    </Text>
                    {statsPending.has(t.type) || count == null ? (
                      <Skeleton height={10} width={64} mt={4} />
                    ) : (
                      <Text size="xs" c="dimmed">
                        {count} asset{count === 1 ? '' : 's'}
                      </Text>
                    )}
                  </UnstyledButton>
                )
              })}
            </Stack>
          </ScrollArea>
        </div>
      </Paper>

      <div
        ref={splitHostRef}
        style={{
          flex: 1,
          minWidth: 0,
          minHeight: 0,
          display: assetId ? 'none' : 'flex',
          overflow: 'hidden',
        }}
      >
      <Paper
        withBorder
        p="md"
        style={{
          flex: 1,
          minWidth: 0,
          minHeight: 0,
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Group justify="space-between" mb="xs" wrap="nowrap" align="flex-start">
          <div>
            <Text size="sm" fw={650}>
              {selectedTitle}
            </Text>
            {selectedDescription && (
              <Text size="xs" c="dimmed">
                {selectedDescription}
              </Text>
            )}
            {!allTypes && (
              <Anchor component={Link} to={schemaLatestTo(selectedType)} size="xs">
                Schema (latest)
              </Anchor>
            )}
          </div>
          <Group gap="xs" wrap="nowrap">
            {exec && (
              <Text size="xs" c="dimmed">
                {exec.records} object{exec.records === 1 ? '' : 's'} · {formatExecDuration(exec.durationMs)}
              </Text>
            )}
            {results.length > 0 && (
              <>
                <Button
                  size="xs"
                  variant="subtle"
                  onClick={() => setExpandedIds(new Set(results.map((a) => a.id)))}
                >
                  Expand all
                </Button>
                <Button size="xs" variant="subtle" onClick={() => setExpandedIds(new Set())}>
                  Collapse all
                </Button>
              </>
            )}
          </Group>
        </Group>
        {dupesNote && (
          <Text size="xs" c="dimmed" mb="xs">
            {dupesNote}
          </Text>
        )}
        {error && (
          <Alert color="red" mb="xs">
            {error}
          </Alert>
        )}
        <div style={{ flex: 1, minHeight: 0, position: 'relative' }}>
          {results.length === 0 ? (
            <Text size="sm" c="dimmed">
              {busy ? 'Searching…' : 'No assets in this selection.'}
            </Text>
          ) : (
            <Stack gap="xs" style={{ position: 'absolute', inset: 0 }}>
              <ScrollArea style={{ flex: 1 }}>
                <Table striped highlightOnHover stickyHeader>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Asset</Table.Th>
                      <Table.Th>Type</Table.Th>
                      {extraColumns.map((col) => (
                        <Table.Th key={col.path}>{col.title}</Table.Th>
                      ))}
                      <Table.Th>Owner</Table.Th>
                      <Table.Th w={72} />
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {pageRows.map((a) => {
                      const open = expandedIds.has(a.id)
                      return (
                        <Fragment key={a.id}>
                          <Table.Tr>
                            <Table.Td>
                              <Anchor component={Link} to={assetTo(a.id)} size="sm">
                                {a.label}
                              </Anchor>
                            </Table.Td>
                            <Table.Td>
                              <Anchor component={Link} to={schemaLatestTo(a.type)} size="sm">
                                {a.type}
                              </Anchor>
                            </Table.Td>
                            {extraColumns.map((col) => (
                              <Table.Td key={col.path}>
                                <Text size="sm" truncate>
                                  {payloadFieldValue(a.payload, col.path)}
                                </Text>
                              </Table.Td>
                            ))}
                            <Table.Td>
                              <Text size="sm" c={a.owner ? undefined : 'dimmed'}>
                                {a.owner || '—'}
                              </Text>
                            </Table.Td>
                            <Table.Td>
                              <Group gap={4} justify="flex-end" wrap="nowrap">
                                <Tooltip label="Find duplicates of this object" withArrow>
                                  <ActionIcon
                                    size="sm"
                                    variant="subtle"
                                    aria-label={`Find duplicates of ${a.label}`}
                                    disabled={busy}
                                    onClick={() => void findDupesForType(a.type, a.id)}
                                  >
                                    <IconCopy size={14} />
                                  </ActionIcon>
                                </Tooltip>
                                <Tooltip label={open ? 'Hide details' : 'Show details'} withArrow>
                                  <ActionIcon
                                    size="sm"
                                    variant="subtle"
                                    aria-label={open ? `Collapse ${a.label}` : `Expand ${a.label}`}
                                    onClick={() => toggleExpanded(a.id)}
                                  >
                                    <IconChevronDown
                                      size={14}
                                      style={{
                                        transform: open ? 'rotate(180deg)' : 'rotate(0deg)',
                                        transition: 'transform 120ms ease',
                                      }}
                                    />
                                  </ActionIcon>
                                </Tooltip>
                              </Group>
                            </Table.Td>
                          </Table.Tr>
                          {open && (
                            <Table.Tr>
                              <Table.Td colSpan={tableColSpan} bg="var(--mantine-color-default-hover)">
                                <ObjectTree
                                  value={{
                                    id: a.id,
                                    type: a.type,
                                    schemaVersion: a.schemaVersion,
                                    owner: a.owner,
                                    payload: a.payload,
                                  }}
                                  leafLinks={{
                                    schemaVersion: schemaVersionTo(a.type, a.schemaVersion),
                                  }}
                                />
                              </Table.Td>
                            </Table.Tr>
                          )}
                        </Fragment>
                      )
                    })}
                  </Table.Tbody>
                </Table>
              </ScrollArea>
              <Group justify="space-between" wrap="wrap">
                <Text size="xs" c="dimmed">
                  {results.length} object{results.length === 1 ? '' : 's'}
                  {results.length > 0
                    ? ` · ${(safePage - 1) * pageSize + 1}–${Math.min(safePage * pageSize, results.length)}`
                    : ''}
                </Text>
                <Group gap="xs">
                  <Select
                    size="xs"
                    w={80}
                    aria-label="Page size"
                    value={String(pageSize)}
                    data={PAGE_SIZE_OPTIONS}
                    onChange={(v) => {
                      if (!v) return
                      setPageSize(Number(v))
                      setPage(1)
                    }}
                  />
                  <Pagination size="xs" value={safePage} onChange={setPage} total={pageCount} />
                </Group>
              </Group>
            </Stack>
          )}
        </div>
      </Paper>

      <Box
        role="separator"
        aria-orientation="vertical"
        aria-label="Resize search pane"
        onPointerDown={onSplitterPointerDown}
        onPointerMove={onSplitterPointerMove}
        onPointerUp={onSplitterPointerUp}
        onPointerCancel={onSplitterPointerUp}
        style={{
          width: SPLITTER_WIDTH,
          flexShrink: 0,
          cursor: 'col-resize',
          display: 'flex',
          alignItems: 'stretch',
          justifyContent: 'center',
          touchAction: 'none',
          userSelect: 'none',
        }}
      >
        <Box
          style={{
            width: 3,
            margin: '8px 0',
            borderRadius: 2,
            background: 'var(--mantine-color-default-border)',
          }}
        />
      </Box>

      <Paper
        withBorder
        p="sm"
        component="form"
        onSubmit={onSearch}
        style={{
          flex: `0 0 ${rightWidth}px`,
          width: rightWidth,
          minHeight: 0,
          overflow: 'auto',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Tabs
          value={activeSearchTab}
          onChange={(v) => {
            if (v === 'form' && !allTypes) setSearchTab('form')
            if (v === 'expr') setSearchTab('expr')
          }}
          mb="sm"
        >
          <Tabs.List>
            <Tabs.Tab value="form" disabled={allTypes}>
              Search
            </Tabs.Tab>
            <Tabs.Tab value="expr">Expression</Tabs.Tab>
          </Tabs.List>
        </Tabs>
        {activeSearchTab === 'expr' ? (
          <Stack gap="sm">
            <Textarea
              size="sm"
              label="obj-expr"
              description="JEXL over the asset pool, e.g. type == 'Component' && p.ecosystem == 'Maven'"
              placeholder="p.ecosystem == 'Maven'"
              autosize
              minRows={6}
              value={objExpr}
              onChange={(e) => setObjExpr(e.currentTarget.value)}
            />
            <Group gap="xs">
              <Button type="submit" size="sm" leftSection={<IconSearch size={14} />} disabled={busy}>
                Search
              </Button>
              <Button type="button" size="sm" variant="default" disabled={busy} onClick={() => void onExec()}>
                Exec
              </Button>
            </Group>
            {exec && (
              <Text size="xs" c="dimmed">
                {exec.records} object{exec.records === 1 ? '' : 's'} · {formatExecDuration(exec.durationMs)}
              </Text>
            )}
          </Stack>
        ) : (
          <Stack gap="sm">
            {detail && detail.searchableFields.length === 0 ? (
              <Text size="sm" c="dimmed">
                This type has no searchable fields.
              </Text>
            ) : (
              detail?.searchableFields.map((f) => (
                <TextInput
                  key={f.path}
                  size="sm"
                  label={f.title}
                  value={filters[f.path] ?? ''}
                  onChange={(e) => {
                    const value = e.currentTarget.value
                    setFilters((prev) => ({ ...prev, [f.path]: value }))
                  }}
                />
              ))
            )}
            <Group gap="xs">
              <Button type="submit" size="sm" leftSection={<IconSearch size={14} />} disabled={busy}>
                Search
              </Button>
              <Button type="button" size="sm" variant="default" disabled={busy} onClick={() => void findDupes()}>
                Find duplicates
              </Button>
            </Group>
            {exec && (
              <Text size="xs" c="dimmed">
                {exec.records} object{exec.records === 1 ? '' : 's'} · {formatExecDuration(exec.durationMs)}
              </Text>
            )}
          </Stack>
        )}
      </Paper>
      </div>
      {assetId && (
        <Paper
          withBorder
          p="md"
          style={{
            flex: 1,
            minWidth: 0,
            minHeight: 0,
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          <Outlet />
        </Paper>
      )}
    </div>
    </>
  )
}
