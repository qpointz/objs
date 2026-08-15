import {
  Alert,
  Anchor,
  Button,
  Group,
  Modal,
  Pagination,
  Select,
  Stack,
  Table,
  Text,
  Title,
} from '@mantine/core'
import { useEffect, useMemo, useState } from 'react'
import { Link, useBlocker, useLocation, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { SchemaPayloadView } from '../SchemaPayloadView'
import { SearchInput } from '../SearchInput'
import type { AssetDetailView, AssetUsageEntry, BoMSchema } from '../api/types'

const PAGE_SIZE_OPTIONS = ['10', '20', '50']
const DEFAULT_PAGE_SIZE = 10

function versionCell(u: AssetUsageEntry): string {
  if (u.context === 'DRAFT') return 'Draft'
  return u.versionLabel || u.versionId || u.context
}

function applicationHref(u: AssetUsageEntry, assetId: string): string {
  const q = new URLSearchParams()
  q.set('tab', 'assets')
  q.set('asset', assetId)
  if (u.versionId) q.set('version', u.versionId)
  return `/applications/${u.applicationId}?${q.toString()}`
}

export function AssetDetailPage() {
  const { assetId = '' } = useParams()
  const location = useLocation()
  const [detail, setDetail] = useState<AssetDetailView | null>(null)
  const [schema, setSchema] = useState<BoMSchema | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [usageSearch, setUsageSearch] = useState('')
  const [usagePage, setUsagePage] = useState(1)
  const [usagePageSize, setUsagePageSize] = useState(DEFAULT_PAGE_SIZE)
  const [editPayload, setEditPayload] = useState<Record<string, unknown> | null>(null)
  const [saving, setSaving] = useState(false)
  const [editing, setEditing] = useState(false)

  const payloadDirty =
    detail != null &&
    editPayload != null &&
    JSON.stringify(editPayload) !== JSON.stringify(detail.asset.payload)
  const leaveBlocked = editing && payloadDirty

  const blocker = useBlocker(leaveBlocked)

  useEffect(() => {
    const onBeforeUnload = (e: BeforeUnloadEvent) => {
      if (!leaveBlocked) return
      e.preventDefault()
      e.returnValue = ''
    }
    window.addEventListener('beforeunload', onBeforeUnload)
    return () => window.removeEventListener('beforeunload', onBeforeUnload)
  }, [leaveBlocked])

  async function load() {
    setError(null)
    try {
      const d = await api.getAsset(assetId)
      setDetail(d)
      setEditPayload({ ...d.asset.payload })
      setEditing(false)
      try {
        setSchema(await api.getSchema(d.asset.type, d.asset.schemaVersion))
      } catch {
        setSchema(null)
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not load asset')
    }
  }

  useEffect(() => {
    void load()
    setUsageSearch('')
    setUsagePage(1)
    setEditing(false)
  }, [assetId])

  const usageRows = useMemo(() => {
    const q = usageSearch.trim().toLowerCase()
    const rows = detail?.usage ?? []
    const filtered = q
      ? rows.filter((u) => u.applicationName.toLowerCase().includes(q))
      : rows
    return [...filtered].sort((a, b) => a.applicationName.localeCompare(b.applicationName))
  }, [detail?.usage, usageSearch])

  const usagePageCount = Math.max(1, Math.ceil(usageRows.length / usagePageSize))
  const safeUsagePage = Math.min(usagePage, usagePageCount)
  const pagedUsage = usageRows.slice((safeUsagePage - 1) * usagePageSize, safeUsagePage * usagePageSize)

  function typeListTo(type: string): string {
    const q = new URLSearchParams(location.search)
    q.set('type', type)
    return `/applications/assets?${q.toString()}`
  }

  const leaveModal = (
    <Modal
      opened={blocker.state === 'blocked'}
      onClose={() => blocker.reset?.()}
      title="Unsaved changes"
      centered
    >
      <Stack>
        <Text size="sm">You have unsaved changes. Leave this asset and discard them?</Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={() => blocker.reset?.()}>
            Stay
          </Button>
          <Button color="red" onClick={() => blocker.proceed?.()}>
            Discard and leave
          </Button>
        </Group>
      </Stack>
    </Modal>
  )

  if (!detail) {
    return (
      <Stack gap="md" style={{ overflow: 'auto', flex: 1, minHeight: 0 }}>
        <Text c={error ? 'red' : 'dimmed'}>{error ?? 'Loading…'}</Text>
        {leaveModal}
      </Stack>
    )
  }

  const { asset } = detail

  async function savePayload() {
    if (!editPayload || !detail) return
    setSaving(true)
    setError(null)
    try {
      const updated = await api.updateAsset(asset.id, editPayload)
      setDetail({ asset: updated, usage: detail.usage ?? [] })
      setEditPayload({ ...updated.payload })
      setEditing(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not save asset')
    } finally {
      setSaving(false)
    }
  }

  function discardPayload() {
    setEditPayload({ ...asset.payload })
    setEditing(false)
  }

  return (
    <Stack gap="md" style={{ overflow: 'auto', flex: 1, minHeight: 0 }}>
      <Group justify="space-between" align="center" wrap="nowrap" gap="md">
        <Group gap="xs" wrap="nowrap" style={{ minWidth: 0, flex: 1 }}>
          <Anchor component={Link} to={typeListTo(asset.type)} size="sm">
            {asset.type}
          </Anchor>
          <Text size="sm" c="dimmed">
            /
          </Text>
          <Title order={3} lineClamp={1} style={{ minWidth: 0 }}>
            {asset.label}
          </Title>
          <Text size="sm" c="dimmed" style={{ marginLeft: 'auto', flexShrink: 0 }}>
            schema:{' '}
            <Anchor
              component={Link}
              to={`/schemas/${encodeURIComponent(asset.type)}/${encodeURIComponent(asset.schemaVersion)}`}
            >
              {asset.schemaVersion}
            </Anchor>
          </Text>
        </Group>
        <Group gap="xs" wrap="nowrap" style={{ flexShrink: 0 }}>
          {!editing ? (
            <Button size="sm" onClick={() => setEditing(true)}>
              Edit
            </Button>
          ) : (
            <>
              <Button size="sm" disabled={!payloadDirty} loading={saving} onClick={() => void savePayload()}>
                Save
              </Button>
              <Button size="sm" variant="default" disabled={saving} onClick={discardPayload}>
                Discard
              </Button>
            </>
          )}
        </Group>
      </Group>
      {error && <Alert color="red">{error}</Alert>}

      {schema && editPayload ? (
        <SchemaPayloadView
          schema={schema.contentSchema}
          value={editPayload}
          editable={editing}
          onChange={editing ? setEditPayload : undefined}
        />
      ) : (
        <Text size="sm" c="dimmed">
          Schema fields are unavailable.
        </Text>
      )}

      <Title order={4}>Used by</Title>
      {detail.usage.length === 0 ? (
        <Text c="dimmed">Not used in any application draft or version yet.</Text>
      ) : (
        <Stack gap="xs">
          <SearchInput
            size="sm"
            placeholder="Filter by application"
            value={usageSearch}
            onValueChange={(v) => {
              setUsageSearch(v)
              setUsagePage(1)
            }}
          />
          {usageRows.length === 0 ? (
            <Text size="sm" c="dimmed">
              No matching applications.
            </Text>
          ) : (
            <>
              <Table striped highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Application</Table.Th>
                    <Table.Th>Application version</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {pagedUsage.map((u, i) => (
                    <Table.Tr key={`${u.applicationId}-${u.versionId ?? u.context}-${i}`}>
                      <Table.Td>
                        <Anchor component={Link} to={applicationHref(u, asset.id)} size="sm">
                          {u.applicationName}
                        </Anchor>
                      </Table.Td>
                      <Table.Td>
                        <Text size="sm">{versionCell(u)}</Text>
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
              <Group justify="space-between" wrap="wrap">
                <Text size="xs" c="dimmed">
                  {usageRows.length} application{usageRows.length === 1 ? '' : 's'}
                </Text>
                <Group gap="xs">
                  <Select
                    size="xs"
                    w={80}
                    aria-label="Page size"
                    value={String(usagePageSize)}
                    data={PAGE_SIZE_OPTIONS}
                    onChange={(v) => {
                      if (!v) return
                      setUsagePageSize(Number(v))
                      setUsagePage(1)
                    }}
                  />
                  <Pagination size="xs" value={safeUsagePage} onChange={setUsagePage} total={usagePageCount} />
                </Group>
              </Group>
            </>
          )}
        </Stack>
      )}
      {leaveModal}
    </Stack>
  )
}
