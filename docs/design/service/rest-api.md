# REST API

**Modules:** `:objs-service` (foundation controllers) · `:objs-gremlin-service` (traverse) · `:objs-service-app` (workbench runner + Swagger UI)  
**Base path:** `/api/v1/objs`  
**Auth:** none (G-R15)  
**OpenAPI:** springdoc-openapi **3.0.3** — UI via `:objs-service-app:run` (`/swagger-ui.html`, `/v3/api-docs`, groups `graph` / `registry` / `traverse` / …)

## Graphs + entity pool

No global graph: `/entities` is the pool (CRUD, no graph scope); `/graphs` is graph CRUD, membership,
graph-local edges, resolve, query, clone, and graph versions. See [`../graph/model.md`](../graph/model.md) and
[`../graph/annotations-and-matchers.md`](../graph/annotations-and-matchers.md).

| Method | Path | Behaviour | Module |
|--------|------|-----------|--------|
| `GET` | `/entities` | List pool entities | `:objs-service` |
| `POST` | `/entities/query` | Matcher DSL (`obj-expr` / chain of `obj-expr`) over the **pool** (orphans included); edges empty; equality/`&&` SQL pushdown | `:objs-service` |
| `POST` | `/entities` | Create an entity in the pool only (no graph membership) | `:objs-service` |
| `GET` | `/entities/{id}` | Fetch one pool entity; `404` if missing | `:objs-service` |
| `PUT` | `/entities/{id}` | Update payload/annotations | `:objs-service` |
| `DELETE` | `/entities/{id}` | Remove from pool; cascades membership rows and incident edges | `:objs-service` |
| `GET` | `/graphs` | List graph headers (`id`, `annotations`, member/edge counts) | `:objs-service` |
| `POST` | `/graphs` | Create graph header (`annotations`); optional `entityIds` to seed membership → `201` | `:objs-service` |
| `GET` | `/graphs/{id}` | Header + resolved members + graph-local edges; `404` if missing | `:objs-service` |
| `PUT` | `/graphs/{id}/annotations` | Replace graph header annotations (membership unchanged) | `:objs-service` |
| `PATCH` | `/graphs/{id}` | **MERGE** mutate: `entities`/`edges` × `set`/`unset`; omission keeps | `:objs-service` |
| `PUT` | `/graphs/{id}` | **REPLACE** mutate: `*.set` is full desired membership + edges; `unset` rejected | `:objs-service` |
| `PATCH`/`PUT` | `/graphs/{id}/validate` | Dry-run MERGE / REPLACE (no persist); `POST …/validate` = MERGE alias | `:objs-service` |
| `DELETE` | `/graphs/{id}` | Drop header + membership + edges (CASCADE); pool entities kept; `204` | `:objs-service` |
| `POST`/`DELETE` | `/graphs/{id}/members/{entityId}` | Attach / detach an existing pool entity id (membership row only; pool entity kept on detach) | `:objs-service` |
| `POST` | `/graphs/{id}/query` | Matcher DSL (`obj-expr` / chained) scoped to this graph's members; edges induced within scope | `:objs-service` |
| `POST` | `/graphs/query` | Matcher DSL (`all`, `graph-expr`, or chained starting with either) over graph headers → matching graphs' stored members + graph-local edges (distinct by id) | `:objs-service` |
| `POST` | `/graphs/{id}/clone` | Deep copy into a **new** independent graph (new entity/edge ids, current HEAD only); source unchanged; no parent/lineage link; clone history starts empty | `:objs-service` |
| `POST` | `/graphs/{id}/versions` | **Snapshot** / `createDeepGraphVersion`: pin current HEAD on the **same** `graph_id`; body optional version `annotations` | `:objs-service` |
| `GET` | `/graphs/{id}/versions` | List deep versions newest first (`version DESC`); empty if never snapshotted | `:objs-service` |
| `GET` | `/graphs/{id}/versions/{version}` | Reconstruct pinned graph (read-only; slower OK). Works after HEAD delete | `:objs-service` |
| `POST` | `/graph/traverse/gremlin` | Matcher + gremlin-lang script → `BoMGremlinResult` (OpenAPI tag **`traverse`**); matcher DSL scoping rules as above | `:objs-gremlin-service` |

