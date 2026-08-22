import { useState } from 'react'
import {
  ActionIcon,
  Button,
  Group,
  Menu,
  Paper,
  Text,
  Tooltip,
} from '@mantine/core'
import {
  IconAffiliate,
  IconCheck,
  IconChevronDown,
  IconCopy,
  IconFilter,
  IconWorld,
} from '@tabler/icons-react'
import { notifications } from '@mantine/notifications'
import { execMatcher } from './api'
import { AnnotationSplitPill } from './EntityCardNode'
import { shortId } from './graphContext'
import { useGraphContext } from './GraphContextProvider'
import {
  GraphContextVersionControl,
  nonEmptyAnnotations,
} from './GraphContextVersionControl'
import { parseGraphVersionTime } from './graphContextVersions'
import { matcherBodyOneLiner } from './MatcherQueryForm'
import { OpenGraphModal } from './OpenGraphModal'
import { OpenMatcherModal } from './OpenMatcherModal'
import type { BoMGraphResponse } from './types'

const ANN_MAX = 4
const ALL_MATCHER = { all: true } as const

function isAllMatcher(body: unknown): boolean {
  if (body == null || typeof body !== 'object' || Array.isArray(body)) return false
  const o = body as Record<string, unknown>
  return o.all === true && Object.keys(o).length === 1
}

type Props = {
  /** Explorer: apply matcher result onto the canvas. */
  onMatcherApplied?: (contents: { entities: unknown[]; edges: unknown[] }, body: unknown) => void
  /** Explorer: apply opened graph onto the canvas (in addition to shared context). */
  onGraphOpened?: (graphId: string, resolved: BoMGraphResponse) => void
}

/**
 * Slim shared graph-context chrome (Note 1 Pic4/Pic5) for Explorer / Objects / Query.
 */
