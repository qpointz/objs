import { Alert, Loader, MultiSelect, Select, Stack, Text, TextInput, Textarea, Title } from '@mantine/core'
import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  createCollection,
  getCollection,
  listSchemas,
  patchCollection,
  type BoMSchema,
  type Collection,
} from './api'
import { FormFooterActions } from './FormFooterActions'

const WRITE_MODES = [
  { value: 'UUID_OR_IDENTIFIER', label: 'UUID or identifier' },
  { value: 'UUID', label: 'UUID' },
  { value: 'IDENTIFIER', label: 'Identifier' },
]

function uniqueObjectTypes(schemas: BoMSchema[]): string[] {
  const types = new Set<string>()
  for (const schema of schemas) {
    if (schema.usage && schema.usage !== 'ENTITY') continue
    types.add(schema.type)
  }
  return [...types].sort()
}

export function CollectionFormPage({ mode }: { mode: 'create' | 'edit' }) {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const [collection, setCollection] = useState<Collection | null>(null)
  const [schemaTypes, setSchemaTypes] = useState<string[]>([])
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [owner, setOwner] = useState('')
  const [ownerEmail, setOwnerEmail] = useState('')
  const [supportEmail, setSupportEmail] = useState('')
  const [sla, setSla] = useState('')
  const [objectWriteMode, setObjectWriteMode] = useState('UUID_OR_IDENTIFIER')
  const [types, setTypes] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(mode === 'edit')

  useEffect(() => {
    void (async () => {
      try {
        const schemas = await listSchemas()
        setSchemaTypes(uniqueObjectTypes(schemas))
        if (mode === 'edit') {
          const c = await getCollection(id)
          setCollection(c)
          setName(c.name)
          setDescription(c.description ?? '')
          setOwner(c.owner)
          setOwnerEmail(c.ownerEmail ?? '')
          setSupportEmail(c.supportEmail ?? '')
          setSla(c.sla ?? '')
          setObjectWriteMode(c.objectWriteMode)
          setTypes(c.types.map((t) => t.objectType))
        }
      } catch (e) {
        setError(String(e))
      } finally {
        setLoading(false)
      }
    })()
  }, [id, mode])

  const typeData = useMemo(() => schemaTypes.map((t) => ({ value: t, label: t })), [schemaTypes])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (types.length === 0) {
      setError('Select at least one accepted object type from existing schemas')
      return
    }
    const body = {
      name,
      description: description || null,
      owner,
      ownerEmail: ownerEmail || null,
      supportEmail: supportEmail || null,
      sla: sla || null,
      objectWriteMode,
      types: types.map((objectType) => ({ objectType })),
    }
    try {
      const saved = mode === 'create' ? await createCollection(body) : await patchCollection(id, body)
      navigate(`/collections/${saved.id}`)
    } catch (err) {
      setError(String(err))
    }
  }

  if (loading) return <Loader size="sm" />
  if (mode === 'edit' && !collection && error) return <Alert color="red">{error}</Alert>

  return (
    <Stack gap="md" component="form" onSubmit={onSubmit} maw={560} style={{ overflow: 'auto' }}>
      <Title order={3}>{mode === 'create' ? 'New collection' : 'Edit collection'}</Title>
      {error && <Alert color="red">{error}</Alert>}
      <TextInput label="Name" required value={name} onChange={(e) => setName(e.currentTarget.value)} />
      <Textarea
        label="Description"
        autosize
        minRows={2}
        value={description}
        onChange={(e) => setDescription(e.currentTarget.value)}
      />
      <TextInput label="Owner" required value={owner} onChange={(e) => setOwner(e.currentTarget.value)} />
      <TextInput
        label="Owner email"
        type="email"
        value={ownerEmail}
        onChange={(e) => setOwnerEmail(e.currentTarget.value)}
      />
      <TextInput
        label="Support email"
        type="email"
        value={supportEmail}
        onChange={(e) => setSupportEmail(e.currentTarget.value)}
      />
      <TextInput label="SLA" value={sla} onChange={(e) => setSla(e.currentTarget.value)} />
      <Select label="Write mode" data={WRITE_MODES} value={objectWriteMode} onChange={(v) => v && setObjectWriteMode(v)} />
      <MultiSelect
        label="Accepted object types"
        description="Choose from registered object schemas (managed in workbench)."
        data={typeData}
        searchable
        required
        value={types}
        onChange={setTypes}
        nothingFoundMessage="No schemas"
      />
      <FormFooterActions
        secondary={{
          label: 'Cancel',
          to: mode === 'edit' ? `/collections/${id}` : '/',
        }}
        primary={{ label: 'Save', type: 'submit' }}
      />
      {schemaTypes.length === 0 && (
        <Text size="sm" c="dimmed">
          No object schemas found. Create them in workbench first.
        </Text>
      )}
    </Stack>
  )
}
