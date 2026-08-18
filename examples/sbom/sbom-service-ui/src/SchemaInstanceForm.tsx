import {
  ActionIcon,
  Badge,
  Button,
  Checkbox,
  Group,
  NumberInput,
  Paper,
  Select,
  Stack,
  Text,
  TextInput,
  Textarea,
  Tooltip,
} from '@mantine/core'
import { IconInfoCircle, IconPlus, IconTrash } from '@tabler/icons-react'
import type { ReactNode } from 'react'
import { enumCaption, type BoMSchemaField, type BoMSchemaNode } from './api/types'

const GENERIC_FIELD_TITLES = new Set([
  'Text',
  'URI',
  'Date and time',
  'Number',
  'Integer',
  'Boolean',
  'Flag',
  'Choice',
  'Object',
  'List',
  'Item',
  'New schema',
])

export function fieldLabel(field: BoMSchemaField): string {
  const title = field.schema.title?.trim()
  if (title && !GENERIC_FIELD_TITLES.has(title) && title !== field.schema.type) {
    return title
  }
  return field.name
}

export function fieldDescription(field: BoMSchemaField): string | undefined {
  const description = field.schema.description?.trim()
  return description || undefined
}

export function defaultValueForSchema(schema: BoMSchemaNode): unknown {
  if (schema.default !== undefined) return structuredClone(schema.default)
  switch (schema.type) {
    case 'OBJECT': {
      const obj: Record<string, unknown> = {}
      for (const field of schema.fields ?? []) {
        if (field.schema.default !== undefined) {
          obj[field.name] = defaultValueForSchema(field.schema)
          continue
        }
        if (field.schema.type === 'OBJECT') {
          const nested = defaultValueForSchema(field.schema) as Record<string, unknown>
          if (Object.keys(nested).length > 0) obj[field.name] = nested
        }
      }
      return obj
    }
    case 'ARRAY':
      return []
    case 'STRING':
    case 'ENUM':
      return ''
    case 'BOOLEAN':
      return false
    default:
      return undefined
  }
}

export function identifierPaths(schema: BoMSchemaNode, prefix = ''): string[] {
  if (schema.type !== 'OBJECT') return []
  const out: string[] = []
  for (const field of schema.fields ?? []) {
    const path = prefix ? `${prefix}.${field.name}` : field.name
    if (field.schema.type === 'OBJECT') out.push(...identifierPaths(field.schema, path))
    else if (field.schema.type !== 'ARRAY' && field.identifier) out.push(path)
  }
  return out
}

export function getByPath(payload: Record<string, unknown>, path: string): unknown {
  let cur: unknown = payload
  for (const part of path.split('.')) {
    if (cur == null || typeof cur !== 'object' || Array.isArray(cur)) return undefined
    cur = (cur as Record<string, unknown>)[part]
  }
  return cur
}

export function isUnset(value: unknown): boolean {
  return value == null || (typeof value === 'string' && value.trim() === '')
}

export function filledIdentifiers(
  schema: BoMSchemaNode,
  payload: Record<string, unknown>,
): { path: string; value: unknown }[] {
  return identifierPaths(schema)
    .map((path) => ({ path, value: getByPath(payload, path) }))
    .filter((row) => !isUnset(row.value))
}

export function missingCreateFields(schema: BoMSchemaNode, payload: Record<string, unknown>): string[] {
  const ids = identifierPaths(schema)
  if (ids.length > 0) {
    return ids.filter((path) => isUnset(getByPath(payload, path)))
  }
  return missingRequired(schema, payload)
}

