# WI-001 — Design lock (G-P18–20)

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  
**Depends on:** WI-000  

## Goal

Close every **`open`** row in this story’s [`GAPS.md`](GAPS.md). No Drools modules yet.

**Done:** Gap-by-gap confirm 2026-09-04 — G-P18, G-P19, G-P20 `resolved`. See Decision log in [`GAPS.md`](GAPS.md).

## Scope

- [x] Lock Drools fact model — maps-first / optional Map wrapper; typed facts deferred (`G-P18`)
- [x] Lock Drools version & deps — poll BOM; minimize to `drools-bom` + `drools-engine`; isolate from api/core (`G-P19`)
- [x] Lock thread-safety / isolation — per-call session; KB cache by single policy revision (`G-P20`)
- [x] Design page [`docs/design/policy/drools.md`](../../../design/policy/drools.md)
- [x] [`STORY.md`](STORY.md) — normative locked table + tracker
- [x] [`GAPS.md`](GAPS.md) — all rows resolved; Decision log filled

## Out of scope

- Gradle / Kotlin (`WI-002`)
- Living docs polish after ship (`WI-003`)

## Acceptance

- [x] Implementer can build WI-002 without reopening G-P18–20
- [x] No production code required for this WI
