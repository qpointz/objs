# WI-005 — Design docs

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Docs  
**Status:** done  
**Depends on:** WI-001, WI-002, WI-003, WI-004, WI-006

## Goal

Document `obj-expr`, `ids`, Composer Add objects / exclude, and the shared matcher / visual chain UI in design docs; leave story tracker ready for closure when requested.

## Scope

- [`annotations-and-subgraphs.md`](../../../design/graph/annotations-and-subgraphs.md) — `obj-expr` (bindings, lazy/pushdown), `ids`; note `anno-expr` unchanged
- [`persistence.md`](../../../design/graph/persistence.md) — mention `obj-expr` / `ids` in selection plan notes if needed
- [`ui.md`](../../../design/ui.md) — Composer **Add objects…**, exclude vs delete, edge merge on Done, local page size 20, new `qid` on Search; matcher modes incl. `obj-expr`; **visual chained builder** (per-stage visual editors; JSON for full chain only)
- [`rest-api.md`](../../../design/service/rest-api.md) — matcher keys `obj-expr`, `ids`
- STORY tracker: all WIs `[x]`; GAPS only **resolved** or **deferred** (G-S18)

## Out of scope

- Story archive / MILESTONE completed move (explicit closure only)
- Field-builder follow-up design (optional one-liner under Open)

## Acceptance

- [x] Design docs match shipped behavior and locked GAPS
- [x] All implementation WIs `[x]` in STORY.md
- [x] No unresolved **open** rows in GAPS (deferred G-S18 OK)
