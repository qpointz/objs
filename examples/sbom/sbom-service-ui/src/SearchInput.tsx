import { ActionIcon, TextInput, type TextInputProps } from '@mantine/core'
import { IconSearch, IconX } from '@tabler/icons-react'
import type { KeyboardEvent } from 'react'

export function SearchInput({
  value,
  onValueChange,
  showSearchIcon = true,
  ...props
}: Omit<TextInputProps, 'value' | 'onChange' | 'rightSection'> & {
  value: string
  onValueChange: (value: string) => void
  showSearchIcon?: boolean
}) {
  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Escape' && value) {
      e.preventDefault()
      e.stopPropagation()
      onValueChange('')
    }
    props.onKeyDown?.(e)
  }

  return (
    <TextInput
      {...props}
      value={value}
      onChange={(e) => onValueChange(e.currentTarget.value)}
      onKeyDown={onKeyDown}
      leftSection={props.leftSection ?? (showSearchIcon ? <IconSearch size={14} /> : undefined)}
      rightSection={
        value ? (
          <ActionIcon size="sm" variant="subtle" aria-label="Clear search" onClick={() => onValueChange('')}>
            <IconX size={12} />
          </ActionIcon>
        ) : undefined
      }
    />
  )
}
