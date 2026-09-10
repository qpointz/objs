import { type ReactNode, useState } from 'react'
import { ActionIcon, Checkbox, Group, Menu, ScrollArea, Text, Tooltip } from '@mantine/core'
import { IconFilter } from '@tabler/icons-react'

export type ColumnFilterOption = { value: string; label: string }

type Props = {
  label: string
  options: ColumnFilterOption[]
  selected: ReadonlySet<string>
  onToggle: (value: string) => void
  onClear?: () => void
  emptyMessage?: string
  /** Wider menu for long type names. */
  menuWidth?: number
}

/**
 * Table column title + funnel that opens a multi-select checklist (Note 7).
 * Quiet until the filter is active (icon highlights).
 */
export function ColumnFilterHeader({
  label,
  options,
  selected,
  onToggle,
  onClear,
  emptyMessage = 'No values',
  menuWidth = 240,
}: Props) {
  const [opened, setOpened] = useState(false)
  const active = selected.size > 0

  return (
    <Group
      gap={4}
      wrap="nowrap"
      justify="space-between"
      style={{ width: '100%' }}
      onMouseDown={(e) => e.stopPropagation()}
      onClick={(e) => e.stopPropagation()}
    >
      <Text size="xs" fw={600} style={{ flex: 1, minWidth: 0 }} truncate>
        {label}
      </Text>
      <Menu
        opened={opened}
        onChange={setOpened}
        position="bottom-start"
        withinPortal
        width={menuWidth}
        closeOnItemClick={false}
        shadow="md"
        zIndex={400}
      >
        <Menu.Target>
          <Tooltip label={`Filter ${label.toLowerCase()}`} withArrow>
            <ActionIcon
              size={22}
              variant={active ? 'light' : 'subtle'}
              color={active ? 'blue' : 'gray'}
              aria-label={`Filter ${label}`}
              aria-expanded={opened}
            >
              <IconFilter size={14} stroke={1.5} />
            </ActionIcon>
          </Tooltip>
        </Menu.Target>
        <Menu.Dropdown
          onMouseDown={(e) => e.stopPropagation()}
          onClick={(e) => e.stopPropagation()}
        >
          <Group justify="space-between" px="xs" pt={4} pb={2} wrap="nowrap">
            <Menu.Label p={0}>{label}</Menu.Label>
            {active && onClear != null && (
              <Text
                size="xs"
                c="blue"
                style={{ cursor: 'pointer', flexShrink: 0 }}
                onClick={() => onClear()}
              >
                Clear
              </Text>
            )}
          </Group>
          {options.length === 0 ? (
            <Text size="sm" c="dimmed" px="sm" py={8}>
              {emptyMessage}
            </Text>
          ) : (
            <ScrollArea.Autosize mah={320}>
              {options.map((opt) => {
                const checked = selected.has(opt.value)
                return (
                  <Menu.Item
                    key={opt.value}
                    closeMenuOnClick={false}
                    py={8}
                    px="sm"
                    onClick={() => onToggle(opt.value)}
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
                    <Text size="sm" truncate>
                      {opt.label}
                    </Text>
                  </Menu.Item>
                )
              })}
            </ScrollArea.Autosize>
          )}
        </Menu.Dropdown>
      </Menu>
    </Group>
  )
}

/** Convenience when header is only plain text elsewhere. */
export function plainColumnHeader(label: string): ReactNode {
  return (
    <Text size="xs" fw={600}>
      {label}
    </Text>
  )
}