export function GraphContextBar({ onMatcherApplied, onGraphOpened }: Props) {
  const { context, setGraph, setMatcher } = useGraphContext()
  const [openGraph, setOpenGraph] = useState(false)
  const [openMatcher, setOpenMatcher] = useState(false)
  const [openingAll, setOpeningAll] = useState(false)

  async function copyText(label: string, value: string) {
    try {
      await navigator.clipboard.writeText(value)
      notifications.show({ message: `${label} copied`, color: 'green', autoClose: 1500 })
    } catch {
      notifications.show({ message: `Could not copy ${label}`, color: 'red' })
    }
  }

  async function openAll() {
    setOpeningAll(true)
    try {
      const contents = await execMatcher('all', ALL_MATCHER, null)
      const entities = contents.entities ?? []
      const edges = contents.edges ?? []
      const line = matcherBodyOneLiner(ALL_MATCHER)
      setMatcher(ALL_MATCHER, line, {
        nodeCount: entities.length,
        edgeCount: edges.length,
      })
      onMatcherApplied?.({ entities, edges }, ALL_MATCHER)
    } catch (e) {
      notifications.show({
        message: e instanceof Error ? e.message : String(e),
        color: 'red',
        title: 'Open All failed',
      })
    } finally {
      setOpeningAll(false)
    }
  }

  const graphAnn = Object.entries(context.annotations).filter(
    ([k, v]) => k.trim().length > 0 && v.trim().length > 0,
  )
  const shownGraphAnn = graphAnn.slice(0, ANN_MAX)
  const moreGraphAnn = graphAnn.length - shownGraphAnn.length

  const versionAnn =
    context.kind === 'graph'
      ? Object.entries(nonEmptyAnnotations(context.graphVersionAnnotations))
      : []
  const shownVersionAnn = versionAnn.slice(0, ANN_MAX)
  const moreVersionAnn = versionAnn.length - shownVersionAnn.length
  const versionCreated =
    context.kind === 'graph' && context.graphVersion != null
      ? parseGraphVersionTime(context.graphVersionCreatedAt ?? undefined, context.graphVersion)
      : null

  return (
    <>
      <Paper withBorder px="sm" py={6} radius="md" data-tour="graph-context">
        <Group gap="sm" wrap="nowrap" justify="space-between" align="center">
          <Group gap="xs" wrap="nowrap" style={{ flex: 1, minWidth: 0 }} align="center">
            {context.kind === 'graph' && context.graphId ? (
              <>
                <Tooltip label="Graph context" withArrow>
                  <IconAffiliate
                    size={18}
                    stroke={1.5}
                    color="var(--mantine-color-blue-filled)"
                    aria-label="Graph context"
                  />
                </Tooltip>
                <Tooltip label={context.graphId} withArrow>
                  <Text
                    size="sm"
                    ff="monospace"
                    style={{ flexShrink: 0 }}
                    data-tour="graph-context-id"
                  >
                    {shortId(context.graphId)}
                  </Text>
                </Tooltip>
                <CopyButton
                  ariaLabel="Copy graph id"
                  onCopy={() => void copyText('Graph id', context.graphId!)}
                />

                <Group gap={6} wrap="nowrap" style={{ flex: 1, minWidth: 0 }} align="center">
                  <Group
                    gap={4}
                    wrap="nowrap"
                    style={{ minWidth: 0, overflow: 'hidden' }}
                    align="center"
                  >
                    {shownGraphAnn.length === 0 ? (
                      <Text size="xs" c="dimmed" fs="italic">
                        none
                      </Text>
                    ) : (
                      <>
                        {shownGraphAnn.map(([k, v]) => (
                          <AnnotationSplitPill key={`g-${k}`} k={k} v={v} size="bar" />
                        ))}
                        {moreGraphAnn > 0 && (
                          <Text size="xs" c="dimmed" style={{ flexShrink: 0 }}>
                            +{moreGraphAnn}
                          </Text>
                        )}
                      </>
                    )}
                  </Group>

                  <Text size="xs" c="dimmed" fw={700} style={{ flexShrink: 0, opacity: 0.55 }}>
                    |
                  </Text>

                  <Text size="xs" c="dimmed" fw={600} style={{ flexShrink: 0 }}>
                    Version:
                  </Text>
                  <GraphContextVersionControl />
                  {versionCreated && (
                    <Text size="xs" c="dimmed" style={{ whiteSpace: 'nowrap', flexShrink: 0 }}>
                      {versionCreated.toLocaleDateString()} {versionCreated.toLocaleTimeString()}
                    </Text>
                  )}
                  {shownVersionAnn.length > 0 && (
                    <Group
                      gap={4}
                      wrap="nowrap"
                      style={{ minWidth: 0, overflow: 'hidden' }}
                      align="center"
                    >
                      {shownVersionAnn.map(([k, v]) => (
                        <AnnotationSplitPill key={`v-${k}`} k={k} v={v} size="bar" />
                      ))}
                      {moreVersionAnn > 0 && (
                        <Text size="xs" c="dimmed" style={{ flexShrink: 0 }}>
                          +{moreVersionAnn}
                        </Text>
                      )}
                    </Group>
                  )}
                </Group>
              </>
            ) : context.kind === 'matcher' && isAllMatcher(context.matcherBody) ? (
              <>
                <Tooltip label="All graphs" withArrow>
                  <IconWorld
                    size={18}
                    stroke={1.5}
                    color="var(--mantine-color-blue-filled)"
                    aria-label="All graphs"
                    data-tour="graph-context-all"
                  />
                </Tooltip>
                <Text size="sm" fw={500} style={{ flexShrink: 0 }}>
                  All
                </Text>
              </>
            ) : context.kind === 'matcher' ? (
              <>
                <Tooltip label="Matcher context" withArrow>
                  <IconFilter
                    size={18}
                    stroke={1.5}
                    color="var(--mantine-color-blue-filled)"
                    aria-label="Matcher context"
                  />
                </Tooltip>
                <Tooltip label={context.matcherLine ?? ''} withArrow disabled={!context.matcherLine}>
                  <Text
                    size="sm"
                    ff="monospace"
                    lineClamp={1}
                    style={{ flex: 1, minWidth: 0 }}
                    data-tour="graph-context-matcher"
                  >
                    {context.matcherLine?.trim() || '(empty matcher)'}
                  </Text>
                </Tooltip>
                {context.matcherLine && (
                  <CopyButton
                    ariaLabel="Copy matcher expression"
                    onCopy={() => void copyText('Matcher', context.matcherLine!)}
                  />
                )}
              </>
            ) : (
              <>
                <IconAffiliate size={18} stroke={1.5} color="var(--mantine-color-dimmed)" />
                <Text size="sm" c="dimmed">
                  No graph context — Open a graph or matcher
                </Text>
              </>
            )}
          </Group>

          <Group gap="xs" wrap="nowrap" style={{ flexShrink: 0 }}>
            {(context.kind === 'graph' || context.kind === 'matcher') && (
              <Text
                size="xs"
                c="dimmed"
                style={{ whiteSpace: 'nowrap' }}
                data-tour="graph-context-stats"
              >
                Nodes {context.nodeCount} / Edges {context.edgeCount}
              </Text>
            )}
            <Menu shadow="md" width={160} position="bottom-end">
              <Menu.Target>
                <Button
                  size="xs"
                  variant="light"
                  rightSection={<IconChevronDown size={14} />}
                  data-tour="graph-context-open"
                  loading={openingAll}
                >
                  Open
                </Button>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item
                  leftSection={<IconAffiliate size={14} />}
                  onClick={() => setOpenGraph(true)}
                >
                  Graph
                </Menu.Item>
                <Menu.Item
                  leftSection={<IconFilter size={14} />}
                  onClick={() => setOpenMatcher(true)}
                >
                  Matcher
                </Menu.Item>
                <Menu.Item
                  leftSection={<IconWorld size={14} />}
                  onClick={() => void openAll()}
                >
                  All
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
        </Group>
      </Paper>

      <OpenGraphModal
        opened={openGraph}
        onClose={() => setOpenGraph(false)}
        onOpen={(graphId, resolved) => {
          const entities = resolved.graph?.entities?.length ?? 0
          const edges = resolved.graph?.edges?.length ?? 0
          setGraph(graphId, resolved.annotations ?? {}, {
            nodeCount: entities,
            edgeCount: edges,
          })
          onGraphOpened?.(graphId, resolved)
          setOpenGraph(false)
        }}
      />
      <OpenMatcherModal
        opened={openMatcher}
        onClose={() => setOpenMatcher(false)}
        scopeGraphId={null}
        onApplied={onMatcherApplied}
      />
    </>
  )
}

function CopyButton({ ariaLabel, onCopy }: { ariaLabel: string; onCopy: () => void }) {
  const [done, setDone] = useState(false)
  return (
    <ActionIcon
      size="sm"
      variant="subtle"
      aria-label={ariaLabel}
      onClick={() => {
        onCopy()
        setDone(true)
        window.setTimeout(() => setDone(false), 1200)
      }}
    >
      {done ? <IconCheck size={14} /> : <IconCopy size={14} />}
    </ActionIcon>
  )
}
