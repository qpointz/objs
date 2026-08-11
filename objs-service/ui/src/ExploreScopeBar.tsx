import type { ReactNode, Ref } from 'react'
import { Button, Group, Paper, Stack, Tabs, Text } from '@mantine/core'
import { IconAffiliate, IconFilter } from '@tabler/icons-react'
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
  onModeChange: (mode: ExploreMode) => void
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
  /** Optional trailing chrome (Selection panel only). */
  extra?: ReactNode
}

/**
 * Explore-scope chrome: Graph | Selection pill switcher (same optical pattern as Open graph).
 * Each mode shows only its active content — no mixed Open-graph + Matcher row.
 */
export function ExploreScopeBar({
  mode,
  onModeChange,
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
    <Paper withBorder p="xs" radius="md">
      <Tabs
        value={mode}
        onChange={(v) => onModeChange((v as ExploreMode) ?? 'graph')}
        variant="pills"
        radius="md"
      >
        <Tabs.List grow mb="xs">
          <Tabs.Tab value="graph" leftSection={<IconAffiliate size={14} />}>
            Graph
          </Tabs.Tab>
          <Tabs.Tab value="selection" leftSection={<IconFilter size={14} />}>
            Selection
          </Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="graph">
          <Stack gap="xs">
            <Group gap="xs" align="center" wrap="wrap">
              <Button size="xs" variant="light" onClick={onOpenGraph}>
                Open graph…
              </Button>
              <Text size="xs" c="dimmed">
                Members of one opened graph
              </Text>
            </Group>
            {graphId ? (
              <GraphHeaderReadout
                graphId={graphId}
                annotations={graphAnnotations}
                compactId
              />
            ) : (
              <Text size="sm" c="dimmed">
                No graph opened — use Open graph…
              </Text>
            )}
          </Stack>
        </Tabs.Panel>

        <Tabs.Panel value="selection">
          <Stack gap="xs">
            <Group gap="xs" align="flex-start" wrap="wrap">
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
                    <Button size="sm" onClick={onExec} loading={execLoading}>
                      Exec
                    </Button>
                  }
                />
              </Stack>
              {extra}
            </Group>
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
                {lastMatcher != null
                  ? matcherBodyOneLiner(lastMatcher)
                  : 'Run Exec to load a selection'}
              </Text>
            </Group>
          </Stack>
        </Tabs.Panel>
      </Tabs>
    </Paper>
  )
}
