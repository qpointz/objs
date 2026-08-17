import {
  ActionIcon,
  Alert,
  Anchor,
  Badge,
  Button,
  Checkbox,
  Divider,
  Group,
  LoadingOverlay,
  Menu,
  Modal,
  Paper,
  ScrollArea,
  SegmentedControl,
  Select,
  Stack,
  Switch,
  Table,
  Tabs,
  TagsInput,
  Text,
  Textarea,
  TextInput,
  Title,
  Tooltip,
  UnstyledButton,
} from '@mantine/core'
import { IconArrowBackUp, IconChevronDown, IconChevronRight, IconChevronsDown, IconChevronsUp, IconFilter, IconPlus, IconTrash, IconX } from '@tabler/icons-react'
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { DraftStatusPill } from '../DraftStatusPill'
import { computeBomDraft, isChanged } from '../bomDraft'
import { api } from '../api/client'
import type {
  ApplicationFingerprintSummary,
  ApplicationSummary,
  ApplicationVersionSummary,
  AssetRelationshipSpec,
  AssetTypeSummary,
  AssetView,
  BomSummary,
  BoMSchema,
  BoMSchemaField,
  RelationView,
  VersionBomView,
} from '../api/types'
import { AddRelatedAssetsDialog, type AssetPickMode } from '../AddRelatedAssetsDialog'
import { BomImpactDialog, type BomImpactPlan } from '../BomImpactDialog'
import { CreateRelatedAssetDialog } from '../CreateRelatedAssetDialog'
import { RelatedAssetsBlock, type RelatedViewMode } from '../RelatedAssets'
import { SchemaPayloadView } from '../SchemaPayloadView'
import { SearchInput } from '../SearchInput'
import { colorForType, SbomGraphCanvas, type GraphLayoutDir, type GraphViewMode, type SbomGraphHandle } from '../SbomGraphCanvas'
type AssetPickRequest = { mode: AssetPickMode; spec: AssetRelationshipSpec }
type AddAssetMode = 'existing' | 'create'

function typeSpec(type: string, label?: string): AssetRelationshipSpec {
  return { role: '', label: label || type, targetType: type, section: type, cardinality: '*' }
}

function TypeSplitButton({
  label,
  types,
  onPick,
}: {
  label: string
  types: AssetTypeSummary[]
  onPick: (type: string) => void
}) {
  if (types.length === 0) return null
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
        {types.map((t) => (
          <Menu.Item key={t.type} onClick={() => onPick(t.type)}>
            {t.title || t.type}
          </Menu.Item>
        ))}
      </Menu.Dropdown>
    </Menu>
  )
}
type ConfirmRequest = {
  title: string
  message: string
  confirmLabel: string
  color?: string
  onConfirm: () => void
  cancelLabel?: string
  discardLabel?: string
  onDiscard?: () => void
}

function requiredScalarFields(schema: BoMSchema | null): BoMSchemaField[] {
  if (!schema) return []
  return (schema.contentSchema.fields ?? []).filter(
    (field) =>
      (field.required || field.identifier) &&
      field.schema.type !== 'OBJECT' &&
      field.schema.type !== 'ARRAY',
  )
}

const GRAPH_LAYOUTS: { value: GraphLayoutDir; label: string }[] = [
  { value: 'TB', label: 'Top to bottom' },
  { value: 'LR', label: 'Left to right' },
  { value: 'BT', label: 'Bottom to top' },
  { value: 'RL', label: 'Right to left' },
]

function versionLabel(v: ApplicationVersionSummary): string {
  if (v.status === 'DRAFT') return v.version || v.label || 'Draft'
  return v.version || v.label || 'Released'
}

function fingerprintTitle(fp: ApplicationFingerprintSummary): string {
  return fp.name?.trim() || fp.note?.trim() || fp.contentSha256.slice(0, 12)
}

function fingerprintCategory(fp: ApplicationFingerprintSummary): string {
  return fp.category?.trim() || 'unknown'
}

function fingerprintMenuLabel(fp: ApplicationFingerprintSummary): string {
  return `${fingerprintTitle(fp)} · ${fingerprintCategory(fp)}`
}

function uniqueTags(...lists: (string[] | undefined | null)[]): string[] {
  const seen = new Set<string>()
  const out: string[] = []
  for (const list of lists) {
    for (const raw of list ?? []) {
      const tag = raw.trim()
      if (!tag || seen.has(tag)) continue
      seen.add(tag)
      out.push(tag)
    }
  }
  return out
}

function sameTags(a: string[] | undefined | null, b: string[] | undefined | null): boolean {
  const left = a ?? []
  const right = b ?? []
  if (left.length !== right.length) return false
  return left.every((tag, i) => tag === right[i])
}

function parseSbomQuery(param: string | null, bomList: BomSummary[]): string[] {
  if (bomList.length < 2) return bomList.map((b) => b.id)
  if (!param || param === 'combined') return bomList.map((b) => b.id)
  const wanted = param.split(',').map((part) => part.trim()).filter(Boolean)
  const ids = wanted.filter((id) => bomList.some((b) => b.id === id))
  return ids.length > 0 ? ids : bomList.map((b) => b.id)
}

function formatSbomQuery(ids: string[], bomList: BomSummary[]): string | null {
  if (bomList.length < 2) return null
  if (ids.length === 0 || ids.length === bomList.length) return 'combined'
  if (ids.length === 1) return ids[0]
  return ids.join(',')
}

const FINGERPRINT_CATEGORIES = [
  { value: 'approval', label: 'approval' },
  { value: 'history', label: 'history' },
  { value: 'unknown', label: 'unknown' },
]

type FingerprintRow = ApplicationFingerprintSummary & { versionLabel: string }

function MetaField({ label, children }: { label: string; children: string }) {
  return (
    <div>
      <Text size="xs" fw={700} c="dimmed">
        {label}
      </Text>
      <Text size="sm" mt={4} style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
        {children || '—'}
      </Text>
    </div>
  )
}

