import { describe, expect, it } from 'vitest'
import { graphShapeError } from './ObjectLinterPage'

describe('Object linter graph shape', () => {
  it('accepts a graph batch envelope', () => {
    expect(graphShapeError({ entities: [], edges: [] })).toBeNull()
  })

  it('requires entity and edge arrays', () => {
    expect(graphShapeError({ edges: [] })).toBe('Graph document must contain an entities array')
    expect(graphShapeError({ entities: [] })).toBe('Graph document must contain an edges array')
  })

  it('rejects non-object documents', () => {
    expect(graphShapeError([])).toBe('Graph document must be an object')
  })
})
