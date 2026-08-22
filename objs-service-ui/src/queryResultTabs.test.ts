import { describe, expect, it } from 'vitest'
import { resolveQueryResultTab } from './queryResultTabs'

describe('resolveQueryResultTab', () => {
  it('keeps current when that tab has content', () => {
    expect(
      resolveQueryResultTab('structured', {
        hasGraph: true,
        hasStructured: true,
        hasRaw: true,
      }),
    ).toBe('structured')
    expect(
      resolveQueryResultTab('raw', {
        hasGraph: true,
        hasStructured: false,
        hasRaw: true,
      }),
    ).toBe('raw')
  })

  it('switches to first non-empty when current is empty', () => {
    expect(
      resolveQueryResultTab('structured', {
        hasGraph: true,
        hasStructured: false,
        hasRaw: true,
      }),
    ).toBe('graph')
    expect(
      resolveQueryResultTab('graph', {
        hasGraph: false,
        hasStructured: true,
        hasRaw: true,
      }),
    ).toBe('structured')
    expect(
      resolveQueryResultTab('structured', {
        hasGraph: false,
        hasStructured: false,
        hasRaw: true,
      }),
    ).toBe('raw')
  })

  it('falls back to graph when nothing has content', () => {
    expect(
      resolveQueryResultTab('structured', {
        hasGraph: false,
        hasStructured: false,
        hasRaw: false,
      }),
    ).toBe('structured')
  })
})
