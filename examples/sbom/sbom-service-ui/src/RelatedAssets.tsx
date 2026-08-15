import { ActionIcon, Anchor, Button, Group, Menu, Paper, SegmentedControl, Stack, Table, Tabs, Text, Title, Tooltip } from '@mantine/core'
import { IconArrowBackUp, IconLayoutNavbar, IconList, IconTrash } from '@tabler/icons-react'
import type { AssetRelationshipSpec, AssetView, RelationView } from './api/types'
import { DraftStatusPill } from './DraftStatusPill'
import type { DraftKind } from './bomDraft'

export type RelatedViewMode = 'list' | 'tabs'

const TYPE_COL_W = 140

function specKey(spec: AssetRelationshipSpec) {
  return `${spec.direction || 'OUT'}:${spec.role}:${spec.targetType}`
}

function specMenuLabel(spec: AssetRelationshipSpec) {
  const label = spec.label || spec.section || spec.role
  if (!spec.targetType || spec.targetType === label) return label
  return `${label} · ${spec.targetType}`
}

function ViewModeToggle({
  value,
  onChange,
}: {
  value: RelatedViewMode
  onChange: (value: RelatedViewMode) => void
}) {
  return (
    <SegmentedControl
      size="xs"
      aria-label="Related assets view"
      value={value}
      onChange={(v) => onChange((v as RelatedViewMode) || 'tabs')}
      data={[
        {
          value: 'tabs',
          label: (
            <Tooltip label="Tabs" withArrow>
              <IconLayoutNavbar size={14} />
            </Tooltip>
          ),
        },
        {
          value: 'list',
          label: (
            <Tooltip label="List" withArrow>
              <IconList size={14} />
            </Tooltip>
          ),
        },
      ]}
    />
  )
}

function SplitAction({
  label,
  specs,
  onPick,
}: {
  label: string
  specs: AssetRelationshipSpec[]
  onPick: (spec: AssetRelationshipSpec) => void
}) {
  if (specs.length === 0) return null
  return (
    <Menu position="bottom-end" withinPortal>
      <Menu.Target>
        <div style={{ display: 'flex' }}>
          <Button size="sm" variant="light" style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}>
            {label}
          </Button>
          <Button
            size="sm"
            variant="light"
            aria-label={`${label} options`}
            px="xs"
            style={{
              borderTopLeftRadius: 0,
              borderBottomLeftRadius: 0,
              borderLeft: '1px solid var(--mantine-color-default-border)',
            }}
          >
            ▾
          </Button>
        </div>
      </Menu.Target>
      <Menu.Dropdown>
        {specs.map((spec) => (
          <Menu.Item key={specKey(spec)} onClick={() => onPick(spec)}>
            {specMenuLabel(spec)}
          </Menu.Item>
        ))}
      </Menu.Dropdown>
    </Menu>
  )
}

