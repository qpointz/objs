import {
  Alert,
  Badge,
  Group,
  Loader,
  ScrollArea,
  SegmentedControl,
  Select,
  Skeleton,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from './api/client'
import type { BoMSchema } from './api/types'
import { JsonYamlEditor, type EditorFormat } from './JsonYamlEditor'
import { SchemaTreeView } from './SchemaTreeView'
import { useSchemasOutlet } from './SchemasWorkspace'

type SchemaViewMode = 'form' | 'json' | 'yaml'

const VIEW_MODES = [
  { value: 'form', label: 'Visual' },
  { value: 'json', label: 'JSON' },
  { value: 'yaml', label: 'YAML' },
]

export function SchemaViewPage() {
  const navigate = useNavigate()
  const { type, version } = useParams<{ type: string; version?: string }>()
  const { catalog, usagePending, error: catalogError } = useSchemasOutlet()
  const entry = catalog.find((s) => s.type === type)
  const resolvedVersion = version ?? entry?.latestVersion
  const [schema, setSchema] = useState<BoMSchema | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [viewMode, setViewMode] = useState<SchemaViewMode>('form')
  const [rawFormat, setRawFormat] = useState<EditorFormat>('json')

  useEffect(() => {
    if (!type || !resolvedVersion) {
      setSchema(null)
      return
    }
    let cancelled = false
    setError(null)
    void api
      .getSchema(type, resolvedVersion)
      .then((s) => {
        if (!cancelled) setSchema(s)
      })
      .catch((e) => {
        if (!cancelled) {
          setSchema(null)
          setError(e instanceof Error ? e.message : String(e))
        }
      })
    return () => {
      cancelled = true
    }
  }, [type, resolvedVersion])

  function switchViewMode(next: SchemaViewMode) {
    setViewMode(next)
    if (next === 'json' || next === 'yaml') setRawFormat(next)
  }

  if (!type) {
    return <Alert color="red">Missing schema type</Alert>
  }

  const versions = entry?.versions ?? (schema ? [schema.version] : [])
  const latestVersion = entry?.latestVersion
  const isLatest = Boolean(resolvedVersion && latestVersion && resolvedVersion === latestVersion)
  const versionOptions = useMemo(
    () =>
      versions.map((v) => ({
        value: v,
        label: v,
      })),
    [versions],
  )

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <div>
        <Group justify="space-between" align="center" wrap="nowrap" gap="md">
          <Group gap="xs" align="center" wrap="nowrap" style={{ minWidth: 0, flex: 1 }}>
            <Title order={3} lineClamp={1}>
              {type}
            </Title>
            {isLatest && (
              <Badge size="sm" variant="light" color="blue">
                LATEST
              </Badge>
            )}
          </Group>
          {versions.length > 0 && (
            <Select
              size="sm"
              w={180}
              aria-label="Schema version"
              value={resolvedVersion ?? null}
              data={versionOptions}
              allowDeselect={false}
              renderOption={({ option }) => (
                <Group gap="xs" justify="space-between" wrap="nowrap" w="100%">
                  <Text size="sm">{option.value}</Text>
                  {option.value === latestVersion && (
                    <Badge size="xs" variant="light" color="blue">
                      LATEST
                    </Badge>
                  )}
                </Group>
              )}
              onChange={(v) => {
                if (!v || !type) return
                navigate(`/schemas/${encodeURIComponent(type)}/${encodeURIComponent(v)}`)
              }}
            />
          )}
        </Group>
        <Text size="sm" c="dimmed">
          {entry?.description || entry?.title || schema?.contentSchema.description || ''}
        </Text>
        {type && usagePending.has(type) ? (
          <Skeleton height={12} width={180} mt={8} />
        ) : (
          <Text size="xs" c="dimmed" mt={8}>
            {!entry || entry.usedIn.length === 0
              ? 'Not used in applications'
              : `Used in ${entry.usedIn.length} application${entry.usedIn.length === 1 ? '' : 's'}`}
          </Text>
        )}
      </div>

      {(error || catalogError) && <Alert color="red">{error || catalogError}</Alert>}

      <SegmentedControl
        value={viewMode}
        onChange={(v) => switchViewMode(v as SchemaViewMode)}
        data={VIEW_MODES}
        w={280}
      />

      {!resolvedVersion && catalog.length === 0 && !catalogError ? (
        <Loader size="sm" />
      ) : !schema && !error ? (
        <Loader size="sm" />
      ) : schema ? (
        <div style={{ flex: 1, minHeight: 0, position: 'relative' }}>
          <ScrollArea style={{ position: 'absolute', inset: 0 }}>
            {viewMode === 'form' ? (
              <SchemaTreeView schema={schema.contentSchema} />
            ) : (
              <JsonYamlEditor
                value={schema}
                onChange={() => undefined}
                format={rawFormat}
                onFormatChange={setRawFormat}
                readOnly
                showFormatToggle={false}
              />
            )}
          </ScrollArea>
        </div>
      ) : null}
    </Stack>
  )
}
