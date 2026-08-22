import { describe, expect, it } from 'vitest'
import { clamp, maxSidePaneWidth } from './sidePaneSplit'

describe('maxSidePaneWidth', () => {
  it('shouldCapAtHalfHost', () => {
    expect(maxSidePaneWidth(1000, 240)).toBe(500)
  })

  it('shouldNotGoBelowMin', () => {
    expect(maxSidePaneWidth(400, 240)).toBe(240)
  })

  it('shouldFallbackWhenHostInvalid', () => {
    expect(maxSidePaneWidth(0, 240)).toBe(240)
    expect(maxSidePaneWidth(Number.NaN, 240)).toBe(240)
  })
})

describe('clamp', () => {
  it('shouldClamp', () => {
    expect(clamp(10, 0, 5)).toBe(5)
    expect(clamp(-1, 0, 5)).toBe(0)
    expect(clamp(3, 0, 5)).toBe(3)
  })
})
