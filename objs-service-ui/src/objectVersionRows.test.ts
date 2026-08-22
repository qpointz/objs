import { describe, expect, it } from 'vitest'
import {
  formatVersionIdForList,
  formatVersionRowLabel,
  withLatestInBrowser,
  withLatestInRecent,
  type ObjectVersionRow,
} from './objectVersionRows'

function row(version: number): ObjectVersionRow {
  return {
    id: 'g',
    version,
    createdAt: '2026-01-01T00:00:00Z',
  }
}

describe('formatVersionIdForList', () => {
  it('shouldShowLastSixWithEllipsis', () => {
    expect(formatVersionIdForList(1787333643179)).toBe('...643179')
  })

  it('shouldShowFullWhenShort', () => {
    expect(formatVersionIdForList(99)).toBe('99')
    expect(formatVersionIdForList(123456)).toBe('123456')
  })
})

describe('formatVersionRowLabel', () => {
  it('shouldShowLatestOrShortId', () => {
    expect(formatVersionRowLabel(null)).toBe('LATEST')
    expect(formatVersionRowLabel(1787333643179)).toBe('...643179')
  })
})

describe('withLatestInRecent', () => {
  it('shouldPrependLatestWhenPinned', () => {
    const out = withLatestInRecent([row(3), row(2), row(1)], {
      includeLatest: true,
      id: 'g',
      recentN: 2,
    })
    expect(out.map((r) => r.version)).toEqual([null, 3, 2])
    expect(formatVersionRowLabel(out[0].version)).toBe('LATEST')
  })

  it('shouldOmitLatestWhenAlreadyOnLatest', () => {
    const out = withLatestInRecent([row(3), row(2)], {
      includeLatest: false,
      id: 'g',
      recentN: 5,
    })
    expect(out.map((r) => r.version)).toEqual([3, 2])
  })
})

describe('withLatestInBrowser', () => {
  it('shouldPutLatestFirst', () => {
    const out = withLatestInBrowser([row(9), row(8)], {
      includeLatest: true,
      id: 'g',
      annotations: { a: '1' },
    })
    expect(out[0].version).toBeNull()
    expect(out[0].annotations).toEqual({ a: '1' })
    expect(out.map((r) => r.version)).toEqual([null, 9, 8])
  })
})
