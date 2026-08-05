import {
  ActionIcon,
  Button,
  Checkbox,
  Group,
  NumberInput,
  Select,
  Stack,
  Text,
  TextInput,
  Textarea,
} from '@mantine/core'
import { IconPlus, IconTrash } from '@tabler/icons-react'
import type { BoMSchemaField, BoMSchemaNode } from './types'

/** Placeholder titles from the schema visual builder defaults — not useful as instance-form labels. */
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

function fieldDescription(field: BoMSchemaField): string | undefined {
  const description = field.schema.description?.trim()
  if (!description) return undefined
  if (
    description === 'Text value' ||
    description === 'Numeric value' ||
    description === 'Whole-number value' ||
    description === 'Boolean value' ||
    description === 'Enumerated value' ||
    description === 'Object value' ||
    description === 'Array value' ||
    description === 'Array item' ||
    description === 'Schema draft'
  ) {
    return undefined
  }
  return description
}

type Props = {
  schema: BoMSchemaNode
  value: Record<string, unknown>
  onChange: (next: Record<string, unknown>) => void
  pathPrefix?: string
  /** Denser controls for side panels (Object linter). */
  compact?: boolean
}

export function defaultValueForSchema(schema: BoMSchemaNode): unknown {
  if (schema.default !== undefined) return structuredClone(schema.default)
  switch (schema.type) {
    case 'OBJECT': {
      const obj: Record<string, unknown> = {}
      for (const field of schema.fields ?? []) {
        if (field.required || field.schema.default !== undefined) {
          obj[field.name] = defaultValueForSchema(field.schema)
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
      return 0
    case 'BOOLEAN':
      return false
    default:
      return null
  }
}

export function SchemaInstanceForm({
  schema,
  value,
  onChange,
  pathPrefix = '',
  compact = false,
}: Props) {
  if (schema.type !== 'OBJECT') {
    return (
      <Text size={compact ? 'xs' : 'sm'} c="dimmed">
        Root schema must be OBJECT to build a form.
      </Text>
    )
  }

  const fields = schema.fields ?? []

  function setField(name: string, nextValue: unknown) {
    onChange({ ...value, [name]: nextValue })
  }

  return (
    <Stack gap={compact ? 6 : 'sm'}>
      {fields.map((field) => (
        <FieldEditor
          key={`${pathPrefix}${field.name}`}
          field={field}
          value={value[field.name]}
          onChange={(next) => setField(field.name, next)}
          path={`${pathPrefix}${field.name}`}
          compact={compact}
        />
      ))}
      {fields.length === 0 && (
        <Text size={compact ? 'xs' : 'sm'} c="dimmed">
          Schema has no fields.
        </Text>
      )}
    </Stack>
  )
}

function FieldEditor({
  field,
  value,
  onChange,
  path,
  compact,
}: {
  field: BoMSchemaField
  value: unknown
  onChange: (next: unknown) => void
  path: string
  compact: boolean
}) {
  const label = fieldLabel(field)
  const description = compact ? undefined : fieldDescription(field)
  const required = field.required === true
  const multiline = field.stereotype?.includes('multiline') === true
  const size = compact ? 'xs' : undefined

  switch (field.schema.type) {
    case 'STRING':
      return multiline ? (
        <Textarea
          label={label}
          description={description}
          required={required}
          size={size}
          value={typeof value === 'string' ? value : ''}
          onChange={(e) => onChange(e.currentTarget.value)}
          minRows={compact ? 2 : 3}
        />
      ) : (
        <TextInput
          label={label}
          description={description}
          required={required}
          size={size}
          value={typeof value === 'string' ? value : ''}
          onChange={(e) => onChange(e.currentTarget.value)}
          placeholder={field.schema.format ?? undefined}
        />
      )
    case 'NUMBER':
    case 'INTEGER':
      return (
        <NumberInput
          label={label}
          description={description}
          required={required}
          size={size}
          value={typeof value === 'number' ? value : undefined}
          onChange={(v) => onChange(typeof v === 'number' ? v : Number(v) || 0)}
          allowDecimal={field.schema.type === 'NUMBER'}
        />
      )
    case 'BOOLEAN':
      return (
        <Checkbox
          label={label}
          description={description}
          size={size}
          checked={Boolean(value)}
          onChange={(e) => onChange(e.currentTarget.checked)}
        />
      )
    case 'ENUM':
      return (
        <Select
          label={label}
          description={description}
          required={required}
          size={size}
          data={(field.schema.values ?? []).map((v) => ({
            value: v.value,
            label: v.description ? `${v.value} — ${v.description}` : v.value,
          }))}
          value={typeof value === 'string' ? value : null}
          onChange={(v) => onChange(v ?? '')}
          searchable
          clearable={!required}
        />
      )
    case 'OBJECT': {
      const obj = (value && typeof value === 'object' && !Array.isArray(value)
        ? value
        : {}) as Record<string, unknown>
      return (
        <Stack gap={compact ? 4 : 4}>
          <Text size={compact ? 'xs' : 'sm'} fw={600}>
            {label}
            {required ? ' *' : ''}
          </Text>
          {description && (
            <Text size="xs" c="dimmed">
              {description}
            </Text>
          )}
          <SchemaInstanceForm
            schema={field.schema}
            value={obj}
            onChange={onChange}
            pathPrefix={`${path}.`}
            compact={compact}
          />
        </Stack>
      )
    }
    case 'ARRAY': {
      const items = Array.isArray(value) ? value : []
      const itemSchema = field.schema.items
      return (
        <Stack gap={compact ? 6 : 'xs'}>
          <Group justify="space-between" gap="xs">
            <div>
              <Text size={compact ? 'xs' : 'sm'} fw={600}>
                {label}
                {required ? ' *' : ''}
              </Text>
              {description && (
                <Text size="xs" c="dimmed">
                  {description}
                </Text>
              )}
            </div>
            <Button
              size="compact-xs"
              variant="light"
              leftSection={<IconPlus size={12} />}
              onClick={() =>
                onChange([...items, itemSchema ? defaultValueForSchema(itemSchema) : null])
              }
            >
              Add
            </Button>
          </Group>
          {items.map((item, index) => (
            <Group key={`${path}[${index}]`} align="flex-start" wrap="nowrap" gap={6}>
              <div style={{ flex: 1 }}>
                {itemSchema?.type === 'OBJECT' ? (
                  <SchemaInstanceForm
                    schema={itemSchema}
                    value={
                      (item && typeof item === 'object' && !Array.isArray(item)
                        ? item
                        : {}) as Record<string, unknown>
                    }
                    onChange={(next) => {
                      const copy = [...items]
                      copy[index] = next
                      onChange(copy)
                    }}
                    pathPrefix={`${path}[${index}].`}
                    compact={compact}
                  />
                ) : (
                  <FieldEditor
                    field={{
                      name: `[${index}]`,
                      schema: itemSchema ?? { type: 'STRING', title: 'Item', description: '' },
                      required: true,
                    }}
                    value={item}
                    onChange={(next) => {
                      const copy = [...items]
                      copy[index] = next
                      onChange(copy)
                    }}
                    path={`${path}[${index}]`}
                    compact={compact}
                  />
                )}
              </div>
              <ActionIcon
                variant="subtle"
                color="red"
                size={compact ? 'sm' : 'md'}
                aria-label="Remove item"
                onClick={() => onChange(items.filter((_, i) => i !== index))}
                mt={compact ? 2 : 4}
              >
                <IconTrash size={compact ? 14 : 16} />
              </ActionIcon>
            </Group>
          ))}
        </Stack>
      )
    }
    default:
      return (
        <Text size={compact ? 'xs' : 'sm'} c="dimmed">
          Unsupported field type for {field.name}
        </Text>
      )
  }
}

export function AnnotationsEditor({
  value,
  onChange,
  compact = false,
}: {
  value: Record<string, string>
  onChange: (next: Record<string, string>) => void
  compact?: boolean
}) {
  const rows = Object.entries(value)
  const displayRows = rows.length > 0 ? rows : [['', '']]
  const size = compact ? 'xs' : undefined

  function updateRow(index: number, key: string, val: string) {
    const nextEntries = displayRows.map(([k, v], i) => (i === index ? [key, val] : [k, v]))
    const next: Record<string, string> = {}
    for (const [k, v] of nextEntries) {
      if (!k.trim()) continue
      next[k.trim()] = v
    }
    onChange(next)
  }

  function removeRow(index: number) {
    const nextEntries = displayRows.filter((_, i) => i !== index)
    const next: Record<string, string> = {}
    for (const [k, v] of nextEntries) {
      if (!k.trim()) continue
      next[k.trim()] = v
    }
    onChange(next)
  }

  return (
    <Stack gap={compact ? 6 : 'xs'}>
      {displayRows.map(([key, val], index) => (
        <Group key={index} align={compact ? 'center' : 'flex-end'} wrap="nowrap" gap={6}>
          <TextInput
            label={compact ? undefined : index === 0 ? 'Key' : undefined}
            placeholder={compact ? 'key' : undefined}
            size={size}
            value={key}
            onChange={(e) => updateRow(index, e.currentTarget.value, val)}
            style={{ flex: 1 }}
          />
          <TextInput
            label={compact ? undefined : index === 0 ? 'Value' : undefined}
            placeholder={compact ? 'value' : undefined}
            size={size}
            value={val}
            onChange={(e) => updateRow(index, key, e.currentTarget.value)}
            style={{ flex: 1 }}
          />
          <ActionIcon
            variant="subtle"
            color="red"
            size={compact ? 'sm' : 'md'}
            aria-label="Remove annotation"
            onClick={() => removeRow(index)}
            disabled={displayRows.length <= 1 && !key && !val}
            mb={compact ? 0 : 4}
          >
            <IconTrash size={compact ? 14 : 16} />
          </ActionIcon>
        </Group>
      ))}
      <Button
        size="compact-xs"
        variant="light"
        leftSection={<IconPlus size={12} />}
        onClick={() => onChange({ ...value, '': '' })}
      >
        Add annotation
      </Button>
    </Stack>
  )
}
