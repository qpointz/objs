import { createElement, type ReactNode } from 'react'
import { act, renderHook } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import type { GraphNode } from './types'
import { useGraphSelectionHistory } from './useGraphSelectionHistory'

function node(id: string, annotations: Record<string, string> = {}): GraphNode {
  return {
    id,
    name: id,
    type: 'Product',
    schemaVersion: '1.0.0',
    color: '#000',
    payload: {},
    annotations,
  }
}

function wrapper({ children }: { children: ReactNode }) {
  return createElement(MemoryRouter, { initialEntries: ['/composer'] }, children)
}

describe('useGraphSelectionHistory', () => {
  it('keeps local selection when graph nodes change without a query id', () => {
    const initial = [node('a')]
    const { result, rerender } = renderHook(
      ({ nodes }) =>
        useGraphSelectionHistory({
          nodes,
          links: [],
        }),
      {
        wrapper,
        initialProps: { nodes: initial },
      },
    )

    act(() => {
      result.current.select({ kind: 'node', node: initial[0] })
    })
    expect(result.current.selection?.kind).toBe('node')
    if (result.current.selection?.kind === 'node') {
      expect(result.current.selection.node.id).toBe('a')
    }

    // Simulate annotation/payload upsert: new nodes array identity, same ids.
    rerender({ nodes: [node('a', { app: 'x' })] })

    expect(result.current.selection?.kind).toBe('node')
    if (result.current.selection?.kind === 'node') {
      expect(result.current.selection.node.id).toBe('a')
    }
  })
})
