import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import {
  Anchor,
  Badge,
  Box,
  Button,
  Checkbox,
  Group,
  Paper,
  Select,
  SimpleGrid,
  Stack,
  TagsInput,
  Text,
  TextInput,
  Textarea,
  Tooltip,
} from '@mantine/core'
import {
  IconArrowNarrowLeftDashed,
  IconArrowNarrowRightDashed,
  IconPlus,
} from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import {
  allowedEdgeKey,
  directedEdgeKey,
  edgesForType,
  type AllowedEdgeRef,
} from './allowedEdgeRef'
import {
  EMPTY_KEY_VALUE_ROWS,
  KeyValueRowsEditor,
  rowsToStringMap,
  stringMapToRows,
} from './KeyValueRowsEditor'
import type { BoMAllowedEdgeRule, BoMEdgeCardinality } from './types'

type EdgeDirection = 'incoming' | 'outbound'

type DirectedRow = BoMAllowedEdgeRule & { direction: EdgeDirection; deleted: boolean }

type PanelMode = 'view' | 'add' | 'edit'

export type AllowedEdgeDraft = {
  sourceType: string
  role: string
  targetType: string
  cardinality: BoMEdgeCardinality
  propertiesPolicy: 'NONE' | 'SCHEMA'
  propertiesSchemaType?: string
  propertiesSchemaVersion?: string
  emptyPropertiesAllowed: boolean
  description?: string
  sourceVerb?: string
  targetVerb?: string
  tags?: string[]
  attributes?: Record<string, string>
}

/** Icon | Source 25% (½ of prior 50%) | Role 15% (½ of prior 30%) | card | Target remainder */
const EDGE_LIST_COLUMNS = '2rem 25% 15% 3rem minmax(0, 1fr)'

const CARDINALITY_OPTIONS: { value: BoMEdgeCardinality; label: string }[] = [
  { value: 'UNSPECIFIED', label: 'UNSPECIFIED' },
  { value: '1:1', label: '1:1' },
  { value: '1:*', label: '1:*' },
]

const VERB_INPUT_PROPS = {
  autoCapitalize: 'off' as const,
  autoCorrect: 'off',
  spellCheck: false,
  styles: { input: { textTransform: 'none' as const } },
}

function TypeLink({ type, currentType }: { type: string; currentType?: string }) {
  if (type === '*' || type === currentType) return <>{type === '*' ? '*' : type}</>
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
      <Icon size={16} stroke={1.75} aria-label={label} />
    </Tooltip>
  )
}

function AttributePill({ name, value }: { name: string; value: string }) {
  return (
    <Group gap={0} wrap="nowrap" title={`${name}: ${value}`}>
      <Badge
        size="sm"
        variant="filled"
        color="gray"
        radius="xl"
        tt="none"
        style={{
          borderTopRightRadius: 0,
          borderBottomRightRadius: 0,
          paddingLeft: 8,
          paddingRight: 8,
        }}
      >
        {name}
      </Badge>
      <Badge
        size="sm"
        variant="light"
        color="gray"
        radius="xl"
        tt="none"
        style={{
          borderTopLeftRadius: 0,
          borderBottomLeftRadius: 0,
          paddingLeft: 8,
          paddingRight: 8,
        }}
      >
        {value || '—'}
      </Badge>
    </Group>
  )
}

function DetailField({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Stack gap={4}>
      <Text size="sm" fw={500}>
        {label}
      </Text>
      {children}
    </Stack>
  )
}

function ViewValue({ children, dimmed }: { children: ReactNode; dimmed?: boolean }) {
  return (
    <Text size="sm" c={dimmed ? 'dimmed' : undefined} style={{ minHeight: 28, paddingTop: 6 }}>
      {children || '—'}
    </Text>
  )
}

function mergeDirected(
  current: BoMAllowedEdgeRule[],
  baseline: BoMAllowedEdgeRule[],
  direction: EdgeDirection,
): DirectedRow[] {
  const currentKeys = new Set(current.map(allowedEdgeKey))
  const rows: DirectedRow[] = current.map((rule) => ({ ...rule, direction, deleted: false }))
  for (const rule of baseline) {
    if (!currentKeys.has(allowedEdgeKey(rule))) {
      rows.push({ ...rule, direction, deleted: true })
    }
  }
  return rows
}

