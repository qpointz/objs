# WI-003 — Backdated createDeepGraphVersion

**Story:** [`STORY.md`](STORY.md)  
**Stage:** B — Store: backdated freeze  
**Status:** done  
**Depends on:** WI-001 (may parallel WI-002 after lock)  
**Examples:** per G-O12 lock

## Goal

Extend `createDeepGraphVersion` so callers can supply a past `Instant` for migration of existing product versions into objs deep versions while preserving dates (and version-key chronology if G-O1 locks option B).

## Deliverables

- [x] API overload / options object with optional `at: Instant`
- [x] Default path unchanged when `at` omitted (`Instant.now()`)
- [x] Tests: past stamp on locked row set; reject/allow future per G-O3; ordering with successive freezes

## Out of scope

- Rewriting existing version rows
- Backdating live HEAD clocks without a freeze (G-X5)
