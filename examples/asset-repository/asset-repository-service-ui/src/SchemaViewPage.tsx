import {
  Alert,
  Anchor,
  Badge,
  Box,
  Group,
  Loader,
  SegmentedControl,
  Select,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { AllowedEdgesSection } from './AllowedEdgesSection'
import { getAllowedEdges, getSchema, type BoMSchema, type TypeAllowedEdges } from './api'
import { JsonYamlEditor, type EditorFormat } from './JsonYamlEditor'
import { SchemaTreeView } from './SchemaTreeView'
import { useSchemasOutlet } from './SchemasWorkspace'

type SchemaViewMode = 'form' | 'json' | 'yaml'

const VIEW_MODES = [
  { value: 'form', label: 'Visual' },
  { value: 'json', label: 'JSON' },
  { value: 'yaml', label: 'YAML' },
]

const HEX_COLOR = /^#[0-9a-fA-F]{6}$/

function TypeColorSwatch({ color }: { color?: string }) {
  const raw = color?.trim()
  if (!raw) return null
  const background =
    raw.toLowerCase() === 'nocolor'
      ? 'var(--mantine-color-gray-6)'
      : HEX_COLOR.test(raw)
        ? raw
        : null
  if (!background) return null
  return (
    <Box
      w={14}
      h={14}
      style={{
        flexShrink: 0,
        borderRadius: 3,
        background,
        border: '1px solid var(--mantine-color-default-border)',
      }}
    />
  )
}

export function SchemaViewPage() {
  const navigate = useNavigate()
  const { type, version } = useParams<{ type: string; version?: string }>()
  const { catalog, error: catalogError } = useSchemasOutlet()
  const entry = catalog.find((s) => s.type === type)
  const resolvedVersion = version ?? entry?.latestVersion
  const [schema, setSchema] = useState<BoMSchema | null>(null)
  const [allowedEdges, setAllowedEdges] = useState<TypeAllowedEdges>({ incoming: [], outgoing: [] })
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
    void getSchema(type, resolvedVersion)
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

  useEffect(() => {
    if (!type) {
      setAllowedEdges({ incoming: [], outgoing: [] })
      return
    }
    let cancelled = false
    void getAllowedEdges(type)
      .then((edges) => {
        if (!cancelled) setAllowedEdges(edges)
      })
      .catch(() => {
        if (!cancelled) setAllowedEdges({ incoming: [], outgoing: [] })
      })
    return () => {
      cancelled = true
    }
  }, [type])

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
  const isEdgeSchema = (schema?.usage ?? entry?.usage) === 'EDGE_PROPERTIES'
  const versionOptions = useMemo(
    () =>
      versions.map((v) => ({
        value: v,
        label: v,
      })),
    [versions],
  )

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%', overflow: 'auto' }}>
      <div>
        <Group justify="space-between" align="center" wrap="nowrap" gap="md">
          <Group gap="xs" align="center" wrap="nowrap" style={{ minWidth: 0, flex: 1 }}>
            {!isEdgeSchema && <TypeColorSwatch color={schema?.attributes?.color} />}
            <Title order={3} lineClamp={1}>
              {type}
            </Title>
            {resolvedVersion && (
              <Text size="sm" c="dimmed" style={{ flexShrink: 0 }}>
                {resolvedVersion}
              </Text>
            )}
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
        {((schema?.tags && schema.tags.length > 0) ||
          Object.entries(schema?.attributes ?? {}).some(([name]) => name !== 'color')) && (
          <Group gap={6} mt={8} wrap="wrap">
            {(schema?.tags ?? []).map((tag) => (
              <Badge key={tag} size="sm" variant="light" radius="xl" tt="none">
                {tag}
              </Badge>
            ))}
            {Object.entries(schema?.attributes ?? {})
              .filter(([name]) => name !== 'color')
              .map(([name, value]) => (
              <Badge
                key={name}
                size="sm"
                variant="light"
                radius="xl"
                tt="none"
                leftSection={
                  <Text span size="xs" fw={700}>
                    {name}
                  </Text>
                }
              >
                {value}
              </Badge>
            ))}
          </Group>
        )}
        {!isEdgeSchema && (
          <Group gap={6} mt={8} wrap="wrap">
            <Text size="xs" c="dimmed">
              Used in
            </Text>
            {entry && entry.usedIn.length === 0 && (
              <Text size="xs" c="dimmed">
                no collections
              </Text>
            )}
            {entry?.usedIn.map((c) => (
              <Anchor key={c.id} component={Link} to={`/collections/${c.id}`} size="sm">
                {c.name}
              </Anchor>
            ))}
          </Group>
        )}
      </div>

      {(error || catalogError) && <Alert color="red">{error || catalogError}</Alert>}

      <Title order={4}>Schema</Title>
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
        viewMode === 'form' ? (
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
        )
      ) : null}

      {!isEdgeSchema && (
        <AllowedEdgesSection
          incoming={allowedEdges.incoming}
          outgoing={allowedEdges.outgoing}
          currentType={type}
          knownTypes={new Set(catalog.map((s) => s.type))}
        />
      )}
    </Stack>
  )
}
