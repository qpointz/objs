# WI-005 — Workbench graph-centric UI

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Workbench  
**Status:** done  
**Depends on:** WI-004 (**stage 2 confirmed**)  
**Modules:** `objs-service/ui`

## Goal

Explorer / Composer / Query always use a **current graph**. Matcher UI = **`graph-expr` / `obj-expr` / chained** only. Schema stays catalog-global.

## Today → target (summary)

| Today | Target |
|-------|--------|
| Whole-store `queryGraph` / `putGraphMutation` | Graph-scoped APIs |
| Open packs / Save ▾ Subgraph | Open graph / Save graph / optional Clone |
| Many matcher modes | Three modes only |
| No graph in chrome | Graph picker + empty CTA |

Key files: `GraphExplorerPage.tsx`, `ObjectLinterPage.tsx`, `QueryPage.tsx`, `MatcherQueryForm.tsx`, `CreateSubgraphModal.tsx`, `SubgraphPacksModal.tsx`, `api.ts`, `types.ts`, `AppLayout.tsx`.

## Checklist

### Shell

- [x] Current-graph control on Explorer, Composer, Query
- [x] Persist `objs.ui.currentGraphId` (or similar)
- [x] Block Exec/Save without a graph

### Graph lifecycle

- [x] Open / New / Save graph via `/graphs`
- [x] Optional Clone (no hierarchy/tree UI)
- [x] Retire pack / “Subgraph” primary chrome

### Matchers

- [x] Modes: `graph-expr`, `obj-expr`, chained only
- [x] Help text matches STORY bindings

### Explorer / Composer / Query

- [x] Exec/query scoped to current graph (or `graph-expr` then filter)
- [x] Create entity → pool + attach; edges graph-local
- [x] Validate/Apply graph-scoped

### Client

- [x] Rename SoftLink helpers → graphs (component file names `SubgraphPacksModal.tsx` /
      `CreateSubgraphModal.tsx` kept as-is; full rename deferred to WI-008)

## Out of scope

- Snapshot hierarchy UI
- Rich orphan browser
- Object versioning UX
- Schema redesign

## Stage gate

UI tests as applicable → **STORY § Stage 3 — Manual test** → **STOP**.  
**Do not start WI-006** until `stage 3 confirmed`.

## Acceptance

- [x] No whole-store-as-graph primary UX
- [x] Three matcher modes only
- [x] Open/Save against `/graphs`
- [x] STORY `[x]`; commit; (push pending manual confirmation)

## Commit message hint

`[feat] Workbench graph context and minimal matchers (WI-005)`
