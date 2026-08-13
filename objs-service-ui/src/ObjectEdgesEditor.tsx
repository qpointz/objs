import { useEffect, useMemo, useRef, useState } from 'react'
import {
  ActionIcon,
  Anchor,
  Badge,
  Button,
  Checkbox,
  Group,
  Paper,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
  Tooltip,
} from '@mantine/core'
import {
  IconArrowNarrowLeftDashed,
  IconArrowNarrowRightDashed,
  IconPencil,
  IconTrash,
} from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import { allowedEdgeKey, type AllowedEdgeRef } from './allowedEdgeRef'
import type { BoMAllowedEdgeRule, BoMEdgeCardinality } from './types'

type EdgeDirection = 'incoming' | 'outbound'

type DirectedRow = BoMAllowedEdgeRule & { direction: EdgeDirection }

export type AllowedEdgeDraft = {
  sourceType: string
  role: string
  targetType: string
  cardinality: BoMEdgeCardinality
  propertiesPolicy: 'NONE' | 'SCHEMA'
  propertiesSchemaType?: string
  propertiesSchemaVersion?: string
  emptyPropertiesAllowed: boolean
}

const CARDINALITY_OPTIONS: { value: BoMEdgeCardinality; label: string }[] = [
  { value: 'UNSPECIFIED', label: 'UNSPECIFIED' },
  { value: '1:1', label: '1:1' },
  { value: '1:*', label: '1:*' },
]

function TypeLink({ type }: { type: string }) {
  if (type === '*') return <>*</>
  return (
    <Anchor component={Link} to={`/model/${encodeURIComponent(type)}`}>
      {type}
    </Anchor>
  )
}

function DirectionIcon({ direction }: { direction: EdgeDirection }) {
  const Icon = direction === 'incoming' ? IconArrowNarrowLeftDashed : IconArrowNarrowRightDashed
  const label = direction === 'incoming' ? 'Incoming' : 'Outgoing'
  return (
    <Tooltip label={label}>
      <Icon size={18} stroke={1.75} aria-label={label} />
    </Tooltip>
  )
}

