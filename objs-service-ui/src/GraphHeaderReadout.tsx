import { useState, type MouseEvent, type ReactNode } from 'react'
import { ActionIcon, Box, Group, Stack, Text, Tooltip } from '@mantine/core'
import { IconAffiliate, IconCheck, IconCopy } from '@tabler/icons-react'
import { notifications } from '@mantine/notifications'
import { AnnotationSplitPill } from './EntityCardNode'
import { colorForType } from './color'

const VALUE_TRUNCATE = 48
/** Compact hit rows: fixed height, single line; readable panel-sized pills. */
const COMPACT_MAX_PILLS = 4
export const GRAPH_HEADER_COMPACT_ROW_HEIGHT = 52

type Density = 'comfortable' | 'compact'

type Props = {
  graphId: string
  annotations?: Record<string, string> | null
  /** Show full id (default) or compact mono badge label. */
  compactId?: boolean
  /**
   * `comfortable` — multi-line header chrome (bars).
   * `compact` — fixed-height single-line hit row (Open graph dialog).
   */
  density?: Density
  /** Optional trailing control (e.g. Open button in search hits). */
  action?: ReactNode
  /** Emphasize as a selectable hit card. */
  interactive?: boolean
  onClick?: () => void
}

function truncateValue(value: string, max = VALUE_TRUNCATE): { text: string; truncated: boolean } {
  if (value.length <= max) return { text: value || '∅', truncated: false }
  return { text: `${value.slice(0, max - 1)}…`, truncated: true }
}

