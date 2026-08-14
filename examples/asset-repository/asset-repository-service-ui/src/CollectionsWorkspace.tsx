import { Button, Group, Paper, ScrollArea, Stack, Text, TextInput, UnstyledButton } from '@mantine/core'
import { IconPlus } from '@tabler/icons-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { listCollections, type Collection } from './api'

function collectionIdFromPath(pathname: string): string | undefined {
  const match = pathname.match(/^\/collections\/([^/]+)/)
  if (!match) return undefined
  const id = match[1]
  if (id === 'new') return undefined
  return id
}

function isPortalPath(pathname: string): boolean {
  return pathname === '/' || pathname === '/collections' || pathname === '/collections/'
}

export function CollectionsWorkspace() {
  const location = useLocation()
  const navigate = useNavigate()
  const selectedId = collectionIdFromPath(location.pathname)
  const showNav = !isPortalPath(location.pathname)
  const [rows, setRows] = useState<Collection[]>([])
  const [search, setSearch] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function reload() {
    try {
      setError(null)
      setRows(await listCollections())
    } catch (e) {
      setError(String(e))
    }
  }

  useEffect(() => {
    void reload()
  }, [selectedId])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return rows
    return rows.filter((c) => {
      const hay = [c.name, c.owner, c.description ?? '', ...c.types.map((t) => t.objectType)]
        .join(' ')
        .toLowerCase()
      return hay.includes(q)
    })
  }, [rows, search])

  return (
    <div
      style={{
        flex: 1,
        minHeight: 0,
        height: '100%',
        display: 'flex',
        gap: 'var(--mantine-spacing-sm)',
        overflow: 'hidden',
      }}
    >
      {showNav && (
      <Paper
        withBorder
        p="sm"
        style={{
          flex: '0 0 260px',
          width: 260,
          minHeight: 0,
          height: '100%',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Group justify="space-between" wrap="nowrap" mb="xs" style={{ flexShrink: 0 }}>
          <Text size="sm" fw={650}>
            Collections
          </Text>
          <Button
            size="compact-xs"
            leftSection={<IconPlus size={12} />}
            onClick={() => navigate('/collections/new')}
          >
            New
          </Button>
        </Group>
        <TextInput
          size="xs"
          placeholder="Search collections"
          value={search}
          onChange={(e) => setSearch(e.currentTarget.value)}
          mb="xs"
          style={{ flexShrink: 0 }}
        />
        {error && (
          <Text size="xs" c="red" mb="xs">
            {error}
          </Text>
        )}
        <div style={{ flex: 1, minHeight: 0, position: 'relative' }}>
          <ScrollArea type="auto" offsetScrollbars style={{ position: 'absolute', inset: 0 }}>
            <Stack gap={2}>
              {filtered.map((c) => {
                const active = selectedId === c.id
                return (
                  <UnstyledButton
                    key={c.id}
                    component={Link}
                    to={`/collections/${c.id}`}
                    px={6}
                    py={6}
                    style={{
                      borderRadius: 4,
                      background: active ? 'var(--mantine-color-blue-light)' : undefined,
                    }}
                  >
                    <Text size="xs" fw={active ? 700 : 500} truncate>
                      {c.name}
                    </Text>
                    <Text size="xs" c="dimmed" truncate>
                      {c.types.map((t) => t.objectType).join(', ') || 'No types'}
                    </Text>
                  </UnstyledButton>
                )
              })}
              {filtered.length === 0 && (
                <Text size="xs" c="dimmed">
                  No collections
                </Text>
              )}
            </Stack>
          </ScrollArea>
        </div>
      </Paper>
      )}
      <Paper
        withBorder
        p="md"
        style={{
          flex: 1,
          minWidth: 0,
          minHeight: 0,
          height: '100%',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Outlet context={{ collections: rows, error }} />
      </Paper>
    </div>
  )
}