export function ObjectEdgesEditor({
  selectedType,
  incoming,
  outgoing,
  entityTypes,
  edgeSchemaOptions,
  busy,
  highlightedEdge,
  onCreate,
  onUpdate,
  onDelete,
}: {
  selectedType: string
  incoming: BoMAllowedEdgeRule[]
  outgoing: BoMAllowedEdgeRule[]
  entityTypes: string[]
  edgeSchemaOptions: { value: string; label: string }[]
  busy?: boolean
  highlightedEdge?: AllowedEdgeRef | null
  onCreate: (rule: AllowedEdgeDraft) => Promise<void>
  onUpdate: (previous: BoMAllowedEdgeRule, next: AllowedEdgeDraft) => Promise<void>
  onDelete: (rule: BoMAllowedEdgeRule) => Promise<void>
}) {
  const rows: DirectedRow[] = useMemo(
    () => [
      ...incoming.map((rule) => ({ ...rule, direction: 'incoming' as const })),
      ...outgoing.map((rule) => ({ ...rule, direction: 'outbound' as const })),
    ],
    [incoming, outgoing],
  )
  const highlightKey = highlightedEdge ? allowedEdgeKey(highlightedEdge) : null
  const highlightedRowRef = useRef<HTMLTableRowElement | null>(null)

  useEffect(() => {
    if (!highlightKey || !highlightedRowRef.current) return
    highlightedRowRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [highlightKey])

  const typeOptions = [
    { value: '*', label: 'Any type (*)' },
    ...entityTypes.map((type) => ({ value: type, label: type })),
  ]

  const [editingOriginal, setEditingOriginal] = useState<BoMAllowedEdgeRule | null>(null)
  const [direction, setDirection] = useState<'incoming' | 'outbound'>('outbound')
  const [otherType, setOtherType] = useState(entityTypes[0] ?? '*')
  const [role, setRole] = useState('RELATES_TO')
  const [cardinality, setCardinality] = useState<BoMEdgeCardinality>('UNSPECIFIED')
  const [policy, setPolicy] = useState<'NONE' | 'SCHEMA'>('NONE')
  const [edgeSchemaKey, setEdgeSchemaKey] = useState<string | null>(edgeSchemaOptions[0]?.value ?? null)
  const [emptyAllowed, setEmptyAllowed] = useState(true)
  const [formError, setFormError] = useState<string | null>(null)

  function resetForm() {
    setEditingOriginal(null)
    setDirection('outbound')
    setOtherType(entityTypes[0] ?? '*')
    setRole('RELATES_TO')
    setCardinality('UNSPECIFIED')
    setPolicy('NONE')
    setEdgeSchemaKey(edgeSchemaOptions[0]?.value ?? null)
    setEmptyAllowed(true)
    setFormError(null)
  }

  useEffect(() => {
    setEditingOriginal(null)
    setDirection('outbound')
    setOtherType(entityTypes[0] ?? '*')
    setRole('RELATES_TO')
    setCardinality('UNSPECIFIED')
    setPolicy('NONE')
    setEdgeSchemaKey(edgeSchemaOptions[0]?.value ?? null)
    setEmptyAllowed(true)
    setFormError(null)
  }, [selectedType, entityTypes, edgeSchemaOptions])

  function startEdit(rule: DirectedRow) {
    setEditingOriginal(rule)
    setDirection(rule.direction)
    setOtherType(rule.direction === 'outbound' ? rule.targetType : rule.sourceType)
    setRole(rule.role)
    setCardinality(rule.cardinality ?? 'UNSPECIFIED')
    setPolicy(rule.propertiesPolicy)
    setEdgeSchemaKey(
      rule.propertiesSchemaType && rule.propertiesSchemaVersion
        ? `${rule.propertiesSchemaType}@${rule.propertiesSchemaVersion}`
        : edgeSchemaOptions[0]?.value ?? null,
    )
    setEmptyAllowed(rule.emptyPropertiesAllowed)
    setFormError(null)
  }

  function buildDraft(): AllowedEdgeDraft | null {
    setFormError(null)
    if (!role.trim() || !otherType.trim()) {
      setFormError('Role and related type are required')
      return null
    }
    const sourceType = direction === 'outbound' ? selectedType : otherType
    const targetType = direction === 'outbound' ? otherType : selectedType
    let propertiesSchemaType: string | undefined
    let propertiesSchemaVersion: string | undefined
    if (policy === 'SCHEMA') {
      if (!edgeSchemaKey) {
        setFormError('Select an edge property schema')
        return null
      }
      const [type, version] = edgeSchemaKey.split('@')
      propertiesSchemaType = type
      propertiesSchemaVersion = version
    }
    return {
      sourceType,
      role: role.trim(),
      targetType,
      cardinality,
      propertiesPolicy: policy,
      propertiesSchemaType,
      propertiesSchemaVersion,
      emptyPropertiesAllowed: emptyAllowed,
    }
  }

  async function submit() {
    const draft = buildDraft()
    if (!draft) return
    if (editingOriginal) {
      await onUpdate(editingOriginal, draft)
    } else {
      await onCreate(draft)
    }
    resetForm()
  }

  const editingKey = editingOriginal ? allowedEdgeKey(editingOriginal) : null

  return (
    <Stack gap="xs">
      {rows.length === 0 ? (
        <Text size="sm" c="dimmed">
          No inbound or outbound rules for this object yet.
        </Text>
      ) : (
        <Table striped withTableBorder>
          <Table.Thead>
            <Table.Tr>
              <Table.Th w={44}></Table.Th>
              <Table.Th>Source</Table.Th>
              <Table.Th>Role</Table.Th>
              <Table.Th>Target</Table.Th>
              <Table.Th>Cardinality</Table.Th>
              <Table.Th>Properties</Table.Th>
              <Table.Th w={72}></Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {rows.map((rule) => {
              const rowKey = allowedEdgeKey(rule)
              const highlighted = highlightKey === rowKey
              const editing = editingKey === rowKey
              return (
                <Table.Tr
                  key={`${rule.direction}|${rowKey}`}
                  ref={highlighted ? highlightedRowRef : undefined}
                  bg={
                    editing
                      ? 'var(--mantine-color-yellow-light)'
                      : highlighted
                        ? 'var(--mantine-color-blue-light)'
                        : undefined
                  }
                  style={
                    highlighted || editing
                      ? {
                          outline: `2px solid var(--mantine-color-${editing ? 'yellow' : 'blue'}-filled)`,
                          outlineOffset: -2,
                        }
                      : undefined
                  }
                >
                  <Table.Td>
                    <DirectionIcon direction={rule.direction} />
                  </Table.Td>
                  <Table.Td>
                    <TypeLink type={rule.sourceType} />
                  </Table.Td>
                  <Table.Td>{rule.role}</Table.Td>
                  <Table.Td>
                    <TypeLink type={rule.targetType} />
                  </Table.Td>
                  <Table.Td>
                    <Badge size="sm" variant="outline">
                      {rule.cardinality ?? 'UNSPECIFIED'}
                    </Badge>
                  </Table.Td>
                  <Table.Td>
                    <Badge size="sm" variant="light">
                      {rule.propertiesPolicy}
                    </Badge>
                    {rule.propertiesSchemaType && (
                      <Text size="xs" c="dimmed" mt={2}>
                        {rule.propertiesSchemaType}@{rule.propertiesSchemaVersion}
                      </Text>
                    )}
                  </Table.Td>
                  <Table.Td>
                    <Group gap={4} wrap="nowrap" justify="flex-end">
                      <ActionIcon
                        variant="subtle"
                        title="Edit rule"
                        disabled={busy}
                        onClick={() => startEdit(rule)}
                      >
                        <IconPencil size={16} />
                      </ActionIcon>
                      <ActionIcon
                        color="red"
                        variant="subtle"
                        title="Delete rule"
                        disabled={busy}
                        onClick={() => {
                          if (editingKey === rowKey) resetForm()
                          void onDelete(rule)
                        }}
                      >
                        <IconTrash size={16} />
                      </ActionIcon>
                    </Group>
                  </Table.Td>
                </Table.Tr>
              )
            })}
          </Table.Tbody>
        </Table>
      )}

      <Paper withBorder p="sm">
        <Group justify="space-between" mb="xs">
          <Text size="sm" fw={600}>
            {editingOriginal ? 'Edit allowed edge' : 'Add allowed edge'}
          </Text>
          {editingOriginal && (
            <Button size="compact-xs" variant="subtle" onClick={resetForm} disabled={busy}>
              Cancel
            </Button>
          )}
        </Group>
        <Group align="flex-end" wrap="wrap">
          <Select
            label="Direction"
            allowDeselect={false}
            data={[
              { value: 'outbound', label: 'Outgoing' },
              { value: 'incoming', label: 'Incoming' },
            ]}
            value={direction}
            onChange={(v) => setDirection((v as 'incoming' | 'outbound') ?? 'outbound')}
            w={140}
          />
          <Select
            label={direction === 'outbound' ? 'Target type' : 'Source type'}
            searchable
            allowDeselect={false}
            data={typeOptions}
            value={otherType}
            onChange={(v) => setOtherType(v ?? '*')}
            style={{ flex: 1, minWidth: 160 }}
          />
          <TextInput
            label="Role"
            value={role}
            onChange={(e) => setRole(e.currentTarget.value)}
            style={{ flex: 1, minWidth: 140 }}
          />
          <Select
            label="Cardinality"
            allowDeselect={false}
            data={CARDINALITY_OPTIONS}
            value={cardinality}
            onChange={(v) => setCardinality((v as BoMEdgeCardinality) ?? 'UNSPECIFIED')}
            w={140}
          />
          <Select
            label="Properties"
            allowDeselect={false}
            data={[
              { value: 'NONE', label: 'NONE' },
              { value: 'SCHEMA', label: 'SCHEMA' },
            ]}
            value={policy}
            onChange={(v) => setPolicy((v as 'NONE' | 'SCHEMA') ?? 'NONE')}
            w={120}
          />
          {policy === 'SCHEMA' && (
            <Select
              label="Edge schema"
              searchable
              allowDeselect={false}
              data={edgeSchemaOptions}
              value={edgeSchemaKey}
              onChange={setEdgeSchemaKey}
              style={{ flex: 1, minWidth: 180 }}
            />
          )}
          {policy === 'SCHEMA' && (
            <Checkbox
              label="Empty props OK"
              checked={emptyAllowed}
              onChange={(e) => setEmptyAllowed(e.currentTarget.checked)}
              mb={8}
            />
          )}
          <Button loading={busy} onClick={() => void submit()}>
            {editingOriginal ? 'Save' : 'Add'}
          </Button>
        </Group>
        {formError && (
          <Text size="sm" c="red" mt="xs">
            {formError}
          </Text>
        )}
      </Paper>
    </Stack>
  )
}
