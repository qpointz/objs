import { Button, Code } from '@mantine/core'
import { notifications } from '@mantine/notifications'

export function NewUuidButton() {
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
      autoClose: 8000,
    })
  }

  return (
    <Button size="xs" variant="light" onClick={createUuid}>
      New UUID
    </Button>
  )
}
