import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import type { GraphLink, GraphNode, GraphSelection } from './types'

const QID = 'qid'
const NODE = 'node'
const EDGE = 'edge'

export function newGraphQueryId(): string {
  return crypto.randomUUID()
}

function buildSearch(qid: string | null, selection: GraphSelection | null): string {
  const params = new URLSearchParams()
  if (qid) params.set(QID, qid)
  if (selection?.kind === 'node') params.set(NODE, selection.node.id)
  if (selection?.kind === 'edge') params.set(EDGE, selection.edge.id)
  const s = params.toString()
  return s ? `?${s}` : ''
}

function searchString(params: URLSearchParams): string {
  const s = params.toString()
  return s ? `?${s}` : ''
}

function resolveSelection(
  params: URLSearchParams,
  nodes: GraphNode[],
  links: GraphLink[],
): GraphSelection | null {
  const nodeId = params.get(NODE)
  if (nodeId) {
    const node = nodes.find((n) => n.id === nodeId)
    return node ? { kind: 'node', node } : null
  }
  const edgeId = params.get(EDGE)
  if (edgeId) {
    const edge = links.find((e) => e.id === edgeId)
    return edge ? { kind: 'edge', edge } : null
  }
  return null
}

function sameSelection(a: GraphSelection | null, b: GraphSelection | null): boolean {
  if (a == null && b == null) return true
  if (a == null || b == null) return false
  if (a.kind === 'node') {
    return b.kind === 'node' && a.node.id === b.node.id
  }
  return b.kind === 'edge' && a.edge.id === b.edge.id
}

type Options = {
  nodes: GraphNode[]
  links: GraphLink[]
  /** Called when history restores a node selection (pan canvas). */
  onFocusNode?: (nodeId: string) => void
  /** Restore qid after session reload (explorer). */
  initialQueryId?: string | null
}

/**
 * Graph selection backed by URL search params + a query id.
 * Back/Forward restores selection only when `qid` matches the current result set.
 */
export function useGraphSelectionHistory({
  nodes,
  links,
  onFocusNode,
  initialQueryId = null,
}: Options) {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [queryId, setQueryId] = useState<string | null>(() => {
    if (initialQueryId) return initialQueryId
    return searchParams.get(QID)
  })
  const [selection, setSelection] = useState<GraphSelection | null>(null)
  const queryIdRef = useRef(queryId)
  queryIdRef.current = queryId
  const selectionRef = useRef(selection)
  selectionRef.current = selection
  const onFocusNodeRef = useRef(onFocusNode)
  onFocusNodeRef.current = onFocusNode
  const skipUrlApplyRef = useRef(false)

  const beginQueryResult = useCallback((): string => {
    const qid = newGraphQueryId()
    setQueryId(qid)
    setSelection(null)
    skipUrlApplyRef.current = true
    navigate({ search: buildSearch(qid, null) }, { replace: true })
    queueMicrotask(() => {
      skipUrlApplyRef.current = false
    })
    return qid
  }, [navigate])

  const clearQuery = useCallback(() => {
    setQueryId(null)
    setSelection(null)
    skipUrlApplyRef.current = true
    navigate({ search: '' }, { replace: true })
    queueMicrotask(() => {
      skipUrlApplyRef.current = false
    })
  }, [navigate])

  const select = useCallback(
    (next: GraphSelection | null) => {
      const qid = queryIdRef.current
      if (!qid) {
        setSelection(next)
        return
      }
      if (sameSelection(selectionRef.current, next)) return
      setSelection(next)
      const search = buildSearch(qid, next)
      if (search === searchString(new URLSearchParams(window.location.search))) return
      navigate({ search }, { replace: false })
    },
    [navigate],
  )

  // Apply URL → selection when params or graph data change.
  useEffect(() => {
    if (skipUrlApplyRef.current) return

    const urlQid = searchParams.get(QID)
    const currentQid = queryIdRef.current

    if (!urlQid || !currentQid || urlQid !== currentQid) {
      if (selectionRef.current != null) {
        setSelection(null)
      }
      return
    }

    const resolved = resolveSelection(searchParams, nodes, links)
    if (sameSelection(selectionRef.current, resolved)) return
    setSelection(resolved)
    if (resolved?.kind === 'node') {
      const id = resolved.node.id
      requestAnimationFrame(() => onFocusNodeRef.current?.(id))
    }
  }, [searchParams, nodes, links])

  // Keep URL qid in sync when we have a result set but URL lacks it (session restore).
  useEffect(() => {
    if (!queryId) return
    if (searchParams.get(QID) === queryId) return
    if (skipUrlApplyRef.current) return
    navigate({ search: buildSearch(queryId, selectionRef.current) }, { replace: true })
  }, [queryId, navigate, searchParams])

  return { queryId, selection, select, beginQueryResult, clearQuery }
}
