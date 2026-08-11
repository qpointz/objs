# WI-003 — Domain stores: pool, graph, clone, matchers

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design + DB + store  
**Status:** done  
**Depends on:** WI-002  
**Modules:** `:objs-core`

## Goal

Replace global-graph store + soft packs with:

1. **Entity pool** — CRUD/list (orphans OK)
2. **Graph store** — CRUD header; attach/detach; graph-local edges; resolve; list; optional **clone**
3. **Matchers** — only **`graph-expr`**, **`obj-expr`**, **chained**; retire old DSL keys

## Behaviour

| Op | Behaviour |
|----|-----------|
| Pool write | `bom_entity` only |
| Attach / detach | M2M `bom_graph_entity` |
| Create edge | `graph_id` required; both ends members of that graph |
| Resolve | Members + edges for one graph |
| Delete graph | CASCADE membership + edges; entities kept |
| Clone | New graph + cloned entities/edges (new ids); no parent FK |
| `graph-expr` | JEXL `id`, `a` on graph headers → union members + edges |
| `obj-expr` | As today; candidates = active graph membership only |
| chained | Stage-0 often `graph-expr`; later `obj-expr` |
| `obj-expr` alone | **Reject** without graph scope (G-G16) |
| Old keys | `anno` / `anno-expr` / `ids` / `subgraph` / `subg-expr` → decode error |

## Tests

| Case | Expect |
|------|--------|
| Orphan entity | No membership |
| Same entity in two graphs | Two M2M rows |
| Edge without membership | Reject |
| Resolve / delete graph | Per STORY |
| Clone | New ids; source intact |
| `graph-expr` | Correct members/edges |
| chained graph-expr → obj-expr | Filters inside selected graphs |
| `obj-expr` alone | Reject |
| Old DSL keys | Clear error |

## Stage gate

`:objs-core:test` green → hand user **STORY § Stage 1 — Manual test** → **STOP**.  
**Do not start WI-004** until `stage 1 confirmed`.

## Out of scope

- REST, UI, SBOM
- Snapshot hierarchy

## Acceptance

- [x] Pool + graph + clone + three matchers behave as above
- [x] No whole-store-as-graph default select
- [x] STORY `[x]`; commit; push

## Commit message hint

`[feat] Graph store, clone, and graph-expr/obj-expr matchers (WI-003)`

## Implementation notes

- **Matchers:** `BoMSubgraphMatchers.kt` (`BoMSubgExprMatcher` / `BoMSubgraphIdMatcher`) replaced by
  `BoMGraphExprMatcher.kt` — `BoMGraphExprMatcher` (`graph-expr`, JEXL over header `id` + `a`,
  same bindings as the old `subg-expr`) and `BoMGraphExprCompile` (`MATCHER_GRAPH_EXPR_*` codes).
  `subgraph: { id }` has no direct replacement class; express it as `graph-expr: "id == '<uuid>'"`.
  `BoMIdsMatcher` deleted (no DSL key references it anymore).
- **DSL retirement (G-G17):** `BoMMatcherDsl.defaultHandlers()` now registers only `graph-expr` +
  `obj-expr`, plus a `RetiredMatcherKeyHandler` for each of `anno` / `anno-expr` / `ids` /
  `subgraph` / `subg-expr` that fails decode with `MATCHER_DSL_RETIRED_KEY` and a "migrate to …"
  message (clearer than a generic unknown-key error). `encode()` only supports `BoMChainedMatcher`
  / `BoMObjExprMatcher` / `BoMGraphExprMatcher`; other matcher types hit
  `MATCHER_DSL_ENCODE_UNSUPPORTED`. `MatchAllAnnotationMatcher` / `BoMAnnoExprMatcher` /
  `BoMAnnoExprLowerer` / `BoMMatchExpression` classes are kept (unused by the DSL) because they are
  still exercised directly by generic matcher-infrastructure unit tests
  (`BoMEntitySelectionPlanTest`, `BoMEntityColumnProjectionTest`, `BoMMatcherHierarchyTest`,
  `BoMRawEntityCandidateLazyTest`); full deletion is deferred to WI-008. `BoMAnnoExprEngine` is
  kept as-is — `obj-expr` / `graph-expr` compile through it.
- **G-G16 fail-closed:** `BoMGraphStore.selectSubgraph(matcher)` now requires stage-0
  `BoMGraphExprMatcher`; any other first stage (bare `obj-expr`, or a chain starting with
  `obj-expr`) throws `BoMValidationException` with code `MATCHER_GRAPH_SCOPE_REQUIRED`. Added
  `BoMGraphStore.selectInGraph(graphId, matcher)`: resolves one known graph's stored
  members/edges, then applies the matcher's stages as in-memory filters (no whole-pool scan
  possible since the candidate set is already the graph's membership). `rawGraphReader`
  (`BoMRawGraphReader`, whole-`bom_entity`-table scan/pushdown) is no longer wired into
  `BoMGraphStore` — it was the only place a bare matcher could scan the whole pool as if it were a
  graph. The class itself is left in place (still a valid `@Component`, its supporting
  infrastructure — `BoMEntitySelectionPlan`, `BoMObjExprLowerer`, etc. — is still unit-tested
  directly); full removal is a WI-008 cleanup call.
- **Legacy pool-wide overloads:** `BoMGraphStore.selectSubgraph(BoMAnnotationMatcher)` and
  `selectSubgraphMatchAll(filter)` are kept (marked `@Deprecated`) purely so `objs-service`
  (`ObjsGraphController` — `/graph/query`, `/graph/export`), `objs-sbom-example` (`SbomService`),
  and `objs-gremlin-core` (`BoMGremlinEngine`) keep compiling; they now always throw
  `MATCHER_GRAPH_SCOPE_REQUIRED` via the delegation to `selectSubgraph`. Those unscoped
  whole-store-as-graph endpoints are explicitly removed in WI-004 — no REST/UI behaviour changes
  were made here beyond keeping them compiling, per story scope.
- **Clone:** `BoMSubgraphStore.snapshot` unchanged; added `clone(sourceId, annotations)` as a
  same-behaviour alias (story vocabulary). No parent FK either way.
- **Pool ops:** entity CRUD without membership already worked via `BoMGraphStore`; added
  `BoMGraphStoreTest.shouldAllowOrphanEntity_withNoGraphMembership` to document/lock it in.
- **Tests:** rewrote `BoMMatcherDslTest` (graph-expr/obj-expr/chained decode+encode, retired-key
  migrate errors) and `BoMSubgraphMatcherSelectTest` (graph-expr select/annotation/id, chained
  graph-expr→obj-expr, `selectInGraph` scoping incl. excluding non-member entities matching the
  same predicate, and the `MATCHER_GRAPH_SCOPE_REQUIRED` fail-closed path). Updated
  `BoMGraphStoreTest`, `BoMSubgraphSelectorTest` (anno/anno-expr chain → obj-expr chain),
  `BoMSubgraphStoreTest` (added clone-alias + same-entity-in-two-graphs tests), and
  `BoMGraphStorePostgresIT` (testIT; dropped the now-fail-closed pool-wide assertions) for the
  retired keys / new store surface. `:objs-core:test` green: 136/136.