export function ApplicationDetailPage() {
  const { id = '' } = useParams()
  const [params, setParams] = useSearchParams()
  const versionParam = params.get('version')
  const fingerprintParam = params.get('fingerprint')
  const sbomParam = params.get('sbom')
  const tab = params.get('tab') === 'graph' ? 'graph' : 'assets'
  const graphRef = useRef<SbomGraphHandle>(null)
  const paneScrollRef = useRef<HTMLDivElement>(null)
  const enterEditAfterLoad = useRef(false)

  const [app, setApp] = useState<ApplicationSummary | null>(null)
  const [versions, setVersions] = useState<ApplicationVersionSummary[]>([])
  const [bom, setBom] = useState<VersionBomView | null>(null)
  const [workingAssets, setWorkingAssets] = useState<AssetView[]>([])
  const [workingRels, setWorkingRels] = useState<RelationView[]>([])
  const [dirty, setDirty] = useState(false)
  const selectedAssetId = params.get('asset')
  const selectedTypeParam = params.get('type')
  const [fingerprints, setFingerprints] = useState<FingerprintRow[]>([])
  const [editName, setEditName] = useState('')
  const [editDescription, setEditDescription] = useState('')
  const [editAppTags, setEditAppTags] = useState<string[]>([])
  const [editTargetVersion, setEditTargetVersion] = useState('')
  const [editVersionTags, setEditVersionTags] = useState<string[]>([])
  const [editBomName, setEditBomName] = useState('')
  const [editBomDescription, setEditBomDescription] = useState('')
  const [editBomTags, setEditBomTags] = useState<string[]>([])
  const [boms, setBoms] = useState<BomSummary[]>([])
  const [draftOpen, setDraftOpen] = useState(false)
  const [draftTarget, setDraftTarget] = useState('')
  const [draftFrom, setDraftFrom] = useState<string | null>(null)
  const [draftCombine, setDraftCombine] = useState(false)
  const [draftFromBomCount, setDraftFromBomCount] = useState(0)
  const [fpOpen, setFpOpen] = useState(false)
  const [fpName, setFpName] = useState('')
  const [fpCategory, setFpCategory] = useState<string | null>('unknown')
  const [createBomOpen, setCreateBomOpen] = useState(false)
  const [newBomName, setNewBomName] = useState('')
  const [newBomDescription, setNewBomDescription] = useState('')
  const [newBomTags, setNewBomTags] = useState<string[]>([])
  const [changesOnly, setChangesOnly] = useState(false)
  const [roleSpecs, setRoleSpecs] = useState<AssetRelationshipSpec[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [promoteOpen, setPromoteOpen] = useState(false)
  const [promoteName, setPromoteName] = useState('')
  const [addOpen, setAddOpen] = useState(false)
  const [addMode, setAddMode] = useState<AddAssetMode>('existing')
  const [addType, setAddType] = useState<string | null>(null)
  const [addName, setAddName] = useState('')
  const [addSearch, setAddSearch] = useState('')
  const [addHits, setAddHits] = useState<AssetView[]>([])
  const [addSchema, setAddSchema] = useState<BoMSchema | null>(null)
  const [addFields, setAddFields] = useState<Record<string, string>>({})
  const [assetTypes, setAssetTypes] = useState<AssetTypeSummary[]>([])
  const [relOpen, setRelOpen] = useState<AssetPickRequest | null>(null)
  const [createRelOpen, setCreateRelOpen] = useState<AssetPickRequest | null>(null)
  const [impact, setImpact] = useState<BomImpactPlan | null>(null)
  const [expandedTypes, setExpandedTypes] = useState<Set<string>>(new Set())
  const [paneSearch, setPaneSearch] = useState('')
  const [graphViewMode, setGraphViewMode] = useState<GraphViewMode>('details')
  const [graphLayout, setGraphLayout] = useState<GraphLayoutDir>('TB')
  const [panToSelection, setPanToSelection] = useState(true)
  const [highlightedTypes, setHighlightedTypes] = useState<Set<string>>(() => new Set())
  const [relatedViewMode, setRelatedViewMode] = useState<RelatedViewMode>('tabs')
  const [relatedOutgoingTab, setRelatedOutgoingTab] = useState<string | null>(null)
  const [relatedIncomingTab, setRelatedIncomingTab] = useState<string | null>(null)
  const [confirm, setConfirm] = useState<ConfirmRequest | null>(null)
  const [viewSchema, setViewSchema] = useState<BoMSchema | null>(null)
  const [editPayload, setEditPayload] = useState<Record<string, unknown> | null>(null)
  const [editing, setEditing] = useState(false)
  const [contentLoading, setContentLoading] = useState(false)
  const [appCatalogTab, setAppCatalogTab] = useState<'versions' | 'fingerprints'>('versions')
  const [snapshotSearch, setSnapshotSearch] = useState('')
  const [paneChangesOnly, setPaneChangesOnly] = useState(false)

  const versionId = bom?.version.id ?? versionParam
  const currentFingerprint = fingerprints.find((fp) => fp.id === fingerprintParam) ?? null
  const versionOpen = currentFingerprint == null
  const fingerprintView = currentFingerprint != null
  const selectedBomIds = useMemo(() => parseSbomQuery(sbomParam, boms), [sbomParam, boms])
  const multiBomChrome = boms.length >= 2 && !fingerprintView
  const isCombined = multiBomChrome && selectedBomIds.length === boms.length
  const exactlyOneBom = !fingerprintView && (boms.length === 1 || (multiBomChrome && selectedBomIds.length === 1))
  const selectedBom =
    exactlyOneBom
      ? boms.find((b) => b.id === (boms.length === 1 ? boms[0].id : selectedBomIds[0])) ?? null
      : null
  const snapshotLabel = currentFingerprint
    ? fingerprintMenuLabel(currentFingerprint)
    : versions.find((v) => v.id === versionId)
      ? versionLabel(versions.find((v) => v.id === versionId)!)
      : 'Version'
  const combinedTagList = useMemo(
    () => bom?.combinedTags ?? uniqueTags(app?.tags, bom?.version.tags, ...boms.map((b) => b.tags)),
    [app?.tags, bom?.combinedTags, bom?.version.tags, boms],
  )

  const filteredSnapshots = useMemo(() => {
    const q = snapshotSearch.trim().toLowerCase()
    const vers = q
      ? versions.filter((v) => `${versionLabel(v)} ${v.status} ${v.version ?? ''}`.toLowerCase().includes(q))
      : versions
    const fps = q
      ? fingerprints.filter((fp) =>
          `${fingerprintMenuLabel(fp)} ${fp.versionLabel} ${fp.contentSha256}`.toLowerCase().includes(q),
        )
      : fingerprints
    return { vers, fps }
  }, [snapshotSearch, versions, fingerprints])

  async function load(preferredVersion?: string | null, fingerprintId?: string | null) {
    setError(null)
    const [a, list] = await Promise.all([api.getApplication(id), api.listVersions(id)])
    setApp(a)
    setEditName(a.name)
    setEditDescription(a.description ?? '')
    setEditAppTags(a.tags ?? [])
    setVersions(list)
    const preferred =
      list.find((v) => v.id === preferredVersion) ||
      list.find((v) => v.status === 'DRAFT') ||
      list.find((v) => v.status === 'RELEASED') ||
      list[0]
    if (!preferred) {
      setBom(null)
      setBoms([])
      return
    }
    const fpRows = (
      await Promise.all(
        list.map(async (v) => {
          const items = await api.listFingerprints(id, v.id)
          return items.map((fp) => ({ ...fp, versionLabel: versionLabel(v) }))
        }),
      )
    ).flat()
    setFingerprints(fpRows)
    const requestedFp = fingerprintId !== undefined ? fingerprintId : fingerprintParam
    const validFp =
      requestedFp && fpRows.some((fp) => fp.id === requestedFp && fp.versionId === preferred.id)
        ? requestedFp
        : null
    let view: VersionBomView
    let bomList: BomSummary[] = []
    if (validFp) {
      view = await api.getFingerprint(id, preferred.id, validFp)
      setBoms([])
    } else {
      bomList = await api.listBoms(id, preferred.id)
      setBoms(bomList)
      const selectedIds = parseSbomQuery(sbomParam, bomList)
      if (bomList.length >= 2) {
        if (selectedIds.length === 1) {
          view = await api.getBom(id, preferred.id, selectedIds[0])
        } else {
          view = await api.getCombined(
            id,
            preferred.id,
            selectedIds.length === bomList.length ? undefined : selectedIds,
          )
        }
      } else {
        view = await api.getVersion(id, preferred.id)
      }
    }
    setBom(view)
    setWorkingAssets(view.assets)
    setWorkingRels(view.relations)
    setDirty(false)
    setEditTargetVersion(view.version.version ?? '')
    setEditVersionTags(view.version.tags ?? [])
    const oneBom =
      !validFp && (bomList.length === 1 || (bomList.length >= 2 && parseSbomQuery(sbomParam, bomList).length === 1))
    const currentBom = oneBom
      ? bomList.find((b) => b.id === (bomList.length === 1 ? bomList[0].id : parseSbomQuery(sbomParam, bomList)[0]))
      : undefined
    setEditBomName(currentBom?.name ?? '')
    setEditBomDescription(currentBom?.description ?? '')
    setEditBomTags(currentBom?.tags ?? [])
    setEditing(enterEditAfterLoad.current)
    if (!enterEditAfterLoad.current) setPaneChangesOnly(false)
    const next = new URLSearchParams(params)
    let urlChanged = false
    if (!versionParam || versionParam !== preferred.id) {
      next.set('version', preferred.id)
      urlChanged = true
    }
    const assetQ = next.get('asset')
    if (assetQ && !view.assets.some((item) => item.id === assetQ)) {
      next.delete('asset')
      urlChanged = true
    }
    if (validFp) {
      if (next.get('fingerprint') !== validFp) {
        next.set('fingerprint', validFp)
        urlChanged = true
      }
      if (next.get('sbom')) {
        next.delete('sbom')
        urlChanged = true
      }
    } else {
      if (next.get('fingerprint')) {
        next.delete('fingerprint')
        urlChanged = true
      }
      const sbomQuery = formatSbomQuery(parseSbomQuery(sbomParam, bomList), bomList)
      if (sbomQuery) {
        if (next.get('sbom') !== sbomQuery) {
          next.set('sbom', sbomQuery)
          urlChanged = true
        }
      } else if (next.get('sbom')) {
        next.delete('sbom')
        urlChanged = true
      }
    }
    if (urlChanged) setParams(next, { replace: true })
  }

  useEffect(() => {
    if (id) void load(versionParam, fingerprintParam).catch((e) => setError(e instanceof Error ? e.message : 'Load failed'))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, versionParam, fingerprintParam, sbomParam])

  const draft = useMemo(
    () => computeBomDraft(bom?.assets ?? [], bom?.relations ?? [], workingAssets, workingRels),
    [bom, workingAssets, workingRels],
  )

  const grouped = useMemo(() => {
    const map = new Map<string, AssetView[]>()
    const source = editing ? draft.graphAssets : workingAssets
    for (const a of source) {
      const list = map.get(a.type) ?? []
      list.push(a)
      map.set(a.type, list)
    }
    return [...map.entries()].sort(([x], [y]) => x.localeCompare(y))
  }, [workingAssets, draft.graphAssets, editing])

  const assetTypeOptions = useMemo(() => {
    const byType = new Map<string, AssetTypeSummary>()
    for (const t of assetTypes) {
      const prev = byType.get(t.type)
      if (!prev || t.version > prev.version) byType.set(t.type, t)
    }
    return [...byType.values()].sort((a, b) => (a.title || a.type).localeCompare(b.title || b.type))
  }, [assetTypes])

  const selectedAsset = workingAssets.find((a) => a.id === selectedAssetId) ?? draft.graphAssets.find((a) => a.id === selectedAssetId) ?? null
  const appSelected = !selectedAssetId && !selectedTypeParam
  const selectedType = selectedTypeParam || selectedAsset?.type || null
  const typeAssets = selectedType ? (editing ? draft.graphAssets : workingAssets).filter((a) => a.type === selectedType) : []
  const writable =
    editing &&
    bom != null &&
    bom.version.status === 'DRAFT' &&
    !fingerprintParam &&
    (boms.length <= 1 || selectedBomIds.length === 1)
  const appMetaDirty =
    app != null &&
    (editName.trim() !== app.name ||
      (editDescription.trim() || '') !== (app.description ?? '') ||
      !sameTags(editAppTags, app.tags ?? []))
  const versionMetaDirty =
    bom != null &&
    bom.version.status === 'DRAFT' &&
    !fingerprintView &&
    (editTargetVersion.trim() !== (bom.version.version ?? '') || !sameTags(editVersionTags, bom.version.tags ?? []))
  const bomMetaDirty =
    selectedBom != null &&
    boms.length >= 2 &&
    !fingerprintView &&
    (editBomName.trim() !== selectedBom.name ||
      (editBomDescription.trim() || '') !== (selectedBom.description ?? '') ||
      !sameTags(editBomTags, selectedBom.tags ?? []))
  const payloadUnapplied =
    selectedAsset != null &&
    editPayload != null &&
    JSON.stringify(editPayload) !== JSON.stringify(selectedAsset.payload)
  const versionDirty = dirty || appMetaDirty || versionMetaDirty || bomMetaDirty

  const groupedVisible = useMemo(() => {
    const q = paneSearch.trim().toLowerCase()
    return grouped
      .map(([type, items]) => {
        let next = items
        if (q) next = next.filter((a) => a.label.toLowerCase().includes(q))
        if (editing && paneChangesOnly) {
          next = next.filter((a) => {
            if (isChanged(draft.assetStatus.get(a.id))) return true
            return payloadUnapplied && a.id === selectedAssetId
          })
        }
        return [type, next] as [string, AssetView[]]
      })
      .filter(([, items]) => items.length > 0)
  }, [grouped, paneSearch, editing, paneChangesOnly, draft.assetStatus, payloadUnapplied, selectedAssetId])

  useEffect(() => {
    if (!editing) return
    void api.listAssetTypes().then(setAssetTypes).catch(() => setAssetTypes([]))
  }, [editing])

  useEffect(() => {
    if (!addOpen || !addType) {
      setAddSchema(null)
      return
    }
    const summary = assetTypeOptions.find((t) => t.type === addType)
    if (!summary) return
    void api
      .getSchema(summary.type, summary.version)
      .then((schema) => {
        setAddSchema(schema)
        setAddFields((prev) => {
          const next = { ...prev }
          for (const field of requiredScalarFields(schema)) {
            if (next[field.name] == null || next[field.name] === '') {
              next[field.name] = field.schema.default != null ? String(field.schema.default) : ''
            }
          }
          return next
        })
      })
      .catch(() => setAddSchema(null))
  }, [addOpen, addType, assetTypeOptions])

  useEffect(() => {
    if (!selectedAsset) {
      setRoleSpecs([])
      setViewSchema(null)
      setEditPayload(null)
      setContentLoading(false)
      return
    }
    setEditPayload({ ...selectedAsset.payload })
    setContentLoading(true)
    let cancelled = false
    void Promise.all([
      api.relationshipsForType(selectedAsset.type).catch(() => [] as AssetRelationshipSpec[]),
      api.getSchema(selectedAsset.type, selectedAsset.schemaVersion).catch(() => null),
    ]).then(([specs, schema]) => {
      if (cancelled) return
      setRoleSpecs(specs)
      setViewSchema(schema)
      setContentLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [selectedAsset?.id, selectedAsset?.type, selectedAsset?.schemaVersion])

  function markDirty(assets: AssetView[], rels: RelationView[]) {
    setWorkingAssets(assets)
    setWorkingRels(rels)
    setDirty(true)
  }

  function applyPayload() {
    if (!selectedAsset || !editPayload) return
    const name = typeof editPayload.name === 'string' && editPayload.name.trim() ? editPayload.name.trim() : selectedAsset.label
    const next = workingAssets.map((a) =>
      a.id === selectedAsset.id ? { ...a, payload: { ...editPayload }, label: name } : a,
    )
    markDirty(next, workingRels)
    setEditPayload({ ...editPayload })
  }

  function revertAssetPayload(assetId: string) {
    const baseline = bom?.assets.find((a) => a.id === assetId)
    if (!baseline) return
    markDirty(
      workingAssets.map((a) => (a.id === assetId ? { ...baseline } : a)),
      workingRels,
    )
    if (selectedAssetId === assetId) setEditPayload({ ...baseline.payload })
  }

  function revertRelation(rel: RelationView) {
    const status = draft.relationStatus.get(rel.id)
    if (status === 'new') {
      markDirty(
        workingAssets,
        workingRels.filter((r) => r.id !== rel.id),
      )
      return
    }
    if (status === 'deleted') {
      const baseline = bom?.relations.find((r) => r.id === rel.id)
      if (!baseline) return
      markDirty(workingAssets, [...workingRels, baseline])
    }
  }

  function switchVersion(nextId: string) {
    setAppCatalogTab('versions')
    switchSnapshot(nextId, null)
  }

  function switchFingerprint(fp: FingerprintRow) {
    setAppCatalogTab('fingerprints')
    switchSnapshot(fp.versionId, fp.id)
  }

  function switchSnapshot(nextId: string, fingerprintId: string | null) {
    enterEditAfterLoad.current = false
    askLeaveUnsaved(() => {
      const next = new URLSearchParams(params)
      next.set('version', nextId)
      if (fingerprintId) next.set('fingerprint', fingerprintId)
      else next.delete('fingerprint')
      next.delete('sbom')
      setParams(next)
    })
  }

  function askLeaveUnsaved(then: () => void) {
    if (versionDirty || payloadUnapplied) {
      setConfirm({
        title: 'Leave unsaved changes?',
        message: 'Leave this selection to drop edits that have not been saved.',
        confirmLabel: 'Leave',
        cancelLabel: 'Stay',
        color: 'red',
        onConfirm: () => {
          discard()
          then()
        },
      })
      return
    }
    then()
  }

  function switchBomSelection(ids: string[]) {
    askLeaveUnsaved(() => {
      enterEditAfterLoad.current = false
      const next = new URLSearchParams(params)
      const query = formatSbomQuery(ids, boms)
      if (query) next.set('sbom', query)
      else next.delete('sbom')
      setParams(next)
    })
  }

  function selectCombinedSbom() {
    switchBomSelection(boms.map((b) => b.id))
  }

  function selectSingleBom(bomId: string) {
    switchBomSelection([bomId])
  }

  function toggleBomSelected(bomId: string, checked: boolean) {
    if (checked) {
      const next = selectedBomIds.includes(bomId) ? selectedBomIds : [...selectedBomIds, bomId]
      switchBomSelection(next)
      return
    }
    const next = selectedBomIds.filter((id) => id !== bomId)
    if (next.length === 0) switchBomSelection([bomId])
    else switchBomSelection(next)
  }

  async function save() {
    if (!bom) return
    let assetsToSave = workingAssets
    if (payloadUnapplied && selectedAsset && editPayload) {
      const name =
        typeof editPayload.name === 'string' && editPayload.name.trim()
          ? editPayload.name.trim()
          : selectedAsset.label
      assetsToSave = workingAssets.map((a) =>
        a.id === selectedAsset.id ? { ...a, payload: { ...editPayload }, label: name } : a,
      )
      setWorkingAssets(assetsToSave)
      setDirty(true)
      setEditPayload({ ...editPayload })
    }
    setBusy(true)
    setError(null)
    try {
      const baselineById = new Map((bom.assets ?? []).map((a) => [a.id, a]))
      for (const asset of assetsToSave) {
        const prev = baselineById.get(asset.id)
        if (prev && JSON.stringify(prev.payload) !== JSON.stringify(asset.payload)) {
          await api.updateAsset(asset.id, asset.payload)
        }
      }
      const relationBody = workingRels.map((r) => ({
        fromAssetId: r.fromAssetId,
        toAssetId: r.toAssetId,
        role: r.role,
      }))
      let saved: VersionBomView = bom
      if ((dirty || payloadUnapplied) && writable) {
        if (boms.length >= 2 && selectedBomIds.length === 1) {
          saved = await api.saveBom(id, bom.version.id, selectedBomIds[0], {
            assetIds: assetsToSave.map((a) => a.id),
            relations: relationBody,
          })
        } else if (boms.length <= 1) {
          saved = await api.saveVersionBom(id, bom.version.id, {
            assetIds: assetsToSave.map((a) => a.id),
            relations: relationBody,
          })
        }
      }
      if (app && appMetaDirty) {
        const updatedApp = await api.updateApplication(id, {
          name: editName.trim(),
          description: editDescription.trim() || null,
          tags: editAppTags,
        })
        setApp(updatedApp)
        setEditName(updatedApp.name)
        setEditDescription(updatedApp.description ?? '')
        setEditAppTags(updatedApp.tags ?? [])
      }
      if (versionMetaDirty) {
        const patched = await api.patchVersion(id, bom.version.id, {
          version: editTargetVersion.trim(),
          tags: editVersionTags,
        })
        saved = { ...saved, version: { ...saved.version, ...patched } }
        setEditTargetVersion(patched.version ?? '')
        setEditVersionTags(patched.tags ?? [])
      }
      if (bomMetaDirty && selectedBom) {
        const patchedBom = await api.patchBom(id, bom.version.id, selectedBom.id, {
          name: editBomName.trim(),
          description: editBomDescription.trim() || null,
          tags: editBomTags,
        })
        setBoms((prev) => prev.map((b) => (b.id === patchedBom.id ? patchedBom : b)))
        setEditBomName(patchedBom.name)
        setEditBomDescription(patchedBom.description ?? '')
        setEditBomTags(patchedBom.tags ?? [])
      }
      setBom(saved)
      setWorkingAssets(saved.assets)
      setWorkingRels(saved.relations)
      setDirty(false)
      setEditing(false)
      enterEditAfterLoad.current = false
      setPaneChangesOnly(false)
      setVersions(await api.listVersions(id))
      if (!fingerprintView) {
        setBoms(await api.listBoms(id, bom.version.id))
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Save failed')
    } finally {
      setBusy(false)
    }
  }

  function discard() {
    if (!bom) return
    setWorkingAssets(bom.assets)
    setWorkingRels(bom.relations)
    setDirty(false)
    setEditing(false)
    enterEditAfterLoad.current = false
    setPaneChangesOnly(false)
    if (app) {
      setEditName(app.name)
      setEditDescription(app.description ?? '')
      setEditAppTags(app.tags ?? [])
    }
    setEditTargetVersion(bom.version.version ?? '')
    setEditVersionTags(bom.version.tags ?? [])
    if (selectedBom) {
      setEditBomName(selectedBom.name)
      setEditBomDescription(selectedBom.description ?? '')
      setEditBomTags(selectedBom.tags ?? [])
    }
    if (selectedAssetId) {
      const restored = bom.assets.find((a) => a.id === selectedAssetId)
      setEditPayload(restored ? { ...restored.payload } : null)
    }
  }

  function basedOnLabel(v: ApplicationVersionSummary): string {
    if (v.basedOnFingerprintId) {
      const fp = fingerprints.find((item) => item.id === v.basedOnFingerprintId)
      return fp ? fingerprintTitle(fp) : 'Fingerprint'
    }
    if (v.basedOnVersionId) {
      const src = versions.find((item) => item.id === v.basedOnVersionId)
      return src ? versionLabel(src) : '—'
    }
    return '—'
  }

  function dependentLine(d: ApplicationVersionSummary): string {
    const target = d.version || d.label || 'Draft'
    if (d.basedOnFingerprintId) {
      const fp = fingerprints.find((item) => item.id === d.basedOnFingerprintId)
      return fp ? `${target} (based on ${fingerprintTitle(fp)})` : `${target} (based on fingerprint)`
    }
    return target
  }

  const draftSourceOptions = useMemo(() => {
    const released = versions
      .filter((v) => v.status === 'RELEASED')
      .map((v) => ({ value: `v:${v.id}`, label: versionLabel(v) }))
    const drafts = versions
      .filter((v) => v.status === 'DRAFT')
      .map((v) => ({ value: `v:${v.id}`, label: versionLabel(v) }))
    const fps = fingerprints.map((fp) => ({
      value: `fp:${fp.id}`,
      label: `${fingerprintMenuLabel(fp)} · ${fp.versionLabel}`,
    }))
    const groups: { group: string; items: { value: string; label: string }[] }[] = []
    if (released.length) groups.push({ group: 'Released', items: released })
    if (drafts.length) groups.push({ group: 'Drafts', items: drafts })
    if (fps.length) groups.push({ group: 'Fingerprints', items: fps })
    return groups
  }, [versions, fingerprints])

  useEffect(() => {
    if (!draftOpen || !draftFrom?.startsWith('v:')) {
      if (draftFrom?.startsWith('fp:')) setDraftFromBomCount(0)
      return
    }
    const fromVersionId = draftFrom.slice(2)
    let cancelled = false
    void api
      .listBoms(id, fromVersionId)
      .then((list) => {
        if (!cancelled) setDraftFromBomCount(list.length)
      })
      .catch(() => {
        if (!cancelled) setDraftFromBomCount(0)
      })
    return () => {
      cancelled = true
    }
  }, [draftOpen, draftFrom, id])

  function openNewDraftModal() {
    askLeaveUnsaved(() => {
      setDraftTarget('')
      setDraftFrom(bom ? `v:${bom.version.id}` : null)
      setDraftCombine(false)
      setDraftFromBomCount(0)
      setDraftOpen(true)
    })
  }

  function openFingerprintModal() {
    setFpName('')
    setFpCategory('unknown')
    setFpOpen(true)
  }

  function openCreateBomModal() {
    askLeaveUnsaved(() => {
      setNewBomName('')
      setNewBomDescription('')
      setNewBomTags([])
      setCreateBomOpen(true)
    })
  }

  async function submitNewDraft() {
    if (!draftTarget.trim() || !draftFrom) return
    setBusy(true)
    setError(null)
    try {
      const body: {
        targetVersion: string
        fromVersionId?: string
        fromFingerprintId?: string
        combineConstituents?: boolean
      } = { targetVersion: draftTarget.trim() }
      if (draftFrom.startsWith('fp:')) {
        body.fromFingerprintId = draftFrom.slice(3)
      } else {
        body.fromVersionId = draftFrom.slice(2)
        if (draftFromBomCount > 1) body.combineConstituents = draftCombine
      }
      enterEditAfterLoad.current = true
      const created = await api.createDraftVersion(id, body)
      setDraftOpen(false)
      const next = new URLSearchParams(params)
      next.set('version', created.version.id)
      next.delete('fingerprint')
      next.delete('sbom')
      setParams(next)
    } catch (e) {
      enterEditAfterLoad.current = false
      setError(e instanceof Error ? e.message : 'Could not create draft')
    } finally {
      setBusy(false)
    }
  }

  async function submitFingerprint() {
    if (!bom || !fpName.trim() || !fpCategory) return
    setBusy(true)
    setError(null)
    try {
      const fp = await api.createFingerprint(id, bom.version.id, {
        name: fpName.trim(),
        category: fpCategory,
      })
      setFingerprints((prev) => [{ ...fp, versionLabel: versionLabel(bom.version) }, ...prev])
      setFpOpen(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not create fingerprint')
    } finally {
      setBusy(false)
    }
  }

  async function submitCreateBom() {
    if (!bom || !newBomName.trim()) return
    setBusy(true)
    setError(null)
    try {
      const created = await api.createBom(id, bom.version.id, {
        name: newBomName.trim(),
        description: newBomDescription.trim() || undefined,
        tags: newBomTags,
      })
      setCreateBomOpen(false)
      enterEditAfterLoad.current = bom.version.status === 'DRAFT'
      const next = new URLSearchParams(params)
      next.set('sbom', created.id)
      next.delete('fingerprint')
      setParams(next)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not create BOM')
    } finally {
      setBusy(false)
    }
  }

  function askDeleteDraft(v: ApplicationVersionSummary) {
    askLeaveUnsaved(() => {
      void (async () => {
        try {
          const dependents = await api.listVersionDependents(id, v.id)
          if (dependents.length === 0) {
            setConfirm({
              title: 'Delete draft?',
              message: `Delete draft ${versionLabel(v)}? This removes its BOMs and fingerprints.`,
              confirmLabel: 'Delete',
              color: 'red',
              onConfirm: () => void deleteDraft(v, false),
            })
            return
          }
          const lines = dependents.map((d) => `• ${dependentLine(d)}`).join('\n')
          setConfirm({
            title: 'Delete draft and dependents?',
            message: `Deleting this draft will also delete the listed drafts (their BOMs and fingerprints):\n${lines}`,
            confirmLabel: 'Delete all',
            color: 'red',
            onConfirm: () => void deleteDraft(v, true),
          })
        } catch (e) {
          setError(e instanceof Error ? e.message : 'Could not list dependents')
        }
      })()
    })
  }

  async function deleteDraft(v: ApplicationVersionSummary, confirmDependents: boolean) {
    setBusy(true)
    setError(null)
    try {
      await api.deleteVersion(id, v.id, confirmDependents)
      const next = new URLSearchParams(params)
      if (versionId === v.id) {
        next.delete('version')
        next.delete('fingerprint')
        next.delete('sbom')
      }
      if (next.get('version') === v.id) {
        next.delete('version')
      }
      setParams(next)
      await load(versionId === v.id ? null : versionId, fingerprintParam)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not delete draft')
    } finally {
      setBusy(false)
    }
  }

  function askDeleteBom(row: BomSummary) {
    if (boms.length <= 1) return
    askLeaveUnsaved(() => {
      setConfirm({
        title: 'Delete BOM?',
        message: `Delete BOM “${row.name}”? This cannot be undone.`,
        confirmLabel: 'Delete',
        color: 'red',
        onConfirm: () => void deleteBomRow(row),
      })
    })
  }

  async function deleteBomRow(row: BomSummary) {
    if (!bom) return
    setBusy(true)
    setError(null)
    try {
      await api.deleteBom(id, bom.version.id, row.id)
      const remaining = await api.listBoms(id, bom.version.id)
      const next = new URLSearchParams(params)
      if (remaining.length <= 1) {
        next.delete('sbom')
      } else if (selectedBomIds.length === 1 && selectedBomIds[0] === row.id) {
        next.set('sbom', 'combined')
      } else {
        const kept = selectedBomIds.filter((bomId) => bomId !== row.id)
        const query = formatSbomQuery(kept, remaining)
        if (query) next.set('sbom', query)
        else next.delete('sbom')
      }
      setParams(next)
      setBoms(remaining)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not delete BOM')
    } finally {
      setBusy(false)
    }
  }

  function includeAssets(targets: AssetView[]) {
    if (targets.length === 0) return
    let assets = workingAssets
    for (const t of targets) {
      if (!assets.some((a) => a.id === t.id)) assets = [...assets, t]
    }
    if (assets !== workingAssets) markDirty(assets, workingRels)
    const last = targets[targets.length - 1]
    writeSelection({ asset: last.id, type: last.type })
    setExpandedTypes((prev) => new Set(prev).add(last.type))
    setRelOpen(null)
    setCreateRelOpen(null)
  }

  function includeAsset(asset: AssetView) {
    includeAssets([asset])
    setAddOpen(false)
    setAddHits([])
    setAddSearch('')
    setAddName('')
    setAddFields({})
  }

  function openAddType(type: string, mode: AssetPickMode = 'add') {
    setRelOpen({ mode, spec: typeSpec(type) })
  }

  function openCreateType(type: string, mode: AssetPickMode = 'add') {
    setCreateRelOpen({ mode, spec: typeSpec(type) })
  }

  async function addNewAsset() {
    if (!addType) return
    const required = requiredScalarFields(addSchema)
    const payload: Record<string, unknown> = {}
    if (required.length === 0) {
      if (!addName.trim()) return
      payload.name = addName.trim()
    } else {
      for (const field of required) {
        const value = (addFields[field.name] ?? '').trim()
        if (!value) {
          setError(`Field “${field.schema.title || field.name}” is required`)
          return
        }
        payload[field.name] = value
      }
    }
    setBusy(true)
    setError(null)
    try {
      const created = await api.createAsset({
        type: addType,
        payload,
        owner: app?.name,
      })
      includeAsset(created)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not create asset')
    } finally {
      setBusy(false)
    }
  }

  async function searchAddRegistry() {
    setError(null)
    try {
      let hits: AssetView[]
      if (addType) {
        try {
          hits = await api.searchAssets({
            type: addType,
            filters: addSearch.trim() ? { name: addSearch.trim() } : {},
          })
        } catch {
          hits = await api.searchAssets({ type: addType })
        }
      } else {
        hits = await api.searchAssets({})
      }
      const q = addSearch.trim().toLowerCase()
      setAddHits(
        hits.filter((a) => {
          if (workingAssets.some((x) => x.id === a.id)) return false
          if (!q) return true
          return `${a.label} ${a.type}`.toLowerCase().includes(q)
        }),
      )
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Search failed')
    }
  }

  function addRelations(targets: AssetView[], spec: AssetRelationshipSpec) {
    if (!selectedAsset || targets.length === 0) return
    let assets = workingAssets
    const rels = [...workingRels]
    const existing = new Set(
      workingRels.flatMap((r) => {
        if (r.role !== spec.role) return []
        if (spec.direction === 'IN') return r.toAssetId === selectedAsset.id ? [r.fromAssetId] : []
        return r.fromAssetId === selectedAsset.id ? [r.toAssetId] : []
      }),
    )
    existing.add(selectedAsset.id)
    for (const target of targets) {
      if (existing.has(target.id)) continue
      if (!assets.some((a) => a.id === target.id)) assets = [...assets, target]
      rels.push({
        id: `local-${crypto.randomUUID()}`,
        role: spec.role,
        label: spec.label,
        fromAssetId: spec.direction === 'IN' ? target.id : selectedAsset.id,
        toAssetId: spec.direction === 'IN' ? selectedAsset.id : target.id,
      })
      existing.add(target.id)
    }
    markDirty(assets, rels)
    setRelOpen(null)
    setCreateRelOpen(null)
  }

  function assetLabelById(id: string) {
    return workingAssets.find((a) => a.id === id)?.label || draft.graphAssets.find((a) => a.id === id)?.label || id
  }

  function describeRel(rel: RelationView) {
    return { rel, fromLabel: assetLabelById(rel.fromAssetId), toLabel: assetLabelById(rel.toAssetId) }
  }

  function incidentRels(id: string, rels: RelationView[]) {
    return rels.filter((r) => r.fromAssetId === id || r.toAssetId === id)
  }

  function otherEnd(rel: RelationView, assetId: string) {
    return rel.fromAssetId === assetId ? rel.toAssetId : rel.fromAssetId
  }

  function applyImpact(plan: BomImpactPlan) {
    const dropAssets = new Set(plan.deleteAssets.map((a) => a.id))
    for (const row of plan.orphans) {
      if (row.delete) dropAssets.add(row.asset.id)
    }
    const dropRels = new Set(plan.relations.map((row) => row.rel.id))
    const rels = workingRels.filter((r) => {
      if (dropRels.has(r.id)) return false
      if (dropAssets.has(r.fromAssetId) || dropAssets.has(r.toAssetId)) return false
      return true
    })
    const assets = workingAssets.filter((a) => !dropAssets.has(a.id))
    markDirty(assets, rels)
    setImpact(null)
  }

  function removeRelation(rel: RelationView) {
    const next = workingRels.filter((r) => r.id !== rel.id)
    const ends = [rel.fromAssetId, rel.toAssetId]
    const orphans: { asset: AssetView; delete: boolean }[] = []
    const seen = new Set<string>()
    for (const id of ends) {
      if (seen.has(id)) continue
      seen.add(id)
      if (incidentRels(id, next).length > 0) continue
      const asset = workingAssets.find((a) => a.id === id)
      if (asset) orphans.push({ asset, delete: false })
    }
    if (orphans.length === 0) {
      markDirty(workingAssets, next)
      return
    }
    setImpact({
      title: 'Remove relation',
      message:
        'An asset with no remaining relations can be kept or deleted. Keep is checked by default; uncheck it to delete.',
      confirmLabel: 'Apply',
      relations: [describeRel(rel)],
      deleteAssets: [],
      orphans,
    })
  }

  function askDeleteAsset(asset: AssetView) {
    const incident = incidentRels(asset.id, workingRels)
    const without = workingRels.filter((r) => r.fromAssetId !== asset.id && r.toAssetId !== asset.id)
    const seen = new Set<string>()
    const orphans: { asset: AssetView; delete: boolean }[] = []
    for (const r of incident) {
      const oid = otherEnd(r, asset.id)
      if (seen.has(oid)) continue
      seen.add(oid)
      if (incidentRels(oid, without).length > 0) continue
      const other = workingAssets.find((a) => a.id === oid)
      if (other) orphans.push({ asset: other, delete: false })
    }
    if (incident.length === 0 && orphans.length === 0) {
      markDirty(
        workingAssets.filter((a) => a.id !== asset.id),
        workingRels,
      )
      writeSelection({ asset: null })
      return
    }
    setImpact({
      title: 'Delete asset',
      message:
        'This also removes the listed relations. Assets that would be left with no relations can be deleted or kept.',
      confirmLabel: 'Delete',
      relations: incident.map(describeRel),
      deleteAssets: [asset],
      orphans,
    })
  }

  function replaceAsset(next: AssetView) {
    if (!selectedAsset || next.type !== selectedAsset.type || next.id === selectedAsset.id) return
    const oldId = selectedAsset.id
    let assets = workingAssets.filter((a) => a.id !== oldId)
    if (!assets.some((a) => a.id === next.id)) assets = [...assets, next]
    const seen = new Set<string>()
    const rels: RelationView[] = []
    for (const r of workingRels) {
      const from = r.fromAssetId === oldId ? next.id : r.fromAssetId
      const to = r.toAssetId === oldId ? next.id : r.toAssetId
      if (from === to) continue
      const key = `${from}|${to}|${r.role}`
      if (seen.has(key)) continue
      seen.add(key)
      rels.push({ ...r, fromAssetId: from, toAssetId: to })
    }
    markDirty(assets, rels)
    writeSelection({ asset: next.id, type: next.type })
    setRelOpen(null)
    setCreateRelOpen(null)
  }

  function onPickAdd(targets: AssetView[]) {
    if (!relOpen || targets.length === 0) return
    if (relOpen.mode === 'relate') addRelations(targets, relOpen.spec)
    else if (relOpen.mode === 'replace') replaceAsset(targets[0])
    else includeAssets(targets)
  }

  function onPickCreateExisting(targets: AssetView[]) {
    if (!createRelOpen || targets.length === 0) return
    if (createRelOpen.mode === 'relate') addRelations(targets, createRelOpen.spec)
    else if (createRelOpen.mode === 'replace') replaceAsset(targets[0])
    else includeAssets(targets)
  }

  function onPickCreated(asset: AssetView) {
    onPickCreateExisting([asset])
  }

  function writeSelection(patch: { asset?: string | null; type?: string | null; tab?: string }) {
    const next = new URLSearchParams(params)
    if ('asset' in patch) {
      if (patch.asset) next.set('asset', patch.asset)
      else next.delete('asset')
    }
    if ('type' in patch) {
      if (patch.type) next.set('type', patch.type)
      else next.delete('type')
    }
    if (patch.tab) next.set('tab', patch.tab)
    setParams(next)
  }

  function withAppliedPayload(action: () => void) {
    if (!payloadUnapplied) {
      action()
      return
    }
    setConfirm({
      title: 'Apply component changes?',
      message: 'This component has field edits that are not applied to the version yet.',
      confirmLabel: 'Apply',
      discardLabel: 'Discard',
      onConfirm: () => {
        applyPayload()
        action()
      },
      onDiscard: () => {
        if (selectedAsset) setEditPayload({ ...selectedAsset.payload })
        action()
      },
    })
  }

  function selectApplication() {
    withAppliedPayload(() => writeSelection({ asset: null, type: null }))
  }

  function selectType(type: string) {
    withAppliedPayload(() => {
      writeSelection({ asset: null, type })
      setExpandedTypes((prev) => new Set(prev).add(type))
    })
  }

  function selectAsset(assetId: string, opts?: { pan?: boolean; tab?: string }) {
    withAppliedPayload(() => {
      const a = workingAssets.find((x) => x.id === assetId) ?? draft.graphAssets.find((x) => x.id === assetId)
      if (!a) return
      writeSelection({ asset: assetId, type: a.type, tab: opts?.tab })
      setExpandedTypes((prev) => new Set(prev).add(a.type))
      const shouldPan = opts?.pan ?? (tab === 'graph' && panToSelection)
      if (shouldPan) {
        requestAnimationFrame(() => graphRef.current?.focusNode(assetId))
      }
    })
  }

  function showOnGraph(assetId: string) {
    selectAsset(assetId, { pan: true, tab: 'graph' })
  }

  const prevTab = useRef('')
  useEffect(() => {
    const enteredGraph = tab === 'graph' && prevTab.current !== 'graph'
    prevTab.current = tab
    if (!enteredGraph) return
    const timer = window.setTimeout(() => {
      graphRef.current?.applyLayout(graphLayout)
      if (panToSelection && selectedAssetId) graphRef.current?.focusNode(selectedAssetId)
    }, 50)
    return () => window.clearTimeout(timer)
  }, [tab, panToSelection, selectedAssetId, graphLayout])

  useLayoutEffect(() => {
    if (!selectedAssetId) return
    const scroll = () => {
      const el = paneScrollRef.current?.querySelector(`[data-asset-id="${CSS.escape(selectedAssetId)}"]`)
      el?.scrollIntoView({ block: 'nearest' })
    }
    scroll()
    const frame = requestAnimationFrame(scroll)
    return () => cancelAnimationFrame(frame)
  }, [selectedAssetId, expandedTypes])

  useEffect(() => {
    if (!selectedAsset) return
    setExpandedTypes((prev) => {
      if (prev.has(selectedAsset.type)) return prev
      return new Set(prev).add(selectedAsset.type)
    })
  }, [selectedAsset])

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group justify="space-between" wrap="nowrap">
        <div style={{ minWidth: 0 }}>
          <Text size="sm" c="dimmed">
            <Anchor component={Link} to="/applications">
              Applications
            </Anchor>
          </Text>
          <Title order={3} lineClamp={1}>
            {app?.name || 'Application'}
          </Title>
          {!editing && combinedTagList.length > 0 && (
            <Group gap={4} mt={4} wrap="wrap">
              {combinedTagList.map((tag) => (
                <Badge key={tag} size="xs" variant="light">
                  {tag}
                </Badge>
              ))}
            </Group>
          )}
        </div>
        <Group gap="xs" wrap="nowrap">
          <Menu width={300} position="bottom-end" withinPortal onClose={() => setSnapshotSearch('')}>
            <Menu.Target>
              <Button size="sm" variant="default" rightSection={<IconChevronDown size={14} />} maw={220}>
                <Text size="sm" truncate>
                  {snapshotLabel}
                </Text>
              </Button>
            </Menu.Target>
            <Menu.Dropdown>
              <div onMouseDown={(e) => e.stopPropagation()}>
                <SearchInput
                  size="xs"
                  mx="xs"
                  mt={6}
                  mb={4}
                  placeholder="Filter versions and fingerprints"
                  value={snapshotSearch}
                  onValueChange={setSnapshotSearch}
                />
              </div>
              <ScrollArea.Autosize mah={320}>
                <Menu.Label>Versions</Menu.Label>
                {filteredSnapshots.vers.length === 0 ? (
                  <Text size="xs" c="dimmed" px="sm" py={4}>
                    None
                  </Text>
                ) : (
                  filteredSnapshots.vers.map((v) => {
                    const open = versionOpen && v.id === versionId
                    return (
                      <Menu.Item
                        key={v.id}
                        onClick={() => switchVersion(v.id)}
                        rightSection={open ? <Badge size="xs">Open</Badge> : undefined}
                      >
                        {versionLabel(v)}
                        <Text span size="xs" c="dimmed" ml={6}>
                          {v.status}
                        </Text>
                      </Menu.Item>
                    )
                  })
                )}
                <Menu.Label>Fingerprints</Menu.Label>
                {filteredSnapshots.fps.length === 0 ? (
                  <Text size="xs" c="dimmed" px="sm" py={4}>
                    None
                  </Text>
                ) : (
                  filteredSnapshots.fps.map((fp) => {
                    const open = fp.id === fingerprintParam
                    return (
                      <Menu.Item
                        key={fp.id}
                        onClick={() => switchFingerprint(fp)}
                        rightSection={open ? <Badge size="xs">Open</Badge> : undefined}
                      >
                        {fingerprintMenuLabel(fp)}
                        <Text span size="xs" c="dimmed" ml={6}>
                          {fp.versionLabel}
                        </Text>
                      </Menu.Item>
                    )
                  })
                )}
              </ScrollArea.Autosize>
            </Menu.Dropdown>
          </Menu>
          {bom &&
            !editing &&
            !fingerprintParam &&
            bom.version.status === 'DRAFT' &&
            (boms.length <= 1 || selectedBomIds.length === 1) && (
            <Button size="sm" onClick={() => setEditing(true)}>
              Edit
            </Button>
          )}
          {bom && !fingerprintParam && (editing || versionDirty) && (
            <>
              <Button size="sm" disabled={!versionDirty || busy} onClick={() => void save()}>
                Save
              </Button>
              <Button size="sm" variant="default" disabled={busy} onClick={discard}>
                Discard
              </Button>
            </>
          )}
          {bom && bom.version.status === 'DRAFT' && !fingerprintParam && (
            <Button
              size="sm"
              variant="light"
              onClick={() => {
                setPromoteName('')
                setPromoteOpen(true)
              }}
            >
              Promote
            </Button>
          )}
          {bom && !fingerprintParam && (
            <Button size="sm" variant="light" onClick={openNewDraftModal}>
              New draft
            </Button>
          )}
          {bom && !fingerprintParam && (
            <Button size="sm" variant="light" onClick={openFingerprintModal}>
              Fingerprint
            </Button>
          )}
        </Group>
      </Group>
      {error && <Alert color="red">{error}</Alert>}
      {(versionDirty || payloadUnapplied) && (
        <Text size="xs" c="orange">
          {payloadUnapplied ? 'Unapplied component edits' : 'Unsaved changes'}
        </Text>
      )}
      <div style={{ flex: 1, minHeight: 0, display: 'flex', gap: 8 }}>
        <Paper withBorder p="xs" style={{ width: 260, flex: '0 0 260px', display: 'flex', flexDirection: 'column', minHeight: 0 }}>
          <Group justify="space-between" wrap="nowrap" mb="xs">
            <Text size="xs" fw={650}>
              Application assets
            </Text>
            <Group gap={4} wrap="nowrap">
              <Tooltip label="Expand all" withArrow>
                <ActionIcon
                  size="sm"
                  variant="subtle"
                  aria-label="Expand all"
                  disabled={grouped.length === 0}
                  onClick={() => setExpandedTypes(new Set(grouped.map(([type]) => type)))}
                >
                  <IconChevronsDown size={14} />
                </ActionIcon>
              </Tooltip>
              <Tooltip label="Collapse all" withArrow>
                <ActionIcon
                  size="sm"
                  variant="subtle"
                  aria-label="Collapse all"
                  disabled={grouped.length === 0}
                  onClick={() => setExpandedTypes(new Set())}
                >
                  <IconChevronsUp size={14} />
                </ActionIcon>
              </Tooltip>
              {writable && (
                <Tooltip label={paneChangesOnly ? 'Show all assets' : 'Changes only'} withArrow>
                  <ActionIcon
                    size="sm"
                    variant={paneChangesOnly ? 'filled' : 'subtle'}
                    aria-label="Changes only"
                    onClick={() => setPaneChangesOnly((v) => !v)}
                  >
                    <IconFilter size={14} />
                  </ActionIcon>
                </Tooltip>
              )}
              {writable && (
                <Menu position="bottom-end" withinPortal>
                  <Menu.Target>
                    <ActionIcon size="sm" variant="subtle" aria-label="Add asset">
                      <IconPlus size={14} />
                    </ActionIcon>
                  </Menu.Target>
                  <Menu.Dropdown>
                    {assetTypeOptions.map((t) => (
                      <Menu.Item key={t.type} onClick={() => openAddType(t.type)}>
                        {t.title || t.type}
                      </Menu.Item>
                    ))}
                  </Menu.Dropdown>
                </Menu>
              )}
            </Group>
          </Group>
          {multiBomChrome && (
            <Menu position="bottom-start" withinPortal width={240}>
              <Menu.Target>
                <div style={{ display: 'flex', marginBottom: 8 }}>
                  <Button
                    size="xs"
                    variant="light"
                    style={{ flex: 1, borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
                  >
                    <Text size="xs" truncate>
                      {isCombined
                        ? 'Combined SBOM'
                        : selectedBom?.name || `${selectedBomIds.length} BOMs`}
                    </Text>
                  </Button>
                  <Button
                    size="xs"
                    variant="light"
                    aria-label="Switch BOM"
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
                <Menu.Item
                  onClick={selectCombinedSbom}
                  rightSection={isCombined ? <Badge size="xs">Open</Badge> : undefined}
                >
                  Combined SBOM
                </Menu.Item>
                <Menu.Divider />
                {boms.map((row) => {
                  const checked = selectedBomIds.includes(row.id)
                  const onlyThis = selectedBomIds.length === 1 && checked
                  return (
                    <Menu.Item
                      key={row.id}
                      closeMenuOnClick={false}
                      onClick={() => selectSingleBom(row.id)}
                      leftSection={
                        <Checkbox
                          size="xs"
                          checked={checked}
                          onChange={(e) => {
                            e.stopPropagation()
                            toggleBomSelected(row.id, e.currentTarget.checked)
                          }}
                          onClick={(e) => e.stopPropagation()}
                        />
                      }
                      rightSection={onlyThis ? <Badge size="xs">Open</Badge> : undefined}
                    >
                      {row.name}
                    </Menu.Item>
                  )
                })}
              </Menu.Dropdown>
            </Menu>
          )}
          <SearchInput
            size="xs"
            mb="xs"
            placeholder="Filter by name"
            value={paneSearch}
            onValueChange={setPaneSearch}
          />
          <ScrollArea style={{ flex: 1 }} viewportRef={paneScrollRef}>
            <UnstyledButton
              onClick={selectApplication}
              style={{
                display: 'flex',
                width: '100%',
                padding: '6px 8px',
                borderRadius: 6,
                marginBottom: 4,
                alignItems: 'center',
                background: appSelected ? 'var(--mantine-color-blue-light)' : undefined,
              }}
            >
              <Text size="sm" fw={appSelected ? 650 : 600} truncate style={{ flex: 1, minWidth: 0 }}>
                {!multiBomChrome
                  ? app?.name || 'Application'
                  : exactlyOneBom && selectedBom
                    ? `${app?.name || 'Application'} / ${selectedBom.name}`
                    : isCombined && !appSelected
                      ? 'Combined SBOM'
                      : app?.name || 'Application'}
              </Text>
            </UnstyledButton>
            {multiBomChrome && (
              <Stack gap={2} mb="xs">
                <UnstyledButton
                  onClick={selectCombinedSbom}
                  style={{
                    display: 'flex',
                    width: '100%',
                    padding: '4px 8px 4px 16px',
                    borderRadius: 6,
                    alignItems: 'center',
                    background: isCombined ? 'var(--mantine-color-blue-light)' : undefined,
                  }}
                >
                  <Text size="xs" fw={isCombined ? 650 : 500} truncate style={{ flex: 1 }}>
                    Combined SBOM
                  </Text>
                  {isCombined && (
                    <Badge size="xs" variant="light">
                      Open
                    </Badge>
                  )}
                </UnstyledButton>
                {boms.map((row) => {
                  const checked = selectedBomIds.includes(row.id)
                  const onlyThis = selectedBomIds.length === 1 && checked
                  return (
                    <Group key={row.id} gap={6} wrap="nowrap" px={8} py={2} style={{ paddingLeft: 28 }}>
                      <Checkbox
                        size="xs"
                        checked={checked}
                        onChange={(e) => toggleBomSelected(row.id, e.currentTarget.checked)}
                        aria-label={`Select ${row.name}`}
                      />
                      <UnstyledButton
                        onClick={() => selectSingleBom(row.id)}
                        style={{ flex: 1, minWidth: 0, display: 'flex', alignItems: 'center', gap: 6 }}
                      >
                        <Text size="xs" truncate fw={onlyThis ? 650 : 400}>
                          {row.name}
                        </Text>
                        {onlyThis && (
                          <Badge size="xs" variant="light">
                            Open
                          </Badge>
                        )}
                      </UnstyledButton>
                    </Group>
                  )
                })}
              </Stack>
            )}
            {grouped.length === 0 && (
              <Text size="sm" c="dimmed" px="xs">
                {writable ? 'No assets yet. Add an existing asset or create a new one.' : 'No assets in this version.'}
              </Text>
            )}
            {grouped.length > 0 && groupedVisible.length === 0 && (
              <Text size="sm" c="dimmed" px="xs">
                {paneChangesOnly ? 'No pending changes.' : 'No matching assets.'}
              </Text>
            )}
            {groupedVisible.map(([type, items]) => {
              const open = expandedTypes.has(type) || paneSearch.trim().length > 0 || paneChangesOnly
              const typeIsSelected = selectedType === type && selectedAssetId == null && !appSelected
              const typeContainsSelection = selectedType === type && selectedAssetId != null
              const typeChanged = items.some((a) => isChanged(draft.assetStatus.get(a.id)))
              return (
                <div key={type}>
                  <UnstyledButton
                    onClick={() => selectType(type)}
                    style={{
                      display: 'flex',
                      width: '100%',
                      gap: 6,
                      alignItems: 'center',
                      padding: '6px 8px',
                      borderRadius: 6,
                      background: typeIsSelected
                        ? 'var(--mantine-color-blue-light)'
                        : typeContainsSelection
                          ? 'var(--mantine-color-default-hover)'
                          : undefined,
                    }}
                  >
                    <span
                      onClick={(e) => {
                        e.stopPropagation()
                        setExpandedTypes((prev) => {
                          const next = new Set(prev)
                          if (next.has(type)) next.delete(type)
                          else next.add(type)
                          return next
                        })
                      }}
                      style={{ display: 'flex' }}
                    >
                      {open ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />}
                    </span>
                    <Text
                      size="sm"
                      fw={typeIsSelected || typeContainsSelection || (editing && typeChanged) ? 700 : 500}
                      c={editing && typeChanged ? 'blue' : undefined}
                    >
                      {type} ({items.length})
                    </Text>
                  </UnstyledButton>
                  {open &&
                    items.map((a) => {
                      const kind = draft.assetStatus.get(a.id)
                      return (
                      <UnstyledButton
                        key={a.id}
                        data-asset-id={a.id}
                        onClick={() => selectAsset(a.id)}
                        style={{
                          display: 'flex',
                          width: '100%',
                          padding: '4px 8px 4px 28px',
                          borderRadius: 6,
                          alignItems: 'center',
                          background:
                            a.id === selectedAssetId ? 'var(--mantine-color-blue-light)' : undefined,
                          textDecoration: kind === 'deleted' ? 'line-through' : undefined,
                        }}
                      >
                        <Text
                          size="sm"
                          truncate
                          fw={a.id === selectedAssetId || (editing && isChanged(kind)) ? 700 : 400}
                          c={editing && isChanged(kind) ? 'blue' : undefined}
                          style={{ flex: 1, minWidth: 0 }}
                        >
                          {a.label}
                        </Text>
                      </UnstyledButton>
                    )})}
                </div>
              )
            })}
          </ScrollArea>
        </Paper>
        <Paper withBorder p="sm" style={{ flex: 1, minWidth: 0, minHeight: 0, display: 'flex', flexDirection: 'column', position: 'relative' }}>
          <LoadingOverlay
            visible={contentLoading && tab === 'assets'}
            zIndex={20}
            overlayProps={{ blur: 1, backgroundOpacity: 0.35 }}
            loaderProps={{ size: 'sm' }}
          />
          <Tabs
            value={tab}
            onChange={(v) => {
              const next = new URLSearchParams(params)
              next.set('tab', v || 'assets')
              setParams(next)
            }}
            style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
            keepMounted={false}
          >
            <Group justify="space-between" wrap="nowrap" align="center">
              <Tabs.List>
                <Tabs.Tab value="assets">{appSelected ? 'Application' : 'Assets'}</Tabs.Tab>
                <Tabs.Tab value="graph">Graph</Tabs.Tab>
              </Tabs.List>
              <Group gap="sm" wrap="nowrap">
                  {tab === 'assets' && selectedAsset && (
                    <Button
                      size="xs"
                      variant="light"
                      onClick={() => showOnGraph(selectedAsset.id)}
                    >
                      Show on graph
                    </Button>
                  )}
                  {tab === 'graph' && (
                    <>
                  <Group gap={6} wrap="nowrap">
                    <Text size="xs" c="dimmed">
                      View mode
                    </Text>
                    <Select
                      size="xs"
                      w={120}
                      value={graphViewMode}
                      onChange={(v) => setGraphViewMode((v as GraphViewMode) || 'details')}
                      data={[
                        { value: 'details', label: 'Details' },
                        { value: 'minimal', label: 'Minimal' },
                      ]}
                      allowDeselect={false}
                    />
                  </Group>
                  <Divider orientation="vertical" />
                  <Group gap="xs" wrap="nowrap">
                    <Switch
                      size="xs"
                      label="Navigate to selected"
                      checked={panToSelection}
                      onChange={(e) => setPanToSelection(e.currentTarget.checked)}
                    />
                    {!panToSelection && (
                      <Button
                        size="xs"
                        variant="light"
                        disabled={!selectedAssetId}
                        onClick={() => selectedAssetId && graphRef.current?.focusNode(selectedAssetId)}
                      >
                        To selection
                      </Button>
                    )}
                  </Group>
                  <Divider orientation="vertical" />
                  <Group gap={0} wrap="nowrap">
                    <Button
                      size="xs"
                      variant="light"
                      onClick={() => graphRef.current?.applyLayout(graphLayout)}
                      style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
                    >
                      Apply layout
                    </Button>
                    <Menu position="bottom-end" withinPortal>
                      <Menu.Target>
                        <Button
                          size="xs"
                          variant="light"
                          aria-label="Choose graph layout"
                          px="xs"
                          style={{
                            borderTopLeftRadius: 0,
                            borderBottomLeftRadius: 0,
                            borderLeft: '1px solid var(--mantine-color-default-border)',
                          }}
                        >
                          ▾
                        </Button>
                      </Menu.Target>
                      <Menu.Dropdown>
                        <Menu.Label>Layout direction</Menu.Label>
                        {GRAPH_LAYOUTS.map((option) => (
                          <Menu.Item
                            key={option.value}
                            onClick={() => {
                              setGraphLayout(option.value)
                              if (option.value === graphLayout) {
                                graphRef.current?.applyLayout(option.value)
                              } else {
                                requestAnimationFrame(() => graphRef.current?.applyLayout(option.value))
                              }
                            }}
                          >
                            {option.value === graphLayout ? '✓ ' : ''}
                            {option.label}
                          </Menu.Item>
                        ))}
                      </Menu.Dropdown>
                    </Menu>
                  </Group>
                  {editing && (
                    <>
                      <Divider orientation="vertical" />
                      <Switch
                        size="xs"
                        label="Changes only"
                        checked={changesOnly}
                        onChange={(e) => setChangesOnly(e.currentTarget.checked)}
                      />
                    </>
                  )}
                    </>
                  )}
              </Group>
            </Group>
            {tab === 'graph' && grouped.length > 0 && (
              <Group gap="xs" wrap="wrap" mt="xs">
                {grouped.map(([type]) => {
                  const color = colorForType(type)
                  const active = highlightedTypes.has(type)
                  const filtering = highlightedTypes.size > 0
                  return (
                    <Badge
                      key={type}
                      size="sm"
                      variant={active ? 'filled' : 'outline'}
                      color="gray"
                      leftSection={<span style={{ color: active ? '#fff' : color, lineHeight: 1 }}>●</span>}
                      onClick={() => {
                        setHighlightedTypes((prev) => {
                          const next = new Set(prev)
                          if (next.has(type)) next.delete(type)
                          else next.add(type)
                          return next
                        })
                      }}
                      style={{
                        cursor: 'pointer',
                        background: active ? color : undefined,
                        borderColor: color,
                        color: active ? '#fff' : undefined,
                        opacity: filtering && !active ? 0.45 : 1,
                        userSelect: 'none',
                      }}
                    >
                      {type}
                    </Badge>
                  )
                })}
                {highlightedTypes.size > 0 && (
                  <Tooltip label="Clear type highlight" withArrow>
                    <ActionIcon
                      size="sm"
                      variant="subtle"
                      color="gray"
                      aria-label="Clear type highlight"
                      onClick={() => setHighlightedTypes(new Set())}
                    >
                      <IconX size={14} />
                    </ActionIcon>
                  </Tooltip>
                )}
              </Group>
            )}
            <Tabs.Panel value="assets" pt="sm" style={{ flex: 1, minHeight: 0, overflow: 'auto', display: tab === 'assets' ? 'block' : 'none' }}>
              {selectedAsset ? (
                <Stack gap="sm">
                  <Group justify="space-between" wrap="nowrap" align="flex-start">
                    <div>
                      <Title order={4}>{selectedAsset.label}</Title>
                      <Text size="sm" c="dimmed">
                        {selectedAsset.type} · {selectedAsset.schemaVersion}
                      </Text>
                    </div>
                    <Group gap={4} wrap="nowrap">
                      {writable && (
                        <>
                          <Menu position="bottom-end" withinPortal>
                            <Menu.Target>
                              <Button size="xs" variant="light">
                                Replace
                              </Button>
                            </Menu.Target>
                            <Menu.Dropdown>
                              <Menu.Item onClick={() => openAddType(selectedAsset.type, 'replace')}>
                                Existing
                              </Menu.Item>
                              <Menu.Item onClick={() => openCreateType(selectedAsset.type, 'replace')}>
                                Create
                              </Menu.Item>
                            </Menu.Dropdown>
                          </Menu>
                          <Button
                            size="xs"
                            color="red"
                            variant="light"
                            leftSection={<IconTrash size={14} />}
                            onClick={() => askDeleteAsset(selectedAsset)}
                          >
                            Delete
                          </Button>
                        </>
                      )}
                      {writable && isChanged(draft.assetStatus.get(selectedAsset.id)) && (
                        <>
                          <DraftStatusPill status={draft.assetStatus.get(selectedAsset.id)} />
                          {draft.assetStatus.get(selectedAsset.id) === 'modified' && (
                            <Tooltip label="Revert fields" withArrow>
                              <ActionIcon
                                size="sm"
                                variant="subtle"
                                aria-label="Revert fields"
                                onClick={() => revertAssetPayload(selectedAsset.id)}
                              >
                                <IconArrowBackUp size={16} />
                              </ActionIcon>
                            </Tooltip>
                          )}
                        </>
                      )}
                    </Group>
                  </Group>
                  {viewSchema && editPayload ? (
                    <Stack gap="sm">
                      <SchemaPayloadView
                        schema={viewSchema.contentSchema}
                        value={editPayload}
                        editable={writable}
                        onChange={writable ? setEditPayload : undefined}
                      />
                      {writable && (
                        <Group justify="flex-end">
                          <Button
                            size="xs"
                            disabled={!payloadUnapplied}
                            onClick={applyPayload}
                          >
                            Apply
                          </Button>
                        </Group>
                      )}
                    </Stack>
                  ) : (
                    <Text size="sm" c="dimmed">
                      Loading fields…
                    </Text>
                  )}
                  <RelatedAssetsBlock
                    writable={writable}
                    viewMode={relatedViewMode}
                    onViewModeChange={setRelatedViewMode}
                    specs={roleSpecs}
                    selectedAssetId={selectedAsset.id}
                    assets={editing ? draft.graphAssets : workingAssets}
                    relations={editing ? draft.graphRelations : workingRels}
                    relationStatus={draft.relationStatus}
                    outgoingTab={relatedOutgoingTab}
                    incomingTab={relatedIncomingTab}
                    onOutgoingTabChange={setRelatedOutgoingTab}
                    onIncomingTabChange={setRelatedIncomingTab}
                    onSelectAsset={(assetId) => selectAsset(assetId)}
                    onAdd={(spec) => setRelOpen({ mode: 'relate', spec })}
                    onCreate={(spec) => setCreateRelOpen({ mode: 'relate', spec })}
                    onRemove={removeRelation}
                    onRevert={revertRelation}
                  />
                </Stack>
              ) : selectedType ? (
                <Stack gap="sm">
                  <Group justify="space-between">
                    <Title order={4}>{selectedType}</Title>
                    {writable && (
                      <Group gap="xs">
                        <Button size="xs" variant="light" onClick={() => openAddType(selectedType)}>
                          Add asset
                        </Button>
                        <Button size="xs" variant="light" onClick={() => openCreateType(selectedType)}>
                          Create asset
                        </Button>
                      </Group>
                    )}
                  </Group>
                  <Table striped highlightOnHover>
                    <Table.Thead>
                      <Table.Tr>
                        <Table.Th>Name</Table.Th>
                        <Table.Th>Type</Table.Th>
                      </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                      {typeAssets.map((a) => (
                        <Table.Tr key={a.id} onClick={() => selectAsset(a.id)} style={{ cursor: 'pointer' }}>
                          <Table.Td>{a.label}</Table.Td>
                          <Table.Td>{a.type}</Table.Td>
                        </Table.Tr>
                      ))}
                    </Table.Tbody>
                  </Table>
                </Stack>
              ) : (
                <Stack gap="md">
                  {writable && (
                    <Group justify="flex-end">
                      <TypeSplitButton label="Add asset" types={assetTypeOptions} onPick={(type) => openAddType(type)} />
                      <TypeSplitButton label="Create asset" types={assetTypeOptions} onPick={(type) => openCreateType(type)} />
                    </Group>
                  )}
                  <Paper withBorder radius="md" p="md">
                    <Stack gap="md">
                      {editing ? (
                        <>
                          <TextInput
                            label="Name"
                            size="sm"
                            value={editName}
                            onChange={(e) => setEditName(e.currentTarget.value)}
                          />
                          <Textarea
                            label="Description"
                            size="sm"
                            autosize
                            minRows={2}
                            value={editDescription}
                            onChange={(e) => setEditDescription(e.currentTarget.value)}
                          />
                        </>
                      ) : (
                        <>
                          <MetaField label="Name">{app?.name || ''}</MetaField>
                          <MetaField label="Description">{app?.description || ''}</MetaField>
                        </>
                      )}
                      <TagsInput
                        label="Tags"
                        placeholder="Add a tag"
                        size="sm"
                        value={editAppTags}
                        onChange={setEditAppTags}
                        clearable
                      />
                      {bom && !fingerprintView && (
                        bom.version.status === 'DRAFT' ? (
                          <>
                            <TextInput
                              label="Target version"
                              size="sm"
                              required
                              value={editTargetVersion}
                              onChange={(e) => setEditTargetVersion(e.currentTarget.value)}
                            />
                            <TagsInput
                              label="Version tags"
                              placeholder="Add a tag"
                              size="sm"
                              value={editVersionTags}
                              onChange={setEditVersionTags}
                              clearable
                            />
                          </>
                        ) : (
                          <MetaField label="Version">{bom.version.version || bom.version.label || '—'}</MetaField>
                        )
                      )}
                      {selectedBom && boms.length >= 2 && !fingerprintView && bom?.version.status === 'DRAFT' && (
                        <>
                          <TextInput
                            label="BOM name"
                            size="sm"
                            value={editBomName}
                            onChange={(e) => setEditBomName(e.currentTarget.value)}
                          />
                          <Textarea
                            label="BOM description"
                            size="sm"
                            autosize
                            minRows={2}
                            value={editBomDescription}
                            onChange={(e) => setEditBomDescription(e.currentTarget.value)}
                          />
                          <TagsInput
                            label="BOM tags"
                            placeholder="Add a tag"
                            size="sm"
                            value={editBomTags}
                            onChange={setEditBomTags}
                            clearable
                          />
                        </>
                      )}
                    </Stack>
                  </Paper>
                  {bom && bom.version.status === 'DRAFT' && !fingerprintView && (
                    <Group justify="flex-start">
                      <Button size="sm" variant="light" onClick={openCreateBomModal}>
                        Create BOM
                      </Button>
                    </Group>
                  )}
                  {multiBomChrome && (
                    <Table striped highlightOnHover>
                      <Table.Thead>
                        <Table.Tr>
                          <Table.Th>BOM</Table.Th>
                          <Table.Th>Status</Table.Th>
                          <Table.Th />
                        </Table.Tr>
                      </Table.Thead>
                      <Table.Tbody>
                        <Table.Tr
                          style={{
                            background: isCombined ? 'var(--mantine-color-blue-light)' : undefined,
                            cursor: 'pointer',
                          }}
                          onClick={selectCombinedSbom}
                        >
                          <Table.Td>
                            <Group gap="xs" wrap="nowrap">
                              <Text size="sm" fw={600}>
                                Combined SBOM
                              </Text>
                              {isCombined && (
                                <Badge size="xs" variant="light">
                                  Open
                                </Badge>
                              )}
                            </Group>
                          </Table.Td>
                          <Table.Td>
                            <Text size="sm" c="dimmed">
                              All BOMs
                            </Text>
                          </Table.Td>
                          <Table.Td />
                        </Table.Tr>
                        {boms.map((row) => {
                          const open = selectedBomIds.length === 1 && selectedBomIds[0] === row.id
                          return (
                            <Table.Tr
                              key={row.id}
                              style={{
                                background: open ? 'var(--mantine-color-blue-light)' : undefined,
                                cursor: 'pointer',
                              }}
                              onClick={() => selectSingleBom(row.id)}
                            >
                              <Table.Td>
                                <Group gap="xs" wrap="nowrap" pl="md">
                                  <Text size="sm">{row.name}</Text>
                                  {open && (
                                    <Badge size="xs" variant="light">
                                      Open
                                    </Badge>
                                  )}
                                </Group>
                              </Table.Td>
                              <Table.Td>
                                <Text size="sm" c="dimmed">
                                  BOM
                                </Text>
                              </Table.Td>
                              <Table.Td>
                                <Button
                                  size="xs"
                                  color="red"
                                  variant="subtle"
                                  leftSection={<IconTrash size={14} />}
                                  onClick={(e) => {
                                    e.stopPropagation()
                                    askDeleteBom(row)
                                  }}
                                >
                                  Delete
                                </Button>
                              </Table.Td>
                            </Table.Tr>
                          )
                        })}
                      </Table.Tbody>
                    </Table>
                  )}
                  <Tabs
                    value={appCatalogTab}
                    onChange={(v) => setAppCatalogTab((v as 'versions' | 'fingerprints') || 'versions')}
                  >
                    <Tabs.List>
                      <Tabs.Tab value="versions">Versions</Tabs.Tab>
                      <Tabs.Tab value="fingerprints">Fingerprints</Tabs.Tab>
                    </Tabs.List>
                    <Tabs.Panel value="versions" pt="sm">
                      {versions.length === 0 ? (
                        <Text size="sm" c="dimmed">
                          No versions yet.
                        </Text>
                      ) : (
                        <Table striped highlightOnHover stickyHeader>
                          <Table.Thead>
                            <Table.Tr>
                              <Table.Th>Version</Table.Th>
                              <Table.Th>Status</Table.Th>
                              <Table.Th>Based on</Table.Th>
                              <Table.Th>Captured</Table.Th>
                              <Table.Th />
                            </Table.Tr>
                          </Table.Thead>
                          <Table.Tbody>
                            {versions.map((v) => {
                              const open = versionOpen && v.id === versionId
                              return (
                              <Table.Tr
                                key={v.id}
                                style={{
                                  background: open ? 'var(--mantine-color-blue-light)' : undefined,
                                }}
                              >
                                <Table.Td>
                                  <Group gap="xs" wrap="nowrap">
                                    <Anchor
                                      component="button"
                                      type="button"
                                      size="sm"
                                      fw={600}
                                      onClick={() => switchVersion(v.id)}
                                    >
                                      {versionLabel(v)}
                                    </Anchor>
                                    {open && (
                                      <Badge size="xs" variant="light">
                                        Open
                                      </Badge>
                                    )}
                                  </Group>
                                </Table.Td>
                                <Table.Td>
                                  <Text size="sm" c="dimmed">
                                    {v.status}
                                  </Text>
                                </Table.Td>
                                <Table.Td>
                                  <Text size="sm" c="dimmed">
                                    {basedOnLabel(v)}
                                  </Text>
                                </Table.Td>
                                <Table.Td>
                                  <Text size="sm" c="dimmed">
                                    {new Date(v.capturedAt).toLocaleString()}
                                  </Text>
                                </Table.Td>
                                <Table.Td>
                                  {v.status === 'DRAFT' && (
                                    <Button
                                      size="xs"
                                      color="red"
                                      variant="subtle"
                                      leftSection={<IconTrash size={14} />}
                                      onClick={(e) => {
                                        e.stopPropagation()
                                        askDeleteDraft(v)
                                      }}
                                    >
                                      Delete
                                    </Button>
                                  )}
                                </Table.Td>
                              </Table.Tr>
                            )
                            })}
                          </Table.Tbody>
                        </Table>
                      )}
                    </Tabs.Panel>
                    <Tabs.Panel value="fingerprints" pt="sm">
                      {fingerprints.length === 0 ? (
                        <Text size="sm" c="dimmed">
                          No fingerprints yet.
                        </Text>
                      ) : (
                        <Table striped highlightOnHover stickyHeader>
                          <Table.Thead>
                            <Table.Tr>
                              <Table.Th>Name</Table.Th>
                              <Table.Th>Category</Table.Th>
                              <Table.Th>Version</Table.Th>
                              <Table.Th>Created</Table.Th>
                              <Table.Th>SHA-256</Table.Th>
                            </Table.Tr>
                          </Table.Thead>
                          <Table.Tbody>
                            {fingerprints.map((fp) => {
                              const open = fp.id === fingerprintParam
                              return (
                              <Table.Tr
                                key={fp.id}
                                style={{
                                  background: open ? 'var(--mantine-color-blue-light)' : undefined,
                                }}
                              >
                                <Table.Td>
                                  <Group gap="xs" wrap="nowrap">
                                    <Anchor
                                      component="button"
                                      type="button"
                                      size="sm"
                                      fw={600}
                                      onClick={() => switchFingerprint(fp)}
                                    >
                                      {fingerprintTitle(fp)}
                                    </Anchor>
                                    {open && (
                                      <Badge size="xs" variant="light">
                                        Open
                                      </Badge>
                                    )}
                                  </Group>
                                </Table.Td>
                                <Table.Td>
                                  <Text size="sm" c="dimmed">
                                    {fingerprintCategory(fp)}
                                  </Text>
                                </Table.Td>
                                <Table.Td>
                                  <Text size="sm" c="dimmed">
                                    {fp.versionLabel}
                                  </Text>
                                </Table.Td>
                                <Table.Td>
                                  <Text size="sm" c="dimmed">
                                    {new Date(fp.createdAt).toLocaleString()}
                                  </Text>
                                </Table.Td>
                                <Table.Td>
                                  <Text size="sm" c="dimmed" ff="monospace">
                                    {fp.contentSha256.slice(0, 12)}…
                                  </Text>
                                </Table.Td>
                              </Table.Tr>
                            )
                            })}
                          </Table.Tbody>
                        </Table>
                      )}
                    </Tabs.Panel>
                  </Tabs>
                </Stack>
              )}
            </Tabs.Panel>
            <Tabs.Panel
              value="graph"
              pt="sm"
              style={{
                flex: 1,
                minHeight: 0,
                display: tab === 'graph' ? 'flex' : 'none',
                flexDirection: 'column',
              }}
            >
              <div style={{ flex: 1, minHeight: 0 }}>
                <SbomGraphCanvas
                  ref={graphRef}
                  assets={editing ? draft.graphAssets : workingAssets}
                  relations={editing ? draft.graphRelations : workingRels}
                  selectedAssetId={selectedAssetId}
                  visible={tab === 'graph'}
                  viewMode={graphViewMode}
                  highlightedTypes={highlightedTypes}
                  assetStatus={editing ? draft.assetStatus : undefined}
                  relationStatus={editing ? draft.relationStatus : undefined}
                  changesOnly={editing && changesOnly}
                  onSelectAsset={(assetId) => {
                    if (!assetId) {
                      selectApplication()
                      return
                    }
                    selectAsset(assetId, { pan: false })
                  }}
                />
              </div>
            </Tabs.Panel>
          </Tabs>
        </Paper>
      </div>

      <Modal opened={promoteOpen} onClose={() => setPromoteOpen(false)} title="Promote draft">
        <Stack>
          <Text size="sm" c="dimmed">
            Re-type the version to confirm. You may override the current target
            {bom?.version.version ? ` (${bom.version.version})` : ''} if the new value is unique.
          </Text>
          <TextInput
            label="Version"
            required
            value={promoteName}
            onChange={(e) => setPromoteName(e.currentTarget.value)}
          />
          <Button
            disabled={!promoteName.trim() || !bom}
            onClick={() => {
              if (!bom) return
              void api
                .promoteVersion(id, bom.version.id, promoteName.trim())
                .then(() => {
                  setPromoteOpen(false)
                  return load(bom.version.id)
                })
                .catch((e) => setError(e instanceof Error ? e.message : 'Promote failed'))
            }}
          >
            Promote
          </Button>
        </Stack>
      </Modal>
      <Modal opened={draftOpen} onClose={() => setDraftOpen(false)} title="New draft">
        <Stack>
          <Select
            label="Based on"
            required
            searchable
            data={draftSourceOptions}
            value={draftFrom}
            onChange={(value) => {
              setDraftFrom(value)
              setDraftCombine(false)
            }}
          />
          <TextInput
            label="Target version"
            required
            placeholder="1.0.0"
            value={draftTarget}
            onChange={(e) => setDraftTarget(e.currentTarget.value)}
          />
          {draftFrom?.startsWith('v:') && draftFromBomCount > 1 && (
            <Checkbox
              label="Combine into a single BOM"
              checked={draftCombine}
              onChange={(e) => setDraftCombine(e.currentTarget.checked)}
            />
          )}
          <Button disabled={!draftTarget.trim() || !draftFrom || busy} onClick={() => void submitNewDraft()}>
            Create draft
          </Button>
        </Stack>
      </Modal>
      <Modal opened={fpOpen} onClose={() => setFpOpen(false)} title="Create fingerprint">
        <Stack>
          <TextInput
            label="Name"
            required
            value={fpName}
            onChange={(e) => setFpName(e.currentTarget.value)}
          />
          <Select
            label="Category"
            required
            data={FINGERPRINT_CATEGORIES}
            value={fpCategory}
            onChange={setFpCategory}
            allowDeselect={false}
          />
          <Button disabled={!fpName.trim() || !fpCategory || busy} onClick={() => void submitFingerprint()}>
            Create fingerprint
          </Button>
        </Stack>
      </Modal>
      <Modal opened={createBomOpen} onClose={() => setCreateBomOpen(false)} title="Create BOM">
        <Stack>
          <TextInput
            label="Name"
            required
            value={newBomName}
            onChange={(e) => setNewBomName(e.currentTarget.value)}
          />
          <Textarea
            label="Description"
            autosize
            minRows={2}
            value={newBomDescription}
            onChange={(e) => setNewBomDescription(e.currentTarget.value)}
          />
          <TagsInput
            label="Tags"
            placeholder="Add a tag"
            value={newBomTags}
            onChange={setNewBomTags}
            clearable
          />
          <Button disabled={!newBomName.trim() || busy} onClick={() => void submitCreateBom()}>
            Create BOM
          </Button>
        </Stack>
      </Modal>
      <Modal opened={addOpen} onClose={() => setAddOpen(false)} title="Add asset">
        <Stack>
          <Select
            label="Asset type"
            placeholder="Any type"
            searchable
            clearable={addMode === 'existing'}
            data={assetTypeOptions.map((t) => ({ value: t.type, label: t.title || t.type }))}
            value={addType}
            onChange={setAddType}
          />
          <SegmentedControl
            value={addMode}
            onChange={(v) => setAddMode((v as AddAssetMode) || 'existing')}
            data={[
              { value: 'existing', label: 'Add existing' },
              { value: 'create', label: 'Create new' },
            ]}
          />
          {addMode === 'existing' ? (
            <>
              <SearchInput
                label="Search registry"
                value={addSearch}
                onValueChange={setAddSearch}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') void searchAddRegistry()
                }}
              />
              <Button variant="light" onClick={() => void searchAddRegistry()}>
                Search
              </Button>
              {addHits.length === 0 ? (
                <Text size="sm" c="dimmed">
                  Search to find assets to add to this application.
                </Text>
              ) : (
                addHits.map((a) => (
                  <Button key={a.id} variant="subtle" justify="flex-start" onClick={() => includeAsset(a)}>
                    {a.label}
                    <Text span size="xs" c="dimmed" ml={6}>
                      {a.type}
                    </Text>
                  </Button>
                ))
              )}
            </>
          ) : (
            <>
              {!addType && (
                <Text size="sm" c="dimmed">
                  Choose an asset type to create a new asset.
                </Text>
              )}
              {addType &&
                (requiredScalarFields(addSchema).length > 0 ? (
                  requiredScalarFields(addSchema).map((field) =>
                    field.schema.type === 'ENUM' && field.schema.values?.length ? (
                      <Select
                        key={field.name}
                        label={field.schema.title || field.name}
                        required
                        data={field.schema.values.map((v) => ({ value: v.value, label: v.value }))}
                        value={addFields[field.name] ?? ''}
                        onChange={(v) => setAddFields((prev) => ({ ...prev, [field.name]: v || '' }))}
                      />
                    ) : (
                      <TextInput
                        key={field.name}
                        label={field.schema.title || field.name}
                        required
                        value={addFields[field.name] ?? ''}
                        onChange={(e) => setAddFields((prev) => ({ ...prev, [field.name]: e.currentTarget.value }))}
                      />
                    ),
                  )
                ) : (
                  <TextInput label="Name" required value={addName} onChange={(e) => setAddName(e.currentTarget.value)} />
                ))}
              <Button disabled={!addType || busy} onClick={() => void addNewAsset()}>
                Create and add
              </Button>
            </>
          )}
        </Stack>
      </Modal>
      <AddRelatedAssetsDialog
        opened={!!relOpen}
        spec={relOpen?.spec ?? null}
        mode={relOpen?.mode}
        selectedAssetId={selectedAsset?.id ?? null}
        assets={workingAssets}
        relations={workingRels}
        onClose={() => setRelOpen(null)}
        onAdd={onPickAdd}
      />
      <CreateRelatedAssetDialog
        opened={!!createRelOpen}
        spec={createRelOpen?.spec ?? null}
        mode={createRelOpen?.mode}
        selectedAssetId={selectedAsset?.id ?? null}
        assets={workingAssets}
        relations={workingRels}
        ownerName={app?.name}
        onClose={() => setCreateRelOpen(null)}
        onUseExisting={onPickCreateExisting}
        onCreated={onPickCreated}
      />
      <BomImpactDialog plan={impact} onClose={() => setImpact(null)} onConfirm={applyImpact} />
      <Modal opened={!!confirm} onClose={() => setConfirm(null)} title={confirm?.title} centered>
        <Stack>
          <Text size="sm" style={{ whiteSpace: 'pre-wrap' }}>
            {confirm?.message}
          </Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setConfirm(null)}>
              {confirm?.cancelLabel || 'Cancel'}
            </Button>
            {confirm?.onDiscard && (
              <Button
                variant="light"
                color="red"
                onClick={() => {
                  const action = confirm.onDiscard
                  setConfirm(null)
                  action?.()
                }}
              >
                {confirm.discardLabel || 'Discard'}
              </Button>
            )}
            <Button
              color={confirm?.color}
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
    </Stack>
  )
}
