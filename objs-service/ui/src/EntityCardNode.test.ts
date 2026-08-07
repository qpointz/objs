import { describe, expect, it } from 'vitest'
import {
  flattenPayloadRows,
  formatPayloadCell,
  isScalarList,
} from './EntityCardNode'
import { payloadFieldKindsFromSchema } from './payloadFieldKinds'
import type { BoMSchemaNode } from './types'

describe('formatPayloadCell', () => {
  it('shouldSummarizeObjects', () => {
    expect(formatPayloadCell({ a: 1, b: 2 })).toBe('{2}')
    expect(formatPayloadCell(true)).toBe('true')
    expect(formatPayloadCell(null)).toBe('—')
  })

  it('shouldTruncateLongStrings', () => {
    const long = 'abcdefghijklmnopqrstuvwxyz0123456789'
    expect(formatPayloadCell(long, 10)).toBe('abcdefghi…')
  })
})

describe('isScalarList', () => {
  it('shouldAcceptPrimitiveArrays', () => {
    expect(isScalarList(['a', 'b'])).toBe(true)
    expect(isScalarList([1, true, null])).toBe(true)
    expect(isScalarList([{ x: 1 }])).toBe(false)
  })
})

describe('flattenPayloadRows', () => {
  it('shouldAttachFieldKindsAndOverflow', () => {
    const payload = Object.fromEntries(Array.from({ length: 10 }, (_, i) => [`k${i}`, i]))
    const rows = flattenPayloadRows(payload, { k0: 'ENUM' }, 3)
    expect(rows).toHaveLength(4)
    expect(rows[0]).toMatchObject({ key: 'k0', value: 0, kind: 'ENUM' })
    expect(rows[3]).toMatchObject({ key: '…', overflow: 'more' })
  })
})

describe('payloadFieldKindsFromSchema', () => {
  it('shouldMapEnumAndStringFields', () => {
    const schema: BoMSchemaNode = {
      type: 'OBJECT',
      title: 'T',
      description: '',
      fields: [
        { name: 'name', schema: { type: 'STRING', title: '', description: '' } },
        {
          name: 'status',
          schema: {
            type: 'ENUM',
            title: '',
            description: '',
            values: [{ value: 'A', description: '' }],
          },
        },
        {
          name: 'tags',
          schema: {
            type: 'ARRAY',
            title: '',
            description: '',
            items: { type: 'STRING', title: '', description: '' },
          },
        },
      ],
    }
    expect(payloadFieldKindsFromSchema(schema)).toEqual({
      name: 'STRING',
      status: 'ENUM',
      tags: 'ARRAY',
    })
  })
})
