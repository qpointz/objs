import { ActionIcon, Text, Tooltip } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { IconCopy } from '@tabler/icons-react'
import type { MouseEvent } from 'react'

export async function copyIdToClipboard(id: string, label = 'Id copied') {
  try {
    await navigator.clipboard.writeText(id)
    notifications.show({
      color: 'teal',
      message: label,
      autoClose: 2000,
    })
  } catch {
    notifications.show({
      color: 'orange',
      title: 'Copy failed',
      message: id,
      autoClose: 2000,
    })
  }
}

export function CopyIdButton({
  id,
  label = 'Id copied',
}: {
  id: string
  label?: string
}) {
  function onCopy(e: MouseEvent) {
    e.preventDefault()
    e.stopPropagation()
    void copyIdToClipboard(id, label)
  }

  return (
    <Tooltip label="Copy id" withArrow>
      <ActionIcon
        size="xs"
        variant="subtle"
        color="gray"
        aria-label="Copy id"
        onClick={onCopy}
      >
        <IconCopy size={12} stroke={1.75} />
      </ActionIcon>
    </Tooltip>
  )
}

export function CopyableId({
  id,
  compact = true,
}: {
  id: string
  compact?: boolean
}) {
  const shown = compact && id.length > 12 ? `${id.slice(0, 8)}…` : id
  return (
    <Tooltip label={id} withArrow>
      <Text
        component="span"
        size="xs"
        c="dimmed"
        ff="monospace"
        style={{ cursor: 'default' }}
      >
        {shown}
      </Text>
    </Tooltip>
  )
}
