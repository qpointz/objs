import type { BoMSchemaNode } from './types'

/** Missing / null / blank string — identity not yet set (mirrors BoMIdentityProjection.isUnset). */
export function isUnsetIdentityValue(value: unknown): boolean {
  if (value == null) return true
  if (typeof value === 'string') return value.trim().length === 0
  return false
}

/**
 * Flat dotted-path identity map from schema `identifier` flags (arrays skipped).
 * Omits unset values — same contract as server `BoMIdentityProjection`.
 */
export function projectIdentityPaths(
  contentSchema: BoMSchemaNode,
  payload: Record<string, unknown> | null | undefined,
): Set<string> {
  const out = new Set<string>()
  if (contentSchema.type !== 'OBJECT') return out
  walk(contentSchema, payload ?? undefined, '', out)
  return out
}

function walk(
  schema: BoMSchemaNode,
  payload: Record<string, unknown> | undefined,
  prefix: string,
  out: Set<string>,
) {
  for (const field of schema.fields ?? []) {
    const path = prefix ? `${prefix}.${field.name}` : field.name
    const value = payload?.[field.name]
    if (field.schema.type === 'OBJECT') {
      walk(
        field.schema,
        value != null && typeof value === 'object' && !Array.isArray(value)
          ? (value as Record<string, unknown>)
          : undefined,
        path,
        out,
      )
      continue
    }
    if (field.schema.type === 'ARRAY') continue
    if (field.identifier === true && !isUnsetIdentityValue(value)) {
      out.add(path)
    }
  }
}

export function payloadValueAtPath(
  payload: Record<string, unknown> | null | undefined,
  path: string,
): unknown {
  if (!payload || !path) return undefined
  const parts = path.split('.')
  let cur: unknown = payload
  for (const part of parts) {
    if (cur == null || typeof cur !== 'object' || Array.isArray(cur)) return undefined
    cur = (cur as Record<string, unknown>)[part]
  }
  return cur
}
