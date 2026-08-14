import {
  Alert,
  Anchor,
  Badge,
  Button,
  Group,
  Loader,
  Menu,
  Pagination,
  Paper,
  ScrollArea,
  SegmentedControl,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
  Tooltip,
} from '@mantine/core'
import { IconChevronDown, IconPencil, IconPlus, IconSearch } from '@tabler/icons-react'
import { FormEvent, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getCollection, listObjects, searchObjects, type ArObject, type Collection } from './api'
import { CollectionObjectCount } from './CollectionObjectCount'
import { CopyIdButton, CopyableId } from './CopyId'
import { SyntaxCodeEditor } from './SyntaxCodeEditor'

const PAGE_SIZE_OPTIONS = ['10', '20', '50']
const DEFAULT_PAGE_SIZE = 20
const MAX_PAYLOAD_COLS = 6

function isScalar(value: unknown): boolean {
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean'
}

function formatCell(value: unknown): string {
  if (value == null) return '—'
  if (typeof value === 'boolean') return value ? 'true' : 'false'
  const text = String(value)
  return text.length > 48 ? `${text.slice(0, 45)}…` : text
}

function scalarPayloadColumns(objects: ArObject[], max = MAX_PAYLOAD_COLS): string[] {
  const counts = new Map<string, number>()
  for (const object of objects) {
    const payload = object.payload
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) continue
    for (const [key, value] of Object.entries(payload)) {
      if (!isScalar(value)) continue
      counts.set(key, (counts.get(key) ?? 0) + 1)
    }
  }
  return [...counts.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
    .slice(0, max)
    .map(([key]) => key)
}

