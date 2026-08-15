import {
  ActionIcon,
  Button,
  Divider,
  Group,
  LoadingOverlay,
  Modal,
  Pagination,
  ScrollArea,
  Stack,
  Table,
  Tabs,
  Text,
  Textarea,
  Title,
  Tooltip,
} from '@mantine/core'
import { IconPlus, IconX } from '@tabler/icons-react'
import type { ReactNode } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { api } from './api/client'
import type { AssetRelationshipSpec, AssetView, RelationView } from './api/types'
import { SearchInput } from './SearchInput'

type SourceTab = 'application' | 'registry' | 'expression'

const REGISTRY_PAGE_SIZE = 20

export type AssetPickMode = 'relate' | 'add' | 'replace'

export function AddRelatedAssetsDialog({
  opened,
  spec,
  mode = 'relate',
  selectedAssetId,
  assets,
  relations,
  onClose,
  onAdd,
}: {
  opened: boolean
  spec: AssetRelationshipSpec | null
  mode?: AssetPickMode
  selectedAssetId: string | null
  assets: AssetView[]
  relations: RelationView[]
  onClose: () => void
  onAdd: (targets: AssetView[]) => void
}) {
  const [sourceTab, setSourceTab] = useState<SourceTab>('application')
  const [appQuery, setAppQuery] = useState('')
  const [registryQuery, setRegistryQuery] = useState('')
  const [registryHits, setRegistryHits] = useState<AssetView[]>([])
  const [registryTotal, setRegistryTotal] = useState(0)
  const [registryPage, setRegistryPage] = useState(1)
  const [registryLoading, setRegistryLoading] = useState(false)
  const [registryError, setRegistryError] = useState<string | null>(null)
  const [exprQuery, setExprQuery] = useState('')
  const [exprHits, setExprHits] = useState<AssetView[]>([])
  const [exprTotal, setExprTotal] = useState(0)
  const [exprPage, setExprPage] = useState(1)
  const [exprLoading, setExprLoading] = useState(false)
  const [exprError, setExprError] = useState<string | null>(null)
  const [shelf, setShelf] = useState<AssetView[]>([])
  const targetType = spec?.targetType || undefined

  useEffect(() => {
    if (!opened) return
    setSourceTab('application')
    setAppQuery('')
    setRegistryQuery('')
    setRegistryHits([])
    setRegistryTotal(0)
    setRegistryPage(1)
    setRegistryError(null)
    setExprQuery('')
    setExprHits([])
    setExprTotal(0)
    setExprPage(1)
    setExprError(null)
    setShelf([])
  }, [opened, spec?.role, spec?.targetType, spec?.direction, mode])

  const inAppIds = useMemo(() => new Set(assets.map((a) => a.id)), [assets])

  const relatedIds = useMemo(() => {
    const ids = new Set<string>()
    if (mode === 'add') {
      inAppIds.forEach((id) => ids.add(id))
      return ids
    }
    if (mode === 'replace') {
      if (selectedAssetId) ids.add(selectedAssetId)
      return ids
    }
    if (!spec || !selectedAssetId) return ids
    ids.add(selectedAssetId)
    for (const rel of relations) {
      if (rel.role !== spec.role) continue
      if (spec.direction === 'IN') {
        if (rel.toAssetId === selectedAssetId) ids.add(rel.fromAssetId)
      } else if (rel.fromAssetId === selectedAssetId) {
        ids.add(rel.toAssetId)
      }
    }
    return ids
  }, [mode, spec, selectedAssetId, relations, inAppIds])

  const shelfIds = useMemo(() => new Set(shelf.map((a) => a.id)), [shelf])

  const inAppHits = useMemo(() => {
    if (!targetType) return []
    const q = appQuery.trim().toLowerCase()
    return assets.filter((a) => {
      if (relatedIds.has(a.id) || shelfIds.has(a.id)) return false
      if (targetType && a.type !== targetType) return false
      if (!q) return true
      return `${a.label} ${a.type}`.toLowerCase().includes(q)
    })
  }, [assets, targetType, appQuery, relatedIds, shelfIds])

  useEffect(() => {
    if (!opened) return
    let cancelled = false
    const q = registryQuery.trim()
    const delay = q ? 250 : 0
    const handle = window.setTimeout(async () => {
      setRegistryLoading(true)
      setRegistryError(null)
      try {
        const body: { type?: string; objExpr?: string } = {}
        if (targetType) body.type = targetType
        if (q) body.objExpr = nameContainsExpr(q)
        const page = await api.searchAssetsPage(body, registryPage, REGISTRY_PAGE_SIZE)
        if (!cancelled) {
          setRegistryHits(Array.isArray(page.items) ? page.items : [])
          setRegistryTotal(typeof page.total === 'number' ? page.total : 0)
        }
      } catch (e) {
        if (!cancelled) {
          setRegistryHits([])
          setRegistryTotal(0)
          setRegistryError(e instanceof Error ? e.message : 'Search failed')
        }
      } finally {
        if (!cancelled) setRegistryLoading(false)
      }
    }, delay)
    return () => {
      cancelled = true
      window.clearTimeout(handle)
    }
  }, [opened, targetType, registryQuery, registryPage])

  useEffect(() => {
    if (!opened) return
    const expr = exprQuery.trim()
    if (!expr) {
      setExprHits([])
      setExprTotal(0)
      setExprError(null)
      setExprLoading(false)
      return
    }
    let cancelled = false
    const handle = window.setTimeout(async () => {
      setExprLoading(true)
      setExprError(null)
      try {
        const page = await api.searchAssetsPage(
          {
            type: targetType,
            objExpr: expr,
          },
          exprPage,
          REGISTRY_PAGE_SIZE,
        )
        if (!cancelled) {
          setExprHits(Array.isArray(page.items) ? page.items : [])
          setExprTotal(typeof page.total === 'number' ? page.total : 0)
        }
      } catch (e) {
        if (!cancelled) {
          setExprHits([])
          setExprTotal(0)
          setExprError(e instanceof Error ? e.message : 'Search failed')
        }
      } finally {
        if (!cancelled) setExprLoading(false)
      }
    }, 400)
    return () => {
      cancelled = true
      window.clearTimeout(handle)
    }
  }, [opened, targetType, exprQuery, exprPage])

  const registryRows = useMemo(
    () => registryHits.filter((a) => !relatedIds.has(a.id)),
    [registryHits, relatedIds],
  )
  const registryPageCount = Math.max(1, Math.ceil(registryTotal / REGISTRY_PAGE_SIZE))
  const exprRows = useMemo(
    () => exprHits.filter((a) => !relatedIds.has(a.id)),
    [exprHits, relatedIds],
  )
  const exprPageCount = Math.max(1, Math.ceil(exprTotal / REGISTRY_PAGE_SIZE))

  function addToShelf(asset: AssetView) {
    setShelf((prev) => {
      if (mode === 'replace') return [asset]
      return prev.some((x) => x.id === asset.id) ? prev : [...prev, asset]
    })
  }

  function removeFromShelf(assetId: string) {
    setShelf((prev) => prev.filter((a) => a.id !== assetId))
  }

  function submit() {
    if (shelf.length === 0) return
    onAdd(shelf)
  }

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={mode === 'replace' ? `Replace ${spec?.label || targetType || 'asset'}` : spec ? `Add ${spec.label}` : `Add ${targetType || 'asset'}`}
      centered
      size="80%"
    >
      <Group align="stretch" gap="md" wrap="nowrap">
        <Stack gap="sm" style={{ flex: 1, minWidth: 0 }}>
          <Tabs value={sourceTab} onChange={(v) => setSourceTab((v as SourceTab) || 'application')}>
            <Tabs.List>
              <Tabs.Tab value="application">Application</Tabs.Tab>
              <Tabs.Tab value="registry">Registry</Tabs.Tab>
              <Tabs.Tab value="expression">Expression</Tabs.Tab>
            </Tabs.List>
            <Tabs.Panel value="application" pt="sm">
              <Stack gap="xs">
                <SearchInput
                  placeholder="Filter by name"
                  value={appQuery}
                  onValueChange={setAppQuery}
                />
                <AssetPickList assets={inAppHits} empty="No matching assets." onAdd={addToShelf} />
              </Stack>
            </Tabs.Panel>
            <Tabs.Panel value="registry" pt="sm">
              <PagedRegistryPanel
                query={
                  <SearchInput
                    placeholder="Search by name"
                    value={registryQuery}
                    onValueChange={(v) => {
                      setRegistryQuery(v)
                      setRegistryPage(1)
                    }}
                  />
                }
                loading={registryLoading}
                error={registryError}
                rows={registryRows}
                total={registryTotal}
                page={registryPage}
                pageCount={registryPageCount}
                onPage={setRegistryPage}
                selectedIds={shelfIds}
                onAdd={addToShelf}
              />
            </Tabs.Panel>
            <Tabs.Panel value="expression" pt="sm">
              <PagedRegistryPanel
                query={
                  <Textarea
                    size="sm"
                    placeholder="p.ecosystem == 'Maven'"
                    description="obj-expr over the asset pool"
                    autosize
                    minRows={2}
                    maxRows={6}
                    value={exprQuery}
                    onChange={(e) => {
                      setExprQuery(e.currentTarget.value)
                      setExprPage(1)
                    }}
                  />
                }
                loading={exprLoading}
                error={exprError}
                rows={exprRows}
                total={exprTotal}
                page={exprPage}
                pageCount={exprPageCount}
                onPage={setExprPage}
                selectedIds={shelfIds}
                onAdd={addToShelf}
                empty={exprQuery.trim() ? 'No matching assets.' : 'Enter an obj-expr to search.'}
              />
            </Tabs.Panel>
          </Tabs>
        </Stack>
        <Divider orientation="vertical" />
        <Stack gap="xs" w={280} style={{ flexShrink: 0 }}>
          <Title order={6}>Selected ({shelf.length})</Title>
          <Text size="xs" c="dimmed">
            Pick from Application, Registry, or Expression, then add together.
          </Text>
          {shelf.length === 0 ? (
            <Text size="sm" c="dimmed">
              Nothing selected yet.
            </Text>
          ) : (
            <ScrollArea h={360} type="auto">
              <Table striped highlightOnHover layout="fixed" withRowBorders={false}>
                <Table.Tbody>
                  {shelf.map((a) => (
                    <Table.Tr key={a.id}>
                      <Table.Td>
                        <Text size="sm" fw={600} truncate>
                          {a.label}
                        </Text>
                        <Text size="xs" c="dimmed" truncate>
                          {a.type}
                        </Text>
                      </Table.Td>
                      <Table.Td w={36}>
                        <Tooltip label="Remove" withArrow>
                          <ActionIcon size="sm" variant="subtle" color="gray" onClick={() => removeFromShelf(a.id)}>
                            <IconX size={14} />
                          </ActionIcon>
                        </Tooltip>
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </ScrollArea>
          )}
        </Stack>
      </Group>
      <Group justify="flex-end" mt="md">
        <Button variant="default" onClick={onClose}>
          Cancel
        </Button>
        <Button disabled={shelf.length === 0} onClick={submit}>
          {mode === 'replace' ? 'Replace' : `Add${shelf.length ? ` (${shelf.length})` : ''}`}
        </Button>
      </Group>
    </Modal>
  )
}

function nameContainsExpr(q: string): string {
  const regex = q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/'/g, "\\'")
  return `p.name =~ '(?i)${regex}'`
}

function PagedRegistryPanel({
  query,
  loading,
  error,
  rows,
  total,
  page,
  pageCount,
  onPage,
  selectedIds,
  onAdd,
  empty,
}: {
  query: ReactNode
  loading: boolean
  error: string | null
  rows: AssetView[]
  total: number
  page: number
  pageCount: number
  onPage: (page: number) => void
  selectedIds: Set<string>
  onAdd: (asset: AssetView) => void
  empty?: string
}) {
  return (
    <Stack gap="xs" style={{ position: 'relative' }}>
      <LoadingOverlay visible={loading} />
      {query}
      {error ? (
        <Text size="sm" c="red">
          {error}
        </Text>
      ) : (
        <AssetPickList
          assets={rows}
          empty={loading ? 'Searching…' : empty || 'No matching assets.'}
          selectedIds={selectedIds}
          onAdd={onAdd}
        />
      )}
      <Group justify="space-between" wrap="nowrap">
        <Text size="xs" c="dimmed">
          {total === 0
            ? 'No results'
            : `${(page - 1) * REGISTRY_PAGE_SIZE + 1}–${Math.min(page * REGISTRY_PAGE_SIZE, total)} of ${total}`}
        </Text>
        <Pagination size="xs" total={pageCount} value={page} onChange={onPage} />
      </Group>
    </Stack>
  )
}

function AssetPickList({
  assets,
  empty,
  selectedIds,
  onAdd,
}: {
  assets: AssetView[]
  empty: string
  selectedIds?: Set<string>
  onAdd: (asset: AssetView) => void
}) {
  if (assets.length === 0) {
    return (
      <Text size="sm" c="dimmed">
        {empty}
      </Text>
    )
  }
  return (
    <ScrollArea h={360} type="auto">
      <Table striped highlightOnHover layout="fixed" withRowBorders={false}>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Name</Table.Th>
            <Table.Th w={140}>Type</Table.Th>
            <Table.Th w={36} />
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {assets.map((a) => {
            const selected = selectedIds?.has(a.id)
            return (
              <Table.Tr
                key={a.id}
                style={{ cursor: selected ? 'default' : 'pointer', opacity: selected ? 0.55 : 1 }}
                onClick={() => {
                  if (!selected) onAdd(a)
                }}
              >
                <Table.Td>
                  <Text size="sm" fw={600} truncate>
                    {a.label}
                  </Text>
                </Table.Td>
                <Table.Td>
                  <Text size="sm" c="dimmed" truncate>
                    {a.type}
                  </Text>
                </Table.Td>
                <Table.Td>
                  {!selected && (
                    <Tooltip label="Add to selected" withArrow>
                      <ActionIcon
                        size="sm"
                        variant="subtle"
                        onClick={(e) => {
                          e.stopPropagation()
                          onAdd(a)
                        }}
                      >
                        <IconPlus size={14} />
                      </ActionIcon>
                    </Tooltip>
                  )}
                </Table.Td>
              </Table.Tr>
            )
          })}
        </Table.Tbody>
      </Table>
    </ScrollArea>
  )
}
