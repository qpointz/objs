import { afterEach, describe, expect, it } from 'vitest'
import {
  OBJECT_SHELF_STORAGE_KEY,
  shelfToComposerNavState,
  useObjectShelf,
} from './useObjectShelf'
import type { BoMEntity } from './types'
import { act, renderHook } from '@testing-library/react'

function entity(id: string, type = 'Component'): BoMEntity {
  return { id, type, schemaVersion: '1', payload: {}, annotations: {} }
}

describe('useObjectShelf', () => {
  afterEach(() => {
    window.localStorage.removeItem(OBJECT_SHELF_STORAGE_KEY)
  })

  it('adds uniquely by id and persists', () => {
    const { result } = renderHook(() => useObjectShelf())
    act(() => {
      result.current.add([entity('a'), entity('a'), entity('b')])
    })
    expect(result.current.entities.map((e) => e.id)).toEqual(['a', 'b'])
    expect(JSON.parse(window.localStorage.getItem(OBJECT_SHELF_STORAGE_KEY)!)).toHaveLength(2)
  })

  it('toggles and clears', () => {
    const { result } = renderHook(() => useObjectShelf())
    act(() => {
      result.current.toggle(entity('x'))
    })
    expect(result.current.ids.has('x')).toBe(true)
    act(() => {
      result.current.toggle(entity('x'))
    })
    expect(result.current.ids.has('x')).toBe(false)
    act(() => {
      result.current.add([entity('y')])
      result.current.clear()
    })
    expect(result.current.entities).toEqual([])
  })
})

describe('shelfToComposerNavState', () => {
  it('builds replaceDraft handoff with empty edges', () => {
    const entities = [entity('1'), entity('2')]
    expect(shelfToComposerNavState(entities)).toEqual({
      graphId: null,
      replaceDraft: true,
      graphContents: { entities, edges: [] },
    })
  })
})
