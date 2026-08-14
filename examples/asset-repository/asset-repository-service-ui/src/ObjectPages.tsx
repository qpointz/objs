import {
  Alert,
  Anchor,
  Button,
  Group,
  Loader,
  Paper,
  ScrollArea,
  SegmentedControl,
  Select,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  getCollection,
  getObject,
  listObjectRelations,
  listSchemasByType,
  writeComposition,
  writeObject,
  type ArObject,
  type BoMSchema,
  type Collection,
  type ObjectRelation,
} from './api'
import { JsonYamlEditor, isCompositionDocument, type EditorFormat } from './JsonYamlEditor'
import { CopyIdButton, CopyableId } from './CopyId'
import { FormFooterActions } from './FormFooterActions'
import { SchemaInstanceForm, defaultValueForSchema } from './SchemaInstanceForm'

export type ObjectViewMode = 'form' | 'json' | 'yaml'
export type ObjectPageMode = 'view' | 'edit' | 'create'

const VIEW_MODES = [
  { value: 'form', label: 'Visual' },
  { value: 'json', label: 'JSON' },
  { value: 'yaml', label: 'YAML' },
]

const COMPOSITION_ROLES: Record<string, { role: string; target: string }[]> = {
  Database: [{ role: 'CONTAINS', target: 'Dataset' }],
  AiAgent: [
    { role: 'USES_PROMPT', target: 'Prompt' },
    { role: 'HAS_SKILL', target: 'Skill' },
    { role: 'USES_TOOL', target: 'Tool' },
    { role: 'CONNECTS_TO', target: 'McpServer' },
  ],
  Skill: [{ role: 'USES_TOOL', target: 'Tool' }],
  Tool: [{ role: 'BACKED_BY', target: 'McpServer' }],
  ModelFamily: [{ role: 'HAS_VERSION', target: 'ModelVersion' }],
  ModelVersion: [{ role: 'SUPPORTS', target: 'Modality' }],
}

function compositionTemplate(accepted: string[]): Record<string, unknown> {
  const objects = accepted.map((objectType) => ({
    type: objectType,
    schemaVersion: '1.0.0',
    payload: { name: '' },
  }))
  const indexByType = new Map(accepted.map((t, i) => [t, i]))
  const relations: { sourceKey: string; role: string; targetKey: string }[] = []
  for (const [sourceType, edges] of Object.entries(COMPOSITION_ROLES)) {
    const sourceIdx = indexByType.get(sourceType)
    if (sourceIdx === undefined) continue
    for (const edge of edges) {
      const targetIdx = indexByType.get(edge.target)
      if (targetIdx === undefined) continue
      relations.push({
        sourceKey: `obj-${sourceIdx}`,
        role: edge.role,
        targetKey: `obj-${targetIdx}`,
      })
    }
  }
  return { objects, relations }
}

function objectLabel(object: ArObject): string {
  const name = object.payload?.name
  if (typeof name === 'string' && name.trim()) {
    return name
  }
  return 'Object'
}

function relatedObjectTitle(object: ArObject): string {
  const name = object.payload?.name
  if (typeof name === 'string' && name.trim()) {
    return name
  }
  return object.id
}

function isRawMode(mode: ObjectViewMode): mode is EditorFormat {
  return mode === 'json' || mode === 'yaml'
}

function ObjectDocumentPane({
  schema,
  payload,
  document,
  viewMode,
  onPayloadChange,
  onDocumentChange,
  onParseError,
  readOnly,
}: {
  schema: BoMSchema | null
  payload: Record<string, unknown>
  document: unknown
  viewMode: ObjectViewMode
  onPayloadChange: (next: Record<string, unknown>) => void
  onDocumentChange: (next: unknown) => void
  onParseError?: (error: string | null) => void
  readOnly?: boolean
}) {
  if (viewMode === 'form') {
    if (!schema) {
      return (
        <Text size="sm" c="dimmed">
          Loading schema…
        </Text>
      )
    }
    return (
      <Paper withBorder p="md">
        <SchemaInstanceForm
          schema={schema.contentSchema}
          value={payload}
          onChange={onPayloadChange}
          readOnly={readOnly}
        />
      </Paper>
    )
  }
  return (
    <JsonYamlEditor
      value={document}
      onChange={onDocumentChange}
      format={viewMode}
      onFormatChange={() => undefined}
      onParseError={onParseError}
      readOnly={readOnly}
      showFormatToggle={false}
    />
  )
}

