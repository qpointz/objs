import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { MantineProvider, localStorageColorSchemeManager } from '@mantine/core'
import { Notifications } from '@mantine/notifications'
import '@mantine/core/styles.css'
import '@mantine/notifications/styles.css'
import { App } from './App'
import './styles.css'

const colorSchemeManager = localStorageColorSchemeManager({
  key: 'asset-repository.ui.colorScheme',
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <MantineProvider defaultColorScheme="light" colorSchemeManager={colorSchemeManager}>
      <Notifications position="top-right" />
      <BrowserRouter basename="/app">
        <App />
      </BrowserRouter>
    </MantineProvider>
  </StrictMode>,
)
