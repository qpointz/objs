import { Badge, Button, Group, Menu, Text } from '@mantine/core'

export type SchemaContextVersionControlProps = {
  versions: string[]
  value: string
  latestVersion: string
  onChange: (version: string) => void
  /** Shown when creating a new version draft that is not persisted yet. */
  draftVersion?: string | null
  disabled?: boolean
}

/**
 * Schema type version dropdown — visual match to {@link GraphContextVersionControl}.
 */
export function SchemaContextVersionControl({
  versions,
  value,
  latestVersion,
  onChange,
  draftVersion,
  disabled,
}: SchemaContextVersionControlProps) {
  const label =
    draftVersion && value === draftVersion ? `${value} (draft)` : value

  return (
    <Menu shadow="md" width={280} position="bottom-start" withinPortal>
      <Menu.Target>
        <Button
          size="compact-xs"
          variant="light"
          disabled={disabled}
          data-tour="schema-context-version"
          style={{ flexShrink: 0 }}
        >
          {label} ▾
        </Button>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>Schema versions</Menu.Label>
        {versions.map((v) => {
          const current = v === value
          const latest = v === latestVersion
          return (
            <Menu.Item key={v} disabled={current} onClick={() => onChange(v)}>
              <Group gap={6} wrap="nowrap" align="center">
                <Text size="xs" ff="monospace" lh={1.3}>
                  {v}
                </Text>
                {latest && (
                  <Badge size="xs" variant="light" color="teal">
                    latest
                  </Badge>
                )}
                {current && (
                  <Badge size="xs" variant="light" color="blue">
                    current
                  </Badge>
                )}
              </Group>
            </Menu.Item>
          )
        })}
        {draftVersion && !versions.includes(draftVersion) && (
          <Menu.Item disabled>
            <Group gap={6} wrap="nowrap" align="center">
              <Text size="xs" ff="monospace" lh={1.3}>
                {draftVersion}
              </Text>
              <Badge size="xs" variant="light" color="violet">
                draft
              </Badge>
              <Badge size="xs" variant="light" color="blue">
                current
              </Badge>
            </Group>
          </Menu.Item>
        )}
      </Menu.Dropdown>
    </Menu>
  )
}