export function CollectionBrowsePage() {
  const { id = '' } = useParams()
  const [collection, setCollection] = useState<Collection | null>(null)
  const [objects, setObjects] = useState<ArObject[]>([])
  const [query, setQuery] = useState('')
  const [view, setView] = useState<'grid' | 'json'>('grid')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  async function load(matcherExpr?: string) {
    try {
      setLoading(true)
      setError(null)
      const c = await getCollection(id)
      setCollection(c)
      const expr = matcherExpr?.trim()
      setObjects(expr ? await searchObjects(id, { matcherExpr: expr }) : await listObjects(id))
      setPage(1)
    } catch (e) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    setQuery('')
    void load()
  }, [id])

  function onSearch(e: FormEvent) {
    e.preventDefault()
    void load(query)
  }

  const payloadCols = useMemo(() => scalarPayloadColumns(objects), [objects])
  const pageCount = Math.max(1, Math.ceil(objects.length / pageSize))
  const safePage = Math.min(page, pageCount)
  const pageRows = objects.slice((safePage - 1) * pageSize, safePage * pageSize)

  if (!collection && loading) {
    return <Loader size="sm" />
  }
  if (!collection) {
    return <Alert color="red">{error || 'Collection not found'}</Alert>
  }

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group justify="space-between" wrap="nowrap" align="flex-start">
        <div>
          <Group gap="xs" align="baseline" wrap="nowrap">
            <Title order={3}>{collection.name}</Title>
            <CopyableId id={collection.id} />
            <CopyIdButton id={collection.id} label="Collection id copied" />
          </Group>
          <Text size="sm" c="dimmed">
            {collection.description || collection.owner}
          </Text>
          <Group gap={4} mt={6}>
            {collection.types.map((t) => (
              <Badge
                key={t.id}
                size="xs"
                variant="light"
                component={Link}
                to={`/schemas/${encodeURIComponent(t.objectType)}`}
                style={{ cursor: 'pointer' }}
              >
                {t.objectType}
              </Badge>
            ))}
          </Group>
          <Group mt={6}>
            <CollectionObjectCount collectionId={collection.id} />
          </Group>
        </div>
        <Group gap="xs">
          <Button
            size="sm"
            variant="default"
            leftSection={<IconPencil size={14} />}
            component={Link}
            to={`/collections/${id}/edit`}
          >
            Edit collection
          </Button>
          <Menu position="bottom-end" withinPortal>
            <Menu.Target>
              <Button
                size="sm"
                leftSection={<IconPlus size={14} />}
                rightSection={<IconChevronDown size={14} />}
                disabled={collection.types.length === 0}
              >
                Create object
              </Button>
            </Menu.Target>
            <Menu.Dropdown>
              {collection.types.map((t) => (
                <Menu.Item
                  key={t.id}
                  component={Link}
                  to={`/collections/${id}/objects/new?type=${encodeURIComponent(t.objectType)}`}
                >
                  {t.objectType}
                </Menu.Item>
              ))}
            </Menu.Dropdown>
          </Menu>
        </Group>
      </Group>

      <Paper withBorder p="sm" component="form" onSubmit={onSearch}>
        <Group wrap="nowrap" align="flex-end" gap="xs">
          <TextInput
            style={{ flex: 1 }}
            size="sm"
            label="Query (obj-expr)"
            description="JEXL over collection members, e.g. type == 'Prompt' && p.name.contains('triage')"
            placeholder="type == 'Dataset'"
            value={query}
            onChange={(e) => setQuery(e.currentTarget.value)}
          />
          <Button type="submit" size="sm" leftSection={<IconSearch size={14} />}>
            Search
          </Button>
          <Button
            type="button"
            size="sm"
            variant="default"
            onClick={() => {
              setQuery('')
              void load()
            }}
          >
            Clear
          </Button>
          <SegmentedControl
            size="sm"
            value={view}
            onChange={(v) => setView(v as 'grid' | 'json')}
            data={[
              { value: 'grid', label: 'Grid' },
              { value: 'json', label: 'JSON' },
            ]}
          />
        </Group>
      </Paper>

      {error && <Alert color="red">{error}</Alert>}

      <div style={{ flex: 1, minHeight: 0, position: 'relative' }}>
        {loading ? (
          <Loader size="sm" />
        ) : view === 'json' ? (
          <ScrollArea style={{ position: 'absolute', inset: 0 }}>
            <SyntaxCodeEditor
              value={JSON.stringify(objects, null, 2)}
              onChange={() => undefined}
              language="json"
              minHeight={480}
            />
          </ScrollArea>
        ) : objects.length === 0 ? (
          <Text size="sm" c="dimmed">
            No objects in this collection.
          </Text>
        ) : (
          <Stack gap="xs" style={{ position: 'absolute', inset: 0 }}>
            <ScrollArea style={{ flex: 1 }}>
              <Table striped highlightOnHover stickyHeader>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Id</Table.Th>
                    <Table.Th>Type</Table.Th>
                    {payloadCols.map((col) => (
                      <Table.Th key={col}>{col}</Table.Th>
                    ))}
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {pageRows.map((o) => (
                    <Table.Tr key={o.id}>
                      <Table.Td>
                        <Group gap={4} wrap="nowrap">
                          <Tooltip label={o.id} withArrow>
                            <Anchor
                              component={Link}
                              to={`/collections/${id}/objects/${o.id}`}
                              size="sm"
                              ff="monospace"
                            >
                              {o.id.slice(0, 8)}…
                            </Anchor>
                          </Tooltip>
                          <CopyIdButton id={o.id} label="Object id copied" />
                        </Group>
                      </Table.Td>
                      <Table.Td>{o.type}</Table.Td>
                      {payloadCols.map((col) => (
                        <Table.Td key={col}>{formatCell(o.payload?.[col])}</Table.Td>
                      ))}
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </ScrollArea>
            <Group justify="space-between" wrap="wrap">
              <Text size="xs" c="dimmed">
                {objects.length} object{objects.length === 1 ? '' : 's'}
                {objects.length > 0
                  ? ` · ${ (safePage - 1) * pageSize + 1 }–${ Math.min(safePage * pageSize, objects.length) }`
                  : ''}
              </Text>
              <Group gap="xs">
                <Select
                  size="xs"
                  w={80}
                  aria-label="Page size"
                  value={String(pageSize)}
                  data={PAGE_SIZE_OPTIONS}
                  onChange={(v) => {
                    if (!v) return
                    setPageSize(Number(v))
                    setPage(1)
                  }}
                />
                <Pagination size="xs" value={safePage} onChange={setPage} total={pageCount} />
              </Group>
            </Group>
          </Stack>
        )}
      </div>
    </Stack>
  )
}
