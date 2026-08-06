import { AppShell, ActionIcon, Group, Text, Tooltip, UnstyledButton, useMantineColorScheme } from '@mantine/core'
import {
  IconAffiliate,
  IconMoon,
  IconPencilCode,
  IconRoute,
  IconSchema,
  IconSun,
  IconTournament,
} from '@tabler/icons-react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'

function HeaderNavLink({
  to,
  label,
  icon,
}: {
  to: string
  label: string
  icon: ReactNode
}) {
  const location = useLocation()
  const active =
    location.pathname === to || location.pathname.startsWith(`${to}/`)

  return (
    <UnstyledButton
      component={Link}
      to={to}
      px="md"
      py={7}
      style={{
        borderRadius: 8,
        fontSize: 14,
        fontWeight: active ? 650 : 600,
        letterSpacing: '0.01em',
        background: active ? 'var(--mantine-color-blue-light)' : 'transparent',
        color: active ? 'var(--mantine-color-blue-filled)' : 'var(--mantine-color-text)',
      }}
    >
      <Group gap={8} wrap="nowrap">
        {icon}
        <span>{label}</span>
      </Group>
    </UnstyledButton>
  )
}

function ColorSchemeToggle() {
  const { colorScheme, setColorScheme } = useMantineColorScheme()
  const dark = colorScheme === 'dark'
  return (
    <Tooltip label={dark ? 'Light mode' : 'Dark mode'} withArrow>
      <ActionIcon
        variant="subtle"
        size="sm"
        aria-label={dark ? 'Switch to light mode' : 'Switch to dark mode'}
        onClick={() => setColorScheme(dark ? 'light' : 'dark')}
      >
        {dark ? <IconSun size={16} stroke={1.5} /> : <IconMoon size={16} stroke={1.5} />}
      </ActionIcon>
    </Tooltip>
  )
}

export function AppLayout() {
  return (
    <AppShell
      header={{ height: 56 }}
      padding="md"
      styles={{
        main: {
          height: '100dvh',
          maxHeight: '100dvh',
          minHeight: 0,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
        },
      }}
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group gap={0} wrap="nowrap" style={{ flexShrink: 0 }}>
            <UnstyledButton
              component={Link}
              to="/"
              aria-label="Workbench home"
              style={{ borderRadius: 8 }}
            >
              <Group gap={8} wrap="nowrap" px={4} py={4}>
                <IconTournament
                  size={20}
                  stroke={1.5}
                  color="var(--mantine-color-dimmed)"
                  aria-hidden
                />
                <Text size="xs" c="dimmed" fw={500} tt="uppercase" style={{ letterSpacing: '0.06em' }}>
                  Workbench
                </Text>
              </Group>
            </UnstyledButton>

            <Group
              gap={2}
              wrap="nowrap"
              p={4}
              ml="lg"
              style={{
                borderRadius: 10,
                background: 'var(--mantine-color-default-hover)',
                border: '1px solid var(--mantine-color-default-border)',
              }}
            >
              <HeaderNavLink
                to="/explorer"
                label="Explorer"
                icon={<IconAffiliate size={16} stroke={1.75} />}
              />
              <HeaderNavLink
                to="/composer"
                label="Composer"
                icon={<IconPencilCode size={16} stroke={1.75} />}
              />
              <HeaderNavLink
                to="/query"
                label="Query"
                icon={<IconRoute size={16} stroke={1.75} />}
              />
              <HeaderNavLink
                to="/model"
                label="Schema"
                icon={<IconSchema size={16} stroke={1.75} />}
              />
            </Group>
          </Group>

          <ColorSchemeToggle />
        </Group>
      </AppShell.Header>

      <AppShell.Main>
        <div
          style={{
            flex: 1,
            minHeight: 0,
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
          }}
        >
          <Outlet />
        </div>
      </AppShell.Main>
    </AppShell>
  )
}
