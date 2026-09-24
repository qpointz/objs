# WI-005 — Living docs (prose / paths)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 6 — Docs prose  
**Status:** done  
**Depends on:** WI-007  
**Examples:** **docs**

## Goal

Update living design **prose and path tables** to entity/edge vocabulary and new REST paths. Mermaid ER is **WI-006**.

## Deliverables

- [x] [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md) — path table (`/entities`, `/edges`, `apply-structure`)
- [x] [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md) — prose / tables
- [x] [`docs/design/graph/pin-reverse-lookup.md`](../../../design/graph/pin-reverse-lookup.md) — `objs_graph_version_entity`
- [x] programmatic recipes / related mentions
- [x] Note C-41 deferred where relevant (GAPS / backlog already)

## Acceptance

- [x] Prose/path docs match shipped SQL + REST
- [x] Ripgrep clean for `/graphs/{id}/members` and `apply-membership` in `docs/design/`
- [x] `objs_graph_version_member` cleared from prose (ER in WI-006)

## Out of scope

- Mermaid `erDiagram` blocks (WI-006)
- Story closure / archive (user must ask)
