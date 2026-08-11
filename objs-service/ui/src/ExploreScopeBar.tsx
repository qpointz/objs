import type { ReactNode, Ref } from 'react'
import { Badge, Button, Group, Paper, Stack, Text } from '@mantine/core'
import {
  MatcherQueryForm,
  matcherBodyOneLiner,
  type MatcherQueryFormHandle,
} from './MatcherQueryForm'
import { GraphHeaderReadout } from './GraphHeaderReadout'
import type { QueryExecStats } from './queryExecStats'

export type ExploreMode = 'graph' | 'selection'

export type ExploreScopeBarProps = {
  mode: ExploreMode
  /** Opened graph id when mode is Graph (null if none opened yet). */
  graphId: string | null
  graphAnnotations?: Record<string, string> | null
  objectCount: number
  edgeCount: number
  /** Last executed matcher body for Selection summary one-liner. */
  lastMatcher?: unknown | null
  onOpenGraph: () => void
  matcherRef?: Ref<MatcherQueryFormHandle>
  storedMatcher?: unknown | null
  formError?: string | null
  execStats?: QueryExecStats | null
  execLoading?: boolean
  onExec: () => void
  /** Optional trailing chrome in the Open graph / Matcher row. */
  extra?: ReactNode
}

/**
 * United Explore-scope fragment (WI-002): Mode + Open graph ∪ Matcher + always-visible summary.
 * Optical either/or Graph | Selection — mode is controlled by the page (Open graph / Exec).
 */
export function ExploreScopeBar({
  mode,
  graphId,
  graphAnnotations,
  objectCount,
  edgeCount,
  lastMatcher,
  onOpenGraph,
  matcherRef,
  storedMatcher,
  formError,
  execStats,
  execLoading,
  onExec,
  extra,
}: ExploreScopeBarProps) {
  return (
    <Paper withBorder p="xs">
      <Stack gap="xs">
        <Group gap="xs" align="center" wrap="wrap">
          <Text size="sm" fw={500} style={{ flexShrink: 0 }}>
            Mode:
          </Text>
          <Badge
            size="lg"
            variant={mode === 'graph' ? 'filled' : 'outline'}
            color={mode === 'graph' ? 'blue' : 'gray'}
          >
            Graph
          </Badge>
          <Badge
            size="lg"
            variant={mode === 'selection' ? 'filled' : 'outline'}
            color={mode === 'selection' ? 'blue' : 'gray'}
          >
            Selection
          </Badge>
          <Text size="xs" c="dimmed">
            {mode === 'graph' ? 'Opened graph' : 'Matcher selection'}
          </Text>
        </Group>

        <Group gap="xs" align="flex-start" wrap="wrap">
          <Button size="compact-xs" variant="light" onClick={onOpenGraph}>
            Open graph…
          </Button>
          <Text size="xs" c="dimmed" pt={4}>
            or
          </Text>
          <Stack gap={4} style={{ flex: 1, minWidth: 240 }}>
            <MatcherQueryForm
              ref={matcherRef}
              emptyDefaults
              matcher={storedMatcher}
              error={formError}
              stats={execStats}
              collapsible
              defaultCollapsed={false}
              collapseStorageKey="objs.ui.graphExplorer.matcherCollapsed"
              action={
                <Button size="xs" onClick={onExec} loading={execLoading}>
                  Exec
                </Button>
              }
            />
          </Stack>
          {extra}
        </Group>

        {mode === 'graph' ? (
          graphId ? (
            <GraphHeaderReadout graphId={graphId} annotations={graphAnnotations} />
          ) : (
            <Text size="sm" c="dimmed">
              No graph opened — use Open graph… or run a matcher.
            </Text>
          )
        ) : (
          <Group gap="xs" wrap="wrap" align="center">
            <Text size="sm">
              {objectCount} object{objectCount === 1 ? '' : 's'} / {edgeCount} edge
              {edgeCount === 1 ? '' : 's'}
            </Text>
            <Text size="xs" c="dimmed">
              ·
            </Text>
            <Text
              size="sm"
              c="dimmed"
              style={{
                minWidth: 0,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                flex: 1,
              }}
              title={matcherBodyOneLiner(lastMatcher)}
            >
              {matcherBodyOneLiner(lastMatcher)}
            </Text>
          </Group>
        )}
      </Stack>
    </Paper>
  )
}