export function RelatedAssetsBlock({
  writable,
  viewMode,
  specs,
  selectedAssetId,
  assets,
  relations,
  outgoingTab,
  incomingTab,
  onOutgoingTabChange,
  onIncomingTabChange,
  onSelectAsset,
  onAdd,
  onCreate,
  onRemove,
  onRevert,
  relationStatus,
  onViewModeChange,
}: {
  writable: boolean
  viewMode: RelatedViewMode
  specs: AssetRelationshipSpec[]
  selectedAssetId: string
  assets: AssetView[]
  relations: RelationView[]
  outgoingTab: string | null
  incomingTab: string | null
  onOutgoingTabChange: (value: string | null) => void
  onIncomingTabChange: (value: string | null) => void
  onSelectAsset: (assetId: string) => void
  onAdd: (spec: AssetRelationshipSpec) => void
  onCreate?: (spec: AssetRelationshipSpec) => void
  onRemove: (rel: RelationView) => void
  onRevert?: (rel: RelationView) => void
  relationStatus?: Map<string, DraftKind>
  onViewModeChange: (value: RelatedViewMode) => void
}) {
  const outgoing = specs.filter((s) => s.direction !== 'IN')
  const incoming = specs.filter((s) => s.direction === 'IN')
  if (outgoing.length === 0 && incoming.length === 0) {
    return (
      <Text size="sm" c="dimmed">
        {writable ? 'No relationship types for this asset.' : 'No related assets.'}
      </Text>
    )
  }
  const shared = {
    writable,
    viewMode,
    selectedAssetId,
    assets,
    relations,
    onSelectAsset,
    onAdd,
    onCreate,
    onRemove,
    onRevert,
    relationStatus,
    onViewModeChange,
  }
  return (
    <Stack gap="md">
      <RelatedDirectionGroup
        title="Related to"
        incoming={false}
        specs={outgoing}
        tab={outgoingTab}
        onTabChange={onOutgoingTabChange}
        {...shared}
      />
      <RelatedDirectionGroup
        title="Related from"
        incoming
        specs={incoming}
        tab={incomingTab}
        onTabChange={onIncomingTabChange}
        {...shared}
      />
    </Stack>
  )
}

