import { describe, expect, it } from 'vitest'
import { formatInstanceVersionLabel, objectDisplayTitle } from './objectViewerTitle'

describe('objectDisplayTitle', () => {
  it('shouldPreferDisplayName', () => {
    expect(objectDisplayTitle('API Gateway', 'Product', '77028bb3-56db-4944-b763-73945b6b98ca')).toBe(
      'API Gateway',
    )
  })

  it('shouldFallbackToTypeAndShortId', () => {
    expect(objectDisplayTitle(null, 'Product', 'ea85d111-2222-4333-8444-555555555555')).toBe(
      'product-ea85d',
    )
    expect(objectDisplayTitle('  ', 'Component', 'abc')).toBe('component-abc')
  })
})

describe('formatInstanceVersionLabel', () => {
  it('shouldShowLatestOrNumber', () => {
    expect(formatInstanceVersionLabel(null)).toBe('LATEST')
    expect(formatInstanceVersionLabel(undefined)).toBe('LATEST')
    expect(formatInstanceVersionLabel(99)).toBe('99')
  })
})
