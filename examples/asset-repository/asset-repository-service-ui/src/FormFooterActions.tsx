import { Button, Group } from '@mantine/core'
import { Link } from 'react-router-dom'

type FooterButton = {
  label: string
  to?: string
  type?: 'button' | 'submit'
  onClick?: () => void
}

export function FormFooterActions({
  secondary,
  primary,
}: {
  secondary: FooterButton
  primary: FooterButton
}) {
  return (
    <Group justify="flex-end" gap="sm">
      <NavButton {...secondary} variant="default" />
      <NavButton {...primary} />
    </Group>
  )
}

function NavButton({
  label,
  to,
  type = 'button',
  onClick,
  variant,
}: FooterButton & { variant?: 'default' | 'filled' }) {
  if (to) {
    return (
      <Button size="sm" variant={variant} component={Link} to={to}>
        {label}
      </Button>
    )
  }
  return (
    <Button size="sm" variant={variant} type={type} onClick={onClick}>
      {label}
    </Button>
  )
}
