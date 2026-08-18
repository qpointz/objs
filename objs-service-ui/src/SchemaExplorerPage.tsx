import { useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Badge,
  Breadcrumbs,
  Button,
  Code,
  Group,
  Loader,
  Menu,
  Modal,
  Paper,
  ScrollArea,
  SegmentedControl,
  Select,
  Stack,
  Tabs,
  TagsInput,
  Text,
  TextInput,
  Textarea,
  Title,
  Tooltip,
  UnstyledButton,
} from '@mantine/core'
import { IconSchema } from '@tabler/icons-react'
import { useBlocker, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  deleteEdge,
  deleteSchemaType,
  deleteSchemaVersion,
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
import {
  EMPTY_KEY_VALUE_ROWS,
  KeyValueRowsEditor,
  rowsToStringMap,
  stringMapToRows,
} from './KeyValueRowsEditor'
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
import { emptyObjectSchema, type EditorFormat } from './schemaDsl'
import { SyntaxCodeEditor } from './SyntaxCodeEditor'
import { NewUuidButton } from './NewUuidButton'
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

/** Mirrors BoMSchemaVersioning.nextMajor for draft preview. */
function nextMajorVersion(existingVersions: string[]): string {
  if (existingVersions.length === 0) return '1.0.0'
  const versionPattern = /^(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:[-+].*)?$/
  const parsed = existingVersions
    .map((raw) => {
      const match = versionPattern.exec(raw.trim())
      if (!match) return null
      const major = Number.parseInt(match[1]!, 10)
      if (!Number.isFinite(major)) return null
      const dotted = Boolean(match[2] || match[3])
      return { major, dotted }
    })
    .filter((row): row is { major: number; dotted: boolean } => row != null)
  if (parsed.length === 0) {
    throw new Error(`Cannot compute next major version from: ${existingVersions.join(', ')}`)
  }
  const maxMajor = Math.max(...parsed.map((row) => row.major))
  const dotted = parsed.some((row) => row.dotted)
  return dotted ? `${maxMajor + 1}.0.0` : `${maxMajor + 1}`
}

function isValidSchemaVersion(raw: string): boolean {
  return /^(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:[-+].*)?$/.test(raw.trim())
}

function primaryKind(usage: BoMSchemaUsage): 'object' | 'edge' {
  return usage === 'ENTITY' ? 'object' : 'edge'
}

function KindPill({ kind }: { kind: 'object' | 'edge' }) {
  return (
    <Badge size="xs" variant="light" color={kind === 'object' ? 'blue' : 'grape'} w={28} px={0}>
      {kind === 'object' ? 'O' : 'E'}
    </Badge>
  )
}

function AttributePill({ name, value }: { name: string; value: string }) {
  return (
    <Group gap={0} wrap="nowrap" title={`${name}: ${value}`}>
      <Badge
        size="sm"
        variant="filled"
        color="gray"
        radius="xl"
        tt="none"
        style={{
          borderTopRightRadius: 0,
          borderBottomRightRadius: 0,
          paddingLeft: 8,
          paddingRight: 8,
        }}
      >
        {name}
      </Badge>
      <Badge
        size="sm"
        variant="light"
        color="gray"
        radius="xl"
        tt="none"
        style={{
          borderTopLeftRadius: 0,
          borderBottomLeftRadius: 0,
          paddingLeft: 8,
          paddingRight: 8,
        }}
      >
        {value || '—'}
      </Badge>
    </Group>
  )
}

function CatalogMetaPills({
  tags,
  attributes,
}: {
  tags: string[]
  attributes: { key: string; value: string }[]
}) {
  const filledAttributes = attributes.filter((row) => row.key.trim().length > 0)
  if (tags.length === 0 && filledAttributes.length === 0) return null
  return (
    <Group gap={6} wrap="wrap">
      {tags.map((tag) => (
        <Badge key={tag} size="sm" variant="light" radius="xl" tt="none">
          {tag}
        </Badge>
      ))}
      {filledAttributes.map((row) => (
        <AttributePill key={row.key} name={row.key.trim()} value={row.value} />
      ))}
    </Group>
  )
}

function expertDoc(
  type: string,
  version: string,
  usage: BoMSchemaUsage,
  contentSchema: BoMSchemaNode,
): SchemaExpertDocument {
  return { type, version, usage, contentSchema }
}

function cloneDoc(doc: SchemaExpertDocument): SchemaExpertDocument {
  return JSON.parse(JSON.stringify(doc)) as SchemaExpertDocument
}

const schemaTabPanelStyle = {
  position: 'absolute',
  inset: 0,
  paddingTop: 'var(--mantine-spacing-sm)',
  display: 'flex',
  flexDirection: 'column',
  overflow: 'hidden',
} as const

type SchemaView = 'editor' | 'yaml' | 'json' | 'json-schema'

function isTextSchemaView(view: SchemaView): view is 'yaml' | 'json' {
  return view === 'yaml' || view === 'json'
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
  const draftNewVersionParam = searchParams.get('newVersion')

  const editorRef = useRef<JsonYamlEditorHandle>(null)
  const [search, setSearch] = useState('')
  const [schemas, setSchemas] = useState<BoMSchema[]>([])
  const [selected, setSelected] = useState<BoMSchema | null>(null)
  const [jsonSchema, setJsonSchema] = useState<Record<string, unknown> | null>(null)
  const [edgeRules, setEdgeRules] = useState<BoMAllowedEdgeRule[] | null>(null)
  const [baselineEdgeRules, setBaselineEdgeRules] = useState<BoMAllowedEdgeRule[]>([])
  const [highlightedEdge, setHighlightedEdge] = useState<AllowedEdgeRef | null>(null)
  const loadedDetailKeyRef = useRef<string | null>(null)
  /** Skip detail fetches/errors while leaving a deleted type for Full schema. */
  const leavingDetailRef = useRef(false)
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lint, setLint] = useState<SchemaLintResponse | null>(null)
  const [editorMode, setEditorMode] = useState<'visual' | 'general' | 'schema' | 'edges'>('visual')
  const [schemaView, setSchemaView] = useState<SchemaView>('editor')
  const [dirty, setDirty] = useState(false)
  const [expertDirty, setExpertDirty] = useState(false)

  const [typeName, setTypeName] = useState('NewType')
  const [version, setVersion] = useState('1.0.0')
  const [usage, setUsage] = useState<BoMSchemaUsage>('ENTITY')
  const [contentSchema, setContentSchema] = useState<BoMSchemaNode>(emptyObjectSchema())
  const [schemaTags, setSchemaTags] = useState<string[]>([])
  const [schemaAttributeRows, setSchemaAttributeRows] = useState(EMPTY_KEY_VALUE_ROWS)
  const [expertSnapshot, setExpertSnapshot] = useState<SchemaExpertDocument>(() =>
    expertDoc('NewType', '1.0.0', 'ENTITY', emptyObjectSchema()),
  )
  const [baseline, setBaseline] = useState<SchemaExpertDocument>(() =>
    cloneDoc(expertDoc('NewType', '1.0.0', 'ENTITY', emptyObjectSchema())),
  )

  const [createVersionOpen, setCreateVersionOpen] = useState(false)
  const [createBaseVersion, setCreateBaseVersion] = useState('')
  const [createNewVersion, setCreateNewVersion] = useState('')
  const [createVersionBusy, setCreateVersionBusy] = useState(false)

  const [deleteVersionOpen, setDeleteVersionOpen] = useState(false)
  const [deleteSchemaOpen, setDeleteSchemaOpen] = useState(false)
  const [deleteConfirmText, setDeleteConfirmText] = useState('')
  const [deleteBusy, setDeleteBusy] = useState(false)

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
    setUsage(doc.usage)
    setContentSchema(cloneDoc(doc).contentSchema)
    setExpertSnapshot(cloneDoc(doc))
  }

  function rollbackUnsaved() {
    applyDocument(baseline)
    setSchemaTags(selected?.tags ?? [])
    setSchemaAttributeRows(stringMapToRows(selected?.attributes))
    setEdgeRules(cloneEdgeRules(baselineEdgeRules))
    setHighlightedEdge(null)
    setLint(null)
    markClean()
    if (createVersionMode && selectedType && selectedVersion) {
      navigate(schemaDetailPath(selectedType, selectedVersion))
    }
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
      ? `${selectedType}@${selectedVersion}${
          createVersionMode ? `:create-version:${draftNewVersionParam ?? ''}` : ''
        }`
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
      const nextUsage: BoMSchemaUsage = createKind === 'edge' ? 'EDGE_PROPERTIES' : 'ENTITY'
      const name = createKind === 'edge' ? 'NewEdge' : 'NewType'
      const draft = expertDoc(name, '1.0.0', nextUsage, emptyObjectSchema())
      setTypeName(name)
      setVersion('1.0.0')
      setUsage(nextUsage)
      setContentSchema(emptyObjectSchema())
      setSchemaTags([])
      setSchemaAttributeRows(EMPTY_KEY_VALUE_ROWS)
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
      setSchemaView('editor')
      markClean()
      loadedDetailKeyRef.current = detailKey
      return
    }

    if (!selectedType) {
      leavingDetailRef.current = false
      setSelected(null)
      setJsonSchema(null)
      setEdgeRules(null)
      setError(null)
      loadedDetailKeyRef.current = null
      return
    }

    // Wait for catalog load before deciding the type is missing (deep links from
    // Explorer/Composer must not bounce to the overview while schemas === []).
    if (loading) {
      return
    }

    if (leavingDetailRef.current) {
      return
    }

    let cancelled = false
    ;(async () => {
      setError(null)
      try {
        const versions = schemas.filter((s) => s.type === selectedType).map((s) => s.version)
        if (selectedVersion && versions.length === 0) {
          // Type removed from catalog (e.g. Delete schema) — return to overview quietly.
          navigate('/model', { replace: true })
          return
        }
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
          schema.usage === 'ENTITY'
            ? getTypeEdges(selectedType)
            : Promise.resolve({ incoming: [], outgoing: [] }),
        ])
        if (cancelled) return

        const rules = uniqueEdgeRules(typeEdges)
        setSelected(schema)
        setTypeName(schema.type)
        setUsage(schema.usage)
        setContentSchema(schema.contentSchema)
        setSchemaTags(schema.tags ?? [])
        setSchemaAttributeRows(stringMapToRows(schema.attributes))
        const doc = expertDoc(schema.type, schema.version, schema.usage, schema.contentSchema)
        setEdgeRules(cloneEdgeRules(rules))
        captureBaseline(doc, rules)
        setJsonSchema(projection)
        setLint(null)
        if (contextChanged) {
          setHighlightedEdge(null)
          setEditorMode('visual')
          setSchemaView('editor')
        }
        if (createVersionMode) {
          try {
            const next =
              draftNewVersionParam && draftNewVersionParam.trim()
                ? draftNewVersionParam.trim()
                : nextMajorVersion(versions)
            setVersion(next)
            setExpertSnapshot(expertDoc(schema.type, next, schema.usage, schema.contentSchema))
            setDirty(true)
            setExpertDirty(false)
          } catch (e) {
            setVersion(schema.version)
            setExpertSnapshot(doc)
            markClean()
            setError(e instanceof Error ? e.message : String(e))
            navigate(schemaDetailPath(schema.type, schema.version), { replace: true })
          }
        } else {
          setVersion(schema.version)
          setExpertSnapshot(doc)
          markClean()
        }
        loadedDetailKeyRef.current = detailKey
      } catch (e) {
        if (!cancelled && !leavingDetailRef.current) {
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
  }, [
    selectedType,
    selectedVersion,
    schemas,
    loading,
    navigate,
    isNewDraft,
    createKind,
    detailKey,
    createVersionMode,
    draftNewVersionParam,
  ])

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
        const kind = primaryKind(
          versions.some((v) => v.usage === 'ENTITY') ? 'ENTITY' : 'EDGE_PROPERTIES',
        )
        return {
          type,
          versions: versions.map((v) => v.version).sort(),
          kind,
        }
      })
      .sort((a, b) => a.type.localeCompare(b.type))
  }, [schemas, search])

  const entityTypes = useMemo(
    () =>
      [...new Set(schemas.filter((s) => s.usage === 'ENTITY').map((s) => s.type))].sort(),
    [schemas],
  )

  const edgeSchemaOptions = useMemo(
    () =>
      schemas
        .filter((s) => s.usage === 'EDGE_PROPERTIES')
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

  function flushExpertEditor(): SchemaExpertDocument | null {
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
    setTypeName(document.value.type)
    setVersion(document.value.version)
    setUsage(document.value.usage)
    setContentSchema(document.value.contentSchema)
    setExpertSnapshot(document.value)
    setDirty(JSON.stringify(cloneDoc(document.value)) !== JSON.stringify(baseline))
    setExpertDirty(false)
    setError(null)
    return document.value
  }

  function switchSchemaView(next: SchemaView) {
    if (next === schemaView) return
    if (next === 'json-schema' && isNewDraft) return

    if (isTextSchemaView(schemaView)) {
      if (!flushExpertEditor()) return
    }
    if (isTextSchemaView(next) && !isTextSchemaView(schemaView)) {
      setExpertSnapshot(expertDoc(typeName, version, usage, contentSchema))
      setExpertDirty(false)
    }
    setSchemaView(next)
  }

  function currentDocument(): SchemaExpertDocument | null {
    if (editorMode === 'schema' && isTextSchemaView(schemaView)) {
      return flushExpertEditor()
    }
    return expertDoc(typeName, version, usage, contentSchema)
  }

  async function runLint() {
    const document = currentDocument()
    if (!document) return
    setBusy(true)
    setError(null)
    try {
      const result = await lintSchema(document.type || 'Draft', document.version || '0', {
        contentSchema: document.contentSchema,
        usage: document.usage,
        tags: schemaTags,
        attributes: rowsToStringMap(schemaAttributeRows),
      })
      setLint(result)
      if (result.valid && result.schema) {
        setContentSchema(result.schema.contentSchema)
        setUsage(result.schema.usage)
        setExpertSnapshot(
          expertDoc(
            result.schema.type,
            result.schema.version,
            result.schema.usage,
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
        usage: document.usage,
        tags: schemaTags,
        attributes: rowsToStringMap(schemaAttributeRows),
      })
      const draftEdges = edgeRules ?? []
      await persistEdgeChanges(baselineEdgeRules, draftEdges)
      const refreshedEdges = saved.usage === 'ENTITY'
        ? uniqueEdgeRules(await getTypeEdges(saved.type))
        : []
      setEdgeRules(cloneEdgeRules(refreshedEdges))
      await reloadSchemas()
      captureBaseline(
        expertDoc(saved.type, saved.version, saved.usage, saved.contentSchema),
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

  function onStartCreateVersion() {
    if (!selectedType || !selectedVersion || isNewDraft) return
    if (editorMode === 'schema' && isTextSchemaView(schemaView)) {
      if (!flushExpertEditor()) return
    }
    const versions = schemas.filter((s) => s.type === selectedType).map((s) => s.version)
    setCreateBaseVersion(selectedVersion)
    try {
      setCreateNewVersion(nextMajorVersion(versions))
    } catch {
      setCreateNewVersion('')
    }
    setCreateVersionOpen(true)
  }

  async function confirmCreateVersion() {
    if (!selectedType || !createBaseVersion || !createNewVersion.trim()) return
    const next = createNewVersion.trim()
    const versions = schemas.filter((s) => s.type === selectedType).map((s) => s.version)
    if (!isValidSchemaVersion(next)) {
      setError('New version must be a numeric version (e.g. 5 or 5.0.0)')
      return
    }
    if (versions.includes(next)) {
      setError(`Version ${next} already exists for ${selectedType}`)
      return
    }
    setCreateVersionBusy(true)
    setError(null)
    try {
      // Ensure base content is fetchable before entering draft mode.
      await getSchema(selectedType, createBaseVersion)
      setCreateVersionOpen(false)
      navigate(
        `${schemaDetailPath(selectedType, createBaseVersion)}?mode=create-version&newVersion=${encodeURIComponent(next)}`,
      )
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setCreateVersionBusy(false)
    }
  }

  async function confirmDeleteVersion() {
    if (!selectedType || !selectedVersion) return
    if (deleteConfirmText !== selectedVersion) return
    setDeleteBusy(true)
    setError(null)
    try {
      await deleteSchemaVersion(selectedType, selectedVersion)
      setDeleteVersionOpen(false)
      setDeleteConfirmText('')
      markClean()
      const list = await reloadSchemas()
      const remaining = list.filter((s) => s.type === selectedType).map((s) => s.version)
      if (remaining.length === 0) {
        leavingDetailRef.current = true
        loadedDetailKeyRef.current = null
        setSelected(null)
        setEdgeRules(null)
        setError(null)
        navigate('/model', { replace: true })
      } else {
        navigate(schemaDetailPath(selectedType, latestVersion(remaining)), { replace: true })
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setDeleteBusy(false)
    }
  }

  async function confirmDeleteSchema() {
    if (!selectedType) return
    if (deleteConfirmText !== selectedType) return
    setDeleteBusy(true)
    setError(null)
    try {
      await deleteSchemaType(selectedType)
      setDeleteSchemaOpen(false)
      setDeleteConfirmText('')
      markClean()
      leavingDetailRef.current = true
      loadedDetailKeyRef.current = null
      setSelected(null)
      setEdgeRules(null)
      setError(null)
      // Navigate first so the detail effect does not re-fetch the deleted type.
      navigate('/model', { replace: true })
      await reloadSchemas()
    } catch (e) {
      leavingDetailRef.current = false
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setDeleteBusy(false)
    }
  }

  async function onCreateSchema() {
    const document = currentDocument()
    if (!document) return
    setBusy(true)
    setError(null)
    try {
      const created = await updateSchema(document.type, document.version || '1.0.0', {
        contentSchema: document.contentSchema,
        usage: document.usage,
        tags: schemaTags,
        attributes: rowsToStringMap(schemaAttributeRows),
      })
      await reloadSchemas()
      captureBaseline(
        expertDoc(created.type, created.version, created.usage, created.contentSchema),
        [],
      )
      setEdgeRules(created.usage === 'ENTITY' ? [] : null)
      markClean()
      navigate(schemaDetailPath(created.type, created.version))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
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
          maxWidth: 260,
          minHeight: 0,
          height: '100%',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Group justify="space-between" wrap="nowrap" style={{ flexShrink: 0 }} mb="xs">
          <Title order={5}>Schemas</Title>
          <Menu position="bottom-end" withinPortal>
            <Menu.Target>
              <UnstyledButton
                aria-label="Create object or edge"
                style={{ display: 'inline-flex', borderRadius: 'var(--mantine-radius-default)' }}
              >
                <Group gap={0}>
                  <Button
                    size="compact-xs"
                    component="span"
                    style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0, pointerEvents: 'none' }}
                  >
                    Create
                  </Button>
                  <Button
                    size="compact-xs"
                    component="span"
                    px={6}
                    style={{
                      borderTopLeftRadius: 0,
                      borderBottomLeftRadius: 0,
                      borderLeft: '1px solid var(--mantine-color-default-border)',
                      pointerEvents: 'none',
                    }}
                  >
                    ▾
                  </Button>
                </Group>
              </UnstyledButton>
            </Menu.Target>
            <Menu.Dropdown>
              <Menu.Item onClick={() => requestNavigate(schemaCreatePath('object'))}>
                Object
              </Menu.Item>
              <Menu.Item onClick={() => requestNavigate(schemaCreatePath('edge'))}>
                Edge
              </Menu.Item>
            </Menu.Dropdown>
          </Menu>
        </Group>
        <TextInput
          size="xs"
          placeholder="Search types"
          value={search}
          onChange={(e) => setSearch(e.currentTarget.value)}
          mb="xs"
          style={{ flexShrink: 0 }}
        />
        {loading ? (
          <Loader size="sm" />
        ) : (
          <div style={{ flex: 1, minHeight: 0, position: 'relative' }}>
            <ScrollArea
              type="auto"
              offsetScrollbars
              style={{ position: 'absolute', inset: 0 }}
            >
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
          </div>
        )}
      </Paper>

      <Paper
        withBorder
        p="md"
        style={{
          flex: 1,
          minWidth: 0,
          minHeight: 0,
          height: '100%',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        {error && (
          <Alert color="red" title="Error" mb="md" style={{ flexShrink: 0 }}>
            {error}
          </Alert>
        )}
        {detailLoading && <Loader size="sm" />}

        {!selectedType && !detailLoading && (
          <div style={{ flex: 1, minHeight: 0, position: 'relative' }}>
            <div
              style={{
                position: 'absolute',
                inset: 0,
                overflow: 'hidden',
                display: 'flex',
                flexDirection: 'column',
              }}
            >
              <SchemaCatalogOverview
                schemas={schemas}
                onCatalogChanged={async () => {
                  await reloadSchemas()
                }}
              />
            </div>
          </div>
        )}

        {(isNewDraft || (selected && !detailLoading)) && (
          <div
            style={{
              flex: 1,
              minHeight: 0,
              display: 'flex',
              flexDirection: 'column',
              gap: 'var(--mantine-spacing-md)',
              overflow: 'hidden',
            }}
          >
            <Group justify="space-between" align="flex-start" style={{ flexShrink: 0 }}>
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
                    <SegmentedControl
                      size="xs"
                      value={usage}
                      data={[
                        { label: 'Entity', value: 'ENTITY' },
                        { label: 'Edge props', value: 'EDGE_PROPERTIES' },
                      ]}
                      onChange={(v) => {
                        const next = v as BoMSchemaUsage
                        setUsage(next)
                        setDirty(true)
                      }}
                    />
                    {hasUnsavedChanges && (
                      <Button size="compact-xs" variant="subtle" onClick={rollbackUnsaved}>
                        Rollback
                      </Button>
                    )}
                  </Group>
                ) : (
                  <Group gap="sm" wrap="wrap">
                    <Breadcrumbs
                      separator="/"
                      separatorMargin="xs"
                      styles={{
                        root: { alignItems: 'center', flexWrap: 'wrap' },
                        breadcrumb: { lineHeight: 1.2 },
                      }}
                    >
                      <Tooltip label="Full schema">
                        <UnstyledButton
                          onClick={() => requestNavigate('/model')}
                          aria-label="Full schema"
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            color: 'var(--mantine-color-dimmed)',
                          }}
                        >
                          <IconSchema size={22} stroke={1.75} />
                        </UnstyledButton>
                      </Tooltip>
                      <Group gap={8} wrap="nowrap" align="center">
                        <Title order={3} style={{ lineHeight: 1.2 }}>
                          {typeName}
                        </Title>
                        <Badge size="xs" variant="filled" color="gray">
                          {version}
                        </Badge>
                        {createVersionMode && (
                          <Badge size="xs" variant="filled" color="violet">
                            draft
                          </Badge>
                        )}
                        {!createVersionMode &&
                          typeVersions.includes(version) &&
                          version === latestVersion(typeVersions) && (
                            <Badge size="xs" variant="filled" color="teal">
                              latest
                            </Badge>
                          )}
                      </Group>
                    </Breadcrumbs>
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
                <CatalogMetaPills tags={schemaTags} attributes={schemaAttributeRows} />
              </Stack>

              <Group>
                {!isNewDraft && (
                  <>
                    <Select
                      size="sm"
                      w={150}
                      allowDeselect={false}
                      data={[
                        ...typeVersions.map((v) => ({ value: v, label: v })),
                        ...(createVersionMode && !typeVersions.includes(version)
                          ? [{ value: version, label: `${version} (draft)` }]
                          : []),
                      ]}
                      value={version}
                      onChange={(v) => {
                        if (!v || !selectedType) return
                        if (createVersionMode && v === version) return
                        requestNavigate(schemaDetailPath(selectedType, v))
                      }}
                    />
                    {!createVersionMode && (
                      <Button size="sm" variant="light" onClick={onStartCreateVersion}>
                        Create version
                      </Button>
                    )}
                  </>
                )}
                {!isNewDraft && (
                  <Button
                    loading={busy}
                    disabled={!hasUnsavedChanges}
                    onClick={() => void onSaveUpdate()}
                  >
                    Save
                  </Button>
                )}
                {!isNewDraft && !createVersionMode && (
                  <Menu position="bottom-end" withinPortal>
                    <Menu.Target>
                      <UnstyledButton
                        aria-label="Delete version or schema"
                        disabled={busy || deleteBusy}
                        style={{
                          display: 'inline-flex',
                          borderRadius: 'var(--mantine-radius-default)',
                          opacity: busy || deleteBusy ? 0.6 : 1,
                        }}
                      >
                        <Group gap={0}>
                          <Button
                            size="sm"
                            variant="light"
                            color="red"
                            component="span"
                            style={{
                              borderTopRightRadius: 0,
                              borderBottomRightRadius: 0,
                              pointerEvents: 'none',
                            }}
                          >
                            Delete
                          </Button>
                          <Button
                            size="sm"
                            variant="light"
                            color="red"
                            component="span"
                            px="xs"
                            style={{
                              borderTopLeftRadius: 0,
                              borderBottomLeftRadius: 0,
                              borderLeft: '1px solid var(--mantine-color-default-border)',
                              pointerEvents: 'none',
                            }}
                          >
                            ▾
                          </Button>
                        </Group>
                      </UnstyledButton>
                    </Menu.Target>
                    <Menu.Dropdown>
                      <Menu.Item
                        color="red"
                        onClick={() => {
                          setDeleteConfirmText('')
                          setDeleteVersionOpen(true)
                        }}
                      >
                        Version
                      </Menu.Item>
                      <Menu.Item
                        color="red"
                        onClick={() => {
                          setDeleteConfirmText('')
                          setDeleteSchemaOpen(true)
                        }}
                      >
                        Schema
                      </Menu.Item>
                    </Menu.Dropdown>
                  </Menu>
                )}
                {isNewDraft && (
                  <Button loading={busy} onClick={() => void onCreateSchema()}>
                    Create schema
                  </Button>
                )}
              </Group>
            </Group>

            <Tabs
              value={editorMode}
              style={{
                flex: 1,
                minHeight: 0,
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
              }}
              onChange={(v) => {
                const next = (v as typeof editorMode) ?? 'visual'
                if (
                  editorMode === 'schema' &&
                  next !== 'schema' &&
                  isTextSchemaView(schemaView)
                ) {
                  if (!flushExpertEditor()) return
                }
                if (
                  next === 'schema' &&
                  editorMode !== 'schema' &&
                  isTextSchemaView(schemaView)
                ) {
                  setExpertSnapshot(expertDoc(typeName, version, usage, contentSchema))
                  setExpertDirty(false)
                }
                setEditorMode(next)
              }}
            >
              <Tabs.List style={{ flexShrink: 0 }}>
                {!isNewDraft && <Tabs.Tab value="visual">Visual</Tabs.Tab>}
                <Tabs.Tab value="general">General</Tabs.Tab>
                <Tabs.Tab value="schema">Schema</Tabs.Tab>
                {!isNewDraft && usage === 'ENTITY' && (
                  <Tabs.Tab value="edges">Edges</Tabs.Tab>
                )}
              </Tabs.List>

              <div style={{ flex: 1, minHeight: 0, position: 'relative' }}>
                {!isNewDraft && selected && (
                  <Tabs.Panel
                    value="visual"
                    style={{
                      position: 'absolute',
                      inset: 0,
                      paddingTop: 'var(--mantine-spacing-sm)',
                      display: 'flex',
                      flexDirection: 'column',
                      overflow: 'hidden',
                    }}
                  >
                    <SchemaRelationshipGraph
                      schema={{
                        ...selected,
                        contentSchema,
                        usage,
                      }}
                      relationships={edges ?? { incoming: [], outgoing: [] }}
                      highlightedEdge={highlightedEdge}
                      onEdgeSelect={setHighlightedEdge}
                      fillHeight
                    />
                  </Tabs.Panel>
                )}
                <Tabs.Panel value="general" style={schemaTabPanelStyle}>
                  <Stack gap="sm" maw={720} style={{ flex: 1, minHeight: 0, height: '100%', overflow: 'auto' }}>
                    <Textarea
                      label="Description"
                      placeholder="Schema description"
                      autosize
                      minRows={4}
                      value={contentSchema.description}
                      onChange={(e) => {
                        setContentSchema({ ...contentSchema, description: e.currentTarget.value })
                        setDirty(true)
                      }}
                    />
                    <TagsInput
                      label="Tags"
                      placeholder="Catalog tags"
                      value={schemaTags}
                      onChange={(next) => {
                        setSchemaTags(next)
                        setDirty(true)
                      }}
                    />
                    <Text size="sm" fw={500}>
                      Attributes
                    </Text>
                    <KeyValueRowsEditor
                      rows={schemaAttributeRows}
                      onChange={(rows) => {
                        setSchemaAttributeRows(rows)
                        setDirty(true)
                      }}
                    />
                  </Stack>
                </Tabs.Panel>
                <Tabs.Panel
                  value="schema"
                  style={schemaTabPanelStyle}
                >
                  <Stack
                    gap="sm"
                    style={{
                      flex: 1,
                      minHeight: 0,
                      height: '100%',
                    }}
                  >
                    <Group justify="space-between" wrap="wrap" style={{ flexShrink: 0 }}>
                      <SegmentedControl
                        size="xs"
                        value={schemaView}
                        onChange={(v) => switchSchemaView(v as SchemaView)}
                        data={[
                          { label: 'Editor', value: 'editor' },
                          { label: 'YAML', value: 'yaml' },
                          { label: 'JSON', value: 'json' },
                          ...(!isNewDraft
                            ? [{ label: 'JSON Schema', value: 'json-schema' }]
                            : []),
                        ]}
                      />
                      <Group gap="xs">
                        {isTextSchemaView(schemaView) && (
                          <Button
                            size="xs"
                            variant="light"
                            onClick={() => editorRef.current?.formatDocument()}
                          >
                            Format
                          </Button>
                        )}
                        <Button
                          size="xs"
                          variant="subtle"
                          disabled={!hasUnsavedChanges}
                          onClick={rollbackUnsaved}
                        >
                          Rollback
                        </Button>
                        {schemaView !== 'json-schema' && (
                          <Button
                            size="xs"
                            variant="light"
                            loading={busy}
                            onClick={() => void runLint()}
                          >
                            Lint
                          </Button>
                        )}
                        {isTextSchemaView(schemaView) && <NewUuidButton />}
                      </Group>
                    </Group>
                    {lint && schemaView !== 'json-schema' && (
                      <Alert
                        color={lint.valid ? 'green' : 'red'}
                        title={lint.valid ? 'Valid' : 'Invalid'}
                        style={{ flexShrink: 0 }}
                      >
                        {lint.issues.length === 0
                          ? 'No issues'
                          : lint.issues.map((i) => i.message).join('; ')}
                      </Alert>
                    )}
                    {schemaView === 'editor' && (
                      <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
                        <SchemaVisualBuilder
                          value={contentSchema}
                          onChange={(next) => {
                            setContentSchema(next)
                            setDirty(true)
                          }}
                        />
                      </div>
                    )}
                    {isTextSchemaView(schemaView) && (
                      <div
                        style={{
                          flex: 1,
                          minHeight: 0,
                          display: 'flex',
                          flexDirection: 'column',
                        }}
                      >
                        <JsonYamlEditor
                          ref={editorRef}
                          value={expertSnapshot}
                          format={schemaView as EditorFormat}
                          hideFormatToggle
                          hideToolbar
                          fillHeight
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
                              JSON.stringify(cloneDoc(document.value)) !==
                                JSON.stringify(baseline),
                            )
                          }}
                        />
                      </div>
                    )}
                    {schemaView === 'json-schema' && !isNewDraft && (
                      <div
                        style={{
                          flex: 1,
                          minHeight: 0,
                          display: 'flex',
                          flexDirection: 'column',
                        }}
                      >
                        <SyntaxCodeEditor
                          language="json"
                          readOnly
                          fillHeight
                          minHeight={240}
                          value={jsonSchema ? JSON.stringify(jsonSchema, null, 2) : ''}
                        />
                      </div>
                    )}
                  </Stack>
                </Tabs.Panel>
                {!isNewDraft && usage === 'ENTITY' && edges && (
                  <Tabs.Panel value="edges" style={schemaTabPanelStyle}>
                    <ObjectEdgesEditor
                      selectedType={selectedType!}
                      incoming={edges.incoming}
                      outgoing={edges.outgoing}
                      baselineRules={baselineEdgeRules}
                      entityTypes={entityTypes}
                      edgeSchemaOptions={edgeSchemaOptions}
                      busy={busy}
                      highlightedEdge={highlightedEdge}
                      onHighlight={setHighlightedEdge}
                      onCreate={async (rule) => {
                        setEdgeRules((prev) => [...(prev ?? []), rule])
                        setDirty(true)
                        setHighlightedEdge({
                          ...rule,
                          direction: rule.sourceType === selectedType ? 'outbound' : 'incoming',
                        })
                      }}
                      onUpdate={async (previous, next) => {
                        setEdgeRules((prev) => {
                          const without = (prev ?? []).filter(
                            (rule) => allowedEdgeKey(rule) !== allowedEdgeKey(previous),
                          )
                          return [...without, next]
                        })
                        setDirty(true)
                        setHighlightedEdge({
                          ...next,
                          direction: next.sourceType === selectedType ? 'outbound' : 'incoming',
                        })
                      }}
                      onDelete={async (rule) => {
                        setEdgeRules((prev) =>
                          (prev ?? []).filter((row) => allowedEdgeKey(row) !== allowedEdgeKey(rule)),
                        )
                        setDirty(true)
                      }}
                      onRestore={async (rule) => {
                        setEdgeRules((prev) => [...(prev ?? []), rule])
                        setDirty(true)
                      }}
                    />
                  </Tabs.Panel>
                )}
              </div>
            </Tabs>
          </div>
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

      <Modal
        opened={createVersionOpen}
        onClose={() => setCreateVersionOpen(false)}
        title="Create version"
        centered
      >
        <Stack gap="sm">
          <Text size="sm" c="dimmed">
            Choose a base version for initial content, then set the new version identifier.
          </Text>
          <Select
            label="Base version"
            data={typeVersions.map((v) => ({ value: v, label: v }))}
            value={createBaseVersion}
            allowDeselect={false}
            onChange={(v) => {
              if (v) setCreateBaseVersion(v)
            }}
          />
          <TextInput
            label="New version"
            value={createNewVersion}
            onChange={(e) => setCreateNewVersion(e.currentTarget.value)}
            placeholder="e.g. 5.0.0"
          />
          <Group justify="flex-end" mt="xs">
            <Button variant="default" onClick={() => setCreateVersionOpen(false)}>
              Cancel
            </Button>
            <Button
              loading={createVersionBusy}
              disabled={
                !createBaseVersion ||
                !createNewVersion.trim() ||
                !isValidSchemaVersion(createNewVersion) ||
                typeVersions.includes(createNewVersion.trim())
              }
              onClick={() => void confirmCreateVersion()}
            >
              Create draft
            </Button>
          </Group>
        </Stack>
      </Modal>

      <Modal
        opened={deleteVersionOpen}
        onClose={() => {
          setDeleteVersionOpen(false)
          setDeleteConfirmText('')
        }}
        title="Delete version"
        centered
      >
        <Stack gap="sm">
          <Text size="sm">
            Permanently delete <Text span fw={700}>{typeName}@{selectedVersion}</Text>. Type the
            version <Code>{selectedVersion}</Code> to confirm.
          </Text>
          <TextInput
            label="Confirm version"
            value={deleteConfirmText}
            onChange={(e) => setDeleteConfirmText(e.currentTarget.value)}
            placeholder={selectedVersion}
          />
          <Group justify="flex-end">
            <Button
              variant="default"
              onClick={() => {
                setDeleteVersionOpen(false)
                setDeleteConfirmText('')
              }}
            >
              Cancel
            </Button>
            <Button
              color="red"
              loading={deleteBusy}
              disabled={deleteConfirmText !== selectedVersion}
              onClick={() => void confirmDeleteVersion()}
            >
              Delete version
            </Button>
          </Group>
        </Stack>
      </Modal>

      <Modal
        opened={deleteSchemaOpen}
        onClose={() => {
          setDeleteSchemaOpen(false)
          setDeleteConfirmText('')
        }}
        title="Delete schema"
        centered
      >
        <Stack gap="sm">
          <Text size="sm">
            Permanently delete type <Text span fw={700}>{typeName}</Text> including all versions and
            allow-list edges where it is source or target. Type the name <Code>{typeName}</Code> to
            confirm.
          </Text>
          <TextInput
            label="Confirm type name"
            value={deleteConfirmText}
            onChange={(e) => setDeleteConfirmText(e.currentTarget.value)}
            placeholder={typeName}
          />
          <Group justify="flex-end">
            <Button
              variant="default"
              onClick={() => {
                setDeleteSchemaOpen(false)
                setDeleteConfirmText('')
              }}
            >
              Cancel
            </Button>
            <Button
              color="red"
              loading={deleteBusy}
              disabled={deleteConfirmText !== typeName}
              onClick={() => void confirmDeleteSchema()}
            >
              Delete schema
            </Button>
          </Group>
        </Stack>
      </Modal>
    </div>
  )
}
