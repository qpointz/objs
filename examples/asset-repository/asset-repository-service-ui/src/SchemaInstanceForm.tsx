import {
  ActionIcon,
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
import type { BoMSchemaField, BoMSchemaNode } from './api'

const GENERIC_FIELD_TITLES = new Set([
  'Text',
  'Number',
  'Integer',
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
          if (Object.keys(nested).length > 0) {
            obj[field.name] = nested
          }
        }
      }
      return obj
    }
    case 'ARRAY':
      return []
    case 'STRING':
    case 'ENUM':
      return ''
    case 'NUMBER':
    case 'INTEGER':
      return undefined
    case 'BOOLEAN':
      return false
    default:
      return null
  }
}

function formatViewValue(schema: BoMSchemaNode, value: unknown): string {
  if (value == null || value === '') return '—'
  if (schema.type === 'BOOLEAN') return value ? 'Yes' : 'No'
  if (schema.type === 'ENUM') {
    const found = (schema.values ?? []).find((v) => v.value === String(value))
    return found?.description || String(value)
  }
  return String(value)
}

function FieldLabel({
  label,
  extra,
  description,
  readOnly,
}: {
  label: string
  extra?: string
  description?: string
  readOnly: boolean
}) {
  return (
    <Group gap={4} wrap="nowrap">
      <Text size="sm" fw={500} c={readOnly ? 'dimmed' : undefined}>
        {label}
        {extra ? ` (${extra})` : ''}
      </Text>
      {description && (
        <Tooltip label={description} withArrow multiline maw={360}>
          <ActionIcon
            variant="subtle"
            size="xs"
            color="gray"
            aria-label={description}
          >
            <IconInfoCircle size={14} stroke={1.6} />
          </ActionIcon>
        </Tooltip>
      )}
    </Group>
  )
}

function ViewField({
  label,
  extra,
  description,
  children,
}: {
  label: string
  extra?: string
  description?: string
  children: ReactNode
}) {
  return (
    <Stack gap={2}>
      <FieldLabel label={label} extra={extra} description={description} readOnly />
      {children}
    </Stack>
  )
}

type Props = {
  schema: BoMSchemaNode
  value: Record<string, unknown>
  onChange: (next: Record<string, unknown>) => void
  readOnly?: boolean
}

export function SchemaInstanceForm({ schema, value, onChange, readOnly = false }: Props) {
  if (schema.type !== 'OBJECT') {
    return (
      <Text size="sm" c="dimmed">
        Root schema must be OBJECT to build a form.
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
    const next = { ...value }
    if (nextValue === undefined || nextValue === '') {
      delete next[name]
    } else {
      next[name] = nextValue
    }
    onChange(next)
  }

  return (
    <Stack gap="sm">
      {fields.map((field) => (
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
  const extra = [field.required ? 'required' : null, field.identifier ? 'identifier' : null]
    .filter(Boolean)
    .join(', ')

  if (field.schema.type === 'OBJECT') {
    const nested = (value && typeof value === 'object' && !Array.isArray(value)
      ? value
      : {}) as Record<string, unknown>
    return (
      <Paper withBorder p="sm">
        <FieldLabel label={label} extra={extra || undefined} description={description} readOnly={readOnly} />
        <div style={{ marginTop: 8 }}>
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
      <Paper withBorder p="sm">
        <Group justify="space-between" mb="xs" wrap="nowrap">
          <FieldLabel label={label} extra={extra || undefined} description={description} readOnly={readOnly} />
          {!readOnly && (
            <Button
              size="compact-xs"
              variant="light"
              leftSection={<IconPlus size={12} />}
              onClick={() => onChange([...items, itemSchema ? defaultValueForSchema(itemSchema) : ''])}
            >
              Add item
            </Button>
          )}
        </Group>
        <Stack gap="xs">
          {items.length === 0 && readOnly && (
            <Text size="sm" c="dimmed">
              —
            </Text>
          )}
          {items.map((item, idx) => (
            <Group key={idx} align="flex-start" wrap="nowrap" gap="xs">
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

  if (readOnly) {
    return (
      <ViewField label={label} extra={extra || undefined} description={description}>
        <Text size="sm">{formatViewValue(field.schema, value)}</Text>
      </ViewField>
    )
  }

  return (
    <ScalarControl
      field={field}
      value={value}
      onChange={onChange}
      label={<FieldLabel label={label} extra={extra || undefined} description={description} readOnly={false} />}
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
  if (readOnly) {
    return <Text size="sm">{formatViewValue(schema, value)}</Text>
  }
  return (
    <ScalarControl
      field={{ name: 'item', schema, required: false }}
      value={value}
      onChange={onChange}
    />
  )
}

function ScalarControl({
  field,
  value,
  onChange,
  label,
}: {
  field: BoMSchemaField
  value: unknown
  onChange: (next: unknown) => void
  label?: ReactNode
}) {
  const schema = field.schema
  switch (schema.type) {
    case 'BOOLEAN':
      return (
        <Checkbox
          label={label}
          checked={Boolean(value)}
          onChange={(e) => onChange(e.currentTarget.checked)}
        />
      )
    case 'ENUM':
      return (
        <Select
          label={label}
          clearable
          value={value == null ? null : String(value)}
          data={(schema.values ?? []).map((v) => ({
            value: v.value,
            label: v.description || v.value,
          }))}
          onChange={(v) => onChange(v || undefined)}
        />
      )
    case 'NUMBER':
    case 'INTEGER':
      return (
        <NumberInput
          label={label}
          allowDecimal={schema.type !== 'INTEGER'}
          value={typeof value === 'number' ? value : ''}
          onChange={(v) => onChange(typeof v === 'number' ? v : undefined)}
        />
      )
    case 'STRING':
    default:
      if (schema.format === 'uri' || (value != null && String(value).length > 80)) {
        return (
          <Textarea
            label={label}
            autosize
            minRows={2}
            value={value == null ? '' : String(value)}
            onChange={(e) => onChange(e.currentTarget.value)}
          />
        )
      }
      return (
        <TextInput
          label={label}
          type={schema.format === 'email' ? 'email' : 'text'}
          value={value == null ? '' : String(value)}
          onChange={(e) => onChange(e.currentTarget.value)}
        />
      )
  }
}
