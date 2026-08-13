import { describe, expect, it } from 'vitest'
import {
  buildMatcherBody,
  hydrateFromMatcher,
  type MatcherMode,
} from './MatcherQueryForm'
import { scalarPayloadColumns } from './AddObjectsPanel'
import type { BoMEntity } from './types'

describe('buildMatcherBody / hydrateFromMatcher', () => {
  it('builds and hydrates all', () => {
    const body = buildMatcherBody('all', '', '', [], '[]', 'visual')
    expect(body).toEqual({ all: true })
    const hydrated = hydrateFromMatcher(body)
    expect(hydrated?.mode).toBe('all')
  })

  it('builds and hydrates graph-expr', () => {
    const expr = "a.env == 'prod'"
    const body = buildMatcherBody('graph-expr', expr, '', [], '[]', 'visual')
    expect(body).toEqual({ 'graph-expr': expr })
    const hydrated = hydrateFromMatcher(body)
    expect(hydrated?.mode).toBe('graph-expr')
    expect(hydrated?.graphExprText).toBe(expr)
  })

  it('builds and hydrates obj-expr', () => {
    const expr = "type == 'Product' && a.app == 'payments-api'"
    const body = buildMatcherBody('obj-expr', '', expr, [], '[]', 'visual')
    expect(body).toEqual({ 'obj-expr': expr })
    const hydrated = hydrateFromMatcher(body)
    expect(hydrated?.mode).toBe('obj-expr')
    expect(hydrated?.objExprText).toBe(expr)
  })

  it('round-trips visual chain with all then obj-expr', () => {
    const stages = [
      { kind: 'all' as const },
      { kind: 'obj-expr' as const, expr: "type == 'Component'" },
    ]
    const body = buildMatcherBody('chained', '', '', stages, '[]', 'visual')
    expect(body).toEqual([{ all: true }, { 'obj-expr': "type == 'Component'" }])
    expect(hydrateFromMatcher(body)?.chainStages).toEqual(stages)
  })

  it('round-trips visual chain stages to JSON body', () => {
    const stages = [
      { kind: 'graph-expr' as const, expr: "a.decisionId == 'D-1'" },
      { kind: 'obj-expr' as const, expr: "type == 'Component'" },
    ]
    const body = buildMatcherBody('chained', '', '', stages, '[]', 'visual')
    expect(body).toEqual([
      { 'graph-expr': "a.decisionId == 'D-1'" },
      { 'obj-expr': "type == 'Component'" },
    ])
    const hydrated = hydrateFromMatcher(body)
    expect(hydrated?.mode).toBe('chained')
    expect(hydrated?.chainView).toBe('visual')
    expect(hydrated?.chainStages).toEqual(stages)
  })

  it('builds chained from JSON view', () => {
    const json = JSON.stringify(
      [{ 'graph-expr': "id == 'a'" }, { 'obj-expr': "type == 'X'" }],
      null,
      2,
    )
    const body = buildMatcherBody('chained', '', '', [], json, 'json')
    expect(body).toEqual([{ 'graph-expr': "id == 'a'" }, { 'obj-expr': "type == 'X'" }])
  })

  it('hydrates unknown chain stages as JSON view', () => {
    const body = [{ ids: ['11111111-1111-1111-1111-111111111111'] }]
    const hydrated = hydrateFromMatcher(body)
    expect(hydrated?.mode).toBe('chained')
    expect(hydrated?.chainView).toBe('json')
  })

  it('rejects blank graph-expr', () => {
    expect(() =>
      buildMatcherBody('graph-expr' as MatcherMode, '  ', '', [], '[]', 'visual'),
    ).toThrow(/graph-expr/)
  })

  it('rejects blank obj-expr', () => {
    expect(() =>
      buildMatcherBody('obj-expr' as MatcherMode, '', '  ', [], '[]', 'visual'),
    ).toThrow(/obj-expr/)
  })
})

describe('scalarPayloadColumns', () => {
  it('picks frequent scalar keys and skips nested values', () => {
    const entities: BoMEntity[] = [
      {
        id: '1',
        type: 'A',
        annotations: {},
        payload: { name: 'a', count: 1, nested: { x: 1 }, tags: ['t'] },
      },
      {
        id: '2',
        type: 'A',
        annotations: {},
        payload: { name: 'b', flag: true, count: 2 },
      },
    ]
    expect(scalarPayloadColumns(entities, 6)).toEqual(['count', 'name', 'flag'])
  })
})
