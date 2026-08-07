import { useCallback, useMemo, useState } from 'react'
import {
  EMPTY_MUTATION,
  applyMutationDocument,
  buildMutationDocument,
  clearPendingDeletes,
  cloneDocument,
  deleteEdgeFromDraft,
  deleteEntityFromDraft,
  draftFromSubgraph,
  emptyDraftState,
  EMPTY_GRAPH,
  excludeEdgeFromDraft,
  excludeEntityFromDraft,
  mergeEdgesIntoDraft,
  mergeEntitiesIntoDraft,
  normalizeGraphMutation,
  replaceDocument,
  undoDeleteEdge,
  undoDeleteEntity,
  undoEdgeModifications,
  undoEntityModifications,
  visualDocument,
  type GraphDraftDocument,
  type GraphDraftState,
  type GraphMutationDocument,
} from './graphDraft'
import type { BoMEdge, BoMEntity, BoMSubgraph } from './types'

export function useGraphDraft(initial: GraphDraftDocument = EMPTY_GRAPH) {
  const [state, setState] = useState<GraphDraftState>(() => emptyDraftState(initial))
  const [rollbackDocument, setRollbackDocument] = useState<GraphDraftDocument>(() =>
    cloneDocument(initial),
  )

  const pendingDeleteCount =
    state.pendingDeleteEntityIds.size + state.pendingDeleteEdgeIds.size

  const canvasDocument = useMemo(() => visualDocument(state), [state])

  const mutationBody = useMemo(() => buildMutationDocument(state), [state])

  const setDocument = useCallback((document: GraphDraftDocument) => {
    setState((prev) => replaceDocument(prev, document))
  }, [])

  /** Apply Text mutation envelope onto baseline (does not wipe unchanged loaded items). */
  const applyParsedMutation = useCallback((value: unknown): string | null => {
    const normalized = normalizeGraphMutation(value)
    if (!normalized) {
      return 'Mutation must contain entities and edges arrays'
    }
    setState((prev) => applyMutationDocument(prev, normalized))
    return null
  }, [])

  const loadSubgraph = useCallback((subgraph: BoMSubgraph) => {
    const next = draftFromSubgraph(subgraph)
    setState(next)
    setRollbackDocument(cloneDocument(next.document))
  }, [])

  const resetToRollback = useCallback(() => {
    setState((prev) => {
      const next = replaceDocument(
        {
          ...prev,
          softDeletedEntities: new Map(),
          softDeletedEdges: new Map(),
          pendingDeleteEntityIds: new Set(),
          pendingDeleteEdgeIds: new Set(),
        },
        rollbackDocument,
      )
      return next
    })
  }, [rollbackDocument])

  const clearDraft = useCallback(() => {
    const next = emptyDraftState(EMPTY_GRAPH)
    setState(next)
    setRollbackDocument(cloneDocument(EMPTY_GRAPH))
  }, [])

  const upsertEntity = useCallback((entity: BoMEntity) => {
    setState((prev) => {
      if (prev.pendingDeleteEntityIds.has(entity.id)) return prev
      const entities = [...prev.document.entities]
      const idx = entities.findIndex((e) => e.id === entity.id)
      if (idx >= 0) entities[idx] = entity
      else entities.push(entity)
      return replaceDocument(prev, { ...prev.document, entities })
    })
  }, [])

  const upsertEdge = useCallback((edge: BoMEdge) => {
    setState((prev) => {
      if (edge.id && prev.pendingDeleteEdgeIds.has(edge.id)) return prev
      const edges = [...prev.document.edges]
      const idx = edges.findIndex((e) => e.id && edge.id && e.id === edge.id)
      if (idx >= 0) edges[idx] = edge
      else edges.push(edge)
      return replaceDocument(prev, { ...prev.document, edges })
    })
  }, [])

  const removeEntity = useCallback((entityId: string) => {
    setState((prev) => deleteEntityFromDraft(prev, entityId))
  }, [])

  const removeEdge = useCallback((edgeId: string) => {
    setState((prev) => deleteEdgeFromDraft(prev, edgeId))
  }, [])

  const excludeEntity = useCallback((entityId: string) => {
    setState((prev) => excludeEntityFromDraft(prev, entityId))
  }, [])

  const excludeEdge = useCallback((edgeId: string) => {
    setState((prev) => excludeEdgeFromDraft(prev, edgeId))
  }, [])

  const mergeEntities = useCallback((entities: BoMEntity[]) => {
    setState((prev) => mergeEntitiesIntoDraft(prev, entities))
  }, [])

  const mergeEdges = useCallback((edges: BoMEdge[]) => {
    setState((prev) => mergeEdgesIntoDraft(prev, edges))
  }, [])

  const restoreDeletedEntity = useCallback((entityId: string) => {
    setState((prev) => undoDeleteEntity(prev, entityId))
  }, [])

  const restoreDeletedEdge = useCallback((edgeId: string) => {
    setState((prev) => undoDeleteEdge(prev, edgeId))
  }, [])

  const revertEntityChanges = useCallback((entityId: string) => {
    setState((prev) => undoEntityModifications(prev, entityId))
  }, [])

  const revertEdgeChanges = useCallback((edgeId: string) => {
    setState((prev) => undoEdgeModifications(prev, edgeId))
  }, [])

  /** After successful Apply: current live graph becomes the new baseline; mutation clears. */
  const markApplied = useCallback(() => {
    setState((prev) => {
      const cleared = clearPendingDeletes(prev)
      setRollbackDocument(cloneDocument(cleared.document))
      return cleared
    })
  }, [])

  return {
    state,
    document: state.document,
    canvasDocument,
    rollbackDocument,
    pendingDeleteCount,
    mutationBody,
    emptyMutation: EMPTY_MUTATION as GraphMutationDocument,
    setDocument,
    applyParsedMutation,
    loadSubgraph,
    resetToRollback,
    clearDraft,
    upsertEntity,
    upsertEdge,
    removeEntity,
    removeEdge,
    excludeEntity,
    excludeEdge,
    mergeEntities,
    mergeEdges,
    restoreDeletedEntity,
    restoreDeletedEdge,
    revertEntityChanges,
    revertEdgeChanges,
    markApplied,
    setRollbackDocument,
  }
}
