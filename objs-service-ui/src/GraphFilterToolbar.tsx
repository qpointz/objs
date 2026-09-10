import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type ReactNode,
} from 'react'
import {
  ActionIcon,
  Checkbox,
  Group,
  Menu,
  ScrollArea,
  Text,
  TextInput,
  Tooltip,
} from '@mantine/core'
import {
  IconAlertTriangle,
  IconFilterOff,
  IconRoute,
  IconTags,
  IconX,
} from '@tabler/icons-react'

const CLOSE_DELAY_MS = 450
const MENU_WIDTH = 420
const MENU_MAX_HEIGHT = 480

const toolbarChrome: CSSProperties = {
  position: 'absolute',
  top: 8,
  left: 8,
  zIndex: 6,
  opacity: 0.45,
  transition: 'opacity 120ms ease',
  borderRadius: 8,
  background: 'color-mix(in srgb, var(--mantine-color-body) 78%, transparent)',
  border: '1px solid var(--mantine-color-default-border)',
  backdropFilter: 'blur(4px)',
  boxShadow: '0 1px 4px color-mix(in srgb, #000 12%, transparent)',
}

export type GraphFilterTypeOption = { value: string; label: string; color?: string }
export type GraphFilterSeverityOption = { value: string; label: string; color?: string }

type Props = {
  types: GraphFilterTypeOption[]
  selectedTypes: ReadonlySet<string>
  onToggleType: (type: string) => void
  edgeRoles: string[]
  selectedEdgeRoles: ReadonlySet<string>
  onToggleEdgeRole: (role: string) => void
  /** Policy / suites context — omit when not applicable. */
  severities?: GraphFilterSeverityOption[]
  selectedSeverities?: ReadonlySet<string>
  onToggleSeverity?: (severity: string) => void
  onReset: () => void
  disabled?: boolean
}

type OpenMenu = 'types' | 'edges' | 'severity' | null

function FilterCheckItem({
  checked,
  onToggle,
  children,
}: {
  checked: boolean
  onToggle: () => void
  children: ReactNode
}) {
  return (
    <Menu.Item
      closeMenuOnClick={false}
      onClick={onToggle}
      py={8}
      px="sm"
      leftSection={
        <Checkbox
          size="sm"
          checked={checked}
          readOnly
          tabIndex={-1}
          styles={{ input: { pointerEvents: 'none', cursor: 'pointer' } }}
        />
      }
    >
      {children}
    </Menu.Item>
  )
}

type SelectorRow = {
  key: string
  searchText: string
  checked: boolean
  onToggle: () => void
  content: ReactNode
}

/** Case-insensitive substring match on row search text. */
export function filterSelectorRows<T extends { searchText: string }>(
  rows: T[],
  query: string,
): T[] {
  const q = query.trim().toLowerCase()
  if (!q) return rows
  return rows.filter((row) => row.searchText.toLowerCase().includes(q))
}

/** Selected-first order; stable within each group. Recomputed only when menu opens. */
function selectedFirstKeys(rows: SelectorRow[]): string[] {
  return rows
    .map((row, index) => ({ row, index }))
    .sort((a, b) => {
      if (a.row.checked !== b.row.checked) return a.row.checked ? -1 : 1
      return a.index - b.index
    })
    .map(({ row }) => row.key)
}

function orderRowsByKeys(rows: SelectorRow[], orderKeys: string[]): SelectorRow[] {
  const byKey = new Map(rows.map((row) => [row.key, row]))
  const ordered: SelectorRow[] = []
  for (const key of orderKeys) {
    const row = byKey.get(key)
    if (row != null) {
      ordered.push(row)
      byKey.delete(key)
    }
  }
  for (const row of rows) {
    if (byKey.has(row.key)) ordered.push(row)
  }
  return ordered
}

