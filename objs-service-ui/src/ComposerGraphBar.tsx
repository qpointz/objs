import { useState } from 'react'
import {
  ActionIcon,
  Button,
  Group,
  Paper,
  Text,
  Tooltip,
} from '@mantine/core'
import {
  IconAffiliate,
  IconCheck,
  IconCopy,
} from '@tabler/icons-react'
import { notifications } from '@mantine/notifications'
import { AnnotationsCountLabel } from './AnnotationsCountLabel'
import { shortId } from './graphContext'

export type ComposerGraphBarProps = {
  graphId: string | null
  annotations: Record<string, string>
  /** Read-only opened-graph version label (e.g. Latest); null when no saved graph id. */
  versionLabel?: string | null
  nodeCount: number
  edgeCount: number
  onOpenGraph: () => void
}

/**
 * Composer draft-graph chrome (Note 8 / Note 2 / Note 3). Visual match to {@link GraphContextBar};
 * New lives on view actions — Open stays here.
 */
export function ComposerGraphBar({
  graphId,
  annotations,
  versionLabel,
  nodeCount,
  edgeCount,
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
  ) as [string, string][]

  return (
    <Paper
      withBorder
      px="sm"
      py={6}
      radius="md"
      data-tour="composer-graph-bar"
      style={{ width: '100%', maxWidth: '100%' }}
    >
      <Group gap="sm" wrap="nowrap" justify="space-between" align="center" w="100%">
        <Group gap="xs" wrap="nowrap" style={{ flex: 1, minWidth: 0, overflow: 'hidden' }} align="center">
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

              <AnnotationsCountLabel
                entries={graphAnn}
                dataTour="composer-graph-annotations"
              />

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
            N/E: {nodeCount}/{edgeCount}
          </Text>
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
