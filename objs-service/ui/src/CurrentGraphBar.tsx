import type { ReactNode } from 'react'
import { Badge, Button, Group, Text, Tooltip } from '@mantine/core'
import { shortGraphId } from './useCurrentGraph'

type Props = {
  graphId: string | null
  onOpenGraph: () => void
  onNewGraph: () => void
  /** Extra graph-lifecycle actions (e.g. Composer's Save ▾ with Clone). */
  extra?: ReactNode
}

/**
 * Current-graph chrome shared by Explorer / Composer / Query (WI-005): shows the selected graph
 * (or a clear "no graph" CTA) plus Open / New graph actions.
 */
export function CurrentGraphBar({ graphId, onOpenGraph, onNewGraph, extra }: Props) {
  return (
    <Group gap="xs" align="center" wrap="wrap">
      <Text size="sm" fw={500} style={{ flexShrink: 0 }}>
        Graph:
      </Text>
      {graphId ? (
        <Tooltip label={graphId} withArrow>
          <Badge variant="light" size="lg" style={{ fontFamily: 'var(--mantine-font-family-monospace)' }}>
            {shortGraphId(graphId)}
          </Badge>
        </Tooltip>
      ) : (
        <Badge variant="outline" color="orange" size="lg">
          No graph selected
        </Badge>
      )}
      <Button size="compact-xs" variant="light" onClick={onOpenGraph}>
        Open graph…
      </Button>
      <Button size="compact-xs" variant="default" onClick={onNewGraph}>
        New graph
      </Button>
      {extra}
    </Group>
  )
}
