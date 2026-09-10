import { describe, expect, it } from 'vitest'
import { applyEdgeRoleDimming, applyGraphCanvasFilters } from './graphFilterDimming'
import type { GraphLink, GraphNode } from './types'

function node(id: string, type: string, dimmed?: boolean): GraphNode {
  return {
    id,
    name: id,
    type,
    schemaVersion: '1',
    color: '#000',
    payload: {},
    annotations: {},
    ...(dimmed ? { dimmed: true } : {}),
  }
}

function link(
  id: string,
  source: string,
  target: string,
  role: string,
  dimmed?: boolean,
): GraphLink {
  return {
    id,
    source,
    target,
    role,
    type: null,
    schemaVersion: null,
    properties: {},
    ...(dimmed ? { dimmed: true } : {}),
  }
}

describe('applyEdgeRoleDimming', () => {
  it('shouldDimNonMatchingRolesAndIsolatedNodes', () => {
    const nodes = [node('a', 'Pkg'), node('b', 'Comp'), node('c', 'Pkg')]
    const links = [link('e1', 'a', 'b', 'depends'), link('e2', 'a', 'c', 'contains')]
    const result = applyEdgeRoleDimming(nodes, links, new Set(['depends']))
    expect(result.links.find((l) => l.id === 'e1')?.dimmed).toBe(false)
    expect(result.links.find((l) => l.id === 'e2')?.dimmed).toBe(true)
    expect(result.nodes.find((n) => n.id === 'a')?.dimmed).toBe(false)
    expect(result.nodes.find((n) => n.id === 'b')?.dimmed).toBe(false)
    expect(result.nodes.find((n) => n.id === 'c')?.dimmed).toBe(true)
  })

  it('shouldClearWhenEmpty', () => {
    const nodes = [node('a', 'Pkg', true)]
    const links = [link('e1', 'a', 'a', 'r', true)]
    const result = applyEdgeRoleDimming(nodes, links, new Set())
    expect(result.nodes[0].dimmed).toBe(false)
    expect(result.links[0].dimmed).toBe(false)
  })
})

describe('applyGraphCanvasFilters', () => {
  it('shouldUnionTypeAndEdgeFiltersWhenBothActive', () => {
    const nodes = [node('a', 'Pkg'), node('b', 'Comp'), node('c', 'Pkg')]
    const links = [link('e1', 'a', 'b', 'depends'), link('e2', 'a', 'c', 'contains')]
    const result = applyGraphCanvasFilters(nodes, links, {
      types: new Set(['Pkg']),
      edgeRoles: new Set(['depends']),
    })
    // Union: all Pkgs + depends endpoints (includes Comp b)
    expect(result.nodes.find((n) => n.id === 'a')?.dimmed).toBe(false)
    expect(result.nodes.find((n) => n.id === 'b')?.dimmed).toBe(false)
    expect(result.nodes.find((n) => n.id === 'c')?.dimmed).toBe(false)
    expect(result.links.find((l) => l.id === 'e1')?.dimmed).toBe(false)
    expect(result.links.find((l) => l.id === 'e2')?.dimmed).toBe(false) // either end is Pkg
  })

  it('shouldApplyTypeOnlyWhenNoEdgeFilter', () => {
    const nodes = [node('a', 'Pkg'), node('b', 'Comp')]
    const links = [link('e1', 'a', 'b', 'depends')]
    const result = applyGraphCanvasFilters(nodes, links, {
      types: new Set(['Pkg']),
      edgeRoles: new Set(),
    })
    expect(result.nodes.find((n) => n.id === 'a')?.dimmed).toBe(false)
    expect(result.nodes.find((n) => n.id === 'b')?.dimmed).toBe(true)
  })
})
