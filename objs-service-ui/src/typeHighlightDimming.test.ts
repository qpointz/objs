import { describe, expect, it } from 'vitest'
import { applyTypeHighlightDimming, toggleTypeInSet } from './typeHighlightDimming'
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

function link(id: string, source: string, target: string, dimmed?: boolean): GraphLink {
  return {
    id,
    source,
    target,
    role: 'r',
    type: null,
    schemaVersion: null,
    properties: {},
    ...(dimmed ? { dimmed: true } : {}),
  }
}

describe('applyTypeHighlightDimming', () => {
  it('shouldDimNonMatchingTypesAndEdges', () => {
    const nodes = [node('a', 'Pkg'), node('b', 'Comp'), node('c', 'Pkg')]
    const links = [link('e1', 'a', 'c'), link('e2', 'a', 'b')]
    const result = applyTypeHighlightDimming(nodes, links, new Set(['Pkg']))
    expect(result.nodes.find((n) => n.id === 'a')?.dimmed).toBe(false)
    expect(result.nodes.find((n) => n.id === 'b')?.dimmed).toBe(true)
    expect(result.nodes.find((n) => n.id === 'c')?.dimmed).toBe(false)
    expect(result.links.find((l) => l.id === 'e1')?.dimmed).toBe(false)
    // either-end (default): a is selected → edge stays visible
    expect(result.links.find((l) => l.id === 'e2')?.dimmed).toBe(false)
  })

  it('shouldDimEdgeUnlessBothEndsWhenBothEndsMode', () => {
    const nodes = [node('a', 'Pkg'), node('b', 'Comp')]
    const links = [link('e2', 'a', 'b')]
    const result = applyTypeHighlightDimming(nodes, links, new Set(['Pkg']), {
      edgeKeepIf: 'both-ends',
    })
    expect(result.links.find((l) => l.id === 'e2')?.dimmed).toBe(true)
  })

  it('shouldClearDimWhenFilterEmpty', () => {
    const nodes = [node('a', 'Pkg', true)]
    const links = [link('e1', 'a', 'a', true)]
    const result = applyTypeHighlightDimming(nodes, links, new Set())
    expect(result.nodes[0].dimmed).toBe(false)
    expect(result.links[0].dimmed).toBe(false)
  })

  it('shouldComposeWithExistingDimWithoutClearing', () => {
    const nodes = [node('a', 'Pkg', true), node('b', 'Comp')]
    const links = [link('e1', 'a', 'b', true)]
    const empty = applyTypeHighlightDimming(nodes, links, new Set(), { compose: true })
    expect(empty.nodes.find((n) => n.id === 'a')?.dimmed).toBe(true)
    expect(empty.links[0].dimmed).toBe(true)

    const filtered = applyTypeHighlightDimming(nodes, links, new Set(['Pkg']), { compose: true })
    expect(filtered.nodes.find((n) => n.id === 'a')?.dimmed).toBe(true)
    expect(filtered.nodes.find((n) => n.id === 'b')?.dimmed).toBe(true)
    expect(filtered.links[0].dimmed).toBe(true)
  })
})

describe('toggleTypeInSet', () => {
  it('shouldAddAndRemove', () => {
    const once = toggleTypeInSet(new Set(), 'A')
    expect([...once]).toEqual(['A'])
    expect([...toggleTypeInSet(once, 'A')]).toEqual([])
    expect([...toggleTypeInSet(once, 'B')].sort()).toEqual(['A', 'B'])
  })
})
