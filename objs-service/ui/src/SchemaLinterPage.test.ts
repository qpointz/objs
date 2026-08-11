import { describe, expect, it } from 'vitest'
import { parseSchemaExpertDocument } from './SchemaLinterPage'

describe('Schema linter expert document', () => {
  it('accepts a complete edge schema document', () => {
    const result = parseSchemaExpertDocument({
      type: 'CanonicalEdge',
      version: '1.0.0',
      usage: 'EDGE_PROPERTIES',
      contentSchema: {
        type: 'OBJECT',
        title: 'Canonical edge',
        description: 'Relationship properties',
        fields: [],
      },
      allowedRelations: [
        {
          sourceType: 'Product',
          role: 'CONTAINS',
          targetType: 'Component',
          emptyPropertiesAllowed: true,
          cardinality: '1:1',
        },
      ],
    })

    expect(result.ok).toBe(true)
    if (result.ok) {
      expect(result.value.allowedRelations?.[0]?.role).toBe('CONTAINS')
      expect(result.value.allowedRelations?.[0]?.cardinality).toBe('1:1')
    }
  })

  it('requires the complete catalog envelope', () => {
    const result = parseSchemaExpertDocument({
      contentSchema: {
        type: 'OBJECT',
        title: 'Incomplete',
        description: 'Missing catalog identity',
        fields: [],
      },
    })

    expect(result).toEqual({ ok: false, error: 'Expert document type must not be blank' })
  })

  it('rejects unsupported usage', () => {
    const result = parseSchemaExpertDocument({
      type: 'Thing',
      version: '1.0.0',
      usage: 'UNKNOWN',
      contentSchema: {},
    })

    expect(result).toEqual({
      ok: false,
      error: 'Expert document usage must be ENTITY or EDGE_PROPERTIES',
    })
  })
})
