import { Tabs } from '@mantine/core'

export type PolicyWorkbenchMode = 'policies' | 'suites'

/** Left-pane Policies | Suites mode switch (Note 1). */
export function PolicyModeTabs({
  mode,
  onModeChange,
}: {
  mode: PolicyWorkbenchMode
  onModeChange: (mode: PolicyWorkbenchMode) => void
}) {
  return (
    <Tabs
      value={mode}
      onChange={(v) => {
        if (v === 'policies' || v === 'suites') onModeChange(v)
      }}
      data-tour="policy-mode-tabs"
      mb="xs"
    >
      <Tabs.List grow>
        <Tabs.Tab value="policies">Policies</Tabs.Tab>
        <Tabs.Tab value="suites">Suites</Tabs.Tab>
      </Tabs.List>
    </Tabs>
  )
}
