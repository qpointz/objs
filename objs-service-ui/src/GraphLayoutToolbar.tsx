import { type CSSProperties, type ReactNode } from 'react'
import { ActionIcon, Group, Menu, Tooltip } from '@mantine/core'
import {
  IconChevronDown,
  IconChevronRight,
  IconLayoutDashboard,
  IconMaximize,
} from '@tabler/icons-react'

/** Shared with GraphCanvas / catalog overview. */
export type GraphLayoutDirection = 'TB' | 'LR' | 'BT' | 'RL'

export const GRAPH_LAYOUT_OPTIONS: { value: GraphLayoutDirection; label: string }[] = [
  { value: 'TB', label: 'Top to bottom' },
  { value: 'LR', label: 'Left to right' },
  { value: 'BT', label: 'Bottom to top' },
  { value: 'RL', label: 'Right to left' },
]

const iconBtn = (extra?: CSSProperties): CSSProperties => ({
  borderRadius: 0,
  height: 34,
  width: 34,
  minHeight: 34,
  minWidth: 34,
  ...extra,
})

type Props = {
  layout: GraphLayoutDirection
  disabled?: boolean
  /** Apply current layout direction. */
  onApply: () => void
  /** Switch direction (parent updates `layout`) then typically apply. */
  onLayoutChange: (layout: GraphLayoutDirection) => void
  /** Zoom/pan so the graph (or non-dimmed subset) fits the viewport. */
  onFitView: () => void
}

/**
 * Hover-reveal layout control for graph canvases (Note 4).
 * Sits top-right; quiet until pointer enters the cluster.
 */
export function GraphLayoutToolbar({ layout, disabled, onApply, onLayoutChange, onFitView }: Props) {
  return (
    <Group
      gap={0}
      wrap="nowrap"
      data-tour="graph-layout-toolbar"
      style={{
        position: 'absolute',
        top: 8,
        right: 8,
        zIndex: 6,
        opacity: 0.45,
        transition: 'opacity 120ms ease',
        borderRadius: 8,
        background: 'color-mix(in srgb, var(--mantine-color-body) 78%, transparent)',
        border: '1px solid var(--mantine-color-default-border)',
        backdropFilter: 'blur(4px)',
        boxShadow: '0 1px 4px color-mix(in srgb, #000 12%, transparent)',
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.opacity = '1'
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.opacity = '0.45'
      }}
    >
      <Tooltip label="Apply layout" withArrow>
        <ActionIcon
          size={34}
          variant="subtle"
          aria-label="Apply layout"
          disabled={disabled}
          onClick={onApply}
          style={iconBtn({
            borderTopLeftRadius: 7,
            borderBottomLeftRadius: 7,
          })}
        >
          <IconLayoutDashboard size={18} stroke={1.5} />
        </ActionIcon>
      </Tooltip>
      <Menu position="bottom-end" withinPortal>
        <Menu.Target>
          <ActionIcon
            size={34}
            variant="subtle"
            aria-label="Choose graph layout"
            disabled={disabled}
            style={iconBtn({
              borderLeft: '1px solid var(--mantine-color-default-border)',
            })}
          >
            <IconChevronDown size={16} />
          </ActionIcon>
        </Menu.Target>
        <Menu.Dropdown miw={220}>
          <Menu.Label fz="sm">Layout direction</Menu.Label>
          {GRAPH_LAYOUT_OPTIONS.map((option) => (
            <Menu.Item
              key={option.value}
              py={10}
              px="sm"
              onClick={() => {
                if (option.value === layout) onApply()
                else onLayoutChange(option.value)
              }}
            >
              {option.value === layout ? '✓ ' : ''}
              {option.label}
            </Menu.Item>
          ))}
        </Menu.Dropdown>
      </Menu>
      <Tooltip label="Fit to view" withArrow>
        <ActionIcon
          size={34}
          variant="subtle"
          aria-label="Fit to view"
          disabled={disabled}
          onClick={onFitView}
          style={iconBtn({
            borderTopRightRadius: 7,
            borderBottomRightRadius: 7,
            borderLeft: '1px solid var(--mantine-color-default-border)',
          })}
        >
          <IconMaximize size={18} stroke={1.5} />
        </ActionIcon>
      </Tooltip>
    </Group>
  )
}

/** Cascade submenu (Mantine 7 has no Menu.Sub) — local copy to avoid import cycles. */
function LayoutCascadeSub({
  label,
  leftSection,
  children,
}: {
  label: string
  leftSection?: ReactNode
  children: ReactNode
}) {
  return (
    <Menu trigger="hover" openDelay={0} closeDelay={120} position="right-start" offset={4} withinPortal={false}>
      <Menu.Target>
        <Menu.Item
          closeMenuOnClick={false}
          leftSection={leftSection}
          rightSection={<IconChevronRight size={14} />}
        >
          {label}
        </Menu.Item>
      </Menu.Target>
      <Menu.Dropdown>{children}</Menu.Dropdown>
    </Menu>
  )
}

/** Canvas context menu: Apply layout ▸ direction items, then Fit to view. */
export function GraphLayoutMenuItems({
  layout,
  disabled,
  onApply,
  onLayoutChange,
  onFitView,
  onDone,
}: {
  layout: GraphLayoutDirection
  disabled?: boolean
  onApply: () => void
  onLayoutChange: (layout: GraphLayoutDirection) => void
  onFitView: () => void
  onDone?: () => void
}) {
  if (disabled) return null
  return (
    <>
      <LayoutCascadeSub
        label="Apply layout"
        leftSection={<IconLayoutDashboard size={14} />}
      >
        {GRAPH_LAYOUT_OPTIONS.map((option) => (
          <Menu.Item
            key={option.value}
            onClick={() => {
              onDone?.()
              if (option.value === layout) onApply()
              else onLayoutChange(option.value)
            }}
          >
            {option.value === layout ? '✓ ' : ''}
            {option.label}
          </Menu.Item>
        ))}
      </LayoutCascadeSub>
      <Menu.Item
        leftSection={<IconMaximize size={14} />}
        onClick={() => {
          onDone?.()
          onFitView()
        }}
      >
        Fit to view
      </Menu.Item>
    </>
  )
}
