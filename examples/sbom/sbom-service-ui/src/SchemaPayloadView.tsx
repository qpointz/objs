import {
  ActionIcon,
  Badge,
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
import { IconInfoCircle } from '@tabler/icons-react'
import { enumCaption, type BoMSchemaField, type BoMSchemaNode } from './api/types'

function fieldLabel(field: BoMSchemaField): string {
  return field.schema.title?.trim() || field.name
}

function fieldDescription(field: BoMSchemaField): string | undefined {
  const description = field.schema.description?.trim()
  if (!description || description === fieldLabel(field)) return undefined
  return description
}

function formatScalar(value: unknown): string {
  if (value == null || value === '') return ''
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return value != null && typeof value === 'object' && !Array.isArray(value)
}

function enumLabel(field: BoMSchemaField, value: unknown): string {
  const raw = formatScalar(value)
  if (!raw) return ''
  const match = field.schema.values?.find((v) => v.value === raw)
  if (match) return enumCaption(match)
  return raw
}

function FieldHint({ description }: { description?: string }) {
  if (!description) return null
  return (
    <Tooltip label={description} multiline maw={260} withArrow openDelay={200}>
      <ActionIcon
        size={18}
        variant="subtle"
        color="gray"
        radius="xl"
        aria-label="Field description"
        style={{ flexShrink: 0 }}
      >
        <IconInfoCircle size={12} stroke={1.75} />
      </ActionIcon>
    </Tooltip>
  )
}

function Caption({ label, description }: { label: string; description?: string }) {
  return (
    <Group gap={6} wrap="nowrap">
      <Text size="xs" fw={700} c="dimmed">
        {label}
      </Text>
      <FieldHint description={description} />
    </Group>
  )
}

function FormRow({
  label,
  description,
  locked,
  children,
}: {
  label: string
  description?: string
  locked?: boolean
  children: string
}) {
  return (
    <div>
      <Group gap={6} wrap="nowrap">
        <Caption label={label} description={description} />
        {locked && (
          <Badge size="xs" variant="light" color="gray">
            Identity
          </Badge>
        )}
      </Group>
      <Text size="sm" mt={4} style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
        {children || '—'}
      </Text>
    </div>
  )
}

export function SchemaPayloadView({
  schema,
  value,
  editable = false,
  onChange,
}: {
  schema: BoMSchemaNode
  value: Record<string, unknown>
  editable?: boolean
  onChange?: (next: Record<string, unknown>) => void
}) {
  if (schema.type !== 'OBJECT') {
    return (
      <Text size="sm" c="dimmed">
        {formatScalar(value) || '—'}
      </Text>
    )
  }
  const fields = schema.fields ?? []
  if (fields.length === 0) {
    return (
      <Text size="sm" c="dimmed">
        No fields on this type.
      </Text>
    )
  }
  const scalars = fields.filter((f) => f.schema.type !== 'OBJECT' && f.schema.type !== 'ARRAY')
  const nested = fields.filter((f) => f.schema.type === 'OBJECT' || f.schema.type === 'ARRAY')

  function setField(name: string, nextValue: unknown) {
    onChange?.({ ...value, [name]: nextValue })
  }

  return (
    <Stack gap="sm">
      {scalars.length > 0 && (
        <Stack gap="md">
          {scalars.map((field) => (
            <SchemaScalarField
              key={field.name}
              field={field}
              value={value[field.name]}
              editable={editable && !field.identifier && !!onChange}
              locked={editable && field.identifier}
              onChange={(next) => setField(field.name, next)}
            />
          ))}
        </Stack>
      )}
      {nested.map((field) => (
        <SchemaFieldView
          key={field.name}
          field={field}
          value={value[field.name]}
          editable={editable && !field.identifier && !!onChange}
          onChange={(next) => setField(field.name, next)}
        />
      ))}
    </Stack>
  )
}

function SchemaScalarField({
  field,
  value,
  editable,
  locked,
  onChange,
}: {
  field: BoMSchemaField
  value: unknown
  editable: boolean
  locked?: boolean
  onChange: (next: unknown) => void
}) {
  const label = fieldLabel(field)
  const description = fieldDescription(field)
  if (!editable) {
    return (
      <FormRow label={label} description={description} locked={locked}>
        {field.schema.type === 'ENUM' ? enumLabel(field, value) : formatScalar(value)}
      </FormRow>
    )
  }
  const type = field.schema.type
  if (type === 'BOOLEAN') {
    return (
      <Checkbox
        label={label}
        description={description}
        checked={value === true}
        onChange={(e) => onChange(e.currentTarget.checked)}
      />
    )
  }
  if (type === 'NUMBER' || type === 'INTEGER') {
    return (
      <NumberInput
        label={label}
        description={description}
        value={typeof value === 'number' ? value : undefined}
        allowDecimal={type === 'NUMBER'}
        onChange={(v) => {
          if (v === '' || v === undefined || v === null) onChange(undefined)
          else onChange(typeof v === 'number' ? v : Number(v))
        }}
      />
    )
  }
  if (type === 'ENUM') {
    return (
      <Select
        label={label}
        description={description}
        data={(field.schema.values ?? []).map((v) => ({
          value: v.value,
          label: enumCaption(v),
        }))}
        value={typeof value === 'string' && value.length > 0 ? value : null}
        onChange={(v) => onChange(v ?? undefined)}
        searchable
        clearable
      />
    )
  }
  const text = typeof value === 'string' ? value : formatScalar(value)
  const long = field.name === 'description' || text.includes('\n')
  if (long) {
    return (
      <Textarea
        label={label}
        description={description}
        value={text}
        onChange={(e) => onChange(e.currentTarget.value)}
        autosize
        minRows={3}
        maxRows={10}
      />
    )
  }
  return (
    <TextInput
      label={label}
      description={description}
      value={text}
      onChange={(e) => onChange(e.currentTarget.value)}
    />
  )
}

function SchemaFieldView({
  field,
  value,
  editable,
  onChange,
}: {
  field: BoMSchemaField
  value: unknown
  editable: boolean
  onChange: (next: unknown) => void
}) {
  const label = fieldLabel(field)
  const description = fieldDescription(field)
  const type = field.schema.type

  if (type === 'OBJECT') {
    const obj = isPlainObject(value) ? value : {}
    return (
      <Paper withBorder radius="md" p="md">
        <Group gap={6} mb={4} wrap="nowrap">
          <Text size="sm" fw={600}>
            {label}
          </Text>
          <FieldHint description={description} />
        </Group>
        <SchemaPayloadView schema={field.schema} value={obj} editable={editable} onChange={onChange} />
      </Paper>
    )
  }

  const items = Array.isArray(value) ? value : []
  const itemSchema = field.schema.items
  return (
    <Paper withBorder radius="md" p="md">
      <Group gap={8} mb="xs">
        <Text size="sm" fw={600}>
          {label}
        </Text>
        <FieldHint description={description} />
        <Badge size="xs" variant="light">
          {items.length}
        </Badge>
      </Group>
      {items.length === 0 ? (
        <Text size="sm" c="dimmed">
          None
        </Text>
      ) : (
        <Stack gap="sm">
          {items.map((item, i) =>
            itemSchema?.type === 'OBJECT' ? (
              <SchemaPayloadView
                key={i}
                schema={itemSchema}
                value={isPlainObject(item) ? item : {}}
                editable={editable}
                onChange={
                  editable
                    ? (next) => {
                        const copy = [...items]
                        copy[i] = next
                        onChange(copy)
                      }
                    : undefined
                }
              />
            ) : (
              <FormRow key={i} label={`${label} ${i + 1}`}>
                {formatScalar(item)}
              </FormRow>
            ),
          )}
        </Stack>
      )}
    </Paper>
  )
}