function FilterSelectorDropdown({
  label,
  emptyMessage,
  rows,
  active,
  onMouseEnter,
  onMouseLeave,
}: {
  label: string
  emptyMessage: string
  rows: SelectorRow[]
  active: boolean
  onMouseEnter: () => void
  onMouseLeave: () => void
}) {
  const [query, setQuery] = useState('')
  const queryRef = useRef(query)
  queryRef.current = query
  const inputRef = useRef<HTMLInputElement>(null)
  const rowsRef = useRef(rows)
  rowsRef.current = rows
  const [pinnedOrder, setPinnedOrder] = useState<string[] | null>(null)
  const searchVisible = query.length > 0

  const focusSearch = useCallback(() => {
    requestAnimationFrame(() => inputRef.current?.focus())
  }, [])

  useEffect(() => {
    if (!active) {
      setQuery('')
      setPinnedOrder(null)
      return
    }
    setPinnedOrder(selectedFirstKeys(rowsRef.current))
    const t = window.setTimeout(() => inputRef.current?.focus(), 0)
    return () => clearTimeout(t)
  }, [active])

  // Fallback when focus is on a menu item: still type-to-filter, and block Mantine typeahead.
  useEffect(() => {
    if (!active) return
    const onKeyDown = (event: KeyboardEvent) => {
      const target = event.target
      const inOwn =
        target instanceof HTMLElement &&
        (target === inputRef.current || target.closest('[data-filter-typeahead]') != null)
      if (inOwn) return

      if (
        target instanceof HTMLElement &&
        (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable)
      ) {
        return
      }

      if (event.key === 'Escape' && queryRef.current.length > 0) {
        event.preventDefault()
        event.stopPropagation()
        setQuery('')
        focusSearch()
        return
      }

      if (event.key === 'Backspace' && queryRef.current.length > 0) {
        event.preventDefault()
        event.stopPropagation()
        setQuery((q) => q.slice(0, -1))
        focusSearch()
        return
      }

      if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
        event.preventDefault()
        event.stopPropagation()
        setQuery((q) => q + event.key)
        focusSearch()
      }
    }
    window.addEventListener('keydown', onKeyDown, true)
    return () => window.removeEventListener('keydown', onKeyDown, true)
  }, [active, focusSearch])

  const orderedRows = useMemo(() => {
    if (pinnedOrder == null) return rows
    return orderRowsByKeys(rows, pinnedOrder)
  }, [rows, pinnedOrder])

  const filtered = useMemo(
    () => filterSelectorRows(orderedRows, query),
    [orderedRows, query],
  )

  return (
    <Menu.Dropdown onMouseEnter={onMouseEnter} onMouseLeave={onMouseLeave}>
      {/* Always mounted: collapsed until the user types (avoids first-key mount/focus race). */}
      <TextInput
        ref={inputRef}
        data-filter-typeahead
        size="sm"
        placeholder="Filter…"
        value={query}
        onChange={(e) => setQuery(e.currentTarget.value)}
        mx="xs"
        mt={searchVisible ? 6 : 0}
        mb={searchVisible ? 4 : 0}
        aria-label="Filter list"
        rightSection={
          searchVisible ? (
            <ActionIcon
              size="sm"
              variant="subtle"
              color="gray"
              aria-label="Clear filter"
              onClick={() => {
                setQuery('')
                focusSearch()
              }}
            >
              <IconX size={14} />
            </ActionIcon>
          ) : (
            <span style={{ width: 22, display: 'inline-block' }} />
          )
        }
        styles={{
          root: searchVisible
            ? undefined
            : {
                height: 0,
                minHeight: 0,
                margin: 0,
                overflow: 'hidden',
                opacity: 0,
                pointerEvents: 'none',
              },
          wrapper: searchVisible ? undefined : { margin: 0 },
          input: searchVisible ? undefined : { height: 0, minHeight: 0, padding: 0, border: 0 },
        }}
        onKeyDown={(e) => {
          if (e.key === 'Escape' && query.length > 0) {
            e.preventDefault()
            e.stopPropagation()
            setQuery('')
          }
        }}
      />
      <Menu.Label fz="xs">{label}</Menu.Label>
      {rows.length === 0 ? (
        <Text size="sm" c="dimmed" px="sm" py={8}>
          {emptyMessage}
        </Text>
      ) : filtered.length === 0 ? (
        <Text size="sm" c="dimmed" px="sm" py={8}>
          No matches
        </Text>
      ) : (
        <ScrollArea.Autosize mah={MENU_MAX_HEIGHT}>
          {filtered.map((row) => (
            <FilterCheckItem
              key={row.key}
              checked={row.checked}
              onToggle={() => {
                row.onToggle()
                focusSearch()
              }}
            >
              {row.content}
            </FilterCheckItem>
          ))}
        </ScrollArea.Autosize>
      )}
    </Menu.Dropdown>
  )
}

