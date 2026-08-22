import type { ReactNode } from 'react'
import { Divider } from '@mantine/core'

/** SBOM-style section divider — darker / larger than field keys (Note 5 polish). */
export function ObjectViewerSection({
  label,
  children,
}: {
  label: string
  children: ReactNode
}) {
  return (
    <div>
      <Divider
        my={10}
        mx={0}
        label={label}
        labelPosition="left"
        styles={{
          label: {
            fontSize: 13,
            fontWeight: 700,
            color: 'var(--mantine-color-dark-3)',
            marginRight: 10,
            textTransform: 'none',
            letterSpacing: 0.01,
          },
          root: {
            borderColor: 'var(--mantine-color-gray-4)',
          },
        }}
      />
      <div>{children}</div>
    </div>
  )
}
