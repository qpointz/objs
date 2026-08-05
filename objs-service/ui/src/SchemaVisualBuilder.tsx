import { useState } from 'react'
import {
  ActionIcon,
  Badge,
  Button,
  Checkbox,
  Group,
  Paper,
  ScrollArea,
  Select,
  Stack,
  Text,
  TextInput,
  Textarea,
  Title,
} from '@mantine/core'
import type { BoMSchemaNode, BoMSchemaType } from './types'
import {
  SchemaPath,
  addFieldAt,
  defaultNodeForType,
  moveFieldAt,
  removeFieldAt,
  resolveNode,
  updateFieldAt,
  updateNodeAt,
} from './schemaDsl'

const TYPE_OPTIONS: { value: BoMSchemaType; label: string }[] = [
  { value: 'OBJECT', label: 'OBJECT' },
  { value: 'ARRAY', label: 'ARRAY' },
  { value: 'STRING', label: 'STRING' },
  { value: 'NUMBER', label: 'NUMBER' },
  { value: 'INTEGER', label: 'INTEGER' },
  { value: 'BOOLEAN', label: 'BOOLEAN' },
  { value: 'ENUM', label: 'ENUM' },
]

const FORMAT_OPTIONS = [
  { value: '', label: '(none)' },
  { value: 'date', label: 'date' },
  { value: 'date-time', label: 'date-time' },
  { value: 'email', label: 'email' },
  { value: 'uri', label: 'uri' },
  { value: 'uuid', label: 'uuid' },
  { value: 'hostname', label: 'hostname' },
  { value: 'ipv4', label: 'ipv4' },
  { value: 'ipv6', label: 'ipv6' },
]

type Selection =
  | { kind: 'node'; path: SchemaPath }
  | { kind: 'field'; path: SchemaPath; fieldIndex: number }

function pathKey(path: SchemaPath): string {
  return path.join('/')
}