function RelatedObjects({
  collectionId,
  relations,
}: {
  collectionId: string
  relations: ObjectRelation[]
}) {
  return (
    <Paper withBorder p="sm">
      <Text size="sm" fw={650} mb="xs">
        Related objects
      </Text>
      {relations.length === 0 ? (
        <Text size="sm" c="dimmed">
          No in-collection relations.
        </Text>
      ) : (
        <Stack gap={4}>
          {relations.map((rel) => (
            <Group key={rel.edgeId} gap="xs" wrap="nowrap">
              <Text size="xs" c="dimmed" style={{ flexShrink: 0 }}>
                {rel.direction === 'OUTGOING' ? `${rel.role} →` : `← ${rel.role}`}
              </Text>
              <Text
                component={Link}
                to={`/collections/${collectionId}/objects/${rel.related.id}`}
                size="sm"
                c="blue"
                style={{ textDecoration: 'none' }}
              >
                {relatedObjectTitle(rel.related)}
              </Text>
            </Group>
          ))}
        </Stack>
      )}
    </Paper>
  )
}

export function ObjectDetailPage() {
  return <ObjectPage mode="view" />
}

export function ObjectFormPage({ mode }: { mode: 'create' | 'edit' }) {
  return <ObjectPage mode={mode} />
}

