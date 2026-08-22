import { useEffect, useRef, useState } from 'react'
import { Alert, Button, Group, ScrollArea, Stack, Text } from '@mantine/core'
import { queryAddObjects, queryInGraph } from './api'
import {
  MatcherQueryForm,
  type MatcherQueryFormHandle,
} from './MatcherQueryForm'
import { ObjectResultsTable, scalarPayloadColumns } from './ObjectResultsTable'
import { formatQueryExecStats, type QueryExecStats } from './queryExecStats'
import type { BoMEntity, BoMEdge } from './types'

export { scalarPayloadColumns }

/** obj-expr OR-chain over ids — the graph-scoped substitute for the retired `ids` matcher key. */
function objExprForIds(ids: string[]): string {
  return ids.map((id) => `id == '${id}'`).join(' || ')
}

export type AddObjectsPanelProps = {
  /** When false, panel unmount effects reset auto-search state. */
  active: boolean
  /** Current graph: used for induced-edge refresh on Done; Search always uses pool / cross-graph. */
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
  const autoSearchDone = useRef(false)
  const autoAddPending = useRef(false)

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
      const subgraph = await queryAddObjects(body, null)
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
      onSearchSuccess?.(body, nextStats)
      if (autoAddPending.current) {
        autoAddPending.current = false
        if (entities.length > 0) onMergeEntities(entities)
        if (edges.length > 0) onMergeEdges(edges)
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

  function toggleInDraft(entity: BoMEntity) {
    if (draftEntityIds.has(entity.id)) {
      onExcludeEntity(entity.id)
      return
    }
    onMergeEntities([entity])
    mergeSearchEdgesAmong(new Set([...draftEntityIds, entity.id]))
  }

  function addSelected(entities: BoMEntity[]) {
    if (entities.length === 0) return
    onMergeEntities(entities)
    mergeSearchEdgesAmong(new Set([...draftEntityIds, ...entities.map((e) => e.id)]))
  }

  function excludeSelected(ids: string[]) {
    for (const id of ids) onExcludeEntity(id)
  }

  async function handleDone() {
    setDoneBusy(true)
    try {
      await refreshInducedEdges()
      onClose()
    } finally {
      setDoneBusy(false)
    }
  }

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
          <Alert color="gray" p="xs" title="Search scope">
            obj-expr runs over the entity pool (all objects, orphans included). all / graph-expr
            search across graphs.
          </Alert>
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

          <ObjectResultsTable
            results={results}
            memberIds={draftEntityIds}
            summary={
              results.length > 0
                ? `${results.length} result${results.length === 1 ? '' : 's'}${
                    stats != null ? ` · ${formatQueryExecStats(stats)}` : ''
                  }`
                : undefined
            }
            statusColumnLabel="Draft"
            memberButtonLabel="In draft"
            nonMemberButtonLabel="Add"
            onToggleMember={toggleInDraft}
            onAddSelected={addSelected}
            onRemoveSelected={excludeSelected}
          />

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
