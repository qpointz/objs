import type { GraphContextSnapshot } from './graphContext'
import { MATCH_ALL_OBJ_EXPR } from './queryGraphContext'

export type ObjectsSearchScope = {
  graphId: string | null
  graphVersion: number | null
  body: unknown
}

/**
 * Scope an Objects `obj-expr` (or obj-expr-only body) inside the shared graph context.
 * Graph mode → query that graph (optionally a pinned version). Matcher mode → chain context matcher then the Objects body.
 * Blank / missing filter → match-all within scope.
 */
export function scopeObjectsSearch(
  objExprBody: unknown | null | undefined,
  context: GraphContextSnapshot,
): ObjectsSearchScope {
  const filter =
    objExprBody == null || isBlankObjExpr(objExprBody) ? MATCH_ALL_OBJ_EXPR : objExprBody

  if (context.kind === 'graph' && context.graphId) {
    return {
      graphId: context.graphId,
      graphVersion: context.graphVersion,
      body: filter,
    }
  }
  if (context.kind === 'matcher' && context.matcherBody != null) {
    if (isBlankObjExpr(filter) || sameMatchAll(filter)) {
      return { graphId: null, graphVersion: null, body: context.matcherBody }
    }
    const head = Array.isArray(context.matcherBody)
      ? context.matcherBody
      : [context.matcherBody]
    const tail = Array.isArray(filter) ? filter : [filter]
    return { graphId: null, graphVersion: null, body: [...head, ...tail] }
  }
  throw new Error('Open a graph or matcher as graph context before searching Objects')
}

function sameMatchAll(body: unknown): boolean {
  if (body == null || typeof body !== 'object' || Array.isArray(body)) return false
  return (body as Record<string, unknown>)['obj-expr'] === 'true'
}

function isBlankObjExpr(body: unknown): boolean {
  if (body == null) return true
  if (typeof body !== 'object' || Array.isArray(body)) return false
  const expr = (body as Record<string, unknown>)['obj-expr']
  return typeof expr === 'string' && expr.trim().length === 0
}
