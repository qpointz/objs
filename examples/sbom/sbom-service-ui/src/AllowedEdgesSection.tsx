import { Anchor, Stack, Table, Text, Title } from '@mantine/core'
import { Link } from 'react-router-dom'
import type { AllowedEdgeRuleView } from './api/types'

const COLS = {
  source: '16%',
  role: '18%',
  target: '16%',
  properties: '16%',
} as const

function TypeCell({
  type,
  currentType,
  knownTypes,
}: {
  type: string
  currentType: string
  knownTypes: Set<string>
}) {
  if (type === '*' || type === currentType || !knownTypes.has(type)) {
    return <Text size="sm">{type}</Text>
  }
  return (
    <Anchor component={Link} to={`/schemas/${encodeURIComponent(type)}`} size="sm">
      {type}
    </Anchor>
  )
}

function PropertiesCell({
  rule,
  knownTypes,
}: {
  rule: AllowedEdgeRuleView
  knownTypes: Set<string>
}) {
  const schemaType = rule.propertiesSchemaType?.trim()
  const schemaVersion = rule.propertiesSchemaVersion?.trim()
  if (!schemaType) return null
  const label = schemaVersion ? `${schemaType}@${schemaVersion}` : schemaType
  if (!knownTypes.has(schemaType)) {
    return <Text size="sm">{label}</Text>
  }
  const to = schemaVersion
    ? `/schemas/${encodeURIComponent(schemaType)}/${encodeURIComponent(schemaVersion)}`
    : `/schemas/${encodeURIComponent(schemaType)}`
  return (
    <Anchor component={Link} to={to} size="sm">
      {label}
    </Anchor>
  )
}

function EdgeTable({
  rules,
  currentType,
  knownTypes,
}: {
  rules: AllowedEdgeRuleView[]
  currentType: string
  knownTypes: Set<string>
}) {
  return (
    <Table striped withTableBorder style={{ tableLayout: 'fixed', width: '100%' }}>
      <colgroup>
        <col style={{ width: COLS.source }} />
        <col style={{ width: COLS.role }} />
        <col style={{ width: COLS.target }} />
        <col style={{ width: COLS.properties }} />
        <col />
      </colgroup>
      <Table.Thead>
        <Table.Tr>
          <Table.Th>Source</Table.Th>
          <Table.Th>Role</Table.Th>
          <Table.Th>Target</Table.Th>
          <Table.Th>Properties</Table.Th>
          <Table.Th>Description</Table.Th>
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {rules.length === 0 ? (
          <Table.Tr>
            <Table.Td colSpan={5}>
              <Text size="sm" c="dimmed">
                None
              </Text>
            </Table.Td>
          </Table.Tr>
        ) : (
          rules.map((rule) => {
            const cardinality =
              rule.cardinality && rule.cardinality !== 'UNSPECIFIED' ? rule.cardinality : null
            const verbs = [rule.sourceVerb, rule.targetVerb].filter(Boolean).join(' / ')
            return (
              <Table.Tr key={`${rule.sourceType}|${rule.role}|${rule.targetType}`}>
                <Table.Td>
                  <TypeCell type={rule.sourceType} currentType={currentType} knownTypes={knownTypes} />
                </Table.Td>
                <Table.Td>
                  <Text size="sm">
                    {rule.role}
                    {cardinality ? (
                      <Text span size="sm" fw={700}>
                        {' '}
                        {cardinality}
                      </Text>
                    ) : null}
                  </Text>
                  {verbs ? (
                    <Text size="xs" c="dimmed">
                      {verbs}
                    </Text>
                  ) : null}
                </Table.Td>
                <Table.Td>
                  <TypeCell type={rule.targetType} currentType={currentType} knownTypes={knownTypes} />
                </Table.Td>
                <Table.Td>
                  <PropertiesCell rule={rule} knownTypes={knownTypes} />
                </Table.Td>
                <Table.Td>
                  <Text size="sm" c={rule.description ? undefined : 'dimmed'}>
                    {rule.description || ''}
                  </Text>
                </Table.Td>
              </Table.Tr>
            )
          })
        )}
      </Table.Tbody>
    </Table>
  )
}

export function AllowedEdgesSection({
  incoming,
  outgoing,
  currentType,
  knownTypes,
}: {
  incoming: AllowedEdgeRuleView[]
  outgoing: AllowedEdgeRuleView[]
  currentType: string
  knownTypes: Set<string>
}) {
  return (
    <Stack gap="sm" mt="md">
      <Title order={4}>Relations</Title>
      {incoming.length === 0 && outgoing.length === 0 ? (
        <Text size="sm" c="dimmed">
          No allowed edges for this type.
        </Text>
      ) : (
        <>
          <div>
            <Text size="sm" fw={600} mb="xs">
              Incoming
            </Text>
            <EdgeTable rules={incoming} currentType={currentType} knownTypes={knownTypes} />
          </div>
          <div>
            <Text size="sm" fw={600} mb="xs">
              Outgoing
            </Text>
            <EdgeTable rules={outgoing} currentType={currentType} knownTypes={knownTypes} />
          </div>
        </>
      )}
    </Stack>
  )
}
