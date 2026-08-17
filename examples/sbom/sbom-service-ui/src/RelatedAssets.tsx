import { ActionIcon, Anchor, Button, Group, Menu, Paper, SegmentedControl, Stack, Table, Tabs, Text, Title, Tooltip } from '@mantine/core'
import { IconArrowBackUp, IconLayoutNavbar, IconList, IconTrash } from '@tabler/icons-react'
import type { AssetRelationshipSpec, AssetView, RelationView } from './api/types'
import { DraftStatusPill } from './DraftStatusPill'
import type { DraftKind } from './bomDraft'

export type RelatedViewMode = 'list' | 'tabs'

const ACTIONS_COL_W = 88
const EMPTY_COLSPAN = (writable: boolean) => (writable ? 4 : 3)
const SOURCE_MAX_CH = 48
const TYPE_MAX_CH = 72

type RelColWidths = {
  source: number
  type: number
}

function specKey(spec: AssetRelationshipSpec) {
  return `${spec.direction || 'OUT'}:${spec.role}:${spec.targetType}`
}

function assetDisplayName(asset: AssetView | undefined, id: string) {
  return asset?.label || id
}

function specTypeLabel(spec: AssetRelationshipSpec) {
  return spec.label || spec.section || spec.role
}

function relatedForSpec(
  spec: AssetRelationshipSpec,
  incoming: boolean,
  selectedAssetId: string,
  assets: AssetView[],
  relations: RelationView[],
) {
  return relations.filter((r) => {
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
}

function measureRelColWidths(
  outgoing: AssetRelationshipSpec[],
  incoming: AssetRelationshipSpec[],
  selectedAssetId: string,
  assets: AssetView[],
  relations: RelationView[],
): RelColWidths {
  const selectedLabel = assetDisplayName(
    assets.find((x) => x.id === selectedAssetId),
    selectedAssetId,
  )
  let maxSource = 'Source'.length
  let maxType = 'Type'.length

  for (const spec of outgoing) {
    const typeFallback = specTypeLabel(spec)
    maxType = Math.max(maxType, typeFallback.length)
    for (const r of relatedForSpec(spec, false, selectedAssetId, assets, relations)) {
      maxSource = Math.max(maxSource, selectedLabel.length)
      maxType = Math.max(maxType, (r.label || typeFallback).length)
    }
  }
  for (const spec of incoming) {
    const typeFallback = specTypeLabel(spec)
    maxType = Math.max(maxType, typeFallback.length)
    for (const r of relatedForSpec(spec, true, selectedAssetId, assets, relations)) {
      const otherId = r.fromAssetId
      maxSource = Math.max(maxSource, assetDisplayName(assets.find((x) => x.id === otherId), otherId).length)
      maxType = Math.max(maxType, (r.label || typeFallback).length)
    }
  }

  return {
    source: Math.min(Math.max(Math.ceil(maxSource * 1.3) + 1, 7), SOURCE_MAX_CH),
    type: Math.min(Math.max(maxType * 3 + 1, 5), TYPE_MAX_CH),
  }
}

function RelAssetText({
  label,
  deleted,
}: {
  label: string
  deleted?: boolean
}) {
  return (
    <Text
      size="sm"
      truncate
      style={{
        textDecoration: deleted ? 'line-through' : undefined,
        opacity: deleted ? 0.7 : 1,
      }}
    >
      {label}
    </Text>
  )
}

function RelAssetLink({
  label,
  deleted,
  onClick,
}: {
  label: string
  deleted?: boolean
  onClick: () => void
}) {
  return (
    <Anchor
      component="button"
      type="button"
      size="sm"
      fw={600}
      style={{
        textDecoration: deleted ? 'line-through' : undefined,
        opacity: deleted ? 0.7 : 1,
      }}
      onClick={onClick}
    >
      {label}
    </Anchor>
  )
}

function RelationTableCols({ writable, cols }: { writable: boolean; cols: RelColWidths }) {
  return (
    <colgroup>
      <col style={{ width: `${cols.source}ch` }} />
      <col style={{ width: `${cols.type}ch` }} />
      <col />
      {writable && <col style={{ width: ACTIONS_COL_W }} />}
    </colgroup>
  )
}

function RelationTableHead({ writable }: { writable: boolean }) {
  return (
    <Table.Thead>
      <Table.Tr>
        <Table.Th style={{ whiteSpace: 'nowrap' }}>Source</Table.Th>
        <Table.Th style={{ whiteSpace: 'nowrap' }}>Type</Table.Th>
        <Table.Th>Target</Table.Th>
        {writable && <Table.Th w={ACTIONS_COL_W} />}
      </Table.Tr>
    </Table.Thead>
  )
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
  const cols = measureRelColWidths(outgoing, incoming, selectedAssetId, assets, relations)
  const shared = {
    writable,
    viewMode,
    selectedAssetId,
    assets,
    relations,
    cols,
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
  cols,
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
  cols: RelColWidths
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
    relatedForSpec(spec, incoming, selectedAssetId, assets, relations)
  // No relationship types in this direction → same empty section, grayed out.
  if (specs.length === 0) {
    return (
      <Stack gap={6} style={{ opacity: 0.45, pointerEvents: 'none' }} aria-disabled>
        <Group justify="space-between" wrap="nowrap" align="center">
          <Group gap="sm" wrap="nowrap">
            <ViewModeToggle value={viewMode} onChange={onViewModeChange} />
            <Title order={6}>{title}</Title>
          </Group>
        </Group>
        <Table striped highlightOnHover stickyHeader layout="fixed">
          <RelationTableCols writable={writable} cols={cols} />
          <RelationTableHead writable={writable} />
          <Table.Tbody>
            <Table.Tr>
              <Table.Td colSpan={EMPTY_COLSPAN(writable)}>
                <Text size="sm" c="dimmed">
                  None
                </Text>
              </Table.Td>
            </Table.Tr>
          </Table.Tbody>
        </Table>
      </Stack>
    )
  }
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
          None
        </Text>
      </Stack>
    )
  }
  const activeTab = tab && visibleSpecs.some((s) => specKey(s) === tab) ? tab : specKey(visibleSpecs[0])
  const selectedAsset = assets.find((x) => x.id === selectedAssetId)
  const selectedLabel = assetDisplayName(selectedAsset, selectedAssetId)

  function pick(spec: AssetRelationshipSpec, action: (spec: AssetRelationshipSpec) => void) {
    onTabChange(specKey(spec))
    action(spec)
  }

  const table = (spec: AssetRelationshipSpec) => {
    const related = relatedFor(spec)
    const relTypeLabel = specTypeLabel(spec)
    return (
      <Table striped highlightOnHover stickyHeader layout="fixed">
        <RelationTableCols writable={writable} cols={cols} />
        <RelationTableHead writable={writable} />
        <Table.Tbody>
          {related.length === 0 ? (
            <Table.Tr>
              <Table.Td colSpan={EMPTY_COLSPAN(writable)}>
                <Text size="sm" c="dimmed">
                  None
                </Text>
              </Table.Td>
            </Table.Tr>
          ) : (
            related.map((r) => {
              const otherId = incoming ? r.fromAssetId : r.toAssetId
              const other = assets.find((x) => x.id === otherId)
              const otherLabel = assetDisplayName(other, otherId)
              const status = relationStatus?.get(r.id) ?? 'unchanged'
              const deleted = status === 'deleted'
              const typeLabel = r.label || relTypeLabel
              const source = incoming ? (
                <RelAssetLink label={otherLabel} deleted={deleted} onClick={() => onSelectAsset(otherId)} />
              ) : (
                <RelAssetText label={selectedLabel} deleted={deleted} />
              )
              const target = incoming ? (
                <RelAssetText label={selectedLabel} deleted={deleted} />
              ) : (
                <RelAssetLink label={otherLabel} deleted={deleted} onClick={() => onSelectAsset(otherId)} />
              )
              return (
                <Table.Tr key={r.id}>
                  <Table.Td style={{ whiteSpace: 'nowrap' }}>{source}</Table.Td>
                  <Table.Td style={{ whiteSpace: 'nowrap' }}>
                    <Text
                      size="sm"
                      c="dimmed"
                      truncate
                      style={{
                        textDecoration: deleted ? 'line-through' : undefined,
                        opacity: deleted ? 0.7 : 1,
                      }}
                    >
                      {typeLabel}
                    </Text>
                  </Table.Td>
                  <Table.Td style={{ minWidth: 0 }}>{target}</Table.Td>
                  {writable && (
                    <Table.Td>
                      <Group gap={4} wrap="nowrap" justify="flex-end">
                        {status !== 'unchanged' && <DraftStatusPill status={status} />}
                        {status !== 'unchanged' && onRevert && (
                          <Tooltip label="Revert this relation" withArrow>
                            <ActionIcon
                              size="sm"
                              variant="subtle"
                              aria-label={`Revert ${otherLabel}`}
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
                            aria-label={`Remove ${otherLabel}`}
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
