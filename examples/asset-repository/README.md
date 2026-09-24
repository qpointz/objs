# Asset repository example

Objs as a **centralized object store**: collections (named graphs), typed objects, domain REST + SPA, foundation workbench for schemas, Python producer/consumer.

```text
examples/asset-repository/
  asset-repository-service/       # Java 21 Spring Boot
  asset-repository-service-ui/    # Domain SPA → /ar/
  scripts/                        # Python client (WI-007)
  demo/load-data/                 # qsynth model, default CSVs, REST loader
```

Design: [`docs/design/asset-repository/example.md`](../../docs/design/asset-repository/example.md)

## Run

```bash
./gradlew :asset-repository-service:run
```

Uses the **`demo`** profile by default (ontology + collection instance seeds). Skip the Vite UI build with `-PskipUi=true` if you only need the API.

## Offline typed bindings

The checked-in [`asset-repository-catalog.codegen.schema.json`](asset-repository-service/src/jsonschema/asset-repository-catalog.codegen.schema.json)
drives both JSON-schema POJO generation and the application-owned typed graph bindings. No backend is
needed during compilation:

```bash
./gradlew :asset-repository-service:compileJava
```

Generated output is written under `asset-repository-service/build/generated/sources/`.

| Surface | URL |
|---------|-----|
| Domain UI | http://localhost:8080/ar/ |
| Workbench (schemas) | http://localhost:8080/workbench/ |
| Domain OpenAPI | http://localhost:8080/swagger-ui.html — select group **asset-repository** (also `/v3/api-docs/asset-repository`) |
| Domain REST | http://localhost:8080/api/v1/asset-repository/** |

Default port **8080** — do not run SBOM inventory on the same port at the same time (override `server.port` if needed).

The domain UI uses Mantine (dark/light toggle). Collections sit in a searchable left pane; the content pane queries with **obj-expr** and shows objects as a grid or raw JSON. Collection create picks accepted types from existing schemas. Object create/edit is **schema-driven** (one object at a time). JSON/YAML can post a **composition** (`objects` + `relations`). Schemas are read via domain REST (`GET /api/v1/asset-repository/schemas/...`), not foundation `/api/v1/objs/**`.

Foundation `/api/v1/objs/**` is present as a **sidecar** for the workbench. Domain Java, the domain SPA, and the Python client must use **`/api/v1/asset-repository/**` only.

## Demo data (`demo` profile)

Classpath seeds (no Java seeder):

- [`asset-repository-ontology.yaml`](asset-repository-service/src/main/resources/seeds/asset-repository-ontology.yaml) — `ObjectSchema` + `AllowedEdgeRule`
- [`asset-repository-demo-data.yaml`](asset-repository-service/src/main/resources/seeds/asset-repository-demo-data.yaml) — `Collection` + `CollectionObjects`

| Collection | Accepted types | Notes |
|------------|----------------|-------|
| `datasets` | Dataset | ~50 library objects |
| `models` | LlmModel | ~20 library objects |
| `agents` | AiAgent | ~100 library objects |
| `composables` | Prompt, Skill, Tool, Guardrail, KnowledgeSource, Template | ~200 objects + wiring |
| `mcp-servers` | McpServer, Tool, Prompt, KnowledgeSource | ~50 servers plus provided components |
| `customer-support` | all 10 types | Larger solution graph (~140 objects, dense wiring) |

## Performance fill (`perf` profile)

In-process noise fill for scale/perf experiments. Ontology seeds only — do **not** combine with `demo` for clean baselines.

Each **collection = one named graph**. Configure how many graphs to create, then **distribute** total objects and edges across them. Deep versions are taken **per graph**.

```bash
# H2 defaults: 1 graph, 1000 objects, 2000 edges, 0 versions
./gradlew :asset-repository-service:run --args="--spring.profiles.active=perf"

# Postgres
./gradlew :asset-repository-service:run --args="--spring.profiles.active=perf,postgres"
```

