# WI-004 — REST `/graphs` + entity pool

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — REST  
**Status:** done  
**Depends on:** WI-003 (**stage 1 confirmed**)  
**Modules:** `:objs-service`

## Goal

HTTP surface for pool + graphs. Remove unscoped whole-store-as-graph mutate/query. Expose only the three matcher forms.

## Target surface

| Method | Path | Notes |
|--------|------|-------|
| `*` | `/api/v1/objs/entities/**` | Pool CRUD/list |
| `GET/POST` | `/api/v1/objs/graphs` | List / create |
| `GET/PUT/DELETE` | `/api/v1/objs/graphs/{id}` | Header + resolve |
| `POST/DELETE` | `/api/v1/objs/graphs/{id}/members…` | Attach / detach |
| `POST` | `/api/v1/objs/graphs/{id}/…` mutate/edges | Graph-local |
| `POST` | `/api/v1/objs/graphs/{id}/query` | Matcher body; graph fixed by path |
| `POST` | `/api/v1/objs/graphs/{id}/clone` | Optional independent clone |

Matchers only:

```yaml
graph-expr: "…"
obj-expr: "…"
# or YAML/JSON array chain
```

Also allow top-level query that **requires** stage-0 `graph-expr` if kept; otherwise only `/graphs/{id}/query`.

## Remove

- `/api/v1/objs/graph/subgraphs/**`
- Unscoped `PUT /graph` / `POST /graph/query` as global graph
- OpenAPI examples using `anno` / `ids` / `subg-expr` / `subgraph`

## Tests

MockMvc: pool, graph CRUD, membership, query with `obj-expr` / `graph-expr` / chained, reject old keys, 404/400 cases, optional clone.

## Stage gate

`:objs-service:test` → **STORY § Stage 2 — Manual test** → **STOP**.  
**Do not start WI-005** until `stage 2 confirmed`.

## Acceptance

- [x] `/graphs` + `/entities` in OpenAPI
- [x] Three matcher forms only
- [x] No whole-store-as-graph API
- [x] STORY `[x]`; commit; push

## Commit message hint

`[feat] REST graphs + entity pool; minimal matchers (WI-004)`
