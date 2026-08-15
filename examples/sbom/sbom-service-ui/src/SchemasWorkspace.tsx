import { Paper, ScrollArea, Stack, Text, UnstyledButton } from '@mantine/core'
import { useEffect, useMemo, useState } from 'react'
import { Link, Outlet, useLocation, useOutletContext } from 'react-router-dom'
import { api } from './api/client'
import { SearchInput } from './SearchInput'
import type { SchemaCatalogEntry } from './api/types'

function schemaTypeFromPath(pathname: string): string | undefined {
  const match = pathname.match(/^\/schemas\/([^/]+)/)
  if (!match) return undefined
  return decodeURIComponent(match[1])
}

function isPortalPath(pathname: string): boolean {
  return pathname === '/schemas' || pathname === '/schemas/'
}

export type SchemasOutletContext = {
  catalog: SchemaCatalogEntry[]
  usagePending: Set<string>
  error: string | null
}

export function SchemasWorkspace() {
  const location = useLocation()
  const selectedType = schemaTypeFromPath(location.pathname)
  const showNav = !isPortalPath(location.pathname)
  const [rows, setRows] = useState<SchemaCatalogEntry[]>([])
  const [catalogTypes, setCatalogTypes] = useState<string[]>([])
  const [usagePending, setUsagePending] = useState<Set<string>>(new Set())
  const [search, setSearch] = useState('')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    void api
      .listSchemaCatalog()
      .then((data) => {
        if (cancelled) return
        setError(null)
        setRows(data)
        setUsagePending(new Set(data.map((s) => s.type)))
        setCatalogTypes(data.map((s) => s.type))
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e))
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (catalogTypes.length === 0) return
    let cancelled = false
    void (async () => {
      for (const type of catalogTypes) {
        if (cancelled) return
        try {
          const usedIn = await api.getSchemaUsedIn(type)
          if (cancelled) return
          setRows((prev) => prev.map((s) => (s.type === type ? { ...s, usedIn } : s)))
        } catch {
          if (cancelled) return
        } finally {
          if (!cancelled) {
            setUsagePending((prev) => {
              const next = new Set(prev)
              next.delete(type)
              return next
            })
          }
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [catalogTypes])

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
          <SearchInput
            size="xs"
            placeholder="Search schemas"
            value={search}
            onValueChange={setSearch}
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
                      <Text size="xs" fw={active ? 700 : 500} truncate>
                        {s.type}
                      </Text>
                      <Text size="xs" c="dimmed" truncate>
                        {s.latestVersion}
                      </Text>
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
        <Outlet context={{ catalog: rows, usagePending, error }} />
      </Paper>
    </div>
  )
}

export function useSchemasOutlet() {
  return useOutletContext<SchemasOutletContext>()
}