function missingRequired(schema: BoMSchemaNode, payload: Record<string, unknown>, prefix = ''): string[] {
  if (schema.type !== 'OBJECT') return []
  const missing: string[] = []
  for (const field of schema.fields ?? []) {
    const path = prefix ? `${prefix}.${field.name}` : field.name
    const value = payload[field.name]
    if (field.schema.type === 'OBJECT') {
      const nested =
        value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, unknown>) : {}
      missing.push(...missingRequired(field.schema, nested, path))
      continue
    }
    if (field.schema.type === 'ARRAY') {
      if (field.required && (!Array.isArray(value) || value.length === 0)) missing.push(path)
      const itemSchema = field.schema.items
      if (itemSchema?.type === 'OBJECT' && Array.isArray(value)) {
        value.forEach((item, i) => {
          const obj = item && typeof item === 'object' && !Array.isArray(item) ? (item as Record<string, unknown>) : {}
          missing.push(...missingRequired(itemSchema, obj, `${path}[${i}]`))
        })
      }
      continue
    }
    if (field.required && isUnset(value)) missing.push(path)
  }
  return missing
}

function FieldLabel({
  label,
  required,
  identifier,
  description,
}: {
  label: string
  required?: boolean
  identifier?: boolean
  description?: string
}) {
  return (
    <Group gap={4} wrap="nowrap">
      <Text size="xs" fw={600}>
        {label}
        {required ? ' *' : ''}
      </Text>
      {identifier && (
        <Badge size="xs" variant="light" px={4}>
          ID
        </Badge>
      )}
      {description && (
        <Tooltip label={description} withArrow multiline maw={360}>
          <ActionIcon variant="subtle" size="xs" color="gray" aria-label={description}>
            <IconInfoCircle size={12} stroke={1.6} />
          </ActionIcon>
        </Tooltip>
      )}
    </Group>
  )
}

export function SchemaInstanceForm({
  schema,
  value,
  onChange,
  readOnly = false,
}: {
  schema: BoMSchemaNode
  value: Record<string, unknown>
  onChange: (next: Record<string, unknown>) => void
  readOnly?: boolean
}) {
  if (schema.type !== 'OBJECT') {
    return (
      <Text size="sm" c="dimmed">
        Root schema must be an object.
      </Text>
    )
  }
  const fields = schema.fields ?? []
  if (fields.length === 0) {
    return (
      <Text size="sm" c="dimmed">
        Schema has no fields.
      </Text>
    )
  }

  function setField(name: string, nextValue: unknown) {
    if (readOnly) return
    const next = { ...value }
    if (nextValue === undefined || nextValue === '') delete next[name]
    else next[name] = nextValue
    onChange(next)
  }

  const scalars = fields.filter((f) => f.schema.type !== 'OBJECT' && f.schema.type !== 'ARRAY')
  const nested = fields.filter((f) => f.schema.type === 'OBJECT' || f.schema.type === 'ARRAY')

  return (
    <Stack gap="sm">
      {scalars.map((field) => (
        <FieldEditor
          key={field.name}
          field={field}
          value={value[field.name]}
          onChange={(next) => setField(field.name, next)}
          readOnly={readOnly}
        />
      ))}
      {nested.map((field) => (
        <FieldEditor
          key={field.name}
          field={field}
          value={value[field.name]}
          onChange={(next) => setField(field.name, next)}
          readOnly={readOnly}
        />
      ))}
    </Stack>
  )
}

