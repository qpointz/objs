import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Badge,
  Box,
  Button,
  Code,
  Group,
  Menu,
  Paper,
  Stack,
  Table,
  Tabs,
  Text,
  Title,
} from '@mantine/core'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { mutationShapeError, normalizeGraphMutation } from './graphDraft'
import { putGraphMutation, validateGraphMutation, toGraphData } from './api'
import { CreateSubgraphModal, type CreateSubgraphMode } from './CreateSubgraphModal'
import { JsonYamlEditor, type JsonYamlEditorHandle } from './JsonYamlEditor'
import { NewUuidButton } from './NewUuidButton'
import {
  ObjectLinterVisualPanel,
  type ObjectLinterVisualPanelHandle,
} from './ObjectLinterVisualPanel'
import type { QueryExecStats } from './queryExecStats'
import { SubgraphPacksModal } from './SubgraphPacksModal'
import type { BoMSubgraph, BoMValidationIssue, GraphValidationResult } from './types'
import { useGraphDraft } from './useGraphDraft'
import { useGraphSelectionHistory } from './useGraphSelectionHistory'
import {
  edgeIdsFromValidationIssues,
  entityIdsFromValidationIssues,
  validationTargetFromIssue,
} from './validationIssueTargets'

export { graphShapeError, mutationShapeError } from './graphDraft'

