# REST API

**Modules:** `:objs-service` (foundation controllers + workbench) · `:objs-gremlin-service` (traverse) · `:objs-app` (runnable + Swagger UI)  
**Base path:** `/api/v1/objs`  
**Auth:** none (G-R15)  
**OpenAPI:** springdoc-openapi **3.0.3** — UI via `:objs-app:run` (`/swagger-ui.html`, `/v3/api-docs`, groups `graph` / `registry` / `traverse` / …)

## Graph

| Method | Path | Behaviour | Module |
|--------|------|-----------|--------|
| `PUT` | `/graph` | Mutate `BoMGraphMutation` (`upsert.entities` / `upsert.edges`, `delete.entities` / `delete.edges` ids) in one TX; return upsert `BoMGraph` with assigned ids; `400` + issues if invalid. Empty delete = upsert-only. | `:objs-service` |
| `POST` | `/graph/validate` | Dry-run of the same mutation body; always `200` + `BoMValidationResult` | `:objs-service` |
| `POST` | `/graph/query` | JSON/YAML matcher DSL → subgraph (induced edges, or stored pack members for `subgraph` / `subg-expr`); sole **matcher** graph query endpoint | `:objs-service` |
| `GET` | `/graph/subgraphs` | List soft-link packs (`id`, `annotations`, `entityCount`, `edgeCount`) | `:objs-service` |
| `POST` | `/graph/subgraphs` | Create soft-link pack (`annotations`, `entityIds`, `edgeIds`; optional `id`) → `201` + resolved members | `:objs-service` |
| `GET` | `/graph/subgraphs/{id}` | Header + resolved live members; `404` if missing | `:objs-service` |
| `PUT` | `/graph/subgraphs/{id}` | Replace annotations and membership | `:objs-service` |
| `DELETE` | `/graph/subgraphs/{id}` | Drop header + membership; graph objects remain; `204` | `:objs-service` |
| `POST` | `/graph/subgraphs/{id}/snapshot` | Hard clone: body `{ "annotations": {…} }` **required**; new ids + new pack → `201` | `:objs-service` |
| `POST` | `/graph/traverse/gremlin` | Matcher + gremlin-lang script → `BoMGremlinResult` (OpenAPI tag **`traverse`**) | `:objs-gremlin-service` |
| `POST` | `/graph/import?format=seeds` | Multipart Graph seed YAML (MERGE); catalog kinds rejected | `:objs-service` |
| `GET` | `/graph/export?format=seeds` | Bounded Graph seed YAML; annotation filter required (`FILTER_EMPTY` if missing) | `:objs-service` |
| `DELETE` | `/graph` | **Deprecated** shim over mutate delete lists; body `{ entityIds?, edgeIds? }`; `204` / `404` / `400` | `:objs-service` |

### Traverse (Gremlin)

Body: `{ "matcher", "script", "strategy?", "bindings?", "traversalOptions?" }`.
Matcher DSL matches `/graph/query`. See [`../graph/gremlin.md`](../graph/gremlin.md).

`BoMGraphMutation` keeps deletes explicit so seed MERGE semantics stay “omission never deletes”. Persist order: validate projected state → explicit edge deletes → entity deletes (cascade incident edges) → upserts. Same id in delete and upsert: upsert wins.

JSON shape:

```json
{
  "upsert": { "entities": [], "edges": [] },
  "delete": { "entities": [], "edges": [] }
}
```

`delete.entities` / `delete.edges` are id arrays.

Matcher DSL root is one matcher object (`anno`, `anno-expr`, `obj-expr`, `ids`, `subgraph`, `subg-expr`, …) or an ordered array of matcher
objects (chained). See [`../graph/annotations-and-subgraphs.md`](../graph/annotations-and-subgraphs.md).

Soft-link pack create/replace body:

```json
{
  "annotations": { "decisionId": "D-42" },
  "entityIds": ["11111111-1111-1111-1111-111111111111"],
  "edgeIds": ["22222222-2222-2222-2222-222222222222"]
}
```

Response shape `{ "id", "annotations", "subgraph": { "entities", "edges" } }`. Edge membership write requires both endpoints already in the entity membership set. Snapshot without `annotations` → `400` (`SUBGRAPH_SNAPSHOT_ANNOTATIONS`).

Entity delete removes incident edges (store behaviour) and cascades pack membership rows.

## Registry

| Method | Path | Behaviour |
|--------|------|-----------|
| `GET` | `/registry/types` | Distinct schema type names |
| `GET` | `/registry/schemas` | All schemas |
| `GET` | `/registry/schemas/{type}` | Versions for type |
| `DELETE` | `/registry/schemas/{type}` | Remove all versions of the type + incident allow-list rules (source/target match, properties-schema refs) |
| `GET`/`PUT`/`DELETE` | `/registry/schemas/{type}/{version}` | Get / upsert / remove one version |
| `GET`/`PUT` | `/registry/edges` | List / upsert edge definition (allow-list); body may include `cardinality` (`UNSPECIFIED` / `1:1` / `1:*`) |
| `DELETE` | `/registry/edges?sourceType&role&targetType` | Remove exact triple |
| `GET`/`PUT` | `/registry/schemas/{type}/{version}/edges` | List / replace relations for an edge-property schema (includes `cardinality`) |
| `POST` | `/registry/import?format=seeds` | Multipart catalog seed YAML (MERGE); Graph kinds rejected |
| `GET` | `/registry/export?format=seeds` | Catalog-only seed YAML |
| `GET` | `/registry/export?format=json-schema` | Full-catalog JSON Schema for codegen; optional `dialect` / `includeEdges` / `includeEdgePropertySchemas` |

See [`../graph/seeds.md`](../graph/seeds.md) and [`../graph/object-schema-dsl.md`](../graph/object-schema-dsl.md).

JSON Schema export options (C-10): defaults are `dialect=2020-12`, `includeEdges=outbound`,
`includeEdgePropertySchemas=true`. Use `includeEdges=linked` for bidirectional relation props
(codegen parent/child navigation).
## Status

| Method | Path | Behaviour |
|--------|------|-----------|
| `GET` | `/status` | Smoke `{ state, module }` |

## Related

- Catalog persistence / seeds: [`docs/workitems/completed/20260729-graph-config-seeds/`](../../workitems/completed/20260729-graph-config-seeds/STORY.md)
- Allowed-edge cardinality: [`docs/design/graph/model.md`](../graph/model.md)
- Gremlin traverse: [`docs/design/graph/gremlin.md`](../graph/gremlin.md)
- Registry/graph I/O formats: backlog **C-7**
- Catalog persistence: backlog **C-3** / **C-4** (done); cardinality **C-6**