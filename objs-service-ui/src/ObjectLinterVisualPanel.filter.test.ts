import { describe, expect, it } from 'vitest'
import { applyChangesOnlyDimming } from './ObjectLinterVisualPanel'
import type { GraphLink, GraphNode } from './types'

function node(id: string, draftStatus: GraphNode['draftStatus']): GraphNode {
  return {
    id,
    name: id,
    type: 'T',
    schemaVersion: '1',
    color: '#000',
    payload: {},
    annotations: {},
    draftStatus,
  }
}

function link(
  id: string,
  source: string,
  target: string,
  draftStatus: GraphLink['draftStatus'],
): GraphLink {
  return {
    id,
    source,
    target,
    role: 'R',
    type: null,
    schemaVersion: null,
    properties: {},
    draftStatus,
  }
}

describe('applyChangesOnlyDimming', () => {
  it('shouldKeepAllNodesAndDimUnchanged_whenChangesOnly', () => {
    const nodes = [node('a', 'new'), node('b', 'unchanged'), node('c', 'modified')]
    const links = [link('e1', 'a', 'c', 'new'), link('e2', 'b', 'c', 'unchanged')]
    const result = applyChangesOnlyDimming(nodes, links, true)
    expect(result.nodes).toHaveLength(3)
    expect(result.links).toHaveLength(2)
    expect(result.nodes.find((n) => n.id === 'a')?.dimmed).toBe(false)
    expect(result.nodes.find((n) => n.id === 'b')?.dimmed).toBe(true)
    expect(result.nodes.find((n) => n.id === 'c')?.dimmed).toBe(false)
    expect(result.links.find((l) => l.id === 'e1')?.dimmed).toBe(false)
    expect(result.links.find((l) => l.id === 'e2')?.dimmed).toBe(true)
  })

  it('shouldClearDimmed_whenChangesOnlyOff', () => {
    const nodes = [{ ...node('b', 'unchanged'), dimmed: true }]
    const links = [{ ...link('e2', 'b', 'b', 'unchanged'), dimmed: true }]
    const result = applyChangesOnlyDimming(nodes, links, false)
    expect(result.nodes[0].dimmed).toBe(false)
    expect(result.links[0].dimmed).toBe(false)
  })
})
