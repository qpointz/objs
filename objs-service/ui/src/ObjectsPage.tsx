import { useRef, useState } from 'react'
import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Group,
  Paper,
  Popover,
  ScrollArea,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { IconHelp, IconX } from '@tabler/icons-react'
import { useNavigate } from 'react-router-dom'
import { queryAddObjects } from './api'
import {
  MatcherQueryForm,
  type MatcherQueryFormHandle,
} from './MatcherQueryForm'
import { ObjectResultsTable } from './ObjectResultsTable'
import { formatQueryExecStats, type QueryExecStats } from './queryExecStats'
import { shelfToComposerNavState, useObjectShelf } from './useObjectShelf'
import type { BoMEntity } from './types'

const OBJECTS_HELP = (
  <>
    Search the entity pool (orphans included for bare <code>obj-expr</code>). Add hits to the
    shelf, then <strong>New graph from shelf</strong> opens Composer with those objects as a new
    draft. Objects never writes to the store — Save happens in Composer.
  </>
)

export function ObjectsPage() {
  const navigate = useNavigate()
  const formRef = useRef<MatcherQueryFormHandle>(null)
  const shelf = useObjectShelf()
  const [searchError, setSearchError] = useState<string | null>(null)
  const [searchBusy, setSearchBusy] = useState(false)
  const [stats, setStats] = useState<QueryExecStats | null>(null)
  const [results, setResults] = useState<BoMEntity[]>([])

  async function runSearch() {
    setSearchError(null)
    try {
      const body = formRef.current?.build()
      if (body === undefined) {
        throw new Error('Matcher form is not ready')
      }
      setSearchBusy(true)
      const started = performance.now()
      const subgraph = await queryAddObjects(body, null)
      const durationMs = performance.now() - started
      const entities = subgraph.entities ?? []
      setStats({
        durationMs,
        nodes: entities.length,
        edges: subgraph.edges?.length ?? 0,
      })
      setResults(entities)
    } catch (e) {
      setStats(null)
      setResults([])
      setSearchError(e instanceof Error ? e.message : String(e))
    } finally {
      setSearchBusy(false)
    }
  }

  function onNewGraphFromShelf() {
    if (shelf.entities.length === 0) return
    navigate('/composer', { state: shelfToComposerNavState(shelf.entities) })
  }

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group justify="space-between" align="flex-start" wrap="wrap" style={{ flexShrink: 0 }}>
        <Group gap={6} align="center">
          <Title order={3}>Objects</Title>
          <Popover width={360} position="bottom-start" withArrow shadow="md">
            <Popover.Target>
              <ActionIcon variant="subtle" color="gray" size="sm" aria-label="Objects help">
                <IconHelp size={16} />
              </ActionIcon>
            </Popover.Target>
            <Popover.Dropdown>
              <Text size="sm" c="dimmed">
                {OBJECTS_HELP}
              </Text>
            </Popover.Dropdown>
          </Popover>
          {shelf.entities.length > 0 && (
            <Badge size="sm" variant="light">
              {shelf.entities.length} on shelf
            </Badge>
          )}
        </Group>
        <Group gap="xs" wrap="wrap">
          <Button
            size="sm"
            variant="default"
            disabled={shelf.entities.length === 0}
            onClick={() => shelf.clear()}
          >
            Clear shelf
          </Button>
          <Button
            size="sm"
            disabled={shelf.entities.length === 0}
            onClick={onNewGraphFromShelf}
          >
            New graph from shelf
          </Button>
        </Group>
      </Group>

      <Group
        align="stretch"
        gap="sm"
        wrap="nowrap"
        style={{ flex: 1, minHeight: 0, overflow: 'hidden' }}
      >
        <Stack gap="xs" style={{ flex: 1, minWidth: 0, minHeight: 0 }}>
          <MatcherQueryForm
            ref={formRef}
            emptyDefaults
            defaultMode="obj-expr"
            error={formError}
            stats={stats}
            action={
              <Button size="xs" loading={searchBusy} onClick={() => void runSearch()}>
                Search
              </Button>
            }
          />
          {searchError && (
            <Alert color="red" p="xs" title="Search error">
              {searchError}
            </Alert>
          )}
          <ScrollArea style={{ flex: 1, minHeight: 0 }} offsetScrollbars type="auto">
            <Stack gap="xs" pb="xs">
              <ObjectResultsTable
                results={results}
                memberIds={shelf.ids}
                summary={
                  results.length > 0
                    ? `${results.length} result${results.length === 1 ? '' : 's'}${
                        stats != null ? ` · ${formatQueryExecStats(stats)}` : ''
                      }`
                    : undefined
                }
                statusColumnLabel="Shelf"
                memberButtonLabel="On shelf"
                nonMemberButtonLabel="Add"
                addSelectedLabel="Add selected to shelf"
                removeSelectedLabel="Remove selected from shelf"
                onToggleMember={(entity) => shelf.toggle(entity)}
                onAddSelected={(entities) => shelf.add(entities)}
                onRemoveSelected={(ids) => shelf.remove(ids)}
              />
              {results.length === 0 && !searchBusy && stats != null && (
                <Text size="sm" c="dimmed">
                  No entities matched.
                </Text>
              )}
            </Stack>
          </ScrollArea>
        </Stack>

        <Paper
          withBorder
          p="xs"
          style={{
            width: 280,
            flexShrink: 0,
            display: 'flex',
            flexDirection: 'column',
            minHeight: 0,
            overflow: 'hidden',
          }}
        >
          <Text fw={600} size="sm" mb={6}>
            Shelf
          </Text>
          {shelf.entities.length === 0 ? (
            <Text size="xs" c="dimmed">
              Add objects from search results.
            </Text>
          ) : (
            <ScrollArea style={{ flex: 1, minHeight: 0 }} offsetScrollbars type="auto">
              <Stack gap={4}>
                {shelf.entities.map((entity) => (
                  <Group key={entity.id} justify="space-between" wrap="nowrap" gap={4}>
                    <Stack gap={0} style={{ minWidth: 0, flex: 1 }}>
                      <Text size="xs" fw={600} truncate>
                        {entity.type}
                      </Text>
                      <Text size="xs" c="dimmed" truncate title={entity.id}>
                        {entity.id.length > 16 ? `${entity.id.slice(0, 12)}…` : entity.id}
                      </Text>
                    </Stack>
                    <ActionIcon
                      size="xs"
                      variant="subtle"
                      color="gray"
                      aria-label={`Remove ${entity.id} from shelf`}
                      onClick={() => shelf.remove([entity.id])}
                    >
                      <IconX size={12} />
                    </ActionIcon>
                  </Group>
                ))}
              </Stack>
            </ScrollArea>
          )}
        </Paper>
      </Group>
    </Stack>
  )
}
