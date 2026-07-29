import { useCallback, useRef, useState } from 'react'
import { Alert, Badge, Button, Code, Group, Paper, Stack, Text, Title } from '@mantine/core'
import { Link } from 'react-router-dom'
import { validateGraphDraft } from './api'
import { JsonYamlEditor, type JsonYamlEditorHandle } from './JsonYamlEditor'
import type { GraphValidationResult } from './types'

const EXAMPLE_GRAPH = {
  entities: [
    {
      id: '11111111-1111-4111-8111-111111111111',
      type: 'Product',
      schemaVersion: '1.0.0',
      payload: {
        name: 'Payments API',
        version: '2.3.1',
      },
      annotations: {
        app: 'payments-api',
        appVersion: '2.3.1',
      },
    },
    {
      id: '22222222-2222-4222-8222-222222222222',
      type: 'Component',
      schemaVersion: '1.0.0',
      payload: {
        name: 'payment-core',
        version: '2.3.1',
        ecosystem: 'maven',
        kind: 'library',
      },
      annotations: {
        app: 'payments-api',
        appVersion: '2.3.1',
      },
    },
  ],
  edges: [
    {
      id: '33333333-3333-4333-8333-333333333333',
      source: '11111111-1111-4111-8111-111111111111',
      target: '22222222-2222-4222-8222-222222222222',
      role: 'CONTAINS',
      type: 'CanonicalEdge',
      schemaVersion: '1.0.0',
      properties: {},
    },
  ],
}

export function graphShapeError(value: unknown): string | null {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return 'Graph document must be an object'
  }
  const graph = value as Record<string, unknown>
  if (!Array.isArray(graph.entities)) return 'Graph document must contain an entities array'
  if (!Array.isArray(graph.edges)) return 'Graph document must contain an edges array'
  return null
}

export function ObjectLinterPage() {
  const editorRef = useRef<JsonYamlEditorHandle>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<GraphValidationResult | null>(null)
  const onDraftParsed = useCallback(() => {
    setError(null)
    setResult(null)
  }, [])

  async function validate() {
    const parsed = editorRef.current?.getParsedForSubmit()
    if (!parsed?.ok) {
      setError(parsed?.error ?? 'Invalid graph document')
      setResult(null)
      return
    }
    const shapeError = graphShapeError(parsed.value)
    if (shapeError) {
      setError(shapeError)
      setResult(null)
      return
    }

    setBusy(true)
    setError(null)
    setResult(null)
    try {
      setResult(await validateGraphDraft(parsed.value))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const valid = result != null && result.issues.length === 0

  return (
    <Stack gap="md" style={{ flex: 1, minHeight: 0 }}>
      <Group justify="space-between" align="flex-start">
        <div>
          <Title order={3}>Object linter</Title>
          <Text size="sm" c="dimmed">
            Author a complete graph batch as YAML or JSON and validate it against registered entity
            schemas and allowed edge relations. Validation never persists data.
          </Text>
        </div>
        <Group>
          <Button variant="default" component={Link} to="/schemas">
            Browse schemas
          </Button>
          <Button loading={busy} onClick={validate}>
            Validate graph
          </Button>
        </Group>
      </Group>

      <Alert color="blue" title="Graph references">
        Entities without IDs can be validated when they are not referenced by an edge. For a
        connected draft, assign UUIDs and use those values in edge <Code>source</Code> and{' '}
        <Code>target</Code>.
      </Alert>

      <Paper withBorder p="sm">
        <JsonYamlEditor
          ref={editorRef}
          value={EXAMPLE_GRAPH}
          rollbackValue={EXAMPLE_GRAPH}
          minHeight={520}
          onDraftParsed={onDraftParsed}
        />
      </Paper>

      {error && (
        <Alert color="red" title="Cannot validate">
          {error}
        </Alert>
      )}

      {result && (
        <Paper withBorder p="md">
          <Group mb="sm">
            <Text fw={700}>Validation result</Text>
            <Badge color={valid ? 'green' : 'red'}>{valid ? 'valid' : 'invalid'}</Badge>
          </Group>
          {valid ? (
            <Text size="sm">The graph conforms to the registered schemas and edge rules.</Text>
          ) : (
            <Stack gap="xs">
              {result.issues.map((issue, index) => (
                <Alert key={`${issue.code}:${issue.path}:${index}`} color="red" title={issue.code}>
                  <Text size="sm">{issue.message}</Text>
                  {issue.path && (
                    <Code mt={4} style={{ display: 'inline-block' }}>
                      {issue.path}
                    </Code>
                  )}
                </Alert>
              ))}
            </Stack>
          )}
        </Paper>
      )}
    </Stack>
  )
}