type ObjectLinterNavState = {
  matcher?: unknown
  /** Explorer handoff: merge all canvas objects (and edges) into the draft. */
  addAll?: boolean
  /** Explorer canvas snapshot — preferred over re-running matcher. */
  subgraph?: BoMSubgraph
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
    loadSubgraph,
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
  const [packsOpen, setPacksOpen] = useState(false)
  const [createPack, setCreatePack] = useState<CreateSubgraphMode | null>(null)
  const [handoffMatcher, setHandoffMatcher] = useState<unknown | null>(null)
  const [autoSearch, setAutoSearch] = useState(false)
  const [autoAddAllResults, setAutoAddAllResults] = useState(false)
  const [addObjectsStats, setAddObjectsStats] = useState<QueryExecStats | null>(null)

  const draftSubgraph = useMemo<BoMSubgraph>(
    () => ({ entities: document.entities, edges: document.edges }),
    [document.entities, document.edges],
  )

  const graphView = useMemo(() => toGraphData(canvasDocument), [canvasDocument])
  const onFocusNode = useCallback((nodeId: string) => {
    visualRef.current?.focusNode(nodeId)
  }, [])
  const { selection, select, beginQueryResult, clearQuery } = useGraphSelectionHistory({
    nodes: graphView.nodes,
    links: graphView.links,
    onFocusNode,
  })

  const openPack = useCallback(
    (subgraph: BoMSubgraph) => {
      loadSubgraph(subgraph)
      clearQuery()
      setResult(null)
      setError(null)
    },
    [clearQuery, loadSubgraph],
  )

  const draftEntityIds = useMemo(
    () => new Set(document.entities.map((e) => e.id)),
    [document.entities],
  )

  const invalidEntityIds = useMemo(() => {
    if (!result || result.issues.length === 0) return new Set<string>()
    return entityIdsFromValidationIssues(
      result.issues,
      mutationBody.upsert.entities,
      mutationBody.upsert.edges,
    )
  }, [mutationBody.upsert.edges, mutationBody.upsert.entities, result])

  const invalidEdgeIds = useMemo(() => {
    if (!result || result.issues.length === 0) return new Set<string>()
    return edgeIdsFromValidationIssues(
      result.issues,
      mutationBody.upsert.entities,
      mutationBody.upsert.edges,
    )
  }, [mutationBody.upsert.edges, mutationBody.upsert.entities, result])

  const focusValidationIssue = useCallback(
    (issue: BoMValidationIssue) => {
      const target = validationTargetFromIssue(
        issue,
        mutationBody.upsert.entities,
        mutationBody.upsert.edges,
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
    [graphView.links, graphView.nodes, mutationBody.upsert.edges, mutationBody.upsert.entities, select],
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
      setResult(await validateGraphMutation(synced.body))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  async function saveGraph() {
    const synced = await syncTextIntoDraft()
    if (!synced) return
    setBusy(true)
    setError(null)
    setResult(null)
    try {
      const validation = await validateGraphMutation(synced.body)
      setResult(validation)
      if (validation.issues.length > 0) {
        setError('Fix validation issues before Save')
        return
      }
      await putGraphMutation(synced.body)
      markApplied()
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
    const hasSubgraph = navState.subgraph != null && typeof navState.subgraph === 'object'
    if (!hasMatcher && !hasSubgraph) return

    const matcher = hasMatcher ? navState.matcher : undefined
    const addAll = navState.addAll === true
    const subgraph = hasSubgraph ? navState.subgraph : undefined
    navigate('.', { replace: true, state: null })

    if (addAll && subgraph) {
      const entities = subgraph.entities ?? []
      const edges = subgraph.edges ?? []
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
  }, [location.state, navigate, mergeEntities, mergeEdges])

  const valid = result != null && result.issues.length === 0

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group justify="space-between" align="flex-start" style={{ flexShrink: 0 }}>
        <div>
          <Title order={3}>Object linter</Title>
          <Text size="sm" c="dimmed">
            Add objects from the store into the draft, edit visually or as YAML/JSON, then Validate
            or Save (transactional upsert + delete). Subgraph / Snapshot pack the draft.
          </Text>
        </div>
        <Group>
          <Button variant="default" component={Link} to="/model">
            Browse schemas
          </Button>
          <Button
            variant="light"
            onClick={() => (addObjectsOpen ? closeAddObjects() : openAddObjects())}
          >
            {addObjectsOpen ? 'Hide add objects' : 'Add objects…'}
          </Button>
          <Button variant="light" onClick={() => setPacksOpen(true)}>
            Open packs…
          </Button>
          <Button variant="default" onClick={resetToRollback}>
            Reset
          </Button>
          <Button
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
          <Button loading={busy} variant="light" onClick={() => void validate()}>
            Validate
          </Button>
          <Group gap={0} wrap="nowrap" style={{ display: 'inline-flex' }}>
            <Button
              loading={busy}
              onClick={() => void saveGraph()}
              style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
            >
              Save
            </Button>
            <Menu shadow="md" width={200} position="bottom-end" withinPortal>
              <Menu.Target>
                <Button
                  loading={busy}
                  aria-label="Save options"
                  px="xs"
                  style={{
                    borderTopLeftRadius: 0,
                    borderBottomLeftRadius: 0,
                    borderLeft: '1px solid var(--mantine-color-default-border)',
                  }}
                >
                  ▾
                </Button>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item onClick={() => void saveGraph()}>Save</Menu.Item>
                <Menu.Item onClick={() => setCreatePack('soft')}>Subgraph</Menu.Item>
                <Menu.Item onClick={() => setCreatePack('hard')}>Snapshot</Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
        </Group>
      </Group>

      <Tabs
        value={tab}
        onChange={trySwitchTab}
        style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', position: 'relative' }}
      >
        <Group justify="space-between" align="center" gap="sm" wrap="nowrap" style={{ flexShrink: 0 }}>
          <Tabs.List style={{ flex: 1 }}>
            <Tabs.Tab value="visual">Visual</Tabs.Tab>
            <Tabs.Tab value="text">Text</Tabs.Tab>
          </Tabs.List>
          <Group gap={6} wrap="nowrap">
            {mutationBody.upsert.entities.length + mutationBody.upsert.edges.length > 0 && (
              <Badge color="blue" variant="light" size="sm">
                {mutationBody.upsert.entities.length + mutationBody.upsert.edges.length} upsert
                {mutationBody.upsert.entities.length + mutationBody.upsert.edges.length === 1
                  ? ''
                  : 's'}
              </Badge>
            )}
            {pendingDeleteCount > 0 && (
              <Badge color="orange" variant="filled" size="sm">
                {pendingDeleteCount} pending delete{pendingDeleteCount === 1 ? '' : 's'}
              </Badge>
            )}
            <Badge variant="light" size="sm">
              {canvasDocument.entities.length} on canvas
            </Badge>
            {addObjectsStats != null && (
              <Badge variant="outline" size="sm">
                last search {addObjectsStats.nodes} nodes
              </Badge>
            )}
          </Group>
        </Group>

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
                      mutationBody.upsert.entities,
                      mutationBody.upsert.edges,
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

      <SubgraphPacksModal
        opened={packsOpen}
        onClose={() => setPacksOpen(false)}
        onOpenPack={openPack}
      />
      <CreateSubgraphModal
        opened={createPack != null}
        mode={createPack ?? 'soft'}
        draftSubgraph={draftSubgraph}
        onClose={() => setCreatePack(null)}
        onHardCreated={openPack}
      />
    </Stack>
  )
}
