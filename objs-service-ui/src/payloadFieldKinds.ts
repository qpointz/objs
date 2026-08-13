import type { BoMSchema, BoMSchemaNode, PayloadFieldKind } from './types'

/** Map top-level entity schema fields → kinds for graph-card rendering. */
export function payloadFieldKindsFromSchema(
  contentSchema: BoMSchemaNode | undefined | null,
): Record<string, PayloadFieldKind> {
  if (!contentSchema || contentSchema.type !== 'OBJECT') return {}
  const out: Record<string, PayloadFieldKind> = {}
  for (const field of contentSchema.fields ?? []) {
    const t = field.schema.type
    if (
      t === 'STRING' ||
      t === 'ENUM' ||
      t === 'NUMBER' ||
      t === 'INTEGER' ||
      t === 'BOOLEAN' ||
      t === 'ARRAY' ||
      t === 'OBJECT'
    ) {
      out[field.name] = t
    } else {
      out[field.name] = 'OTHER'
    }
  }
  return out
}

/** Index schemas by `type@version` (entity + edge property schemas). */
export function payloadFieldKindsByTypeVersion(
  schemas: BoMSchema[],
): Map<string, Record<string, PayloadFieldKind>> {
  const map = new Map<string, Record<string, PayloadFieldKind>>()
  for (const schema of schemas) {
    map.set(`${schema.type}@${schema.version}`, payloadFieldKindsFromSchema(schema.contentSchema))
  }
  return map
}
