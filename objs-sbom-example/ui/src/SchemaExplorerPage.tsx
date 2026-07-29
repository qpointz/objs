import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Code,
  Group,
  Loader,
  Paper,
  ScrollArea,
  SegmentedControl,
  Stack,
  Table,
  Tabs,
  Text,
  TextInput,
  Title,
  Anchor,
} from '@mantine/core'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { stringify as stringifyYaml } from 'yaml'
import {
  getJsonSchema,
  getSchema,
  getSchemaEdges,
  getTypeEdges,
  listSchemas,
  schemaDetailPath,
  schemaLinterPath,
} from './api'
import { SchemaRelationshipGraph } from './SchemaRelationshipGraph'
import type { BoMAllowedEdgeRule, BoMSchema, BoMSchemaUsage, TypeEdgesResponse } from './types'

function EdgeTable({
  title,
  rules,
}: {
  title: string
  rules: BoMAllowedEdgeRule[]
}) {
  return (
    <Stack gap="xs">
      <Text fw={600}>{title}</Text>
      {rules.length === 0 ? (
        <Text size="sm" c="dimmed">
          None
        </Text>
      ) : (
        <Table striped withTableBorder>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Source</Table.Th>
              <Table.Th>Role</Table.Th>
              <Table.Th>Target</Table.Th>
              <Table.Th>Properties</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {rules.map((rule) => (
              <Table.Tr key={`${rule.sourceType}|${rule.role}|${rule.targetType}`}>
                <Table.Td>
                  {rule.sourceType === '*' ? (
                    '*'
                  ) : (
                    <Anchor component={Link} to={`/schemas/${encodeURIComponent(rule.sourceType)}`}>
                      {rule.sourceType}
                    </Anchor>
                  )}
                </Table.Td>
                <Table.Td>{rule.role}</Table.Td>
                <Table.Td>
                  {rule.targetType === '*' ? (
                    '*'
                  ) : (
                    <Anchor component={Link} to={`/schemas/${encodeURIComponent(rule.targetType)}`}>
                      {rule.targetType}
                    </Anchor>
                  )}
                </Table.Td>
                <Table.Td>
                  <Badge size="sm" variant="light">
                    {rule.propertiesPolicy}
                  </Badge>
                  {rule.propertiesSchemaType && (
                    <Text size="xs" c="dimmed" mt={2}>
                      {rule.propertiesSchemaType}@{rule.propertiesSchemaVersion}
                    </Text>
                  )}
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  )
}

export function SchemaExplorerPage() {
  const params = useParams()
  const navigate = useNavigate()
  const selectedType = params.type ? decodeURIComponent(params.type) : undefined
  const selectedVersion = params.version ? decodeURIComponent(params.version) : undefined

  const [usage, setUsage] = useState<BoMSchemaUsage | 'ALL'>('ENTITY')
  const [search, setSearch] = useState('')
  const [schemas, setSchemas] = useState<BoMSchema[]>([])
  const [selected, setSelected] = useState<BoMSchema | null>(null)
  const [jsonSchema, setJsonSchema] = useState<Record<string, unknown> | null>(null)
  const [edges, setEdges] = useState<TypeEdgesResponse | null>(null)
  const [schemaEdges, setSchemaEdges] = useState<BoMAllowedEdgeRule[]>([])
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setLoading(true)
      setError(null)
      try {
        const list = await listSchemas(usage === 'ALL' ? undefined : usage)
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
  }, [usage])

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
    return [...byType.entries()].sort(([a], [b]) => a.localeCompare(b))
  }, [schemas, search])

  useEffect(() => {
    if (!selectedType) {
      setSelected(null)
      setJsonSchema(null)
      setEdges(null)
      setSchemaEdges([])
      return
    }
    let cancelled = false
    ;(async () => {
      setDetailLoading(true)
      setError(null)
      try {
        const versions = schemas.filter((s) => s.type === selectedType)
        const version =
          selectedVersion ??
          versions.map((s) => s.version).sort().at(-1) ??
          '1.0.0'
        if (!selectedVersion) {
          navigate(schemaDetailPath(selectedType, version), { replace: true })
          return
        }
        const schema = await getSchema(selectedType, version)
        const [projection, typeEdges, propertySchemaEdges] = await Promise.all([
          getJsonSchema(selectedType, version),
          schema.usages.includes('ENTITY')
            ? getTypeEdges(selectedType)
            : Promise.resolve({ incoming: [], outgoing: [] }),
          schema.usages.includes('EDGE_PROPERTIES')
            ? getSchemaEdges(selectedType, version)
            : Promise.resolve([]),
        ])
        if (!cancelled) {
          setSelected(schema)
          setJsonSchema(projection)
          setEdges(typeEdges)
          setSchemaEdges(propertySchemaEdges)
        }
      } catch (e) {
        if (!cancelled) {
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
  }, [selectedType, selectedVersion, schemas, navigate])

  return (
    <Group align="stretch" grow preventGrowOverflow={false} style={{ flex: 1, minHeight: 0 }} gap="md">
      <Paper withBorder p="md" style={{ flex: '0 0 320px', maxWidth: 360, overflow: 'hidden' }}>
        <Stack gap="sm" style={{ height: '100%' }}>
          <Group justify="space-between">
            <Title order={4}>Schemas</Title>
            <Button size="xs" component={Link} to="/linter">
              New draft
            </Button>
          </Group>
          <SegmentedControl
            value={usage}
            onChange={(v) => setUsage(v as BoMSchemaUsage | 'ALL')}
            data={[
              { label: 'Entity', value: 'ENTITY' },
              { label: 'Edge props', value: 'EDGE_PROPERTIES' },
              { label: 'All', value: 'ALL' },
            ]}
          />
          <TextInput
            placeholder="Search type or version"
            value={search}
            onChange={(e) => setSearch(e.currentTarget.value)}
          />
          {loading ? (
            <Loader size="sm" />
          ) : (
            <ScrollArea style={{ flex: 1 }}>
              <Stack gap="xs">
                {grouped.map(([type, versions]) => (
                  <Paper key={type} withBorder p="xs">
                    <Text fw={600} size="sm">
                      {type}
                    </Text>
                    <Group gap={4} mt={4}>
                      {versions.map((schema) => (
                        <Badge
                          key={schema.version}
                          component={Link}
                          to={schemaDetailPath(schema.type, schema.version)}
                          variant={
                            selectedType === schema.type && selectedVersion === schema.version
                              ? 'filled'
                              : 'light'
                          }
                          style={{ cursor: 'pointer', textDecoration: 'none' }}
                        >
                          {schema.version}
                        </Badge>
                      ))}
                    </Group>
                  </Paper>
                ))}
              </Stack>
            </ScrollArea>
          )}
        </Stack>
      </Paper>

      <Paper withBorder p="md" style={{ flex: 1, minWidth: 0, overflow: 'hidden' }}>
        <ScrollArea h="100%">
          {error && (
            <Alert color="red" title="Failed to load" mb="md">
              {error}
            </Alert>
          )}
          {detailLoading && <Loader size="sm" />}
          {!selectedType && !detailLoading && (
            <Text c="dimmed">Select a schema type and version to inspect its definition and edges.</Text>
          )}
          {selected && !detailLoading && (
            <Stack gap="md">
              <Group justify="space-between" align="flex-start">
                <div>
                  <Title order={3}>
                    {selected.type}{' '}
                    <Text span c="dimmed" size="lg">
                      v{selected.version}
                    </Text>
                  </Title>
                  <Group gap="xs" mt={4}>
                    {selected.usages.map((u) => (
                      <Badge key={u} variant="light">
                        {u}
                      </Badge>
                    ))}
                  </Group>
                </div>
                <Group>
                  <Button
                    variant="light"
                    component={Link}
                    to={schemaLinterPath(selected.type, selected.version, 'edit')}
                  >
                    Edit / lint
                  </Button>
                  <Button
                    component={Link}
                    to={schemaLinterPath(selected.type, selected.version, 'create-version')}
                  >
                    Create version
                  </Button>
                </Group>
              </Group>

              {edges && selected.usages.includes('ENTITY') && (
                <Stack gap="md">
                  <EdgeTable title="Outgoing edges" rules={edges.outgoing} />
                  <EdgeTable title="Incoming edges" rules={edges.incoming} />
                </Stack>
              )}

              {selected.usages.includes('EDGE_PROPERTIES') && (
                <EdgeTable
                  title="Allowed relations using this property schema"
                  rules={schemaEdges}
                />
              )}

              <Tabs defaultValue="visual">
                <Tabs.List>
                  <Tabs.Tab value="visual">Visual</Tabs.Tab>
                  <Tabs.Tab value="yaml">DSL YAML</Tabs.Tab>
                  <Tabs.Tab value="json">DSL JSON</Tabs.Tab>
                  <Tabs.Tab value="json-schema">JSON Schema</Tabs.Tab>
                </Tabs.List>
                <Tabs.Panel value="visual" pt="sm">
                  <Text size="sm" c="dimmed" mb="xs">
                    The selected schema is shown as a UML-like property block. Follow an incoming
                    or outgoing relationship by selecting its related entity.
                  </Text>
                  <SchemaRelationshipGraph
                    schema={selected}
                    relationships={
                      selected.usages.includes('EDGE_PROPERTIES') &&
                      !selected.usages.includes('ENTITY')
                        ? { incoming: schemaEdges, outgoing: schemaEdges }
                        : (edges ?? { incoming: [], outgoing: [] })
                    }
                  />
                </Tabs.Panel>
                <Tabs.Panel value="yaml" pt="sm">
                  <Code block>{stringifyYaml(selected.contentSchema)}</Code>
                </Tabs.Panel>
                <Tabs.Panel value="json" pt="sm">
                  <Code block>{JSON.stringify(selected.contentSchema, null, 2)}</Code>
                </Tabs.Panel>
                <Tabs.Panel value="json-schema" pt="sm">
                  <Code block>{JSON.stringify(jsonSchema, null, 2)}</Code>
                </Tabs.Panel>
              </Tabs>
            </Stack>
          )}
        </ScrollArea>
      </Paper>
    </Group>
  )
}
