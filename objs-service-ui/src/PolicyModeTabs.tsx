import { SegmentedControl } from '@mantine/core'

export type PolicyWorkbenchMode = 'policies' | 'suites' | 'archives'

/** Left-pane Policies | Suites | Archives mode switch (fits narrow side pane). */
export function PolicyModeTabs({
  mode,
  onModeChange,
}: {
  mode: PolicyWorkbenchMode
  onModeChange: (mode: PolicyWorkbenchMode) => void
}) {
  return (
    <SegmentedControl
      fullWidth
      size="xs"
      value={mode}
      onChange={(v) => onModeChange(v as PolicyWorkbenchMode)}
      data={[
        { label: 'Policies', value: 'policies' },
        { label: 'Suites', value: 'suites' },
        { label: 'Archives', value: 'archives' },
      ]}
      data-tour="policy-mode-tabs"
      mb="xs"
    />
  )
}
