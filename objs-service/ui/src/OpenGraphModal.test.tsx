import { createElement } from 'react'
import { MantineProvider } from '@mantine/core'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { OpenGraphModal } from './OpenGraphModal'

beforeAll(() => {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    }),
  })
})

function renderModal(opened = true) {
  return render(
    createElement(
      MantineProvider,
      null,
      createElement(OpenGraphModal, {
        opened,
        onClose: () => {},
        onOpen: () => {},
      }),
    ),
  )
}

describe('OpenGraphModal (WI-007 smoke)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('shows type-to-search and does not fetch until there is a query', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    renderModal()

    expect(screen.getByText(/Type to search/i)).toBeTruthy()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('debounced search calls /graphs/search and lists hits', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        items: [{ id: 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee', annotations: { env: 'prod' } }],
      }),
    })
    vi.stubGlobal('fetch', fetchMock)

    renderModal()
    await user.type(screen.getByLabelText('Search'), 'prod')

    await waitFor(
      () => {
        expect(fetchMock).toHaveBeenCalled()
        const url = String(fetchMock.mock.calls[0][0])
        expect(url).toContain('/api/v1/objs/graphs/search?')
        expect(url).toContain('q=prod')
        expect(url).not.toBe('/api/v1/objs/graphs')
      },
      { timeout: 2000 },
    )

    expect(await screen.findByText('aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Open' })).toBeTruthy()
  })
})