function FieldEditor({
  field,
  value,
  onChange,
  readOnly,
}: {
  field: BoMSchemaField
  value: unknown
  onChange: (next: unknown) => void
  readOnly: boolean
}) {
  const label = fieldLabel(field)
  const description = fieldDescription(field)
  const required = Boolean(field.required || field.identifier)

  if (field.schema.type === 'OBJECT') {
    const nested =
      value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, unknown>) : {}
    return (
      <Paper withBorder p="xs" radius="sm">
        <FieldLabel label={label} required={required} identifier={field.identifier} description={description} />
        <div style={{ marginTop: 6 }}>
          <SchemaInstanceForm
            schema={field.schema}
            value={nested}
            onChange={(next) => onChange(Object.keys(next).length ? next : undefined)}
            readOnly={readOnly}
          />
        </div>
      </Paper>
    )
  }

  if (field.schema.type === 'ARRAY') {
    const items = Array.isArray(value) ? value : []
    const itemSchema = field.schema.items
    return (
      <Paper withBorder p="xs" radius="sm">
        <Group justify="space-between" mb={4} wrap="nowrap">
          <FieldLabel label={label} required={required} identifier={field.identifier} description={description} />
          {!readOnly && (
            <Button
              size="compact-xs"
              variant="light"
              leftSection={<IconPlus size={12} />}
              onClick={() => onChange([...items, itemSchema ? defaultValueForSchema(itemSchema) : ''])}
            >
              Add
            </Button>
          )}
        </Group>
        <Stack gap={4}>
          {items.map((item, idx) => (
            <Group key={idx} align="flex-start" wrap="nowrap" gap={4}>
              <div style={{ flex: 1 }}>
                <ScalarOrNested
                  schema={itemSchema ?? { type: 'STRING', title: 'Item', description: '' }}
                  value={item}
                  onChange={(next) => {
                    const copy = [...items]
                    copy[idx] = next
                    onChange(copy)
                  }}
                  readOnly={readOnly}
                />
              </div>
              {!readOnly && (
                <ActionIcon
                  size="sm"
                  variant="subtle"
                  color="red"
                  aria-label="Remove item"
                  onClick={() => onChange(items.filter((_, i) => i !== idx))}
                >
                  <IconTrash size={14} />
                </ActionIcon>
              )}
            </Group>
          ))}
        </Stack>
      </Paper>
    )
  }

  return (
    <ScalarControl
      field={field}
      value={value}
      onChange={onChange}
      required={required}
      readOnly={readOnly}
      label={
        <FieldLabel label={label} required={required} identifier={field.identifier} description={description} />
      }
    />
  )
}

function ScalarOrNested({
  schema,
  value,
  onChange,
  readOnly,
}: {
  schema: BoMSchemaNode
  value: unknown
  onChange: (next: unknown) => void
  readOnly: boolean
}) {
  if (schema.type === 'OBJECT') {
    return (
      <SchemaInstanceForm
        schema={schema}
        value={(value && typeof value === 'object' && !Array.isArray(value)
          ? value
          : {}) as Record<string, unknown>}
        onChange={onChange}
        readOnly={readOnly}
      />
    )
  }
  return (
    <ScalarControl
      field={{ name: 'item', schema, required: false }}
      value={value}
      onChange={onChange}
      readOnly={readOnly}
    />
  )
}

function ScalarControl({
  field,
  value,
  onChange,
  label,
  required,
  readOnly,
}: {
  field: BoMSchemaField
  value: unknown
  onChange: (next: unknown) => void
  label?: ReactNode
  required?: boolean
  readOnly?: boolean
}) {
  const schema = field.schema
  const size = 'xs' as const
  switch (schema.type) {
    case 'BOOLEAN':
      return (
        <Checkbox
          size={size}
          label={label}
          disabled={readOnly}
          checked={Boolean(value)}
          onChange={(e) => onChange(e.currentTarget.checked)}
        />
      )
    case 'ENUM':
      return (
        <Select
          size={size}
          label={label}
          required={required}
          readOnly={readOnly}
          clearable={!readOnly}
          value={value == null || value === '' ? null : String(value)}
          data={(schema.values ?? []).map((v) => ({
            value: v.value,
            label: enumCaption(v),
          }))}
          onChange={(v) => onChange(v || undefined)}
        />
      )
    case 'NUMBER':
    case 'INTEGER':
      return (
        <NumberInput
          size={size}
          label={label}
          required={required}
          readOnly={readOnly}
          allowDecimal={schema.type !== 'INTEGER'}
          value={typeof value === 'number' ? value : ''}
          onChange={(v) => onChange(typeof v === 'number' ? v : undefined)}
        />
      )
    default:
      if (field.name === 'description' || schema.format === 'uri' || (value != null && String(value).length > 80)) {
        return (
          <Textarea
            size={size}
            label={label}
            required={required}
            readOnly={readOnly}
            autosize
            minRows={1}
            maxRows={4}
            value={value == null ? '' : String(value)}
            onChange={(e) => onChange(e.currentTarget.value)}
          />
        )
      }
      return (
        <TextInput
          size={size}
          label={label}
          required={required}
          readOnly={readOnly}
          value={value == null ? '' : String(value)}
          onChange={(e) => onChange(e.currentTarget.value)}
        />
      )
  }
}
