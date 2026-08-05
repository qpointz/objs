# WI-001 — Graph mutate API

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Mutate API  
**Status:** done  
**Depends on:** WI-000

## Goal

Add a transactional graph **mutation** that upserts entities/edges and deletes by id in one request, with dry-run validation of the full mutation.

## Scope

- Domain/API type `BoMGraphMutation` (`upsert` / `delete` nested; delete = ids) — do not overload `BoMGraph`
- `BoMGraphStore.mutate` / `validateMutation`: one TX; validate projected state; explicit edge deletes → entity deletes (cascade incident edges) → upserts
- Extend `PUT /api/v1/objs/graph` and `POST /api/v1/objs/graph/validate` to accept the mutation body (empty delete lists = today’s upsert-only)
- Keep `DELETE /api/v1/objs/graph` as thin shim to mutate; mark deprecated in OpenAPI/docs
- Core + controller tests; update `rest-api.md` / `validation.md` notes for this WI’s API contract

## Out of scope

- Object linter UI (later WIs)
- Removing the DELETE endpoint entirely
- Seed format changes (seeds stay MERGE-only)

## Acceptance

- [x] PUT with only entities/edges still upserts as today
- [x] PUT with delete lists applies upserts + deletes atomically
- [x] Validate dry-runs upserts and deletes without writing
- [x] Entity delete cascades incident edges in the store
- [x] DELETE `/graph` still works via shim