function TreeRows({
  node,
  path,
  selection,
  onSelect,
  onChange,
  showHeader = true,
}: {
  node: BoMSchemaNode
  path: SchemaPath
  selection: Selection | null
  onSelect: (selection: Selection) => void
  onChange: (next: BoMSchemaNode) => void
  /** When false, omit type/title header (used under a field row that already shows name + type). */
  showHeader?: boolean
}) {
  const selected = selection?.kind === 'node' && pathKey(selection.path) === pathKey(path)
  const fieldCount = node.fields?.length ?? 0

  function moveField(index: number, direction: -1 | 1) {
    const target = index + direction
    if (target < 0 || target >= fieldCount) return
    onChange(moveFieldAt(node, [], index, direction))
    onSelect({ kind: 'field', path, fieldIndex: target })
  }

  return (
    <Stack gap={2}>
      {showHeader && (
        <Group
          gap="xs"
          wrap="nowrap"
          p={4}
          style={{
            background: selected ? 'var(--mantine-color-blue-light)' : undefined,
            borderRadius: 4,
            cursor: 'pointer',
          }}
          onClick={() => onSelect({ kind: 'node', path })}
        >
          <Badge size="xs">{node.type}</Badge>
          <Text size="sm" style={{ flex: 1 }}>
            {node.title}
          </Text>
          {node.type === 'OBJECT' && (
            <ActionIcon
              size="sm"
              variant="subtle"
              onClick={(e) => {
                e.stopPropagation()
                onChange(addFieldAt(node, [], `field${(node.fields?.length ?? 0) + 1}`))
              }}
              title="Add field"
            >
              +
            </ActionIcon>
          )}
        </Group>
      )}

      {node.type === 'OBJECT' &&
        (node.fields ?? []).map((field, index) => {
          const fieldSelected =
            selection?.kind === 'field' &&
            pathKey(selection.path) === pathKey(path) &&
            selection.fieldIndex === index
          const nested = field.schema.type === 'OBJECT' || field.schema.type === 'ARRAY'
          return (
            <Stack key={`${pathKey(path)}:${field.name}:${index}`} gap={2} ml="md">
              <Group
                gap="xs"
                wrap="nowrap"
                p={4}
                style={{
                  background: fieldSelected ? 'var(--mantine-color-blue-light)' : undefined,
                  borderRadius: 4,
                  cursor: 'pointer',
                }}
                onClick={() => onSelect({ kind: 'field', path, fieldIndex: index })}
              >
                <Text size="sm" fw={600} style={{ flex: 1 }} truncate>
                  {field.name}
                  {field.required === false ? '' : ' *'}
                </Text>
                <Badge size="xs" variant="outline">
                  {field.schema.type}
                </Badge>
                <ActionIcon
                  size="sm"
                  variant="subtle"
                  onClick={(e) => {
                    e.stopPropagation()
                    moveField(index, -1)
                  }}
                >
                  ↑
                </ActionIcon>
                <ActionIcon
                  size="sm"
                  variant="subtle"
                  onClick={(e) => {
                    e.stopPropagation()
                    moveField(index, 1)
                  }}
                >
                  ↓
                </ActionIcon>
                <ActionIcon
                  size="sm"
                  variant="subtle"
                  color="red"
                  onClick={(e) => {
                    e.stopPropagation()
                    onChange(removeFieldAt(node, [], index))
                    if (fieldSelected) onSelect({ kind: 'node', path })
                  }}
                >
                  ×
                </ActionIcon>
              </Group>
              {nested && (
                <TreeRows
                  node={field.schema}
                  path={[...path, index]}
                  selection={selection}
                  onSelect={onSelect}
                  onChange={(child) => onChange(updateNodeAt(node, [index], () => child))}
                  showHeader={false}
                />
              )}
            </Stack>
          )
        })}

      {node.type === 'ARRAY' && node.items && (
        <Stack gap={2} ml="md">
          <Text size="xs" c="dimmed">
            items
          </Text>
          {node.items.type === 'OBJECT' || node.items.type === 'ARRAY' ? (
            <TreeRows
              node={node.items}
              path={[...path, 'items']}
              selection={selection}
              onSelect={onSelect}
              onChange={(child) => onChange({ ...node, items: child })}
              showHeader={false}
            />
          ) : (
            <Group
              gap="xs"
              wrap="nowrap"
              p={4}
              style={{
                background:
                  selection?.kind === 'node' && pathKey(selection.path) === pathKey([...path, 'items'])
                    ? 'var(--mantine-color-blue-light)'
                    : undefined,
                borderRadius: 4,
                cursor: 'pointer',
              }}
              onClick={() => onSelect({ kind: 'node', path: [...path, 'items'] })}
            >
              <Badge size="xs" variant="outline">
                {node.items.type}
              </Badge>
            </Group>
          )}
        </Stack>
      )}
    </Stack>
  )
}

function NodeFields({
  node,
  onChange,
  hideTitle = false,
  hideType = false,
}: {
  node: BoMSchemaNode
  onChange: (next: BoMSchemaNode) => void
  hideTitle?: boolean
  hideType?: boolean
}) {
  return (
    <>
      {!hideType && (
        <Select
          label="Type"
          data={TYPE_OPTIONS}
          value={node.type}
          onChange={(value) => {
            if (!value) return
            onChange(defaultNodeForType(value as BoMSchemaType))
          }}
        />
      )}
      {!hideTitle && (
        <TextInput
          label="Title"
          value={node.title}
          onChange={(e) => onChange({ ...node, title: e.currentTarget.value })}
        />
      )}
      <Textarea
        label="Description"
        autosize
        minRows={2}
        value={node.description}
        onChange={(e) => onChange({ ...node, description: e.currentTarget.value })}
      />
      {node.type === 'STRING' && (
        <Select
          label="Format"
          data={FORMAT_OPTIONS}
          value={node.format ?? ''}
          onChange={(value) => onChange({ ...node, format: value ? value : undefined })}
        />
      )}
      {node.type === 'ENUM' && (
        <Stack gap="xs">
          <Group justify="space-between">
            <Text fw={600} size="sm">
              Values
            </Text>
            <Button
              size="xs"
              variant="light"
              onClick={() =>
                onChange({
                  ...node,
                  values: [...(node.values ?? []), { value: 'VALUE', description: 'Description' }],
                })
              }
            >
              Add value
            </Button>
          </Group>
          {(node.values ?? []).map((entry, index) => (
            <Group key={index} grow align="flex-end">
              <TextInput
                label="Value"
                value={entry.value}
                onChange={(e) => {
                  const values = [...(node.values ?? [])]
                  values[index] = { ...entry, value: e.currentTarget.value }
                  onChange({ ...node, values })
                }}
              />
              <TextInput
                label="Description"
                value={entry.description}
                onChange={(e) => {
                  const values = [...(node.values ?? [])]
                  values[index] = { ...entry, description: e.currentTarget.value }
                  onChange({ ...node, values })
                }}
              />
              <ActionIcon
                color="red"
                variant="subtle"
                onClick={() => {
                  const values = [...(node.values ?? [])]
                  values.splice(index, 1)
                  onChange({ ...node, values })
                }}
              >
                ×
              </ActionIcon>
            </Group>
          ))}
        </Stack>
      )}
      {node.type === 'OBJECT' && (
        <Button size="xs" variant="light" onClick={() => onChange(addFieldAt(node, []))}>
          Add field
        </Button>
      )}
    </>
  )
}

