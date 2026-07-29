import { parse as parseYaml, stringify as stringifyYaml } from 'yaml'
import type { BoMSchemaNode, BoMSchemaType } from './types'

export type EditorFormat = 'json' | 'yaml'

export function serializeSchemaNode(node: BoMSchemaNode, format: EditorFormat): string {
  return format === 'json' ? JSON.stringify(node, null, 2) : stringifyYaml(node)
}

export function parseSchemaNode(
  text: string,
  format: EditorFormat,
): { ok: true; value: BoMSchemaNode } | { ok: false; error: string } {
  try {
    const parsed = format === 'json' ? JSON.parse(text) : parseYaml(text)
    const value = format === 'yaml' ? (JSON.parse(JSON.stringify(parsed)) as unknown) : parsed
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
      return { ok: false, error: 'Schema must be an object' }
    }
    return { ok: true, value: value as BoMSchemaNode }
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : 'Invalid document' }
  }
}

export function emptyObjectSchema(title = 'New schema', description = 'Schema draft'): BoMSchemaNode {
  return {
    type: 'OBJECT',
    title,
    description,
    fields: [],
  }
}

export function defaultNodeForType(type: BoMSchemaType): BoMSchemaNode {
  switch (type) {
    case 'OBJECT':
      return { type: 'OBJECT', title: 'Object', description: 'Object value', fields: [] }
    case 'ARRAY':
      return {
        type: 'ARRAY',
        title: 'List',
        description: 'Array value',
        items: { type: 'STRING', title: 'Item', description: 'Array item' },
      }
    case 'STRING':
      return { type: 'STRING', title: 'Text', description: 'Text value' }
    case 'NUMBER':
      return { type: 'NUMBER', title: 'Number', description: 'Numeric value' }
    case 'INTEGER':
      return { type: 'INTEGER', title: 'Integer', description: 'Whole-number value' }
    case 'BOOLEAN':
      return { type: 'BOOLEAN', title: 'Flag', description: 'Boolean value' }
    case 'ENUM':
      return {
        type: 'ENUM',
        title: 'Choice',
        description: 'Enumerated value',
        values: [{ value: 'VALUE', description: 'Example value' }],
      }
  }
}

/** Path segments into a recursive schema tree. Numbers index object fields; 'items' descends into arrays. */
export type SchemaPath = Array<number | 'items'>

export function resolveNode(root: BoMSchemaNode, path: SchemaPath): BoMSchemaNode {
  let current = root
  for (const segment of path) {
    if (segment === 'items') {
      if (!current.items) throw new Error('Missing array items')
      current = current.items
      continue
    }
    const field = current.fields?.[segment]
    if (!field) throw new Error(`Missing field at ${segment}`)
    current = field.schema
  }
  return current
}

export function updateNodeAt(
  root: BoMSchemaNode,
  path: SchemaPath,
  updater: (node: BoMSchemaNode) => BoMSchemaNode,
): BoMSchemaNode {
  if (path.length === 0) return updater(root)
  const [head, ...tail] = path
  if (head === 'items') {
    if (!root.items) throw new Error('Missing array items')
    return { ...root, items: updateNodeAt(root.items, tail, updater) }
  }
  const fields = [...(root.fields ?? [])]
  const field = fields[head]
  if (!field) throw new Error(`Missing field at ${head}`)
  fields[head] = { ...field, schema: updateNodeAt(field.schema, tail, updater) }
  return { ...root, fields }
}

export function updateFieldAt(
  root: BoMSchemaNode,
  path: SchemaPath,
  fieldIndex: number,
  updater: (field: NonNullable<BoMSchemaNode['fields']>[number]) => NonNullable<BoMSchemaNode['fields']>[number],
): BoMSchemaNode {
  return updateNodeAt(root, path, (node) => {
    const fields = [...(node.fields ?? [])]
    fields[fieldIndex] = updater(fields[fieldIndex])
    return { ...node, fields }
  })
}

export function addFieldAt(root: BoMSchemaNode, path: SchemaPath, name = 'field'): BoMSchemaNode {
  return updateNodeAt(root, path, (node) => ({
    ...node,
    fields: [
      ...(node.fields ?? []),
      {
        name,
        required: true,
        schema: defaultNodeForType('STRING'),
      },
    ],
  }))
}

export function removeFieldAt(root: BoMSchemaNode, path: SchemaPath, fieldIndex: number): BoMSchemaNode {
  return updateNodeAt(root, path, (node) => {
    const fields = [...(node.fields ?? [])]
    fields.splice(fieldIndex, 1)
    return { ...node, fields }
  })
}

export function moveFieldAt(
  root: BoMSchemaNode,
  path: SchemaPath,
  fieldIndex: number,
  direction: -1 | 1,
): BoMSchemaNode {
  return updateNodeAt(root, path, (node) => {
    const fields = [...(node.fields ?? [])]
    const target = fieldIndex + direction
    if (target < 0 || target >= fields.length) return node
    ;[fields[fieldIndex], fields[target]] = [fields[target], fields[fieldIndex]]
    return { ...node, fields }
  })
}
