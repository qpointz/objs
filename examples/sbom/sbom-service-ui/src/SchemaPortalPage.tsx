import {
  Badge,
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
import { IconLayoutGrid, IconList, IconSchema } from '@tabler/icons-react'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { SchemaCatalogEntry } from './api/types'
import { SearchInput } from './SearchInput'
import { useSchemasOutlet } from './SchemasWorkspace'

function UsedInLabel({ entry, pending }: { entry: SchemaCatalogEntry; pending: boolean }) {
  if (pending) {
    return <Skeleton height={12} width={160} />
  }
  const n = entry.usedIn.length
  return (
    <Text size="xs" c="dimmed">
      {n === 0 ? 'Not used in applications' : `Used in ${n} application${n === 1 ? '' : 's'}`}
    </Text>
  )
}

function SchemaCard({ entry, pending }: { entry: SchemaCatalogEntry; pending: boolean }) {
  const navigate = useNavigate()
  return (
    <Paper
      withBorder
      p="md"
      h="100%"
      style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column' }}
      onClick={() => navigate(`/schemas/${encodeURIComponent(entry.type)}`)}
    >
      <Stack gap="xs" style={{ flex: 1 }}>
        <Group justify="space-between" wrap="nowrap" align="flex-start">
          <Group gap="sm" wrap="nowrap" style={{ flex: 1, minWidth: 0 }}>
            <IconSchema size={20} stroke={1.5} color="var(--mantine-color-dimmed)" aria-hidden />
            <Title order={5} lineClamp={1}>
              {entry.type}
            </Title>
          </Group>
          <Badge size="sm" variant="light">
            {entry.latestVersion}
          </Badge>
        </Group>
        <Text size="sm" c="dimmed" lineClamp={3} style={{ flex: 1 }}>
          {entry.description || entry.title || 'No description'}
        </Text>
        <UsedInLabel entry={entry} pending={pending} />
      </Stack>
    </Paper>
  )
}

function SchemaListRow({ entry, pending }: { entry: SchemaCatalogEntry; pending: boolean }) {
  const navigate = useNavigate()
  return (
    <Paper
      withBorder
      px="sm"
      py={8}
      style={{ cursor: 'pointer' }}
      onClick={() => navigate(`/schemas/${encodeURIComponent(entry.type)}`)}
    >
      <Group wrap="nowrap" gap="md">
        <IconSchema size={18} stroke={1.5} color="var(--mantine-color-dimmed)" aria-hidden />
        <Stack gap={2} style={{ flex: 1, minWidth: 0 }}>
          <Text size="sm" fw={600} truncate>
            {entry.type}
          </Text>
          <Text size="xs" c="dimmed" truncate>
            {entry.description || entry.title || 'No description'}
          </Text>
        </Stack>
        <Badge size="sm" variant="light">
          {entry.latestVersion}
        </Badge>
        <UsedInLabel entry={entry} pending={pending} />
      </Group>
    </Paper>
  )
}

export function SchemaPortalPage() {
  const { catalog, usagePending, error } = useSchemasOutlet()
  const [search, setSearch] = useState('')
  const [view, setView] = useState<'cards' | 'list'>('cards')

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return catalog
    return catalog.filter((s) => {
      const hay = [s.type, s.title ?? '', s.description ?? '', ...s.usedIn.map((c) => c.name)]
        .join(' ')
        .toLowerCase()
      return hay.includes(q)
    })
  }, [catalog, search])

  const searching = search.trim().length > 0
  const countLabel = searching
    ? `${filtered.length} / ${catalog.length} schemas`
    : `${catalog.length} schema${catalog.length === 1 ? '' : 's'}`

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group wrap="nowrap" align="flex-end" gap="sm">
        <SearchInput
          style={{ flex: 1 }}
          size="sm"
          label="Schemas"
          placeholder="Search by type, description, or application"
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
              No schemas
            </Text>
          ) : view === 'cards' ? (
            <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="sm">
              {filtered.map((s) => (
                <SchemaCard key={s.type} entry={s} pending={usagePending.has(s.type)} />
              ))}
            </SimpleGrid>
          ) : (
            <Stack gap="xs">
              {filtered.map((s) => (
                <SchemaListRow key={s.type} entry={s} pending={usagePending.has(s.type)} />
              ))}
            </Stack>
          )}
        </ScrollArea>
      </div>
    </Stack>
  )
}
