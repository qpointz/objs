import {
  Badge,
  Box,
  Button,
  Group,
  Paper,
  ScrollArea,
  SegmentedControl,
  SimpleGrid,
  Skeleton,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { IconApps, IconLayoutGrid, IconList, IconPlus } from '@tabler/icons-react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { SearchInput } from '../SearchInput'
import type { ApplicationPortalStats, ApplicationSummary } from '../api/types'

function useApplicationStats(id: string) {
  const [stats, setStats] = useState<ApplicationPortalStats | null>(null)
  useEffect(() => {
    let cancelled = false
    void api
      .getApplicationStats(id)
      .then((next) => {
        if (!cancelled) setStats(next)
      })
      .catch(() => {
        if (!cancelled) setStats(null)
      })
    return () => {
      cancelled = true
    }
  }, [id])
  return stats
}

function latestLabel(stats: ApplicationPortalStats | null) {
  const latest = stats?.latestVersion
  if (!latest) return 'No released version'
  return latest.version || latest.label || 'Released'
}

function ApplicationCard({ app }: { app: ApplicationSummary }) {
  const navigate = useNavigate()
  const stats = useApplicationStats(app.id)
  return (
    <Paper
      withBorder
      p={0}
      h="100%"
      style={{ cursor: 'pointer', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}
      onClick={() => navigate(`/applications/${app.id}`)}
    >
      <Stack gap="xs" p="md" style={{ flex: 1 }}>
        <Group gap="sm" wrap="nowrap" style={{ flex: 1, minWidth: 0 }}>
          <IconApps size={20} stroke={1.5} color="var(--mantine-color-dimmed)" aria-hidden />
          <Title order={5} lineClamp={1}>
            {app.name}
          </Title>
        </Group>
        <Text size="sm" c="dimmed" lineClamp={3} style={{ flex: 1 }}>
          {app.description || 'Open the edit draft to add assets and create a version.'}
        </Text>
        {stats ? (
          <Group gap={6} wrap="wrap">
            <Text size="sm">{latestLabel(stats)}</Text>
            {stats.latestMultiBom && (
              <Badge size="xs" variant="light">
                Multi-BOM
              </Badge>
            )}
          </Group>
        ) : (
          <Skeleton height={16} width="55%" />
        )}
      </Stack>
      <Box
        px="md"
        py={8}
        style={{
          borderTop: '1px solid var(--mantine-color-default-border)',
          background: 'var(--mantine-color-default-hover)',
        }}
      >
        {stats ? (
          <Text size="xs" c="dimmed">
            {stats.bomCount} BOMs · {stats.versionCount} versions
          </Text>
        ) : (
          <Skeleton height={12} width="40%" />
        )}
      </Box>
    </Paper>
  )
}

function ApplicationListRow({ app }: { app: ApplicationSummary }) {
  const navigate = useNavigate()
  const stats = useApplicationStats(app.id)
  return (
    <Paper
      withBorder
      p={0}
      style={{ cursor: 'pointer', overflow: 'hidden' }}
      onClick={() => navigate(`/applications/${app.id}`)}
    >
      <Group wrap="nowrap" gap="md" px="sm" py={8}>
        <IconApps size={18} stroke={1.5} color="var(--mantine-color-dimmed)" aria-hidden />
        <Stack gap={2} style={{ flex: 1, minWidth: 0 }}>
          <Group gap={6} wrap="nowrap">
            <Text size="sm" fw={600} truncate>
              {app.name}
            </Text>
            {stats?.latestMultiBom && (
              <Badge size="xs" variant="light">
                Multi-BOM
              </Badge>
            )}
          </Group>
          <Text size="xs" c="dimmed" truncate>
            {stats ? latestLabel(stats) : (app.description || 'Open the edit draft')}
          </Text>
        </Stack>
        <Box miw={140} ta="right">
          {stats ? (
            <Text size="xs" c="dimmed">
              {stats.bomCount} BOMs · {stats.versionCount} versions
            </Text>
          ) : (
            <Skeleton height={12} width={120} ml="auto" />
          )}
        </Box>
      </Group>
    </Paper>
  )
}

export function ApplicationsPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<ApplicationSummary[]>([])
  const [search, setSearch] = useState('')
  const [view, setView] = useState<'cards' | 'list'>('cards')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void (async () => {
      try {
        setItems(await api.listApplications())
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Could not load applications')
      }
    })()
  }, [])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return items
    return items.filter((a) => {
      const hay = `${a.name} ${a.description ?? ''}`.toLowerCase()
      return hay.includes(q)
    })
  }, [items, search])

  const searching = search.trim().length > 0
  const countLabel = searching
    ? `${filtered.length} / ${items.length} applications`
    : `${items.length} application${items.length === 1 ? '' : 's'}`

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group wrap="nowrap" align="flex-end" gap="sm">
        <SearchInput
          style={{ flex: 1 }}
          size="sm"
          label="Applications"
          placeholder="Search by name"
          value={search}
          onValueChange={setSearch}
        />
        <SegmentedControl
          size="sm"
          value={view}
          onChange={(v) => setView(v as 'cards' | 'list')}
          data={[
            {
              value: 'cards',
              label: (
                <Group gap={6} wrap="nowrap">
                  <IconLayoutGrid size={14} />
                  Cards
                </Group>
              ),
            },
            {
              value: 'list',
              label: (
                <Group gap={6} wrap="nowrap">
                  <IconList size={14} />
                  List
                </Group>
              ),
            },
          ]}
        />
        <Button size="sm" leftSection={<IconPlus size={14} />} onClick={() => navigate('/applications/new')}>
          New application
        </Button>
      </Group>
      <Text size="xs" c="dimmed">
        {countLabel}
      </Text>
      {error && (
        <Text size="sm" c="red">
          {error}
        </Text>
      )}
      <div style={{ flex: 1, minHeight: 0, position: 'relative' }}>
        <ScrollArea style={{ position: 'absolute', inset: 0 }}>
          {filtered.length === 0 ? (
            <Text size="sm" c="dimmed">
              No applications. Create one to start composing a BOM.
            </Text>
          ) : view === 'cards' ? (
            <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="sm">
              {filtered.map((app) => (
                <ApplicationCard key={app.id} app={app} />
              ))}
            </SimpleGrid>
          ) : (
            <Stack gap="xs">
              {filtered.map((app) => (
                <ApplicationListRow key={app.id} app={app} />
              ))}
            </Stack>
          )}
        </ScrollArea>
      </div>
    </Stack>
  )
}
