import { describe, expect, it } from 'vitest'
import {
  addFieldAt,
  emptyObjectSchema,
  moveFieldAt,
  parseSchemaNode,
  removeFieldAt,
  resolveNode,
  serializeSchemaNode,
  updateFieldAt,
} from './schemaDsl'

describe('schemaDsl', () => {
  it('should round-trip yaml and json', () => {
    const node = emptyObjectSchema('Person', 'Person payload')
    const withField = addFieldAt(node, [], 'name')
    const yaml = serializeSchemaNode(withField, 'yaml')
    const json = serializeSchemaNode(withField, 'json')
    expect(parseSchemaNode(yaml, 'yaml').ok).toBe(true)
    expect(parseSchemaNode(json, 'json').ok).toBe(true)
    const parsed = parseSchemaNode(yaml, 'yaml')
    if (!parsed.ok) throw new Error(parsed.error)
    expect(parsed.value.fields?.[0]?.name).toBe('name')
  })

  it('should mutate nested fields immutably', () => {
    let root = emptyObjectSchema()
    root = addFieldAt(root, [], 'address')
    root = updateFieldAt(root, [], 0, (field) => ({
      ...field,
      schema: {
        type: 'OBJECT',
        title: 'Address',
        description: 'Postal address',
        fields: [],
      },
    }))
    root = addFieldAt(root, [0], 'city')
    expect(resolveNode(root, [0, 0]).title).toBe('Text')
    root = moveFieldAt(root, [], 0, 1)
    expect(root.fields?.[0]?.name).toBe('address')
    root = addFieldAt(root, [], 'name')
    root = moveFieldAt(root, [], 1, -1)
    expect(root.fields?.[0]?.name).toBe('name')
    root = removeFieldAt(root, [], 0)
    expect(root.fields?.map((f) => f.name)).toEqual(['address'])
  })

  it('should reject invalid source documents', () => {
    const result = parseSchemaNode('{', 'json')
    expect(result.ok).toBe(false)
  })
})