function RelatedDirectionGroup({
  title,
  incoming,
  writable,
  viewMode,
  specs,
  selectedAssetId,
  assets,
  relations,
  tab,
  onTabChange,
  onSelectAsset,
  onAdd,
  onCreate,
  onRemove,
  onRevert,
  relationStatus,
  onViewModeChange,
}: {
  title: string
  incoming: boolean
  writable: boolean
  viewMode: RelatedViewMode
  specs: AssetRelationshipSpec[]
  selectedAssetId: string
  assets: AssetView[]
  relations: RelationView[]
  tab: string | null
  onTabChange: (value: string | null) => void
  onSelectAsset: (assetId: string) => void
  onAdd: (spec: AssetRelationshipSpec) => void
  onCreate?: (spec: AssetRelationshipSpec) => void
  onRemove: (rel: RelationView) => void
  onRevert?: (rel: RelationView) => void
  relationStatus?: Map<string, DraftKind>
  onViewModeChange: (value: RelatedViewMode) => void
}) {
  const relatedFor = (spec: AssetRelationshipSpec) =>
    relations.filter((r) => {
      if (r.role !== spec.role) return false
      if (incoming) {
        if (r.toAssetId !== selectedAssetId) return false
        const other = assets.find((x) => x.id === r.fromAssetId)
        return !spec.targetType || other?.type === spec.targetType
      }
      if (r.fromAssetId !== selectedAssetId) return false
      const other = assets.find((x) => x.id === r.toAssetId)
      return !spec.targetType || other?.type === spec.targetType
    })
  const visibleSpecs = writable ? specs : specs.filter((spec) => relatedFor(spec).length > 0)
  if (visibleSpecs.length === 0) {
    return (
      <Stack gap={6}>
        <Group justify="space-between" wrap="nowrap" align="center">
          <Group gap="sm" wrap="nowrap">
            <ViewModeToggle value={viewMode} onChange={onViewModeChange} />
            <Title order={6}>{title}</Title>
          </Group>
        </Group>
        <Text size="sm" c="dimmed">
          {writable ? 'None defined for this type.' : 'None'}
        </Text>
      </Stack>
    )
  }
  const activeTab = tab && visibleSpecs.some((s) => specKey(s) === tab) ? tab : specKey(visibleSpecs[0])

  function pick(spec: AssetRelationshipSpec, action: (spec: AssetRelationshipSpec) => void) {
    onTabChange(specKey(spec))
    action(spec)
  }

  const table = (spec: AssetRelationshipSpec) => {
    const related = relatedFor(spec)
    return (
      <Table striped highlightOnHover stickyHeader layout="fixed">
        <Table.Thead>
          <Table.Tr>
            <Table.Th w={TYPE_COL_W}>Type</Table.Th>
            <Table.Th>Name</Table.Th>
            {writable && <Table.Th w={88} />}
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {related.length === 0 ? (
            <Table.Tr>
              <Table.Td colSpan={writable ? 3 : 2}>
                <Text size="sm" c="dimmed">
                  None
                </Text>
              </Table.Td>
            </Table.Tr>
          ) : (
            related.map((r) => {
              const otherId = incoming ? r.fromAssetId : r.toAssetId
              const other = assets.find((x) => x.id === otherId)
              const status = relationStatus?.get(r.id) ?? 'unchanged'
              const deleted = status === 'deleted'
              return (
                <Table.Tr key={r.id}>
                  <Table.Td w={TYPE_COL_W}>
                    <Text size="sm" c="dimmed" truncate>
                      {other?.type || spec.targetType || '—'}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    <Anchor
                      component="button"
                      type="button"
                      size="sm"
                      fw={600}
                      style={{
                        textDecoration: deleted ? 'line-through' : undefined,
                        opacity: deleted ? 0.7 : 1,
                      }}
                      onClick={() => onSelectAsset(otherId)}
                    >
                      {other?.label || otherId}
                    </Anchor>
                  </Table.Td>
                  {writable && (
                    <Table.Td>
                      <Group gap={4} wrap="nowrap" justify="flex-end">
                        {status !== 'unchanged' && <DraftStatusPill status={status} />}
                        {status !== 'unchanged' && onRevert && (
                          <Tooltip label="Revert this relation" withArrow>
                            <ActionIcon
                              size="sm"
                              variant="subtle"
                              aria-label={`Revert ${other?.label || otherId}`}
                              onClick={() => onRevert(r)}
                            >
                              <IconArrowBackUp size={14} />
                            </ActionIcon>
                          </Tooltip>
                        )}
                        {status !== 'deleted' && (
                          <ActionIcon
                            size="sm"
                            variant="subtle"
                            color="red"
                            aria-label={`Remove ${other?.label || otherId}`}
                            onClick={() => onRemove(r)}
                          >
                            <IconTrash size={14} />
                          </ActionIcon>
                        )}
                      </Group>
                    </Table.Td>
                  )}
                </Table.Tr>
              )
            })
          )}
        </Table.Tbody>
      </Table>
    )
  }

  return (
    <Stack gap={6}>
      <Group justify="space-between" wrap="nowrap" align="center">
        <Group gap="sm" wrap="nowrap" style={{ minWidth: 0 }}>
          <ViewModeToggle value={viewMode} onChange={onViewModeChange} />
          <Title order={6}>{title}</Title>
        </Group>
        {writable && (
          <Group gap="xs" wrap="nowrap">
            {onCreate && <SplitAction label="Create" specs={specs} onPick={(spec) => pick(spec, onCreate)} />}
            <SplitAction label="Add" specs={specs} onPick={(spec) => pick(spec, onAdd)} />
          </Group>
        )}
      </Group>
      {viewMode === 'tabs' ? (
        <Tabs value={activeTab} onChange={onTabChange}>
          <Tabs.List>
            {visibleSpecs.map((spec) => (
              <Tabs.Tab key={specKey(spec)} value={specKey(spec)}>
                {spec.label || spec.section}
              </Tabs.Tab>
            ))}
          </Tabs.List>
          {visibleSpecs.map((spec) => (
            <Tabs.Panel key={specKey(spec)} value={specKey(spec)} pt="sm">
              {table(spec)}
            </Tabs.Panel>
          ))}
        </Tabs>
      ) : (
        <Stack gap="sm">
          {visibleSpecs.map((spec) => (
            <Paper key={specKey(spec)} withBorder p="sm" radius="md">
              <Text size="sm" fw={600} mb={6}>
                {spec.label || spec.section}
              </Text>
              {table(spec)}
            </Paper>
          ))}
        </Stack>
      )}
    </Stack>
  )
}
