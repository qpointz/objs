import { AppShell, Burger, Group, NavLink, Text, Title } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { NewUuidButton } from './NewUuidButton'

export function AppLayout() {
  const [opened, { toggle }] = useDisclosure()
  const location = useLocation()

  return (
    <AppShell
      header={{ height: 56 }}
      navbar={{ width: 220, breakpoint: 'sm', collapsed: { mobile: !opened } }}
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
          <Group>
            <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" />
            <Title order={4}>Objs</Title>
            <Text size="sm" c="dimmed">
              SBOM workbench
            </Text>
          </Group>
          <NewUuidButton />
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md">
        <NavLink
          component={Link}
          to="/graph"
          label="Graph explorer"
          active={location.pathname.startsWith('/graph')}
        />
        <NavLink
          component={Link}
          to="/schemas"
          label="Schema explorer"
          active={location.pathname.startsWith('/schemas') && !location.pathname.includes('/lint')}
        />
        <NavLink
          component={Link}
          to="/linter"
          label="Schema linter"
          active={location.pathname.includes('/lint') || location.pathname === '/linter'}
        />
        <NavLink
          component={Link}
          to="/object-linter"
          label="Object linter"
          active={location.pathname === '/object-linter'}
        />
      </AppShell.Navbar>

      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  )
}
