import { afterEach, describe, expect, it } from 'vitest'
import {
  EMPTY_GRAPH_CONTEXT,
  GRAPH_CONTEXT_STORAGE_KEY,
  loadGraphContextSnapshot,
  saveGraphContextSnapshot,
  shortId,
} from './graphContext'

describe('graphContext', () => {
  afterEach(() => {
    window.localStorage.removeItem(GRAPH_CONTEXT_STORAGE_KEY)
    window.localStorage.removeItem('objs.ui.currentGraphId')
  })

  it('shortId truncates long ids', () => {
    expect(shortId('abcdefghijklmnop')).toBe('abcdefgh…')
    expect(shortId('short')).toBe('short')
  })

  it('round-trips graph mode snapshot with version pin', () => {
    const snap = {
      ...EMPTY_GRAPH_CONTEXT,
      kind: 'graph' as const,
      graphId: 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
      graphVersion: 1700000000001,
      graphVersionAnnotations: { env: 'prod' },
      graphVersionCreatedAt: '2026-01-02T03:04:05Z',
      annotations: { kind: 'demo' },
      nodeCount: 3,
      edgeCount: 2,
    }
    saveGraphContextSnapshot(snap)
    expect(loadGraphContextSnapshot()).toEqual(snap)
  })

  it('clears graphVersion when kind is not graph', () => {
    window.localStorage.setItem(
      GRAPH_CONTEXT_STORAGE_KEY,
      JSON.stringify({
        kind: 'matcher',
        graphId: null,
        graphVersion: 99,
        graphVersionAnnotations: { x: 'y' },
        matcherBody: { 'obj-expr': 'true' },
        matcherLine: 'x',
        annotations: {},
        nodeCount: 1,
        edgeCount: 0,
      }),
    )
    expect(loadGraphContextSnapshot().graphVersion).toBeNull()
    expect(loadGraphContextSnapshot().graphVersionAnnotations).toEqual({})
  })

  it('round-trips matcher mode snapshot', () => {
    const snap = {
      ...EMPTY_GRAPH_CONTEXT,
      kind: 'matcher' as const,
      matcherBody: { 'obj-expr': "type == 'API'" },
      matcherLine: "obj-expr: type == 'API'",
      nodeCount: 10,
      edgeCount: 0,
    }
    saveGraphContextSnapshot(snap)
    expect(loadGraphContextSnapshot()).toEqual(snap)
  })

  it('returns empty on corrupt storage', () => {
    window.localStorage.setItem(GRAPH_CONTEXT_STORAGE_KEY, '{not-json')
    expect(loadGraphContextSnapshot()).toEqual(EMPTY_GRAPH_CONTEXT)
  })

  it('seeds from legacy currentGraphId when empty', () => {
    window.localStorage.setItem('objs.ui.currentGraphId', 'legacy-graph-id')
    expect(loadGraphContextSnapshot()).toMatchObject({
      kind: 'graph',
      graphId: 'legacy-graph-id',
    })
  })
})
