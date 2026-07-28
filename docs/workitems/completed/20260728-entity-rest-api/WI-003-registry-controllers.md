# WI-003 — `/registry/*` REST controllers

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-001  
**Gaps:** G-R7–G-R12, G-R15, G-R19

## Goal

Expose in-memory schema and edge-rule catalogs under `/api/v1/objs/registry/**`.

## Scope

Implemented `ObjsRegistryController` for types, schemas CRUD, and `/registry/edges` CRUD against in-memory catalog beans. Unit tests via standalone MockMvc (G-R19).

## Acceptance

- [x] All registry routes work against in-memory catalogs
- [x] Upsert replace; missing DELETE → `404`
- [x] Types list is derived from registered schemas (no separate type store)
- [x] Controller unit tests present and green (`:objs-service:test`)
