import { useState } from 'react'
import {
  ActionIcon,
  Badge,
  Group,
  Paper,
  Text,
  Tooltip,
} from '@mantine/core'
import { IconCheck, IconCopy, IconSchema } from '@tabler/icons-react'
import { notifications } from '@mantine/notifications'
import { SchemaContextVersionControl } from './SchemaContextVersionControl'

function KindPill({ kind }: { kind: 'object' | 'edge' }) {
  return (
    <Badge size="sm" variant="filled" color={kind === 'object' ? 'blue' : 'grape'} radius="sm">
      {kind === 'object' ? 'O' : 'E'}
    </Badge>
  )
}

function AttributePill({ name, value }: { name: string; value: string }) {
  return (
    <Group gap={0} wrap="nowrap" title={`${name}: ${value}`}>
      <Badge
        size="sm"
        variant="filled"
        color="gray"
        radius="xl"
        tt="none"
        style={{
          borderTopRightRadius: 0,
          borderBottomRightRadius: 0,
          paddingLeft: 8,
          paddingRight: 8,
        }}
      >
        {name}
      </Badge>
      <Badge
        size="sm"
        variant="light"
        color="gray"
        radius="xl"
        tt="none"
        style={{
          borderTopLeftRadius: 0,
          borderBottomLeftRadius: 0,
          paddingLeft: 8,
          paddingRight: 8,
        }}
      >
        {value || '—'}
      </Badge>
    </Group>
  )
}

export type SchemaContextBarMode = 'catalog' | 'detail' | 'new-draft'

export type SchemaContextBarProps = {
  mode: SchemaContextBarMode
  typeName?: string
  version?: string
  kind?: 'object' | 'edge'
  tags?: string[]
  attributes?: { key: string; value: string }[]
  typeCount?: number
  edgeRuleCount?: number
  unsaved?: boolean
  createVersionDraft?: boolean
  typeVersions?: string[]
  latestTypeVersion?: string
  onVersionChange?: (version: string) => void
}

/**
 * Schema view context chrome (Note 9). Visual match to graph bars only —
 * never uses shared graph context / GraphContextProvider.
 */
export function SchemaContextBar({
  mode,
  typeName,
  version,
  kind,
  tags = [],
  attributes = [],
  typeCount = 0,
  edgeRuleCount = 0,
  unsaved,
  createVersionDraft,
  typeVersions = [],
  latestTypeVersion,
  onVersionChange,
}: SchemaContextBarProps) {
  const filledAttributes = attributes.filter((row) => row.key.trim().length > 0)
  const copyValue =
    mode === 'detail' && typeName && version ? `${typeName}@${version}` : typeName ?? ''

  return (
    <Paper withBorder px="sm" py={6} radius="md" data-tour="schema-context-bar">
      <Group gap="sm" wrap="nowrap" justify="space-between" align="center">
        <Group gap="xs" wrap="nowrap" style={{ flex: 1, minWidth: 0 }} align="center">
          <Tooltip label="Schema" withArrow>
            <IconSchema
              size={18}
              stroke={1.5}
              color="var(--mantine-color-blue-filled)"
              aria-label="Schema"
            />
          </Tooltip>

          {mode === 'catalog' && (
            <Text size="sm" fw={600} style={{ flexShrink: 0 }}>
              Schema catalog
            </Text>
          )}

          {mode === 'new-draft' && (
            <Group gap={6} wrap="nowrap" style={{ minWidth: 0 }}>
              <Text size="sm" fw={600} style={{ flexShrink: 0 }}>
                New schema
              </Text>
              {typeName && (
                <Text size="sm" c="dimmed" truncate>
                  {typeName}@{version ?? '1.0.0'}
                </Text>
              )}
            </Group>
          )}

          {mode === 'detail' && typeName && (
            <>
              <Tooltip label={typeName} withArrow>
                <Text size="sm" fw={600} style={{ flexShrink: 0 }}>
                  {typeName}
                </Text>
              </Tooltip>
              {copyValue && (
                <CopyButton
                  ariaLabel="Copy type@version"
                  onCopy={() => void copyText('Type@version', copyValue)}
                />
              )}
              {kind && <KindPill kind={kind} />}
              {(tags.length > 0 || filledAttributes.length > 0) && (
                <Group gap={6} wrap="nowrap" style={{ minWidth: 0, overflow: 'hidden' }}>
                  {tags.map((tag) => (
                    <Badge key={tag} size="sm" variant="light" radius="xl" tt="none">
                      {tag}
                    </Badge>
                  ))}
                  {filledAttributes.map((row) => (
                    <AttributePill
                      key={row.key}
                      name={row.key.trim()}
                      value={row.value}
                    />
                  ))}
                </Group>
              )}
              {version && onVersionChange && (
                <>
                  <Text size="xs" c="dimmed" fw={700} style={{ flexShrink: 0, opacity: 0.55 }}>
                    |
                  </Text>
                  <Text size="xs" c="dimmed" fw={600} style={{ flexShrink: 0 }}>
                    Version:
                  </Text>
                  <SchemaContextVersionControl
                    versions={typeVersions}
                    value={version}
                    latestVersion={latestTypeVersion ?? version}
                    draftVersion={createVersionDraft ? version : null}
                    onChange={onVersionChange}
                    disabled={createVersionDraft}
                  />
                </>
              )}
              {unsaved && (
                <Text size="xs" c="dimmed" fs="italic" style={{ flexShrink: 0 }}>
                  unsaved
                </Text>
              )}
            </>
          )}
        </Group>

        {mode === 'catalog' && (
          <Text size="xs" c="dimmed" style={{ whiteSpace: 'nowrap', flexShrink: 0 }}>
            {typeCount} types / {edgeRuleCount} edge rules
          </Text>
        )}
      </Group>
    </Paper>
  )
}

async function copyText(label: string, value: string) {
  try {
    await navigator.clipboard.writeText(value)
    notifications.show({ message: `${label} copied`, color: 'green', autoClose: 1500 })
  } catch {
    notifications.show({ message: `Could not copy ${label}`, color: 'red' })
  }
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
