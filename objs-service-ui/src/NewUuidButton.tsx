import { Button, Code, type ButtonProps } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { VIEW_ACTION_VARIANT } from './viewActionButtons'

/** Match Composer Visual/Text L2 actions (`compact-xs` + bordered secondary). */
type Props = Pick<ButtonProps, 'size' | 'variant'>

export function NewUuidButton({
  size = 'compact-xs',
  variant = VIEW_ACTION_VARIANT,
}: Props) {
  async function createUuid() {
    const uuid = crypto.randomUUID()
    let copied = false
    try {
      await navigator.clipboard.writeText(uuid)
      copied = true
    } catch {
      // The notification still exposes the generated value for manual copying.
    }

    notifications.show({
      color: copied ? 'teal' : 'blue',
      title: copied ? 'New UUID copied to clipboard' : 'New UUID',
      message: <Code>{uuid}</Code>,
      autoClose: 3000,
    })
  }

  return (
    <Button size={size} variant={variant} onClick={createUuid}>
      New UUID
    </Button>
  )
}
