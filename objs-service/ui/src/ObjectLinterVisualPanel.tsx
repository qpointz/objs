import { forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Anchor,
  Badge,
  Button,
  Checkbox,
  Code,
  Group,
  Menu,
  Modal,
  Paper,
  ScrollArea,
  Select,
  Stack,
  Tabs,
  Text,
} from '@mantine/core'
import { getSchema, getTypeEdges, listSchemas, toGraphData } from './api'
import { colorForType, nodeLabel } from './color'
import {
  GraphCanvas,
  type GraphCanvasHandle,
  type GraphLayout,
} from './GraphCanvas'
import { edgeStatus, entityStatus, type GraphDraftState } from './graphDraft'
import { newEntityId } from './graphDraft'
import { AnnotationsEditor, SchemaInstanceForm, defaultValueForSchema } from './SchemaInstanceForm'
import type {
  BoMAllowedEdgeRule,
  BoMEdge,
  BoMEntity,
  BoMSchema,
  GraphNode,
  GraphSelection,
} from './types'

const GRAPH_LAYOUTS: { value: GraphLayout; label: string }[] = [
  { value: 'TB', label: 'Top to bottom' },
  { value: 'BT', label: 'Bottom to top' },
  { value: 'LR', label: 'Left to right' },
  { value: 'RL', label: 'Right to left' },
]

function entityToGraphNode(entity: BoMEntity): GraphNode {
  return {
    id: entity.id,
    name: nodeLabel(entity.payload, entity.id),
    type: entity.type,
    schemaVersion: entity.schemaVersion ?? '?',
    color: colorForType(entity.type),
    payload: entity.payload ?? {},
    annotations: entity.annotations ?? {},
  }
}

type Props = {
  draftState: GraphDraftState
  canvasDocument: { entities: BoMEntity[]; edges: BoMEdge[] }
  selection: GraphSelection | null
  onSelect: (selection: GraphSelection | null) => void
  onUpsertEntity: (entity: BoMEntity) => void
  onUpsertEdge: (edge: BoMEdge) => void
  onRemoveEntity: (id: string) => void
  onRemoveEdge: (id: string) => void
  onRestoreDeletedEntity: (id: string) => void
  onRestoreDeletedEdge: (id: string) => void
  onRevertEntityChanges: (id: string) => void
  onRevertEdgeChanges: (id: string) => void
}

export type ObjectLinterVisualPanelHandle = {
  focusNode: (nodeId: string) => void
}

function latestEntitySchemas(schemas: BoMSchema[]): BoMSchema[] {
  const byType = new Map<string, BoMSchema>()
  for (const schema of schemas.filter((s) => s.usages.includes('ENTITY'))) {
    const prev = byType.get(schema.type)
    if (!prev || schema.version.localeCompare(prev.version) > 0) {
      byType.set(schema.type, schema)
    }
  }
  return [...byType.values()].sort((a, b) => a.type.localeCompare(b.type))
}

