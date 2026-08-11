import type { ReactNode } from 'react'
import { Badge, Button, Group, Stack, Text, Tooltip } from '@mantine/core'
import { GraphHeaderReadout } from './GraphHeaderReadout'
import { shortGraphId } from './useCurrentGraph'

type Props = {
  graphId: string | null
  onOpenGraph: () => void
  onNewGraph: () => void
  /** Graph header annotations when known (enables shared GraphHeaderReadout). */
  annotations?: Record<string, string> | null
  /** Extra graph-lifecycle actions. */
  extra?: ReactNode
}

/**
 * Current-graph chrome shared by Composer / Query (WI-005). Explorer uses ExploreScopeBar.
 * When annotations are provided with a graph id, shows shared GraphHeaderReadout (id+copy+pills).
 */
export function CurrentGraphBar({ graphId, onOpenGraph, onNewGraph, annotations, extra }: Props) {
  return (
    <Stack gap={6}>
      <Group gap="xs" align="center" wrap="wrap">
        <Text size="sm" fw={500} style={{ flexShrink: 0 }}>
          Graph:
        </Text>
        {graphId && annotations == null ? (
          <Tooltip label={graphId} withArrow>
            <Badge variant="light" size="lg" style={{ fontFamily: 'var(--mantine-font-family-monospace)' }}>
              {shortGraphId(graphId)}
            </Badge>
          </Tooltip>
        ) : !graphId ? (
          <Badge variant="outline" color="orange" size="lg">
            No graph selected
          </Badge>
        ) : null}
        <Button size="compact-xs" variant="light" onClick={onOpenGraph}>
          Open graph…
        </Button>
        <Button size="compact-xs" variant="default" onClick={onNewGraph}>
          New graph
        </Button>
        {extra}
      </Group>
      {graphId && annotations != null && (
        <GraphHeaderReadout graphId={graphId} annotations={annotations} />
      )}
    </Stack>
  )
}
