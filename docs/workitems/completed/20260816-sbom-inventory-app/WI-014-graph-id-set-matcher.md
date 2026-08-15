# WI-014 — Graph-id-set matcher (FB-5)

**Story:** [`STORY.md`](STORY.md)  
**Foundation:** [`FOUNDATION-BACKLOG.md`](FOUNDATION-BACKLOG.md) FB-5  
**Gaps:** G-F8  
**Depends on:** WI-003  
**Status:** complete  

## Goal

Add an objs-core matcher that selects an **explicit set of graph ids** so portfolio MI can materialize a union subgraph and run Gremlin (`selectAndEval`) without teaching core about portfolios.

## Deliverables

- [x] Matcher `BoMGraphIdsMatcher` + DSL key **`graphs-in`**  
- [x] `BoMGraphStore.select` / `selectAcrossGraphs` support  
- [x] Optional chain with `obj-expr`  
- [x] Unit / store tests (`BoMMatcherDslTest`, `BoMGraphMatcherSelectTest`)  
- [x] Document in `docs/design/graph/annotations-and-matchers.md`  
- [x] Mark FB-5 **done**  

## Acceptance

- [x] Given a list of version `graph_id`s, select returns the union subgraph  
- [x] Empty `graphs-in` → empty result; unknown ids skipped  
