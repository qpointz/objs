import { useState } from 'react'
import { Stack } from '@mantine/core'
import { PolicyArchivesPage } from './PolicyArchivesPage'
import { PolicyPlayPage } from './PolicyPlayPage'
import { PolicySuitesPage } from './PolicySuitesPage'
import type { PolicyWorkbenchMode } from './PolicyModeTabs'

/** Policy workbench shell: Policies | Suites | Archives mode pages. */
export function PolicyPage() {
  const [mode, setMode] = useState<PolicyWorkbenchMode>('policies')
  const [openArchiveId, setOpenArchiveId] = useState<string | null>(null)

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      {mode === 'policies' ? (
        <PolicyPlayPage mode={mode} onModeChange={setMode} />
      ) : mode === 'suites' ? (
        <PolicySuitesPage
          mode={mode}
          onModeChange={setMode}
          onOpenArchive={(id) => {
            setOpenArchiveId(id)
            setMode('archives')
          }}
        />
      ) : (
        <PolicyArchivesPage
          mode={mode}
          onModeChange={setMode}
          initialArchiveId={openArchiveId}
          onInitialArchiveConsumed={() => setOpenArchiveId(null)}
        />
      )}
    </Stack>
  )
}
