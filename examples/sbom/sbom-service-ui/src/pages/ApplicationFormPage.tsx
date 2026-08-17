import { Alert, Stack, TagsInput, TextInput, Textarea, Title } from '@mantine/core'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { FormFooterActions } from '../FormFooterActions'

export function ApplicationFormPage() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [targetVersion, setTargetVersion] = useState('')
  const [tags, setTags] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (!targetVersion.trim()) {
      setError('Target version is required')
      return
    }
    try {
      const created = await api.createApplication({
        name: name.trim(),
        description: description.trim() || undefined,
        targetVersion: targetVersion.trim(),
        tags,
      })
      navigate(`/applications/${created.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not create application')
    }
  }

  return (
    <Stack gap="md" component="form" onSubmit={onSubmit} maw={560} style={{ overflow: 'auto' }}>
      <Title order={3}>New application</Title>
      {error && <Alert color="red">{error}</Alert>}
      <TextInput label="Name" required value={name} onChange={(e) => setName(e.currentTarget.value)} />
      <Textarea
        label="Description"
        autosize
        minRows={2}
        value={description}
        onChange={(e) => setDescription(e.currentTarget.value)}
      />
      <TextInput
        label="Target version"
        required
        placeholder="1.0.0"
        value={targetVersion}
        onChange={(e) => setTargetVersion(e.currentTarget.value)}
      />
      <TagsInput
        label="Tags"
        placeholder="Add a tag"
        value={tags}
        onChange={setTags}
        clearable
      />
      <FormFooterActions
        secondary={{ label: 'Cancel', to: '/applications' }}
        primary={{ label: 'Save', type: 'submit' }}
      />
    </Stack>
  )
}
