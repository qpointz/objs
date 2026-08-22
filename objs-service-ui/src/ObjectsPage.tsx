import { useCallback, useEffect, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import {
  ActionIcon,
  Alert,
  Anchor,
  Box,
  Button,
  Group,
  Paper,
  ScrollArea,
  Stack,
  Tabs,
  Text,
  Title,
} from '@mantine/core'
import { IconTrash } from '@tabler/icons-react'
import { useNavigate } from 'react-router-dom'
import { listSchemas, queryAddObjects } from './api'
import { GraphContextBar } from './GraphContextBar'
import { useGraphContext } from './GraphContextProvider'
import {
  MatcherQueryForm,
  type MatcherQueryFormHandle,
} from './MatcherQueryForm'
import { ObjectInspectPane } from './ObjectInspectPane'
import { ObjectResultsTable } from './ObjectResultsTable'
import { scopeObjectsSearch } from './objectsSearchScope'
import { entityDisplayName } from './queryStructuredModel'
import { truncateQueryId } from './QueryStructColumns'
import { objectDisplayTitle } from './objectViewerTitle'
import { payloadFieldKindsByTypeVersion } from './payloadFieldKinds'
import { formatQueryExecStats, type QueryExecStats } from './queryExecStats'
import { clamp, maxSidePaneWidth } from './sidePaneSplit'
import type { BoMEntity, BoMSchema, GraphNode, GraphSelection } from './types'
import { shelfToComposerNavState, useObjectShelf } from './useObjectShelf'
import { VIEW_ACTION_BUTTON_SIZE } from './viewActionButtons'

const SIDE_PANE_WIDTH_KEY = 'objs.ui.objects.sidePaneWidth'
const DEFAULT_SIDE_WIDTH = 320
const MIN_SIDE_WIDTH = 240
const SPLITTER_WIDTH = 8

function loadSideWidth(): number {
  try {
    const raw = localStorage.getItem(SIDE_PANE_WIDTH_KEY)
    if (!raw) return DEFAULT_SIDE_WIDTH
    const n = Number(raw)
    return Number.isFinite(n) && n >= MIN_SIDE_WIDTH ? n : DEFAULT_SIDE_WIDTH
  } catch {
    return DEFAULT_SIDE_WIDTH
  }
}

function saveSideWidth(width: number) {
  try {
    localStorage.setItem(SIDE_PANE_WIDTH_KEY, String(Math.round(width)))
  } catch {
    // ignore
  }
}

function entityToGraphNode(entity: BoMEntity): GraphNode {
  const name =
    entity.payload != null && typeof entity.payload.name === 'string'
      ? entity.payload.name
      : null
  return {
    id: entity.id,
    name: objectDisplayTitle(name, entity.type, entity.id),
    type: entity.type,
    schemaVersion: entity.schemaVersion ?? '?',
    color: '#868e96',
    payload: entity.payload ?? {},
    annotations: entity.annotations ?? {},
    headVersion: entity.headVersion ?? null,
  }
}

function formatObjectsStats(resultsCount: number, stats: QueryExecStats): string {
  return `${resultsCount} result${resultsCount === 1 ? '' : 's'} · ${formatQueryExecStats(stats)}`
}

export function ObjectsPage() {
  const navigate = useNavigate()
  const { context } = useGraphContext()
  const formRef = useRef<MatcherQueryFormHandle>(null)
  const shelf = useObjectShelf()
  const [searchError, setSearchError] = useState<string | null>(null)
  const [searchBusy, setSearchBusy] = useState(false)
  const [stats, setStats] = useState<QueryExecStats | null>(null)
  const [results, setResults] = useState<BoMEntity[]>([])
  const [schemas, setSchemas] = useState<BoMSchema[]>([])
  const [sideTab, setSideTab] = useState<string | null>('shelf')
  const [inspectSelection, setInspectSelection] = useState<GraphSelection | null>(null)
  const [selectedIds, setSelectedIds] = useState<Set<string>>(() => new Set())
  const [sideWidth, setSideWidth] = useState(loadSideWidth)
  const splitHostRef = useRef<HTMLDivElement>(null)
  const dragRef = useRef<{ startX: number; startWidth: number } | null>(null)
  const contextKey =
    context.kind === 'graph'
      ? `graph:${context.graphId}:${context.graphVersion ?? 'latest'}`
      : context.kind === 'matcher'
        ? `matcher:${context.matcherLine ?? ''}:${JSON.stringify(context.matcherBody)}`
        : 'empty'

  const inspectNodes = useMemo(() => results.map(entityToGraphNode), [results])
  const fieldKindsByTypeVersion = useMemo(
    () => payloadFieldKindsByTypeVersion(schemas),
    [schemas],
  )

  useEffect(() => {
    listSchemas()
      .then(setSchemas)
      .catch(() => setSchemas([]))
  }, [])

  async function runSearch() {
    setSearchError(null)
    try {
      if (context.kind === 'empty') {
        throw new Error('Open a graph or matcher as graph context before searching Objects')
      }
      const body = formRef.current?.build()
      if (body === undefined) {
        throw new Error('Matcher form is not ready')
      }
      const scoped = scopeObjectsSearch(body, context)
      setSearchBusy(true)
      const started = performance.now()
      const subgraph = await queryAddObjects(scoped.body, scoped.graphId, scoped.graphVersion)
      const durationMs = performance.now() - started
      const entities = subgraph.entities ?? []
      setStats({
        durationMs,
        nodes: entities.length,
        edges: subgraph.edges?.length ?? 0,
      })
      setResults(entities)
    } catch (e) {
      setStats(null)
      setResults([])
      setSearchError(e instanceof Error ? e.message : String(e))
    } finally {
      setSearchBusy(false)
    }
  }

  useEffect(() => {
    if (context.kind === 'empty') {
      setResults([])
      setStats(null)
      setSearchError(null)
      setInspectSelection(null)
      return
    }
    setInspectSelection(null)
    setSelectedIds(new Set())
    void runSearch()
    // Re-list when shared context changes; Search button re-runs with current filter.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [contextKey])

  function onNewGraphFromShelf() {
    if (shelf.entities.length === 0) return
    navigate('/composer', { state: shelfToComposerNavState(shelf.entities) })
  }

  function openEntityInspect(entity: BoMEntity) {
    setInspectSelection({ kind: 'node', node: entityToGraphNode(entity) })
  }

  function endpointLabel(nodeId: string): string {
    const node = inspectNodes.find((n) => n.id === nodeId)
    return node?.name ?? nodeId
  }

  const onSplitterPointerDown = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      e.preventDefault()
      dragRef.current = { startX: e.clientX, startWidth: sideWidth }
      e.currentTarget.setPointerCapture(e.pointerId)
    },
    [sideWidth],
  )

  const onSplitterPointerMove = useCallback((e: ReactPointerEvent<HTMLDivElement>) => {
    const drag = dragRef.current
    if (drag == null) return
    const host = splitHostRef.current
    if (host == null) return
    const max = maxSidePaneWidth(host.clientWidth, MIN_SIDE_WIDTH)
    const next = clamp(drag.startWidth - (e.clientX - drag.startX), MIN_SIDE_WIDTH, max)
    setSideWidth(next)
  }, [])

  const onSplitterPointerUp = useCallback((e: ReactPointerEvent<HTMLDivElement>) => {
    const drag = dragRef.current
    dragRef.current = null
    if (e.currentTarget.hasPointerCapture(e.pointerId)) {
      e.currentTarget.releasePointerCapture(e.pointerId)
    }
    if (drag != null) {
      saveSideWidth(sideWidth)
    }
  }, [sideWidth])

  useEffect(() => {
    const host = splitHostRef.current
    if (host == null) return
    const clampToHost = () => {
      const max = maxSidePaneWidth(host.clientWidth, MIN_SIDE_WIDTH)
      setSideWidth((w) => {
        const next = clamp(w, MIN_SIDE_WIDTH, max)
        return next === w ? w : next
      })
    }
    clampToHost()
    const ro = new ResizeObserver(clampToHost)
    ro.observe(host)
    return () => ro.disconnect()
  }, [])

  const shelfCount = shelf.entities.length
  const showInspect = inspectSelection != null
  const canAddSelected =
    selectedIds.size > 0 && [...selectedIds].some((id) => !shelf.ids.has(id))
  const canRemoveSelected =
    selectedIds.size > 0 && [...selectedIds].some((id) => shelf.ids.has(id))

  function addSelectedToShelf() {
    const entities = results.filter((e) => selectedIds.has(e.id) && !shelf.ids.has(e.id))
    if (entities.length > 0) shelf.add(entities)
  }

  function removeSelectedFromShelf() {
    const ids = [...selectedIds].filter((id) => shelf.ids.has(id))
    if (ids.length > 0) shelf.remove(ids)
  }

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group align="center" wrap="nowrap" gap="md" style={{ flexShrink: 0 }}>
        <Title order={3} style={{ flexShrink: 0 }}>
          Objects
        </Title>
        <Box style={{ flex: 1, minWidth: 0 }}>
          <GraphContextBar />
        </Box>
      </Group>

      <Group
        justify="space-between"
        align="center"
        wrap="wrap"
        style={{ flexShrink: 0 }}
        gap="xs"
        data-tour="objects-view-actions"
      >
        <Text size="xs" c="dimmed" style={{ alignSelf: 'center' }}>
          {stats != null ? formatObjectsStats(results.length, stats) : '\u00a0'}
        </Text>
        <Group gap="xs" wrap="nowrap">
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            variant="default"
            disabled={!canAddSelected}
            onClick={addSelectedToShelf}
          >
            Add selected to shelf
          </Button>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            variant="default"
            disabled={!canRemoveSelected}
            onClick={removeSelectedFromShelf}
          >
            Remove selected from shelf
          </Button>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            variant="default"
            disabled={shelfCount === 0}
            onClick={() => shelf.clear()}
            data-tour="objects-clear-shelf"
          >
            Clear shelf
          </Button>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            disabled={shelfCount === 0}
            onClick={onNewGraphFromShelf}
            data-tour="objects-new-graph"
          >
            New graph from shelf
          </Button>
        </Group>
      </Group>

      <Group
        ref={splitHostRef}
        align="stretch"
        gap={0}
        wrap="nowrap"
        style={{ flex: 1, minHeight: 0, overflow: 'hidden' }}
      >
        <Stack gap="xs" style={{ flex: 1, minWidth: 0, minHeight: 0 }} data-tour="objects-results">
          {searchError && (
            <Alert color="red" p="xs" title="Search error" withCloseButton onClose={() => setSearchError(null)}>
              {searchError}
            </Alert>
          )}
          {results.length > 0 ? (
            <ObjectResultsTable
              results={results}
              memberIds={shelf.ids}
              statusColumnLabel="Shelf"
              memberButtonLabel="On shelf"
              nonMemberButtonLabel="Add"
              onToggleMember={(entity) => shelf.toggle(entity)}
              onAddSelected={(entities) => shelf.add(entities)}
              onRemoveSelected={(ids) => shelf.remove(ids)}
              onOpenId={openEntityInspect}
              selectedIds={selectedIds}
              onSelectedIdsChange={setSelectedIds}
              hideBulkActions
            />
          ) : (
            !searchBusy &&
            (stats != null ? (
              <Text size="sm" c="dimmed">
                No entities matched.
              </Text>
            ) : (
              !searchError && (
                <Text size="sm" c="dimmed">
                  {context.kind === 'empty'
                    ? 'Open a graph or matcher in the context bar to list objects.'
                    : 'Loading objects from the current graph context…'}
                </Text>
              )
            ))
          )}
        </Stack>

        <Box
          role="separator"
          aria-orientation="vertical"
          aria-label="Resize Objects side pane"
          onPointerDown={onSplitterPointerDown}
          onPointerMove={onSplitterPointerMove}
          onPointerUp={onSplitterPointerUp}
          onPointerCancel={onSplitterPointerUp}
          style={{
            width: SPLITTER_WIDTH,
            flexShrink: 0,
            cursor: 'col-resize',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            touchAction: 'none',
            userSelect: 'none',
          }}
        >
          <Box
            style={{
              width: 3,
              height: 48,
              borderRadius: 2,
              background: 'var(--mantine-color-default-border)',
            }}
          />
        </Box>

        <Paper
          withBorder
          p="xs"
          data-tour={showInspect ? 'objects-object-inspect' : 'objects-side'}
          style={{
            width: sideWidth,
            flexShrink: 0,
            display: 'flex',
            flexDirection: 'column',
            minHeight: 0,
            overflow: 'hidden',
          }}
        >
          {showInspect ? (
            <ObjectInspectPane
              selection={inspectSelection}
              nodes={inspectNodes}
              graphContext={null}
              fieldKindsByTypeVersion={fieldKindsByTypeVersion}
              onSelectNode={(id) => {
                const entity = results.find((e) => e.id === id)
                if (entity) openEntityInspect(entity)
              }}
              onClearSelection={() => setInspectSelection(null)}
              endpointLabel={endpointLabel}
            />
          ) : (
            <Tabs
              value={sideTab}
              onChange={setSideTab}
              style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
            >
              <Tabs.List style={{ flexShrink: 0 }}>
                <Tabs.Tab
                  value="shelf"
                  data-tour="objects-shelf-tab"
                  fw={shelfCount > 0 ? 700 : undefined}
                >
                  Shelf{shelfCount > 0 ? ` (${shelfCount})` : ''}
                </Tabs.Tab>
                <Tabs.Tab value="matcher" data-tour="objects-matcher-tab">
                  Matcher
                </Tabs.Tab>
              </Tabs.List>

              <Tabs.Panel
                value="shelf"
                pt="xs"
                style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
              >
                {shelfCount === 0 ? (
                  <Text size="xs" c="dimmed">
                    Add objects from search results.
                  </Text>
                ) : (
                  <ScrollArea style={{ flex: 1, minHeight: 0 }} offsetScrollbars type="auto">
                    <Stack gap={4}>
                      {shelf.entities.map((entity) => (
                        <Group key={entity.id} justify="space-between" wrap="nowrap" gap={4}>
                          <Stack gap={0} style={{ minWidth: 0, flex: 1 }}>
                            <Group gap={4} wrap="nowrap" align="center" style={{ minWidth: 0 }}>
                              <Text size="xs" fw={600} style={{ flexShrink: 0 }}>
                                {entity.type}
                              </Text>
                              <Anchor
                                component="button"
                                type="button"
                                size="xs"
                                truncate
                                title={entityDisplayName(entity)}
                                onClick={() => openEntityInspect(entity)}
                                style={{ minWidth: 0 }}
                              >
                                {entityDisplayName(entity)}
                              </Anchor>
                            </Group>
                            <Text
                              size="xs"
                              c="dimmed"
                              ff="monospace"
                              truncate
                              title={entity.id}
                            >
                              {truncateQueryId(entity.id)}
                            </Text>
                          </Stack>
                          <ActionIcon
                            size="xs"
                            variant="subtle"
                            color="red"
                            aria-label={`Remove ${entityDisplayName(entity)} from shelf`}
                            onClick={() => shelf.remove([entity.id])}
                          >
                            <IconTrash size={12} />
                          </ActionIcon>
                        </Group>
                      ))}
                    </Stack>
                  </ScrollArea>
                )}
              </Tabs.Panel>

              <Tabs.Panel
                value="matcher"
                pt="xs"
                style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
              >
                <ScrollArea style={{ flex: 1, minHeight: 0 }} offsetScrollbars type="auto">
                  <MatcherQueryForm
                    ref={formRef}
                    emptyDefaults
                    defaultMode="obj-expr"
                    fixedMode="obj-expr"
                    allowEmptyObjExpr
                    action={
                      <Button
                        size="xs"
                        loading={searchBusy}
                        onClick={() => void runSearch()}
                        data-tour="objects-search"
                      >
                        Search
                      </Button>
                    }
                  />
                </ScrollArea>
              </Tabs.Panel>
            </Tabs>
          )}
        </Paper>
      </Group>
    </Stack>
  )
}
