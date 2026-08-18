import {
  Anchor,
  Badge,
  Group,
  Paper,
  ScrollArea,
  SegmentedControl,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core'
import { IconLayoutGrid, IconList } from '@tabler/icons-react'
import { useMemo, useState, type MouseEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import type { SchemaCatalogEntry } from './api'
import { SchemaKindPill, useSchemasOutlet } from './SchemasWorkspace'

function UsedInLinks({ entry, compact }: { entry: SchemaCatalogEntry; compact?: boolean }) {
  const n = entry.usedIn.length
  if (n === 0) {
    return (
      <Text size="xs" c="dimmed">
        Not used in collections
      </Text>
    )
  }
  return (
    <Group gap={6} wrap={compact ? 'nowrap' : 'wrap'} style={compact ? { overflow: 'hidden' } : undefined}>
      <Text size="xs" c="dimmed">
        Used in {n} collection{n === 1 ? '' : 's'}
      </Text>
      {entry.usedIn.map((c) => (
        <Anchor
          key={c.id}
          component={Link}
          to={`/collections/${c.id}`}
          size="xs"
          onClick={(e: MouseEvent) => e.stopPropagation()}
        >
          {c.name}
        </Anchor>
      ))}
    </Group>
  )
}

function SchemaCard({ entry }: { entry: SchemaCatalogEntry }) {
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
            <SchemaKindPill usage={entry.usage} />
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
        {entry.usage !== 'EDGE_PROPERTIES' && <UsedInLinks entry={entry} />}
      </Stack>
    </Paper>
  )
}

function SchemaListRow({ entry }: { entry: SchemaCatalogEntry }) {
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
        <SchemaKindPill usage={entry.usage} />
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
        {entry.usage !== 'EDGE_PROPERTIES' && <UsedInLinks entry={entry} compact />}
      </Group>
    </Paper>
  )
}

export function SchemaPortalPage() {
  const { catalog, error } = useSchemasOutlet()
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
        <TextInput
          style={{ flex: 1 }}
          size="sm"
          label="Schemas"
          placeholder="Search by type, description, or collection"
          value={search}
          onChange={(e) => setSearch(e.currentTarget.value)}
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
                <SchemaCard key={s.type} entry={s} />
              ))}
            </SimpleGrid>
          ) : (
            <Stack gap="xs">
              {filtered.map((s) => (
                <SchemaListRow key={s.type} entry={s} />
              ))}
            </Stack>
          )}
        </ScrollArea>
      </div>
    </Stack>
  )
}
