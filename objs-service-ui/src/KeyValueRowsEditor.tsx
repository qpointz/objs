import { ActionIcon, Button, Group, Stack, TextInput } from '@mantine/core'
import { IconPlus, IconTrash } from '@tabler/icons-react'

export type KeyValueRow = { key: string; value: string }

export const EMPTY_KEY_VALUE_ROWS: KeyValueRow[] = [{ key: '', value: '' }]

/** Non-blank keys only; later duplicate keys overwrite earlier ones. */
export function rowsToStringMap(rows: KeyValueRow[]): Record<string, string> {
  const filter: Record<string, string> = {}
  for (const row of rows) {
    const key = row.key.trim()
    if (!key) continue
    filter[key] = row.value
  }
  return filter
}

export function KeyValueRowsEditor({
  rows,
  onChange,
  keyPlaceholder = 'key',
  valuePlaceholder = 'value',
  addLabel = 'Add key/value',
}: {
  rows: KeyValueRow[]
  onChange: (rows: KeyValueRow[]) => void
  keyPlaceholder?: string
  valuePlaceholder?: string
  addLabel?: string
}) {
  return (
    <Stack gap={4}>
      {rows.map((row, index) => (
        <Group key={index} align="center" wrap="nowrap" gap={4}>
          <TextInput
            size="xs"
            placeholder={keyPlaceholder}
            aria-label={index === 0 ? 'Annotation key' : undefined}
            value={row.key}
            onChange={(e) => {
              const next = [...rows]
              next[index] = { ...row, key: e.currentTarget.value }
              onChange(next)
            }}
            style={{ flex: 1, minWidth: 0 }}
          />
          <TextInput
            size="xs"
            placeholder={valuePlaceholder}
            aria-label={index === 0 ? 'Annotation value' : undefined}
            value={row.value}
            onChange={(e) => {
              const next = [...rows]
              next[index] = { ...row, value: e.currentTarget.value }
              onChange(next)
            }}
            style={{ flex: 1, minWidth: 0 }}
          />
          <ActionIcon
            size="xs"
            variant="subtle"
            color="red"
            aria-label="Remove annotation row"
            onClick={() => onChange(rows.filter((_, i) => i !== index))}
            disabled={rows.length <= 1}
          >
            <IconTrash size={12} />
          </ActionIcon>
        </Group>
      ))}
      <Button
        variant="subtle"
        size="compact-xs"
        w="fit-content"
        px={4}
        leftSection={<IconPlus size={11} />}
        onClick={() => onChange([...rows, { key: '', value: '' }])}
      >
        {addLabel}
      </Button>
    </Stack>
  )
}
