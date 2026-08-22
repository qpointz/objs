import { describe, expect, it } from 'vitest'
import { truncateQueryId } from './QueryStructColumns'

describe('truncateQueryId', () => {
  it('keeps short ids', () => {
    expect(truncateQueryId('abc')).toBe('abc')
    expect(truncateQueryId('12345678')).toBe('12345678')
  })

  it('truncates to 8 chars plus ellipsis', () => {
    expect(truncateQueryId('1234567890abcdef')).toBe('12345678...')
  })
})
