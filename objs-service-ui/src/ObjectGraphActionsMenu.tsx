import { ActionIcon, Menu } from '@mantine/core'
import { IconDots } from '@tabler/icons-react'

type Props = {
  graphId: string
  onOpenAsContext: (graphId: string) => void
  onOpenInExplorer: (graphId: string) => void
  onEditInComposer: (graphId: string) => void
}

/** Tiny ⋮ menu for live-graph usage rows (preview + Graphs browser). */
export function ObjectGraphActionsMenu({
  graphId,
  onOpenAsContext,
  onOpenInExplorer,
  onEditInComposer,
}: Props) {
  return (
    <Menu shadow="sm" width={180} position="bottom-start" withinPortal>
      <Menu.Target>
        <ActionIcon
          size="xs"
          variant="subtle"
          aria-label="Graph actions"
          data-tour="object-graph-actions"
          onClick={(e) => e.stopPropagation()}
        >
          <IconDots size={12} />
        </ActionIcon>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Item
          data-tour="object-graph-open-context"
          onClick={(e) => {
            e.stopPropagation()
            onOpenAsContext(graphId)
          }}
        >
          Open
        </Menu.Item>
        <Menu.Item
          data-tour="object-graph-open-explorer"
          onClick={(e) => {
            e.stopPropagation()
            onOpenInExplorer(graphId)
          }}
        >
          Open in Explorer
        </Menu.Item>
        <Menu.Item
          data-tour="object-graph-edit-composer"
          onClick={(e) => {
            e.stopPropagation()
            onEditInComposer(graphId)
          }}
        >
          Edit in Composer
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  )
}
