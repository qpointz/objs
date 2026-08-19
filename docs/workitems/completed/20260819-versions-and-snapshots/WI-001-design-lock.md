# WI-001 — Design lock

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design  
**Status:** done  
**Depends on:** WI-000  
**Examples:** **docs**

## Goal

Fold [`ER.md`](ER.md) into living design. Lock: versioning **strategy SPI** (`BomVersioningStrategy`); C-18 default `ExplicitOnly` (persist = today); `createDeepGraphVersion` always captures; `head_version` nullable; write-context graph golden for future policies.

## Docs

- [x] [`ER.md`](ER.md) — already the target; no second model
- [x] [`model.md`](../../../design/graph/model.md)
- [x] [`persistence.md`](../../../design/graph/persistence.md)
- [x] [`apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md) — clocks **C-18**; freeze = deep graph version (Snapshot); `clone()` **kept** (deep copy, new ids, empty history)
- [x] [`docs/design/sbom/example.md`](../../../design/sbom/example.md) — fingerprint = `(graph_id, graph_version)`
- [x] [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md) + [`docs/design/ui.md`](../../../design/ui.md)
- [x] [`SEQUENCE.md`](../../SEQUENCE.md), [`BACKLOG.md`](../../BACKLOG.md) C-18/C-19 blurbs
- [x] C-19 [`STORY.md`](../../planned/foundation-after-versions/STORY.md) / [`WI-002-timestamps.md`](../../planned/foundation-after-versions/WI-002-timestamps.md): clocks **superseded by C-18 WI-002/WI-003**
- [x] [`GAPS.md`](GAPS.md) — REST freeze path locked `POST /graphs/{id}/versions`

## Acceptance

- Living docs match ER: live GET = HEAD tables; deep freeze = pin children; DIY `*_version` = at your own risk (H2 demo).
- Snapshot ≠ `clone()`: freeze stays on the same `graph_id`; clone stays a new-id deep copy with an empty history line.
