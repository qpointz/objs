import { describe, expect, it } from 'vitest'
import {
  catalogSeedContainsGraph,
  catalogSeedObjectTypes,
  catalogSeedTypeRevealQuery,
  jsonSchemaDefKeys,
  jsonSchemaDefRevealQuery,
  schemaCatalogElements,
} from './catalogOverviewModel'
import type { BoMAllowedEdgeRule } from './types'

describe('schemaCatalogElements', () => {
  it('builds entity nodes and labelled allow-list edges', () => {
    const rules: BoMAllowedEdgeRule[] = [
      {
        sourceType: 'Product',
        role: 'CONTAINS',
        targetType: 'Component',
        propertiesPolicy: 'NONE',
        emptyPropertiesAllowed: true,
        cardinality: '1:*',
      },
    ]
    const { nodes, edges } = schemaCatalogElements(
      [
        { type: 'Product', version: '1.0.0' },
        { type: 'Component', version: '2.0.0' },
      ],
      rules,
    )
    expect(nodes.map((n) => n.id).sort()).toEqual(['type:Component', 'type:Product'])
    expect(edges).toHaveLength(1)
    expect(edges[0].label).toBe('CONTAINS · 1:*')
    expect(edges[0].source).toBe('type:Product')
    expect(edges[0].target).toBe('type:Component')
    expect(edges[0].type).toBe('step')
    const product = nodes.find((n) => n.id === 'type:Product')!
    const component = nodes.find((n) => n.id === 'type:Component')!
    expect(product.position.x).toBeLessThan(component.position.x)
  })

  it('adds a wildcard node when rules use *', () => {
    const { nodes } = schemaCatalogElements(
      [{ type: 'Component', version: '1.0.0' }],
      [
        {
          sourceType: '*',
          role: 'DEPENDS_ON',
          targetType: 'Component',
          propertiesPolicy: 'NONE',
          emptyPropertiesAllowed: true,
        },
      ],
    )
    expect(nodes.some((n) => n.id === 'type:*')).toBe(true)
  })
})

describe('catalogSeedContainsGraph', () => {
  it('detects Graph documents in multi-doc YAML', () => {
    const yaml = `
apiVersion: objs.poc.org/v1
kind: ObjectSchema
type: A
version: "1.0.0"
---
apiVersion: objs.poc.org/v1
kind: Graph
name: demo
entities: []
edges: []
`
    expect(catalogSeedContainsGraph(yaml)).toBe(true)
  })

  it('accepts catalog-only YAML', () => {
    const yaml = `
apiVersion: objs.poc.org/v1
kind: ObjectSchema
type: A
version: "1.0.0"
---
apiVersion: objs.poc.org/v1
kind: AllowedEdgeRule
sourceType: A
role: RELATES_TO
targetType: A
propertiesPolicy: NONE
emptyPropertiesAllowed: true
cardinality: UNSPECIFIED
`
    expect(catalogSeedContainsGraph(yaml)).toBe(false)
  })
})

describe('jsonSchemaDefKeys', () => {
  it('shouldListSortedDefKeys', () => {
    const body = JSON.stringify(
      {
        $defs: {
          Dataset: { type: 'object' },
          Database: { type: 'object' },
        },
      },
      null,
      2,
    )
    expect(jsonSchemaDefKeys(body)).toEqual(['Database', 'Dataset'])
    expect(jsonSchemaDefRevealQuery('Database')).toBe('"Database": {')
  })
})

describe('catalogSeedObjectTypes', () => {
  it('shouldCollectObjectSchemaTypes', () => {
    const yaml = `
apiVersion: objs.poc.org/v1
kind: ObjectSchema
type: Dataset
version: "1.0.0"
---
apiVersion: objs.poc.org/v1
kind: ObjectSchema
type: Database
version: "1.0.0"
---
apiVersion: objs.poc.org/v1
kind: AllowedEdgeRule
sourceType: Database
role: CONTAINS
targetType: Dataset
`
    expect(catalogSeedObjectTypes(yaml)).toEqual(['Database', 'Dataset'])
    expect(catalogSeedTypeRevealQuery('Database')).toBe('type: "Database"')
  })
})
