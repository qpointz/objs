import { useState } from 'react'
import { ActionIcon, Group, Text, Tooltip } from '@mantine/core'
import { IconCheck, IconCopy } from '@tabler/icons-react'
import { notifications } from '@mantine/notifications'
import { AnnotationSplitPill } from './EntityCardNode'
import { colorForType } from './color'

const VALUE_TRUNCATE = 48

type Props = {
  graphId: string
  annotations?: Record<string, string> | null
  /** Show full id (default) or compact mono badge label. */
  compactId?: boolean
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
      onClick={() => setExpanded((e) => !e)}
      style={{
        display: 'inline-flex',
        alignItems: 'stretch',
        maxWidth: expanded ? '100%' : undefined,
        borderRadius: 999,
        overflow: 'hidden',
        border: `1px solid ${color}`,
        fontSize: 11,
        lineHeight: 1.2,
        background: '#fff',
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
          color: '#212529',
          fontWeight: 600,
          background: `color-mix(in srgb, ${color} 12%, #fff)`,
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
 * Shared graph-header chrome: full id + copy, annotation pills (No annotations / truncate+expand).
 * Used by ExploreScopeBar (Graph mode) and later Composer/Query CurrentGraphBar.
 */
export function GraphHeaderReadout({ graphId, annotations, compactId = false }: Props) {
  const [copied, setCopied] = useState(false)
  const entries = Object.entries(annotations ?? {})

  async function copyId() {
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

  return (
    <Group gap="xs" align="center" wrap="wrap" style={{ minWidth: 0 }}>
      <Group gap={4} wrap="nowrap" style={{ minWidth: 0, maxWidth: '100%' }}>
        <Text size="xs" c="dimmed" style={{ flexShrink: 0 }}>
          Id:
        </Text>
        <Tooltip label={graphId} withArrow disabled={!compactId || graphId.length <= 12}>
          <Text
            size="sm"
            ff="monospace"
            style={{
              minWidth: 0,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
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
            onClick={() => void copyId()}
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
    </Group>
  )
}
