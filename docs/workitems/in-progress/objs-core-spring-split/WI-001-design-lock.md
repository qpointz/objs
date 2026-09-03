# WI-001 — Design lock

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design lock  
**Status:** complete  
**Depends on:** WI-000  
**Examples:** **docs** (no Java/Kotlin product code)

## Goal

Close every **open** row in [`GAPS.md`](GAPS.md) and record the persistence pattern for implementers.

## Docs (this WI)

- [x] Resolve G-A1…G-A21 (and G-X* deferred/cancelled) — see [`GAPS.md`](GAPS.md)
- [x] [`docs/design/core/README.md`](../../../design/core/README.md) — module split + dependency graph
- [x] [`docs/design/core/spring-split.md`](../../../design/core/spring-split.md) — imported; status updated to C-25 locks
- [x] [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md) — autoconfigure owns Boot wiring; core owns SQL/DAOs
- [x] DAO ↔ repository mapping table in GAPS
- [x] Internal UoW contract sketched in GAPS / spring-split (hidden from API)
- [x] Tracker: backlog **C-25** (C-24 remains `objs-policy`)

## Out of scope

- Runtime code (WI-002+)
- REST API changes

## Acceptance

- [x] An implementer can build api move, DAOs, and autoconfigure without reopening design GAPS
- [x] Every design GAPS row is `resolved`, `deferred`, or `cancelled` with rationale
