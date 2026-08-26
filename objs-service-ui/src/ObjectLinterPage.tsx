import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Badge,
  Box,
  Button,
  Code,
  Group,
  Menu,
  Modal,
  Paper,
  Stack,
  Table,
  Tabs,
  Text,
  Title,
  Tooltip,
} from '@mantine/core'
import { useLocation, useNavigate } from 'react-router-dom'
import { mutationShapeError, normalizeGraphMutation } from './graphDraft'
import {
  createGraph,
  getGraph,
  patchGraphMutation,
  putGraphMutation,
  putGraphAnnotations,
  validateGraphMutation,
  toGraphData,
  type GraphMutationBody,
} from './api'
import { ComposerGraphBar } from './ComposerGraphBar'
import { JsonYamlEditor, type JsonYamlEditorHandle } from './JsonYamlEditor'
import { NewGraphModal } from './NewGraphModal'
import { NewUuidButton } from './NewUuidButton'
import {
  ObjectLinterVisualPanel,
  type ObjectLinterVisualPanelHandle,
} from './ObjectLinterVisualPanel'
import { OpenGraphModal } from './OpenGraphModal'
import { OpenMatcherModal } from './OpenMatcherModal'
import type { QueryExecStats } from './queryExecStats'
import type {
  BoMEdge,
  BoMEntity,
  BoMGraphResponse,
  BoMGraphContents,
  BoMValidationIssue,
  GraphValidationResult,
} from './types'
import { useCurrentGraphId } from './useCurrentGraph'
import { useGraphDraft } from './useGraphDraft'
import { useGraphSelectionHistory } from './useGraphSelectionHistory'
import {
  edgeIdsFromValidationIssues,
  entityIdsFromValidationIssues,
  validationTargetFromIssue,
} from './validationIssueTargets'
import { VIEW_ACTION_BUTTON_SIZE } from './viewActionButtons'

export { graphShapeError, mutationShapeError } from './graphDraft'

function annotationsEqual(a: Record<string, string>, b: Record<string, string>): boolean {
  const aKeys = Object.keys(a)
  const bKeys = Object.keys(b)
  if (aKeys.length !== bKeys.length) return false
  return aKeys.every((k) => a[k] === b[k])
}

function isMutationDirty(body: {
  entities: { set: unknown[]; unset: unknown[] }
  edges: { set: unknown[]; unset: unknown[] }
}): boolean {
  return (
    body.entities.set.length +
      body.edges.set.length +
      body.entities.unset.length +
      body.edges.unset.length >
    0
  )
}

type ObjectLinterNavState = {
  matcher?: unknown
  /** Explorer handoff: merge all canvas objects (and edges) into the draft. */
  addAll?: boolean
  /**
   * Explorer Selection → New graph from selection: replace draft with entire canvas
   * (ids preserved; graphId null until first Save).
   */
  replaceDraft?: boolean
  /** Explorer canvas snapshot — preferred over re-running matcher. */
  graphContents?: BoMGraphContents
  /**
   * Graph mode → Open in Composer: load this graph from the API.
   * Explicit `null` clears current graph (Selection → New graph from selection).
   */
  graphId?: string | null
}

