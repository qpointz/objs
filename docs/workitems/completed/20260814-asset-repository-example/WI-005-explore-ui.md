# WI-005 — Domain UI: explore, search, create/edit

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  

## Goal

Ship the domain SPA: browse/search collections and objects; create/edit via dynamic forms; link to foundation workbench for schemas.

## Deliverables

- [x] Collections list with filters (name / owner / accepted type) — left pane + incremental search  
- [x] Collection detail: metadata + object list (grid or raw JSON)  
- [x] **Search** on collection detail (`obj-expr` query bar → domain search API / G-P7)  
- [x] Object detail: Visual / JSON / YAML (same as edit) + related-object links (in-collection edges)  
- [x] Create / edit object via **dynamic forms** (accepted types only) — schema-driven fields from domain schema API, with JSON/YAML raw toggle  
- [x] Collection metadata create/edit form (G-P2 fields + accepted types from schema catalog + `object_write_mode`; edit is a separate view)  
- [x] Chrome link to **`/workbench/`** for schema management (sidecar; domain UI does **not** call `/api/v1/objs/**`)  
- [x] Packaged into `:asset-repository-service` static UI path  
- [x] Product copy: collection / object / owner — not entity / edge / graph  
- [x] Domain SPA talks only to **`/api/v1/asset-repository/**`** (including schema reads for forms)  

## Out of scope

- Schema authoring inside domain SPA  
- Composer / Gremlin product UX  
- Auth  
- Cross-collection search  

## Acceptance

- Journey 3 works end-to-end (explore, search, create/edit) against the running service  
- Schemas reachable via workbench link  
