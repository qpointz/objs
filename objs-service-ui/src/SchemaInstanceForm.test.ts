import { describe, expect, it } from 'vitest'
import {
  defaultValueForSchema,
  fieldDescription,
  fieldLabel,
  migrateNeedsConfirm,
  migratePayloadByKey,
} from './SchemaInstanceForm'
import type { BoMSchemaNode } from './types'

describe('defaultValueForSchema', () => {
  it('seeds only explicit schema defaults, not invented required placeholders', () => {
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
          name: 'title',
          required: true,
          schema: { type: 'STRING', title: 'Title', description: '' },
        },
        {
          name: 'count',
          required: true,
          schema: { type: 'INTEGER', title: 'Count', description: '' },
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
    expect(defaultValueForSchema(schema)).toEqual({ name: 'x' })
  })

  it('nests only object branches that contain explicit defaults', () => {
    const schema: BoMSchemaNode = {
      type: 'OBJECT',
      title: 'T',
      description: '',
      fields: [
        {
          name: 'meta',
          required: true,
          schema: {
            type: 'OBJECT',
            title: 'Meta',
            description: '',
            fields: [
              {
                name: 'kind',
                required: true,
                schema: { type: 'STRING', title: 'Kind', description: '', default: 'a' },
              },
              {
                name: 'label',
                required: true,
                schema: { type: 'STRING', title: 'Label', description: '' },
              },
            ],
          },
        },
        {
          name: 'emptyNest',
          required: true,
          schema: {
            type: 'OBJECT',
            title: 'Empty',
            description: '',
            fields: [
              {
                name: 'x',
                required: true,
                schema: { type: 'STRING', title: 'X', description: '' },
              },
            ],
          },
        },
      ],
    }
    expect(defaultValueForSchema(schema)).toEqual({ meta: { kind: 'a' } })
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

describe('fieldDescription', () => {
  it('shouldOmitGenericPlaceholders', () => {
    expect(
      fieldDescription({
        name: 'x',
        schema: { type: 'STRING', title: '', description: 'Text value' },
      }),
    ).toBeUndefined()
  })

  it('shouldReturnAuthoredDescription', () => {
    expect(
      fieldDescription({
        name: 'x',
        schema: { type: 'STRING', title: '', description: '  Package name  ' },
      }),
    ).toBe('Package name')
  })
})

describe('migratePayloadByKey', () => {
  const productV1: BoMSchemaNode = {
    type: 'OBJECT',
    title: 'Product',
    description: '',
    fields: [
      { name: 'name', schema: { type: 'STRING', title: 'Name', description: '' } },
      { name: 'version', schema: { type: 'STRING', title: 'Version', description: '' } },
    ],
  }

  const productV2: BoMSchemaNode = {
    type: 'OBJECT',
    title: 'Product',
    description: '',
    fields: [
      { name: 'name', schema: { type: 'STRING', title: 'Name', description: '' } },
      {
        name: 'meta',
        schema: {
          type: 'OBJECT',
          title: 'Meta',
          description: '',
          fields: [
            { name: 'kind', schema: { type: 'STRING', title: 'Kind', description: '' } },
          ],
        },
      },
    ],
  }

  it('shouldCopyMatchingKeys_andDropUnmatched', () => {
    const result = migratePayloadByKey(
      { name: 'pkg', version: '1.0', extra: true },
      productV1,
    )
    expect(result.payload).toEqual({ name: 'pkg', version: '1.0' })
    expect(result.copied).toBe(2)
    expect(result.dropped).toBe(1)
    expect(migrateNeedsConfirm(result)).toBe(true)
  })

  it('shouldRecurseObjectFields_andDropNestedUnmatched', () => {
    const result = migratePayloadByKey(
      { name: 'pkg', meta: { kind: 'lib', leftover: 1 } },
      productV2,
    )
    expect(result.payload).toEqual({ name: 'pkg', meta: { kind: 'lib' } })
    expect(result.copied).toBe(3) // name + meta object + kind
    expect(result.dropped).toBe(1)
    expect(migrateNeedsConfirm(result)).toBe(true)
  })

  it('shouldNeedConfirm_whenZeroCopies', () => {
    const result = migratePayloadByKey({ other: 'x' }, productV1)
    expect(result.payload).toEqual({})
    expect(result.copied).toBe(0)
    expect(result.dropped).toBe(1)
    expect(migrateNeedsConfirm(result)).toBe(true)
  })

  it('shouldNotNeedConfirm_whenFullMatch', () => {
    const result = migratePayloadByKey({ name: 'a', version: 'b' }, productV1)
    expect(result.payload).toEqual({ name: 'a', version: 'b' })
    expect(result.copied).toBe(2)
    expect(result.dropped).toBe(0)
    expect(migrateNeedsConfirm(result)).toBe(false)
  })

  it('shouldTreatEmptyStringAsPresentValue_notDropped', () => {
    const result = migratePayloadByKey({ name: '', version: '1' }, productV1)
    expect(result.payload).toEqual({ name: '', version: '1' })
    expect(result.copied).toBe(2)
    expect(result.dropped).toBe(0)
  })

  it('shouldNeedConfirm_whenEmptySource', () => {
    const result = migratePayloadByKey({}, productV1)
    expect(result.copied).toBe(0)
    expect(result.dropped).toBe(0)
    expect(migrateNeedsConfirm(result)).toBe(true)
  })
})
