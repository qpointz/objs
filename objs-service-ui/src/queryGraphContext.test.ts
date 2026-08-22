import { describe, expect, it } from 'vitest'
import { EMPTY_GRAPH_CONTEXT } from './graphContext'
import {
  MATCH_ALL_OBJ_EXPR,
  ensureTraverseMatcherScoped,
  isGraphScopedMatcher,
  matcherFromGraphContext,
} from './queryGraphContext'

describe('matcherFromGraphContext', () => {
  it('uses match-all obj-expr for graph mode (traverse with graphId)', () => {
    expect(
      matcherFromGraphContext({
        ...EMPTY_GRAPH_CONTEXT,
        kind: 'graph',
        graphId: 'abc',
      }),
    ).toEqual(MATCH_ALL_OBJ_EXPR)
  })

  it('reuses graph-scoped matcher body', () => {
    const body = [{ 'graph-expr': "id == 'g'" }, { 'obj-expr': "type == 'API'" }]
    expect(
      matcherFromGraphContext({
        ...EMPTY_GRAPH_CONTEXT,
        kind: 'matcher',
        matcherBody: body,
        matcherLine: '…',
      }),
    ).toBe(body)
  })

  it('rejects bare obj-expr matcher context', () => {
    expect(() =>
      matcherFromGraphContext({
        ...EMPTY_GRAPH_CONTEXT,
        kind: 'matcher',
        matcherBody: { 'obj-expr': "type == 'API'" },
        matcherLine: '…',
      }),
    ).toThrow(/graph scope/i)
  })

  it('throws when empty', () => {
    expect(() => matcherFromGraphContext(EMPTY_GRAPH_CONTEXT)).toThrow(/graph context/i)
  })
})

describe('isGraphScopedMatcher', () => {
  it('detects stage-0 keys', () => {
    expect(isGraphScopedMatcher({ all: true })).toBe(true)
    expect(isGraphScopedMatcher({ 'graph-expr': 'id == "x"' })).toBe(true)
    expect(isGraphScopedMatcher({ 'graphs-in': ['x'] })).toBe(true)
    expect(isGraphScopedMatcher({ 'obj-expr': 'true' })).toBe(false)
  })
})

describe('ensureTraverseMatcherScoped', () => {
  it('passes through scoped matchers', () => {
    const body = { all: true }
    expect(ensureTraverseMatcherScoped(body)).toBe(body)
  })
})
