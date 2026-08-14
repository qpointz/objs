# Asset repository example

Objs as a **centralized object store**: collections (named graphs), typed objects, domain REST + SPA, foundation workbench for schemas, Python producer/consumer.

```text
examples/asset-repository/
  asset-repository-service/       # Java 21 Spring Boot
  asset-repository-service-ui/    # Domain SPA → /app/
  scripts/                        # Python client (WI-007)
  demo/load-data/                 # qsynth model, default CSVs, REST loader
```

Design: [`docs/design/asset-repository/example.md`](../../docs/design/asset-repository/example.md)

## Run

```bash
./gradlew :asset-repository-service:run
```

Uses the **`demo`** profile by default (ontology + collection instance seeds). Skip the Vite UI build with `-PskipUi=true` if you only need the API.

| Surface | URL |
|---------|-----|
| Domain UI | http://localhost:8080/app/ |
| Workbench (schemas) | http://localhost:8080/workbench/ |
| Domain OpenAPI | http://localhost:8080/swagger-ui.html — select group **asset-repository** (also `/v3/api-docs/asset-repository`) |
| Domain REST | http://localhost:8080/api/v1/asset-repository/** |

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

## Sample REST

```bash
# List collections
curl -s http://localhost:8080/api/v1/asset-repository/collections | jq .

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

## Synthetic load (performance)

[`demo/load-data`](demo/load-data/README.md) holds a qsynth model (demo-seed ratios), a committed CSV extract, and `load.py`. Load the default extract into a running app without Docker. Scale by changing `rows_multiply` and regenerating with `qpointz/qsynth:latest`.

## Notes

- Hybrid persistence: `ar_collection` / `ar_collection_type` for metadata; object payloads only in objs.
- `objs-app` does **not** depend on this example.
