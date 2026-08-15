import {
  Alert,
  Anchor,
  Box,
  Button,
  Checkbox,
  Group,
  Modal,
  Pagination,
  Paper,
  ScrollArea,
  Select,
  Stack,
  Switch,
  Table,
  Tabs,
  Text,
  TextInput,
  Textarea,
  Title,
  UnstyledButton,
} from '@mantine/core'
import { IconChevronDown, IconChevronRight, IconFolder, IconFolderOpen, IconPencil, IconPlus, IconTrash } from '@tabler/icons-react'
import { useCallback, useEffect, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import { SearchInput } from '../SearchInput'
import type {
  ApplicationSummary,
  ApplicationVersionSummary,
  CategoryAssetPage,
  MiReportTable,
  PortfolioLevelApps,
  PortfolioTreeView,
  SubjectAreaView,
} from '../api/types'

const REPORTS = [
  { id: 'MI-1', title: 'Portfolio composition' },
  { id: 'MI-2', title: 'Application dependency map' },
  { id: 'MI-3', title: 'Shared asset hotspots' },
  { id: 'MI-4', title: 'Duplicate & risk signals' },
] as const

const DEFAULT_TREE_PANE = 280
const MIN_TREE_PANE = 180
const MIN_CONTENT_PANE = 360
const SPLITTER_WIDTH = 8
let lastTreeWidth = DEFAULT_TREE_PANE

function findArea(areas: SubjectAreaView[], id: string): SubjectAreaView | null {
  for (const a of areas) {
    if (a.id === id) return a
    const nested = findArea(a.children, id)
    if (nested) return nested
  }
  return null
}

function collectExpandableIds(areas: SubjectAreaView[], into: Set<string>) {
  for (const a of areas) {
    if (a.children.length > 0) {
      into.add(a.id)
      collectExpandableIds(a.children, into)
    }
  }
}

function flattenCategories(
  areas: SubjectAreaView[],
  prefix = '',
): { value: string; label: string }[] {
  const out: { value: string; label: string }[] = []
  for (const a of areas) {
    const label = prefix ? `${prefix} / ${a.name}` : a.name
    out.push({ value: a.id, label })
    out.push(...flattenCategories(a.children, label))
  }
  return out
}

function pathNamesToNode(areas: SubjectAreaView[], targetId: string, acc: string[] = []): string[] | null {
  for (const a of areas) {
    const next = [...acc, a.name]
    if (a.id === targetId) return next
    const found = pathNamesToNode(a.children, targetId, next)
    if (found) return found
  }
  return null
}

function relativeCategoryPath(
  areas: SubjectAreaView[],
  currentLevel: string,
  nodeId: string | null | undefined,
  includeSubcategories: boolean,
): { display: string; full: string } | null {
  if (!includeSubcategories || !nodeId) return null
  if (currentLevel !== 'root' && nodeId === currentLevel) return null
  const fullPath = pathNamesToNode(areas, nodeId)
  if (!fullPath || fullPath.length === 0) return null
  let relative = fullPath
  if (currentLevel !== 'root') {
    const currentPath = pathNamesToNode(areas, currentLevel)
    if (!currentPath || fullPath.length <= currentPath.length) return null
    relative = fullPath.slice(currentPath.length)
  }
  if (relative.length === 0) return null
  const full = `/${relative.join('/')}`
  const display = relative.length === 1 ? `/${relative[0]}` : `/..../ ${relative[relative.length - 1]}`
  return { display, full }
}

function TreeNode({
  id,
  name,
  leafCount,
  children,
  level,
  expanded,
  onSelect,
  onToggle,
}: {
  id: string
  name: string
  leafCount: number
  children: SubjectAreaView[]
  level: string
  expanded: Set<string>
  onSelect: (id: string) => void
  onToggle: (id: string) => void
}) {
  const active = level === id
  const hasChildren = children.length > 0
  const open = expanded.has(id)
  return (
    <div>
      <UnstyledButton
        onClick={() => onSelect(id)}
        px={6}
        py={5}
        w="100%"
        style={{
          borderRadius: 6,
          background: active ? 'var(--mantine-color-blue-light)' : 'transparent',
        }}
      >
        <Group gap={6} wrap="nowrap" justify="space-between">
          <Group gap={4} wrap="nowrap" style={{ minWidth: 0, flex: 1 }}>
            <Box
              component="span"
              onClick={(e) => {
                e.stopPropagation()
                if (hasChildren) onToggle(id)
              }}
              style={{
                width: 16,
                display: 'flex',
                justifyContent: 'center',
                flexShrink: 0,
                cursor: hasChildren ? 'pointer' : 'default',
              }}
            >
              {hasChildren ? (
                open ? (
                  <IconChevronDown size={14} />
                ) : (
                  <IconChevronRight size={14} />
                )
              ) : (
                <span style={{ width: 14 }} />
              )}
            </Box>
            {open && hasChildren ? (
              <IconFolderOpen size={15} stroke={1.5} color="var(--mantine-color-dimmed)" />
            ) : (
              <IconFolder size={15} stroke={1.5} color="var(--mantine-color-dimmed)" />
            )}
            <Text size="sm" fw={active ? 650 : 500} truncate>
              {name}
            </Text>
          </Group>
          <Text size="xs" c="dimmed">
            {leafCount}
          </Text>
        </Group>
      </UnstyledButton>
      {hasChildren && open && (
        <div
          style={{
            marginLeft: 12,
            paddingLeft: 8,
            borderLeft: '1px solid var(--mantine-color-default-border)',
          }}
        >
          {children.map((child) => (
            <TreeNode
              key={child.id}
              id={child.id}
              name={child.name}
              leafCount={child.leafCount ?? 0}
              children={child.children}
              level={level}
              expanded={expanded}
              onSelect={onSelect}
              onToggle={onToggle}
            />
          ))}
        </div>
      )}
    </div>
  )
}

export function PortfolioWorkspace() {
  const { id = '' } = useParams()
  const [params, setParams] = useSearchParams()
  const level = params.get('level') || 'root'
  const tab = params.get('tab') || 'apps'
  const includeSub = params.get('includeSubcategories') !== 'false'

  const [tree, setTree] = useState<PortfolioTreeView | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [appsPage, setAppsPage] = useState<PortfolioLevelApps | null>(null)
  const [assetsPage, setAssetsPage] = useState<CategoryAssetPage | null>(null)
  const [report, setReport] = useState<(typeof REPORTS)[number]['id']>('MI-1')
  const [reportTable, setReportTable] = useState<MiReportTable | null>(null)
  const [page, setPage] = useState(1)
  const [reportPage, setReportPage] = useState(1)

  const [createOpen, setCreateOpen] = useState(false)
  const [newName, setNewName] = useState('')
  const [newDesc, setNewDesc] = useState('')
  const [editName, setEditName] = useState('')
  const [editDesc, setEditDesc] = useState('')
  const [editing, setEditing] = useState(false)
  const [addOpen, setAddOpen] = useState(false)
  const [allApps, setAllApps] = useState<ApplicationSummary[]>([])
  const [pickApp, setPickApp] = useState<string | null>(null)
  const [versions, setVersions] = useState<ApplicationVersionSummary[]>([])
  const [pickVersion, setPickVersion] = useState<string | null>(null)
  const [appQuery, setAppQuery] = useState('')
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const [moveOpen, setMoveOpen] = useState(false)
  const [moveTarget, setMoveTarget] = useState<string | null>('root')
  const [expanded, setExpanded] = useState<Set<string>>(() => new Set(['root']))
  const [treeWidth, setTreeWidth] = useState(lastTreeWidth)
  const splitHostRef = useRef<HTMLDivElement | null>(null)
  const dragRef = useRef<{ startX: number; startWidth: number } | null>(null)
  const expandedForPortfolio = useRef<string | null>(null)

  const setQuery = useCallback(
    (patch: Record<string, string>) => {
      const next = new URLSearchParams(params)
      for (const [k, v] of Object.entries(patch)) next.set(k, v)
      setParams(next, { replace: true })
    },
    [params, setParams],
  )

  const selectedArea = useMemo(() => {
    if (!tree || level === 'root') return null
    return findArea(tree.subjectAreas, level)
  }, [tree, level])

  const headerTitle = selectedArea?.name || tree?.portfolio.name || 'Portfolio'
  const headerDesc =
    level === 'root' ? tree?.portfolio.description : selectedArea?.description
  const uniqueness = tree?.portfolio.uniqueness || 'UNIQUE_APP'

  useEffect(() => {
    lastTreeWidth = treeWidth
  }, [treeWidth])

  useEffect(() => {
    if (!tree) return
    if (expandedForPortfolio.current === tree.portfolio.id) return
    expandedForPortfolio.current = tree.portfolio.id
    const ids = new Set<string>(['root'])
    collectExpandableIds(tree.subjectAreas, ids)
    setExpanded(ids)
  }, [tree])

  const toggleExpanded = useCallback((nodeId: string) => {
    setExpanded((prev) => {
      const next = new Set(prev)
      if (next.has(nodeId)) next.delete(nodeId)
      else next.add(nodeId)
      return next
    })
  }, [])

  const onSplitterPointerDown = useCallback(
    (e: ReactPointerEvent<HTMLDivElement>) => {
      e.preventDefault()
      dragRef.current = { startX: e.clientX, startWidth: treeWidth }
      e.currentTarget.setPointerCapture(e.pointerId)
    },
    [treeWidth],
  )

  const onSplitterPointerMove = useCallback((e: ReactPointerEvent<HTMLDivElement>) => {
    const drag = dragRef.current
    if (drag == null) return
    const host = splitHostRef.current
    if (host == null) return
    const maxLeft = Math.max(MIN_TREE_PANE, host.clientWidth - MIN_CONTENT_PANE - SPLITTER_WIDTH)
    const next = Math.min(maxLeft, Math.max(MIN_TREE_PANE, drag.startWidth + (e.clientX - drag.startX)))
    setTreeWidth(next)
  }, [])

  const onSplitterPointerUp = useCallback((e: ReactPointerEvent<HTMLDivElement>) => {
    dragRef.current = null
    if (e.currentTarget.hasPointerCapture(e.pointerId)) {
      e.currentTarget.releasePointerCapture(e.pointerId)
    }
  }, [])

  const reloadTree = useCallback(async () => {
    setTree(await api.getPortfolio(id))
  }, [id])

  useEffect(() => {
    void (async () => {
      try {
        await reloadTree()
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Could not load portfolio')
      }
    })()
  }, [reloadTree])

  useEffect(() => {
    setEditing(false)
    if (selectedArea) {
      setEditName(selectedArea.name)
      setEditDesc(selectedArea.description || '')
    } else if (tree) {
      setEditName(tree.portfolio.name)
      setEditDesc(tree.portfolio.description || '')
    }
  }, [selectedArea, tree, level])

  useEffect(() => {
    setPage(1)
    setReportPage(1)
    setReportTable(null)
    setAssetsPage(null)
    setSelectedIds(new Set())
  }, [level, includeSub, tab])

  useEffect(() => {
    setPage(1)
  }, [appQuery])

  useEffect(() => {
    if (!id || tab !== 'apps') return
    void (async () => {
      try {
        setAppsPage(await api.portfolioLevelApps(id, level, includeSub, page, 20, appQuery))
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Could not load applications')
      }
    })()
  }, [id, level, includeSub, tab, page, appQuery])

  useEffect(() => {
    if (!id || tab !== 'assets') return
    void (async () => {
      try {
        setAssetsPage(await api.portfolioAssets(id, level, includeSub, page, 20))
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Could not load assets')
      }
    })()
  }, [id, level, includeSub, tab, page])

  useEffect(() => {
    if (!pickApp) {
      setVersions([])
      return
    }
    void (async () => {
      setVersions(await api.listVersions(pickApp))
    })()
  }, [pickApp])

  async function saveHeader() {
    setError(null)
    try {
      if (level === 'root') {
        await api.updatePortfolio(id, { name: editName, description: editDesc })
      } else {
        await api.updateSubjectArea(id, level, { name: editName, description: editDesc })
      }
      await reloadTree()
      setEditing(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not save')
    }
  }

  function cancelEdit() {
    setEditing(false)
    if (selectedArea) {
      setEditName(selectedArea.name)
      setEditDesc(selectedArea.description || '')
    } else if (tree) {
      setEditName(tree.portfolio.name)
      setEditDesc(tree.portfolio.description || '')
    }
  }

  async function createSubcategory() {
    setError(null)
    try {
      await api.addSubjectArea(id, {
        name: newName.trim(),
        description: newDesc.trim() || undefined,
        parentId: level === 'root' ? null : level,
      })
      setCreateOpen(false)
      setNewName('')
      setNewDesc('')
      await reloadTree()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not create category')
    }
  }

  async function deleteCategory() {
    if (level === 'root') return
    setError(null)
    try {
      await api.deleteSubjectArea(id, level)
      setQuery({ level: 'root' })
      await reloadTree()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not delete category')
    }
  }

  async function addApp() {
    if (!pickApp) return
    setError(null)
    try {
      await api.placeApplication(id, {
        applicationId: pickApp,
        subjectAreaId: level === 'root' ? null : level,
        versionId: uniqueness === 'UNIQUE_APP_VERSION' ? pickVersion : null,
      })
      setAddOpen(false)
      setPickApp(null)
      setPickVersion(null)
      await reloadTree()
      setAppsPage(await api.portfolioLevelApps(id, level, includeSub, 1, 20, appQuery))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not add application')
    }
  }

  async function deleteSelected() {
    if (selectedIds.size === 0) return
    setError(null)
    try {
      await api.deletePlacements(id, [...selectedIds])
      setSelectedIds(new Set())
      await reloadTree()
      setAppsPage(await api.portfolioLevelApps(id, level, includeSub, page, 20, appQuery))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not remove applications')
    }
  }

  async function moveSelected() {
    if (selectedIds.size === 0) return
    setError(null)
    try {
      await api.movePlacements(id, [...selectedIds], moveTarget === 'root' || !moveTarget ? null : moveTarget)
      setMoveOpen(false)
      setSelectedIds(new Set())
      await reloadTree()
      setAppsPage(await api.portfolioLevelApps(id, level, includeSub, page, 20, appQuery))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not move applications')
    }
  }

  async function runReport(nextPage = 1) {
    setError(null)
    try {
      setReportPage(nextPage)
      setReportTable(
        await api.runMiReport(id, {
          level: level || 'root',
          includeSubcategories: includeSub,
          report,
          page: nextPage,
          size: 20,
        }),
      )
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not run report')
    }
  }

  const appTotalPages = Math.max(1, Math.ceil((appsPage?.total ?? 0) / 20))
  const assetTotalPages = Math.max(1, Math.ceil((assetsPage?.total ?? 0) / 20))
  const reportTotalPages = Math.max(1, Math.ceil((reportTable?.total ?? 0) / 20))
  const pagePlacementIds = (appsPage?.applications ?? [])
    .map((r) => r.placementId)
    .filter((pid): pid is string => Boolean(pid))
  const allPageSelected = pagePlacementIds.length > 0 && pagePlacementIds.every((pid) => selectedIds.has(pid))
  const somePageSelected = pagePlacementIds.some((pid) => selectedIds.has(pid))

  return (
    <div
      ref={splitHostRef}
      style={{
        flex: 1,
        minHeight: 0,
        height: '100%',
        display: 'flex',
        overflow: 'hidden',
      }}
    >
      <Paper
        withBorder
        p="xs"
        style={{
          flex: `0 0 ${treeWidth}px`,
          width: treeWidth,
          display: 'flex',
          flexDirection: 'column',
          minHeight: 0,
          minWidth: 0,
        }}
      >
        <Text size="sm" fw={650} truncate px={6} mb="xs">
          {tree?.portfolio.name || 'Portfolio'}
        </Text>
        <ScrollArea style={{ flex: 1 }}>
          <TreeNode
            id="root"
            name={tree?.portfolio.name || 'Root'}
            leafCount={tree?.rootLeafCount ?? 0}
            children={tree?.subjectAreas ?? []}
            level={level}
            expanded={expanded}
            onSelect={(next) => setQuery({ level: next })}
            onToggle={toggleExpanded}
          />
        </ScrollArea>
      </Paper>
      <Box
        role="separator"
        aria-orientation="vertical"
        aria-label="Resize category tree"
        onPointerDown={onSplitterPointerDown}
        onPointerMove={onSplitterPointerMove}
        onPointerUp={onSplitterPointerUp}
        onPointerCancel={onSplitterPointerUp}
        style={{
          width: SPLITTER_WIDTH,
          flexShrink: 0,
          cursor: 'col-resize',
          display: 'flex',
          alignItems: 'stretch',
          justifyContent: 'center',
          touchAction: 'none',
          userSelect: 'none',
        }}
      >
        <Box
          style={{
            width: 3,
            margin: '8px 0',
            borderRadius: 2,
            background: 'var(--mantine-color-default-border)',
          }}
        />
      </Box>
      <Stack gap="sm" p="md" style={{ flex: 1, minWidth: 0, minHeight: 0 }}>
        {error && <Alert color="red">{error}</Alert>}
        {editing ? (
          <Stack gap="sm">
            <TextInput
              size="sm"
              label="Name"
              value={editName}
              onChange={(e) => setEditName(e.currentTarget.value)}
            />
            <Textarea
              size="sm"
              label="Description"
              autosize
              minRows={2}
              value={editDesc}
              onChange={(e) => setEditDesc(e.currentTarget.value)}
            />
            <Group gap="xs">
              <Button size="xs" onClick={() => void saveHeader()}>
                Save
              </Button>
              <Button size="xs" variant="default" onClick={cancelEdit}>
                Cancel
              </Button>
            </Group>
          </Stack>
        ) : (
          <>
            <Group justify="space-between" align="flex-start">
              <Stack gap={4} style={{ flex: 1, minWidth: 0 }}>
                <Title order={4}>{headerTitle}</Title>
                <Text size="sm" c="dimmed">
                  {headerDesc || 'No description'}
                </Text>
              </Stack>
              <Group gap="xs">
                <Button size="xs" variant="light" leftSection={<IconPencil size={14} />} onClick={() => setEditing(true)}>
                  Edit
                </Button>
                <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} onClick={() => setCreateOpen(true)}>
                  Subcategory
                </Button>
                {level !== 'root' && (
                  <Button size="xs" color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={() => void deleteCategory()}>
                    Delete
                  </Button>
                )}
              </Group>
            </Group>
            <Switch
              size="sm"
              label="Include subcategories"
              checked={includeSub}
              onChange={(e) => setQuery({ includeSubcategories: String(e.currentTarget.checked) })}
            />
          </>
        )}
        <Tabs value={tab} onChange={(v) => setQuery({ tab: v || 'apps' })} style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
          <Tabs.List>
            <Tabs.Tab value="apps">Apps</Tabs.Tab>
            <Tabs.Tab value="assets">Assets</Tabs.Tab>
            <Tabs.Tab value="reports">Reports</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="apps" pt="sm" style={{ flex: 1, minHeight: 0 }}>
            <Stack gap="sm" h="100%">
              <Group gap="xs">
                <Button size="xs" onClick={() => { void api.listApplications().then(setAllApps); setAddOpen(true) }}>
                  Add application
                </Button>
                <Button
                  size="xs"
                  variant="light"
                  disabled={selectedIds.size === 0}
                  onClick={() => {
                    setMoveTarget('root')
                    setMoveOpen(true)
                  }}
                >
                  Move to category
                </Button>
                <Button
                  size="xs"
                  color="red"
                  variant="light"
                  disabled={selectedIds.size === 0}
                  onClick={() => void deleteSelected()}
                >
                  Delete
                </Button>
              </Group>
              <SearchInput
                size="sm"
                placeholder="Search applications"
                value={appQuery}
                onValueChange={setAppQuery}
              />
              <ScrollArea style={{ flex: 1 }}>
                {(appsPage?.applications ?? []).length === 0 ? (
                  <Text size="sm" c="dimmed">
                    No applications in this category.
                  </Text>
                ) : (
                  <Table striped highlightOnHover stickyHeader>
                    <Table.Thead>
                      <Table.Tr>
                        <Table.Th w={36}>
                          <Checkbox
                            checked={allPageSelected}
                            indeterminate={somePageSelected && !allPageSelected}
                            onChange={() => {
                              setSelectedIds((prev) => {
                                const next = new Set(prev)
                                if (allPageSelected) {
                                  pagePlacementIds.forEach((pid) => next.delete(pid))
                                } else {
                                  pagePlacementIds.forEach((pid) => next.add(pid))
                                }
                                return next
                              })
                            }}
                            aria-label="Select all applications"
                          />
                        </Table.Th>
                        <Table.Th>Application</Table.Th>
                        <Table.Th>Description</Table.Th>
                        {includeSub && <Table.Th>Category</Table.Th>}
                      </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                      {(appsPage?.applications ?? []).map((row) => {
                        const pid = row.placementId
                        const checked = pid != null && selectedIds.has(pid)
                        const relPath = relativeCategoryPath(
                          tree?.subjectAreas ?? [],
                          level,
                          row.nodeId,
                          includeSub,
                        )
                        return (
                          <Table.Tr key={pid || row.applicationId}>
                            <Table.Td>
                              <Checkbox
                                checked={checked}
                                disabled={!pid}
                                onChange={() => {
                                  if (!pid) return
                                  setSelectedIds((prev) => {
                                    const next = new Set(prev)
                                    if (next.has(pid)) next.delete(pid)
                                    else next.add(pid)
                                    return next
                                  })
                                }}
                                aria-label={`Select ${row.applicationName}`}
                              />
                            </Table.Td>
                            <Table.Td>
                              <Anchor component={Link} to={`/applications/${row.applicationId}`} size="sm" fw={600}>
                                {row.applicationName}
                              </Anchor>
                            </Table.Td>
                            <Table.Td>
                              <Text size="sm" c="dimmed" truncate maw={420}>
                                {row.applicationDescription || '—'}
                              </Text>
                            </Table.Td>
                            {includeSub && (
                              <Table.Td>
                                <Text size="sm" c="dimmed" truncate title={relPath?.full}>
                                  {relPath?.display || '—'}
                                </Text>
                              </Table.Td>
                            )}
                          </Table.Tr>
                        )
                      })}
                    </Table.Tbody>
                  </Table>
                )}
              </ScrollArea>
              <Pagination total={appTotalPages} value={page} onChange={setPage} size="sm" />
            </Stack>
          </Tabs.Panel>
          <Tabs.Panel value="assets" pt="sm" style={{ flex: 1, minHeight: 0 }}>
            <Stack gap="sm" h="100%">
              {(assetsPage?.notes ?? []).length > 0 && (
                <Text size="xs" c="dimmed">
                  {assetsPage?.notes?.join(' ')}
                </Text>
              )}
              <ScrollArea style={{ flex: 1 }}>
                {(assetsPage?.items ?? []).length === 0 ? (
                  <Text size="sm" c="dimmed">
                    No assets in this category yet. Add components on the application, then return here.
                  </Text>
                ) : (
                  <Table striped highlightOnHover>
                    <Table.Thead>
                      <Table.Tr>
                        <Table.Th>Asset</Table.Th>
                        <Table.Th>Type</Table.Th>
                        <Table.Th>Used in</Table.Th>
                      </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                      {(assetsPage?.items ?? []).map((row) => (
                        <Table.Tr key={`${row.type}-${row.assetId}`}>
                          <Table.Td>
                            <Anchor component={Link} to={`/applications/assets/${row.assetId}`} size="sm">
                              {row.label}
                            </Anchor>
                          </Table.Td>
                          <Table.Td>{row.type}</Table.Td>
                          <Table.Td>{row.usedInApplicationNames.join(', ')}</Table.Td>
                        </Table.Tr>
                      ))}
                    </Table.Tbody>
                  </Table>
                )}
              </ScrollArea>
              <Pagination total={assetTotalPages} value={page} onChange={setPage} size="sm" />
            </Stack>
          </Tabs.Panel>
          <Tabs.Panel value="reports" pt="sm" style={{ flex: 1, minHeight: 0 }}>
            <Stack gap="sm" h="100%">
              <Group>
                <Select
                  size="xs"
                  data={REPORTS.map((r) => ({ value: r.id, label: r.title }))}
                  value={report}
                  onChange={(v) => setReport((v as (typeof REPORTS)[number]['id']) || 'MI-1')}
                />
                <Button size="xs" onClick={() => void runReport()}>
                  Run
                </Button>
                <Button
                  size="xs"
                  variant="light"
                  component="a"
                  href={api.miReportCsvUrl(id, report, level, includeSub)}
                >
                  Export CSV
                </Button>
              </Group>
              {reportTable?.notes?.length ? (
                <Text size="xs" c="dimmed">
                  {reportTable.notes.join(' ')}
                </Text>
              ) : null}
              <ScrollArea style={{ flex: 1 }}>
                <Table striped highlightOnHover>
                  <Table.Thead>
                    <Table.Tr>
                      {(reportTable?.columns ?? []).map((c) => (
                        <Table.Th key={c}>{c}</Table.Th>
                      ))}
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {(reportTable?.rows ?? []).map((row, i) => (
                      <Table.Tr key={i}>
                        {(reportTable?.columns ?? []).map((c) => (
                          <Table.Td key={c}>{row[c]}</Table.Td>
                        ))}
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </ScrollArea>
              <Pagination
                total={reportTotalPages}
                value={reportPage}
                onChange={(n) => void runReport(n)}
                size="sm"
              />
            </Stack>
          </Tabs.Panel>
        </Tabs>
      </Stack>
      <Modal opened={createOpen} onClose={() => setCreateOpen(false)} title="Create subcategory">
        <Stack>
          <TextInput label="Name" value={newName} onChange={(e) => setNewName(e.currentTarget.value)} />
          <Textarea label="Description" value={newDesc} onChange={(e) => setNewDesc(e.currentTarget.value)} />
          <Button onClick={() => void createSubcategory()}>Create</Button>
        </Stack>
      </Modal>
      <Modal opened={moveOpen} onClose={() => setMoveOpen(false)} title="Move to category">
        <Stack>
          <Select
            label="Category"
            data={[
              { value: 'root', label: `${tree?.portfolio.name || 'Portfolio'} (root)` },
              ...flattenCategories(tree?.subjectAreas ?? []),
            ]}
            value={moveTarget}
            onChange={setMoveTarget}
          />
          <Button onClick={() => void moveSelected()}>Move</Button>
        </Stack>
      </Modal>
      <Modal opened={addOpen} onClose={() => setAddOpen(false)} title="Add application">
        <Stack>
          <Select
            label="Application"
            searchable
            data={allApps.map((a) => ({ value: a.id, label: a.name }))}
            value={pickApp}
            onChange={setPickApp}
          />
          {uniqueness === 'UNIQUE_APP_VERSION' && (
            <Select
              label="Version"
              data={versions.map((v) => ({ value: v.id, label: v.label || v.id }))}
              value={pickVersion}
              onChange={setPickVersion}
            />
          )}
          <Button onClick={() => void addApp()}>Add</Button>
        </Stack>
      </Modal>
    </div>
  )
}
