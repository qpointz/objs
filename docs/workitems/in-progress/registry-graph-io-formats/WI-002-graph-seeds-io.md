# WI-002 — Graph seeds import/export; remove `/seeds/**`

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Graph seeds  
**Status:** done  
**Depends on:** WI-001

## Goal

Move graph seed I/O to `/api/v1/objs/graph/import|export` with `format=seeds`, and remove the old seeds controller.

## Scope

- `POST /api/v1/objs/graph/import?format=seeds` (Graph docs only)
- `GET /api/v1/objs/graph/export?format=seeds` + annotation filter (FILTER_EMPTY when missing)
- Delete `ObjsSeedController` and `/api/v1/objs/seeds/**`
- Migrate / replace `ObjsSeedControllerTest`
- OpenAPI groups: drop standalone `seeds` group (fold into registry/graph)

## Out of scope

- JSON Schema (WI-003)
- UI URL update (WI-004)

## Acceptance

- [ ] Bounded graph export works; unbounded rejected
- [ ] Graph import rejects catalog kinds
- [ ] No `/api/v1/objs/seeds/**` routes remain