function Inspector({
  root,
  selection,
  onChange,
}: {
  root: BoMSchemaNode
  selection: Selection
  onChange: (next: BoMSchemaNode) => void
}) {
  if (selection.kind === 'field') {
    const parent = resolveNode(root, selection.path)
    const field = parent.fields?.[selection.fieldIndex]
    if (!field) return <Text c="dimmed">Field missing</Text>
    return (
      <Stack gap="sm">
        <Title order={5}>Field</Title>
        <Group grow align="flex-end" preventGrowOverflow={false}>
          <TextInput
            label="Name"
            value={field.name}
            onChange={(e) =>
              onChange(
                updateFieldAt(root, selection.path, selection.fieldIndex, (f) => ({
                  ...f,
                  name: e.currentTarget.value,
                })),
              )
            }
          />
          <Select
            label="Type"
            data={TYPE_OPTIONS}
            value={field.schema.type}
            onChange={(value) => {
              if (!value) return
              onChange(
                updateFieldAt(root, selection.path, selection.fieldIndex, (f) => ({
                  ...f,
                  schema: defaultNodeForType(value as BoMSchemaType),
                })),
              )
            }}
          />
        </Group>
        <Checkbox
          label="Required"
          checked={field.required !== false}
          onChange={(e) =>
            onChange(
              updateFieldAt(root, selection.path, selection.fieldIndex, (f) => ({
                ...f,
                required: e.currentTarget.checked,
              })),
            )
          }
        />
        <NodeFields
          node={field.schema}
          hideTitle
          hideType
          onChange={(schema) =>
            onChange(
              updateFieldAt(root, selection.path, selection.fieldIndex, (f) => ({
                ...f,
                schema,
              })),
            )
          }
        />
      </Stack>
    )
  }

  const node = resolveNode(root, selection.path)
  return (
    <Stack gap="sm">
      <Title order={5}>Node</Title>
      <NodeFields
        node={node}
        onChange={(next) => onChange(updateNodeAt(root, selection.path, () => next))}
      />
    </Stack>
  )
}

export function SchemaVisualBuilder({
  value,
  onChange,
}: {
  value: BoMSchemaNode
  onChange: (next: BoMSchemaNode) => void
}) {
  const [selection, setSelection] = useState<Selection | null>({ kind: 'node', path: [] })

  return (
    <Group align="stretch" grow preventGrowOverflow={false} gap="md" style={{ minHeight: 420 }}>
      <Paper withBorder p="sm" style={{ flex: 1, minWidth: 0 }}>
        <Text fw={600} mb="xs">
          Content schema
        </Text>
        <ScrollArea h={420}>
          <TreeRows
            node={value}
            path={[]}
            selection={selection}
            onSelect={setSelection}
            onChange={onChange}
          />
        </ScrollArea>
      </Paper>
      <Paper withBorder p="sm" style={{ flex: 1, minWidth: 0 }}>
        <ScrollArea h={420}>
          {selection ? (
            <Inspector root={value} selection={selection} onChange={onChange} />
          ) : (
            <Text c="dimmed" size="sm">
              Select a node or field to edit.
            </Text>
          )}
        </ScrollArea>
      </Paper>
    </Group>
  )
}
