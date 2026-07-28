# REST API

**Module:** `:objs-service` (controllers) + `:objs-app` (runnable + Swagger UI)  
**Base path:** `/api/v1/objs`  
**Auth:** none (G-R15)  
**OpenAPI:** springdoc-openapi **3.0.3** — UI via `:objs-app:run` (`/swagger-ui.html`, `/v3/api-docs`, groups `graph` / `registry`)

## Graph

| Method | Path | Behaviour |
|--------|------|-----------|
| `PUT` | `/graph` | Upsert `BoMGraph`; return graph with assigned ids; `400` + issues if invalid |
| `POST` | `/graph/validate` | Dry-run; always `200` + `BoMValidationResult` |
| `GET` | `/graph` | Annotation query params → match-all induced subgraph; empty filter → `400` |
| `DELETE` | `/graph` | Body `{ entityIds?, edgeIds? }`; all-or-nothing; `204` / `404` / `400` |

Entity delete removes incident edges (store behaviour).

## Registry (in-memory until C-3)

| Method | Path | Behaviour |
|--------|------|-----------|
| `GET` | `/registry/types` | Distinct schema type names |
| `GET` | `/registry/schemas` | All schemas |
| `GET` | `/registry/schemas/{type}` | Versions for type |
| `GET`/`PUT`/`DELETE` | `/registry/schemas/{type}/{version}` | Get / upsert / remove |
| `GET`/`PUT` | `/registry/edges` | List / upsert edge definition (allow-list) |
| `DELETE` | `/registry/edges?sourceType&role&targetType` | Remove exact triple |

## Status

| Method | Path | Behaviour |
|--------|------|-----------|
| `GET` | `/status` | Smoke `{ state, module }` |

## Related

- Story: [`docs/workitems/in-progress/entity-rest-api/`](../../workitems/in-progress/entity-rest-api/STORY.md)
- Catalog persistence: backlog **C-3**
