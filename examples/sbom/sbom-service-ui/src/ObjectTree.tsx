import { Anchor, Text } from '@mantine/core'
import { Link } from 'react-router-dom'

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function formatLeaf(value: unknown): string {
  if (value === null) return 'null'
  if (value === undefined) return '—'
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  return String(value)
}

export function ObjectTree({
  value,
  name,
  href,
  leafLinks,
}: {
  value: unknown
  name?: string
  href?: string
  leafLinks?: Record<string, string>
}) {
  if (Array.isArray(value)) {
    return (
      <div>
        {name != null && (
          <Text size="xs" fw={600} span>
            {name}
          </Text>
        )}
        {value.length === 0 ? (
          <Text size="xs" c="dimmed" pl={name != null ? 'sm' : 0}>
            []
          </Text>
        ) : (
          <div style={{ paddingLeft: name != null ? 12 : 0 }}>
            {value.map((item, i) => (
              <ObjectTree key={i} name={`[${i}]`} value={item} />
            ))}
          </div>
        )}
      </div>
    )
  }
  if (isPlainObject(value)) {
    const keys = Object.keys(value)
    return (
      <div>
        {name != null && (
          <Text size="xs" fw={600}>
            {name}
          </Text>
        )}
        <div style={{ paddingLeft: name != null ? 12 : 0 }}>
          {keys.length === 0 ? (
            <Text size="xs" c="dimmed">
              {'{}'}
            </Text>
          ) : (
            keys.map((key) => (
              <ObjectTree key={key} name={key} value={value[key]} href={leafLinks?.[key]} />
            ))
          )}
        </div>
      </div>
    )
  }
  return (
    <Text size="xs">
      {name != null && (
        <Text span fw={600}>
          {name}:{' '}
        </Text>
      )}
      {href ? (
        <Anchor component={Link} to={href} size="xs">
          {formatLeaf(value)}
        </Anchor>
      ) : (
        <Text span c="dimmed">
          {formatLeaf(value)}
        </Text>
      )}
    </Text>
  )
}
