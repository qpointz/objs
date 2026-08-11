import {
  ActionIcon,
  Badge,
  Box,
  Button,
  Checkbox,
  Group,
  NumberInput,
  Paper,
  Select,
  Stack,
  Switch,
  Table,
  Text,
  TextInput,
  Textarea,
  Tooltip,
} from '@mantine/core'
import { IconInfoCircle, IconPlus, IconTrash } from '@tabler/icons-react'
import { useEffect, useState, type ReactNode } from 'react'
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

export function fieldDescription(field: BoMSchemaField): string | undefined {
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

function FieldInfoTip({ description }: { description: string }) {
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

type Props = {
  schema: BoMSchemaNode
  value: Record<string, unknown>
  onChange: (next: Record<string, unknown>) => void
  pathPrefix?: string
  /** Denser controls for side panels (Object linter). */
  compact?: boolean
  /** Entity payload only: omit keys on delete; show (deleted); restore on value. */
  allowFieldDelete?: boolean
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
      return 0
    case 'BOOLEAN':
      return false
    default:
      return null
  }
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return value != null && typeof value === 'object' && !Array.isArray(value)
}

/** Key→key payload migrate for Schema ▾ (recurse OBJECT; drop unmatched). */
export type MigratePayloadResult = {
  payload: Record<string, unknown>
  /** Number of source keys copied into the target (nested OBJECT copies counted recursively). */
  copied: number
  /** Number of source keys dropped (no matching target field; nested drops included). */
  dropped: number
}

export function migratePayloadByKey(
  source: Record<string, unknown>,
  targetSchema: BoMSchemaNode,
): MigratePayloadResult {
  if (targetSchema.type !== 'OBJECT') {
    return { payload: {}, copied: 0, dropped: Object.keys(source).length }
  }

  const payload: Record<string, unknown> = {}
  let copied = 0
  let dropped = 0
  const unmatched = new Set(Object.keys(source))

  for (const field of targetSchema.fields ?? []) {
    if (!(field.name in source)) continue
    unmatched.delete(field.name)
    const srcVal = source[field.name]
    if (field.schema.type === 'OBJECT' && isPlainObject(srcVal)) {
      const nested = migratePayloadByKey(srcVal, field.schema)
      payload[field.name] = nested.payload
      copied += 1 + nested.copied
      dropped += nested.dropped
    } else {
      payload[field.name] = structuredClone(srcVal)
      copied += 1
    }
  }

  dropped += unmatched.size
  return { payload, copied, dropped }
}

/** True when migrate should ask before applying (zero copies or any dropped keys). */
export function migrateNeedsConfirm(result: MigratePayloadResult): boolean {
  return result.copied === 0 || result.dropped > 0
}

const compactInputStyles = {
  input: {
    minHeight: 28,
    fontSize: 'var(--mantine-font-size-xs)',
  },
} as const

function SectionChrome({
  title,
  count,
  accent,
  action,
  children,
}: {
  title: string
  count?: number
  accent: string
  action?: ReactNode
  children: ReactNode
}) {
  return (
    <Paper withBorder radius="md" p={0} style={{ overflow: 'hidden' }}>
      <Group
        justify="space-between"
        gap={8}
        wrap="nowrap"
        px="xs"
        py={6}
        style={{
          borderBottom: '1px solid var(--mantine-color-default-border)',
          background: 'color-mix(in srgb, var(--mantine-color-default-hover) 55%, transparent)',
        }}
      >
        <Group gap={8} wrap="nowrap" style={{ minWidth: 0 }}>
          <Box
            style={{
              width: 3,
              height: 14,
              borderRadius: 2,
              background: accent,
              flexShrink: 0,
            }}
          />
          <Text size="xs" fw={700} tt="uppercase" style={{ letterSpacing: '0.05em' }}>
            {title}
          </Text>
          {typeof count === 'number' && (
            <Badge size="xs" variant="light" color="gray">
              {count}
            </Badge>
          )}
        </Group>
        {action}
      </Group>
      <Box p={0}>{children}</Box>
    </Paper>
  )
}

export function SchemaInstanceForm({
  schema,
  value,
  onChange,
  pathPrefix = '',
  compact = false,
  allowFieldDelete = false,
}: Props) {
  const [deletedKeys, setDeletedKeys] = useState<Set<string>>(() => new Set())

  useEffect(() => {
    setDeletedKeys((prev) => {
      let changed = false
      const next = new Set(prev)
      for (const key of prev) {
        if (key in value) {
          next.delete(key)
          changed = true
        }
      }
      return changed ? next : prev
    })
  }, [value])

  if (schema.type !== 'OBJECT') {
    return (
      <Text size={compact ? 'xs' : 'sm'} c="dimmed">
        Root schema must be OBJECT to build a form.
      </Text>
    )
  }

  const fields = schema.fields ?? []
  if (fields.length === 0) {
    return (
      <Text size={compact ? 'xs' : 'sm'} c="dimmed">
        Schema has no fields.
      </Text>
    )
  }

  function setField(name: string, nextValue: unknown) {
    if (allowFieldDelete) {
      setDeletedKeys((prev) => {
        if (!prev.has(name)) return prev
        const next = new Set(prev)
        next.delete(name)
        return next
      })
    }
    onChange({ ...value, [name]: nextValue })
  }

  function deleteField(name: string) {
    if (!allowFieldDelete) return
    setDeletedKeys((prev) => {
      const next = new Set(prev)
      next.add(name)
      return next
    })
    const { [name]: _removed, ...rest } = value
    onChange(rest)
  }

  if (!compact) {
    return (
      <Stack gap="sm">
        {fields.map((field) => (
          <FieldEditor
            key={`${pathPrefix}${field.name}`}
            field={field}
            value={value[field.name]}
            deleted={allowFieldDelete && deletedKeys.has(field.name)}
            onChange={(next) => setField(field.name, next)}
            onDelete={allowFieldDelete ? () => deleteField(field.name) : undefined}
            onRestore={allowFieldDelete ? (next) => setField(field.name, next) : undefined}
            path={`${pathPrefix}${field.name}`}
            compact={false}
            allowFieldDelete={allowFieldDelete}
          />
        ))}
      </Stack>
    )
  }

  const scalarFields = fields.filter(
    (f) => f.schema.type !== 'OBJECT' && f.schema.type !== 'ARRAY',
  )
  const complexFields = fields.filter(
    (f) => f.schema.type === 'OBJECT' || f.schema.type === 'ARRAY',
  )

  return (
    <Stack gap={8}>
      {scalarFields.length > 0 && (
        <Table
          striped
          highlightOnHover
          withTableBorder={false}
          withColumnBorders
          horizontalSpacing={8}
          verticalSpacing={4}
          style={{ fontSize: 'var(--mantine-font-size-xs)' }}
        >
          <Table.Thead>
            <Table.Tr>
              <Table.Th w="38%">Field</Table.Th>
              <Table.Th>Value</Table.Th>
              {allowFieldDelete && <Table.Th w={36} />}
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {scalarFields.map((field) => {
              const deleted = allowFieldDelete && deletedKeys.has(field.name)
              return (
                <Table.Tr key={`${pathPrefix}${field.name}`}>
                  <Table.Td
                    style={{
                      verticalAlign: 'middle',
                      background:
                        'color-mix(in srgb, var(--mantine-color-default-hover) 40%, transparent)',
                    }}
                  >
                    <FieldName
                      label={fieldLabel(field)}
                      required={field.required === true}
                      description={fieldDescription(field)}
                      deleted={deleted}
                    />
                  </Table.Td>
                  <Table.Td style={{ verticalAlign: 'middle' }}>
                    <FieldControl
                      field={field}
                      value={deleted ? undefined : value[field.name]}
                      onChange={(next) => setField(field.name, next)}
                      path={`${pathPrefix}${field.name}`}
                      compact
                    />
                  </Table.Td>
                  {allowFieldDelete && (
                    <Table.Td style={{ verticalAlign: 'middle', paddingRight: 4 }}>
                      <ActionIcon
                        variant="subtle"
                        color="red"
                        size="sm"
                        aria-label={`Delete field ${field.name}`}
                        disabled={deleted}
                        onClick={() => deleteField(field.name)}
                      >
                        <IconTrash size={14} />
                      </ActionIcon>
                    </Table.Td>
                  )}
                </Table.Tr>
              )
            })}
          </Table.Tbody>
        </Table>
      )}
      {complexFields.map((field) => (
        <FieldEditor
          key={`${pathPrefix}${field.name}`}
          field={field}
          value={value[field.name]}
          deleted={allowFieldDelete && deletedKeys.has(field.name)}
          onChange={(next) => setField(field.name, next)}
          onDelete={allowFieldDelete ? () => deleteField(field.name) : undefined}
          onRestore={allowFieldDelete ? (next) => setField(field.name, next) : undefined}
          path={`${pathPrefix}${field.name}`}
          compact
          allowFieldDelete={allowFieldDelete}
        />
      ))}
    </Stack>
  )
}

function FieldName({
  label,
  required,
  description,
  deleted = false,
}: {
  label: string
  required: boolean
  description?: string
  deleted?: boolean
}) {
  return (
    <Group gap={4} wrap="nowrap" style={{ minWidth: 0 }}>
      <Text size="xs" fw={600} lineClamp={2} style={{ wordBreak: 'break-word' }}>
        {label}
      </Text>
      {required && (
        <Text size="xs" c="red" fw={700} aria-hidden>
          *
        </Text>
      )}
      {deleted && (
        <Text size="xs" c="dimmed" fs="italic">
          (deleted)
        </Text>
      )}
      {description && <FieldInfoTip description={description} />}
    </Group>
  )
}

function FieldControl({
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
  const size = compact ? 'xs' : undefined
  const multiline = field.stereotype?.includes('multiline') === true
  const required = field.required === true

  switch (field.schema.type) {
    case 'STRING':
      return multiline ? (
        <Textarea
          size={size}
          value={typeof value === 'string' ? value : ''}
          onChange={(e) => onChange(e.currentTarget.value)}
          minRows={compact ? 2 : 3}
          autosize
          maxRows={6}
          styles={compactInputStyles}
        />
      ) : (
        <TextInput
          size={size}
          value={typeof value === 'string' ? value : ''}
          onChange={(e) => onChange(e.currentTarget.value)}
          placeholder={field.schema.format ?? undefined}
          styles={compactInputStyles}
        />
      )
    case 'NUMBER':
    case 'INTEGER':
      return (
        <NumberInput
          size={size}
          value={typeof value === 'number' ? value : undefined}
          onChange={(v) => {
            if (v === '' || v === undefined || v === null) {
              onChange(undefined)
              return
            }
            onChange(typeof v === 'number' ? v : Number(v))
          }}
          allowDecimal={field.schema.type === 'NUMBER'}
          hideControls={compact}
          styles={compactInputStyles}
        />
      )
    case 'BOOLEAN':
      return compact ? (
        <Switch
          size="xs"
          checked={value === true}
          onChange={(e) => onChange(e.currentTarget.checked)}
          label={value === true ? 'true' : value === false ? 'false' : 'unset'}
          labelPosition="left"
        />
      ) : (
        <Checkbox
          checked={Boolean(value)}
          onChange={(e) => onChange(e.currentTarget.checked)}
        />
      )
    case 'ENUM':
      return (
        <Select
          size={size}
          data={(field.schema.values ?? []).map((v) => ({
            value: v.value,
            label: v.description ? `${v.value} — ${v.description}` : v.value,
          }))}
          value={typeof value === 'string' && value.length > 0 ? value : null}
          onChange={(v) => onChange(v ?? undefined)}
          searchable
          clearable={!required}
          styles={compactInputStyles}
        />
      )
    default:
      return (
        <Text size="xs" c="dimmed">
          Unsupported ({path})
        </Text>
      )
  }
}

function FieldEditor({
  field,
  value,
  onChange,
  onDelete,
  onRestore,
  deleted = false,
  path,
  compact,
  allowFieldDelete = false,
}: {
  field: BoMSchemaField
  value: unknown
  onChange: (next: unknown) => void
  onDelete?: () => void
  onRestore?: (next: unknown) => void
  deleted?: boolean
  path: string
  compact: boolean
  allowFieldDelete?: boolean
}) {
  const label = fieldLabel(field)
  const description = fieldDescription(field)
  const required = field.required === true

  switch (field.schema.type) {
    case 'STRING':
    case 'NUMBER':
    case 'INTEGER':
    case 'BOOLEAN':
    case 'ENUM':
      if (compact) {
        return (
          <FieldControl
            field={field}
            value={deleted ? undefined : value}
            onChange={onChange}
            path={path}
            compact
          />
        )
      }
      return (
        <Group align="flex-end" wrap="nowrap" gap={6}>
          <div style={{ flex: 1, minWidth: 0 }}>
            {deleted && (
              <Text size="xs" c="dimmed" fs="italic" mb={4}>
                (deleted)
              </Text>
            )}
            <LabeledScalar
              field={field}
              value={deleted ? undefined : value}
              onChange={onChange}
              path={path}
            />
          </div>
          {onDelete && (
            <ActionIcon
              variant="subtle"
              color="red"
              size="md"
              aria-label={`Delete field ${field.name}`}
              disabled={deleted}
              onClick={onDelete}
              mb={4}
            >
              <IconTrash size={16} />
            </ActionIcon>
          )}
        </Group>
      )
    case 'OBJECT': {
      const obj = (
        !deleted && value && typeof value === 'object' && !Array.isArray(value) ? value : {}
      ) as Record<string, unknown>
      return (
        <Paper withBorder radius="sm" p={compact ? 'xs' : 'sm'} mx={compact ? 8 : 0} mb={compact ? 8 : 0}>
          <Group justify="space-between" gap={6} mb={compact ? 6 : 'xs'} wrap="nowrap">
            <Group gap={6} wrap="nowrap" style={{ minWidth: 0 }}>
              <Text size={compact ? 'xs' : 'sm'} fw={600}>
                {label}
              </Text>
              {required && (
                <Badge size="xs" variant="light" color="red">
                  required
                </Badge>
              )}
              <Badge size="xs" variant="outline" color="gray">
                object
              </Badge>
              {deleted && (
                <Text size="xs" c="dimmed" fs="italic">
                  (deleted)
                </Text>
              )}
              {description && compact && <FieldInfoTip description={description} />}
            </Group>
            <Group gap={4} wrap="nowrap">
              {deleted && onRestore && (
                <Button
                  size="compact-xs"
                  variant="light"
                  onClick={() => onRestore({})}
                >
                  Restore
                </Button>
              )}
              {onDelete && (
                <ActionIcon
                  variant="subtle"
                  color="red"
                  size="sm"
                  aria-label={`Delete field ${field.name}`}
                  disabled={deleted}
                  onClick={onDelete}
                >
                  <IconTrash size={14} />
                </ActionIcon>
              )}
            </Group>
          </Group>
          {description && !compact && (
            <Text size="xs" c="dimmed" mb={6}>
              {description}
            </Text>
          )}
          {!deleted && (
            <SchemaInstanceForm
              schema={field.schema}
              value={obj}
              onChange={onChange}
              pathPrefix={`${path}.`}
              compact={compact}
              allowFieldDelete={allowFieldDelete}
            />
          )}
        </Paper>
      )
    }
    case 'ARRAY': {
      const items = !deleted && Array.isArray(value) ? value : []
      const itemSchema = field.schema.items
      return (
        <Paper withBorder radius="sm" p={compact ? 'xs' : 'sm'} mx={compact ? 8 : 0} mb={compact ? 8 : 0}>
          <Group justify="space-between" gap="xs" mb={compact ? 6 : 'xs'} wrap="nowrap">
            <Group gap={6} wrap="nowrap" style={{ minWidth: 0 }}>
              <Text size={compact ? 'xs' : 'sm'} fw={600}>
                {label}
              </Text>
              {required && (
                <Badge size="xs" variant="light" color="red">
                  required
                </Badge>
              )}
              {!deleted && (
                <Badge size="xs" variant="light" color="teal">
                  {items.length}
                </Badge>
              )}
              {deleted && (
                <Text size="xs" c="dimmed" fs="italic">
                  (deleted)
                </Text>
              )}
              {description && compact && <FieldInfoTip description={description} />}
            </Group>
            <Group gap={4} wrap="nowrap">
              {deleted && onRestore ? (
                <Button size="compact-xs" variant="light" onClick={() => onRestore([])}>
                  Restore
                </Button>
              ) : (
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
              )}
              {onDelete && (
                <ActionIcon
                  variant="subtle"
                  color="red"
                  size="sm"
                  aria-label={`Delete field ${field.name}`}
                  disabled={deleted}
                  onClick={onDelete}
                >
                  <IconTrash size={14} />
                </ActionIcon>
              )}
            </Group>
          </Group>
          {description && !compact && (
            <Text size="xs" c="dimmed" mb={6}>
              {description}
            </Text>
          )}
          {deleted ? null : items.length === 0 ? (
            <Text size="xs" c="dimmed" fs="italic">
              No items
            </Text>
          ) : (
            <Stack gap={6}>
              {items.map((item, index) => (
                <Paper
                  key={`${path}[${index}]`}
                  withBorder
                  radius="sm"
                  p={compact ? 6 : 'xs'}
                  bg="color-mix(in srgb, var(--mantine-color-default-hover) 40%, transparent)"
                >
                  <Group align="flex-start" wrap="nowrap" gap={6}>
                    <Badge size="xs" variant="filled" color="gray">
                      {index + 1}
                    </Badge>
                    <div style={{ flex: 1, minWidth: 0 }}>
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
                          allowFieldDelete={allowFieldDelete}
                        />
                      ) : (
                        <FieldControl
                          field={{
                            name: `[${index}]`,
                            schema:
                              itemSchema ?? { type: 'STRING', title: 'Item', description: '' },
                            required: true,
                          }}
                          value={item}
                          onChange={(next) => {
                            const copy = [...items]
                            copy[index] = next
                            onChange(copy)
                          }}
                          path={`${path}[${index}]`}
                          compact={!!compact}
                        />
                      )}
                    </div>
                    <ActionIcon
                      variant="subtle"
                      color="red"
                      size="sm"
                      aria-label="Remove item"
                      onClick={() => onChange(items.filter((_, i) => i !== index))}
                    >
                      <IconTrash size={14} />
                    </ActionIcon>
                  </Group>
                </Paper>
              ))}
            </Stack>
          )}
        </Paper>
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

function LabeledScalar({
  field,
  value,
  onChange,
  path,
}: {
  field: BoMSchemaField
  value: unknown
  onChange: (next: unknown) => void
  path: string
}) {
  const label = fieldLabel(field)
  const description = fieldDescription(field)
  const required = field.required === true
  const multiline = field.stereotype?.includes('multiline') === true

  switch (field.schema.type) {
    case 'STRING':
      return multiline ? (
        <Textarea
          label={label}
          description={description}
          required={required}
          value={typeof value === 'string' ? value : ''}
          onChange={(e) => onChange(e.currentTarget.value)}
          minRows={3}
        />
      ) : (
        <TextInput
          label={label}
          description={description}
          required={required}
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
          value={typeof value === 'number' ? value : undefined}
          onChange={(v) => {
            if (v === '' || v === undefined || v === null) {
              onChange(undefined)
              return
            }
            onChange(typeof v === 'number' ? v : Number(v))
          }}
          allowDecimal={field.schema.type === 'NUMBER'}
        />
      )
    case 'BOOLEAN':
      return (
        <Checkbox
          label={label}
          description={description}
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
          data={(field.schema.values ?? []).map((v) => ({
            value: v.value,
            label: v.description ? `${v.value} — ${v.description}` : v.value,
          }))}
          value={typeof value === 'string' && value.length > 0 ? value : null}
          onChange={(v) => onChange(v ?? undefined)}
          searchable
          clearable={!required}
        />
      )
    default:
      return (
        <Text size="sm" c="dimmed">
          Unsupported field type for {path}
        </Text>
      )
  }
}

/** Compact inspector used by Composer: optional section chrome + Field/Value table. */
export function PayloadInspector({
  schema,
  value,
  onChange,
  hideChrome = false,
  allowFieldDelete = false,
}: {
  schema: BoMSchemaNode
  value: Record<string, unknown>
  onChange: (next: Record<string, unknown>) => void
  /** Drop Payload SectionChrome when the label already appears as tab text. */
  hideChrome?: boolean
  allowFieldDelete?: boolean
}) {
  const fieldCount = schema.fields?.length ?? 0
  const body = (
    <Box p={!hideChrome && fieldCount > 0 ? 0 : hideChrome ? 0 : 'xs'}>
      <SchemaInstanceForm
        schema={schema}
        value={value}
        onChange={onChange}
        compact
        allowFieldDelete={allowFieldDelete}
      />
    </Box>
  )
  if (hideChrome) return body
  return (
    <SectionChrome title="Payload" count={fieldCount} accent="var(--mantine-color-blue-filled)">
      {body}
    </SectionChrome>
  )
}

export function AnnotationsEditor({
  value,
  onChange,
  compact = false,
  hideChrome = false,
}: {
  value: Record<string, string>
  onChange: (next: Record<string, string>) => void
  compact?: boolean
  /** Drop Annotations SectionChrome when the label already appears as tab text. */
  hideChrome?: boolean
}) {
  const rows = Object.entries(value)
  const displayRows = rows.length > 0 ? rows : [['', '']]

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

  function addRow() {
    onChange({ ...value, '': value[''] ?? '' })
  }

  if (!compact) {
    return (
      <Stack gap="xs">
        {displayRows.map(([key, val], index) => (
          <Group key={index} align="flex-end" wrap="nowrap" gap={6}>
            <TextInput
              label={index === 0 ? 'Key' : undefined}
              size="sm"
              value={key}
              onChange={(e) => updateRow(index, e.currentTarget.value, val)}
              style={{ flex: 1 }}
            />
            <TextInput
              label={index === 0 ? 'Value' : undefined}
              size="sm"
              value={val}
              onChange={(e) => updateRow(index, key, e.currentTarget.value)}
              style={{ flex: 1 }}
            />
            <ActionIcon
              variant="subtle"
              color="red"
              size="md"
              aria-label="Remove annotation"
              onClick={() => removeRow(index)}
              disabled={displayRows.length <= 1 && !key && !val}
              mb={4}
            >
              <IconTrash size={16} />
            </ActionIcon>
          </Group>
        ))}
        <Button
          size="compact-xs"
          variant="light"
          leftSection={<IconPlus size={12} />}
          onClick={addRow}
        >
          Add annotation
        </Button>
      </Stack>
    )
  }

  const filledCount = rows.filter(([k]) => k.trim().length > 0).length
  const addAction = (
    <Button
      size="compact-xs"
      variant="light"
      leftSection={<IconPlus size={12} />}
      onClick={addRow}
    >
      Add
    </Button>
  )

  const table = (
    <Table
      striped
      highlightOnHover
      withTableBorder={false}
      withColumnBorders
      horizontalSpacing={8}
      verticalSpacing={4}
      style={{ fontSize: 'var(--mantine-font-size-xs)' }}
    >
      <Table.Thead>
        <Table.Tr>
          <Table.Th w="38%">Key</Table.Th>
          <Table.Th>Value</Table.Th>
          <Table.Th w={36} />
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {displayRows.map(([key, val], index) => (
          <Table.Tr key={index}>
            <Table.Td
              style={{
                verticalAlign: 'middle',
                background:
                  'color-mix(in srgb, var(--mantine-color-default-hover) 40%, transparent)',
              }}
            >
              <TextInput
                size="xs"
                variant="unstyled"
                value={key}
                onChange={(e) => updateRow(index, e.currentTarget.value, val)}
                placeholder="key"
                styles={{
                  input: {
                    fontFamily:
                      'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
                    fontWeight: 700,
                    fontSize: 'var(--mantine-font-size-xs)',
                  },
                }}
              />
            </Table.Td>
            <Table.Td style={{ verticalAlign: 'middle' }}>
              <TextInput
                size="xs"
                variant="unstyled"
                value={val}
                onChange={(e) => updateRow(index, key, e.currentTarget.value)}
                placeholder="value"
                styles={{
                  input: { fontSize: 'var(--mantine-font-size-xs)' },
                }}
              />
            </Table.Td>
            <Table.Td style={{ verticalAlign: 'middle', paddingRight: 4 }}>
              <ActionIcon
                variant="subtle"
                color="red"
                size="sm"
                aria-label="Remove annotation"
                onClick={() => removeRow(index)}
                disabled={displayRows.length <= 1 && !key && !val}
              >
                <IconTrash size={14} />
              </ActionIcon>
            </Table.Td>
          </Table.Tr>
        ))}
      </Table.Tbody>
    </Table>
  )

  if (hideChrome) {
    return (
      <Stack gap={6}>
        <Group justify="flex-end">{addAction}</Group>
        {table}
      </Stack>
    )
  }

  return (
    <SectionChrome
      title="Annotations"
      count={filledCount}
      accent="var(--mantine-color-violet-filled)"
      action={addAction}
    >
      {table}
    </SectionChrome>
  )
}
