import { describe, expect, it } from 'vitest'
import {
  buildMatcherBody,
  hydrateFromMatcher,
  type MatcherMode,
} from './MatcherQueryForm'
import { scalarPayloadColumns } from './AddObjectsPanel'
import type { BoMEntity } from './types'

describe('buildMatcherBody / hydrateFromMatcher', () => {
  it('builds and hydrates obj-expr', () => {
    const expr = "type == 'Product' && a.app == 'payments-api'"
    const body = buildMatcherBody(
      'obj-expr',
      [{ key: '', value: '' }],
      '',
      expr,
      '',
      [],
      '[]',
      'visual',
    )
    expect(body).toEqual({ 'obj-expr': expr })
    const hydrated = hydrateFromMatcher(body)
    expect(hydrated?.mode).toBe('obj-expr')
    expect(hydrated?.objExprText).toBe(expr)
  })

  it('builds and hydrates subg-expr', () => {
    const expr = "a.decisionId == 'D-1'"
    const body = buildMatcherBody(
      'subg-expr',
      [{ key: '', value: '' }],
      '',
      '',
      expr,
      [],
      '[]',
      'visual',
    )
    expect(body).toEqual({ 'subg-expr': expr })
    const hydrated = hydrateFromMatcher(body)
    expect(hydrated?.mode).toBe('subg-expr')
    expect(hydrated?.subgExprText).toBe(expr)
  })

  it('round-trips visual chain stages to JSON body', () => {
    const stages = [
      { kind: 'anno' as const, rows: [{ key: 'app', value: 'x' }] },
      { kind: 'obj-expr' as const, expr: "type == 'Component'" },
      { kind: 'anno-expr' as const, expr: "env == 'prod'" },
      { kind: 'subg-expr' as const, expr: "a.decisionId == 'D-1'" },
    ]
    const body = buildMatcherBody('chained', [], '', '', '', stages, '[]', 'visual')
    expect(body).toEqual([
      { anno: { app: 'x' } },
      { 'obj-expr': "type == 'Component'" },
      { 'anno-expr': "env == 'prod'" },
      { 'subg-expr': "a.decisionId == 'D-1'" },
    ])
    const hydrated = hydrateFromMatcher(body)
    expect(hydrated?.mode).toBe('chained')
    expect(hydrated?.chainView).toBe('visual')
    expect(hydrated?.chainStages).toEqual(stages)
  })

  it('builds chained from JSON view', () => {
    const json = JSON.stringify([{ 'obj-expr': "id == 'a'" }, { anno: { k: 'v' } }], null, 2)
    const body = buildMatcherBody('chained', [], '', '', '', [], json, 'json')
    expect(body).toEqual([{ 'obj-expr': "id == 'a'" }, { anno: { k: 'v' } }])
  })

  it('hydrates unknown chain stages as JSON view', () => {
    const body = [{ ids: ['11111111-1111-1111-1111-111111111111'] }]
    const hydrated = hydrateFromMatcher(body)
    expect(hydrated?.mode).toBe('chained')
    expect(hydrated?.chainView).toBe('json')
  })

  it('rejects blank obj-expr', () => {
    expect(() =>
      buildMatcherBody('obj-expr' as MatcherMode, [], '', '  ', '', [], '[]', 'visual'),
    ).toThrow(/obj-expr/)
  })

  it('rejects blank subg-expr', () => {
    expect(() =>
      buildMatcherBody('subg-expr' as MatcherMode, [], '', '', '  ', [], '[]', 'visual'),
    ).toThrow(/subg-expr/)
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
