# Cleanup inventory — graphs-from-objects (pre–WI-008)

**Story:** [`STORY.md`](STORY.md)  
**Normative WI:** [`WI-008-cleanup.md`](WI-008-cleanup.md)  
**Status:** executed (WI-008) — keep as post-cleanup reference / ripgrep gate  
**Date:** 2026-08-11 (executed after Stage 4 + `all` matcher + Flyway V1 squash)

This document lists **concrete leftovers** found by codebase exploration after Stages 1–4.
Prefer **delete** over permanent shims. Keep behaviour; rename for product vocabulary (**graph**, not pack/subgraph).

## Already gone (do not re-hunt)

| Item | Notes |
|------|--------|
| Tables `bom_subgraph*`, `bom_subgraph_edges` | Squashed into `V1__bom_schema`; final names only |
| REST `/api/v1/objs/graph/subgraphs/**` | Removed in WI-004 |
| Unscoped `PUT /graph`, `POST /graph/query`, `DELETE /graph` | Removed in WI-004 |
| SoftLink* API client helpers | Replaced by `/graphs` helpers in WI-005 |
| Primary UI pack chrome / old matcher modes | Replaced in WI-005 (`all` / `graph-expr` / `obj-expr` / chained) |
| SBOM whole-pool `selectSubgraphMatchAll` | WI-006 uses graphs + `graph-expr` |
| Flyway V2–V6 intermediate DDL | Squashed to `V1__bom_schema` |

## Priority A — rename / delete (production code)

### A1. Domain & persistence Kotlin names (G-G12)

| Current | Proposed | Files (entry points) |
|---------|----------|----------------------|
| `BoMSubgraphStore` | `BoMGraphHeaderStore` or `BoMNamedGraphStore` | `objs-core/.../BoMSubgraphStore.kt` |
| `BoMSubgraphRecord` | `BoMGraphRecord` | `BoMSubgraphRecord.kt` |
| `BoMSubgraphEntityRecord` / `BoMSubgraphEntityId` | `BoMGraphMembershipRecord` / `…Id` | same |
| `BoMSubgraphRepository` / `BoMSubgraphEntityRepository` | `BoMGraphRepository` / `BoMGraphEntityRepository` | `BoMSubgraphRepositories.kt` |
| `BoMSubgraphSpec` | `BoMGraphSpec` | `BoMSubgraphModels.kt` |
| `BoMSubgraphListItem` | `BoMGraphListItem` | same |
| `BoMResolvedSubgraph` | `BoMResolvedGraph` | same; field `subgraph` → `members` or `contents` |
| `BoMSubgraphException` | `BoMGraphException` | same |
| Error codes `SUBGRAPH_*` | `GRAPH_*` (keep `GRAPH_NOT_FOUND` already used) | store + `ObjsGraphsController` handler |
| `snapshot(...)` | Keep as private impl behind `clone`, or rename call sites to `clone` only | `BoMSubgraphStore` |

**Keep with care:** `BoMSubgraph` data class (`entities` + `edges`) is the **selection / resolve payload**, not a pack. Options:

1. Rename → `BoMGraphContents` / `BoMGraphSlice` (clearest product language), or  
2. Keep name if churn on gremlin (`result.subgraph`) is too costly — then document “selection result, not pack”.

Cascade: SBOM (`SbomService`), seed handler, REST DTOs, UI `types.ts`, gremlin projector field names if renamed.

### A2. Dead / legacy matcher & selection stack

