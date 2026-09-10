import { useState } from 'react'
import { Stack } from '@mantine/core'
import { PolicyPlayPage } from './PolicyPlayPage'
import { PolicySuitesPage } from './PolicySuitesPage'
import type { PolicyWorkbenchMode } from './PolicyModeTabs'

/** Policy workbench shell: Policies | Suites mode pages (each owns title + actions + context). */
export function PolicyPage() {
  const [mode, setMode] = useState<PolicyWorkbenchMode>('policies')

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      {mode === 'policies' ? (
        <PolicyPlayPage mode={mode} onModeChange={setMode} />
      ) : (
        <PolicySuitesPage mode={mode} onModeChange={setMode} />
      )}
    </Stack>
  )
}
