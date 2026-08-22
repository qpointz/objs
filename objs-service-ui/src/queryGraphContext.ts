import type { GraphContextSnapshot } from './graphContext'

/** Match-all obj-expr for in-graph / pool listing (Note: Objects empty matcher). */
export const MATCH_ALL_OBJ_EXPR = { 'obj-expr': 'true' } as const

/**
 * Matcher for traverse:
 * - Graph mode → match-all in that graph (caller must pass `graphId` to traverse API)
 * - Matcher mode → stored body (must already be graph-scoped for `/graphs/query` select)
 */
export function matcherFromGraphContext(context: GraphContextSnapshot): unknown {
  if (context.kind === 'graph' && context.graphId) {
    return MATCH_ALL_OBJ_EXPR
  }
  if (context.kind === 'matcher' && context.matcherBody != null) {
    return ensureTraverseMatcherScoped(context.matcherBody)
  }
  throw new Error('Open a graph or matcher as graph context before Exec')
}

/** True when stage-0 is all / graph-expr / graphs-in (or a chain starting with one). */
export function isGraphScopedMatcher(body: unknown): boolean {
  const stages = Array.isArray(body) ? body : [body]
  if (stages.length === 0) return false
  const first = stages[0]
  if (first == null || typeof first !== 'object' || Array.isArray(first)) return false
  const keys = Object.keys(first as object)
  if (keys.length !== 1) return false
  const k = keys[0]
  return k === 'all' || k === 'graph-expr' || k === 'graphs-in'
}

/**
 * Ensure matcher can be used with store.select (stage-0 graph scope).
 * Bare obj-expr cannot traverse without an opened graph id.
 */
export function ensureTraverseMatcherScoped(body: unknown): unknown {
  if (isGraphScopedMatcher(body)) return body
  throw new Error(
    'Matcher context has no graph scope. Open a graph, or open a matcher that starts with all / graph-expr / graphs-in.',
  )
}
