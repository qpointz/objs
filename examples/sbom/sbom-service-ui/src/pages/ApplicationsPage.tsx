import {
  Box,
  Button,
  Group,
  Paper,
  ScrollArea,
  SegmentedControl,
  SimpleGrid,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { IconApps, IconLayoutGrid, IconList, IconPlus } from '@tabler/icons-react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { SearchInput } from '../SearchInput'
import type { ApplicationSummary } from '../api/types'

function ApplicationCard({ app }: { app: ApplicationSummary }) {
  const navigate = useNavigate()
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
      </Stack>
      <Box
        px="md"
        py={8}
        style={{
          borderTop: '1px solid var(--mantine-color-default-border)',
          background: 'var(--mantine-color-default-hover)',
        }}
      >
        <Text size="xs" c="dimmed">
          Application
        </Text>
      </Box>
    </Paper>
  )
}

function ApplicationListRow({ app }: { app: ApplicationSummary }) {
  const navigate = useNavigate()
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
          <Text size="sm" fw={600} truncate>
            {app.name}
          </Text>
          <Text size="xs" c="dimmed" truncate>
            {app.description || 'Open the edit draft'}
          </Text>
        </Stack>
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
