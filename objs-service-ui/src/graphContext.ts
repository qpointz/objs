/** Shared graph context for Explorer / Objects / Query (U-7 WI-002). Composer is unbound. */

export const GRAPH_CONTEXT_STORAGE_KEY = 'objs.ui.graphContext.v1'

export type GraphContextKind = 'empty' | 'graph' | 'matcher'

export type GraphContextSnapshot = {
  kind: GraphContextKind
  graphId: string | null
  /** Pinned deep graph version; null = Latest / HEAD (`G-UX-gver`). Graph mode only. */
  graphVersion: number | null
  /** Annotations from the pinned version row (empty when Latest). */
  graphVersionAnnotations: Record<string, string>
  /** ISO createdAt of the pinned version (null when Latest). */
  graphVersionCreatedAt: string | null
  annotations: Record<string, string>
  /** Matcher body when kind is matcher (JSON-serializable). */
  matcherBody: unknown | null
  /** One-line display for matcher expression. */
  matcherLine: string | null
  nodeCount: number
  edgeCount: number
}

export const EMPTY_GRAPH_CONTEXT: GraphContextSnapshot = {
  kind: 'empty',
  graphId: null,
  graphVersion: null,
  graphVersionAnnotations: {},
  graphVersionCreatedAt: null,
  annotations: {},
  matcherBody: null,
  matcherLine: null,
  nodeCount: 0,
  edgeCount: 0,
}

export function loadGraphContextSnapshot(): GraphContextSnapshot {
  try {
    const raw = window.localStorage.getItem(GRAPH_CONTEXT_STORAGE_KEY)
    if (!raw) return migrateFromLegacyCurrentGraph()
    const parsed = JSON.parse(raw) as Partial<GraphContextSnapshot>
    return normalizeSnapshot(parsed)
  } catch {
    return { ...EMPTY_GRAPH_CONTEXT }
  }
}

export function saveGraphContextSnapshot(snapshot: GraphContextSnapshot) {
  try {
    window.localStorage.setItem(GRAPH_CONTEXT_STORAGE_KEY, JSON.stringify(snapshot))
  } catch {
    // ignore quota / private mode
  }
}

function normalizeSnapshot(parsed: Partial<GraphContextSnapshot>): GraphContextSnapshot {
  const kind =
    parsed.kind === 'graph' || parsed.kind === 'matcher' || parsed.kind === 'empty'
      ? parsed.kind
      : 'empty'
  const graphVersion =
    kind === 'graph' &&
    typeof parsed.graphVersion === 'number' &&
    Number.isFinite(parsed.graphVersion)
      ? Math.trunc(parsed.graphVersion)
      : null
  const graphVersionAnnotations =
    kind === 'graph' &&
    graphVersion != null &&
    parsed.graphVersionAnnotations != null &&
    typeof parsed.graphVersionAnnotations === 'object'
      ? Object.fromEntries(
          Object.entries(parsed.graphVersionAnnotations).filter(
            ([k, v]) => typeof k === 'string' && typeof v === 'string',
          ),
        )
      : {}
  const graphVersionCreatedAt =
    kind === 'graph' &&
    graphVersion != null &&
    typeof parsed.graphVersionCreatedAt === 'string' &&
    parsed.graphVersionCreatedAt.length > 0
      ? parsed.graphVersionCreatedAt
      : null
  return {
    kind,
    graphId: typeof parsed.graphId === 'string' && parsed.graphId.length > 0 ? parsed.graphId : null,
    graphVersion,
    graphVersionAnnotations,
    graphVersionCreatedAt,
    annotations:
      parsed.annotations != null && typeof parsed.annotations === 'object'
        ? Object.fromEntries(
            Object.entries(parsed.annotations).filter(
              ([k, v]) => typeof k === 'string' && typeof v === 'string',
            ),
          )
        : {},
    matcherBody: parsed.matcherBody ?? null,
    matcherLine: typeof parsed.matcherLine === 'string' ? parsed.matcherLine : null,
    nodeCount: Number.isFinite(parsed.nodeCount) ? Math.max(0, Number(parsed.nodeCount)) : 0,
    edgeCount: Number.isFinite(parsed.edgeCount) ? Math.max(0, Number(parsed.edgeCount)) : 0,
  }
}

/** One-shot: seed graph mode from Composer-era `objs.ui.currentGraphId` if present. */
function migrateFromLegacyCurrentGraph(): GraphContextSnapshot {
  try {
    const legacy = window.localStorage.getItem('objs.ui.currentGraphId')
    if (legacy && legacy.length > 0) {
      const seeded: GraphContextSnapshot = {
        ...EMPTY_GRAPH_CONTEXT,
        kind: 'graph',
        graphId: legacy,
      }
      saveGraphContextSnapshot(seeded)
      return seeded
    }
  } catch {
    // ignore
  }
  return { ...EMPTY_GRAPH_CONTEXT }
}

export function shortId(id: string, max = 12): string {
  return id.length > max ? `${id.slice(0, 8)}…` : id
}
