import {
  ActionIcon,
  Badge,
  Button,
  Divider,
  Group,
  LoadingOverlay,
  Modal,
  ScrollArea,
  Stack,
  Table,
  Text,
  Title,
  Tooltip,
} from '@mantine/core'
import { IconPlus } from '@tabler/icons-react'
import { useEffect, useMemo, useState } from 'react'
import { api } from './api/client'
import type { AssetRelationshipSpec, AssetView, BoMSchema, RelationView } from './api/types'
import type { AssetPickMode } from './AddRelatedAssetsDialog'
import {
  defaultValueForSchema,
  filledIdentifiers,
  identifierPaths,
  missingCreateFields,
  SchemaInstanceForm,
} from './SchemaInstanceForm'

function jexlEscape(value: string): string {
  return value.replace(/\\/g, '\\\\').replace(/'/g, "\\'")
}

function jexlPayloadPath(path: string): string {
  return `p${path
    .split('.')
    .map((part) => `['${jexlEscape(part)}']`)
    .join('')}`
}

function identityClause(path: string, value: unknown): string {
  if (typeof value === 'boolean') return `${jexlPayloadPath(path)} == ${value}`
  if (typeof value === 'number') return `${jexlPayloadPath(path)} == ${value}`
  return `${jexlPayloadPath(path)} == '${jexlEscape(String(value).trim())}'`
}

type IdentityMatch = {
  asset: AssetView
  inApp: boolean
  alreadyRelated: boolean
}

function matchesFilledIdentity(asset: AssetView, filled: { path: string; value: unknown }[]): boolean {
  return filled.every((row) => {
    const got = row.path.split('.').reduce<unknown>((cur, part) => {
      if (cur == null || typeof cur !== 'object' || Array.isArray(cur)) return undefined
      return (cur as Record<string, unknown>)[part]
    }, asset.payload)
    return String(got ?? '').trim() === String(row.value ?? '').trim()
  })
}

export function CreateRelatedAssetDialog({
  opened,
  spec,
  mode = 'relate',
  selectedAssetId,
  assets,
  relations,
  ownerName,
  onClose,
  onUseExisting,
  onCreated,
}: {
  opened: boolean
  spec: AssetRelationshipSpec | null
  mode?: AssetPickMode
  selectedAssetId: string | null
  assets: AssetView[]
  relations: RelationView[]
  ownerName?: string
  onClose: () => void
  onUseExisting: (targets: AssetView[]) => void
  onCreated: (asset: AssetView) => void
}) {
  const [schema, setSchema] = useState<BoMSchema | null>(null)
  const [payload, setPayload] = useState<Record<string, unknown>>({})
  const [draftPayload, setDraftPayload] = useState<Record<string, unknown>>({})
  const [selectedExisting, setSelectedExisting] = useState<AssetView | null>(null)
  const [formLocked, setFormLocked] = useState(false)
  const [matches, setMatches] = useState<IdentityMatch[]>([])
  const [loadingSchema, setLoadingSchema] = useState(false)
  const [searching, setSearching] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [confirm, setConfirm] = useState<{
    title: string
    message: string
    confirmLabel: string
    onConfirm: () => void
  } | null>(null)

  useEffect(() => {
    if (!opened || !spec?.targetType) {
      setSchema(null)
      setPayload({})
      setDraftPayload({})
      setSelectedExisting(null)
      setFormLocked(false)
      setMatches([])
      setError(null)
      setConfirm(null)
      return
    }
    let cancelled = false
    setLoadingSchema(true)
    setError(null)
    void api
      .listAssetTypes()
      .then((types) => {
        const summary = types.find((t) => t.type === spec.targetType)
        if (!summary) throw new Error(`Unknown asset type: ${spec.targetType}`)
        return api.getSchema(summary.type, summary.version)
      })
      .then((next) => {
        if (cancelled) return
        setSchema(next)
        setPayload((defaultValueForSchema(next.contentSchema) as Record<string, unknown>) || {})
        setDraftPayload({})
        setSelectedExisting(null)
        setFormLocked(false)
        setMatches([])
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Could not load schema')
      })
      .finally(() => {
        if (!cancelled) setLoadingSchema(false)
      })
    return () => {
      cancelled = true
    }
  }, [opened, spec?.targetType])

  const inAppIds = useMemo(() => new Set(assets.map((a) => a.id)), [assets])

  const relatedIds = useMemo(() => {
    const ids = new Set<string>()
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
  }, [spec, selectedAssetId, relations])

  const identityKey = schema
    ? JSON.stringify(filledIdentifiers(schema.contentSchema, payload).map((r) => [r.path, r.value]))
    : ''

  useEffect(() => {
    if (!opened || !schema || !spec?.targetType) return
    if (selectedExisting && formLocked) return
    const filled = filledIdentifiers(schema.contentSchema, payload)
    if (filled.length === 0) {
      setMatches([])
      return
    }
    let cancelled = false
    const handle = window.setTimeout(async () => {
      setSearching(true)
      try {
        const expr = filled.map((row) => identityClause(row.path, row.value)).join(' && ')
        const page = await api.searchAssetsPage(
          { type: spec.targetType, objExpr: expr },
          1,
          50,
        )
        const local = assets.filter(
          (a) => a.id !== selectedAssetId && a.type === spec.targetType && matchesFilledIdentity(a, filled),
        )
        const byId = new Map<string, AssetView>()
        for (const a of page.items ?? []) {
          if (a.id !== selectedAssetId) byId.set(a.id, a)
        }
        for (const a of local) byId.set(a.id, a)
        const next: IdentityMatch[] = [...byId.values()].map((asset) => ({
          asset,
          inApp: inAppIds.has(asset.id),
          alreadyRelated:
            mode === 'add'
              ? inAppIds.has(asset.id)
              : mode === 'replace'
                ? asset.id === selectedAssetId
                : relatedIds.has(asset.id),
        }))
        next.sort((a, b) => Number(b.inApp) - Number(a.inApp) || a.asset.label.localeCompare(b.asset.label))
        if (!cancelled) setMatches(next)
      } catch {
        if (!cancelled) setMatches([])
      } finally {
        if (!cancelled) setSearching(false)
      }
    }, 300)
    return () => {
      cancelled = true
      window.clearTimeout(handle)
    }
  }, [opened, schema, spec?.targetType, identityKey, payload, assets, relatedIds, inAppIds, selectedAssetId, selectedExisting, formLocked, mode])

  const idPaths = schema ? identifierPaths(schema.contentSchema) : []
  const missing = schema ? missingCreateFields(schema.contentSchema, payload) : ['schema']

  function unselectExisting() {
    setPayload(draftPayload)
    setSelectedExisting(null)
    setFormLocked(false)
  }

  function selectExisting(row: IdentityMatch) {
    if (row.alreadyRelated) return
    if (selectedExisting?.id === row.asset.id) {
      unselectExisting()
      return
    }
    if (!selectedExisting) setDraftPayload(structuredClone(payload))
    setSelectedExisting(row.asset)
    setPayload({ ...row.asset.payload })
    setFormLocked(true)
    setError(null)
  }

  function continueEditing() {
    setFormLocked(false)
  }

  async function createAsset() {
    if (!spec?.targetType || !schema) return
    if (missing.length > 0) {
      setError(`Fill required fields: ${missing.join(', ')}`)
      return
    }
    setBusy(true)
    setError(null)
    try {
      const created = await api.createAsset({
        type: spec.targetType,
        schemaVersion: schema.version,
        payload,
        owner: ownerName,
      })
      onCreated(created)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not create asset')
    } finally {
      setBusy(false)
    }
  }

  function onCreateClick() {
    if (selectedExisting && formLocked) {
      setConfirm({
        title: 'Use existing asset?',
        message: `Do you want to use existing “${selectedExisting.label}” instead of creating a new one?`,
        confirmLabel: 'Use existing',
        onConfirm: () => onUseExisting([selectedExisting]),
      })
      return
    }
    if (missing.length > 0) {
      setError(
        idPaths.length > 0
          ? `Identity fields are required: ${missing.join(', ')}`
          : `Required fields: ${missing.join(', ')}`,
      )
      return
    }
    if (matches.length > 0) {
      const inApp = matches.filter((m) => m.inApp).length
      setConfirm({
        title: 'Existing assets found',
        message:
          inApp > 0
            ? `${inApp} matching asset${inApp === 1 ? ' is' : 's are'} already in this application. Do you still want to create a duplicate? Otherwise select an existing asset from the list.`
            : 'There are existing assets that match these identifiers. Do you still want to create a duplicate? Otherwise select an existing asset from the list.',
        confirmLabel: 'Create duplicate',
        onConfirm: () => void createAsset(),
      })
      return
    }
    void createAsset()
  }

  return (
    <>
      <Modal
        opened={opened}
        onClose={onClose}
        title={
          mode === 'replace'
            ? `Replace with ${spec?.label || spec?.targetType || 'asset'}`
            : spec
              ? `Create ${spec.label}`
              : 'Create asset'
        }
        centered
        size="95%"
        styles={{ content: { minHeight: '85vh' } }}
      >
        <div style={{ position: 'relative' }}>
          <LoadingOverlay visible={loadingSchema} />
          <Group align="stretch" gap="md" wrap="nowrap">
            <Stack gap="xs" style={{ flex: 1, minWidth: 0 }}>
              {error && (
                <Text size="sm" c="red">
                  {error}
                </Text>
              )}
              {schema ? (
                <ScrollArea h="calc(85vh - 160px)" type="auto">
                  <SchemaInstanceForm
                    schema={schema.contentSchema}
                    value={payload}
                    onChange={setPayload}
                    readOnly={formLocked}
                  />
                </ScrollArea>
              ) : (
                <Text size="sm" c="dimmed">
                  Loading schema…
                </Text>
              )}
            </Stack>
            {idPaths.length > 0 && (
              <>
                <Divider orientation="vertical" />
                <Stack gap="xs" w={320} style={{ flexShrink: 0, position: 'relative' }}>
                  <LoadingOverlay visible={searching} />
                  <Title order={6}>Existing ({matches.length})</Title>
                  <Text size="xs" c="dimmed">
                    Registry and application matches for the identity fields you have filled.
                  </Text>
                  {matches.length === 0 ? (
                    <Text size="sm" c="dimmed">
                      No existing assets yet.
                    </Text>
                  ) : (
                    <ScrollArea h="calc(85vh - 220px)" type="auto">
                      <Table striped highlightOnHover layout="fixed" withRowBorders={false}>
                        <Table.Tbody>
                          {matches.map((row) => {
                            const selected = selectedExisting?.id === row.asset.id
                            return (
                            <Table.Tr
                              key={row.asset.id}
                              style={{
                                cursor: row.alreadyRelated ? 'default' : 'pointer',
                                opacity: row.alreadyRelated ? 0.65 : 1,
                                background: selected ? 'var(--mantine-color-blue-light)' : undefined,
                              }}
                              onClick={() => selectExisting(row)}
                            >
                              <Table.Td>
                                <Text size="sm" fw={600} truncate>
                                  {row.asset.label}
                                </Text>
                                <Group gap={4} mt={2}>
                                  {row.inApp && (
                                    <Badge size="xs" variant="light">
                                      In app
                                    </Badge>
                                  )}
                                  {row.alreadyRelated && (
                                    <Badge size="xs" variant="light" color="gray">
                                      Related
                                    </Badge>
                                  )}
                                  {selected && (
                                    <Badge size="xs" variant="filled">
                                      Selected
                                    </Badge>
                                  )}
                                </Group>
                              </Table.Td>
                              <Table.Td w={36}>
                                {!row.alreadyRelated && (
                                  <Tooltip label={selected ? 'Unselect' : row.inApp ? 'Use from application' : 'Use existing'} withArrow>
                                    <ActionIcon
                                      size="sm"
                                      variant="subtle"
                                      onClick={(e) => {
                                        e.stopPropagation()
                                        selectExisting(row)
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
                  )}
                </Stack>
              </>
            )}
          </Group>
          <Group justify="space-between" mt="md">
            <Group gap="xs">
              {selectedExisting && formLocked && (
                <Button variant="light" onClick={continueEditing}>
                  Continue editing
                </Button>
              )}
            </Group>
            <Group gap="xs">
              <Button variant="default" onClick={onClose}>
                Cancel
              </Button>
              <Button disabled={busy || !schema} onClick={onCreateClick}>
                {selectedExisting && formLocked
                  ? mode === 'replace'
                    ? 'Replace with existing'
                    : 'Add existing'
                  : mode === 'replace'
                    ? 'Create replacement'
                    : 'Create'}
              </Button>
            </Group>
          </Group>
        </div>
      </Modal>
      <Modal opened={!!confirm} onClose={() => setConfirm(null)} title={confirm?.title} centered>
        <Stack>
          <Text size="sm">{confirm?.message}</Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setConfirm(null)}>
              Cancel
            </Button>
            <Button
              onClick={() => {
                const action = confirm?.onConfirm
                setConfirm(null)
                action?.()
              }}
            >
              {confirm?.confirmLabel || 'Confirm'}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </>
  )
}