function ObjectPage({ mode }: { mode: ObjectPageMode }) {
  const { id = '', objectId = '' } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const readOnly = mode === 'view'
  const [collection, setCollection] = useState<Collection | null>(null)
  const [object, setObject] = useState<ArObject | null>(null)
  const [type, setType] = useState('')
  const [schemaVersion, setSchemaVersion] = useState('1.0.0')
  const [schema, setSchema] = useState<BoMSchema | null>(null)
  const [payload, setPayload] = useState<Record<string, unknown>>({})
  const [rawDocument, setRawDocument] = useState<unknown>({})
  const [viewMode, setViewMode] = useState<ObjectViewMode>('form')
  const [relations, setRelations] = useState<ObjectRelation[]>([])
  const [rawError, setRawError] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function loadSchemaForType(objectType: string, preferredVersion?: string) {
    const versions = await listSchemasByType(objectType)
    const chosen =
      versions.find((s) => s.version === preferredVersion) ??
      versions.find((s) => s.version === '1.0.0') ??
      versions[versions.length - 1]
    if (!chosen) {
      throw new Error(`No schema registered for type ${objectType}`)
    }
    setSchema(chosen)
    setSchemaVersion(chosen.version)
    return chosen
  }

  useEffect(() => {
    setError(null)
    setObject(null)
    void (async () => {
      const c = await getCollection(id)
      setCollection(c)
      if (mode === 'create') {
        const allowed = c.types.map((t) => t.objectType)
        const requested = searchParams.get('type')
        const initialType = (requested && allowed.includes(requested) ? requested : allowed[0]) || ''
        setType(initialType)
        if (initialType) {
          const s = await loadSchemaForType(initialType)
          const next = defaultValueForSchema(s.contentSchema) as Record<string, unknown>
          setPayload(next)
          setRawDocument({ type: initialType, schemaVersion: s.version, payload: next })
        }
        setRelations([])
        return
      }
      const [o, rels] = await Promise.all([getObject(id, objectId), listObjectRelations(id, objectId)])
      setObject(o)
      setRelations(rels)
      setType(o.type)
      const next = { ...(o.payload ?? {}) }
      setPayload(next)
      setRawDocument({ id: o.id, type: o.type, schemaVersion: o.schemaVersion, payload: next })
      await loadSchemaForType(o.type, o.schemaVersion)
    })().catch((e) => setError(String(e)))
  }, [id, objectId, mode, searchParams])

  async function onTypeChange(nextType: string) {
    setType(nextType)
    try {
      const s = await loadSchemaForType(nextType)
      const next = defaultValueForSchema(s.contentSchema) as Record<string, unknown>
      setPayload(next)
      if (!isCompositionDocument(rawDocument)) {
        setRawDocument({ type: nextType, schemaVersion: s.version, payload: next })
      }
    } catch (e) {
      setError(String(e))
    }
  }

  function switchViewMode(next: ObjectViewMode) {
    if (next === viewMode) return
    if (readOnly) {
      setViewMode(next)
      return
    }
    if (next === 'form') {
      if (rawError) {
        setError('Fix JSON/YAML parse errors before switching to visual mode')
        return
      }
      if (isCompositionDocument(rawDocument)) {
        setError(
          'Visual mode edits one object. Load a single-object document, or save the composition from JSON/YAML.',
        )
        return
      }
      if (rawDocument && typeof rawDocument === 'object' && !Array.isArray(rawDocument)) {
        const doc = rawDocument as Record<string, unknown>
        if (doc.payload && typeof doc.payload === 'object' && !Array.isArray(doc.payload)) {
          setPayload(doc.payload as Record<string, unknown>)
          if (typeof doc.type === 'string') setType(doc.type)
        } else {
          setPayload(doc)
        }
      }
    } else if (viewMode === 'form' && !isCompositionDocument(rawDocument)) {
      setRawDocument({
        ...(mode !== 'create' && objectId ? { id: objectId } : {}),
        type,
        schemaVersion,
        payload,
      })
    }
    setError(null)
    setViewMode(next)
  }

  function loadCompositionTemplate() {
    if (!collection) return
    const doc = compositionTemplate(collection.types.map((t) => t.objectType))
    setRawDocument(doc)
    setRawError(null)
    setError(null)
    setViewMode('json')
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (readOnly) return
    if (isRawMode(viewMode) && rawError) {
      setError(rawError)
      return
    }
    try {
      if (isRawMode(viewMode) && isCompositionDocument(rawDocument)) {
        if (mode === 'edit') {
          setError('Compositions create multiple objects. Use Visual to edit a single object.')
          return
        }
        const saved = await writeComposition(id, {
          objects: rawDocument.objects,
          relations: (rawDocument.relations ?? []) as {
            sourceKey: string
            role: string
            targetKey: string
          }[],
        })
        const first = saved[0]
        navigate(first ? `/collections/${id}/objects/${first.id}` : `/collections/${id}`)
        return
      }

      let writeType = type
      let writeVersion = schemaVersion
      let writePayload = payload
      let writeId = mode === 'edit' ? objectId : undefined
      if (isRawMode(viewMode) && rawDocument && typeof rawDocument === 'object' && !Array.isArray(rawDocument)) {
        const doc = rawDocument as Record<string, unknown>
        if (doc.payload && typeof doc.payload === 'object' && !Array.isArray(doc.payload)) {
          writePayload = doc.payload as Record<string, unknown>
          if (typeof doc.type === 'string') writeType = doc.type
          if (typeof doc.schemaVersion === 'string') writeVersion = doc.schemaVersion
          if (typeof doc.id === 'string') writeId = doc.id
        } else {
          writePayload = doc
        }
      }

      const body = writeId
        ? { id: writeId, type: writeType, schemaVersion: writeVersion, payload: writePayload }
        : { type: writeType, schemaVersion: writeVersion, payload: writePayload }
      const saved = await writeObject(id, body)
      navigate(`/collections/${id}/objects/${saved.id}`)
    } catch (err) {
      setError(String(err))
    }
  }

  if (!collection || (mode !== 'create' && !object)) {
    return error ? <Alert color="red">{error}</Alert> : <Loader size="sm" />
  }

  const compositionMode = !readOnly && isRawMode(viewMode) && isCompositionDocument(rawDocument)
  const title =
    mode === 'create' ? 'Create object' : object ? objectLabel(object) : 'Object'
  const document = readOnly && object
    ? { id: object.id, type: object.type, schemaVersion: object.schemaVersion, payload: object.payload }
    : rawDocument

  return (
    <ScrollArea style={{ flex: 1, minHeight: 0 }}>
      <Stack
        gap="md"
        maw={720}
        component={readOnly ? 'div' : 'form'}
        onSubmit={readOnly ? undefined : (e) => void onSubmit(e as FormEvent)}
      >
        <Group justify="space-between" align="flex-end" wrap="wrap" gap="md">
          <Group gap="xs" wrap="nowrap" align="flex-end" style={{ minWidth: 0 }}>
            <Title order={3} lineClamp={1} style={{ lineHeight: 1.2 }}>
              {title}
            </Title>
            {object && (
              <>
                <CopyableId id={object.id} />
                <CopyIdButton id={object.id} label="Object id copied" />
              </>
            )}
          </Group>
          <Group gap={6} wrap="wrap" align="flex-end" style={{ marginLeft: 'auto' }}>
            <Anchor component={Link} to={`/collections/${id}`} size="sm">
              {collection.name}
            </Anchor>
            {type && (
              <>
                <Text size="sm" c="dimmed">
                  /
                </Text>
                {mode === 'create' && !compositionMode ? (
                  <Select
                    size="xs"
                    value={type}
                    data={collection.types.map((t) => ({ value: t.objectType, label: t.objectType }))}
                    onChange={(v) => v && void onTypeChange(v)}
                    w={180}
                  />
                ) : (
                  <Anchor
                    component={Link}
                    to={`/schemas/${encodeURIComponent(type)}/${encodeURIComponent(schema?.version || schemaVersion)}`}
                    size="sm"
                  >
                    {type}@{schema?.version || schemaVersion}
                  </Anchor>
                )}
              </>
            )}
          </Group>
        </Group>

        {error && <Alert color="red">{error}</Alert>}

        {compositionMode && (
          <Text size="xs" c="dimmed">
            Composition: objects + relations (keys obj-0, obj-1, …).
          </Text>
        )}

        <Group>
          <SegmentedControl
            value={viewMode}
            onChange={(v) => switchViewMode(v as ObjectViewMode)}
            data={VIEW_MODES}
            w={280}
          />
          {mode === 'create' && (
            <Button type="button" variant="light" onClick={loadCompositionTemplate}>
              Composition template
            </Button>
          )}
        </Group>

        <ObjectDocumentPane
          schema={schema}
          payload={payload}
          document={document}
          viewMode={viewMode}
          onPayloadChange={setPayload}
          onDocumentChange={setRawDocument}
          onParseError={setRawError}
          readOnly={readOnly}
        />

        {mode !== 'create' && <RelatedObjects collectionId={id} relations={relations} />}

        {mode === 'view' && (
          <FormFooterActions
            secondary={{ label: 'Back', to: `/collections/${id}` }}
            primary={{ label: 'Edit', to: `/collections/${id}/objects/${objectId}/edit` }}
          />
        )}
        {mode === 'edit' && (
          <FormFooterActions
            secondary={{ label: 'Cancel', to: `/collections/${id}/objects/${objectId}` }}
            primary={{ label: 'Save', type: 'submit' }}
          />
        )}
        {mode === 'create' && (
          <FormFooterActions
            secondary={{ label: 'Cancel', to: `/collections/${id}` }}
            primary={{ label: compositionMode ? 'Create composition' : 'Save', type: 'submit' }}
          />
        )}
      </Stack>
    </ScrollArea>
  )
}
