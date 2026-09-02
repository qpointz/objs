import { useState, type MouseEvent } from 'react'
import { ActionIcon, Group, Stack, Text, Tooltip } from '@mantine/core'
import { IconCheck, IconCopy } from '@tabler/icons-react'
import { AnnotationSplitPill } from './EntityCardNode'
import { nonEmptyAnnotations } from './GraphContextVersionControl'
import { ObjectGraphActionsMenu } from './ObjectGraphActionsMenu'
import type { BoMGraphHeader } from './types'

function formatGraphDate(iso: string | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return `${d.toLocaleDateString()} ${d.toLocaleTimeString()}`
}

/** Keep head of id, truncate the tail — e.g. `34a1b2c3…`. */
export function formatGraphIdHead(id: string, head = 8): string {
  if (id.length <= head) return id
  return `${id.slice(0, head)}…`
}

type Props = {
  header: BoMGraphHeader
  density?: 'preview' | 'browser'
  onOpenAsContext: (graphId: string) => void
  onOpenInExplorer: (graphId: string) => void
  onEditInComposer: (graphId: string) => void
}

/**
 * ⋮ + id (+ copy) → annotations → date. Not selectable (Note1).
 */
export function ObjectGraphRowContent({
  header,
  density = 'preview',
  onOpenAsContext,
  onOpenInExplorer,
  onEditInComposer,
}: Props) {
  const [copied, setCopied] = useState(false)
  const ann = Object.entries(nonEmptyAnnotations(header.annotations)).slice(0, 3)
  const dateLabel = formatGraphDate(header.updatedAt ?? header.createdAt)

  async function copyId(e: MouseEvent) {
    e.stopPropagation()
    try {
      await navigator.clipboard.writeText(header.id)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1200)
    } catch {
      // ignore
    }
  }

  return (
    <Stack gap={4} style={{ minWidth: 0, width: '100%' }}>
      <Group gap={4} wrap="nowrap" align="center" style={{ minWidth: 0 }}>
        <ObjectGraphActionsMenu
          graphId={header.id}
          onOpenAsContext={onOpenAsContext}
          onOpenInExplorer={onOpenInExplorer}
          onEditInComposer={onEditInComposer}
        />
        <Text
          size={density === 'browser' ? 'sm' : 'xs'}
          fw={600}
          ff="monospace"
          style={{ whiteSpace: 'nowrap', minWidth: 0 }}
          lineClamp={1}
          title={header.id}
        >
          {formatGraphIdHead(header.id)}
        </Text>
        <Tooltip label={copied ? 'Copied' : 'Copy id'} withArrow>
          <ActionIcon
            size="xs"
            variant="subtle"
            aria-label="Copy graph id"
            data-tour="object-graph-copy-id"
            onClick={(e) => void copyId(e)}
          >
            {copied ? <IconCheck size={12} /> : <IconCopy size={12} />}
          </ActionIcon>
        </Tooltip>
      </Group>
      {ann.length > 0 && (
        <Group gap={4} wrap="wrap">
          {ann.map(([k, v]) => (
            <AnnotationSplitPill key={k} k={k} v={v} size="bar" />
          ))}
        </Group>
      )}
      <Text size="xs" c="dimmed" lh={1.2}>
        {dateLabel}
      </Text>
    </Stack>
  )
}
