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

  it('loads recent graphs when search is empty', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [
        {
          id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
          annotations: { env: 'dev' },
          entityCount: 1,
          edgeCount: 0,
          updatedAt: '2026-09-02T10:00:00Z',
        },
        {
          id: 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
          annotations: { env: 'prod' },
          entityCount: 2,
          edgeCount: 1,
          updatedAt: '2026-09-01T10:00:00Z',
        },
      ],
    })
    vi.stubGlobal('fetch', fetchMock)

    renderModal()

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith('/api/v1/objs/graphs')
    })

    expect(await screen.findByText(/bbbbbbbb/)).toBeTruthy()
    expect(screen.queryByText(/Type to search/i)).toBeNull()
  })

  it('debounced search calls /graphs/search and opens on row click', async () => {
    const user = userEvent.setup()
    const onOpen = vi.fn()
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [],
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          items: [{ id: 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee', annotations: { env: 'prod' } }],
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          id: 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
          annotations: { env: 'prod' },
          graph: { entities: [], edges: [] },
        }),
      })

    vi.stubGlobal('fetch', fetchMock)

    render(
      createElement(
        MantineProvider,
        null,
        createElement(OpenGraphModal, {
          opened: true,
          onClose: () => {},
          onOpen,
        }),
      ),
    )

    await user.type(screen.getByPlaceholderText(/prod, a1b2c3d4/i), 'prod')

    await waitFor(
      () => {
        const searchCall = fetchMock.mock.calls.find((c) =>
          String(c[0]).includes('/api/v1/objs/graphs/search?'),
        )
        expect(searchCall).toBeTruthy()
        expect(String(searchCall![0])).toContain('q=prod')
      },
      { timeout: 2000 },
    )

    const row = await screen.findByRole('button', { name: /aaaaaaaa/i })
    await user.click(row)

    await waitFor(() => {
      expect(onOpen).toHaveBeenCalled()
    })
  })
})
