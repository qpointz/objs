# WI-001 — Design lock (suite GAPS)

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  
**Depends on:** WI-000  

## Goal

Close every **`open`** row in [`GAPS.md`](GAPS.md). No suite production code yet.

## Progress

| Gap | Status |
|-----|--------|
| G-P26s shape | **resolved** |
| G-P27s matchers | **resolved** |
| G-P28s execute scope | **resolved** |
| G-P29s roll-up strategy | **resolved** |
| G-P30s execution / dedupe | **resolved** |
| G-P31s repositories | **resolved** |
| G-P32s versioning / replay intent | **resolved** |
| G-P33 applicability | **resolved** |
| G-P11s result shape | **resolved** |
| G-P15s evaluateSuite entry | **resolved** |

## Artifacts

- [`docs/design/policy/suites.md`](../../../../design/policy/suites.md) — design (promote normative in WI-004 if needed)
- [`GAPS.md`](GAPS.md) — decision log complete
- [`RESULTS-MODEL.md`](RESULTS-MODEL.md) — indicative relational model
- [`STORY.md`](STORY.md) — locked table

## Scope

- [x] Resolve all open suite GAPS
- [x] [`GAPS.md`](GAPS.md) — no open rows; Decision log complete
- [x] [`STORY.md`](STORY.md) — locked table
- [ ] Promote `suites.md` Status from draft → normative (WI-004 ok)

## Out of scope

- api/core implementation (WI-002+)
- Seeds / REST / workbench suite UI

## Acceptance

- [x] Implementer can build WI-002/WI-003 without reopening suite GAPS
- [x] No production code required for this WI
