import {
  ActionIcon,
  Button,
  Checkbox,
  Group,
  Paper,
  Select,
  Stack,
  Text,
  TextInput,
} from '@mantine/core'
import type { EdgeRelationRequest } from './types'

export function EdgeRelationsEditor({
  value,
  entityTypes,
  onChange,
}: {
  value: EdgeRelationRequest[]
  entityTypes: string[]
  onChange: (relations: EdgeRelationRequest[]) => void
}) {
  const typeOptions = [
    { value: '*', label: 'Any type (*)' },
    ...entityTypes.map((type) => ({ value: type, label: type })),
  ]

  function update(index: number, patch: Partial<EdgeRelationRequest>) {
    const next = [...value]
    next[index] = { ...next[index], ...patch }
    onChange(next)
  }

  function remove(index: number) {
    const next = [...value]
    next.splice(index, 1)
    onChange(next)
  }

  return (
    <Paper withBorder p="md">
      <Group justify="space-between" mb="sm">
        <div>
          <Text fw={700}>Allowed entity relations</Text>
          <Text size="sm" c="dimmed">
            This property schema can govern multiple directed source–role–target relations.
          </Text>
        </div>
        <Button
          size="xs"
          variant="light"
          onClick={() =>
            onChange([
              ...value,
              {
                sourceType: entityTypes[0] ?? '*',
                role: 'RELATES_TO',
                targetType: entityTypes[0] ?? '*',
                emptyPropertiesAllowed: true,
              },
            ])
          }
        >
          Add relation
        </Button>
      </Group>

      {value.length === 0 ? (
        <Text size="sm" c="dimmed">
          No relations use this edge-property schema yet.
        </Text>
      ) : (
        <Stack gap="xs">
          {value.map((relation, index) => (
            <Paper key={index} withBorder p="sm">
              <Group align="flex-end" wrap="nowrap">
                <Select
                  label="Source entity"
                  searchable
                  allowDeselect={false}
                  data={typeOptions}
                  value={relation.sourceType}
                  onChange={(sourceType) => update(index, { sourceType: sourceType ?? '' })}
                  style={{ flex: 1 }}
                />
                <TextInput
                  label="Role"
                  value={relation.role}
                  onChange={(event) => update(index, { role: event.currentTarget.value })}
                  style={{ flex: 1 }}
                />
                <Select
                  label="Target entity"
                  searchable
                  allowDeselect={false}
                  data={typeOptions}
                  value={relation.targetType}
                  onChange={(targetType) => update(index, { targetType: targetType ?? '' })}
                  style={{ flex: 1 }}
                />
                <Checkbox
                  label="Empty properties allowed"
                  checked={relation.emptyPropertiesAllowed}
                  onChange={(event) =>
                    update(index, { emptyPropertiesAllowed: event.currentTarget.checked })
                  }
                  mb={8}
                />
                <ActionIcon
                  color="red"
                  variant="subtle"
                  size="lg"
                  mb={2}
                  title="Remove relation"
                  onClick={() => remove(index)}
                >
                  ×
                </ActionIcon>
              </Group>
            </Paper>
          ))}
        </Stack>
      )}
    </Paper>
  )
}
