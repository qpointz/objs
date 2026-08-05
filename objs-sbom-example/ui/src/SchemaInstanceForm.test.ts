import { describe, expect, it } from 'vitest'
import { defaultValueForSchema, fieldLabel } from './SchemaInstanceForm'
import type { BoMSchemaNode } from './types'

describe('defaultValueForSchema', () => {
  it('fills required object fields and defaults', () => {
    const schema: BoMSchemaNode = {
      type: 'OBJECT',
      title: 'T',
      description: '',
      fields: [
        {
          name: 'name',
          required: true,
          schema: { type: 'STRING', title: 'Name', description: '', default: 'x' },
        },
        {
          name: 'optional',
          required: false,
          schema: { type: 'STRING', title: 'Opt', description: '' },
        },
        {
          name: 'flag',
          required: true,
          schema: { type: 'BOOLEAN', title: 'Flag', description: '' },
        },
      ],
    }
    expect(defaultValueForSchema(schema)).toEqual({ name: 'x', flag: false })
  })

  it('returns empty array for ARRAY', () => {
    expect(
      defaultValueForSchema({
        type: 'ARRAY',
        title: 'A',
        description: '',
        items: { type: 'STRING', title: 'S', description: '' },
      }),
    ).toEqual([])
  })
})

describe('fieldLabel', () => {
  it('uses field name when schema title is a generic placeholder', () => {
    expect(
      fieldLabel({
        name: 'ecosystem',
        schema: { type: 'STRING', title: 'Text', description: 'Text value' },
      }),
    ).toBe('ecosystem')
  })

  it('uses authored title when it is meaningful', () => {
    expect(
      fieldLabel({
        name: 'name',
        schema: { type: 'STRING', title: 'Display name', description: 'Human label' },
      }),
    ).toBe('Display name')
  })
})
