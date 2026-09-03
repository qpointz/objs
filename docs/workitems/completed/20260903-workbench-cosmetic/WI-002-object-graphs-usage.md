# WI-002 — Object detail Graphs usage

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Object Graphs section  
**Status:** done  
**Depends on:** WI-001  
**Examples:** **workbench**  
**Gap:** [`G-WC-objgraphs`](GAPS.md) (resolved)

## Goal

In the entity object detail viewer, show live (HEAD) graphs that contain the object — Versions-style preview (5) + expandable browser with Open-graph-style incremental search. Ignore graph version pins.

## Deliverables

- [x] Core: `listLiveGraphHeadersForEntity` (live membership only; sort `updatedAt` desc; optional `q`/`limit`)
- [x] REST: `GET /api/v1/objs/entities/{id}/graphs` → `{ items, total }`
- [x] Workbench: Graphs section in `ObjectViewer` / `ObjectInspectPane` (entity only)
- [x] Click opens shared graph context on Latest
- [x] GAPS / STORY tracker updated; brief `ui.md` inspect note

## Out of scope

- Pin / deep-version membership
- Edges / graph-header inspect
- SBOM / AR usage UI
