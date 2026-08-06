# Gremlin subgraph traversal

**Modules:** `:objs-gremlin-core` (engine), `:objs-gremlin-service` (REST + Boot autoconfig)  
**Workbench:** Query view at `/workbench/query` (SPA in `:objs-service`)  
**TinkerPop:** `4.0.0-beta.3` (JDK 21)

Gremlin runs **behind** the BoM graph API: select a subgraph with the same matcher DSL as
Explorer, materialize a **read-only** in-memory TinkerGraph, evaluate **gremlin-lang**, then
project results back to `BoMGremlinResult` (optional Explorer-shaped `BoMSubgraph`).

```text
matcher  →  subgraph1  →  envelope TinkerGraph  →  gremlin-lang  →  BoMGremlinResult
              │                                                      │
              └─ BoMGraphStore.selectSubgraph                        ├─ subgraph2 when graph-shaped
                 (Explorer / Composer parity)                        └─ table / scalar / items otherwise
```

Mutations on the snapshot are ephemeral and **never** written back to `BoMGraphStore`.
There is no Gremlin Server process.

## Modules

| Module | Role |
|--------|------|
| `:objs-gremlin-core` | Materializer, strategies, `BoMGremlinEngine`, result projection |
| `:objs-gremlin-service` | `POST /api/v1/objs/graph/traverse/gremlin`, OpenAPI tag `traverse` |
| `:objs-app` | Depends on `:objs-gremlin-service` so Traverse/Query REST is on the classpath |
| `:objs-service` UI | Query peer view; does **not** embed TinkerPop |

Foundation modules (`:objs-core`, `:objs-service`) stay free of TinkerPop dependencies.

## Materialization strategies

`BoMGremlinMaterializationStrategy` maps a `BoMSubgraph` to a TinkerGraph.

| Strategy | Behaviour | Status |
|----------|-----------|--------|
| **`envelope`** (default) | One vertex per entity, one edge per BoM edge. Entity `type` → vertex label; edge `role` → edge label. Ids are UUIDs. Nested `payload`, `annotations`, and edge `properties` stay as **nested map** property values (not exploded into child vertices). | **Implemented** |
| `flatten` | Scalarize nested fields to dotted / path keys on the same V/E | Documented only |
| `nested-vertices` | Nested `OBJECT` fields → child vertices + synthetic edges | Documented only |

REST / engine optional `strategy` string defaults to `envelope`; unknown → error.

### Envelope property keys

| BoM | TinkerPop |
|-----|-----------|
| entity id | vertex id |
| entity type | vertex label |
| `schemaVersion` | vertex property `schemaVersion` |
| `payload` | vertex property `payload` (map) |
| `annotations` | vertex property `annotations` (map) |
| edge id | edge id |
| edge role | edge label |
| edge `type` / `schemaVersion` / `properties` | edge properties `type`, `schemaVersion`, `properties` |

Read payload fields in scripts with `values('payload').select('name')` (not top-level `values('name')`).

## Script language

| Topic | Choice |
|-------|--------|
| Language | **`gremlin-lang`** via `GremlinLangScriptEngine` |
| Binding | `g` = `GraphTraversalSource` |
| Options | `BoMGremlinTraversalOptions`: `timeoutSeconds` (default **60**), reserved `language` (default **`gremlin-lang`**; unknown → error) |
| Out of scope | Gremlin-Groovy; SPARQL (removed in TinkerPop 4); GQL dialects |

Workbench Query editor uses **Groovy** highlighting for familiarity; the wire protocol still sends
`language: gremlin-lang`.

## Engine API

```text
BoMGremlinEngine.eval(subgraph, script, bindings?, strategy?, options?)
BoMGremlinEngine.selectAndEval(store, matcher, script, bindings?, strategy?, options?)
```

`selectAndEval` is the product path: matcher → induced subgraph1 → materialize → eval.

## Result model (`BoMGremlinResult`)

Public API never returns TinkerPop types.

| Field | Role |
|-------|------|
| `primary` | `graph` \| `table` \| `scalar` \| `list` \| `mixed` |
| `items` | Ordered kind-tagged projection of the script return |
| `subgraph` / `views.graph` | **subgraph2** — Explorer-shaped `BoMSubgraph` when projectable; else `null` |
| `views.table` | Columns/rows when all items are maps |
| `views.scalar` | Single scalar aggregate when applicable |
| `meta` | Strategy, language, subgraph1/2 stats, `resultCount`, `durationMs` |

### Subgraph2 rules (summary)

1. Collect entities from vertex / path vertex hits.
2. Collect edges from edge / path edge hits.
3. If vertices were returned and **no** edge elements were returned, **induce** edges from subgraph1 among those entity ids.
4. If edges were returned, use those only (no extra induction).
5. Dedupe by id; drop dangling edges.

Missing `subgraph` on success means the result is analytic (table/scalar/mixed), not a failure.

## REST

```http
POST /api/v1/objs/graph/traverse/gremlin
Content-Type: application/json
```

```json
{
  "matcher": { "anno": { "app": "app-00001" } },
  "script": "g.V().hasLabel('Service', 'Policy')",
  "strategy": "envelope",
  "traversalOptions": { "timeoutSeconds": 60, "language": "gremlin-lang" }
}
```

- **`matcher`** — same DSL as `POST /graph/query` (anno / anno-expr / chained).
- **`script`** — gremlin-lang text (required, non-blank).
- Owning module: **`:objs-gremlin-service`** (OpenAPI tag **`traverse`**).
- `200` + `BoMGremlinResult`; `400` + `{ "error": "…" }` or matcher `issues`.

See also [`../service/rest-api.md`](../service/rest-api.md).

## Workbench Query UI

Peer view **Query** (`/workbench/query`):

- Top tabs: **Query** (script) \| **Matcher** \| **Options** (timeout)
- Horizontal splitter between editor and results
- Result tabs: **Structured** (tactical graph/table/scalar) \| **Raw** (full JSON)
- Explorer **Open in…** → Composer | Query (matcher handoff)

See [`../ui.md`](../ui.md).

## Security (v1)

- Grammar limits of **gremlin-lang** (no Groovy sandbox)
- Evaluation **timeout** (`timeoutSeconds`)
- Read-only snapshot; no persist of Gremlin mutations

## Concrete example (SBOM)

Ontology and `:objs-sbom-example` code are unchanged by this feature: traverse is generic over any
stored graph. Demo path: run `:objs-app` with the **sbom** profile (seeded canonical graph), then
Query / traverse REST with a matcher such as `{ "anno": { "app": "app-00001" } }`.

## Related

- Matchers / induced subgraphs: [`annotations-and-subgraphs.md`](annotations-and-subgraphs.md)
- Story: [`../../workitems/completed/20260806-gremlin-subgraph-traversal/STORY.md`](../../workitems/completed/20260806-gremlin-subgraph-traversal/STORY.md)
