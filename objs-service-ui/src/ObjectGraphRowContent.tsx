import { Badge, Group, Stack, Text } from '@mantine/core'
import { AnnotationSplitPill } from './EntityCardNode'
import { nonEmptyAnnotations } from './GraphContextVersionControl'
import type { BoMGraphHeader } from './types'

function formatGraphDate(iso: string | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return `${d.toLocaleDateString()} ${d.toLocaleTimeString()}`
}

function formatGraphIdLabel(id: string): string {
  if (id.length <= 8) return id
  return `…${id.slice(-6)}`
}

type Props = {
  header: BoMGraphHeader
  selected?: boolean
  density?: 'preview' | 'browser'
}

/**
 * Visual priority aligned with ObjectVersionRowContent: date → annotations → id.
 */
export function ObjectGraphRowContent({ header, selected, density = 'preview' }: Props) {
  const ann = Object.entries(nonEmptyAnnotations(header.annotations)).slice(0, 3)
  const dateLabel = formatGraphDate(header.updatedAt ?? header.createdAt)

  return (
    <Stack gap={4} style={{ minWidth: 0, width: '100%' }}>
      <Group gap={6} wrap="nowrap" align="center" justify="space-between">
        <Text
          size={density === 'browser' ? 'sm' : 'xs'}
          fw={600}
          style={{ whiteSpace: 'nowrap', minWidth: 0 }}
          lineClamp={1}
        >
          {dateLabel}
        </Text>
        {selected && (
          <Badge size="xs" variant="light" color="blue" style={{ flexShrink: 0 }}>
            selected
          </Badge>
        )}
      </Group>
      {ann.length > 0 && (
        <Group gap={4} wrap="wrap">
          {ann.map(([k, v]) => (
            <AnnotationSplitPill key={k} k={k} v={v} size="card" />
          ))}
        </Group>
      )}
      <Text size="xs" c="dimmed" ff="monospace" lh={1.2} title={header.id}>
        {formatGraphIdLabel(header.id)}
      </Text>
    </Stack>
  )
}