| Item | Action | Notes |
|------|--------|-------|
| `BoMAnnoExprMatcher` / `BoMAnnoExprLowerer` + tests | **Delete** if only used by retired DSL / hierarchy tests | Engine may stay if `obj-expr` / `graph-expr` still compile through `BoMAnnoExprEngine` |
| `BoMAnnotationMatcher` / `MatchAllAnnotationMatcher` | **Delete or shrink** after removing selectors/shims | Still used by `BoMSubgraphSelector`, hierarchy/plan tests, deprecated store overloads |
| `BoMAnnotationMatcherAdapter` / `asAnnotationMatcher()` | **Delete** with above | `BoMMatcher.kt` |
| `BoMSubgraphSelector` (+ package `…subgraph`) + `BoMSubgraphSelectorTest` | **Delete** | In-memory pack-era selector; superseded by store `selectInGraph` / `selectAcrossGraphs` |
| `BoMGraphStore.selectSubgraph(BoMAnnotationMatcher)` | **Delete** | `@Deprecated`; always fail-closed |
| `BoMGraphStore.selectSubgraphMatchAll` | **Delete** | No production callers after WI-006 |
| `RetiredMatcherKeyHandler` + five retired keys | **Keep** (clear migrate errors) **or** drop and rely on `MATCHER_DSL_UNKNOWN_KEY` | Prefer **keep** until one release after story merge; then optional drop |
| Method name `selectSubgraph(matcher)` | Rename → `select` / `selectAcrossGraphs` (public) | Aligns with graph vocabulary |

### A3. Pool-wide reader leftovers

| Item | Action | Notes |
|------|--------|-------|
| `BoMRawGraphReader` | Audit: **delete** if unwired; else document as pool JDBC helper for future `obj-expr` pushdown | WI-003 left it unwired from `BoMGraphStore` |
| `BoMGraphStore.loadAll()` | Keep for tests/admin **or** replace tests with `listEntities` + scoped queries | Still widely used in `BoMGraphStoreTest` / Postgres IT — not pack-era, but “whole pool as graph” smell |

### A4. REST surface polish

| Item | Action |
|------|--------|
| `ObjsGraphController` (import/export/validate under `/graph`) | Keep paths for now; clarify OpenAPI tag/docs as **I/O**, not “the graph”. Optional later: move export under `/graphs/{id}/export` |
| Exception map still accepts `SUBGRAPH_NOT_FOUND` | Drop after A1 code rename |
| OpenAPI / examples mentioning retired keys | Grep and scrub |

### A5. UI filenames & symbols (cosmetic but normative for G-G10)

| Current | Proposed |
|---------|----------|
| `SubgraphPacksModal.tsx` | `OpenGraphModal.tsx` (or `GraphPickerModal.tsx`) |
| `CreateSubgraphModal.tsx` | `NewGraphModal.tsx` / `CloneGraphModal.tsx` |
| `draftFromSubgraph` / `loadSubgraph` | `draftFromGraphContents` / `loadGraphContents` |
| `subgraphFromGraphView` | `graphContentsFromGraphView` |
| TS type `BoMSubgraph` | Match Kotlin rename (A1) |
| Nav state `subgraph` handoff field | `graph` / `contents` |
| Gremlin UI labels `subgraph1Stats` / `subgraph2Stats` | Optional rename to `selectionStats` / `resultStats` (API contract — coordinate with gremlin module) |

Primary chrome already says Open/New graph; this is **identifier cleanup**.

## Priority B — tests rename / rewrite

| Test / suite | Action |
|--------------|--------|
| `BoMSubgraphStoreTest` | Rename → `BoMGraphStoreMembershipTest` (or similar); update type names |
| `BoMSubgraphMatcherSelectTest` | Rename → `BoMGraphMatcherSelectTest` |
| `BoMSubgraphPersistenceTest` | Rename / fold into store tests |
| `BoMSubgraphSelectorTest` | **Delete** with selector |
| `BoMAnnoExprMatcherTest` / `BoMAnnoExprLowererTest` | **Delete** with classes, or keep only if engine still needs them |
| `BoMMatcherHierarchyTest` / plan tests using `MatchAllAnnotationMatcher` | Rewrite to `obj-expr` / `graph-expr` / `all` |
| `ObjsGraphsControllerTest` still throwing `SUBGRAPH_*` | Update to `GRAPH_*` |
| Retired-key DSL tests | Keep while handlers exist |

