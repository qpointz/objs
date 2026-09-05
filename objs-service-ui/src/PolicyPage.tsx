import { useState } from 'react'
import { Box, Group, SegmentedControl, Stack, Title } from '@mantine/core'
import { GraphContextBar } from './GraphContextBar'
import { PolicyPlayPage } from './PolicyPlayPage'
import { PolicySuitesPage } from './PolicySuitesPage'

type PolicySubnav = 'evaluate' | 'suites'

/** Policy workbench shell: Evaluate | Suites subnav + shared graph context. */
export function PolicyPage() {
  const [subnav, setSubnav] = useState<PolicySubnav>('evaluate')

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group align="center" wrap="nowrap" gap="md" style={{ flexShrink: 0 }}>
        <Title order={3} style={{ flexShrink: 0 }}>
          Policy
        </Title>
        <SegmentedControl
          size="xs"
          value={subnav}
          onChange={(v) => setSubnav(v as PolicySubnav)}
          data={[
            { label: 'Evaluate', value: 'evaluate' },
            { label: 'Suites', value: 'suites' },
          ]}
          data-tour="policy-subnav"
        />
        <Box style={{ flex: 1, minWidth: 0 }}>
          <GraphContextBar />
        </Box>
      </Group>
      {subnav === 'evaluate' ? <PolicyPlayPage hideChrome /> : <PolicySuitesPage />}
    </Stack>
  )
}
