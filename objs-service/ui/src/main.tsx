import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { MantineProvider, localStorageColorSchemeManager } from '@mantine/core'
import { Notifications } from '@mantine/notifications'
import '@mantine/core/styles.css'
import '@mantine/notifications/styles.css'
import App from './App'

const colorSchemeManager = localStorageColorSchemeManager({
  key: 'objs.ui.colorScheme',
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <MantineProvider defaultColorScheme="light" colorSchemeManager={colorSchemeManager}>
      <Notifications position="top-right" />
      <App />
    </MantineProvider>
  </StrictMode>,
)
