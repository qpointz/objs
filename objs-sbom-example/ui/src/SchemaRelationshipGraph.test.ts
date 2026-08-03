import { describe, expect, it } from 'vitest'
import { allowedEdgeLabel, schemaPropertyRows, schemaRelationshipElements } from './SchemaRelationshipGraph'
import type { BoMSchema, BoMSchemaNode, TypeEdgesResponse } from './types'

const contentSchema: BoMSchemaNode = {
  type: 'OBJECT',
  title: 'Component',
  description: 'Component payload',
  fields: [
    {
      name: 'name',
      required: true,
      schema: { type: 'STRING', title: 'Name', description: 'Component name' },
    },
    {
      name: 'metadata',
      required: false,
      schema: {
        type: 'OBJECT',
        title: 'Metadata',
        description: 'Nested metadata',
        fields: [
          {
            name: 'labels',
            schema: {
              type: 'ARRAY',
              title: 'Labels',
              description: 'Labels list',
              items: { type: 'STRING', title: 'Label', description: 'One label' },
            },
          },
        ],
      },
    },
  ],
}

const schema: BoMSchema = {
  type: 'Component',
  version: '1.0.0',
  usages: ['ENTITY'],
  contentSchema,
}

describe('SchemaRelationshipGraph', () => {
  it('flattens hierarchical fields into condensed UML rows', () => {
    const rows = schemaPropertyRows(contentSchema)

    expect(rows.map(({ name, type, depth, required }) => ({ name, type, depth, required }))).toEqual([
      { name: 'name', type: 'STRING', depth: 0, required: true },
      { name: 'metadata', type: 'OBJECT', depth: 0, required: false },
      { name: 'labels', type: 'ARRAY<STRING>', depth: 1, required: true },
    ])
  })

  it('formats edge labels with cardinality when specified', () => {
    expect(allowedEdgeLabel('CONTAINS')).toBe('CONTAINS')
    expect(allowedEdgeLabel('CONTAINS', 'UNSPECIFIED')).toBe('CONTAINS')
    expect(allowedEdgeLabel('CONTAINS', '1:1')).toBe('CONTAINS · 1:1')
    expect(allowedEdgeLabel('DEPENDS_ON', '1:*')).toBe('DEPENDS_ON · 1:*')
  })

  it('builds incoming and outgoing traversal nodes and labelled edges', () => {
    const relationships: TypeEdgesResponse = {
      incoming: [
        {
          sourceType: 'Product',
          role: 'CONTAINS',
          targetType: 'Component',
          propertiesPolicy: 'SCHEMA',
          emptyPropertiesAllowed: true,
          cardinality: '1:1',
        },
      ],
      outgoing: [
        {
          sourceType: 'Component',
          role: 'DEPENDS_ON',
          targetType: 'Component',
          propertiesPolicy: 'SCHEMA',
          emptyPropertiesAllowed: true,
          cardinality: '1:*',
        },
        {
          sourceType: 'Component',
          role: 'LICENSED_UNDER',
          targetType: 'License',
          propertiesPolicy: 'SCHEMA',
          emptyPropertiesAllowed: true,
          cardinality: 'UNSPECIFIED',
        },
      ],
    }

    const elements = schemaRelationshipElements(schema, relationships)

    expect(elements.nodes.map((node) => node.id)).toEqual([
      'selected',
      'incoming:Product',
      'outgoing:Component',
      'outgoing:License',
    ])
    expect(elements.edges.map((edge) => edge.label)).toEqual([
      'CONTAINS · 1:1',
      'DEPENDS_ON · 1:*',
      'LICENSED_UNDER',
    ])
  })
})
