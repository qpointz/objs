import { Badge, Group, Paper, ScrollArea, Stack, Text, TextInput, UnstyledButton } from '@mantine/core'
import { useEffect, useMemo, useState } from 'react'
import { Link, Outlet, useLocation, useOutletContext } from 'react-router-dom'
import { listSchemaCatalog, type SchemaCatalogEntry } from './api'

function schemaTypeFromPath(pathname: string): string | undefined {
  const match = pathname.match(/^\/schemas\/([^/]+)/)
  if (!match) return undefined
  return decodeURIComponent(match[1])
}

function isPortalPath(pathname: string): boolean {
  return pathname === '/schemas' || pathname === '/schemas/'
}

export function SchemaKindPill({ usage }: { usage?: string }) {
  const edge = usage === 'EDGE_PROPERTIES'
  return (
    <Badge size="xs" variant="light" color={edge ? 'grape' : 'blue'} w={28} px={0}>
      {edge ? 'E' : 'O'}
    </Badge>
  )
}

export type SchemasOutletContext = {
  catalog: SchemaCatalogEntry[]
  error: string | null
}

export function SchemasWorkspace() {
  const location = useLocation()
  const selectedType = schemaTypeFromPath(location.pathname)
  const showNav = !isPortalPath(location.pathname)
  const [rows, setRows] = useState<SchemaCatalogEntry[]>([])
  const [search, setSearch] = useState('')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    void listSchemaCatalog()
      .then((data) => {
        if (!cancelled) {
          setError(null)
          setRows(data)
        }
      })
      .catch((e) => {
        if (!cancelled) setError(String(e))
      })
    return () => {
      cancelled = true
    }
  }, [])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return rows
    return rows.filter((s) => {
      const hay = [s.type, s.title ?? '', s.description ?? '', s.latestVersion].join(' ').toLowerCase()
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
          <Text size="sm" fw={650} mb="xs">
            Schemas
          </Text>
          <TextInput
            size="xs"
            placeholder="Search schemas"
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
                {filtered.map((s) => {
                  const active = selectedType === s.type
                  return (
                    <UnstyledButton
                      key={s.type}
                      component={Link}
                      to={`/schemas/${encodeURIComponent(s.type)}`}
                      px={6}
                      py={6}
                      style={{
                        borderRadius: 4,
                        background: active ? 'var(--mantine-color-blue-light)' : undefined,
                      }}
                    >
                      <Group gap={6} wrap="nowrap">
                        <SchemaKindPill usage={s.usage} />
                        <Text size="xs" fw={active ? 700 : 500} truncate style={{ flex: 1, minWidth: 0 }}>
                          {s.type}
                        </Text>
                        <Text size="xs" c="dimmed" style={{ flexShrink: 0 }}>
                          {s.latestVersion}
                        </Text>
                      </Group>
                    </UnstyledButton>
                  )
                })}
                {filtered.length === 0 && (
                  <Text size="xs" c="dimmed">
                    No schemas
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
        <Outlet context={{ catalog: rows, error }} />
      </Paper>
    </div>
  )
}

export function useSchemasOutlet() {
  return useOutletContext<SchemasOutletContext>()
}