/**
 * Hover-reveal filter control for graph canvases (Note 6).
 * Top-left; quiet until pointer enters. Complements GraphLayoutToolbar (top-right).
 */
export function GraphFilterToolbar({
  types,
  selectedTypes,
  onToggleType,
  edgeRoles,
  selectedEdgeRoles,
  onToggleEdgeRole,
  severities,
  selectedSeverities,
  onToggleSeverity,
  onReset,
  disabled,
}: Props) {
  const [openMenu, setOpenMenu] = useState<OpenMenu>(null)
  const closeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const rootRef = useRef<HTMLDivElement>(null)

  const typesActive = selectedTypes.size > 0
  const edgesActive = selectedEdgeRoles.size > 0
  const severityActive = (selectedSeverities?.size ?? 0) > 0
  const anyActive = typesActive || edgesActive || severityActive
  const showSeverity = severities != null && onToggleSeverity != null && selectedSeverities != null

  const clearCloseTimer = useCallback(() => {
    if (closeTimerRef.current != null) {
      clearTimeout(closeTimerRef.current)
      closeTimerRef.current = null
    }
  }, [])

  const scheduleClose = useCallback(() => {
    clearCloseTimer()
    closeTimerRef.current = setTimeout(() => setOpenMenu(null), CLOSE_DELAY_MS)
  }, [clearCloseTimer])

  const cancelClose = useCallback(() => {
    clearCloseTimer()
  }, [clearCloseTimer])

  useEffect(() => () => clearCloseTimer(), [clearCloseTimer])

  // Close when clicking outside toolbar + portal menus (e.g. canvas).
  useEffect(() => {
    if (openMenu == null) return
    const onPointerDown = (event: PointerEvent) => {
      const target = event.target
      if (!(target instanceof Element)) return
      if (rootRef.current?.contains(target)) return
      if (target.closest('.mantine-Menu-dropdown')) return
      setOpenMenu(null)
    }
    document.addEventListener('pointerdown', onPointerDown, true)
    return () => document.removeEventListener('pointerdown', onPointerDown, true)
  }, [openMenu])

  const iconBtn = (extraStyle?: CSSProperties): CSSProperties => ({
    borderRadius: 0,
    height: 34,
    width: 34,
    minHeight: 34,
    minWidth: 34,
    ...extraStyle,
  })

  const typeRows: SelectorRow[] = useMemo(
    () =>
      types.map((t) => ({
        key: t.value,
        searchText: `${t.label} ${t.value}`,
        checked: selectedTypes.has(t.value),
        onToggle: () => onToggleType(t.value),
        content: (
          <Group gap={10} wrap="nowrap">
            {t.color != null && (
              <span
                style={{
                  width: 12,
                  height: 12,
                  borderRadius: 2,
                  background: t.color,
                  flexShrink: 0,
                }}
              />
            )}
            <Text size="sm" truncate style={{ flex: 1, minWidth: 0 }}>
              {t.label}
            </Text>
          </Group>
        ),
      })),
    [types, selectedTypes, onToggleType],
  )

  const edgeRows: SelectorRow[] = useMemo(
    () =>
      edgeRoles.map((role) => ({
        key: role,
        searchText: role,
        checked: selectedEdgeRoles.has(role),
        onToggle: () => onToggleEdgeRole(role),
        content: (
          <Text size="sm" truncate>
            {role}
          </Text>
        ),
      })),
    [edgeRoles, selectedEdgeRoles, onToggleEdgeRole],
  )

  const severityRows: SelectorRow[] = useMemo(
    () =>
      (severities ?? []).map((sev) => ({
        key: sev.value,
        searchText: `${sev.label} ${sev.value}`,
        checked: selectedSeverities?.has(sev.value) ?? false,
        onToggle: () => onToggleSeverity?.(sev.value),
        content: (
          <Text size="sm" c={sev.color} truncate>
            {sev.label}
          </Text>
        ),
      })),
    [severities, selectedSeverities, onToggleSeverity],
  )

  return (
    <Group
      ref={rootRef}
      gap={0}
      wrap="nowrap"
      data-tour="graph-filter-toolbar"
      style={toolbarChrome}
      onMouseEnter={(e) => {
        cancelClose()
        e.currentTarget.style.opacity = '1'
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.opacity = '0.45'
        scheduleClose()
      }}
    >
      <Menu
        position="bottom-start"
        withinPortal
        width={MENU_WIDTH}
        opened={openMenu === 'types'}
        onChange={(next) => setOpenMenu(next ? 'types' : null)}
        closeOnItemClick={false}
        closeOnClickOutside={false}
      >
        <Menu.Target>
          <Tooltip label="Filter types" withArrow>
            <ActionIcon
              size={34}
              variant={typesActive ? 'light' : 'subtle'}
              color={typesActive ? 'blue' : 'gray'}
              aria-label="Filter types"
              disabled={disabled || types.length === 0}
              style={iconBtn({
                borderTopLeftRadius: 7,
                borderBottomLeftRadius: 7,
              })}
            >
              <IconTags size={18} stroke={1.5} />
            </ActionIcon>
          </Tooltip>
        </Menu.Target>
        <FilterSelectorDropdown
          label="Types"
          emptyMessage="No types on canvas"
          rows={typeRows}
          active={openMenu === 'types'}
          onMouseEnter={cancelClose}
          onMouseLeave={scheduleClose}
        />
      </Menu>

      <Menu
        position="bottom-start"
        withinPortal
        width={MENU_WIDTH}
        opened={openMenu === 'edges'}
        onChange={(next) => setOpenMenu(next ? 'edges' : null)}
        closeOnItemClick={false}
        closeOnClickOutside={false}
      >
        <Menu.Target>
          <Tooltip label="Filter edges by role" withArrow>
            <ActionIcon
              size={34}
              variant={edgesActive ? 'light' : 'subtle'}
              color={edgesActive ? 'blue' : 'gray'}
              aria-label="Filter edges by role"
              disabled={disabled || edgeRoles.length === 0}
              style={iconBtn({
                borderLeft: '1px solid var(--mantine-color-default-border)',
              })}
            >
              <IconRoute size={18} stroke={1.5} />
            </ActionIcon>
          </Tooltip>
        </Menu.Target>
        <FilterSelectorDropdown
          label="Edge roles"
          emptyMessage="No edges on canvas"
          rows={edgeRows}
          active={openMenu === 'edges'}
          onMouseEnter={cancelClose}
          onMouseLeave={scheduleClose}
        />
      </Menu>

      {showSeverity && (
        <Menu
          position="bottom-start"
          withinPortal
          width={MENU_WIDTH}
          opened={openMenu === 'severity'}
          onChange={(next) => setOpenMenu(next ? 'severity' : null)}
          closeOnItemClick={false}
          closeOnClickOutside={false}
        >
          <Menu.Target>
            <Tooltip label="Filter by finding severity" withArrow>
              <ActionIcon
                size={34}
                variant={severityActive ? 'light' : 'subtle'}
                color={severityActive ? 'violet' : 'gray'}
                aria-label="Filter by finding severity"
                disabled={disabled || severities.length === 0}
                style={iconBtn({
                  borderLeft: '1px solid var(--mantine-color-default-border)',
                })}
              >
                <IconAlertTriangle size={18} stroke={1.5} />
              </ActionIcon>
            </Tooltip>
          </Menu.Target>
          <FilterSelectorDropdown
            label="Severity"
            emptyMessage="Evaluate to annotate severities"
            rows={severityRows}
            active={openMenu === 'severity'}
            onMouseEnter={cancelClose}
            onMouseLeave={scheduleClose}
          />
        </Menu>
      )}

      <Tooltip label="Reset filters" withArrow>
        <ActionIcon
          size={34}
          variant="subtle"
          aria-label="Reset filters"
          disabled={disabled || !anyActive}
          onClick={onReset}
          style={iconBtn({
            borderTopRightRadius: 7,
            borderBottomRightRadius: 7,
            borderLeft: '1px solid var(--mantine-color-default-border)',
          })}
        >
          <IconFilterOff size={18} stroke={1.5} />
        </ActionIcon>
      </Tooltip>
    </Group>
  )
}