| Knob | Env / property | Default | Meaning |
|------|----------------|---------|---------|
| Graphs | `AR_PERF_GRAPHS` / `ar.perf.graphs` | `1` | Number of collections / named graphs |
| Objects | `AR_PERF_OBJECTS` / `ar.perf.objects` | `1000` | **Total** objects (split evenly across graphs) |
| Edges | `AR_PERF_EDGES` / `ar.perf.edges` | `2000` | **Total** edges (split; each edge stays inside one graph) |
| Versions | `AR_PERF_VERSIONS` / `ar.perf.versions` | `0` | Deep graph snapshots **per graph** |
| Batch size | `AR_PERF_BATCH_SIZE` / `ar.perf.batch-size` | `500` | Entities or edges per `NamedGraphStore.mutate` |
| Name prefix | `AR_PERF_COLLECTION` / `ar.perf.collection` | `perf-noise` | `graphs=1` → exact name; `graphs>1` → `{prefix}-0` … `{prefix}-(n-1)` |

Implementation notes:

- Bulk path: preassigned UUIDs + batched `NamedGraphStore.mutate` (not domain `writeComposition` / identity scans).
- Schema-valid minimal payloads; business ids like `perf-0-12`.
- Collection statistics expose `objectCount` and `edgeCount` (UI shows both when edges > 0).
- While fill runs, `/api/**` returns **503** until log line `Perf fill gate open`.
- Idempotent / growable: restart tops up when targets increase; does not shrink/delete.

Example — 10 graphs, 50k objects / 100k edges total (~5k / ~10k each), 3 deep versions on **each** graph:

```bash
./gradlew :asset-repository-service:run --args="--spring.profiles.active=perf,postgres --ar.perf.graphs=10 --ar.perf.objects=50000 --ar.perf.edges=100000 --ar.perf.versions=3"
```

Version cost scales roughly with **(per-graph size) × versions × graph count**.

### Timed harness

Wait for `Perf fill gate open`, then:

```bash
cd examples/asset-repository/scripts
python ar_perf_harness.py --base-url http://localhost:8080 --collection perf-noise
# multi-graph:
python ar_perf_harness.py --collection perf-noise-0
python ar_perf_harness.py --gremlin
```

Prints `objectCount` / `edgeCount` from statistics, then p50 / p95 / max / mean ms for list collections, full object list, statistics, and typed search (optional Gremlin). Observational only — no CI latency gates. Compare H2 vs `perf,postgres` by swapping profiles.

## Sample REST

```bash
# List collections
curl -s http://localhost:8080/api/v1/asset-repository/collections | jq .

# Copy a collection (shared object ids, new graph)
curl -s -X POST http://localhost:8080/api/v1/asset-repository/collections/COLLECTION_ID/copy \
  -H 'Content-Type: application/json' \
  -d '{}' | jq .

# Filter by accepted type
curl -s 'http://localhost:8080/api/v1/asset-repository/collections?acceptedType=Database' | jq .

# Search objects in a collection (replace COLLECTION_ID)
curl -s -X POST http://localhost:8080/api/v1/asset-repository/collections/COLLECTION_ID/objects/search \
  -H 'Content-Type: application/json' \
  -d '{"filters":{"name":"customers-db"}}' | jq .

# Write an object
curl -s -X POST http://localhost:8080/api/v1/asset-repository/collections/COLLECTION_ID/objects \
  -H 'Content-Type: application/json' \
  -d '{"type":"Prompt","schemaVersion":"1.0.0","payload":{"name":"demo","template":"Hello"}}' | jq .
```

## Python client

Stdlib-only script (no pip packages required):

```bash
cd examples/asset-repository/scripts
python ar_client.py consumer --base-url http://localhost:8080
python ar_client.py producer --base-url http://localhost:8080
python ar_client.py all --delete
```

Calls **`/api/v1/asset-repository/**` only** (never foundation `/api/v1/objs/**`).

## Synthetic load (optional CSV path)

[`demo/load-data`](demo/load-data/README.md) holds a qsynth model (demo-seed ratios), a committed CSV extract, and `load.py`. Prefer the **`perf` profile** above for absolute-N multi-graph fill. Use this kit when you need ratio-scaled CSVs or offline regenerate-with-Docker workflows.

## Notes

- Hybrid persistence: `ar_collection` / `ar_collection_type` for metadata; object payloads only in objs.
- Collection copy uses store `copyGraph` (same object ids, new graph). Object list/count uses `listMembers` / `countByType`. Identifier writes use `findEntitiesByIdentity` then keep members of this collection. Object text `q` is not this slice.
- `objs-service-app` does **not** depend on this example.
