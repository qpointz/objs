import { useEffect, useRef, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Code,
  Group,
  Loader,
  MultiSelect,
  Paper,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  createNextMajorSchema,
  getSchemaEdges,
  getSchema,
  listSchemas,
  lintSchema,
  replaceSchemaEdges,
  schemaDetailPath,
  updateSchema,
} from './api'
import { EdgeRelationsEditor } from './EdgeRelationsEditor'
import { JsonYamlEditor, type JsonYamlEditorHandle } from './JsonYamlEditor'
import { SchemaVisualBuilder } from './SchemaVisualBuilder'
import { emptyObjectSchema } from './schemaDsl'
import type {
  BoMSchemaNode,
  BoMSchemaUsage,
  EdgeRelationRequest,
  SchemaLintResponse,
} from './types'

export type SchemaExpertDocument = {
  type: string
  version: string
  usages: BoMSchemaUsage[]
  contentSchema: BoMSchemaNode
  allowedRelations?: EdgeRelationRequest[]
}

function expertDocument(
  type: string,
  version: string,
  usages: BoMSchemaUsage[],
  contentSchema: BoMSchemaNode,
  allowedRelations: EdgeRelationRequest[],
): SchemaExpertDocument {
  return {
    type,
    version,
    usages,
    contentSchema,
    ...(usages.includes('EDGE_PROPERTIES') ? { allowedRelations } : {}),
  }
}

export function parseSchemaExpertDocument(
  value: unknown,
): { ok: true; value: SchemaExpertDocument } | { ok: false; error: string } {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return { ok: false, error: 'Expert document must be an object' }
  }
  const doc = value as Partial<SchemaExpertDocument>
  if (typeof doc.type !== 'string' || !doc.type.trim()) {
    return { ok: false, error: 'Expert document type must not be blank' }
  }
  if (typeof doc.version !== 'string' || !doc.version.trim()) {
    return { ok: false, error: 'Expert document version must not be blank' }
  }
  if (!Array.isArray(doc.usages) || doc.usages.length === 0) {
    return { ok: false, error: 'Expert document usages must be a non-empty array' }
  }
  if (doc.usages.some((usage) => usage !== 'ENTITY' && usage !== 'EDGE_PROPERTIES')) {
    return { ok: false, error: 'Expert document contains an unsupported usage' }
  }
  if (!doc.contentSchema || typeof doc.contentSchema !== 'object') {
    return { ok: false, error: 'Expert document contentSchema is required' }
  }
  if (doc.allowedRelations != null && !Array.isArray(doc.allowedRelations)) {
    return { ok: false, error: 'Expert document allowedRelations must be an array' }
  }
  return {
    ok: true,
    value: {
      type: doc.type,
      version: doc.version,
      usages: doc.usages,
      contentSchema: doc.contentSchema,
      allowedRelations: doc.allowedRelations ?? [],
    },
  }
}

