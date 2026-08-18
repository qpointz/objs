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
  TagsInput,
  Text,
  TextInput,
  Textarea,
  Title,
  Tooltip,
} from '@mantine/core'
import {
  KeyValueRowsEditor,
  rowsToStringMap,
  stringMapToRows,
} from './KeyValueRowsEditor'
import type { BoMSchemaField, BoMSchemaNode, BoMSchemaType } from './types'
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
                <Group gap={4} wrap="nowrap" style={{ flex: 1, minWidth: 0 }}>
                  <Text size="sm" fw={600} truncate style={{ flexShrink: 1, minWidth: 0 }}>
                    {field.name}
                  </Text>
                  {field.required !== false && (
                    <Tooltip label="Required" withArrow>
                      <Badge size="xs" variant="light" color="red" px={4} style={{ flexShrink: 0 }}>
                        *
                      </Badge>
                    </Tooltip>
                  )}
                  {field.identifier === true && (
                    <Tooltip label="Identifier" withArrow>
                      <Badge size="xs" variant="light" color="blue" px={4} style={{ flexShrink: 0 }}>
                        id
                      </Badge>
                    </Tooltip>
                  )}
                  {field.searchable === true && (
                    <Tooltip label="Searchable" withArrow>
                      <Badge size="xs" variant="light" color="teal" px={4} style={{ flexShrink: 0 }}>
                        s
                      </Badge>
                    </Tooltip>
                  )}
                </Group>
                <Badge size="xs" variant="outline" style={{ flexShrink: 0 }}>
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
  hideDescription = false,
}: {
  node: BoMSchemaNode
  onChange: (next: BoMSchemaNode) => void
  hideTitle?: boolean
  hideType?: boolean
  hideDescription?: boolean
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
      {!hideDescription && (
        <Textarea
          label="Description"
          autosize
          minRows={2}
          value={node.description}
          onChange={(e) => onChange({ ...node, description: e.currentTarget.value })}
        />
      )}
      {node.type === 'STRING' && (
        <TextInput
          label="Format"
          description="Free text (uri, date-time, purl, …)"
          value={node.format ?? ''}
          onChange={(e) =>
            onChange({ ...node, format: e.currentTarget.value.trim() ? e.currentTarget.value : undefined })
          }
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
                  values: [
                    ...(node.values ?? []),
                    { value: 'VALUE', caption: '', description: 'Description' },
                  ],
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
                label="Caption"
                description="UI label; empty uses value"
                value={entry.caption ?? ''}
                onChange={(e) => {
                  const values = [...(node.values ?? [])]
                  const caption = e.currentTarget.value
                  values[index] = { ...entry, caption: caption.length > 0 ? caption : undefined }
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
        <Group gap="md" wrap="wrap">
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
          {field.schema.type !== 'ARRAY' && field.schema.type !== 'OBJECT' ? (
            <>
              <Checkbox
                label="Identifier"
                checked={field.identifier === true}
                onChange={(e) =>
                  onChange(
                    updateFieldAt(root, selection.path, selection.fieldIndex, (f) => ({
                      ...f,
                      identifier: e.currentTarget.checked,
                    })),
                  )
                }
              />
              <Checkbox
                label="Searchable"
                checked={field.searchable === true}
                onChange={(e) =>
                  onChange(
                    updateFieldAt(root, selection.path, selection.fieldIndex, (f) => ({
                      ...f,
                      searchable: e.currentTarget.checked,
                    })),
                  )
                }
              />
            </>
          ) : null}
        </Group>
        <Textarea
          label="Description"
          autosize
          minRows={2}
          value={field.schema.description}
          onChange={(e) =>
            onChange(
              updateFieldAt(root, selection.path, selection.fieldIndex, (f) => ({
                ...f,
                schema: { ...f.schema, description: e.currentTarget.value },
              })),
            )
          }
        />
        <TagsInput
          label="Tags"
          placeholder="Add tag"
          value={field.tags ?? []}
          onChange={(tags) =>
            onChange(
              updateFieldAt(root, selection.path, selection.fieldIndex, (f) => ({
                ...f,
                tags,
              })),
            )
          }
        />
        <Text size="sm" fw={500}>
          Attributes
        </Text>
        <KeyValueRowsEditor
          rows={stringMapToRows(field.attributes)}
          onChange={(rows) =>
            onChange(
              updateFieldAt(root, selection.path, selection.fieldIndex, (f) => ({
                ...f,
                attributes: rowsToStringMap(rows),
              })),
            )
          }
        />
        <NodeFields
          node={field.schema}
          hideTitle
          hideType
          hideDescription
          onChange={(schema) =>
            onChange(
              updateFieldAt(root, selection.path, selection.fieldIndex, (f) => {
                const next: BoMSchemaField = { ...f, schema }
                if (schema.type === 'ARRAY' || schema.type === 'OBJECT') {
                  next.identifier = false
                  next.searchable = false
                }
                return next
              }),
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
        hideDescription={selection.path.length === 0}
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
    <Group
      align="stretch"
      grow
      preventGrowOverflow={false}
      gap="md"
      style={{ flex: 1, minHeight: 0, height: '100%' }}
    >
      <Paper
        withBorder
        p="sm"
        style={{ flex: 1, minWidth: 0, minHeight: 0, display: 'flex', flexDirection: 'column' }}
      >
        <Text fw={600} mb="xs" style={{ flexShrink: 0 }}>
          Content schema
        </Text>
        <div style={{ flex: 1, minHeight: 0 }}>
          <ScrollArea type="auto" h="100%">
            <TreeRows
              node={value}
              path={[]}
              selection={selection}
              onSelect={setSelection}
              onChange={onChange}
            />
          </ScrollArea>
        </div>
      </Paper>
      <Paper
        withBorder
        p="sm"
        style={{ flex: 1, minWidth: 0, minHeight: 0, display: 'flex', flexDirection: 'column' }}
      >
        <div style={{ flex: 1, minHeight: 0 }}>
          <ScrollArea type="auto" h="100%">
            {selection ? (
              <Inspector root={value} selection={selection} onChange={onChange} />
            ) : (
              <Text c="dimmed" size="sm">
                Select a node or field to edit.
              </Text>
            )}
          </ScrollArea>
        </div>
      </Paper>
    </Group>
  )
}
