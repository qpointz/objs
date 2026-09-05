import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  Alert,
  Box,
  Button,
  Code,
  Group,
  Menu,
  Modal,
  Paper,
  ScrollArea,
  Select,
  Stack,
  Switch,
  Text,
  TextInput,
  Tooltip,
} from '@mantine/core'
import { IconChevronDown, IconChevronRight, IconPlus } from '@tabler/icons-react'
import { useGraphContext } from './GraphContextProvider'
import {
  createSuite,
  deleteSuite,
  evaluateSuite,
  fetchPolicyCapabilities,
  fetchSuiteSelection,
  listPolicies,
  listSuites,
  updateSuite,
} from './policyApi'
import type {
  Policy,
  PolicySuite,
  SuiteEvaluationResult,
  SuiteFolder,
  SuiteMatcher,
  SuiteSelectionResult,
} from './policyTypes'
import { formatPolicyVersion } from './policyTypes'
import { policyFragmentMatcher } from './queryGraphContext'
import { VIEW_ACTION_BUTTON_SIZE } from './viewActionButtons'

type NavSel =
  | { kind: 'suite'; id: string }
  | { kind: 'folder'; suiteId: string; folderId: string }

function newId(): string {
  return crypto.randomUUID()
}

function emptySuite(): PolicySuite {
  const rootId = newId()
  return {
    name: 'New suite',
    rollUpStrategyKind: 'BUILTIN',
    executionStrategyKind: 'DEDUPE',
    folders: [
      {
        id: rootId,
        key: 'root',
        name: 'Root',
        participation: 'ENABLED',
        rollUpMode: 'ALL_PASS',
        matchers: [],
      },
    ],
    tags: [],
    annotations: {},
  }
}

function cloneSuite(s: PolicySuite): PolicySuite {
  return JSON.parse(JSON.stringify(s)) as PolicySuite
}

