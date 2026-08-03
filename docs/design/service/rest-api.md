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
| `POST` | `/graph/query` | JSON/YAML matcher DSL → induced subgraph; sole graph matching/query endpoint |
| `POST` | `/graph/import?format=seeds` | Multipart Graph seed YAML (MERGE); catalog kinds rejected |
| `GET` | `/graph/export?format=seeds` | Bounded Graph seed YAML; annotation filter required (`FILTER_EMPTY` if missing) |
| `DELETE` | `/graph` | Body `{ entityIds?, edgeIds? }`; all-or-nothing; `204` / `404` / `400` |

Matcher DSL root is one matcher object (`anno`, `anno-expr`, …) or an ordered array of matcher
objects (chained). See [`../graph/annotations-and-subgraphs.md`](../graph/annotations-and-subgraphs.md).

Entity delete removes incident edges (store behaviour).

## Registry

| Method | Path | Behaviour |
|--------|------|-----------|
| `GET` | `/registry/types` | Distinct schema type names |
| `GET` | `/registry/schemas` | All schemas |
| `GET` | `/registry/schemas/{type}` | Versions for type |
| `GET`/`PUT`/`DELETE` | `/registry/schemas/{type}/{version}` | Get / upsert / remove |
| `GET`/`PUT` | `/registry/edges` | List / upsert edge definition (allow-list); body may include `cardinality` (`UNSPECIFIED` / `1:1` / `1:*`) |
| `DELETE` | `/registry/edges?sourceType&role&targetType` | Remove exact triple |
| `GET`/`PUT` | `/registry/schemas/{type}/{version}/edges` | List / replace relations for an edge-property schema (includes `cardinality`) |
| `POST` | `/registry/import?format=seeds` | Multipart catalog seed YAML (MERGE); Graph kinds rejected |
| `GET` | `/registry/export?format=seeds` | Catalog-only seed YAML |
| `GET` | `/registry/export?format=json-schema` | Full-catalog JSON Schema for codegen |

See [`../graph/seeds.md`](../graph/seeds.md) and [`../graph/object-schema-dsl.md`](../graph/object-schema-dsl.md).

## Status

| Method | Path | Behaviour |
|--------|------|-----------|
| `GET` | `/status` | Smoke `{ state, module }` |

## Related

- Catalog persistence / seeds: [`docs/workitems/completed/20260729-graph-config-seeds/`](../../workitems/completed/20260729-graph-config-seeds/STORY.md)
- Allowed-edge cardinality: [`docs/design/graph/model.md`](../graph/model.md)
- Registry/graph I/O formats: backlog **C-7**
- Catalog persistence: backlog **C-3** / **C-4** (done); cardinality **C-6**