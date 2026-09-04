# WI-001 — Design lock (remaining GAPS)

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  
**Depends on:** WI-000  

## Goal

Close every **`open`** row in [`GAPS.md`](GAPS.md). No Policy HTTP or UI code yet.

## Scope

- [x] Lock evaluate (+ CRUD + check) transport (`G-P23a`)
- [x] Lock engine visibility (`G-P23e`) — UI only; DROOLS-only story
- [x] Lock auth / capability enablement (`G-P23f`)
- [x] Lock Add create UX — blank DROOLS policy then edit (no modal)
- [x] Lock editor Save — explicit Save; Check/Evaluate use editor buffer
- [x] Design note [`docs/design/policy/workbench.md`](../../../design/policy/workbench.md)
- [x] [`GAPS.md`](GAPS.md) — all story gaps resolved; Decision log complete
- [x] [`STORY.md`](STORY.md) — locked table updated

## Out of scope

- HTTP module / wiring (`WI-002`)
- Workbench UI (`WI-003`)
- Living docs polish after ship (`WI-004`)

## Acceptance

- [x] Implementer can build WI-002/WI-003 without reopening GAPS
- [x] No production code required for this WI
