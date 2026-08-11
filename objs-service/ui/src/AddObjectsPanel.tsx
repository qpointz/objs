import { useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Button,
  Checkbox,
  Group,
  Pagination,
  ScrollArea,
  Stack,
  Table,
  Text,
} from '@mantine/core'
import { queryAddObjects, queryInGraph } from './api'
import {
  MatcherQueryForm,
  type MatcherQueryFormHandle,
} from './MatcherQueryForm'
import { formatQueryExecStats, type QueryExecStats } from './queryExecStats'
import type { BoMEntity, BoMEdge } from './types'

/** obj-expr OR-chain over ids — the graph-scoped substitute for the retired `ids` matcher key. */
function objExprForIds(ids: string[]): string {
  return ids.map((id) => `id == '${id}'`).join(' || ')
}

const PAGE_SIZE = 20
const MAX_PAYLOAD_COLS = 6

function isScalar(value: unknown): boolean {
  return (
    typeof value === 'string' ||
    typeof value === 'number' ||
    typeof value === 'boolean'
  )
}

function formatCell(value: unknown): string {
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

export type AddObjectsPanelProps = {
  /** When false, panel unmount effects reset auto-search state. */
  active: boolean
  /** Current graph: Search scopes to its members; when null, obj-expr Search uses the pool. */
  graphId: string | null
  onClose: () => void
  /** Hydrate matcher form (Explorer handoff). */
  matcher?: unknown | null
  /** When true on activate, run Search once with the hydrated matcher. */
  autoSearch?: boolean
  /**
   * When true with autoSearch, merge every Search hit (and its induced edges) into the draft
   * immediately — Explorer → Composer handoff (no per-row Add).
   */
  autoAddAllResults?: boolean
  draftEntityIds: ReadonlySet<string>
  baselineEntityIds: ReadonlySet<string>
  onMergeEntities: (entities: BoMEntity[]) => void
  onExcludeEntity: (entityId: string) => void
  onMergeEdges: (edges: BoMEdge[]) => void
  /** Called after a successful Search (e.g. beginQueryResult → new qid). */
  onSearchSuccess?: (matcherBody: unknown, stats: QueryExecStats) => void
}

export function AddObjectsPanel({
  active,
  graphId,
  onClose,
  matcher,
  autoSearch = false,
  autoAddAllResults = false,
  draftEntityIds,
  baselineEntityIds,
  onMergeEntities,
  onExcludeEntity,
  onMergeEdges,
  onSearchSuccess,
}: AddObjectsPanelProps) {
  const formRef = useRef<MatcherQueryFormHandle>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [searchError, setSearchError] = useState<string | null>(null)
  const [searchBusy, setSearchBusy] = useState(false)
  const [doneBusy, setDoneBusy] = useState(false)
  const [stats, setStats] = useState<QueryExecStats | null>(null)
  const [results, setResults] = useState<BoMEntity[]>([])
  const [lastSearchEdges, setLastSearchEdges] = useState<BoMEdge[]>([])
  const [page, setPage] = useState(1)
  const [selectedIds, setSelectedIds] = useState<Set<string>>(() => new Set())
  const autoSearchDone = useRef(false)
  const autoAddPending = useRef(false)

  const payloadCols = useMemo(() => scalarPayloadColumns(results), [results])
  const pageCount = Math.max(1, Math.ceil(results.length / PAGE_SIZE))
  const pageRows = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE
    return results.slice(start, start + PAGE_SIZE)
  }, [page, results])

  /**
   * Store-backed ids for induced-edge refresh: current baseline ∩ draft, plus just-added
   * search hits (baseline props can lag one render behind mergeEntities).
   */
  function storeIdsForEdgeRefresh(extraIds: Iterable<string> = []): string[] {
    const ids = new Set<string>()
    for (const id of baselineEntityIds) {
      if (draftEntityIds.has(id)) ids.add(id)
    }
    for (const id of extraIds) {
      if (id) ids.add(id)
    }
    return [...ids]
  }

  async function refreshInducedEdges(extraIds: Iterable<string> = []) {
    const storeBacked = storeIdsForEdgeRefresh(extraIds)
    if (storeBacked.length === 0 || !graphId) return
    const subgraph = await queryInGraph(graphId, { 'obj-expr': objExprForIds(storeBacked) })
    const edges = subgraph.edges ?? []
    if (edges.length > 0) onMergeEdges(edges)
  }

  /** Merge last Search edges whose endpoints are all in [entityIds]. */
  function mergeSearchEdgesAmong(entityIds: ReadonlySet<string>) {
    const edges = lastSearchEdges.filter(
      (e) => entityIds.has(e.source) && entityIds.has(e.target),
    )
    if (edges.length > 0) onMergeEdges(edges)
  }

  useEffect(() => {
    if (!active) {
      autoSearchDone.current = false
      autoAddPending.current = false
      return
    }
    if (!autoSearch || autoSearchDone.current) return
    autoSearchDone.current = true
    autoAddPending.current = autoAddAllResults
    const t = window.setTimeout(() => {
      void runSearch()
    }, 50)
    return () => window.clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps -- intentional once-per-open
  }, [active, autoSearch, autoAddAllResults, matcher])

  async function runSearch() {
    setFormError(null)
    setSearchError(null)
    try {
      const body = formRef.current?.build()
      if (body === undefined) {
        throw new Error('Matcher form is not ready')
      }
      setSearchBusy(true)
      const started = performance.now()
      const subgraph = await queryAddObjects(body, graphId)
      const durationMs = performance.now() - started
      const entities = subgraph.entities ?? []
      const edges = subgraph.edges ?? []
      const nextStats = {
        durationMs,
        nodes: entities.length,
        edges: edges.length,
      }
      setStats(nextStats)
      setResults(entities)
      setLastSearchEdges(edges)
      setPage(1)
      setSelectedIds(new Set())
      onSearchSuccess?.(body, nextStats)
      if (autoAddPending.current) {
        autoAddPending.current = false
        if (entities.length > 0) onMergeEntities(entities)
        if (edges.length > 0) onMergeEdges(edges)
        // Induce edges to entities already in the draft outside this hit set.
        await refreshInducedEdges(entities.map((e) => e.id))
        onClose()
      }
    } catch (e) {
      setStats(null)
      setSearchError(e instanceof Error ? e.message : String(e))
    } finally {
      setSearchBusy(false)
    }
  }

  async function addEntitiesToDraft(entities: BoMEntity[]) {
    if (entities.length === 0) return
    onMergeEntities(entities)
    const nextIds = new Set(draftEntityIds)
    for (const e of entities) nextIds.add(e.id)
    mergeSearchEdgesAmong(nextIds)
    try {
      await refreshInducedEdges(entities.map((e) => e.id))
    } catch (e) {
      setSearchError(e instanceof Error ? e.message : String(e))
    }
  }

  function toggleInDraft(entity: BoMEntity) {
    if (draftEntityIds.has(entity.id)) {
      onExcludeEntity(entity.id)
      return
    }
    void addEntitiesToDraft([entity])
  }

  function addSelected() {
    const toAdd = results.filter((e) => selectedIds.has(e.id) && !draftEntityIds.has(e.id))
    if (toAdd.length === 0) return
    void addEntitiesToDraft(toAdd)
  }

  function excludeSelected() {
    for (const id of selectedIds) {
      if (draftEntityIds.has(id)) onExcludeEntity(id)
    }
  }

  async function handleDone() {
    setSearchError(null)
    setDoneBusy(true)
    try {
      // Include search hits already in the draft in case baseline lags a render.
      const inDraftFromSearch = results.filter((e) => draftEntityIds.has(e.id)).map((e) => e.id)
      await refreshInducedEdges(inDraftFromSearch)
      onClose()
    } catch (e) {
      setSearchError(e instanceof Error ? e.message : String(e))
    } finally {
      setDoneBusy(false)
    }
  }

  function toggleRowSelected(id: string, checked: boolean) {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (checked) next.add(id)
      else next.delete(id)
      return next
    })
  }

  const allPageSelected =
    pageRows.length > 0 && pageRows.every((e) => selectedIds.has(e.id))
  const somePageSelected = pageRows.some((e) => selectedIds.has(e.id))

  return (
    <Stack gap="xs" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group justify="space-between" align="center" wrap="nowrap" style={{ flexShrink: 0 }}>
        <Text fw={600} size="sm">
          Add objects
        </Text>
        <Button size="compact-xs" variant="subtle" onClick={onClose}>
          Close
        </Button>
      </Group>

      <ScrollArea style={{ flex: 1, minHeight: 0 }} offsetScrollbars type="auto">
        <Stack gap="xs" pb="xs">
          {!graphId && (
            <Alert color="gray" p="xs" title="No current graph">
              obj-expr Search runs over the entity pool (includes orphans). Open or save a graph
              to scope Search to one graph; all / graph-expr still select across graphs.
            </Alert>
          )}
          <MatcherQueryForm
            ref={formRef}
            matcher={matcher}
            emptyDefaults
            defaultMode="obj-expr"
            error={formError}
            stats={stats}
            action={
              <Button size="xs" loading={searchBusy} onClick={() => void runSearch()}>
                Search
              </Button>
            }
          />

          {searchError && (
            <Alert color="red" p="xs" title="Search error">
              {searchError}
            </Alert>
          )}

          {results.length > 0 && (
            <>
              <Group justify="space-between" wrap="wrap" gap={4}>
                <Text size="xs" c="dimmed">
                  {results.length} result{results.length === 1 ? '' : 's'}
                  {stats != null ? ` · ${formatQueryExecStats(stats)}` : ''}
                </Text>
                <Group gap={4}>
                  <Button
                    size="compact-xs"
                    variant="light"
                    disabled={[...selectedIds].every((id) => draftEntityIds.has(id))}
                    onClick={addSelected}
                  >
                    Add selected
                  </Button>
                  <Button
                    size="compact-xs"
                    variant="default"
                    disabled={[...selectedIds].every((id) => !draftEntityIds.has(id))}
                    onClick={excludeSelected}
                  >
                    Remove selected
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
                      <Table.Th w={88}>Draft</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {pageRows.map((entity) => {
                      const inDraft = draftEntityIds.has(entity.id)
                      const payload = entity.payload ?? {}
                      return (
                        <Table.Tr key={entity.id}>
                          <Table.Td>
                            <Checkbox
                              size="xs"
                              aria-label={`Select ${entity.id}`}
                              checked={selectedIds.has(entity.id)}
                              onChange={(e) =>
                                toggleRowSelected(entity.id, e.currentTarget.checked)
                              }
                            />
                          </Table.Td>
                          <Table.Td
                            title={entity.id}
                            style={{ wordBreak: 'break-all', maxWidth: 96 }}
                          >
                            {entity.id.length > 12
                              ? `${entity.id.slice(0, 8)}…`
                              : entity.id}
                          </Table.Td>
                          <Table.Td>{entity.type}</Table.Td>
                          {payloadCols.map((col) => (
                            <Table.Td key={col}>
                              {formatCell(
                                payload && typeof payload === 'object'
                                  ? (payload as Record<string, unknown>)[col]
                                  : undefined,
                              )}
                            </Table.Td>
                          ))}
                          <Table.Td>
                            <Button
                              size="compact-xs"
                              variant={inDraft ? 'filled' : 'light'}
                              color={inDraft ? 'teal' : 'blue'}
                              onClick={() => toggleInDraft(entity)}
                            >
                              {inDraft ? 'In draft' : 'Add'}
                            </Button>
                          </Table.Td>
                        </Table.Tr>
                      )
                    })}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>

              {pageCount > 1 && (
                <Pagination size="sm" value={page} onChange={setPage} total={pageCount} />
              )}
            </>
          )}

          {results.length === 0 && !searchBusy && stats != null && (
            <Text size="sm" c="dimmed">
              No entities matched.
            </Text>
          )}
        </Stack>
      </ScrollArea>

      <Group justify="flex-end" gap="xs" style={{ flexShrink: 0 }}>
        <Button size="xs" variant="default" onClick={onClose}>
          Cancel
        </Button>
        <Button size="xs" loading={doneBusy} onClick={() => void handleDone()}>
          Done
        </Button>
      </Group>
    </Stack>
  )
}
