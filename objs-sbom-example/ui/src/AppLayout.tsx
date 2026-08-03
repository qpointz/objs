import { AppShell, Group, Text, Title, UnstyledButton } from '@mantine/core'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { NewUuidButton } from './NewUuidButton'

function HeaderNavLink({ to, label }: { to: string; label: string }) {
  const location = useLocation()
  const active =
    to === '/schemas'
      ? location.pathname.startsWith('/schemas')
      : location.pathname === to || location.pathname.startsWith(`${to}/`)

  return (
    <UnstyledButton
      component={Link}
      to={to}
      px="sm"
      py={6}
      style={{
        borderRadius: 6,
        fontWeight: active ? 700 : 500,
        background: active ? 'var(--mantine-color-blue-light)' : 'transparent',
        color: active ? 'var(--mantine-color-blue-filled)' : 'inherit',
      }}
    >
      {label}
    </UnstyledButton>
  )
}

export function AppLayout() {
  return (
    <AppShell
      header={{ height: 56 }}
      padding="md"
      styles={{
        main: {
          height: '100vh',
          display: 'flex',
          flexDirection: 'column',
          minHeight: 0,
        },
      }}
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Group gap="lg">
            <Group gap="xs">
              <Title order={4}>Objs</Title>
              <Text size="sm" c="dimmed">
                SBOM workbench
              </Text>
            </Group>
            <Group gap={4}>
              <HeaderNavLink to="/graph" label="Graph explorer" />
              <HeaderNavLink to="/schemas" label="Schemas" />
              <HeaderNavLink to="/object-linter" label="Object linter" />
            </Group>
          </Group>
          <NewUuidButton />
        </Group>
      </AppShell.Header>

      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  )
}
