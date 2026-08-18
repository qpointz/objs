import { describe, expect, it } from 'vitest'
import { buildGraphNeighborIndex } from './graphGoToNav'
import type { GraphLink, GraphNode } from './types'

function node(id: string, type: string, name = id): GraphNode {
  return {
    id,
    name,
    type,
    schemaVersion: '1',
    color: '#000',
    payload: {},
    annotations: {},
  }
}

function link(id: string, source: string, target: string, role: string): GraphLink {
  return {
    id,
    source,
    target,
    role,
    type: null,
    schemaVersion: null,
    properties: {},
  }
}

describe('buildGraphNeighborIndex', () => {
  it('groups neighbors by role and direction', () => {
    const nodes = [node('a', 'App', 'Portal'), node('b', 'Lib', 'Core'), node('c', 'Lib', 'Util')]
    const links = [
      link('e1', 'a', 'b', 'USES'),
      link('e2', 'c', 'a', 'USED_BY'),
    ]
    const index = buildGraphNeighborIndex(nodes, links)
    expect(index.targets.get('a')?.map((n) => n.id)).toEqual(['b'])
    expect(index.sources.get('a')?.map((n) => n.id)).toEqual(['c'])
    const groups = index.relationGroups.get('a') ?? []
    expect(groups.map((g) => `${g.direction}:${g.title}`).sort()).toEqual(['IN:USED_BY', 'OUT:USES'])
  })
})
