import { Alert, Stack, TextInput, Textarea, Title } from '@mantine/core'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { FormFooterActions } from '../FormFooterActions'

export function ApplicationFormPage() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      const created = await api.createApplication({
        name: name.trim(),
        description: description.trim() || undefined,
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
      <FormFooterActions
        secondary={{ label: 'Cancel', to: '/applications' }}
        primary={{ label: 'Save', type: 'submit' }}
      />
    </Stack>
  )
}
