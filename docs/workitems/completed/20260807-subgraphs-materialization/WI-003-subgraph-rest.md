# WI-003 — REST CRUD `/graph/subgraphs`

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — REST + matcher  
**Status:** done  
**Depends on:** WI-002  
**Modules:** `:objs-service` (controller + MockMvc tests); may need DTO mappers only

## Goal

Expose soft-link subgraph CRUD + resolve over HTTP (G-S4). Snapshot endpoint is **WI-007** (can stub 501 only if unavoidable — prefer omit until WI-007).

## Endpoints (normative)

Base: `/api/v1/objs/graph/subgraphs`  
OpenAPI tag: e.g. `subgraphs` (or under existing `graph` tag — pick one and document).

| Method | Path | Request | Response |
|--------|------|---------|----------|
| GET | `/subgraphs` | — | `[{ id, annotations, entityCount, edgeCount }]` |
| POST | `/subgraphs` | `{ id?, annotations, entityIds, edgeIds }` | `201` + resolved body |
| GET | `/subgraphs/{id}` | — | `{ id, annotations, subgraph: { entities, edges } }` |
| PUT | `/subgraphs/{id}` | `{ annotations, entityIds, edgeIds }` | resolved body |
| DELETE | `/subgraphs/{id}` | — | `204` |

Follow existing controller patterns in `ObjsGraphController.kt` (status codes, `{ "error": "…" }` / validation issues).

## Wire types

Reuse / mirror TS-facing JSON shapes already used for `BoMSubgraph` on `/graph/query`. Entity/edge arrays must keep **string UUID** ids identical to store.

## Tests

MockMvc (see `ObjsGraphControllerTest.kt` patterns):

- Create → get → list sees item
- Put replaces membership
- Delete → get 404
- Invalid membership → 400
- Unknown id → 404

## Out of scope

- Matcher DSL (WI-004)
- Snapshot route (WI-007)
- UI

## Implementation checklist

- [ ] Controller + service wiring
- [ ] OpenAPI annotations
- [ ] MockMvc tests green
- [ ] STORY `[x]`; commit; push

## Acceptance

- [ ] CRUD works via MockMvc
- [ ] Resolve payload uses same entity/edge ids as store
- [ ] 404 / 400 behaviour as above

## Commit message hint

`[feat] Add /graph/subgraphs REST CRUD (WI-003)`
