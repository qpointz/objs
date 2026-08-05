# WI-002 — Candidate source + AllEntities fallback

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Source model  
**Status:** done  
**Depends on:** WI-001 (Stage 1 complete + readiness gate)

## Goal

Introduce `BoMCandidateSource` and wire the graph reader so selection is **source then filter**, not a pushable-type special case.

## Scope

- Add `BoMCandidateSource`, `BoMEntityCandidateBackend`, `BoMSourceCapableMatcher`.
- Add `BoMInMemoryAllEntitiesSource` for `BoMSubgraphSelector`.
- JDBC all-entities fallback source on `BoMRawGraphReader`.
- Resolve: source-capable stage 0 → source + remaining filters; else all-entities + all stages as filters.
- Matcher DSL unchanged.

## Out of scope

- Bound edge loading (WI-005)
- Matcher package cleanup / delete pushable types (WI-008)
- API pagination/caps

## Acceptance

- [x] Reader selects via candidate source + filters
- [x] Non-source-capable first stage falls back to AllEntities
- [x] Existing unit/IT selection tests green
