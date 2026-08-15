import { Alert, Select, Stack, TextInput, Textarea, Title } from '@mantine/core'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { FormFooterActions } from '../FormFooterActions'

export function PortfolioFormPage() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [uniqueness, setUniqueness] = useState('UNIQUE_APP')
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      const created = await api.createPortfolio({
        name: name.trim(),
        description: description.trim() || undefined,
        uniqueness,
      })
      navigate(`/portfolios/${created.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not create portfolio')
    }
  }

  return (
    <Stack gap="md" component="form" onSubmit={onSubmit} maw={560} style={{ overflow: 'auto' }}>
      <Title order={3}>New portfolio</Title>
      {error && <Alert color="red">{error}</Alert>}
      <TextInput label="Name" required value={name} onChange={(e) => setName(e.currentTarget.value)} />
      <Textarea
        label="Description"
        autosize
        minRows={2}
        value={description}
        onChange={(e) => setDescription(e.currentTarget.value)}
      />
      <Select
        label="Uniqueness"
        data={[
          { value: 'UNIQUE_APP', label: 'Unique application' },
          { value: 'UNIQUE_APP_VERSION', label: 'Unique application version' },
          { value: 'NOT_UNIQUE', label: 'Not unique' },
        ]}
        value={uniqueness}
        onChange={(v) => setUniqueness(v || 'UNIQUE_APP')}
      />
      <FormFooterActions
        secondary={{ label: 'Cancel', to: '/portfolios' }}
        primary={{ label: 'Save', type: 'submit' }}
      />
    </Stack>
  )
}
