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
  IconFile,
  IconFilter,
} from '@tabler/icons-react'
import { notifications } from '@mantine/notifications'
import { AnnotationSplitPill } from './EntityCardNode'
import { shortId } from './graphContext'

const ANN_MAX = 4

export type ComposerGraphBarProps = {
  graphId: string | null
  annotations: Record<string, string>
  /** Read-only opened-graph version label (e.g. Latest); null when no saved graph id. */
  versionLabel?: string | null
  nodeCount: number
  edgeCount: number
  onBlank: () => void
  onOpenMatcher: () => void
  onOpenGraph: () => void
}

/**
 * Composer draft-graph chrome (Note 8). Visual match to {@link GraphContextBar} only —
 * never uses shared graph context / GraphContextProvider.
 */
export function ComposerGraphBar({
  graphId,
  annotations,
  versionLabel,
  nodeCount,
  edgeCount,
  onBlank,
  onOpenMatcher,
  onOpenGraph,
}: ComposerGraphBarProps) {
  async function copyText(label: string, value: string) {
    try {
      await navigator.clipboard.writeText(value)
      notifications.show({ message: `${label} copied`, color: 'green', autoClose: 1500 })
    } catch {
      notifications.show({ message: `Could not copy ${label}`, color: 'red' })
    }
  }

  const graphAnn = Object.entries(annotations).filter(
    ([k, v]) => k.trim().length > 0 && v.trim().length > 0,
  )
  const shownGraphAnn = graphAnn.slice(0, ANN_MAX)
  const moreGraphAnn = graphAnn.length - shownGraphAnn.length

  return (
    <Paper withBorder px="sm" py={6} radius="md" data-tour="composer-graph-bar">
      <Group gap="sm" wrap="nowrap" justify="space-between" align="center">
        <Group gap="xs" wrap="nowrap" style={{ flex: 1, minWidth: 0 }} align="center">
          {graphId ? (
            <>
              <Tooltip label="Composer graph" withArrow>
                <IconAffiliate
                  size={18}
                  stroke={1.5}
                  color="var(--mantine-color-blue-filled)"
                  aria-label="Composer graph"
                />
              </Tooltip>
              <Tooltip label={graphId} withArrow>
                <Text
                  size="sm"
                  ff="monospace"
                  style={{ flexShrink: 0 }}
                  data-tour="composer-graph-id"
                >
                  {shortId(graphId)}
                </Text>
              </Tooltip>
              <CopyButton
                ariaLabel="Copy graph id"
                onCopy={() => void copyText('Graph id', graphId)}
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

                {versionLabel != null && versionLabel.length > 0 && (
                  <>
                    <Text size="xs" c="dimmed" fw={700} style={{ flexShrink: 0, opacity: 0.55 }}>
                      |
                    </Text>
                    <Text size="xs" c="dimmed" fw={600} style={{ flexShrink: 0 }}>
                      Version:
                    </Text>
                    <Text size="xs" c="dimmed" style={{ flexShrink: 0 }}>
                      {versionLabel}
                    </Text>
                  </>
                )}
              </Group>
            </>
          ) : (
            <>
              <IconAffiliate size={18} stroke={1.5} color="var(--mantine-color-dimmed)" />
              <Text size="sm" c="dimmed">
                No graph — New or Open
              </Text>
            </>
          )}
        </Group>

        <Group gap="xs" wrap="nowrap" style={{ flexShrink: 0 }}>
          <Text
            size="xs"
            c="dimmed"
            style={{ whiteSpace: 'nowrap' }}
            data-tour="composer-graph-stats"
          >
            Nodes {nodeCount} / Edges {edgeCount}
          </Text>
          <Menu shadow="md" width={160} position="bottom-end">
            <Menu.Target>
              <Button
                size="xs"
                variant="default"
                rightSection={<IconChevronDown size={14} />}
                data-tour="composer-new"
              >
                New
              </Button>
            </Menu.Target>
            <Menu.Dropdown>
              <Menu.Item leftSection={<IconFile size={14} />} onClick={onBlank}>
                Blank
              </Menu.Item>
              <Menu.Item leftSection={<IconFilter size={14} />} onClick={onOpenMatcher}>
                Matcher
              </Menu.Item>
            </Menu.Dropdown>
          </Menu>
          <Button
            size="xs"
            variant="light"
            onClick={onOpenGraph}
            data-tour="composer-open"
          >
            Open
          </Button>
        </Group>
      </Group>
    </Paper>
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