function rowKey(rule: { direction: EdgeDirection } & AllowedEdgeRef): string {
  return directedEdgeKey({ ...rule, direction: rule.direction })
}

export function ObjectEdgesEditor({
  selectedType,
  incoming,
  outgoing,
  baselineRules = [],
  entityTypes,
  edgeSchemaOptions,
  busy,
  highlightedEdge,
  onCreate,
  onUpdate,
  onDelete,
  onRestore,
  onHighlight,
}: {
  selectedType: string
  incoming: BoMAllowedEdgeRule[]
  outgoing: BoMAllowedEdgeRule[]
  baselineRules?: BoMAllowedEdgeRule[]
  entityTypes: string[]
  edgeSchemaOptions: { value: string; label: string }[]
  busy?: boolean
  highlightedEdge?: AllowedEdgeRef | null
  onCreate: (rule: AllowedEdgeDraft) => Promise<void>
  onUpdate: (previous: BoMAllowedEdgeRule, next: AllowedEdgeDraft) => Promise<void>
  onDelete: (rule: BoMAllowedEdgeRule) => Promise<void>
  onRestore?: (rule: BoMAllowedEdgeRule) => Promise<void>
  onHighlight?: (edge: AllowedEdgeRef | null) => void
}) {
  const baselineSplit = useMemo(
    () => edgesForType(selectedType, baselineRules),
    [selectedType, baselineRules],
  )
  const rows: DirectedRow[] = useMemo(
    () => [
      ...mergeDirected(incoming, baselineSplit.incoming, 'incoming'),
      ...mergeDirected(outgoing, baselineSplit.outgoing, 'outbound'),
    ],
    [incoming, outgoing, baselineSplit],
  )
  const highlightKey = highlightedEdge
    ? highlightedEdge.direction
      ? directedEdgeKey(highlightedEdge as AllowedEdgeRef & { direction: EdgeDirection })
      : allowedEdgeKey(highlightedEdge)
    : null
  const highlightedRowRef = useRef<HTMLDivElement | null>(null)

  const typeOptions = [
    { value: '*', label: 'Any type (*)' },
    ...entityTypes.map((type) => ({ value: type, label: type })),
  ]

  const [panelMode, setPanelMode] = useState<PanelMode>('view')
  const [selectedKey, setSelectedKey] = useState<string | null>(null)
  const [editingOriginal, setEditingOriginal] = useState<BoMAllowedEdgeRule | null>(null)
  const [direction, setDirection] = useState<EdgeDirection>('outbound')
  const [otherType, setOtherType] = useState(entityTypes[0] ?? '*')
  const [role, setRole] = useState('RELATES_TO')
  const [cardinality, setCardinality] = useState<BoMEdgeCardinality>('UNSPECIFIED')
  const [policy, setPolicy] = useState<'NONE' | 'SCHEMA'>('NONE')
  const [edgeSchemaKey, setEdgeSchemaKey] = useState<string | null>(
    edgeSchemaOptions[0]?.value ?? null,
  )
  const [emptyAllowed, setEmptyAllowed] = useState(true)
  const [description, setDescription] = useState('')
  const [sourceVerb, setSourceVerb] = useState('')
  const [targetVerb, setTargetVerb] = useState('')
  const [tags, setTags] = useState<string[]>([])
  const [attributeRows, setAttributeRows] = useState(EMPTY_KEY_VALUE_ROWS)
  const [formError, setFormError] = useState<string | null>(null)

  function resetFormFields() {
    setDirection('outbound')
    setOtherType(entityTypes[0] ?? '*')
    setRole('RELATES_TO')
    setCardinality('UNSPECIFIED')
    setPolicy('NONE')
    setEdgeSchemaKey(edgeSchemaOptions[0]?.value ?? null)
    setEmptyAllowed(true)
    setDescription('')
    setSourceVerb('')
    setTargetVerb('')
    setTags([])
    setAttributeRows(EMPTY_KEY_VALUE_ROWS)
    setFormError(null)
  }

  function closeForm() {
    setEditingOriginal(null)
    resetFormFields()
    setPanelMode('view')
  }

  useEffect(() => {
    setEditingOriginal(null)
    resetFormFields()
    setPanelMode('view')
    setSelectedKey(null)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedType, entityTypes, edgeSchemaOptions])

  useEffect(() => {
    if (highlightKey) {
      setSelectedKey(
        highlightedEdge?.direction ? highlightKey : (rows.find((rule) => allowedEdgeKey(rule) === highlightKey)
          ? rowKey(rows.find((rule) => allowedEdgeKey(rule) === highlightKey)!)
          : highlightKey),
      )
      return
    }
    setSelectedKey((current) => {
      if (current && rows.some((rule) => rowKey(rule) === current)) return current
      return rows[0] ? rowKey(rows[0]) : null
    })
  }, [rows, highlightKey, highlightedEdge])

  useEffect(() => {
    if (!highlightKey || !highlightedRowRef.current) return
    highlightedRowRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [highlightKey])

  function fillForm(rule: DirectedRow) {
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
    setDescription(rule.description ?? '')
    setSourceVerb(rule.sourceVerb ?? '')
    setTargetVerb(rule.targetVerb ?? '')
    setTags(rule.tags ?? [])
    setAttributeRows(stringMapToRows(rule.attributes))
    setFormError(null)
  }

  function highlight(rule: DirectedRow) {
    onHighlight?.({
      sourceType: rule.sourceType,
      role: rule.role,
      targetType: rule.targetType,
      direction: rule.direction,
    })
  }

  function selectRow(rule: DirectedRow) {
    setSelectedKey(rowKey(rule))
    highlight(rule)
    if (panelMode !== 'view') closeForm()
  }

  function startAdd() {
    setEditingOriginal(null)
    resetFormFields()
    setPanelMode('add')
  }

  function startEdit(rule: DirectedRow) {
    if (rule.deleted) return
    setSelectedKey(rowKey(rule))
    setEditingOriginal(rule)
    fillForm(rule)
    setPanelMode('edit')
    highlight(rule)
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
      description: description.trim() || undefined,
      sourceVerb: sourceVerb.trim() || undefined,
      targetVerb: targetVerb.trim() || undefined,
      tags: tags.map((t) => t.trim()).filter(Boolean),
      attributes: rowsToStringMap(attributeRows),
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
    setSelectedKey(rowKey({ ...draft, direction }))
    onHighlight?.({ ...draft, direction })
    closeForm()
  }

  async function deleteSelected() {
    if (!selected || selected.deleted) return
    await onDelete(selected)
    setPanelMode('view')
    setEditingOriginal(null)
    resetFormFields()
  }

  async function restoreSelected() {
    if (!selected?.deleted || !onRestore) return
    await onRestore(selected)
    setPanelMode('view')
  }

  const selected = rows.find((rule) => rowKey(rule) === selectedKey) ?? null
  const incomingRows = rows.filter((rule) => rule.direction === 'incoming')
  const outgoingRows = rows.filter((rule) => rule.direction === 'outbound')
  const deletedCount = rows.filter((rule) => rule.deleted).length
  const editing = panelMode === 'edit' || panelMode === 'add'
  const sourceTypeValue = direction === 'outbound' ? selectedType : otherType
  const targetTypeValue = direction === 'outbound' ? otherType : selectedType
  const viewSource = selected?.sourceType ?? sourceTypeValue
  const viewTarget = selected?.targetType ?? targetTypeValue
  const viewTags = editing ? tags : (selected?.tags ?? [])
  const viewAttributes = editing
    ? Object.entries(rowsToStringMap(attributeRows))
    : Object.entries(selected?.attributes ?? {})

  function renderRuleRows(group: DirectedRow[]) {
    return group.map((rule) => {
      const key = rowKey(rule)
      const highlighted = highlightKey === key || (!highlightedEdge?.direction && highlightKey === allowedEdgeKey(rule))
      const selectedRow = selectedKey === key
      return (
        <Box
          key={key}
          ref={highlighted ? highlightedRowRef : undefined}
          onClick={() => selectRow(rule)}
          px="sm"
          py={6}
          style={{
            display: 'grid',
            gridTemplateColumns: EDGE_LIST_COLUMNS,
            alignItems: 'center',
            columnGap: 8,
            width: '100%',
            minWidth: 0,
            cursor: 'pointer',
            opacity: rule.deleted ? 0.7 : 1,
            textDecoration: rule.deleted ? 'line-through' : undefined,
            background: selectedRow
              ? `var(--mantine-color-${rule.deleted ? 'red' : 'blue'}-light)`
              : undefined,
            outline: selectedRow
              ? `2px solid var(--mantine-color-${rule.deleted ? 'red' : 'blue'}-filled)`
              : undefined,
            outlineOffset: -2,
          }}
        >
          <DirectionIcon direction={rule.direction} />
          <Text size="sm" truncate="end" style={{ minWidth: 0 }}>
            <TypeLink type={rule.sourceType} currentType={selectedType} />
          </Text>
          <Group gap={6} wrap="nowrap" style={{ minWidth: 0, overflow: 'hidden' }}>
            <Text size="sm" truncate="end">
              {rule.role}
            </Text>
            {rule.deleted && (
              <Badge size="xs" color="red" variant="filled">
                Deleted
              </Badge>
            )}
          </Group>
          <Text size="sm" c="dimmed" ta="center">
            {rule.cardinality && rule.cardinality !== 'UNSPECIFIED' ? rule.cardinality : ''}
          </Text>
          <Text size="sm" truncate="end" style={{ minWidth: 0 }}>
            <TypeLink type={rule.targetType} currentType={selectedType} />
          </Text>
        </Box>
      )
    })
  }

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group justify="space-between" wrap="wrap" style={{ flexShrink: 0 }}>
        <Text size="sm" c="dimmed">
          Incoming {incomingRows.filter((r) => !r.deleted).length} · Outgoing{' '}
          {outgoingRows.filter((r) => !r.deleted).length}
          {deletedCount > 0 ? ` · ${deletedCount} deleted` : ''}
        </Text>
        {panelMode === 'view' && (
          <Button
            size="xs"
            variant="light"
            leftSection={<IconPlus size={14} />}
            disabled={busy}
            onClick={startAdd}
          >
            Add allowed edge
          </Button>
        )}
      </Group>

      {rows.length === 0 && panelMode !== 'add' ? (
        <Text size="sm" c="dimmed">
          No allowed edges for this type.
        </Text>
      ) : rows.length > 0 ? (
        <Paper
          withBorder
          radius="sm"
          style={{
            overflow: 'auto',
            width: '100%',
            flex: selected || panelMode === 'add' ? '0 1 42%' : 1,
            minHeight: 0,
          }}
        >
          <Box
            px="sm"
            py={6}
            style={{
              display: 'grid',
              gridTemplateColumns: EDGE_LIST_COLUMNS,
              alignItems: 'center',
              columnGap: 8,
              width: '100%',
              borderBottom: '1px solid var(--mantine-color-default-border)',
            }}
          >
            <span />
            <Text size="xs" c="dimmed" fw={600}>
              Source
            </Text>
            <Text size="xs" c="dimmed" fw={600}>
              Role
            </Text>
            <span />
            <Text size="xs" c="dimmed" fw={600}>
              Target
            </Text>
          </Box>
          {incomingRows.length > 0 && (
            <Text size="xs" c="dimmed" fw={600} px="sm" py={4}>
              Incoming
            </Text>
          )}
          {renderRuleRows(incomingRows)}
          {outgoingRows.length > 0 && (
            <Text size="xs" c="dimmed" fw={600} px="sm" py={4}>
              Outgoing
            </Text>
          )}
          {renderRuleRows(outgoingRows)}
        </Paper>
      ) : null}

      {(panelMode === 'add' || selected) && (
        <Paper
          withBorder
          p="md"
          shadow="xs"
          radius="md"
          style={{ flex: 1, minHeight: 0, overflow: 'auto' }}
        >
          <Group justify="space-between" mb="md" wrap="wrap">
            <Group gap="xs">
              <Text size="sm" fw={600}>
                {panelMode === 'add'
                  ? 'Add allowed edge'
                  : panelMode === 'edit'
                    ? 'Edit allowed edge'
                    : 'Allowed edge'}
              </Text>
              {selected?.deleted && panelMode === 'view' && (
                <Badge color="red" variant="filled" size="sm">
                  Deleted
                </Badge>
              )}
            </Group>
            <Group gap={6}>
              {panelMode === 'view' && selected && !selected.deleted && (
                <>
                  <Button size="xs" variant="light" disabled={busy} onClick={() => startEdit(selected)}>
                    Edit
                  </Button>
                  <Button
                    size="xs"
                    color="red"
                    variant="light"
                    disabled={busy}
                    onClick={() => void deleteSelected()}
                  >
                    Delete
                  </Button>
                </>
              )}
              {panelMode === 'view' && selected?.deleted && (
                <Button size="xs" variant="light" disabled={busy} onClick={() => void restoreSelected()}>
                  Restore
                </Button>
              )}
              {editing && (
                <>
                  <Button size="xs" variant="subtle" onClick={closeForm} disabled={busy}>
                    Cancel
                  </Button>
                  <Button size="xs" loading={busy} onClick={() => void submit()}>
                    {panelMode === 'edit' ? 'Save' : 'Add'}
                  </Button>
                </>
              )}
            </Group>
          </Group>

          <Stack gap="md">
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
              <DetailField label="Direction">
                {editing ? (
                  <Select
                    allowDeselect={false}
                    data={[
                      { value: 'outbound', label: 'Outgoing' },
                      { value: 'incoming', label: 'Incoming' },
                    ]}
                    value={direction}
                    onChange={(v) => setDirection((v as EdgeDirection) ?? 'outbound')}
                  />
                ) : (
                  <ViewValue>{selected?.direction === 'incoming' ? 'Incoming' : 'Outgoing'}</ViewValue>
                )}
              </DetailField>
              <DetailField label="Role">
                {editing ? (
                  <TextInput value={role} onChange={(e) => setRole(e.currentTarget.value)} {...VERB_INPUT_PROPS} />
                ) : (
                  <ViewValue>{selected?.role}</ViewValue>
                )}
              </DetailField>
              <DetailField label="Source type">
                {editing ? (
                  direction === 'outbound' ? (
                    <ViewValue>{selectedType}</ViewValue>
                  ) : (
                    <Select
                      searchable
                      allowDeselect={false}
                      data={typeOptions}
                      value={otherType}
                      onChange={(v) => setOtherType(v ?? '*')}
                    />
                  )
                ) : (
                  <ViewValue>
                    <TypeLink type={viewSource} />
                  </ViewValue>
                )}
              </DetailField>
              <DetailField label="Target type">
                {editing ? (
                  direction === 'incoming' ? (
                    <ViewValue>{selectedType}</ViewValue>
                  ) : (
                    <Select
                      searchable
                      allowDeselect={false}
                      data={typeOptions}
                      value={otherType}
                      onChange={(v) => setOtherType(v ?? '*')}
                    />
                  )
                ) : (
                  <ViewValue>
                    <TypeLink type={viewTarget} />
                  </ViewValue>
                )}
              </DetailField>
              <DetailField label="Cardinality">
                {editing ? (
                  <Select
                    allowDeselect={false}
                    data={CARDINALITY_OPTIONS}
                    value={cardinality}
                    onChange={(v) => setCardinality((v as BoMEdgeCardinality) ?? 'UNSPECIFIED')}
                  />
                ) : (
                  <ViewValue>{selected?.cardinality ?? 'UNSPECIFIED'}</ViewValue>
                )}
              </DetailField>
              <DetailField label="Properties">
                {editing ? (
                  <Select
                    allowDeselect={false}
                    data={[
                      { value: 'NONE', label: 'NONE' },
                      { value: 'SCHEMA', label: 'SCHEMA' },
                    ]}
                    value={policy}
                    onChange={(v) => setPolicy((v as 'NONE' | 'SCHEMA') ?? 'NONE')}
                  />
                ) : (
                  <ViewValue>{selected?.propertiesPolicy}</ViewValue>
                )}
              </DetailField>
              {(editing ? policy === 'SCHEMA' : selected?.propertiesPolicy === 'SCHEMA') && (
                <>
                  <DetailField label="Edge schema">
                    {editing ? (
                      <Select
                        searchable
                        allowDeselect={false}
                        data={edgeSchemaOptions}
                        value={edgeSchemaKey}
                        onChange={setEdgeSchemaKey}
                      />
                    ) : (
                      <ViewValue>
                        {selected?.propertiesSchemaType
                          ? `${selected.propertiesSchemaType}@${selected.propertiesSchemaVersion}`
                          : '—'}
                      </ViewValue>
                    )}
                  </DetailField>
                  <DetailField label="Empty properties">
                    {editing ? (
                      <Checkbox
                        label="Empty props OK"
                        checked={emptyAllowed}
                        onChange={(e) => setEmptyAllowed(e.currentTarget.checked)}
                        mt={6}
                      />
                    ) : (
                      <ViewValue>{selected?.emptyPropertiesAllowed ? 'Allowed' : 'Required'}</ViewValue>
                    )}
                  </DetailField>
                </>
              )}
            </SimpleGrid>

            <DetailField label="Description">
              {editing ? (
                <Textarea
                  autosize
                  minRows={2}
                  value={description}
                  onChange={(e) => setDescription(e.currentTarget.value)}
                  {...VERB_INPUT_PROPS}
                />
              ) : (
                <ViewValue dimmed={!selected?.description}>{selected?.description || '—'}</ViewValue>
              )}
            </DetailField>

            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
              <DetailField label="Source verb">
                {editing ? (
                  <TextInput
                    description="Source → target"
                    value={sourceVerb}
                    onChange={(e) => setSourceVerb(e.currentTarget.value)}
                    {...VERB_INPUT_PROPS}
                  />
                ) : (
                  <ViewValue dimmed={!selected?.sourceVerb}>{selected?.sourceVerb || '—'}</ViewValue>
                )}
              </DetailField>
              <DetailField label="Target verb">
                {editing ? (
                  <TextInput
                    description="Target → source"
                    value={targetVerb}
                    onChange={(e) => setTargetVerb(e.currentTarget.value)}
                    {...VERB_INPUT_PROPS}
                  />
                ) : (
                  <ViewValue dimmed={!selected?.targetVerb}>{selected?.targetVerb || '—'}</ViewValue>
                )}
              </DetailField>
            </SimpleGrid>

            <DetailField label="Tags">
              {editing ? (
                <TagsInput placeholder="Add tag" value={tags} onChange={setTags} {...VERB_INPUT_PROPS} />
              ) : viewTags.length > 0 ? (
                <Group gap={6} wrap="wrap">
                  {viewTags.map((tag) => (
                    <Badge key={tag} size="sm" variant="light" radius="xl" tt="none">
                      {tag}
                    </Badge>
                  ))}
                </Group>
              ) : (
                <ViewValue dimmed>—</ViewValue>
              )}
            </DetailField>

            <DetailField label="Attributes">
              {editing ? (
                <KeyValueRowsEditor rows={attributeRows} onChange={setAttributeRows} />
              ) : viewAttributes.length > 0 ? (
                <Group gap={6} wrap="wrap">
                  {viewAttributes.map(([name, value]) => (
                    <AttributePill key={name} name={name} value={value} />
                  ))}
                </Group>
              ) : (
                <ViewValue dimmed>—</ViewValue>
              )}
            </DetailField>

            {formError && (
              <Text size="sm" c="red">
                {formError}
              </Text>
            )}
          </Stack>
        </Paper>
      )}
    </Stack>
  )
}
