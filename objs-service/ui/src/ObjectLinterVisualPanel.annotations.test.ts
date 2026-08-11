import { describe, expect, it } from 'vitest'
import { mergeAnnotations, versionsForEntityType } from './ObjectLinterVisualPanel'
import type { BoMSchema } from './types'

describe('mergeAnnotations', () => {
  it('shouldOverlayWinOnCollision_whenPasteMerge', () => {
    expect(mergeAnnotations({ a: '1', b: '2' }, { b: 'x', c: '3' })).toEqual({
      a: '1',
      b: 'x',
      c: '3',
    })
  })

  it('shouldKeepBaseOnly_whenOverlayEmpty', () => {
    expect(mergeAnnotations({ a: '1' }, {})).toEqual({ a: '1' })
  })

  it('shouldUseOverlayOnly_whenBaseEmpty', () => {
    expect(mergeAnnotations({}, { a: '1' })).toEqual({ a: '1' })
  })
})

describe('versionsForEntityType', () => {
  const emptySchema = { type: 'OBJECT' as const, title: '', description: '', fields: [] }

  it('shouldReturnSortedVersionsDesc_forEntityType', () => {
    const schemas: BoMSchema[] = [
      {
        type: 'Product',
        version: '1.0.0',
        usage: 'ENTITY',
        contentSchema: emptySchema,
      },
      {
        type: 'Product',
        version: '2.0.0',
        usage: 'ENTITY',
        contentSchema: emptySchema,
      },
      {
        type: 'Service',
        version: '1.0.0',
        usage: 'ENTITY',
        contentSchema: emptySchema,
      },
      {
        type: 'Product',
        version: '1.5.0',
        usage: 'EDGE_PROPERTIES',
        contentSchema: emptySchema,
      },
    ]
    expect(versionsForEntityType(schemas, 'Product')).toEqual(['2.0.0', '1.0.0'])
    expect(versionsForEntityType(schemas, null)).toEqual([])
  })
})