/** Annotation pill with truncate + click-to-expand for long values (G-U3a). */
export function ExpandableAnnotationPill({ k, v }: { k: string; v: string }) {
  const [expanded, setExpanded] = useState(false)
  const { text, truncated } = truncateValue(v)
  const color = colorForType(k)

  if (!truncated) {
    return <AnnotationSplitPill k={k} v={v} size="panel" />
  }

  const display = expanded ? v || '∅' : text
  return (
    <button
      type="button"
      title={expanded ? `${k}=${v} (click to collapse)` : `${k}=${v} (click to expand)`}
      aria-expanded={expanded}
      onClick={(e) => {
        e.stopPropagation()
        setExpanded((prev) => !prev)
      }}
      style={{
        display: 'inline-flex',
        alignItems: 'stretch',
        maxWidth: expanded ? '100%' : undefined,
        borderRadius: 999,
        overflow: 'hidden',
        border: `1px solid ${color}`,
        fontSize: 11,
        lineHeight: 1.2,
        background: 'var(--mantine-color-body)',
        cursor: 'pointer',
        padding: 0,
        fontFamily: 'inherit',
      }}
    >
      <span
        style={{
          padding: '3px 8px',
          background: color,
          color: '#fff',
          fontWeight: 700,
          fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
          maxWidth: 140,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}
      >
        {k}
      </span>
      <span
        style={{
          padding: '3px 8px',
          color: 'var(--mantine-color-text)',
          fontWeight: 600,
          background: `color-mix(in srgb, ${color} 14%, var(--mantine-color-body))`,
          maxWidth: expanded ? 480 : 220,
          overflow: 'hidden',
          textOverflow: expanded ? undefined : 'ellipsis',
          whiteSpace: expanded ? 'normal' : 'nowrap',
          wordBreak: expanded ? 'break-word' : undefined,
          textAlign: 'left',
        }}
      >
        {display}
      </span>
    </button>
  )
}

/**
 * Graph-header chrome aligned with canvas entity cards: accent stripe, mono id + copy,
 * annotation pills. Used by Explore-scope, CurrentGraphBar, and Open-graph hits.
 */
export function GraphHeaderReadout({
  graphId,
  annotations,
  compactId = false,
  density = 'comfortable',
  action,
  interactive = false,
  onClick,
}: Props) {
  const [copied, setCopied] = useState(false)
  const entries = Object.entries(annotations ?? {})
  const accent = colorForType(graphId)
  const compact = density === 'compact'

  async function copyId(e?: MouseEvent) {
    e?.stopPropagation()
    try {
      await navigator.clipboard.writeText(graphId)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1500)
    } catch {
      notifications.show({
        color: 'orange',
        title: 'Copy failed',
        message: graphId,
        autoClose: 4000,
      })
    }
  }

  const idLabel = compactId && graphId.length > 12 ? `${graphId.slice(0, 8)}…` : graphId
  const visiblePills = compact ? entries.slice(0, COMPACT_MAX_PILLS) : entries
  const overflowCount = compact ? Math.max(0, entries.length - COMPACT_MAX_PILLS) : 0

  return (
    <Box
      style={{
        display: 'flex',
        width: '100%',
        flexShrink: 0,
        height: compact ? GRAPH_HEADER_COMPACT_ROW_HEIGHT : undefined,
        minHeight: compact ? GRAPH_HEADER_COMPACT_ROW_HEIGHT : undefined,
        maxHeight: compact ? GRAPH_HEADER_COMPACT_ROW_HEIGHT : undefined,
        textAlign: 'left',
        borderRadius: compact ? 8 : 12,
        overflow: 'hidden',
        border: `1px solid color-mix(in srgb, ${accent} 35%, var(--mantine-color-default-border))`,
        background: `linear-gradient(
          135deg,
          color-mix(in srgb, ${accent} 10%, var(--mantine-color-body)) 0%,
          var(--mantine-color-body) 48%
        )`,
        boxShadow: interactive
          ? '0 1px 0 color-mix(in srgb, var(--mantine-color-default-border) 80%, transparent)'
          : undefined,
        cursor: onClick ? 'pointer' : undefined,
        transition: onClick ? 'border-color 120ms ease' : undefined,
      }}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={
        onClick
          ? (e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault()
                onClick()
              }
            }
          : undefined
      }
      onMouseEnter={
        onClick
          ? (e) => {
              e.currentTarget.style.borderColor = accent
            }
          : undefined
      }
      onMouseLeave={
        onClick
          ? (e) => {
              e.currentTarget.style.borderColor = `color-mix(in srgb, ${accent} 35%, var(--mantine-color-default-border))`
            }
          : undefined
      }
    >
      <Box
        aria-hidden
        style={{
          width: compact ? 3 : 5,
          flexShrink: 0,
          background: `linear-gradient(180deg, ${accent}, color-mix(in srgb, ${accent} 45%, #111))`,
        }}
      />
      <Group
        gap={compact ? 8 : 'sm'}
        wrap="nowrap"
        align="center"
        px={compact ? 'xs' : 'sm'}
        py={compact ? 0 : 'sm'}
        style={{ flex: 1, minWidth: 0, height: compact ? '100%' : undefined }}
      >
        <Box
          aria-hidden
          style={{
            width: compact ? 24 : 34,
            height: compact ? 24 : 34,
            borderRadius: compact ? 7 : 10,
            flexShrink: 0,
            display: 'grid',
            placeItems: 'center',
            background: `color-mix(in srgb, ${accent} 18%, var(--mantine-color-body))`,
            border: `1px solid color-mix(in srgb, ${accent} 40%, transparent)`,
            color: accent,
          }}
        >
          <IconAffiliate size={compact ? 14 : 18} stroke={1.6} />
        </Box>

        {compact ? (
          <>
            <Group gap={4} wrap="nowrap" style={{ flexShrink: 0, maxWidth: '28%' }}>
              <Tooltip label={graphId} withArrow>
                <Text
                  size="sm"
                  ff="monospace"
                  fw={600}
                  style={{
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                    letterSpacing: '-0.01em',
                  }}
                >
                  {idLabel}
                </Text>
              </Tooltip>
              <Tooltip label={copied ? 'Copied' : 'Copy graph id'} withArrow>
                <ActionIcon
                  size="sm"
                  variant="subtle"
                  color={copied ? 'teal' : 'gray'}
                  aria-label="Copy graph id"
                  onClick={(e) => void copyId(e)}
                >
                  {copied ? <IconCheck size={14} /> : <IconCopy size={14} />}
                </ActionIcon>
              </Tooltip>
            </Group>
            <Box
              style={{
                flex: 1,
                minWidth: 0,
                overflow: 'hidden',
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                flexWrap: 'nowrap',
              }}
              title={
                entries.length > 0
                  ? entries.map(([k, v]) => `${k}=${v}`).join(' · ')
                  : 'No annotations'
              }
            >
              {entries.length === 0 ? (
                <Text size="sm" c="dimmed" fs="italic" style={{ whiteSpace: 'nowrap' }}>
                  No annotations
                </Text>
              ) : (
                <>
                  {visiblePills.map(([k, v]) => (
                    <span key={k} style={{ flexShrink: 0 }}>
                      <AnnotationSplitPill k={k} v={v} size="panel" />
                    </span>
                  ))}
                  {overflowCount > 0 && (
                    <Text size="sm" c="dimmed" fw={700} style={{ flexShrink: 0 }}>
                      +{overflowCount}
                    </Text>
                  )}
                </>
              )}
            </Box>
            {action ? <Box style={{ flexShrink: 0 }}>{action}</Box> : null}
          </>
        ) : (
          <>
            <Stack gap={6} style={{ flex: 1, minWidth: 0 }}>
              <Group gap={6} wrap="nowrap" style={{ minWidth: 0 }}>
                <Tooltip label={graphId} withArrow disabled={!compactId || graphId.length <= 12}>
                  <Text
                    size="sm"
                    ff="monospace"
                    fw={600}
                    style={{
                      minWidth: 0,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                      letterSpacing: '-0.01em',
                    }}
                  >
                    {idLabel}
                  </Text>
                </Tooltip>
                <Tooltip label={copied ? 'Copied' : 'Copy graph id'} withArrow>
                  <ActionIcon
                    size="sm"
                    variant="subtle"
                    color={copied ? 'teal' : 'gray'}
                    aria-label="Copy graph id"
                    onClick={(e) => void copyId(e)}
                  >
                    {copied ? <IconCheck size={14} /> : <IconCopy size={14} />}
                  </ActionIcon>
                </Tooltip>
              </Group>
              {entries.length === 0 ? (
                <Text size="xs" c="dimmed" fs="italic">
                  No annotations
                </Text>
              ) : (
                <Group gap={6} wrap="wrap">
                  {entries.map(([k, v]) => (
                    <ExpandableAnnotationPill key={k} k={k} v={v} />
                  ))}
                </Group>
              )}
            </Stack>
            {action}
          </>
        )}
      </Group>
    </Box>
  )
}
