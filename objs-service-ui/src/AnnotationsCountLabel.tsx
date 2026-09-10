import { Text, Tooltip } from '@mantine/core'

/** Compact count label for context bars — hover lists lines (k=v or plain). */
export function AnnotationsCountLabel({
  entries,
  noun = 'Annotations',
  dataTour,
  /** When false, render nothing if count is 0 (Schema tags/attrs). Default true. */
  showWhenEmpty = true,
}: {
  entries: [string, string][]
  noun?: string
  dataTour?: string
  showWhenEmpty?: boolean
}) {
  const count = entries.length
  if (count === 0 && !showWhenEmpty) return null

  const label = (
    <Text
      size="xs"
      c="dimmed"
      fw={count > 0 ? 600 : undefined}
      style={{ whiteSpace: 'nowrap', cursor: count > 0 ? 'default' : undefined }}
      data-tour={dataTour}
    >
      {noun}: {count}
    </Text>
  )

  if (count === 0) return label

  const lines = entries.map(([k, v]) => (v.length > 0 ? `${k}=${v}` : k)).join('\n')
  return (
    <Tooltip
      label={
        <Text size="xs" style={{ whiteSpace: 'pre-wrap' }}>
          {lines}
        </Text>
      }
      multiline
      maw={360}
      withArrow
    >
      {label}
    </Tooltip>
  )
}