export const ObjectLinterVisualPanel = forwardRef<ObjectLinterVisualPanelHandle, Props>(
  function ObjectLinterVisualPanel(
    {
      draftState,
      canvasDocument,
      selection,
      onSelect,
      onUpsertEntity,
      onUpsertEdge,
      onRemoveEntity,
      onRemoveEdge,
      onRestoreDeletedEntity,
      onRestoreDeletedEdge,
      onRevertEntityChanges,
      onRevertEdgeChanges,
    },
    ref,
  ) {
  const document = canvasDocument
  const liveDocument = draftState.document
  const graphView = useMemo(() => {
    const base = toGraphData(canvasDocument)
    return {
      nodes: base.nodes.map((n) => ({
        ...n,
        draftStatus: entityStatus(draftState, n.id),
      })),
      links: base.links.map((l) => ({
        ...l,
        draftStatus: edgeStatus(draftState, l.id),
      })),
    }
  }, [canvasDocument, draftState])
  const graphRef = useRef<GraphCanvasHandle>(null)
  useImperativeHandle(
    ref,
    () => ({
      focusNode: (nodeId: string) => {
        graphRef.current?.focusNode(nodeId)
      },
    }),
    [],
  )
  const [layout, setLayout] = useState<GraphLayout>('TB')
  const [schemas, setSchemas] = useState<BoMSchema[]>([])
  const [schemasError, setSchemasError] = useState<string | null>(null)
  const [addOpen, setAddOpen] = useState(false)
  const [linkOpen, setLinkOpen] = useState(false)
  const [connectOpen, setConnectOpen] = useState(false)
  const [addType, setAddType] = useState<string | null>(null)
  const [addVersion, setAddVersion] = useState<string | null>(null)
  const [linkOptions, setLinkOptions] = useState<LinkOption[]>([])
  const [linkOptionKey, setLinkOptionKey] = useState<string | null>(null)
  const [copyAnnotations, setCopyAnnotations] = useState(true)
  const [pairIds, setPairIds] = useState<string[]>([])
  const [connectOptions, setConnectOptions] = useState<ConnectOption[]>([])
  const [connectOptionKey, setConnectOptionKey] = useState<string | null>(null)
  const [connectBusy, setConnectBusy] = useState(false)
  const [editSchema, setEditSchema] = useState<BoMSchema | null>(null)
  const [edgePropSchema, setEdgePropSchema] = useState<BoMSchema | null>(null)

  const entitySchemaOptions = useMemo(() => latestEntitySchemas(schemas), [schemas])

  useEffect(() => {
    const ids = new Set(document.entities.map((e) => e.id))
    setPairIds((prev) => {
      const next = prev.filter((id) => ids.has(id))
      return next.length === prev.length ? prev : next
    })
  }, [document.entities])

  // Sync pair highlight when selection is restored from browser history.
  useEffect(() => {
    if (selection?.kind === 'node') {
      setPairIds((prev) => (prev.includes(selection.node.id) ? prev : [selection.node.id]))
      return
    }
    setPairIds((prev) => (prev.length === 0 ? prev : []))
  }, [selection])

  const pairIdsRef = useRef(pairIds)
  pairIdsRef.current = pairIds

  const handleSelect = useCallback(
    (sel: GraphSelection | null, meta?: { additive?: boolean }) => {
      if (!sel) {
        setPairIds([])
        onSelect(null)
        return
      }
      if (sel.kind === 'edge') {
        setPairIds([])
        onSelect(sel)
        return
      }
      const next = advancePairSelection(pairIdsRef.current, sel.node.id, meta?.additive === true)
      setPairIds(next)
      if (next.length === 0) {
        onSelect(null)
      } else if (next.length === 1) {
        const entity = document.entities.find((e) => e.id === next[0])
        onSelect(entity ? { kind: 'node', node: entityToGraphNode(entity) } : sel)
      } else {
        onSelect(sel)
      }
    },
    [document.entities, onSelect],
  )

  const selectEntityAlone = useCallback(
    (entity: BoMEntity) => {
      setPairIds([entity.id])
      onSelect({ kind: 'node', node: entityToGraphNode(entity) })
    },
    [onSelect],
  )

  const selectAndFocusEntity = useCallback(
    (entityId: string) => {
      const entity = document.entities.find((e) => e.id === entityId)
      if (!entity) return
      selectEntityAlone(entity)
      requestAnimationFrame(() => graphRef.current?.focusNode(entityId))
    },
    [document.entities, selectEntityAlone],
  )

  function endpointLabel(entityId: string): string {
    const entity = document.entities.find((e) => e.id === entityId)
    if (!entity) return entityId
    return `${nodeLabel(entity.payload, entity.id)} (${entity.type})`
  }

  useEffect(() => {
    let cancelled = false
    listSchemas('ENTITY')
      .then((list) => {
        if (!cancelled) setSchemas(list)
      })
      .catch((e) => {
        if (!cancelled) setSchemasError(e instanceof Error ? e.message : String(e))
      })
    return () => {
      cancelled = true
    }
  }, [])

  const selectedEntity = useMemo(() => {
    if (selection?.kind !== 'node') return null
    return document.entities.find((e) => e.id === selection.node.id) ?? null
  }, [document.entities, selection])

  const selectedEdge = useMemo(() => {
    if (selection?.kind !== 'edge') return null
    return document.edges.find((e) => e.id === selection.edge.id) ?? null
  }, [document.edges, selection])

  function deleteSelection() {
    if (selectedEdge?.id && pairIds.length === 0) {
      const keep =
        draftState.baselineEdgeIds.has(selectedEdge.id) &&
        !draftState.pendingDeleteEdgeIds.has(selectedEdge.id)
      onRemoveEdge(selectedEdge.id)
      if (!keep) handleSelect(null)
      return
    }
    if (pairIds.length === 0 && selectedEntity) {
      const keep =
        draftState.baselineEntityIds.has(selectedEntity.id) &&
        !draftState.pendingDeleteEntityIds.has(selectedEntity.id)
      onRemoveEntity(selectedEntity.id)
      if (!keep) handleSelect(null)
      return
    }
    if (pairIds.length > 0) {
      for (const id of [...pairIds]) {
        onRemoveEntity(id)
      }
      handleSelect(null)
    }
  }

  const selectedEntityId = selectedEntity?.id ?? null
  const selectedEntityType = selectedEntity?.type ?? null
  const selectedEntityVersion = selectedEntity?.schemaVersion ?? null
  const selectedEdgeId = selectedEdge?.id ?? null
  const selectedEdgeType = selectedEdge?.type ?? null
  const selectedEdgeVersion = selectedEdge?.schemaVersion ?? null

  useEffect(() => {
    let cancelled = false
    async function load() {
      if (!selectedEntityType || !selectedEntityVersion) {
        setEditSchema(null)
        return
      }
      try {
        const schema = await getSchema(selectedEntityType, selectedEntityVersion)
        if (!cancelled) setEditSchema(schema)
      } catch {
        if (!cancelled) setEditSchema(null)
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [selectedEntityId, selectedEntityType, selectedEntityVersion])

  useEffect(() => {
    let cancelled = false
    async function load() {
      if (!selectedEdgeType || !selectedEdgeVersion) {
        setEdgePropSchema(null)
        return
      }
      try {
        const schema = await getSchema(selectedEdgeType, selectedEdgeVersion)
        if (!cancelled) setEdgePropSchema(schema)
      } catch {
        if (!cancelled) setEdgePropSchema(null)
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [selectedEdgeId, selectedEdgeType, selectedEdgeVersion])

  const versionsForAddType = useMemo(() => {
    if (!addType) return []
    return schemas
      .filter((s) => s.type === addType && s.usages.includes('ENTITY'))
      .map((s) => s.version)
      .sort((a, b) => b.localeCompare(a))
  }, [addType, schemas])

  useEffect(() => {
    if (!addType) {
      setAddVersion(null)
      return
    }
    const latest = entitySchemaOptions.find((s) => s.type === addType)
    setAddVersion(latest?.version ?? versionsForAddType[0] ?? null)
  }, [addType, entitySchemaOptions, versionsForAddType])

  const openLink = useCallback(async () => {
    if (!selectedEntity) return
    if (draftState.pendingDeleteEntityIds.has(selectedEntity.id)) return
    setLinkOpen(true)
    setLinkOptionKey(null)
    setLinkOptions([])
    try {
      const edges = await getTypeEdges(selectedEntity.type)
      setLinkOptions(
        buildLinkOptions(
          selectedEntity,
          edges.outgoing,
          edges.incoming,
          liveDocument.edges,
          liveDocument.entities,
        ),
      )
    } catch {
      setLinkOptions([])
    }
  }, [draftState.pendingDeleteEntityIds, liveDocument.edges, liveDocument.entities, selectedEntity])

  const openConnect = useCallback(async () => {
    if (pairIds.length !== 2) return
    if (pairIds.some((id) => draftState.pendingDeleteEntityIds.has(id))) return
    const a = liveDocument.entities.find((e) => e.id === pairIds[0])
    const b = liveDocument.entities.find((e) => e.id === pairIds[1])
    if (!a || !b) return
    setConnectOpen(true)
    setConnectOptionKey(null)
    setConnectOptions([])
    setConnectBusy(true)
    try {
      const [edgesA, edgesB] = await Promise.all([getTypeEdges(a.type), getTypeEdges(b.type)])
      setConnectOptions(
        buildConnectOptions(a, b, edgesA.outgoing, edgesB.outgoing, liveDocument.edges),
      )
    } catch {
      setConnectOptions([])
    } finally {
      setConnectBusy(false)
    }
  }, [draftState.pendingDeleteEntityIds, liveDocument.edges, liveDocument.entities, pairIds])

  async function confirmAdd() {
    if (!addType || !addVersion) return
    const schema = await getSchema(addType, addVersion)
    const payload = defaultValueForSchema(schema.contentSchema) as Record<string, unknown>
    const entity: BoMEntity = {
      id: newEntityId(),
      type: addType,
      schemaVersion: addVersion,
      payload,
      annotations: {},
    }
    onUpsertEntity(entity)
    selectEntityAlone(entity)
    setAddOpen(false)
  }

  async function confirmLinked() {
    if (!selectedEntity || !linkOptionKey) return
    const option = linkOptions.find((o) => o.key === linkOptionKey)
    if (!option || option.createType === '*') return
    if (linkOptionOccupied(option, selectedEntity, liveDocument.edges, liveDocument.entities)) {
      return
    }
    const latest = entitySchemaOptions.find((s) => s.type === option.createType)
    const version = latest?.version
    if (!version) return
    const schema = await getSchema(option.createType, version)
    const createdId = newEntityId()
    const entity: BoMEntity = {
      id: createdId,
      type: option.createType,
      schemaVersion: version,
      payload: defaultValueForSchema(schema.contentSchema) as Record<string, unknown>,
      annotations: copyAnnotations ? { ...(selectedEntity.annotations ?? {}) } : {},
    }
    const edge: BoMEdge = {
      id: newEntityId(),
      source: option.direction === 'out' ? selectedEntity.id : createdId,
      target: option.direction === 'out' ? createdId : selectedEntity.id,
      role: option.rule.role,
      type: option.rule.propertiesSchemaType ?? undefined,
      schemaVersion: option.rule.propertiesSchemaVersion ?? undefined,
      properties: {},
    }
    onUpsertEntity(entity)
    onUpsertEdge(edge)
    selectEntityAlone(entity)
    setLinkOpen(false)
  }

  function confirmConnect() {
    if (!connectOptionKey) return
    const option = connectOptions.find((o) => o.key === connectOptionKey)
    if (!option) return
    const alreadyExists = liveDocument.edges.some(
      (e) =>
        e.source === option.sourceId &&
        e.target === option.targetId &&
        e.role === option.rule.role,
    )
    if (alreadyExists) return
    const target = liveDocument.entities.find((e) => e.id === option.targetId)
    onUpsertEdge({
      id: newEntityId(),
      source: option.sourceId,
      target: option.targetId,
      role: option.rule.role,
      type: option.rule.propertiesSchemaType ?? undefined,
      schemaVersion: option.rule.propertiesSchemaVersion ?? undefined,
      properties: {},
    })
    if (target) selectEntityAlone(target)
    setConnectOpen(false)
  }

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group style={{ flexShrink: 0 }}>
        <Button size="xs" onClick={() => setAddOpen(true)}>
          Add object
        </Button>
        <Button
          size="xs"
          variant="light"
          disabled={
            pairIds.length !== 1 ||
            !selectedEntity ||
            draftState.pendingDeleteEntityIds.has(selectedEntity.id)
          }
          onClick={() => void openLink()}
        >
          Create linked
        </Button>
        <Button
          size="xs"
          variant="light"
          disabled={
            pairIds.length !== 2 ||
            pairIds.some((id) => draftState.pendingDeleteEntityIds.has(id))
          }
          onClick={() => void openConnect()}
        >
          Connect existing
        </Button>
        <Button
          size="xs"
          variant="light"
          color="red"
          disabled={pairIds.length === 0 && !selectedEdge}
          onClick={deleteSelection}
        >
          Delete
        </Button>
        <Group gap={0}>
          <Button
            size="xs"
            variant="light"
            disabled={graphView.nodes.length === 0}
            onClick={() => graphRef.current?.applyLayout(layout)}
            style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
          >
            Apply layout
          </Button>
          <Menu position="bottom-end" withinPortal>
            <Menu.Target>
              <Button
                size="xs"
                variant="light"
                disabled={graphView.nodes.length === 0}
                aria-label="Choose graph layout"
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
              <Menu.Label>Layout direction</Menu.Label>
              {GRAPH_LAYOUTS.map((option) => (
                <Menu.Item
                  key={option.value}
                  onClick={() => {
                    setLayout(option.value)
                    if (option.value === layout) {
                      graphRef.current?.applyLayout(option.value)
                    } else {
                      requestAnimationFrame(() => graphRef.current?.applyLayout(option.value))
                    }
                  }}
                >
                  {option.value === layout ? '✓ ' : ''}
                  {option.label}
                </Menu.Item>
              ))}
            </Menu.Dropdown>
          </Menu>
        </Group>
        {pairIds.length === 2 && (
          <Text size="xs" c="dimmed">
            2 selected (Ctrl+click to adjust)
          </Text>
        )}
      </Group>

      {schemasError && (
        <Alert color="red" title="Cannot load schemas">
          {schemasError}
        </Alert>
      )}

      <Group align="stretch" gap="sm" wrap="nowrap" style={{ flex: 1, minHeight: 0 }}>
        <Paper
          withBorder
          style={{ flex: 1, minWidth: 0, minHeight: 0, overflow: 'hidden', position: 'relative' }}
        >
          {graphView.nodes.length === 0 ? (
            <Stack p="md" gap="xs">
              <Text size="sm" c="dimmed">
                Draft has no entities. Use Add object or Load a subgraph.
              </Text>
            </Stack>
          ) : (
            <GraphCanvas
              ref={graphRef}
              nodes={graphView.nodes}
              links={graphView.links}
              selection={selection}
              onSelect={handleSelect}
              layout={layout}
              autoLayoutOnDataChange={false}
              highlightedNodeIds={pairIds}
            />
          )}
        </Paper>

        <Paper
          withBorder
          p="sm"
          style={{
            width: 360,
            flexShrink: 0,
            minHeight: 0,
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
          }}
        >
          {pairIds.length > 1 ? (
            <Stack gap="sm">
              <Group justify="space-between" align="flex-start">
                <Text fw={600}>Multiple selection</Text>
                <Button size="compact-xs" variant="subtle" onClick={() => handleSelect(null)}>
                  Clear
                </Button>
              </Group>
              <Text size="sm" c="dimmed">
                Editing is disabled while two objects are selected. Use Connect existing or Delete
                in the toolbar, or click a single object to edit.
              </Text>
              <Stack gap={4}>
                {pairIds.map((id) => {
                  const entity = document.entities.find((e) => e.id === id)
                  return (
                    <Text key={id} size="sm">
                      <Code>{entity?.type ?? '?'}</Code> {id.slice(0, 8)}…
                    </Text>
                  )
                })}
              </Stack>
            </Stack>
          ) : !selectedEntity && !selectedEdge ? (
            <Text size="sm" c="dimmed">
              Select a node or edge to edit. Ctrl+click a second node to connect two objects.
            </Text>
          ) : (
            <ScrollArea style={{ flex: 1, minHeight: 0 }} offsetScrollbars>
              {selectedEntity && (
                <Stack gap="xs">
                  <Group justify="space-between" align="flex-start" gap="xs">
                    <div>
                      <Text fw={600} size="sm">
                        Edit {selectedEntity.type}
                      </Text>
                      <Text size="xs" c="dimmed">
                        {selectedEntity.schemaVersion}
                      </Text>
                    </div>
                    <Button size="compact-xs" variant="subtle" onClick={() => handleSelect(null)}>
                      Close
                    </Button>
                  </Group>
                  <Group gap={6}>
                    <Text size="xs">
                      <Code>{selectedEntity.id}</Code>
                    </Text>
                    {entityStatus(draftState, selectedEntity.id) === 'new' && (
                      <Badge size="xs" color="teal" variant="filled">
                        new
                      </Badge>
                    )}
                    {entityStatus(draftState, selectedEntity.id) === 'modified' && (
                      <Badge size="xs" color="orange" variant="filled">
                        modified
                      </Badge>
                    )}
                    {entityStatus(draftState, selectedEntity.id) === 'deleted' && (
                      <Badge size="xs" color="red" variant="filled">
                        deleted
                      </Badge>
                    )}
                  </Group>
                  {entityStatus(draftState, selectedEntity.id) === 'deleted' ? (
                    <Stack gap="xs">
                      <Text size="sm" c="dimmed">
                        This loaded object is marked for delete. It stays on the canvas until Apply,
                        or you can undo the delete.
                      </Text>
                      <Button
                        size="xs"
                        variant="light"
                        onClick={() => onRestoreDeletedEntity(selectedEntity.id)}
                      >
                        Undo delete
                      </Button>
                    </Stack>
                  ) : (
                    <Stack gap="xs">
                      {entityStatus(draftState, selectedEntity.id) === 'modified' && (
                        <Button
                          size="xs"
                          variant="light"
                          onClick={() => onRevertEntityChanges(selectedEntity.id)}
                        >
                          Undo modifications
                        </Button>
                      )}
                      <Tabs defaultValue="payload" keepMounted={false}>
                        <Tabs.List>
                          <Tabs.Tab value="payload">Payload</Tabs.Tab>
                          <Tabs.Tab value="annotations">Annotations</Tabs.Tab>
                        </Tabs.List>
                        <Tabs.Panel value="payload" pt="xs">
                          {editSchema ? (
                            <SchemaInstanceForm
                              schema={editSchema.contentSchema}
                              value={(selectedEntity.payload ?? {}) as Record<string, unknown>}
                              onChange={(payload) =>
                                onUpsertEntity({ ...selectedEntity, payload })
                              }
                              compact
                            />
                          ) : (
                            <Text size="xs" c="dimmed">
                              Schema not available for form editing.
                            </Text>
                          )}
                        </Tabs.Panel>
                        <Tabs.Panel value="annotations" pt="xs">
                          <AnnotationsEditor
                            value={selectedEntity.annotations ?? {}}
                            onChange={(annotations) =>
                              onUpsertEntity({ ...selectedEntity, annotations })
                            }
                            compact
                          />
                        </Tabs.Panel>
                      </Tabs>
                    </Stack>
                  )}
                </Stack>
              )}
              {selectedEdge && !selectedEntity && (
                <Stack gap="xs">
                  <Group justify="space-between" align="flex-start" gap="xs">
                    <Text fw={600} size="sm">
                      Edge {selectedEdge.role}
                    </Text>
                    <Button size="compact-xs" variant="subtle" onClick={() => handleSelect(null)}>
                      Close
                    </Button>
                  </Group>
                  <Group gap={6}>
                    <Text size="xs">
                      <Code>{selectedEdge.id}</Code>
                    </Text>
                    {selectedEdge.id && edgeStatus(draftState, selectedEdge.id) === 'new' && (
                      <Badge size="xs" color="teal" variant="filled">
                        new
                      </Badge>
                    )}
                    {selectedEdge.id && edgeStatus(draftState, selectedEdge.id) === 'modified' && (
                      <Badge size="xs" color="orange" variant="filled">
                        modified
                      </Badge>
                    )}
                    {selectedEdge.id && edgeStatus(draftState, selectedEdge.id) === 'deleted' && (
                      <Badge size="xs" color="red" variant="filled">
                        deleted
                      </Badge>
                    )}
                  </Group>
                  <Text size="xs" c="dimmed">
                    endpoints
                  </Text>
                  <Stack gap={2}>
                    <Group gap={6} wrap="nowrap" align="flex-start">
                      <Text size="xs" fw={600} style={{ flexShrink: 0 }}>
                        source:
                      </Text>
                      <Anchor
                        component="button"
                        type="button"
                        size="xs"
                        ta="left"
                        style={{ wordBreak: 'break-all' }}
                        onClick={() => selectAndFocusEntity(selectedEdge.source)}
                      >
                        {endpointLabel(selectedEdge.source)}
                      </Anchor>
                    </Group>
                    <Group gap={6} wrap="nowrap" align="flex-start">
                      <Text size="xs" fw={600} style={{ flexShrink: 0 }}>
                        target:
                      </Text>
                      <Anchor
                        component="button"
                        type="button"
                        size="xs"
                        ta="left"
                        style={{ wordBreak: 'break-all' }}
                        onClick={() => selectAndFocusEntity(selectedEdge.target)}
                      >
                        {endpointLabel(selectedEdge.target)}
                      </Anchor>
                    </Group>
                  </Stack>
                  <Text size="xs" c="dimmed">
                    role: {selectedEdge.role}
                  </Text>
                  {selectedEdge.id && edgeStatus(draftState, selectedEdge.id) === 'deleted' ? (
                    <Button
                      size="xs"
                      variant="light"
                      onClick={() => onRestoreDeletedEdge(selectedEdge.id!)}
                    >
                      Undo delete
                    </Button>
                  ) : (
                    <>
                      {selectedEdge.id &&
                        edgeStatus(draftState, selectedEdge.id) === 'modified' && (
                          <Button
                            size="xs"
                            variant="light"
                            onClick={() => onRevertEdgeChanges(selectedEdge.id!)}
                          >
                            Undo modifications
                          </Button>
                        )}
                      {edgePropSchema ? (
                        <SchemaInstanceForm
                          schema={edgePropSchema.contentSchema}
                          value={(selectedEdge.properties ?? {}) as Record<string, unknown>}
                          onChange={(properties) =>
                            onUpsertEdge({ ...selectedEdge, properties })
                          }
                          compact
                        />
                      ) : (
                        <Text size="xs" c="dimmed">
                          No property schema for this edge (NONE policy or schema missing).
                        </Text>
                      )}
                    </>
                  )}
                </Stack>
              )}
            </ScrollArea>
          )}
        </Paper>
      </Group>

      <Modal opened={addOpen} onClose={() => setAddOpen(false)} title="Add object">
        <Stack>
          <Select
            label="Type"
            searchable
            data={entitySchemaOptions.map((s) => s.type)}
            value={addType}
            onChange={setAddType}
          />
          <Select
            label="Schema version"
            data={versionsForAddType}
            value={addVersion}
            onChange={setAddVersion}
            disabled={!addType}
          />
          <Button disabled={!addType || !addVersion} onClick={() => void confirmAdd()}>
            Create
          </Button>
        </Stack>
      </Modal>

      <Modal opened={linkOpen} onClose={() => setLinkOpen(false)} title="Create linked object">
        <Stack>
          {linkOptions.length === 0 ? (
            <Alert color="orange" title="No available relations">
              No allow-listed incoming/outgoing relations left for this object (or all matching
              relations already exist).
            </Alert>
          ) : (
            <>
              <Select
                label="Allowed relation"
                data={linkOptions.map((o) => ({
                  value: o.key,
                  label: `${o.label} (${o.rule.cardinality ?? 'UNSPECIFIED'})`,
                }))}
                value={linkOptionKey}
                onChange={setLinkOptionKey}
                searchable
              />
              <Checkbox
                label="Copy annotations from selected"
                checked={copyAnnotations}
                onChange={(e) => setCopyAnnotations(e.currentTarget.checked)}
              />
              <Button disabled={!linkOptionKey} onClick={() => void confirmLinked()}>
                Create linked
              </Button>
            </>
          )}
        </Stack>
      </Modal>

      <Modal opened={connectOpen} onClose={() => setConnectOpen(false)} title="Connect existing">
        <Stack>
          {connectBusy ? (
            <Text size="sm" c="dimmed">
              Loading allowed edges…
            </Text>
          ) : connectOptions.length === 0 ? (
            <Alert color="orange" title="Connection not possible">
              No defined edges between the selected objects.
            </Alert>
          ) : (
            <>
              <Select
                label="Allowed relation"
                data={connectOptions.map((o) => ({
                  value: o.key,
                  label: `${o.label} (${o.rule.cardinality ?? 'UNSPECIFIED'})`,
                }))}
                value={connectOptionKey}
                onChange={setConnectOptionKey}
                searchable
              />
              <Button disabled={!connectOptionKey} onClick={confirmConnect}>
                Connect
              </Button>
            </>
          )}
        </Stack>
      </Modal>
    </Stack>
  )
  },
)

function ruleKey(rule: BoMAllowedEdgeRule): string {
  return `${rule.sourceType}|${rule.role}|${rule.targetType}`
}

function ruleMatchesEndpoint(ruleType: string, entityType: string): boolean {
  return ruleType === '*' || ruleType === entityType
}

type LinkOption = {
  key: string
  direction: 'out' | 'in'
  rule: BoMAllowedEdgeRule
  /** Entity type to create (other end of the relation). */
  createType: string
  label: string
}

function edgeExists(
  edges: BoMEdge[],
  sourceId: string,
  targetId: string,
  role: string,
): boolean {
  return edges.some((e) => e.source === sourceId && e.target === targetId && e.role === role)
}

function entityTypeById(entities: BoMEntity[], id: string): string | undefined {
  return entities.find((e) => e.id === id)?.type
}

/**
 * Hide Create-linked options when a 1:1 slot from/to the selected entity is already taken
 * for this rule's peer type. Exact (source, target, role) uniqueness does not apply here
 * because create-linked always allocates a new peer id; Connect existing handles that case.
 */
function linkOptionOccupied(
  option: LinkOption,
  selected: BoMEntity,
  existingEdges: BoMEdge[],
  entities: BoMEntity[],
): boolean {
  if (option.rule.cardinality !== '1:1') return false
  if (option.direction === 'out') {
    return existingEdges.some(
      (e) =>
        e.source === selected.id &&
        e.role === option.rule.role &&
        ruleMatchesEndpoint(option.rule.targetType, entityTypeById(entities, e.target) ?? ''),
    )
  }
  return existingEdges.some(
    (e) =>
      e.target === selected.id &&
      e.role === option.rule.role &&
      ruleMatchesEndpoint(option.rule.sourceType, entityTypeById(entities, e.source) ?? ''),
  )
}

function buildLinkOptions(
  selected: BoMEntity,
  outgoing: BoMAllowedEdgeRule[],
  incoming: BoMAllowedEdgeRule[],
  existingEdges: BoMEdge[],
  entities: BoMEntity[],
): LinkOption[] {
  const options: LinkOption[] = []
  const seen = new Set<string>()

  for (const rule of outgoing) {
    if (!ruleMatchesEndpoint(rule.sourceType, selected.type)) continue
    if (rule.targetType === '*') continue
    const key = `out|${ruleKey(rule)}`
    if (seen.has(key)) continue
    seen.add(key)
    options.push({
      key,
      direction: 'out',
      rule,
      createType: rule.targetType,
      label: `${selected.type} —${rule.role}→ ${rule.targetType}`,
    })
  }

  for (const rule of incoming) {
    if (!ruleMatchesEndpoint(rule.targetType, selected.type)) continue
    if (rule.sourceType === '*') continue
    const key = `in|${ruleKey(rule)}`
    if (seen.has(key)) continue
    seen.add(key)
    options.push({
      key,
      direction: 'in',
      rule,
      createType: rule.sourceType,
      label: `${rule.sourceType} —${rule.role}→ ${selected.type}`,
    })
  }

  return options.filter((opt) => !linkOptionOccupied(opt, selected, existingEdges, entities))
}

type ConnectOption = {
  key: string
  rule: BoMAllowedEdgeRule
  sourceId: string
  targetId: string
  label: string
}

function buildConnectOptions(
  a: BoMEntity,
  b: BoMEntity,
  outgoingA: BoMAllowedEdgeRule[],
  outgoingB: BoMAllowedEdgeRule[],
  existingEdges: BoMEdge[],
): ConnectOption[] {
  const options: ConnectOption[] = []
  for (const rule of outgoingA) {
    if (!ruleMatchesEndpoint(rule.sourceType, a.type)) continue
    if (!ruleMatchesEndpoint(rule.targetType, b.type)) continue
    options.push({
      key: `ab|${ruleKey(rule)}`,
      rule,
      sourceId: a.id,
      targetId: b.id,
      label: `${a.type} —${rule.role}→ ${b.type}`,
    })
  }
  for (const rule of outgoingB) {
    if (!ruleMatchesEndpoint(rule.sourceType, b.type)) continue
    if (!ruleMatchesEndpoint(rule.targetType, a.type)) continue
    const key = `ba|${ruleKey(rule)}`
    if (options.some((o) => o.key === key)) continue
    options.push({
      key,
      rule,
      sourceId: b.id,
      targetId: a.id,
      label: `${b.type} —${rule.role}→ ${a.type}`,
    })
  }
  return options.filter(
    (opt) => !edgeExists(existingEdges, opt.sourceId, opt.targetId, opt.rule.role),
  )
}

function advancePairSelection(prev: string[], id: string, additive: boolean): string[] {
  if (!additive) return [id]
  if (prev.includes(id)) return prev.filter((x) => x !== id)
  if (prev.length === 0) return [id]
  if (prev.length === 1) return [prev[0], id]
  return [prev[1], id]
}
