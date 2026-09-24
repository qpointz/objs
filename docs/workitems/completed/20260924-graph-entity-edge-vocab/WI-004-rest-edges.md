# WI-004 — REST edge paths

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Edge REST  
**Status:** done  
**Depends on:** WI-003  
**Examples:** **—**

## Goal

Add graph-scoped edge CRUD so REST is symmetric with entity attach/detach (bulk mutate remains available).

## Deliverables

- [x] `POST /api/v1/objs/graphs/{id}/edges` — create (force `graphId` from path)
- [x] `PUT /api/v1/objs/graphs/{id}/edges/{edgeId}` — update per WI-001 lock
- [x] `DELETE /api/v1/objs/graphs/{id}/edges/{edgeId}` — remove (same semantics as mutate unset)
- [x] Implement as thin wrappers over `NamedGraphStore.mutate` MERGE
- [x] MockMvc + OpenAPI on graphs controller

## Acceptance

- [x] Create / update / delete one edge without full-graph REPLACE body
- [x] Fail closed on wrong graph / missing edge / validation (per WI-001)
- [x] `./gradlew :objs-service:test` (ObjsGraphsControllerTest)

## Out of scope

- Top-level `/api/v1/objs/edges` CRUD mirroring `/entities/*` (edges are graph-owned only)
- Top-level `/edges/{id}` history/compact changes (leave as-is)
- Composer UI for single-edge REST (mutate UI stays)
- SQL rename of edge version/pin tables
- Freeze-scoped edge redesign (C-41)
- Living docs / ER (WI-005 / WI-006)
