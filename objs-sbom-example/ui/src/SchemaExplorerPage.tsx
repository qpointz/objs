import { useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Group,
  Loader,
  Menu,
  Modal,
  Paper,
  ScrollArea,
  Select,
  Stack,
  Tabs,
  Text,
  TextInput,
  Title,
  UnstyledButton,
} from '@mantine/core'
import { useBlocker, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  createNextMajorSchema,
  deleteEdge,
  getJsonSchema,
  getSchema,
  getTypeEdges,
  listSchemas,
  lintSchema,
  putEdge,
  schemaCreatePath,
  schemaDetailPath,
  updateSchema,
} from './api'
import { JsonYamlEditor, type JsonYamlEditorHandle } from './JsonYamlEditor'
import { ObjectEdgesEditor } from './ObjectEdgesEditor'
import { SchemaCatalogOverview } from './SchemaCatalogOverview'
import { parseSchemaExpertDocument, type SchemaExpertDocument } from './SchemaLinterPage'
import {
  allowedEdgeKey,
  cloneEdgeRules,
  edgeRulesEqual,
  edgesForType,
  uniqueEdgeRules,
  type AllowedEdgeRef,
} from './allowedEdgeRef'
import { SchemaRelationshipGraph } from './SchemaRelationshipGraph'
import { SchemaVisualBuilder } from './SchemaVisualBuilder'
import { emptyObjectSchema } from './schemaDsl'
import { SyntaxCodeEditor } from './SyntaxCodeEditor'
import type {
  BoMAllowedEdgeRule,
  BoMSchema,
  BoMSchemaNode,
  BoMSchemaUsage,
  SchemaLintResponse,
  TypeEdgesResponse,
} from './types'

function latestVersion(versions: string[]): string {
  return [...versions].sort().at(-1) ?? '1.0.0'
}

function primaryKind(usages: BoMSchemaUsage[]): 'object' | 'edge' {
  return usages.includes('ENTITY') ? 'object' : 'edge'
}

function KindPill({ kind }: { kind: 'object' | 'edge' }) {
  return (
    <Badge size="xs" variant="light" color={kind === 'object' ? 'blue' : 'grape'} w={28} px={0}>
      {kind === 'object' ? 'O' : 'E'}
    </Badge>
  )
}

function expertDoc(
  type: string,
  version: string,
  usages: BoMSchemaUsage[],
  contentSchema: BoMSchemaNode,
): SchemaExpertDocument {
  return { type, version, usages, contentSchema, allowedRelations: [] }
}

function cloneDoc(doc: SchemaExpertDocument): SchemaExpertDocument {
  return JSON.parse(JSON.stringify(doc)) as SchemaExpertDocument
}

