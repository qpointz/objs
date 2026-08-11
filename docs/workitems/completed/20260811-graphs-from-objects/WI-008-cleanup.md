# WI-008 — Aggressive cleanup

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Cleanup  
**Status:** done  
**Depends on:** WI-007 (**stage 4 confirmed**)  
**Modules:** all touched modules

## Goal

Delete stale global-graph / pack / old-matcher leftovers. Prefer delete over permanent shims.

## Inventory

**[`CLEANUP.md`](CLEANUP.md)** — executed per priorities A–C.

## Done

| Area | Result |
|------|--------|
| Persistence / domain | `BoMNamedGraphStore`, `BoMGraphRecord` / membership, `BoMGraphContents`, `BoMGraphException`, `GRAPH_*` codes; `clone` public, `snapshot` private |
| Dead stack | Deleted `BoMSubgraphSelector`, `BoMAnnotationMatcher` / `MatchAllAnnotationMatcher`, `BoMAnnoExprMatcher` / Lowerer / `BoMMatchExpression`, `BoMRawGraphReader`, deprecated `selectSubgraph*` overloads |
| Matchers | Kept `BoMAnnoExprEngine` + retired-key handlers; `select(matcher)` public name |
| Gremlin wire | `BoMGremlinResult.contents` (was `subgraph`) |
| UI | `OpenGraphModal` / `NewGraphModal`; `draftFromGraphContents` / `loadGraphContents` / `graphContentsFromGraphView`; nav `graphContents` |
| Docs | `annotations-and-matchers.md`; design + core README scrub; `all` in ui/model |

## Automated

```bash
./gradlew :objs-core:test :objs-service:test :objs-sbom-example:test -q
# ripgrep gates: see CLEANUP.md § Ripgrep acceptance gate
```

## Stage gate

**STORY § Stage 5 — Manual test** → user `stage 5 confirmed` → ready for closure when asked.

## Acceptance

- [x] Stale types/APIs/matcher keys gone (per `CLEANUP.md`)
- [x] Tests green (core/service/sbom + UI vitest); smoke `all` + `graph-expr` + `obj-expr` — Stage 5 manual
- [x] G-G10 / G-G12 done; STORY `[x]`; commit; push

## Commit message hint

`[refactor] Remove stale subgraph/global-graph/matcher leftovers (WI-008)`
