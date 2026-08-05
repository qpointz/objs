# WI-009 — Design docs and benchmarks

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 9 — Docs  
**Status:** done  
**Depends on:** WI-005, WI-006, WI-007, WI-008

## Goal

Rewrite design docs to the source/filter execution model only; record lazy JSON + JSONB/GIN decisions and updated **backend** load benchmarks. State that API pagination/caps are out of this story (compensating follow-up).

## Scope

- Update [`docs/design/graph/annotations-and-subgraphs.md`](../../../design/graph/annotations-and-subgraphs.md): source vs filter, chain + fallback; remove pushable/non-pushable as the primary model.
- Update [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md): JDBC source plan, bound edges, lazy/deferred JSON, JSONB + GIN, H2 dropped for graph query; benchmark table retained with measurement date note.
- Note explicitly: matcher DSL unchanged; execution plan changed; response-shape / pagination not in C-8.

## Out of scope

- Public docs site overhaul beyond what persistence/annotations pages require
- Specifying or implementing API pagination / result caps / sparse projection
- New product features

## Acceptance

- [x] Design docs match implemented execution model
- [x] No normative pushable/non-pushable guidance left as current architecture
- [x] Benchmark section updated or marked with measurement date/method
- [x] Docs note API response-shape work is a later compensating concern, not this story
