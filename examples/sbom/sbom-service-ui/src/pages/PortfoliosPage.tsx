import {
  Button,
  Group,
  Paper,
  ScrollArea,
  SegmentedControl,
  SimpleGrid,
  Stack,
  Text,
  Title,
  Badge,
} from '@mantine/core'
import { IconBriefcase, IconLayoutGrid, IconList, IconPlus } from '@tabler/icons-react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { SearchInput } from '../SearchInput'
import type { PortfolioSummary } from '../api/types'

function uniquenessLabel(value: string | undefined) {
  if (value === 'UNIQUE_APP_VERSION') return 'Unique version'
  if (value === 'NOT_UNIQUE') return 'Not unique'
  return 'Unique app'
}

function PortfolioCard({ item }: { item: PortfolioSummary }) {
  const navigate = useNavigate()
  return (
    <Paper
      withBorder
      p={0}
      h="100%"
      style={{ cursor: 'pointer', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}
      onClick={() => navigate(`/portfolios/${item.id}`)}
    >
      <Stack gap="xs" p="md" style={{ flex: 1 }}>
        <Group gap="sm" wrap="nowrap" style={{ flex: 1, minWidth: 0 }}>
          <IconBriefcase size={20} stroke={1.5} color="var(--mantine-color-dimmed)" aria-hidden />
          <Title order={5} lineClamp={1}>
            {item.name}
          </Title>
        </Group>
        <Text size="sm" c="dimmed" lineClamp={3} style={{ flex: 1 }}>
          {item.description || 'Open to organize applications by category.'}
        </Text>
        <Badge size="sm" variant="light">
          {uniquenessLabel(item.uniqueness)}
        </Badge>
      </Stack>
    </Paper>
  )
}

function PortfolioListRow({ item }: { item: PortfolioSummary }) {
  const navigate = useNavigate()
  return (
    <Paper
      withBorder
      p={0}
      style={{ cursor: 'pointer', overflow: 'hidden' }}
      onClick={() => navigate(`/portfolios/${item.id}`)}
    >
      <Group wrap="nowrap" gap="md" px="sm" py={8}>
        <IconBriefcase size={18} stroke={1.5} color="var(--mantine-color-dimmed)" aria-hidden />
        <Stack gap={2} style={{ flex: 1, minWidth: 0 }}>
          <Text size="sm" fw={600} truncate>
            {item.name}
          </Text>
          <Text size="xs" c="dimmed" truncate>
            {item.description || uniquenessLabel(item.uniqueness)}
          </Text>
        </Stack>
        <Badge size="sm" variant="light">
          {uniquenessLabel(item.uniqueness)}
        </Badge>
      </Group>
    </Paper>
  )
}

export function PortfoliosPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<PortfolioSummary[]>([])
  const [search, setSearch] = useState('')
  const [view, setView] = useState<'cards' | 'list'>('cards')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void (async () => {
      try {
        setItems(await api.listPortfolios())
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Could not load portfolios')
      }
    })()
  }, [])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return items
    return items.filter((a) => `${a.name} ${a.description ?? ''}`.toLowerCase().includes(q))
  }, [items, search])

  const searching = search.trim().length > 0
  const countLabel = searching
    ? `${filtered.length} / ${items.length} portfolios`
    : `${items.length} portfolio${items.length === 1 ? '' : 's'}`

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group wrap="nowrap" align="flex-end" gap="sm">
        <SearchInput
          style={{ flex: 1 }}
          size="sm"
          label="Portfolios"
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
        <Button size="sm" leftSection={<IconPlus size={14} />} onClick={() => navigate('/portfolios/new')}>
          New portfolio
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
              No portfolios yet.
            </Text>
          ) : view === 'cards' ? (
            <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="sm">
              {filtered.map((item) => (
                <PortfolioCard key={item.id} item={item} />
              ))}
            </SimpleGrid>
          ) : (
            <Stack gap="xs">
              {filtered.map((item) => (
                <PortfolioListRow key={item.id} item={item} />
              ))}
            </Stack>
          )}
        </ScrollArea>
      </div>
    </Stack>
  )
}