## Priority C — docs & story trackers

| Item | Action |
|------|--------|
| `docs/design/graph/annotations-and-matchers.md` | Rename file → e.g. `annotations-and-matchers.md`; fix inbound links (`model.md`, `persistence.md`, `rest-api.md`, `gremlin.md`, `sbom/example.md`, …) |
| `docs/design/graph/README.md` table row | Update title |
| `docs/design/sbom/example.md` | Still mentions soft-link packs in related links (~L258) — scrub |
| `docs/design/ui.md` / `rest-api.md` | Add `all` where still saying “three forms only” if missed |
| `GAPS.md` G-G10 | Marked **resolved** prematurely while WI-008 pending — set **open** / **in progress** when cleanup starts; **resolved** when done |
| `GAPS.md` G-G12 | Close when A1 done |
| `GAPS.md` G-G15 | Update to include **`all`** |
| `STORY.md` matcher tables | Add `all`; cleanup checklist already lists SoftLink / old keys |
| Completed C-12 story docs | Leave as historical; do not rewrite |

## Priority D — out of scope / do not delete

| Item | Why |
|------|-----|
| Word “subgraph” in gremlin **result** projection (`subgraph` / `views.graph`) | API wire shape; rename only with explicit gremlin contract change |
| `BoMAnnoExprEngine` (if still shared by `obj-expr` / `graph-expr`) | Not pack-era; keep |
| Flyway `V1__bom_schema` | Canonical |
| Retired DSL key handlers (short term) | Better UX than silent unknown-key |
| Snapshot hierarchy / `parent_graph_id` | Explicitly **not** foundation (G-G5a) |
| Object versioning | Out of story |

## Suggested WI-008 work order

1. **Delete** dead selector + deprecated store overloads + unused anno matcher types (A2/A3) — low risk, shrinks tree.  
2. **Rename** domain/persistence `BoMSubgraph*` → `BoMGraph*` and error codes (A1) — mechanical, wide.  
3. **Rename** UI files/symbols (A5).  
4. **Docs** file rename + scrub (C).  
5. **Ripgrep gate** (below) + full test suite + Stage 5 manual checklist.  
6. Mark WI-008 / G-G10 / G-G12 done; commit; push.

## Ripgrep acceptance gate (WI-008)

Run from repo root after cleanup; **production** sources should be clean (tests may still mention retired keys in negative cases):

```text
# Must be empty (or only comments / migrate docs / completed stories):
bom_subgraph[^_]          # old table prefix leftovers
SoftLink
listSoftLink|createSoftLink|getSoftLink
/graph/subgraphs
selectSubgraphMatchAll
BoMSubgraphSelector
MatchAllAnnotationMatcher   # after A2 delete
BoMAnnoExprMatcher          # after A2 delete (engine may remain)
SUBGRAPH_ID_CONFLICT|SUBGRAPH_ENTITY_MISSING|SUBGRAPH_EDGE_
Open packs|subgraph pack
```

Allowed survivors (document in commit notes if kept):

- `RetiredMatcherKeyHandler` + test strings for retired DSL keys  
- Historical `docs/workitems/completed/**`  
- Optional: `BoMSubgraph` / gremlin `$.subgraph` if choice (2) in A1  

## Automated verification

```bash
./gradlew :objs-core:test :objs-service:test :objs-sbom-example:test -q
# UI: cd objs-service/ui && npm test
# Optional: :objs-gremlin-core:test :objs-gremlin-service:test if A1 touches result types
```

## Manual (STORY § Stage 5)

```text
[ ] No production references to bom_subgraph* / SoftLink / anno|ids|subg-expr|subgraph DSL / global-graph Save
[ ] Full test suite green
[ ] Smoke Open/Save/query with all + graph-expr + obj-expr still works
```
