# WI-003 — REST PATCH/PUT + validate

**Status:** done  
**Examples:** —  
**Depends on:** WI-002  
**Parallel with:** WI-004 (either order; story tracker does 004 first for driver dogfood)

## Goal

Verb-only break on the kind-first body (already migrated in WI-008; PUT was still MERGE until this WI).

| Verb | Path | Mode |
|------|------|------|
| `PATCH` | `/api/v1/objs/graphs/{id}` | MERGE |
| `PUT` | `/api/v1/objs/graphs/{id}` | REPLACE |
| `PATCH` | `/api/v1/objs/graphs/{id}/validate` | MERGE dry-run |
| `PUT` | `/api/v1/objs/graphs/{id}/validate` | REPLACE dry-run |
| `POST` | `/api/v1/objs/graphs/{id}/validate` | MERGE alias (prefer PATCH) |

Controller sets `mutation.mode` from the verb (omit body mode or must agree). OpenAPI documents
verb = semantic. Unscoped `POST /graph/validate` stays MERGE-only (pool).

## Acceptance

- [x] PATCH = MERGE (breaking vs today’s PUT-as-merge)
- [x] PUT = REPLACE (+ reject non-empty `unset`)
- [x] Matching validate verbs
- [x] OpenAPI + MockMvc coverage