export function ObjectLinterPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const {
    document,
    pendingDeleteCount,
    mutationBody,
    emptyMutation,
    state,
    applyParsedMutation,
    loadGraphContents,
    resetToRollback,
    clearDraft,
    upsertEntity,
    upsertEdge,
    removeEntity,
    removeEdge,
    excludeEntity,
    excludeEdge,
    mergeEntities,
    mergeEdges,
    markApplied,
    canvasDocument,
    restoreDeletedEntity,
    restoreDeletedEdge,
    revertEntityChanges,
    revertEdgeChanges,
  } = useGraphDraft()

  const editorRef = useRef<JsonYamlEditorHandle>(null)
  const visualRef = useRef<ObjectLinterVisualPanelHandle>(null)
  const [tab, setTab] = useState<'visual' | 'text'>('visual')
  const [textError, setTextError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<GraphValidationResult | null>(null)
  const [addObjectsOpen, setAddObjectsOpen] = useState(false)
  const [openGraphOpen, setOpenGraphOpen] = useState(false)
  const [openMatcherForNew, setOpenMatcherForNew] = useState(false)
  const [snapshotOpen, setSnapshotOpen] = useState(false)
  const [cloneOpen, setCloneOpen] = useState(false)
  const [overwriteOpen, setOverwriteOpen] = useState(false)
  const [handoffMatcher, setHandoffMatcher] = useState<unknown | null>(null)
  const [autoSearch, setAutoSearch] = useState(false)
  const [autoAddAllResults, setAutoAddAllResults] = useState(false)
  const [addObjectsStats, setAddObjectsStats] = useState<QueryExecStats | null>(null)
  const [currentGraphId, setCurrentGraphId] = useCurrentGraphId()
  const [graphAnnotations, setGraphAnnotations] = useState<Record<string, string>>({})
  const [savedGraphAnnotations, setSavedGraphAnnotations] = useState<Record<string, string>>({})
  /** True after a server create that has not yet had a successful Save of draft content. */
  const [neverSavedSinceCreate, setNeverSavedSinceCreate] = useState(false)
  const [graphLoading, setGraphLoading] = useState(false)

  const graphView = useMemo(() => toGraphData(canvasDocument), [canvasDocument])
  const onFocusNode = useCallback((nodeId: string) => {
    visualRef.current?.focusNode(nodeId)
  }, [])
  const { selection, select, beginQueryResult, clearQuery } = useGraphSelectionHistory({
    nodes: graphView.nodes,
    links: graphView.links,
    onFocusNode,
  })

  const annotationsDirty = !annotationsEqual(graphAnnotations, savedGraphAnnotations)
  const mutationDirty = isMutationDirty(mutationBody)
  // Header annotation edits count as dirty for new and existing graphs (persist via create or PUT …/annotations).
  const draftDirty = mutationDirty || annotationsDirty
  const saveEnabled = draftDirty || neverSavedSinceCreate
  const snapshotEnabled = currentGraphId != null && !mutationDirty && !annotationsDirty && !neverSavedSinceCreate

  /** Replace the draft with a graph's stored members (Open graph / Snapshot). */
  const loadGraphMembers = useCallback(
    (contents: BoMGraphContents) => {
      loadGraphContents(contents)
      clearQuery()
      setResult(null)
      setError(null)
    },
    [clearQuery, loadGraphContents],
  )

  const applyGraphHeader = useCallback((id: string, annotations: Record<string, string>) => {
    setCurrentGraphId(id)
    setGraphAnnotations(annotations)
    setSavedGraphAnnotations(annotations)
    setNeverSavedSinceCreate(false)
  }, [setCurrentGraphId])

  const onOpenGraph = useCallback(
    (id: string, resolved: BoMGraphResponse) => {
      applyGraphHeader(id, resolved.annotations ?? {})
      loadGraphMembers(resolved.graph)
    },
    [applyGraphHeader, loadGraphMembers],
  )

  const onSnapshotCreated = useCallback(
    (id: string, resolved: BoMGraphResponse) => {
      applyGraphHeader(id, resolved.annotations ?? {})
      loadGraphMembers(resolved.graph)
    },
    [applyGraphHeader, loadGraphMembers],
  )

  const onNewGraphChrome = useCallback(() => {
    clearDraft()
    clearQuery()
    setCurrentGraphId(null)
    setGraphAnnotations({})
    setSavedGraphAnnotations({})
    setNeverSavedSinceCreate(false)
    setResult(null)
    setError(null)
  }, [clearDraft, clearQuery, setCurrentGraphId])

  const onComposerMatcherApplied = useCallback(
    (contents: { entities: unknown[]; edges: unknown[] }) => {
      onNewGraphChrome()
      const entities = contents.entities as BoMEntity[]
      const edges = contents.edges as BoMEdge[]
      if (entities.length > 0) mergeEntities(entities)
      if (edges.length > 0) mergeEdges(edges)
      setTab('visual')
    },
    [mergeEdges, mergeEntities, onNewGraphChrome],
  )

  // Keep opened-graph chrome and Visual draft in sync: remount / shared currentGraphId
  // previously restored annotations only, leaving an empty canvas.
  useEffect(() => {
    if (!currentGraphId) {
      setGraphLoading(false)
      return
    }
    let cancelled = false
    setGraphLoading(true)
    getGraph(currentGraphId)
      .then((resolved) => {
        if (cancelled) return
        const ann = resolved.annotations ?? {}
        setGraphAnnotations(ann)
        setSavedGraphAnnotations(ann)
        loadGraphMembers(resolved.graph)
        setNeverSavedSinceCreate(false)
      })
      .catch(() => {
        if (cancelled) return
        // Missing graph (e.g. backend restart) — clear selection so chrome matches canvas.
        setCurrentGraphId(null)
        setGraphAnnotations({})
        setSavedGraphAnnotations({})
        clearDraft()
      })
      .finally(() => {
        if (!cancelled) setGraphLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [clearDraft, currentGraphId, loadGraphMembers, setCurrentGraphId])

  const draftEntityIds = useMemo(
    () => new Set(document.entities.map((e) => e.id)),
    [document.entities],
  )

  const invalidEntityIds = useMemo(() => {
    if (!result || result.issues.length === 0) return new Set<string>()
    return entityIdsFromValidationIssues(
      result.issues,
      mutationBody.entities.set,
      mutationBody.edges.set,
    )
  }, [mutationBody.edges.set, mutationBody.entities.set, result])

  const invalidEdgeIds = useMemo(() => {
    if (!result || result.issues.length === 0) return new Set<string>()
    return edgeIdsFromValidationIssues(
      result.issues,
      mutationBody.entities.set,
      mutationBody.edges.set,
    )
  }, [mutationBody.edges.set, mutationBody.entities.set, result])

  const focusValidationIssue = useCallback(
    (issue: BoMValidationIssue) => {
      const target = validationTargetFromIssue(
        issue,
        mutationBody.entities.set,
        mutationBody.edges.set,
      )
      if (!target) return
      setTab('visual')
      setError(null)
      if (target.kind === 'entity') {
        const node = graphView.nodes.find((n) => n.id === target.id)
        if (!node) return
        select({ kind: 'node', node })
        requestAnimationFrame(() => visualRef.current?.focusNode(target.id))
        return
      }
      const edge = graphView.links.find((l) => l.id === target.id)
      if (!edge) return
      select({ kind: 'edge', edge })
      requestAnimationFrame(() => visualRef.current?.focusNode(edge.source))
    },
    [graphView.links, graphView.nodes, mutationBody.edges.set, mutationBody.entities.set, select],
  )

  const onDraftParsed = useCallback(
    (parsed: { valid: boolean; value?: unknown; error?: string }) => {
      // Text editor stays mounted under the Visual tab; ignore its sync while Visual is active
      // so a stale YAML round-trip cannot resurrect cascaded soft-deleted edges.
      if (tab !== 'text') return
      setResult(null)
      if (!parsed.valid) {
        setTextError(parsed.error ?? 'Invalid mutation')
        return
      }
      const shape = mutationShapeError(parsed.value)
      if (shape) {
        setTextError(shape)
        return
      }
      const err = applyParsedMutation(parsed.value)
      if (err) {
        setTextError(err)
        return
      }
      setTextError(null)
    },
    [applyParsedMutation, tab],
  )

  function trySwitchTab(next: string | null) {
    if (!next || next === tab) return
    if (next === 'visual' && textError) {
      setError('Fix YAML/JSON before switching to Visual (last good draft is preserved).')
      return
    }
    setError(null)
    setTab(next as 'visual' | 'text')
  }

  async function syncTextIntoDraft(): Promise<{ body: typeof mutationBody } | null> {
    if (tab !== 'text') return { body: mutationBody }
    const parsed = editorRef.current?.getParsedForSubmit()
    if (!parsed?.ok) {
      setError(parsed?.error ?? 'Invalid mutation')
      setResult(null)
      return null
    }
    const shapeErr = mutationShapeError(parsed.value)
    if (shapeErr) {
      setError(shapeErr)
      setResult(null)
      return null
    }
    const normalized = normalizeGraphMutation(parsed.value)
    if (!normalized) {
      setError('Invalid mutation')
      return null
    }
    const err = applyParsedMutation(parsed.value)
    if (err) {
      setError(err)
      return null
    }
    return { body: normalized }
  }

  async function validate() {
    const synced = await syncTextIntoDraft()
    if (!synced) return
    setBusy(true)
    setError(null)
    setResult(null)
    try {
      setResult(
        await validateGraphMutation(synced.body, {
          graphId: currentGraphId ?? undefined,
          mode: 'merge',
        }),
      )
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  function overwriteMutationBody(): GraphMutationBody {
    return {
      entities: {
        set: document.entities.filter((e) => !state.pendingDeleteEntityIds.has(e.id)),
        unset: [],
      },
      edges: {
        set: document.edges.filter((e) => e.id == null || !state.pendingDeleteEdgeIds.has(e.id)),
        unset: [],
      },
    }
  }

  async function saveGraph(mode: 'merge' | 'overwrite' = 'merge') {
    const synced = await syncTextIntoDraft()
    if (!synced) return
    setBusy(true)
    setError(null)
    setResult(null)
    try {
      const annDirty = !annotationsEqual(graphAnnotations, savedGraphAnnotations)
      const body = mode === 'overwrite' ? overwriteMutationBody() : synced.body
      const mutDirty = mode === 'overwrite' || isMutationDirty(synced.body)
      const validateMode = mode === 'overwrite' ? 'replace' : 'merge'

      if (currentGraphId == null) {
        if (mode === 'overwrite') {
          setError('Overwrite requires an existing graph — Save (merge) first')
          return
        }
        if (mutDirty) {
          const validation = await validateGraphMutation(synced.body)
          setResult(validation)
          if (validation.issues.length > 0) {
            setError('Fix validation issues before Save')
            return
          }
        }

        // First Save: create header with membership for pool-backed ids, then MERGE
        // new/modified entities + all live edges into the new graph (G-U5).
        const membershipIds = document.entities
          .filter((e) => state.baselineEntityIds.has(e.id))
          .map((e) => e.id)
        const created = await createGraph({
          annotations: graphAnnotations,
          entityIds: membershipIds,
        })
        const edgeSets = document.edges.filter(
          (e) => e.id != null && !state.pendingDeleteEdgeIds.has(e.id),
        )
        const createMutation = {
          entities: {
            set: synced.body.entities.set,
            unset: [] as string[],
          },
          edges: {
            set: edgeSets,
            unset: [] as string[],
          },
        }
        if (isMutationDirty(createMutation)) {
          await patchGraphMutation(created.id, createMutation)
        }
        applyGraphHeader(created.id, created.annotations ?? graphAnnotations)
        markApplied()
        return
      }

      if (mutDirty) {
        const validation = await validateGraphMutation(body, {
          graphId: currentGraphId,
          mode: validateMode,
        })
        setResult(validation)
        if (validation.issues.length > 0) {
          setError('Fix validation issues before Save')
          return
        }
        if (mode === 'overwrite') {
          await putGraphMutation(currentGraphId, body)
        } else {
          await patchGraphMutation(currentGraphId, body)
        }
        markApplied()
      }
      if (annDirty) {
        const updated = await putGraphAnnotations(currentGraphId, graphAnnotations)
        setGraphAnnotations(updated.annotations ?? graphAnnotations)
        setSavedGraphAnnotations(updated.annotations ?? graphAnnotations)
      }
      setNeverSavedSinceCreate(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  function closeAddObjects() {
    setAddObjectsOpen(false)
    setAutoSearch(false)
    setAutoAddAllResults(false)
    setHandoffMatcher(null)
  }

  function openAddObjects(opts?: {
    matcher?: unknown
    autoSearch?: boolean
    autoAddAllResults?: boolean
  }) {
    setHandoffMatcher(opts?.matcher ?? null)
    setAutoSearch(opts?.autoSearch ?? false)
    setAutoAddAllResults(opts?.autoAddAllResults ?? false)
    setAddObjectsOpen(true)
    setTab('visual')
  }

  useEffect(() => {
    const navState = location.state as ObjectLinterNavState | null
    if (navState == null || typeof navState !== 'object') return
    const hasMatcher = 'matcher' in navState && navState.matcher !== undefined
    const hasGraphContents = navState.graphContents != null && typeof navState.graphContents === 'object'
    const hasGraphIdKey = 'graphId' in navState
    if (!hasMatcher && !hasGraphContents && !hasGraphIdKey) return

    const matcher = hasMatcher ? navState.matcher : undefined
    const addAll = navState.addAll === true
    const replaceDraft = navState.replaceDraft === true
    const graphContents = hasGraphContents ? navState.graphContents : undefined
    navigate('.', { replace: true, state: null })

    // Selection → New graph from selection: clear id, replace draft with entire canvas.
    if (replaceDraft && graphContents) {
      setCurrentGraphId(null)
      setGraphAnnotations({})
      setSavedGraphAnnotations({})
      setNeverSavedSinceCreate(false)
      loadGraphMembers(graphContents)
      setTab('visual')
      return
    }

    // Graph → Open in Composer: set id and load members from API (no Explorer snapshot).
    if (typeof navState.graphId === 'string' && navState.graphId.length > 0) {
      const id = navState.graphId
      setCurrentGraphId(id)
      void getGraph(id)
        .then((resolved) => {
          applyGraphHeader(id, resolved.annotations ?? {})
          loadGraphMembers(resolved.graph)
          setTab('visual')
        })
        .catch((e: unknown) => {
          setError(e instanceof Error ? e.message : String(e))
        })
      return
    }

    if (hasGraphIdKey && navState.graphId == null) {
      setCurrentGraphId(null)
      setGraphAnnotations({})
      setSavedGraphAnnotations({})
      setNeverSavedSinceCreate(false)
    }

    if (addAll && graphContents) {
      const entities = graphContents.entities ?? []
      const edges = graphContents.edges ?? []
      if (entities.length > 0) mergeEntities(entities)
      if (edges.length > 0) mergeEdges(edges)
      setResult(null)
      setError(null)
      setTab('visual')
      return
    }

    if (matcher !== undefined) {
      openAddObjects({ matcher, autoSearch: true, autoAddAllResults: addAll })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- handoff once per location.state
    // eslint-disable-next-line react-hooks/exhaustive-deps -- handoff once per location.state
  }, [location.state, navigate, mergeEntities, mergeEdges, loadGraphMembers, applyGraphHeader])

  const valid = result != null && result.issues.length === 0

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group align="center" wrap="nowrap" gap="md" style={{ flexShrink: 0 }}>
        <Title order={3} style={{ flexShrink: 0 }}>
          Composer
        </Title>
        <Box style={{ flex: 1, minWidth: 0 }}>
          <ComposerGraphBar
            graphId={currentGraphId}
            annotations={graphAnnotations}
            versionLabel={currentGraphId != null ? 'Latest' : null}
            nodeCount={graphView.nodes.length}
            edgeCount={graphView.links.length}
            onBlank={onNewGraphChrome}
            onOpenMatcher={() => setOpenMatcherForNew(true)}
            onOpenGraph={() => setOpenGraphOpen(true)}
          />
        </Box>
      </Group>

      <Group
        justify="flex-end"
        align="center"
        wrap="wrap"
        style={{ flexShrink: 0 }}
        gap="xs"
        data-tour="composer-view-actions"
      >
        <Button size={VIEW_ACTION_BUTTON_SIZE} variant="default" onClick={resetToRollback}>
          Reset
        </Button>
        <Button
          size={VIEW_ACTION_BUTTON_SIZE}
          variant="default"
          color="red"
          onClick={() => {
            clearDraft()
            clearQuery()
            setResult(null)
          }}
        >
          Clear
        </Button>
        <Button
          size={VIEW_ACTION_BUTTON_SIZE}
          loading={busy}
          variant="light"
          onClick={() => void validate()}
        >
          Validate
        </Button>
        <Tooltip
          label={
            saveEnabled
              ? undefined
              : 'Nothing to save — draft is clean and the graph is already saved'
          }
          disabled={saveEnabled}
          withArrow
        >
          <Group gap={0} style={{ display: 'inline-flex' }}>
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              loading={busy}
              disabled={!saveEnabled}
              onClick={() => void saveGraph('merge')}
              style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
            >
              Save
            </Button>
            <Menu position="bottom-end" withinPortal>
              <Menu.Target>
                <Button
                  size={VIEW_ACTION_BUTTON_SIZE}
                  loading={busy}
                  disabled={!saveEnabled || currentGraphId == null}
                  px={8}
                  style={{ borderTopLeftRadius: 0, borderBottomLeftRadius: 0, borderLeft: 0 }}
                  aria-label="More save options"
                >
                  ▾
                </Button>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item
                  disabled={currentGraphId == null}
                  onClick={() => setOverwriteOpen(true)}
                >
                  Overwrite…
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
        </Tooltip>
        <Tooltip
          label={
            snapshotEnabled
              ? undefined
              : 'Create version requires a saved, clean graph (not dirty / not unsaved)'
          }
          disabled={snapshotEnabled}
          withArrow
        >
          <span style={{ display: 'inline-flex' }} data-tour="composer-version">
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              variant="light"
              disabled={!snapshotEnabled}
              onClick={() => setSnapshotOpen(true)}
            >
              Create version
            </Button>
          </span>
        </Tooltip>
        <Tooltip
          label={
            snapshotEnabled
              ? undefined
              : 'Clone requires a saved, clean graph (not dirty / not unsaved)'
          }
          disabled={snapshotEnabled}
          withArrow
        >
          <span style={{ display: 'inline-flex' }}>
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              variant="light"
              disabled={!snapshotEnabled}
              onClick={() => setCloneOpen(true)}
            >
              Clone
            </Button>
          </span>
        </Tooltip>
        {mutationBody.entities.set.length + mutationBody.edges.set.length > 0 && (
          <Badge color="blue" variant="light" size="sm">
            {mutationBody.entities.set.length + mutationBody.edges.set.length} upsert
            {mutationBody.entities.set.length + mutationBody.edges.set.length === 1
              ? ''
              : 's'}
          </Badge>
        )}
        {pendingDeleteCount > 0 && (
          <Badge color="orange" variant="filled" size="sm">
            {pendingDeleteCount} pending delete{pendingDeleteCount === 1 ? '' : 's'}
          </Badge>
        )}
      </Group>

      <Tabs
        value={tab}
        onChange={trySwitchTab}
        style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', position: 'relative' }}
      >
        <Tabs.List style={{ flexShrink: 0 }}>
          <Tabs.Tab value="visual">Visual</Tabs.Tab>
          <Tabs.Tab value="text">Text</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel
          value="visual"
          pt="sm"
          style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
        >
          <ObjectLinterVisualPanel
            ref={visualRef}
            draftState={state}
            canvasDocument={canvasDocument}
            selection={selection}
            onSelect={select}
            onUpsertEntity={upsertEntity}
            onUpsertEdge={upsertEdge}
            onRemoveEntity={removeEntity}
            onRemoveEdge={removeEdge}
            onExcludeEntity={excludeEntity}
            onExcludeEdge={excludeEdge}
            onRestoreDeletedEntity={restoreDeletedEntity}
            onRestoreDeletedEdge={restoreDeletedEdge}
            onRevertEntityChanges={revertEntityChanges}
            onRevertEdgeChanges={revertEdgeChanges}
            invalidEntityIds={invalidEntityIds}
            invalidEdgeIds={invalidEdgeIds}
            addObjectsOpen={addObjectsOpen}
            graphId={currentGraphId}
            onCloseAddObjects={closeAddObjects}
            addObjectsMatcher={handoffMatcher}
            addObjectsAutoSearch={autoSearch}
            addObjectsAutoAddAll={autoAddAllResults}
            draftEntityIds={draftEntityIds}
            onMergeEntities={mergeEntities}
            onMergeEdges={mergeEdges}
            onAddObjectsSearchSuccess={(_body, stats) => {
              beginQueryResult()
              setAddObjectsStats(stats)
              setResult(null)
            }}
            graphAnnotations={graphAnnotations}
            onGraphAnnotationsChange={setGraphAnnotations}
            onToggleAddObjects={() =>
              addObjectsOpen ? closeAddObjects() : openAddObjects()
            }
            addObjectsStats={addObjectsStats}
            canvasLoading={graphLoading}
          />
        </Tabs.Panel>

        <Tabs.Panel
          value="text"
          pt="sm"
          style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
        >
          <Paper
            withBorder
            p="sm"
            style={{
              flex: 1,
              minHeight: 0,
              display: 'flex',
              flexDirection: 'column',
            }}
          >
            <JsonYamlEditor
              ref={editorRef}
              value={mutationBody}
              rollbackValue={emptyMutation}
              minHeight={240}
              fillHeight
              onDraftParsed={onDraftParsed}
              onRollback={() => {
                resetToRollback()
                setTextError(null)
                setResult(null)
              }}
              extraActions={<NewUuidButton />}
            />
          </Paper>
          {textError && (
            <Alert mt="sm" color="orange" title="Text parse error" style={{ flexShrink: 0 }}>
              {textError}
            </Alert>
          )}
        </Tabs.Panel>
      </Tabs>

      {error && (
        <Alert color="red" title="Error" style={{ flexShrink: 0 }}>
          {error}
        </Alert>
      )}

      {result && (
        <Paper
          withBorder
          radius="md"
          p={0}
          style={{ flexShrink: 0, maxHeight: 200, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}
        >
          <Group
            justify="space-between"
            gap={8}
            wrap="nowrap"
            px="xs"
            py={6}
            style={{
              borderBottom: '1px solid var(--mantine-color-default-border)',
              background: 'color-mix(in srgb, var(--mantine-color-default-hover) 55%, transparent)',
              flexShrink: 0,
            }}
          >
            <Group gap={8} wrap="nowrap" style={{ minWidth: 0 }}>
              <Box
                style={{
                  width: 3,
                  height: 14,
                  borderRadius: 2,
                  background: valid
                    ? 'var(--mantine-color-teal-filled)'
                    : 'var(--mantine-color-red-filled)',
                  flexShrink: 0,
                }}
              />
              <Text size="xs" fw={700} tt="uppercase" style={{ letterSpacing: '0.05em' }}>
                Validation
              </Text>
              <Badge size="xs" variant="light" color={valid ? 'teal' : 'red'}>
                {valid ? 'valid' : `${result.issues.length} issue${result.issues.length === 1 ? '' : 's'}`}
              </Badge>
            </Group>
            <Button size="compact-xs" variant="subtle" onClick={() => setResult(null)}>
              Clear
            </Button>
          </Group>
          <Box style={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
            {valid ? (
              <Text size="xs" c="dimmed" px="xs" py={8}>
                Mutation conforms to registered schemas and edge rules.
              </Text>
            ) : (
              <Table
                striped
                highlightOnHover
                withTableBorder={false}
                withColumnBorders
                horizontalSpacing={8}
                verticalSpacing={4}
                style={{ fontSize: 'var(--mantine-font-size-xs)' }}
              >
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th w={120}>Code</Table.Th>
                    <Table.Th>Message</Table.Th>
                    <Table.Th w="28%">Path</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {result.issues.map((issue, index) => {
                    const target = validationTargetFromIssue(
                      issue,
                      mutationBody.entities.set,
                      mutationBody.edges.set,
                    )
                    const clickable = target != null
                    return (
                      <Table.Tr
                        key={`${issue.code}:${issue.path}:${index}`}
                        onClick={clickable ? () => focusValidationIssue(issue) : undefined}
                        style={{ cursor: clickable ? 'pointer' : 'default' }}
                        title={clickable ? 'Select on canvas' : undefined}
                      >
                        <Table.Td
                          style={{
                            verticalAlign: 'top',
                            background:
                              'color-mix(in srgb, var(--mantine-color-default-hover) 40%, transparent)',
                          }}
                        >
                          <Text size="xs" fw={700} c="red" style={{ wordBreak: 'break-word' }}>
                            {issue.code}
                          </Text>
                        </Table.Td>
                        <Table.Td style={{ verticalAlign: 'top' }}>
                          <Text size="xs" style={{ wordBreak: 'break-word' }}>
                            {issue.message}
                          </Text>
                        </Table.Td>
                        <Table.Td style={{ verticalAlign: 'top' }}>
                          {issue.path ? (
                            <Code
                              style={{
                                fontSize: 10,
                                display: 'inline-block',
                                maxWidth: '100%',
                                whiteSpace: 'normal',
                                wordBreak: 'break-all',
                              }}
                            >
                              {issue.path}
                            </Code>
                          ) : (
                            <Text size="xs" c="dimmed">
                              —
                            </Text>
                          )}
                        </Table.Td>
                      </Table.Tr>
                    )
                  })}
                </Table.Tbody>
              </Table>
            )}
          </Box>
        </Paper>
      )}

      <OpenGraphModal
        opened={openGraphOpen}
        onClose={() => setOpenGraphOpen(false)}
        onOpen={onOpenGraph}
      />
      <OpenMatcherModal
        opened={openMatcherForNew}
        onClose={() => setOpenMatcherForNew(false)}
        bindSharedContext={false}
        title="New graph from matcher"
        description="Run a matcher and seed a new Composer draft with the returned entities and edges. This does not change the shared graph context on Explorer, Objects, or Query."
        onApplied={(contents) => onComposerMatcherApplied(contents)}
      />
      <NewGraphModal
        opened={snapshotOpen}
        mode="snapshot"
        cloneSourceGraphId={currentGraphId}
        onClose={() => setSnapshotOpen(false)}
        onCreated={onSnapshotCreated}
      />
      <NewGraphModal
        opened={cloneOpen}
        mode="clone"
        cloneSourceGraphId={currentGraphId}
        onClose={() => setCloneOpen(false)}
        onCreated={onOpenGraph}
      />
      <Modal
        opened={overwriteOpen}
        onClose={() => setOverwriteOpen(false)}
        title="Overwrite graph"
        centered
      >
        <Stack gap="sm">
          <Text size="sm">
            Overwrite replaces the entire graph membership and edges with the current draft. This
            cannot be undone except by restoring a prior version.
          </Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setOverwriteOpen(false)}>
              Cancel
            </Button>
            <Button
              color="red"
              loading={busy}
              onClick={() => {
                setOverwriteOpen(false)
                void saveGraph('overwrite')
              }}
            >
              Overwrite
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}
