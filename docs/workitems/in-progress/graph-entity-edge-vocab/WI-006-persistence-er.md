# WI-006 — Persistence ER diagrams

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 7 — ER diagrams  
**Status:** done  
**Depends on:** WI-005  
**Examples:** **docs**

## Goal

Update persistence **mermaid ER** so freeze entity pins read `objs_graph_version_entity`. Edge table names stay as today.

## Deliverables

- [x] [`docs/design/graph/database-model.md`](../../../design/graph/database-model.md) — `erDiagram`, HEAD/history/pins tables, index list, V9 migration row
- [x] Relationship labels (`deep_entities`)
- [x] Story acceptance checkboxes in [`STORY.md`](STORY.md)

## Acceptance

- [x] Diagrams show `objs_graph_version_entity` (no `objs_graph_version_member`)
- [x] Edge history/pin names still `objs_graph_edge_version` / `objs_graph_version_edge`
- [x] Ripgrep clean for `objs_graph_version_member` in `docs/design/graph/` (except historical V5 filename)
- [x] Story acceptance list checked (implementation complete; **do not** archive until user asks)

## Out of scope

- Freeze-scoped edge redesign diagrams (C-41)
- Story closure / `completed/` move / BACKLOG `done` (user must ask)
