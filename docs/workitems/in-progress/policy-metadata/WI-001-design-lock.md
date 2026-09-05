# WI-001 — Design lock (metadata GAPS)

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  
**Depends on:** WI-000  

## Goal

Close every **`open`** row in [`GAPS.md`](GAPS.md). No Policy/category production code yet.

## Scope

- [x] Lock category identity + lifecycle (`G-P50m`, `G-P51m`, `G-P52m`)
- [x] Lock tags + Policy annotations (`G-P53m`, `G-P54m`)
- [x] Lock list/query + repository split (`G-P55m`, `G-P56m`)
- [x] Lock HTTP + workbench navigate UX (`G-P57m`, `G-P58m`)
- [x] Lock serial-version interaction (`G-P59m`)
- [x] Design note [`docs/design/policy/metadata.md`](../../../design/policy/metadata.md)
- [x] [`GAPS.md`](GAPS.md) — all open rows resolved; Decision log complete
- [x] [`STORY.md`](STORY.md) — locked table

## Out of scope

- api/core implementation (WI-002)
- HTTP / UI (WI-003)

## Acceptance

- [x] Implementer can build WI-002/WI-003 without reopening metadata GAPS
- [x] Boundary vs C-27 suites documented (navigate ≠ configure run)
- [x] No production code required for this WI