**Fail closed:** bare `obj-expr` on `/graphs/query` with no stage-0 `all` / `graph-expr` → `400`
(lock G-G16). Pool-wide `obj-expr` (orphans included) uses `POST /entities/query` instead.
Retired keys `anno` / `anno-expr` / `ids` / `subgraph` / `subg-expr` are rejected
(`MATCHER_DSL_RETIRED_KEY`) — see the retirement table in
[`../graph/annotations-and-matchers.md`](../graph/annotations-and-matchers.md#retired-matchers-parity-with-pre-c-13-keys).

Matcher DSL root is one matcher object (`all` / `graph-expr` / `obj-expr`) or an ordered array of matcher
objects (chained).

`PATCH /graphs/{id}` = **MERGE**; `PUT /graphs/{id}` = **REPLACE**. Body is kind-first `BoMGraphMutation`:

```json
{
  "entities": { "set": [], "unset": [] },
  "edges": { "set": [], "unset": [] }
}
```

MERGE persist order: validate → edge unsets → entity/membership unsets → sets (set wins on same id).
REPLACE: `*.set` is the desired membership + edges; prune extras; non-empty `unset` → `REPLACE_UNSET_NOT_ALLOWED`.
Empty both `set` under REPLACE clears contents (stable `graphId`).

### Mutate glossary

| Name | What it is | Not |
|------|------------|-----|
| **MERGE** + `PATCH /graphs/{id}` | Patch one graph: `set` + `unset`; omission keeps | Combining several graphs into a new one |
| **REPLACE** + `PUT /graphs/{id}` | Overwrite one graph’s membership + edges from `*.set` | Id-only membership swap; multi-graph union |
| **`replace(id, BoMGraphSpec)`** | Set membership/`edgeIds` by existing ids only | Payload REPLACE mutate |
| **`mergeGraph(sourceIds, …)`** | Create a **new** graph = union of sources | In-place MERGE mutate |
| Seed **MERGE** | Catalog/graph seed import; omission never deletes | Mutate mode or `mergeGraph` |

Composer: **Save** → MERGE; **Overwrite…** → REPLACE.

## Registry

| Method | Path | Behaviour |
|--------|------|-----------|
| `GET` | `/registry/types` | Distinct schema type names |
| `GET` | `/registry/schemas` | All schemas |
| `GET` | `/registry/schemas/{type}` | Versions for type |
| `DELETE` | `/registry/schemas/{type}` | Remove all versions of the type + incident allow-list rules (source/target match, properties-schema refs) |
| `GET`/`PUT`/`DELETE` | `/registry/schemas/{type}/{version}` | Get / upsert / remove one version |
| `GET`/`PUT` | `/registry/edges` | List / upsert edge definition (allow-list); body may include `cardinality` (`UNSPECIFIED` / `1:1` / `1:*`), `description`, `sourceVerb`, `targetVerb`, `tags`, `attributes` |
| `DELETE` | `/registry/edges?sourceType&role&targetType` | Remove exact triple |
| `GET`/`PUT` | `/registry/schemas/{type}/{version}/edges` | List / replace relations for an edge-property schema (includes `cardinality`) |
| `POST` | `/registry/import?format=seeds` | Multipart catalog seed YAML (MERGE); Graph kinds rejected |
| `POST` | `/registry/refresh` | Rehydrate schema + allowed-edge catalogs from the store (bypass TTL) |
| `GET` | `/registry/export?format=seeds` | Catalog-only seed YAML |
| `GET` | `/registry/export?format=json-schema` | Full-catalog JSON Schema for codegen; optional `dialect` / `includeEdges` / `includeEdgePropertySchemas` |

See [`../graph/seeds.md`](../graph/seeds.md), [`../graph/object-schema-dsl.md`](../graph/object-schema-dsl.md),
and [`../graph/json-schema-to-seeds.md`](../graph/json-schema-to-seeds.md) (JSON Schema is export-only in product).

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
- Pool/graph inversion, minimal matchers: backlog **C-13** — [`docs/workitems/in-progress/graphs-from-objects/STORY.md`](../../workitems/in-progress/graphs-from-objects/STORY.md) (Stages 1–4 landed; Stage 5 cleanup remaining)