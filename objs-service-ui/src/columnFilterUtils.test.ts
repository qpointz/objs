import { describe, expect, it } from 'vitest'
import { passesColumnFilter, toggleInSet, uniqueSortedOptions } from './columnFilterUtils'

describe('columnFilterUtils', () => {
  it('shouldPassAll_whenFilterEmpty', () => {
    expect(passesColumnFilter('A', new Set())).toBe(true)
    expect(passesColumnFilter(null, new Set())).toBe(true)
  })

  it('shouldRejectBlank_whenFilterActive', () => {
    expect(passesColumnFilter('—', new Set(['A']))).toBe(false)
    expect(passesColumnFilter('', new Set(['A']))).toBe(false)
    expect(passesColumnFilter(null, new Set(['A']))).toBe(false)
  })

  it('shouldMatchSelectedValues', () => {
    expect(passesColumnFilter('A', new Set(['A', 'B']))).toBe(true)
    expect(passesColumnFilter('C', new Set(['A', 'B']))).toBe(false)
  })

  it('shouldBuildUniqueSortedOptions', () => {
    expect(uniqueSortedOptions(['B', 'A', 'B', '—', null, ''])).toEqual([
      { value: 'A', label: 'A' },
      { value: 'B', label: 'B' },
    ])
  })

  it('shouldToggleInSet', () => {
    expect([...toggleInSet(new Set(['A']), 'B')].sort()).toEqual(['A', 'B'])
    expect([...toggleInSet(new Set(['A', 'B']), 'A')]).toEqual(['B'])
  })
})
