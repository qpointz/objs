# WI-003 — Reverse lookup of pins

**Story:** [`STORY.md`](STORY.md)  
**Depends on:** WI-001  
**Status:** complete  
**Examples:** **SBOM + AR** (+ workbench if useful)

Extend `listGraphIdsForEntity` (C-17) so callers can see live membership **and** snapshot graphs that pin a version of that identity.

## Core

- [x] `BoMGraphVersionMemberRepository.findDistinctGraphIdsByEntityId`
- [x] Flyway V5 index on `bom_graph_version_member(entity_id)` (postgresql + h2)
- [x] `listGraphIdsForEntity` unions live + pin graph ids
- [x] Test: freeze + detach still returns graph id

## SBOM

- [x] `ApplicationVersionServiceTest.shouldReverseLookupPinnedGraphAfterLiveDetach`

## Docs (same commit)

- [x] KDoc on `listGraphIdsForEntity`

## Acceptance

- `./gradlew :objs-core:test :sbom-service:test`
