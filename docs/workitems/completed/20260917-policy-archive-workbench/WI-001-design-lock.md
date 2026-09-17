# WI-001 — Design lock

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-000  

## Cold start

1. [`STORY.md`](STORY.md) § Cold start + Boundary.  
2. [`GAPS.md`](GAPS.md) — resolve every **open** row.  
3. C-33 [`PERSISTENCE-MODEL.md`](../../completed/20260907-policy-results-persistence/PERSISTENCE-MODEL.md) — do not change store shape.  

Docs only — no production code. After this WI, WI-002 can implement list/load without re-debating UX.

## Goal

Close every gap in [`GAPS.md`](GAPS.md). Leave a Decision log a cold-start implementer can trust for WI-002+.

## Scope

- Resolve G-A1…G-A9 (list/load HTTP shape, Archives UX, axis panels, Persist→Open)
- Confirm deferred items stay deferred
- Update STORY boundary / workbench.md pointers if needed

## Acceptance

- [x] All GAPS **resolved** (or explicitly deferred with Decision log)
- [x] WI-002 can start without re-debating list DTO or Archives layout
