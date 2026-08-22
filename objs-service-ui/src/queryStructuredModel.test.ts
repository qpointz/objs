import { describe, expect, it } from 'vitest'
import type { BoMGremlinResult } from './api'
import {
  resolveStructuredMode,
  structuredEdgeRows,
  structuredVertexRows,
} from './queryStructuredModel'

function baseResult(over: Partial<BoMGremlinResult> = {}): BoMGremlinResult {
  return {
    primary: 'list',
    items: [],
    views: {},
    meta: {
      strategy: 'test',
      language: 'gremlin-lang',
      resultCount: 0,
      durationMs: 1,
      subgraph1Stats: { entities: 0, edges: 0 },
    },
    ...over,
  }
}

describe('resolveStructuredMode', () => {
  it('prefers table-alike over graph contents', () => {
    const result = baseResult({
      contents: {
        entities: [{ id: 'a', type: 'Component' }],
        edges: [],
      },
      views: {
        table: { columns: ['x'], rows: [['1']] },
      },
    })
    expect(resolveStructuredMode(result)).toBe('table')
  })

  it('uses graph mode when only contents', () => {
    const result = baseResult({
      contents: {
        entities: [{ id: 'a', type: 'Component', payload: { name: 'pandas' } }],
        edges: [{ source: 'a', target: 'a', role: 'self' }],
      },
    })
    expect(resolveStructuredMode(result)).toBe('graph')
  })

  it('is empty when neither projection nor contents', () => {
    expect(resolveStructuredMode(baseResult())).toBe('empty')
  })
})

describe('structured rows', () => {
  it('builds vertex and edge rows with display names', () => {
    const contents = {
      entities: [
        { id: 'a', type: 'Component', payload: { name: 'pandas' } },
        { id: 'b', type: 'Component', payload: { name: 'numpy' } },
      ],
      edges: [{ id: 'e1', source: 'a', target: 'b', role: 'dependsOn', type: 'Dep' }],
    }
    expect(structuredVertexRows(contents)[0].name).toBe('pandas')
    const edge = structuredEdgeRows(contents)[0]
    expect(edge.sourceName).toBe('pandas')
    expect(edge.targetName).toBe('numpy')
    expect(edge.role).toBe('dependsOn')
  })
})