export function PolicySuitesPage() {
  const { context } = useGraphContext()
  const [capable, setCapable] = useState<boolean | null>(null)
  const [suites, setSuites] = useState<PolicySuite[]>([])
  const [policies, setPolicies] = useState<Policy[]>([])
  const [nav, setNav] = useState<NavSel | null>(null)
  const [draft, setDraft] = useState<PolicySuite | null>(null)
  const [dirty, setDirty] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [expanded, setExpanded] = useState<Set<string>>(() => new Set())
  const [selection, setSelection] = useState<SuiteSelectionResult | null>(null)
  const [evalResult, setEvalResult] = useState<SuiteEvaluationResult | null>(null)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [leftWidth] = useState(260)

  const refresh = useCallback(async () => {
    const caps = await fetchPolicyCapabilities()
    setCapable(caps != null && (caps.operations?.includes('suites') ?? false))
    if (!caps) return
    const [s, p] = await Promise.all([listSuites(), listPolicies()])
    setSuites(s)
    setPolicies(p)
  }, [])

  useEffect(() => {
    void refresh().catch((e: unknown) =>
      setError(e instanceof Error ? e.message : String(e)),
    )
  }, [refresh])

  const selectedSuiteId =
    nav?.kind === 'suite' ? nav.id : nav?.kind === 'folder' ? nav.suiteId : null
  const selectedSuite =
    draft?.id && draft.id === selectedSuiteId
      ? draft
      : suites.find((s) => s.id === selectedSuiteId) ?? draft

  const selectedFolder =
    nav?.kind === 'folder' && selectedSuite
      ? selectedSuite.folders?.find((f) => f.id === nav.folderId) ?? null
      : null

  function applyNav(next: NavSel | null, suiteOverride?: PolicySuite | null) {
    setNav(next)
    setSelection(null)
    setEvalResult(null)
    if (next?.kind === 'suite' || next?.kind === 'folder') {
      const sid = next.kind === 'suite' ? next.id : next.suiteId
      const s = suiteOverride ?? suites.find((x) => x.id === sid) ?? null
      setDraft(s ? cloneSuite(s) : null)
      setDirty(false)
      if (s?.id) setExpanded((prev) => new Set(prev).add(s.id!))
    } else {
      setDraft(null)
      setDirty(false)
    }
  }

  function markDirty(next: PolicySuite) {
    setDraft(next)
    setDirty(true)
  }

  async function onAddSuite() {
    setBusy(true)
    setError(null)
    try {
      const created = await createSuite(emptySuite())
      await refresh()
      applyNav({ kind: 'suite', id: created.id! }, created)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  function onAddFolder() {
    if (!draft?.id) return
    const root = draft.folders?.find((f) => !f.parentId)
    const parentId =
      nav?.kind === 'folder' ? nav.folderId : root?.id ?? null
    if (!parentId) return
    const id = newId()
    const folder: SuiteFolder = {
      id,
      key: `folder-${(draft.folders?.length ?? 0) + 1}`,
      name: 'New folder',
      parentId,
      participation: 'ENABLED',
      rollUpMode: 'ALL_PASS',
      matchers: [],
    }
    markDirty({ ...draft, folders: [...(draft.folders ?? []), folder] })
    setExpanded((prev) => new Set(prev).add(draft.id!).add(parentId))
    setNav({ kind: 'folder', suiteId: draft.id, folderId: id })
  }

  async function onSave() {
    if (!draft?.id) return
    setBusy(true)
    setError(null)
    try {
      const updated = await updateSuite(draft.id, draft)
      await refresh()
      applyNav(
        nav?.kind === 'folder'
          ? { kind: 'folder', suiteId: updated.id!, folderId: nav.folderId }
          : { kind: 'suite', id: updated.id! },
        updated,
      )
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  async function onDelete() {
    if (!draft?.id) return
    setBusy(true)
    setError(null)
    try {
      if (nav?.kind === 'folder') {
        const drop = new Set<string>([nav.folderId])
        let changed = true
        while (changed) {
          changed = false
          for (const f of draft.folders ?? []) {
            if (f.parentId && drop.has(f.parentId) && f.id && !drop.has(f.id)) {
              drop.add(f.id)
              changed = true
            }
          }
        }
        const nextFolders = (draft.folders ?? []).filter((f) => !f.id || !drop.has(f.id))
        if (!nextFolders.some((f) => !f.parentId)) {
          setError('Cannot delete the only root folder')
          return
        }
        markDirty({ ...draft, folders: nextFolders })
        setNav({ kind: 'suite', id: draft.id })
      } else {
        await deleteSuite(draft.id)
        await refresh()
        applyNav(null)
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setConfirmDelete(false)
      setBusy(false)
    }
  }

  async function onExamine() {
    if (!draft?.id) return
    setBusy(true)
    setError(null)
    try {
      // Persist draft first so server selection matches editor
      const saved = dirty ? await updateSuite(draft.id, draft) : draft
      if (dirty) {
        await refresh()
        setDraft(cloneSuite(saved))
        setDirty(false)
      }
      const scope = nav?.kind === 'folder' ? 'SUBFOLDER' : 'FULL'
      const result = await fetchSuiteSelection({
        suiteId: saved.id!,
        scope,
        folderId: nav?.kind === 'folder' ? nav.folderId : null,
      })
      setSelection(result)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  async function onEvaluate() {
    if (!draft?.id) return
    if (context.kind === 'empty') {
      setError('Open a graph or matcher (Matcher / All) in the shared context before Evaluate')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const saved = dirty ? await updateSuite(draft.id, draft) : draft
      if (dirty) {
        await refresh()
        setDraft(cloneSuite(saved))
        setDirty(false)
      }
      const scope = policyFragmentMatcher(context)
      const result = await evaluateSuite({
        suiteId: saved.id!,
        ...scope,
        scope: nav?.kind === 'folder' ? 'SUBFOLDER' : 'FULL',
        folderId: nav?.kind === 'folder' ? nav.folderId : null,
      })
      setEvalResult(result)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const folderChildren = useMemo(() => {
    const map = new Map<string | null, SuiteFolder[]>()
    for (const f of draft?.folders ?? []) {
      const p = f.parentId ?? null
      const list = map.get(p) ?? []
      list.push(f)
      map.set(p, list)
    }
    for (const list of map.values()) {
      list.sort((a, b) => a.name.localeCompare(b.name))
    }
    return map
  }, [draft?.folders])

  function renderFolderTree(parentId: string | null, suiteId: string, depth: number): ReactNode {
    const kids = folderChildren.get(parentId) ?? []
    return kids.map((f) => {
      const fid = f.id!
      const hasKids = (folderChildren.get(fid) ?? []).length > 0
      const isOpen = expanded.has(fid)
      const selected = nav?.kind === 'folder' && nav.folderId === fid
      return (
        <Box key={fid}>
          <Group
            gap={4}
            wrap="nowrap"
            style={{
              paddingLeft: 8 + depth * 12,
              cursor: 'pointer',
              background: selected ? 'var(--mantine-color-default-hover)' : undefined,
              borderRadius: 4,
            }}
            onClick={() => applyNav({ kind: 'folder', suiteId, folderId: fid }, draft)}
          >
            <Box
              style={{ width: 16, flexShrink: 0 }}
              onClick={(e) => {
                e.stopPropagation()
                if (!hasKids) return
                setExpanded((prev) => {
                  const next = new Set(prev)
                  if (next.has(fid)) next.delete(fid)
                  else next.add(fid)
                  return next
                })
              }}
            >
              {hasKids ? (
                isOpen ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />
              ) : null}
            </Box>
            <Text size="xs" truncate style={{ flex: 1 }}>
              {f.name}
              <Text span size="xs" c="dimmed">
                {' '}
                ({f.participation ?? 'ENABLED'})
              </Text>
            </Text>
          </Group>
          {isOpen ? renderFolderTree(fid, suiteId, depth + 1) : null}
        </Box>
      )
    })
  }

  function updateSelectedFolder(patch: Partial<SuiteFolder>) {
    if (!draft || nav?.kind !== 'folder') return
    markDirty({
      ...draft,
      folders: (draft.folders ?? []).map((f) =>
        f.id === nav.folderId ? { ...f, ...patch } : f,
      ),
    })
  }

  function updateMatchers(matchers: SuiteMatcher[]) {
    updateSelectedFolder({ matchers })
  }

  if (capable === false) {
    return (
      <Alert color="yellow">
        `:objs-policy-service` suite APIs are not available. Suites is disabled.
      </Alert>
    )
  }

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0 }}>
      <Group justify="space-between" align="center" wrap="wrap" gap="xs" style={{ flexShrink: 0 }}>
        <Text size="xs" c="dimmed">
          {dirty ? 'unsaved' : evalResult?.meta.overallStatus
            ? `last: ${evalResult.meta.overallStatus}${evalResult.meta.overallSeverity ? ` @ ${evalResult.meta.overallSeverity}` : ''}`
            : '\u00a0'}
        </Text>
        <Group gap={6} wrap="nowrap">
          <Group gap={0}>
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              leftSection={<IconPlus size={14} />}
              onClick={() => void onAddSuite()}
              disabled={!capable || busy}
              style={{ borderTopRightRadius: 0, borderBottomRightRadius: 0 }}
            >
              Add
            </Button>
            <Menu position="bottom-end" withinPortal>
              <Menu.Target>
                <Button
                  size={VIEW_ACTION_BUTTON_SIZE}
                  disabled={!capable || busy || !draft}
                  aria-label="Add suite or folder"
                  px="xs"
                  style={{
                    borderTopLeftRadius: 0,
                    borderBottomLeftRadius: 0,
                    borderLeft: '1px solid var(--mantine-color-default-border)',
                  }}
                >
                  <IconChevronDown size={14} />
                </Button>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item onClick={() => void onAddSuite()}>Suite</Menu.Item>
                <Menu.Item disabled={!draft} onClick={() => onAddFolder()}>
                  Folder
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            variant="light"
            color="red"
            disabled={!capable || !nav || busy}
            onClick={() => setConfirmDelete(true)}
          >
            Delete
          </Button>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            disabled={!capable || !draft?.id || !dirty || busy}
            onClick={() => void onSave()}
          >
            Save
          </Button>
          <Button
            size={VIEW_ACTION_BUTTON_SIZE}
            variant="light"
            disabled={!capable || !draft?.id || busy}
            onClick={() => void onExamine()}
          >
            Examine selection
          </Button>
          <Tooltip
            label={
              context.kind === 'empty'
                ? 'Open a graph or matcher (Matcher / All) first'
                : nav?.kind === 'folder'
                  ? 'Evaluate selected folder subtree'
                  : 'Evaluate suite against current graph context'
            }
          >
            <Button
              size={VIEW_ACTION_BUTTON_SIZE}
              loading={busy}
              disabled={!capable || !draft?.id || context.kind === 'empty'}
              onClick={() => void onEvaluate()}
            >
              Evaluate
            </Button>
          </Tooltip>
        </Group>
      </Group>

      {error && (
        <Alert color="red" withCloseButton onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Group align="stretch" gap={0} wrap="nowrap" style={{ flex: 1, minHeight: 0 }}>
        <Paper withBorder style={{ width: leftWidth, flexShrink: 0, display: 'flex', flexDirection: 'column' }}>
          <Text size="xs" fw={600} p="xs" style={{ borderBottom: '1px solid var(--mantine-color-default-border)' }}>
            Suites
          </Text>
          <ScrollArea style={{ flex: 1 }} p="xs">
            {suites.length === 0 && !draft && (
              <Text size="xs" c="dimmed">
                No suites yet. Add a suite to begin.
              </Text>
            )}
            {suites.map((s) => {
              const sid = s.id!
              const open = expanded.has(sid)
              const selected = nav?.kind === 'suite' && nav.id === sid
              const showing = draft?.id === sid ? draft : s
              return (
                <Box key={sid} mb={4}>
                  <Group
                    gap={4}
                    wrap="nowrap"
                    style={{
                      cursor: 'pointer',
                      background: selected ? 'var(--mantine-color-default-hover)' : undefined,
                      borderRadius: 4,
                      paddingLeft: 4,
                    }}
                    onClick={() => {
                      applyNav({ kind: 'suite', id: sid }, showing)
                      setExpanded((prev) => new Set(prev).add(sid))
                    }}
                  >
                    <Box
                      style={{ width: 16 }}
                      onClick={(e) => {
                        e.stopPropagation()
                        setExpanded((prev) => {
                          const next = new Set(prev)
                          if (next.has(sid)) next.delete(sid)
                          else next.add(sid)
                          return next
                        })
                      }}
                    >
                      {open ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />}
                    </Box>
                    <Text size="xs" fw={600} truncate>
                      {showing.name}
                    </Text>
                  </Group>
                  {open && draft?.id === sid
                    ? renderFolderTree(null, sid, 1)
                    : open
                      ? (s.folders ?? [])
                          .filter((f) => !f.parentId)
                          .map((f) => (
                            <Text
                              key={f.id}
                              size="xs"
                              c="dimmed"
                              style={{ paddingLeft: 28, cursor: 'pointer' }}
                              onClick={() => applyNav({ kind: 'folder', suiteId: sid, folderId: f.id! }, s)}
                            >
                              {f.name}
                            </Text>
                          ))
                      : null}
                </Box>
              )
            })}
          </ScrollArea>
        </Paper>

        <Box style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', padding: '0 8px' }}>
          {!selectedSuite ? (
            <Text size="sm" c="dimmed" p="md">
              Select a suite or folder from the tree.
            </Text>
          ) : nav?.kind === 'suite' ? (
            <Stack gap="xs" p="xs" style={{ overflow: 'auto' }}>
              <TextInput
                size="xs"
                label="Name"
                value={draft?.name ?? ''}
                onChange={(e) => draft && markDirty({ ...draft, name: e.currentTarget.value })}
              />
              <Select
                size="xs"
                label="Roll-up strategy (engine)"
                data={[{ value: 'BUILTIN', label: 'BUILTIN' }]}
                value={draft?.rollUpStrategyKind ?? 'BUILTIN'}
                onChange={(v) =>
                  draft && markDirty({ ...draft, rollUpStrategyKind: v ?? 'BUILTIN' })
                }
              />
              <Select
                size="xs"
                label="Execution strategy"
                data={[
                  { value: 'DEDUPE', label: 'DEDUPE' },
                  { value: 'NO_DEDUPE', label: 'NO_DEDUPE' },
                ]}
                value={draft?.executionStrategyKind ?? 'DEDUPE'}
                onChange={(v) =>
                  draft && markDirty({ ...draft, executionStrategyKind: v ?? 'DEDUPE' })
                }
              />
              <Text size="xs" c="dimmed">
                Folder roll-up mode (ALL_PASS / ANY_PASS) is set per folder. Suite strategy selects
                which engine interprets those modes.
              </Text>
            </Stack>
          ) : selectedFolder ? (
            <Stack gap="xs" p="xs" style={{ overflow: 'auto', flex: 1 }}>
              <TextInput
                size="xs"
                label="Key"
                value={selectedFolder.key}
                onChange={(e) => updateSelectedFolder({ key: e.currentTarget.value })}
              />
              <TextInput
                size="xs"
                label="Name"
                value={selectedFolder.name}
                onChange={(e) => updateSelectedFolder({ name: e.currentTarget.value })}
              />
              <Select
                size="xs"
                label="Participation"
                data={[
                  { value: 'ENABLED', label: 'ENABLED' },
                  { value: 'DISABLED', label: 'DISABLED' },
                  { value: 'IGNORED', label: 'IGNORED' },
                ]}
                value={(selectedFolder.participation as string) ?? 'ENABLED'}
                onChange={(v) => updateSelectedFolder({ participation: v ?? 'ENABLED' })}
              />
              <Select
                size="xs"
                label="Roll-up mode"
                data={[
                  { value: 'ALL_PASS', label: 'ALL_PASS' },
                  { value: 'ANY_PASS', label: 'ANY_PASS' },
                ]}
                value={selectedFolder.rollUpMode ?? 'ALL_PASS'}
                onChange={(v) => updateSelectedFolder({ rollUpMode: v ?? 'ALL_PASS' })}
              />
              <TextInput
                size="xs"
                label="Severity config (optional)"
                placeholder="e.g. HIGH"
                value={selectedFolder.severityConfig ?? ''}
                onChange={(e) =>
                  updateSelectedFolder({
                    severityConfig: e.currentTarget.value.trim() || null,
                  })
                }
              />
              <Group justify="space-between" align="center">
                <Text size="xs" fw={600}>
                  Matchers
                </Text>
                <Menu withinPortal>
                  <Menu.Target>
                    <Button size="compact-xs" variant="light">
                      Add matcher
                    </Button>
                  </Menu.Target>
                  <Menu.Dropdown>
                    <Menu.Item
                      onClick={() =>
                        updateMatchers([
                          ...(selectedFolder.matchers ?? []),
                          { kind: 'STATIC_LIST', mode: 'INCLUDE', entries: [] },
                        ])
                      }
                    >
                      STATIC_LIST
                    </Menu.Item>
                    <Menu.Item
                      onClick={() =>
                        updateMatchers([
                          ...(selectedFolder.matchers ?? []),
                          {
                            kind: 'METADATA',
                            mode: 'INCLUDE',
                            categorySlug: 'general',
                            tags: [],
                            annotations: {},
                          },
                        ])
                      }
                    >
                      METADATA
                    </Menu.Item>
                  </Menu.Dropdown>
                </Menu>
              </Group>
              {(selectedFolder.matchers ?? []).map((m, idx) => (
                <Paper key={idx} withBorder p="xs">
                  <Group justify="space-between" mb={4}>
                    <Text size="xs" fw={600}>
                      {m.kind}
                    </Text>
                    <Button
                      size="compact-xs"
                      variant="subtle"
                      color="red"
                      onClick={() =>
                        updateMatchers((selectedFolder.matchers ?? []).filter((_, i) => i !== idx))
                      }
                    >
                      Remove
                    </Button>
                  </Group>
                  <Select
                    size="xs"
                    label="Mode"
                    mb={4}
                    data={[
                      { value: 'INCLUDE', label: 'INCLUDE' },
                      { value: 'EXCLUDE', label: 'EXCLUDE' },
                    ]}
                    value={(m.mode as string) ?? 'INCLUDE'}
                    onChange={(v) => {
                      const next = [...(selectedFolder.matchers ?? [])]
                      next[idx] = { ...m, mode: v ?? 'INCLUDE' }
                      updateMatchers(next)
                    }}
                  />
                  {m.kind === 'STATIC_LIST' && (
                    <>
                      <Switch
                        size="xs"
                        label="Skip missing"
                        checked={!!m.skipMissing}
                        onChange={(e) => {
                          const next = [...(selectedFolder.matchers ?? [])]
                          next[idx] = { ...m, skipMissing: e.currentTarget.checked }
                          updateMatchers(next)
                        }}
                        mb={4}
                      />
                      <Select
                        size="xs"
                        label="Add policy"
                        searchable
                        clearable
                        data={policies.map((p) => ({
                          value: p.id,
                          label: `${p.name} (${formatPolicyVersion(p)})`,
                        }))}
                        value={null}
                        onChange={(pid) => {
                          if (!pid) return
                          const next = [...(selectedFolder.matchers ?? [])]
                          const entries = [...(m.entries ?? [])]
                          if (!entries.some((e) => e.policyId === pid)) {
                            entries.push({ policyId: pid })
                          }
                          next[idx] = { ...m, entries }
                          updateMatchers(next)
                        }}
                      />
                      <Stack gap={2} mt={4}>
                        {(m.entries ?? []).map((e) => {
                          const p = policies.find((x) => x.id === e.policyId)
                          return (
                            <Group key={e.policyId} justify="space-between" gap={4}>
                              <Text size="xs" truncate style={{ flex: 1 }}>
                                {p ? `${p.name} · ${formatPolicyVersion(p)}` : e.policyId}
                              </Text>
                              <Button
                                size="compact-xs"
                                variant="subtle"
                                color="red"
                                onClick={() => {
                                  const next = [...(selectedFolder.matchers ?? [])]
                                  next[idx] = {
                                    ...m,
                                    entries: (m.entries ?? []).filter((x) => x.policyId !== e.policyId),
                                  }
                                  updateMatchers(next)
                                }}
                              >
                                ×
                              </Button>
                            </Group>
                          )
                        })}
                      </Stack>
                    </>
                  )}
                  {m.kind === 'METADATA' && (
                    <>
                      <TextInput
                        size="xs"
                        label="Category slug"
                        value={m.categorySlug ?? ''}
                        onChange={(e) => {
                          const next = [...(selectedFolder.matchers ?? [])]
                          next[idx] = {
                            ...m,
                            categorySlug: e.currentTarget.value.trim() || null,
                          }
                          updateMatchers(next)
                        }}
                        mb={4}
                      />
                      <TextInput
                        size="xs"
                        label="Tags (comma-separated)"
                        value={(m.tags ?? []).join(', ')}
                        onChange={(e) => {
                          const tags = e.currentTarget.value
                            .split(',')
                            .map((t) => t.trim())
                            .filter(Boolean)
                          const next = [...(selectedFolder.matchers ?? [])]
                          next[idx] = { ...m, tags }
                          updateMatchers(next)
                        }}
                      />
                    </>
                  )}
                </Paper>
              ))}
            </Stack>
          ) : (
            <Text size="sm" c="dimmed" p="md">
              Select a folder to edit matchers.
            </Text>
          )}
        </Box>

        <Paper
          withBorder
          style={{
            width: 280,
            flexShrink: 0,
            display: 'flex',
            flexDirection: 'column',
            minHeight: 0,
          }}
        >
          <Text size="xs" fw={600} p="xs" style={{ borderBottom: '1px solid var(--mantine-color-default-border)' }}>
            Selection / results
          </Text>
          <ScrollArea style={{ flex: 1 }} p="xs">
            {selection && (
              <Stack gap={4} mb="sm">
                <Text size="xs" fw={600}>
                  Effective policies ({selection.policies.length})
                </Text>
                {selection.policies.length === 0 && (
                  <Text size="xs" c="dimmed">
                    Empty selection.
                  </Text>
                )}
                {selection.policies.map((p) => (
                  <Text key={p.id} size="xs">
                    {p.name} · {formatPolicyVersion(p)}
                  </Text>
                ))}
              </Stack>
            )}
            {evalResult && (
              <Stack gap={4}>
                <Text size="xs" fw={600}>
                  Evaluation
                </Text>
                <Code block style={{ fontSize: 11 }}>
                  {evalResult.meta.evaluationId}
                </Code>
                <Text size="xs">
                  Overall: {evalResult.meta.overallStatus ?? '—'}
                  {evalResult.meta.overallSeverity
                    ? ` @ ${evalResult.meta.overallSeverity}`
                    : ''}
                </Text>
                {evalResult.tree && (
                  <SuiteTreeStatus node={evalResult.tree} depth={0} />
                )}
                <Text size="xs" fw={600} mt={4}>
                  Outcomes
                </Text>
                {evalResult.outcomes.map((o, i) => (
                  <Text key={i} size="xs">
                    {o.policyName}: {o.status}
                    {o.findings?.length ? ` (${o.findings.length} findings)` : ''}
                  </Text>
                ))}
              </Stack>
            )}
            {!selection && !evalResult && (
              <Text size="xs" c="dimmed">
                Examine selection or Evaluate to see results here. Graph context is secondary —
                use the shared bar above.
              </Text>
            )}
          </ScrollArea>
        </Paper>
      </Group>

      <Modal
        opened={confirmDelete}
        onClose={() => setConfirmDelete(false)}
        title={nav?.kind === 'folder' ? 'Delete folder?' : 'Delete suite?'}
        size="sm"
      >
        <Text size="sm" mb="md">
          {nav?.kind === 'folder'
            ? `Delete folder "${selectedFolder?.name}" and its descendants from the draft? Save to persist.`
            : `Delete suite "${draft?.name}"? This cannot be undone.`}
        </Text>
        <Group justify="flex-end">
          <Button size="xs" variant="default" onClick={() => setConfirmDelete(false)}>
            Cancel
          </Button>
          <Button size="xs" color="red" onClick={() => void onDelete()}>
            Delete
          </Button>
        </Group>
      </Modal>
    </Stack>
  )
}

function SuiteTreeStatus({ node, depth }: { node: NonNullable<SuiteEvaluationResult['tree']>; depth: number }) {
  return (
    <Box>
      <Text size="xs" style={{ paddingLeft: depth * 10 }}>
        {node.name}: {node.status}
        {node.severity ? ` @ ${node.severity}` : ''}
        {!node.votes ? ' (no vote)' : ''}
      </Text>
      {node.leaves?.map((l) => (
        <Text key={l.policyId} size="xs" c="dimmed" style={{ paddingLeft: (depth + 1) * 10 }}>
          {l.policyName}: {l.status}
          {l.severity ? ` @ ${l.severity}` : ''}
        </Text>
      ))}
      {node.children?.map((c) => (
        <SuiteTreeStatus key={c.folderId} node={c} depth={depth + 1} />
      ))}
    </Box>
  )
}
