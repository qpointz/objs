import { ActionIcon, AppShell, Group, Text, Tooltip, UnstyledButton, useMantineColorScheme } from '@mantine/core'
import { IconApps, IconBriefcase, IconCategory2, IconExternalLink, IconMoon, IconPackage, IconSchema, IconSun } from '@tabler/icons-react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { useEffect, useState, type ReactNode } from 'react'

function HeaderNavLink({
  to,
  label,
  icon,
  external,
  active: activeOverride,
}: {
  to: string
  label: string
  icon?: ReactNode
  external?: boolean
  active?: boolean
}) {
  const location = useLocation()
  const active = activeOverride ?? (!external && (location.pathname === to || location.pathname.startsWith(`${to}/`)))
  const style = {
    borderRadius: 8,
    fontSize: 14,
    fontWeight: active ? 650 : 600,
    letterSpacing: '0.01em',
    background: active ? 'var(--mantine-color-blue-light)' : 'transparent',
    color: active ? 'var(--mantine-color-blue-filled)' : 'var(--mantine-color-text)',
  } as const

  if (external) {
    return (
      <UnstyledButton
        component="a"
        href={to}
        target="_blank"
        rel="noopener noreferrer"
        px="md"
        py={7}
        style={style}
      >
        <Group gap={8} wrap="nowrap">
          {icon}
          <span>{label}</span>
          <IconExternalLink size={14} stroke={1.75} aria-hidden />
        </Group>
      </UnstyledButton>
    )
  }

  return (
    <UnstyledButton component={Link} to={to} px="md" py={7} style={style}>
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

async function probeWorkbench(): Promise<boolean> {
  for (const path of ['/workbench/', '/workbench']) {
    try {
      const res = await fetch(path, { method: 'GET', cache: 'no-store', redirect: 'follow' })
      if (res.status !== 404) {
        return res.ok
      }
    } catch {
      // try next path
    }
  }
  return false
}

function useWorkbenchAvailable() {
  const [available, setAvailable] = useState(false)
  useEffect(() => {
    let cancelled = false
    void probeWorkbench().then((up) => {
      if (!cancelled) setAvailable(up)
    })
    return () => {
      cancelled = true
    }
  }, [])
  return available
}

export function AppLayout() {
  const location = useLocation()
  const workbenchUp = useWorkbenchAvailable()
  const onSchemas = location.pathname.startsWith('/schemas')
  const onApplications = location.pathname.startsWith('/applications')
  const onAssets = location.pathname.startsWith('/applications/assets')
  const onPortfolios = location.pathname.startsWith('/portfolios')

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
            <UnstyledButton component={Link} to="/applications" aria-label="SBOM inventory home" style={{ borderRadius: 8 }}>
              <Group gap={8} wrap="nowrap" px={4} py={4}>
                <IconPackage size={20} stroke={1.5} color="var(--mantine-color-dimmed)" aria-hidden />
                <Text size="xs" c="dimmed" fw={500} tt="uppercase" style={{ letterSpacing: '0.06em' }}>
                  SBOM inventory
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
                to="/applications/assets"
                label="Assets"
                icon={<IconCategory2 size={16} stroke={1.75} />}
                active={onAssets}
              />
              <HeaderNavLink
                to="/applications"
                label="Applications"
                icon={<IconApps size={16} stroke={1.75} />}
                active={onApplications && !onAssets}
              />
              <HeaderNavLink
                to="/portfolios"
                label="Portfolios"
                icon={<IconBriefcase size={16} stroke={1.75} />}
                active={onPortfolios}
              />
              <HeaderNavLink
                to="/schemas"
                label="Schemas"
                icon={<IconSchema size={16} stroke={1.75} />}
                active={onSchemas}
              />
            </Group>
          </Group>
          <Group gap="xs" wrap="nowrap">
            {workbenchUp && <HeaderNavLink to="/workbench/" label="Workbench" external />}
            <ColorSchemeToggle />
          </Group>
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