export function SchemaExplorerPage() {
  const params = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const selectedType = params.type ? decodeURIComponent(params.type) : undefined
  const selectedVersion = params.version ? decodeURIComponent(params.version) : undefined
  const isNewDraft = selectedType === 'new'
  const createKind = (searchParams.get('kind') === 'edge' ? 'edge' : 'object') as 'object' | 'edge'
  const createVersionMode = searchParams.get('mode') === 'create-version'

  const editorRef = useRef<JsonYamlEditorHandle>(null)
  const [search, setSearch] = useState('')
  const [schemas, setSchemas] = useState<BoMSchema[]>([])
  const [selected, setSelected] = useState<BoMSchema | null>(null)
  const [jsonSchema, setJsonSchema] = useState<Record<string, unknown> | null>(null)
  const [edgeRules, setEdgeRules] = useState<BoMAllowedEdgeRule[] | null>(null)
  const [baselineEdgeRules, setBaselineEdgeRules] = useState<BoMAllowedEdgeRule[]>([])
  const [highlightedEdge, setHighlightedEdge] = useState<AllowedEdgeRef | null>(null)
  const loadedDetailKeyRef = useRef<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lint, setLint] = useState<SchemaLintResponse | null>(null)
  const [editorMode, setEditorMode] = useState<'visual' | 'schema' | 'expert' | 'json-schema'>('visual')
  const [dirty, setDirty] = useState(false)
  const [expertDirty, setExpertDirty] = useState(false)

  const [typeName, setTypeName] = useState('NewType')
  const [version, setVersion] = useState('1.0.0')
  const [usages, setUsages] = useState<BoMSchemaUsage[]>(['ENTITY'])
  const [contentSchema, setContentSchema] = useState<BoMSchemaNode>(emptyObjectSchema())
  const [expertSnapshot, setExpertSnapshot] = useState<SchemaExpertDocument>(() =>
    expertDoc('NewType', '1.0.0', ['ENTITY'], emptyObjectSchema()),
  )
  const [baseline, setBaseline] = useState<SchemaExpertDocument>(() =>
    cloneDoc(expertDoc('NewType', '1.0.0', ['ENTITY'], emptyObjectSchema())),
  )

  const hasUnsavedChanges = dirty || expertDirty
  const unsavedRef = useRef(false)
  unsavedRef.current = hasUnsavedChanges

  function markClean() {
    unsavedRef.current = false
    setDirty(false)
    setExpertDirty(false)
  }

  function captureBaseline(doc: SchemaExpertDocument, rules: BoMAllowedEdgeRule[] = baselineEdgeRules) {
    setBaseline(cloneDoc(doc))
    setBaselineEdgeRules(cloneEdgeRules(rules))
  }

  function applyDocument(doc: SchemaExpertDocument) {
    setTypeName(doc.type)
    setVersion(doc.version)
    setUsages([...doc.usages])
    setContentSchema(cloneDoc(doc).contentSchema)
    setExpertSnapshot(cloneDoc(doc))
  }

  function rollbackUnsaved() {
    applyDocument(baseline)
    setEdgeRules(cloneEdgeRules(baselineEdgeRules))
    setHighlightedEdge(null)
    setLint(null)
    markClean()
  }

  function requestNavigate(to: string) {
    navigate(to)
  }

  const edges: TypeEdgesResponse | null = useMemo(() => {
    if (!edgeRules || !selectedType || isNewDraft) return null
    return edgesForType(selectedType, edgeRules)
  }, [edgeRules, selectedType, isNewDraft])

  const detailKey = isNewDraft
    ? `new:${createKind}`
    : selectedType && selectedVersion
      ? `${selectedType}@${selectedVersion}`
      : selectedType
        ? `${selectedType}@pending`
        : null

  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      unsavedRef.current &&
      (currentLocation.pathname !== nextLocation.pathname ||
        currentLocation.search !== nextLocation.search),
  )

  async function reloadSchemas() {
    const list = await listSchemas()
    setSchemas(list)
    return list
  }

  async function persistEdgeChanges(
    baseline: BoMAllowedEdgeRule[],
    draft: BoMAllowedEdgeRule[],
  ) {
    const baseMap = new Map(baseline.map((rule) => [allowedEdgeKey(rule), rule]))
    const draftMap = new Map(draft.map((rule) => [allowedEdgeKey(rule), rule]))
    for (const [key, rule] of draftMap) {
      const previous = baseMap.get(key)
      if (!previous || !edgeRulesEqual(previous, rule)) {
        await putEdge(rule)
      }
    }
    for (const [key, rule] of baseMap) {
      if (!draftMap.has(key)) {
        await deleteEdge(rule.sourceType, rule.role, rule.targetType)
      }
    }
  }

  useEffect(() => {
    const onBeforeUnload = (event: BeforeUnloadEvent) => {
      if (!hasUnsavedChanges) return
      event.preventDefault()
      event.returnValue = ''
    }
    window.addEventListener('beforeunload', onBeforeUnload)
    return () => window.removeEventListener('beforeunload', onBeforeUnload)
  }, [hasUnsavedChanges])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setLoading(true)
      setError(null)
      try {
        const list = await listSchemas()
        if (!cancelled) setSchemas(list)
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e))
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (isNewDraft) {
      const nextUsages: BoMSchemaUsage[] =
        createKind === 'edge' ? ['EDGE_PROPERTIES'] : ['ENTITY']
      const name = createKind === 'edge' ? 'NewEdge' : 'NewType'
      const draft = expertDoc(name, '1.0.0', nextUsages, emptyObjectSchema())
      setTypeName(name)
      setVersion('1.0.0')
      setUsages(nextUsages)
      setContentSchema(emptyObjectSchema())
      setExpertSnapshot(draft)
      setEdgeRules(null)
      setBaselineEdgeRules([])
      captureBaseline(draft, [])
      setSelected(null)
      setHighlightedEdge(null)
      setJsonSchema(null)
      setLint(null)
      setDetailLoading(false)
      setEditorMode('schema')
      markClean()
      loadedDetailKeyRef.current = detailKey
      return
    }

    if (!selectedType) {
      setSelected(null)
      setJsonSchema(null)
      setEdgeRules(null)
      loadedDetailKeyRef.current = null
      return
    }

    let cancelled = false
    ;(async () => {
      setError(null)
      try {
        const versions = schemas.filter((s) => s.type === selectedType).map((s) => s.version)
        const versionToOpen = selectedVersion ?? latestVersion(versions)
        if (!selectedVersion && versions.length > 0) {
          navigate(schemaDetailPath(selectedType, versionToOpen), { replace: true })
          return
        }
        if (!selectedVersion) {
          return
        }

        const contextChanged = loadedDetailKeyRef.current !== detailKey
        if (!contextChanged && unsavedRef.current) {
          return
        }

        setDetailLoading(true)
        const schema = await getSchema(selectedType, versionToOpen)
        const [projection, typeEdges] = await Promise.all([
          getJsonSchema(selectedType, versionToOpen),
          schema.usages.includes('ENTITY')
            ? getTypeEdges(selectedType)
            : Promise.resolve({ incoming: [], outgoing: [] }),
        ])
        if (cancelled) return

        const rules = uniqueEdgeRules(typeEdges)
        setSelected(schema)
        setTypeName(schema.type)
        setVersion(schema.version)
        setUsages(schema.usages)
        setContentSchema(schema.contentSchema)
        const doc = expertDoc(schema.type, schema.version, schema.usages, schema.contentSchema)
        setExpertSnapshot(doc)
        setEdgeRules(cloneEdgeRules(rules))
        captureBaseline(doc, rules)
        setJsonSchema(projection)
        setLint(null)
        if (contextChanged) {
          setHighlightedEdge(null)
          setEditorMode('visual')
        }
        markClean()
        loadedDetailKeyRef.current = detailKey
      } catch (e) {
        if (!cancelled) {
          setSelected(null)
          setError(e instanceof Error ? e.message : String(e))
        }
      } finally {
        if (!cancelled) setDetailLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [selectedType, selectedVersion, schemas, navigate, isNewDraft, createKind, detailKey])

  const grouped = useMemo(() => {
    const q = search.trim().toLowerCase()
    const byType = new Map<string, BoMSchema[]>()
    for (const schema of schemas) {
      if (q && !schema.type.toLowerCase().includes(q) && !schema.version.toLowerCase().includes(q)) {
        continue
      }
      const list = byType.get(schema.type) ?? []
      list.push(schema)
      byType.set(schema.type, list)
    }
    return [...byType.entries()]
      .map(([type, versions]) => {
        const usages = versions.flatMap((v) => v.usages)
        return {
          type,
          versions: versions.map((v) => v.version).sort(),
          kind: primaryKind(usages),
        }
      })
      .sort((a, b) => a.type.localeCompare(b.type))
  }, [schemas, search])

  const entityTypes = useMemo(
    () =>
      [...new Set(schemas.filter((s) => s.usages.includes('ENTITY')).map((s) => s.type))].sort(),
    [schemas],
  )

  const edgeSchemaOptions = useMemo(
    () =>
      schemas
        .filter((s) => s.usages.includes('EDGE_PROPERTIES'))
        .map((s) => ({
          value: `${s.type}@${s.version}`,
          label: `${s.type}@${s.version}`,
        })),
    [schemas],
  )

  const typeVersions = useMemo(
    () => schemas.filter((s) => s.type === (selected?.type ?? selectedType)).map((s) => s.version).sort(),
    [schemas, selected, selectedType],
  )

  function currentDocument(): SchemaExpertDocument | null {
    if (editorMode === 'expert') {
      const parsed = editorRef.current?.getParsedForSubmit()
      if (parsed && !parsed.ok) {
        setError(parsed.error)
        return null
      }
      if (!parsed?.ok) {
        setError('Expert document is empty or invalid')
        return null
      }
      const document = parseSchemaExpertDocument(parsed.value)
      if (!document.ok) {
        setError(document.error)
        return null
      }
      if (!isNewDraft && (document.value.type !== typeName || document.value.version !== version)) {
        setError('Type and version cannot change while editing an existing schema')
        return null
      }
      return document.value
    }
    return expertDoc(typeName, version, usages, contentSchema)
  }

  async function runLint() {
    const document = currentDocument()
    if (!document) return
    setBusy(true)
    setError(null)
    try {
      const result = await lintSchema(document.type || 'Draft', document.version || '0', {
        contentSchema: document.contentSchema,
        usages: document.usages,
      })
      setLint(result)
      if (result.valid && result.schema) {
        setContentSchema(result.schema.contentSchema)
        setUsages(result.schema.usages)
        setExpertSnapshot(
          expertDoc(
            result.schema.type,
            result.schema.version,
            result.schema.usages,
            result.schema.contentSchema,
          ),
        )
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  async function onSaveUpdate() {
    const document = currentDocument()
    if (!document) return
    setBusy(true)
    setError(null)
    try {
      const saved = await updateSchema(document.type, document.version, {
        contentSchema: document.contentSchema,
        usages: document.usages,
      })
      const draftEdges = edgeRules ?? []
      await persistEdgeChanges(baselineEdgeRules, draftEdges)
      const refreshedEdges = saved.usages.includes('ENTITY')
        ? uniqueEdgeRules(await getTypeEdges(saved.type))
        : []
      setEdgeRules(cloneEdgeRules(refreshedEdges))
      await reloadSchemas()
      captureBaseline(
        expertDoc(saved.type, saved.version, saved.usages, saved.contentSchema),
        refreshedEdges,
      )
      markClean()
      navigate(schemaDetailPath(saved.type, saved.version))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  async function onCreateSchemaOrVersion() {
    const document = currentDocument()
    if (!document) return
    setBusy(true)
    setError(null)
    try {
      const created = isNewDraft
        ? await updateSchema(document.type, document.version || '1.0.0', {
            contentSchema: document.contentSchema,
            usages: document.usages,
          })
        : await createNextMajorSchema(document.type, {
            contentSchema: document.contentSchema,
            usages: document.usages,
          })
      await reloadSchemas()
      captureBaseline(
        expertDoc(created.type, created.version, created.usages, created.contentSchema),
        [],
      )
      setEdgeRules(created.usages.includes('ENTITY') ? [] : null)
      markClean()
      navigate(schemaDetailPath(created.type, created.version))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Group align="stretch" grow preventGrowOverflow={false} style={{ flex: 1, minHeight: 0 }} gap="sm">
      <Paper withBorder p="sm" style={{ flex: '0 0 240px', maxWidth: 260, overflow: 'hidden' }}>
        <Stack gap="xs" style={{ height: '100%' }}>
          <Group justify="space-between" wrap="nowrap">
            <Title order={5}>Schemas</Title>
            <Menu position="bottom-end">
              <Menu.Target>
                <Button size="compact-xs">Create</Button>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item
                  onClick={() => requestNavigate(schemaCreatePath('object'))}
                >
                  Create New Object
                </Menu.Item>
                <Menu.Item
                  onClick={() => requestNavigate(schemaCreatePath('edge'))}
                >
                  Create New Edge
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
          <TextInput
            size="xs"
            placeholder="Search types"
            value={search}
            onChange={(e) => setSearch(e.currentTarget.value)}
          />
          {loading ? (
            <Loader size="sm" />
          ) : (
            <ScrollArea style={{ flex: 1 }}>
              <Stack gap={2}>
                {grouped.map((entry) => {
                  const active = selectedType === entry.type
                  return (
                    <UnstyledButton
                      key={entry.type}
                      onClick={() =>
                        requestNavigate(
                          schemaDetailPath(entry.type, latestVersion(entry.versions)),
                        )
                      }
                      px={6}
                      py={4}
                      style={{
                        borderRadius: 4,
                        background: active ? 'var(--mantine-color-blue-light)' : undefined,
                      }}
                    >
                      <Group gap={6} wrap="nowrap">
                        <KindPill kind={entry.kind} />
                        <Text size="xs" fw={active ? 700 : 500} truncate style={{ flex: 1 }}>
                          {entry.type}
                          {active && hasUnsavedChanges ? ' *' : ''}
                        </Text>
                      </Group>
                    </UnstyledButton>
                  )
                })}
              </Stack>
            </ScrollArea>
          )}
        </Stack>
      </Paper>

      <Paper withBorder p="md" style={{ flex: 1, minWidth: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        {error && (
          <Alert color="red" title="Error" mb="md">
            {error}
          </Alert>
        )}
        {detailLoading && <Loader size="sm" />}

        {!selectedType && !detailLoading && (
          <div style={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
            <SchemaCatalogOverview
              schemas={schemas}
              onCatalogChanged={async () => {
                await reloadSchemas()
              }}
            />
          </div>
        )}

        {(isNewDraft || (selected && !detailLoading)) && (
          <ScrollArea style={{ flex: 1 }} h="100%">
            <Stack gap="md">
              <Group justify="space-between" align="flex-start">
                <Stack gap={6} style={{ flex: 1, minWidth: 0 }}>
                  {isNewDraft ? (
                    <Group align="flex-end">
                      <TextInput
                        label="Type"
                        value={typeName}
                        onChange={(e) => {
                          setTypeName(e.currentTarget.value)
                          setDirty(true)
                        }}
                        style={{ flex: 1 }}
                      />
                      <TextInput
                        label="Version"
                        value={version}
                        onChange={(e) => {
                          setVersion(e.currentTarget.value)
                          setDirty(true)
                        }}
                        w={120}
                      />
                      <Badge variant="light">{createKind === 'edge' ? 'EDGE' : 'OBJECT'}</Badge>
                      {hasUnsavedChanges && (
                        <Button size="compact-xs" variant="subtle" onClick={rollbackUnsaved}>
                          Rollback
                        </Button>
                      )}
                    </Group>
                  ) : (
                    <Group gap="xs">
                      <Button
                        size="compact-xs"
                        variant="subtle"
                        onClick={() => requestNavigate('/schemas')}
                      >
                        Full schema
                      </Button>
                      <Title order={3}>{typeName}</Title>
                      {hasUnsavedChanges && (
                        <>
                          <Badge color="yellow" variant="filled">
                            Unsaved changes
                          </Badge>
                          <Button size="compact-xs" variant="subtle" onClick={rollbackUnsaved}>
                            Rollback
                          </Button>
                        </>
                      )}
                    </Group>
                  )}
                </Stack>

                <Group>
                  {!isNewDraft && (
                    <>
                      <Select
                        size="sm"
                        w={130}
                        allowDeselect={false}
                        data={typeVersions.map((v) => ({ value: v, label: v }))}
                        value={version}
                        onChange={(v) => {
                          if (v && selectedType) requestNavigate(schemaDetailPath(selectedType, v))
                        }}
                      />
                      <Button
                        size="sm"
                        variant="light"
                        loading={busy}
                        onClick={() => void onCreateSchemaOrVersion()}
                      >
                        Create version
                      </Button>
                    </>
                  )}
                  {!isNewDraft && !createVersionMode && (
                    <Button
                      loading={busy}
                      disabled={!hasUnsavedChanges}
                      onClick={() => void onSaveUpdate()}
                    >
                      Save update
                    </Button>
                  )}
                  {isNewDraft && (
                    <Button loading={busy} onClick={() => void onCreateSchemaOrVersion()}>
                      Create schema
                    </Button>
                  )}
                </Group>
              </Group>

              <Tabs
                value={editorMode}
                onChange={(v) => {
                  const next = (v as typeof editorMode) ?? 'visual'
                  if (next === 'expert' && editorMode !== 'expert') {
                    setExpertSnapshot(expertDoc(typeName, version, usages, contentSchema))
                    setExpertDirty(false)
                  }
                  if (editorMode === 'expert' && next !== 'expert') {
                    const parsed = editorRef.current?.getParsedForSubmit()
                    if (parsed && !parsed.ok) {
                      setError(parsed.error)
                      return
                    }
                    if (parsed?.ok) {
                      const document = parseSchemaExpertDocument(parsed.value)
                      if (!document.ok) {
                        setError(document.error)
                        return
                      }
                      setTypeName(document.value.type)
                      setVersion(document.value.version)
                      setUsages(document.value.usages)
                      setContentSchema(document.value.contentSchema)
                      setExpertSnapshot(document.value)
                      setDirty(
                        JSON.stringify(cloneDoc(document.value)) !== JSON.stringify(baseline),
                      )
                      setExpertDirty(false)
                    }
                  }
                  setEditorMode(next)
                }}
              >
                <Tabs.List>
                  {!isNewDraft && <Tabs.Tab value="visual">Visual</Tabs.Tab>}
                  <Tabs.Tab value="schema">Schema</Tabs.Tab>
                  <Tabs.Tab value="expert">Expert</Tabs.Tab>
                  {!isNewDraft && <Tabs.Tab value="json-schema">JSON Schema</Tabs.Tab>}
                </Tabs.List>

                {!isNewDraft && selected && (
                  <Tabs.Panel value="visual" pt="sm">
                    <SchemaRelationshipGraph
                      schema={{
                        ...selected,
                        contentSchema,
                        usages,
                      }}
                      relationships={edges ?? { incoming: [], outgoing: [] }}
                      highlightedEdge={highlightedEdge}
                      onEdgeSelect={setHighlightedEdge}
                    />
                  </Tabs.Panel>
                )}
                <Tabs.Panel value="schema" pt="sm">
                  <SchemaVisualBuilder
                    value={contentSchema}
                    onChange={(next) => {
                      setContentSchema(next)
                      setDirty(true)
                    }}
                  />
                </Tabs.Panel>
                <Tabs.Panel value="expert" pt="sm">
                  <Stack gap="sm">
                    {lint && (
                      <Alert
                        color={lint.valid ? 'green' : 'red'}
                        title={lint.valid ? 'Valid' : 'Invalid'}
                      >
                        {lint.issues.length === 0
                          ? 'No issues'
                          : lint.issues.map((i) => i.message).join('; ')}
                      </Alert>
                    )}
                    <JsonYamlEditor
                      ref={editorRef}
                      value={expertSnapshot}
                      rollbackValue={baseline}
                      onRollback={rollbackUnsaved}
                      extraActions={
                        <Button size="xs" variant="light" loading={busy} onClick={() => void runLint()}>
                          Lint
                        </Button>
                      }
                      onDraftParsed={(draft) => {
                        if (!draft.valid || draft.value === undefined) {
                          setExpertDirty(true)
                          return
                        }
                        const document = parseSchemaExpertDocument(draft.value)
                        if (!document.ok) {
                          setExpertDirty(true)
                          return
                        }
                        setExpertDirty(
                          JSON.stringify(cloneDoc(document.value)) !== JSON.stringify(baseline),
                        )
                      }}
                    />
                  </Stack>
                </Tabs.Panel>
                {!isNewDraft && (
                  <Tabs.Panel value="json-schema" pt="sm">
                    <SyntaxCodeEditor
                      language="json"
                      readOnly
                      minHeight={420}
                      value={jsonSchema ? JSON.stringify(jsonSchema, null, 2) : ''}
                    />
                  </Tabs.Panel>
                )}
              </Tabs>

              {!isNewDraft && usages.includes('ENTITY') && edges && (
                <ObjectEdgesEditor
                  selectedType={selectedType!}
                  incoming={edges.incoming}
                  outgoing={edges.outgoing}
                  entityTypes={entityTypes}
                  edgeSchemaOptions={edgeSchemaOptions}
                  busy={busy}
                  highlightedEdge={highlightedEdge}
                  onCreate={async (rule) => {
                    setEdgeRules((prev) => [...(prev ?? []), rule])
                    setDirty(true)
                    setHighlightedEdge(rule)
                  }}
                  onUpdate={async (previous, next) => {
                    setEdgeRules((prev) => {
                      const without = (prev ?? []).filter(
                        (rule) => allowedEdgeKey(rule) !== allowedEdgeKey(previous),
                      )
                      return [...without, next]
                    })
                    setDirty(true)
                    setHighlightedEdge(next)
                  }}
                  onDelete={async (rule) => {
                    setEdgeRules((prev) =>
                      (prev ?? []).filter((row) => allowedEdgeKey(row) !== allowedEdgeKey(rule)),
                    )
                    setDirty(true)
                    setHighlightedEdge(null)
                  }}
                />
              )}
            </Stack>
          </ScrollArea>
        )}
      </Paper>

      <Modal
        opened={blocker.state === 'blocked'}
        onClose={() => blocker.reset?.()}
        title="Unsaved changes"
        centered
      >
        <Text size="sm" mb="md">
          You have unsaved changes. Leave without saving, or stay to save first.
        </Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={() => blocker.reset?.()}>
            Stay
          </Button>
          <Button color="red" onClick={() => blocker.proceed?.()}>
            Leave
          </Button>
        </Group>
      </Modal>
    </Group>
  )
}
