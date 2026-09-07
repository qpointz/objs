# WI-001 — Design lock

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-000  

## Goal

Close every gap in [`GAPS.md`](GAPS.md) and leave a Decision log a cold-start implementer can trust. No production code.

## Cold start

Already finished. **Do not reopen GAPS** unless the user amends the story. Normative locks live in GAPS + [`STORY.md`](STORY.md) § Persist spec. Sketch: [`RESULTS-MODEL.md`](../../completed/20260905-policy-suites/RESULTS-MODEL.md) § Persistence API.

## Scope (completed)

- [x] Persist API: axes, filters, presets, explicit save, labeling, runtime (G-P48r, G-P49r, G-P44r, G-P45r, G-P50r)
- [x] Store: Flyway shape, context≠input, modules, cascade (G-P11r, G-P32r, G-P47r, G-P46r)
- [x] STORY / RESULTS-MODEL / suites.md pointers updated

## Acceptance

- [x] All GAPS **resolved**
- [x] WI-002 can start without re-debating the persist model
