import { Badge, Group, Stack, Text } from '@mantine/core'
import { AnnotationSplitPill } from './EntityCardNode'
import { nonEmptyAnnotations } from './GraphContextVersionControl'
import { parseGraphVersionTime } from './graphContextVersions'
import { formatVersionRowLabel, type ObjectVersionRow } from './objectVersionRows'

type Props = {
  row: ObjectVersionRow
  selected?: boolean
  /** Compact preview in Versions section vs browser pane. */
  density?: 'preview' | 'browser'
}

/**
 * Visual priority: date → annotations → version id (Note 5 polish).
 */
export function ObjectVersionRowContent({ row, selected, density = 'preview' }: Props) {
  const created = parseGraphVersionTime(row.createdAt, row.version ?? 0)
  const ann = Object.entries(nonEmptyAnnotations(row.annotations)).slice(0, 3)
  const isLatest = row.version == null
  const dateLabel = isLatest
    ? 'Latest'
    : created
      ? `${created.toLocaleDateString()} ${created.toLocaleTimeString()}`
      : '—'

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
      <Text
        size="xs"
        c="dimmed"
        ff="monospace"
        lh={1.2}
        title={row.version == null ? 'LATEST' : String(row.version)}
      >
        {formatVersionRowLabel(row.version)}
      </Text>
    </Stack>
  )
}
