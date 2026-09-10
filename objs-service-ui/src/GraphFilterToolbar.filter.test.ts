import { describe, expect, it } from 'vitest'
import { filterSelectorRows } from './GraphFilterToolbar'

describe('filterSelectorRows', () => {
  const rows = [
    { searchText: 'API API' },
    { searchText: 'artifact artifact' },
    { searchText: 'Boba Acre Boba Acre' },
    { searchText: 'Package Package' },
    { searchText: 'Service Service' },
  ]

  it('should return all rows_when query empty', () => {
    expect(filterSelectorRows(rows, '')).toEqual(rows)
    expect(filterSelectorRows(rows, '   ')).toEqual(rows)
  })

  it('should match case-insensitive substring_when typing A', () => {
    const matched = filterSelectorRows(rows, 'A').map((r) => r.searchText)
    expect(matched).toEqual([
      'API API',
      'artifact artifact',
      'Boba Acre Boba Acre',
      'Package Package',
    ])
  })

  it('should narrow further_when query grows', () => {
    const matched = filterSelectorRows(rows, 'ac').map((r) => r.searchText)
    expect(matched).toEqual(['artifact artifact', 'Boba Acre Boba Acre', 'Package Package'])
  })
})
