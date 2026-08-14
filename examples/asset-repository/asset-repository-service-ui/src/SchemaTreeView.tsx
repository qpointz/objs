import { Badge, Group, Table, Text, Tooltip } from '@mantine/core'
import { IconBraces, IconList } from '@tabler/icons-react'
import type { BoMSchemaField, BoMSchemaNode } from './api'

type SchemaKind = 'object' | 'array' | 'scalar'

type SchemaRow = {
  key: string
  depth: number
  name: string
  kind: SchemaKind
  type: string
  required: boolean | null
  identifier: boolean | null
  searchable: boolean | null
  description: string
}

function kindOf(node: BoMSchemaNode): SchemaKind {
  if (node.type === 'OBJECT') return 'object'
  if (node.type === 'ARRAY') return 'array'
  return 'scalar'
}

function typeLabel(node: BoMSchemaNode): string {
  return node.format ? `${node.type} (${node.format})` : node.type
}

function caption(node: BoMSchemaNode, fieldName?: string): string {
  const title = node.title?.trim() ?? ''
  const description = node.description?.trim() ?? ''
  const parts: string[] = []
  if (title && title !== fieldName && title.toLowerCase() !== node.type.toLowerCase()) {
    parts.push(title)
  }
  if (description && description !== title) {
    parts.push(description)
  }
  if (node.type === 'ENUM' && node.values?.length) {
    parts.push(`values: ${node.values.map((v) => v.value).join(', ')}`)
  }
  return parts.join(' — ')
}

function flattenNode(node: BoMSchemaNode, depth: number, path: string): SchemaRow[] {
  const rows: SchemaRow[] = []
  if (node.type === 'OBJECT') {
    for (const field of node.fields ?? []) {
      rows.push(...flattenField(field, depth, path))
    }
    return rows
  }
  if (node.type === 'ARRAY' && node.items) {
    const key = `${path}/items`
    rows.push({
      key,
      depth,
      name: 'items',
      kind: kindOf(node.items),
      type: typeLabel(node.items),
      required: null,
      identifier: null,
      searchable: null,
      description: caption(node.items),
    })
    if (node.items.type === 'OBJECT' || node.items.type === 'ARRAY') {
      rows.push(...flattenNode(node.items, depth + 1, key))
    }
  }
  return rows
}

function flattenField(field: BoMSchemaField, depth: number, path: string): SchemaRow[] {
  const key = `${path}/${field.name}`
  const rows: SchemaRow[] = [
    {
      key,
      depth,
      name: field.name,
      kind: kindOf(field.schema),
      type: typeLabel(field.schema),
      required: field.required !== false,
      identifier: Boolean(field.identifier),
      searchable: Boolean(field.searchable),
      description: caption(field.schema, field.name),
    },
  ]
  if (field.schema.type === 'OBJECT' || field.schema.type === 'ARRAY') {
    rows.push(...flattenNode(field.schema, depth + 1, key))
  }
  return rows
}

function KindMark({ kind }: { kind: SchemaKind }) {
  if (kind === 'object') {
    return (
      <Tooltip label="Object" withArrow>
        <IconBraces size={16} stroke={1.6} color="var(--mantine-color-dimmed)" aria-label="Object" />
      </Tooltip>
    )
  }
  if (kind === 'array') {
    return (
      <Tooltip label="Array" withArrow>
        <IconList size={16} stroke={1.6} color="var(--mantine-color-dimmed)" aria-label="Array" />
      </Tooltip>
    )
  }
  return <span style={{ width: 16, flexShrink: 0 }} />
}

function FieldFlags({
  required,
  identifier,
  searchable,
}: {
  required: boolean | null
  identifier: boolean | null
  searchable: boolean | null
}) {
  if (required == null && !identifier && !searchable) {
    return null
  }
  return (
    <Group gap={4} wrap="nowrap">
      {required && (
        <Tooltip label="Required" withArrow>
          <Badge size="xs" variant="light" color="red" px={6}>
            *
          </Badge>
        </Tooltip>
      )}
      {identifier && (
        <Tooltip label="Identifier" withArrow>
          <Badge size="xs" variant="light" color="violet" px={6}>
            id
          </Badge>
        </Tooltip>
      )}
      {searchable && (
        <Tooltip label="Searchable" withArrow>
          <Badge size="xs" variant="light" color="teal" px={6}>
            s
          </Badge>
        </Tooltip>
      )}
    </Group>
  )
}

export function SchemaTreeView({ schema }: { schema: BoMSchemaNode }) {
  const rows = flattenNode(schema, 0, schema.title || 'root')

  if (rows.length === 0) {
    return (
      <Text size="sm" c="dimmed">
        This schema has no fields.
      </Text>
    )
  }

  return (
    <Table
      striped
      highlightOnHover
      withTableBorder
      verticalSpacing="xs"
      horizontalSpacing="md"
      layout="fixed"
      style={{ width: '100%', tableLayout: 'fixed' }}
    >
      <Table.Thead>
        <Table.Tr>
          <Table.Th w={260}>Field</Table.Th>
          <Table.Th w={130}>Type</Table.Th>
          <Table.Th w={140} style={{ whiteSpace: 'nowrap' }} />
          <Table.Th style={{ width: 'auto' }}>Description</Table.Th>
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {rows.map((row) => (
          <Table.Tr key={row.key}>
            <Table.Td>
              <Group gap={6} wrap="nowrap" style={{ paddingLeft: row.depth * 24 }}>
                <KindMark kind={row.kind} />
                <Text size="sm" fw={row.depth === 0 ? 650 : 500} ff="monospace" truncate>
                  {row.name}
                </Text>
              </Group>
            </Table.Td>
            <Table.Td>
              <Badge size="xs" variant="outline" tt="none">
                {row.type}
              </Badge>
            </Table.Td>
            <Table.Td style={{ whiteSpace: 'nowrap', overflow: 'visible' }}>
              <FieldFlags
                required={row.required}
                identifier={row.identifier}
                searchable={row.searchable}
              />
            </Table.Td>
            <Table.Td>
              <Text size="sm" c={row.description ? undefined : 'dimmed'}>
                {row.description || '—'}
              </Text>
            </Table.Td>
          </Table.Tr>
        ))}
      </Table.Tbody>
    </Table>
  )
}