export function SchemaLinterPage() {
  const params = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const typeParam = params.type ? decodeURIComponent(params.type) : undefined
  const versionParam = params.version ? decodeURIComponent(params.version) : undefined
  const createVersionMode = searchParams.get('mode') === 'create-version'
  const isNewDraft = !typeParam

  const editorRef = useRef<JsonYamlEditorHandle>(null)
  const [typeName, setTypeName] = useState(typeParam ?? 'NewType')
  const [version, setVersion] = useState(versionParam ?? '1.0.0')
  const [usages, setUsages] = useState<BoMSchemaUsage[]>(['ENTITY'])
  const [contentSchema, setContentSchema] = useState<BoMSchemaNode>(emptyObjectSchema())
  const [mode, setMode] = useState<'visual' | 'expert'>('visual')
  const [loading, setLoading] = useState(!isNewDraft)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lint, setLint] = useState<SchemaLintResponse | null>(null)
  const [edgeRelations, setEdgeRelations] = useState<EdgeRelationRequest[]>([])
  const [expertSnapshot, setExpertSnapshot] = useState<SchemaExpertDocument>(() =>
    expertDocument('NewType', '1.0.0', ['ENTITY'], emptyObjectSchema(), []),
  )
  const [entityTypes, setEntityTypes] = useState<string[]>([])
  const [loadedWasEdgeSchema, setLoadedWasEdgeSchema] = useState(false)

  useEffect(() => {
    if (isNewDraft) {
      setLoading(false)
      return
    }
    let cancelled = false
    ;(async () => {
      setLoading(true)
      setError(null)
      try {
        const schema = await getSchema(typeParam!, versionParam!)
        const relations = schema.usages.includes('EDGE_PROPERTIES')
          ? await getSchemaEdges(schema.type, schema.version)
          : []
        const relationDrafts = relations.map((rule) => ({
          sourceType: rule.sourceType,
          role: rule.role,
          targetType: rule.targetType,
          emptyPropertiesAllowed: rule.emptyPropertiesAllowed,
        }))
        if (cancelled) return
        setTypeName(schema.type)
        setVersion(schema.version)
        setUsages(schema.usages)
        setContentSchema(schema.contentSchema)
        setLoadedWasEdgeSchema(schema.usages.includes('EDGE_PROPERTIES'))
        setEdgeRelations(relationDrafts)
        setExpertSnapshot(
          expertDocument(
            schema.type,
            schema.version,
            schema.usages,
            schema.contentSchema,
            relationDrafts,
          ),
        )
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e))
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [isNewDraft, typeParam, versionParam])

  useEffect(() => {
    let cancelled = false
    listSchemas('ENTITY')
      .then((schemas) => {
        if (!cancelled) {
          setEntityTypes([...new Set(schemas.map((schema) => schema.type))].sort())
        }
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e))
      })
    return () => {
      cancelled = true
    }
  }, [])

  function validateEdgeRelations(
    documentUsages: BoMSchemaUsage[],
    relations: EdgeRelationRequest[],
  ): boolean {
    if (!documentUsages.includes('EDGE_PROPERTIES')) return true
    const keys = new Set<string>()
    for (const [index, relation] of relations.entries()) {
      if (!relation.sourceType.trim() || !relation.role.trim() || !relation.targetType.trim()) {
        setError(`Relation ${index + 1}: source, role, and target are required`)
        return false
      }
      const key = `${relation.sourceType.trim()}\u0000${relation.role.trim()}\u0000${relation.targetType.trim()}`
      if (keys.has(key)) {
        setError(`Relation ${index + 1}: duplicate source–role–target relation`)
        return false
      }
      keys.add(key)
    }
    return true
  }

  function applyExpertDocument(document: SchemaExpertDocument) {
    if (isNewDraft) {
      setTypeName(document.type)
      setVersion(document.version)
    }
    setUsages(document.usages)
    setContentSchema(document.contentSchema)
    setEdgeRelations(document.allowedRelations ?? [])
  }

  function currentDocument(): SchemaExpertDocument | null {
    if (mode === 'expert') {
      const parsed = editorRef.current?.getParsedForSubmit()
      if (!parsed?.ok) {
        setError(parsed?.error ?? 'Invalid expert document')
        return null
      }
      const document = parseSchemaExpertDocument(parsed.value)
      if (!document.ok) {
        setError(document.error)
        return null
      }
      if (
        !isNewDraft &&
        (document.value.type !== typeName || document.value.version !== version)
      ) {
        setError('Type and version cannot change while editing an existing schema')
        return null
      }
      return document.value
    }
    return expertDocument(typeName, version, usages, contentSchema, edgeRelations)
  }

  async function runLint() {
    const document = currentDocument()
    if (
      !document ||
      !validateEdgeRelations(document.usages, document.allowedRelations ?? [])
    ) {
      return null
    }
    setBusy(true)
    setError(null)
    try {
      const result = await lintSchema(document.type || 'Draft', document.version || '0', {
        contentSchema: document.contentSchema,
        usages: document.usages,
      })
      setLint(result)
      if (result.valid && result.schema) {
        const normalized = expertDocument(
          result.schema.type,
          result.schema.version,
          result.schema.usages,
          result.schema.contentSchema,
          document.allowedRelations ?? [],
        )
        applyExpertDocument(normalized)
        setExpertSnapshot(normalized)
      }
      return result
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
      return null
    } finally {
      setBusy(false)
    }
  }

  async function onSaveUpdate() {
    const document = currentDocument()
    if (
      !document ||
      !validateEdgeRelations(document.usages, document.allowedRelations ?? [])
    ) {
      return
    }
    setBusy(true)
    setError(null)
    try {
      if (loadedWasEdgeSchema && !document.usages.includes('EDGE_PROPERTIES')) {
        await replaceSchemaEdges(document.type, document.version, [])
      }
      const saved = await updateSchema(document.type, document.version, {
        contentSchema: document.contentSchema,
        usages: document.usages,
      })
      if (document.usages.includes('EDGE_PROPERTIES')) {
        await replaceSchemaEdges(saved.type, saved.version, document.allowedRelations ?? [])
      }
      setContentSchema(saved.contentSchema)
      setLint({
        valid: true,
        issues: [],
        schema: saved,
        jsonSchema: null,
      })
      navigate(schemaDetailPath(saved.type, saved.version))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  async function onCreateVersion() {
    const document = currentDocument()
    if (
      !document ||
      !validateEdgeRelations(document.usages, document.allowedRelations ?? [])
    ) {
      return
    }
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
      if (document.usages.includes('EDGE_PROPERTIES')) {
        await replaceSchemaEdges(created.type, created.version, document.allowedRelations ?? [])
      }
      navigate(schemaDetailPath(created.type, created.version))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  function changeMode(next: 'visual' | 'expert') {
    if (next === mode) return
    if (next === 'expert') {
      setExpertSnapshot(
        expertDocument(typeName, version, usages, contentSchema, edgeRelations),
      )
    } else {
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
        if (
          !isNewDraft &&
          (document.value.type !== typeName || document.value.version !== version)
        ) {
          setError('Type and version cannot change while editing an existing schema')
          return
        }
        applyExpertDocument(document.value)
      }
    }
    setError(null)
    setMode(next)
  }

  if (loading) {
    return <Loader />
  }

  return (
    <Stack gap="md" style={{ flex: 1, minHeight: 0 }}>
      <Group justify="space-between" align="flex-start">
        <div>
          <Title order={3}>Schema linter</Title>
          <Text size="sm" c="dimmed">
            Author visually or edit the complete schema document in Expert JSON/YAML mode. Lint is
            server-authoritative and does not persist.
            {createVersionMode
              ? ' Create version writes the next major version.'
              : ' Save updates the opened version.'}
          </Text>
        </div>
        <Group>
          <Button variant="default" component={Link} to="/schemas">
            Back to explorer
          </Button>
          <Button variant="light" loading={busy} onClick={() => runLint()}>
            Lint
          </Button>
          {!isNewDraft && !createVersionMode && (
            <Button loading={busy} onClick={onSaveUpdate}>
              Save update
            </Button>
          )}
          <Button
            color={createVersionMode || isNewDraft ? 'blue' : 'gray'}
            variant={createVersionMode || isNewDraft ? 'filled' : 'light'}
            loading={busy}
            onClick={onCreateVersion}
          >
            {isNewDraft
              ? 'Create schema'
              : createVersionMode
                ? 'Save new version'
                : 'Create version'}
          </Button>
        </Group>
      </Group>

      <Paper withBorder p={4} w="fit-content">
        <Group gap={4}>
          <Button
            size="xs"
            variant={mode === 'visual' ? 'filled' : 'subtle'}
            onClick={() => changeMode('visual')}
          >
            Visual
          </Button>
          <Button
            size="xs"
            variant={mode === 'expert' ? 'filled' : 'subtle'}
            onClick={() => changeMode('expert')}
          >
            Expert JSON/YAML
          </Button>
        </Group>
      </Paper>

      {error && (
        <Alert color="red" title="Error">
          {error}
        </Alert>
      )}

      {mode === 'visual' ? (
        <>
          <Group grow align="flex-end">
            <TextInput
              label="Type"
              value={typeName}
              onChange={(e) => setTypeName(e.currentTarget.value)}
              disabled={!isNewDraft}
            />
            <TextInput
              label="Opened version"
              value={version}
              onChange={(e) => setVersion(e.currentTarget.value)}
              disabled={!isNewDraft}
              description={
                createVersionMode
                  ? 'Create version increments the highest major across all versions of this type'
                  : undefined
              }
            />
            <MultiSelect
              label="Usages"
              data={[
                { value: 'ENTITY', label: 'ENTITY' },
                { value: 'EDGE_PROPERTIES', label: 'EDGE_PROPERTIES' },
              ]}
              value={usages}
              onChange={(values) => setUsages(values as BoMSchemaUsage[])}
            />
          </Group>

          {usages.includes('EDGE_PROPERTIES') && (
            <EdgeRelationsEditor
              value={edgeRelations}
              entityTypes={entityTypes}
              onChange={setEdgeRelations}
            />
          )}

          <SchemaVisualBuilder value={contentSchema} onChange={setContentSchema} />
        </>
      ) : (
        <Paper withBorder p="sm" style={{ minHeight: 420 }}>
          <Text size="sm" c="dimmed" mb="sm">
            Edit the complete schema document directly. For edge schemas,{' '}
            <Code>allowedRelations</Code> contains source–role–target definitions.
          </Text>
          <JsonYamlEditor
            ref={editorRef}
            value={expertSnapshot}
            rollbackValue={expertSnapshot}
            minHeight={520}
          />
        </Paper>
      )}

      {lint && (
        <Paper withBorder p="md">
          <Group mb="sm">
            <Text fw={600}>Lint result</Text>
            <Badge color={lint.valid ? 'green' : 'red'}>{lint.valid ? 'valid' : 'invalid'}</Badge>
          </Group>
          {lint.issues.length > 0 ? (
            <Stack gap={4}>
              {lint.issues.map((issue, i) => (
                <Text key={i} size="sm" c="red">
                  [{issue.code}] {issue.message}
                </Text>
              ))}
            </Stack>
          ) : (
            <Text size="sm" c="dimmed" mb="sm">
              Normalized successfully.
            </Text>
          )}
          {lint.jsonSchema && (
            <>
              <Text fw={600} size="sm" mt="sm" mb={4}>
                Generated JSON Schema
              </Text>
              <Code block>{JSON.stringify(lint.jsonSchema, null, 2)}</Code>
            </>
          )}
        </Paper>
      )}
    </Stack>
  )
}
