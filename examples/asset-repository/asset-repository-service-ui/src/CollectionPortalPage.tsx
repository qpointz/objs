import {
  ActionIcon,
  Badge,
  Box,
  Button,
  Group,
  Paper,
  ScrollArea,
  SegmentedControl,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Title,
  Tooltip,
} from '@mantine/core'
import { IconBucket, IconLayoutGrid, IconList, IconPencil, IconPlus } from '@tabler/icons-react'
import { useMemo, useState, type MouseEvent } from 'react'
import { Link, useNavigate, useOutletContext } from 'react-router-dom'
import type { Collection } from './api'
import { CollectionObjectCount } from './CollectionObjectCount'

export type CollectionsOutletContext = {
  collections: Collection[]
  error: string | null
}

export function useCollectionsOutlet() {
  return useOutletContext<CollectionsOutletContext>()
}

function TypePills({ collection, compact }: { collection: Collection; compact?: boolean }) {
  return (
    <Group gap={4} wrap={compact ? 'nowrap' : 'wrap'} style={compact ? { overflow: 'hidden' } : undefined}>
      {collection.types.map((t) => (
        <Badge
          key={t.id}
          size="xs"
          variant="light"
          component={Link}
          to={`/schemas/${encodeURIComponent(t.objectType)}`}
          onClick={(e: MouseEvent) => e.stopPropagation()}
          style={{ cursor: 'pointer' }}
        >
          {t.objectType}
        </Badge>
      ))}
    </Group>
  )
}

function EditCollectionIcon({ collectionId }: { collectionId: string }) {
  function onEdit(e: MouseEvent) {
    e.stopPropagation()
  }
  return (
    <Tooltip label="Edit collection" withArrow>
      <ActionIcon
        component={Link}
        to={`/collections/${collectionId}/edit`}
        size="sm"
        variant="subtle"
        color="gray"
        aria-label="Edit collection"
        onClick={onEdit}
      >
        <IconPencil size={16} stroke={1.5} />
      </ActionIcon>
    </Tooltip>
  )
}

function CollectionCard({ collection }: { collection: Collection }) {
  const navigate = useNavigate()
  return (
    <Paper
      withBorder
      p={0}
      h="100%"
      style={{ cursor: 'pointer', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}
      onClick={() => navigate(`/collections/${collection.id}`)}
    >
      <Stack gap="xs" p="md" style={{ flex: 1 }}>
        <Group justify="space-between" wrap="nowrap" align="flex-start">
          <Group gap="sm" wrap="nowrap" style={{ flex: 1, minWidth: 0 }}>
            <IconBucket size={20} stroke={1.5} color="var(--mantine-color-dimmed)" aria-hidden />
            <Title order={5} lineClamp={1}>
              {collection.name}
            </Title>
          </Group>
          <EditCollectionIcon collectionId={collection.id} />
        </Group>
        <Text size="sm" c="dimmed" lineClamp={3} style={{ flex: 1 }}>
          {collection.description || collection.owner}
        </Text>
        <TypePills collection={collection} />
      </Stack>
      <Box
        px="md"
        py={8}
        style={{
          borderTop: '1px solid var(--mantine-color-default-border)',
          background: 'var(--mantine-color-default-hover)',
        }}
      >
        <CollectionObjectCount collectionId={collection.id} />
      </Box>
    </Paper>
  )
}

function CollectionListRow({ collection }: { collection: Collection }) {
  const navigate = useNavigate()
  return (
    <Paper
      withBorder
      p={0}
      style={{ cursor: 'pointer', overflow: 'hidden' }}
      onClick={() => navigate(`/collections/${collection.id}`)}
    >
      <Group wrap="nowrap" gap={0} align="stretch">
        <Group wrap="nowrap" gap="md" px="sm" py={8} style={{ flex: 1, minWidth: 0 }}>
          <IconBucket size={18} stroke={1.5} color="var(--mantine-color-dimmed)" aria-hidden />
          <Stack gap={2} style={{ flex: 1, minWidth: 0 }}>
            <Text size="sm" fw={600} truncate>
              {collection.name}
            </Text>
            <Text size="xs" c="dimmed" truncate>
              {collection.description || collection.owner}
            </Text>
          </Stack>
          <TypePills collection={collection} compact />
          <EditCollectionIcon collectionId={collection.id} />
        </Group>
        <Group
          wrap="nowrap"
          px="sm"
          py={8}
          style={{
            background: 'var(--mantine-color-default-hover)',
            borderLeft: '1px solid var(--mantine-color-default-border)',
            flexShrink: 0,
          }}
        >
          <CollectionObjectCount collectionId={collection.id} />
        </Group>
      </Group>
    </Paper>
  )
}

export function CollectionPortalPage() {
  const navigate = useNavigate()
  const { collections, error } = useCollectionsOutlet()
  const [search, setSearch] = useState('')
  const [view, setView] = useState<'cards' | 'list'>('cards')

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return collections
    return collections.filter((c) => {
      if (c.name.toLowerCase().includes(q)) return true
      return c.types.some((t) => t.objectType.toLowerCase().includes(q))
    })
  }, [collections, search])

  const searching = search.trim().length > 0
  const countLabel = searching
    ? `${filtered.length} / ${collections.length} collections`
    : `${collections.length} collection${collections.length === 1 ? '' : 's'}`

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group wrap="nowrap" align="flex-end" gap="sm">
        <TextInput
          style={{ flex: 1 }}
          size="sm"
          label="Collections"
          placeholder="Search by name or type"
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
        <Button size="sm" leftSection={<IconPlus size={14} />} onClick={() => navigate('/collections/new')}>
          Create collection
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
              No collections
            </Text>
          ) : view === 'cards' ? (
            <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="sm">
              {filtered.map((c) => (
                <CollectionCard key={c.id} collection={c} />
              ))}
            </SimpleGrid>
          ) : (
            <Stack gap="xs">
              {filtered.map((c) => (
                <CollectionListRow key={c.id} collection={c} />
              ))}
            </Stack>
          )}
        </ScrollArea>
      </div>
    </Stack>
  )
}
