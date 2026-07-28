# WI-001 — Core graph + catalog helpers

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** —  
**Gaps:** G-R3, G-R4, G-R5, G-R7, G-R9, G-R12

## Goal

Extend `objs-core` so the REST layer can implement `/graph` and `/registry` without ad-hoc scans or missing catalog mutators.

## Scope

- `BoMGraphStore.write`: ensure caller can obtain the graph **after** id assignment for PUT response (G-R5); adjust return type or documented side-effect as needed
- Transactional **batch delete** by `entityIds` / `edgeIds` (G-R3, G-R4); reuse existing single delete semantics (entity → incident edges)
- `BoMSchemaCatalog`: `remove(type, version)`, `listByType(type)` (or equivalent); keep `all()` / `get` / `register`
- `BoMAllowedEdgeCatalog`: `remove(sourceType, role, targetType)`; keep `all()` / `register` / `find`
- Unit tests for new helpers
- Catalogs already Spring beans via `ObjsCoreAutoConfiguration` — no HTTP in this WI

## Out of scope

- REST controllers
- PostgreSQL catalog persistence (C-3)
- Exposing `loadAll` as a new public “API contract” for HTTP

## Acceptance

- [x] PUT-oriented write path yields graph with all entity/edge ids set when valid
- [x] Batch delete is all-or-nothing; unknown id fails the batch
- [x] Schema and edge-rule catalogs support remove (+ list schemas by type)
- [x] Tests cover success and not-found / validation failure paths for helpers

## Notes

- `write` mutates the input `BoMGraph` in place with assigned ids on success (documented on the method).
- `delete(entityIds, edgeIds)` validates all ids first, then deletes edges then entities (entity delete still cascades incident edges).
- Catalog: `listByType`, `types()`, `remove` on schemas; `remove` on edge rules.
