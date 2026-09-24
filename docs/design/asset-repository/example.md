# Asset repository example

**Status:** product design (aligned with D-3 GAPS)  
**Story:** [`docs/workitems/completed/20260814-asset-repository-example/`](../../workitems/completed/20260814-asset-repository-example/STORY.md)  
**Gaps:** [`GAPS.md`](../../workitems/completed/20260814-asset-repository-example/GAPS.md)

## Purpose

Demonstrate **objs as a centralized object store** for asset information scattered across systems and formats.

Independent of the SBOM inventory example — packaging pattern and adapted type seeds only.

## Product terms

| Term | Meaning |
|------|---------|
| **Collection** | Named container of assets, owned by a team. Maps to an objs named graph. |
| **Object** | One asset record (typed payload). Pool entity; membership places it in a collection. |
| **Owner** | Team / contact fields on the collection (metadata; no auth in v1). |
| **Composition** | Objects plus optional in-collection relations when a collection holds related types. |

## Packaging

```text
examples/asset-repository/
  asset-repository-service/       # Java 21; objs-core + objs-service
  asset-repository-service-ui/    # Domain SPA
  scripts/                        # Python client + ar_perf_harness.py
  demo/load-data/                 # optional qsynth CSV kit (D-4)
```

Workbench (schema): `/workbench/` via `runtimeOnly` `:objs-service-ui` as a **sidecar** — operators manage schemas there. Domain UI: `/ar/`. Packaged SPAs use servlet filters (`SpaRoutingFilter`) so a browser refresh of a client route serves `index.html` instead of a 404. Static files (`*.js`, `*.css`, …) pass through; version path segments such as `1.0.0` are treated as SPA routes.

**Run:** `./gradlew :asset-repository-service:run` (demo profile: ontology + sample collections). Operator guide: [`examples/asset-repository/README.md`](../../../examples/asset-repository/README.md). PostgreSQL: `--spring.profiles.active=demo,postgres` (`OBJS_DB_*` env). Collection list/search casts optional JPQL string params so PostgreSQL does not bind nulls as `bytea`.

### Performance profile (`perf`)

Story **D-10**: [`docs/workitems/completed/20260924-ar-perf-profile/`](../../workitems/completed/20260924-ar-perf-profile/STORY.md).

| Concern | Behavior |
|---------|----------|
| Profile | `perf` (compose with `postgres`); ontology seeds only — not `demo` instance YAML |
| Graphs | `ar.perf.graphs` / `AR_PERF_GRAPHS` collections; each is one named graph |
| Objects / edges | **Totals** (`ar.perf.objects` / `edges`) split evenly across graphs; edges are graph-local |
| Versions | `ar.perf.versions` deep snapshots **per graph** (costly at large N) |
| Write path | Batched `NamedGraphStore.mutate` with preassigned UUIDs (no identity-scan `writeComposition`) |
| Gate | `/api/**` → 503 until fill completes |
| Stats | Collection statistics include `objectCount` and `edgeCount` |
| Harness | [`scripts/ar_perf_harness.py`](../../../examples/asset-repository/scripts/ar_perf_harness.py) — use `--collection perf-noise-0` when `graphs>1` |
| Alt path | Optional qsynth CSV kit: [`demo/load-data`](../../../examples/asset-repository/demo/load-data/README.md) |

```bash
./gradlew :asset-repository-service:run --args="--spring.profiles.active=perf,postgres --ar.perf.graphs=10 --ar.perf.objects=50000 --ar.perf.edges=100000 --ar.perf.versions=3"
```

**Rule:** domain Java, domain SPA, and Python client use **`objs-core` programmatic APIs** and **`/api/v1/asset-repository/**` only. Do **not** use foundation `/api/v1/objs/**` as the application data API (even though those endpoints may be reachable on the same process).

## Hybrid persistence

| Layer | Owns |
|-------|------|
| Domain DB `ar_collection` | name, description, owner, owner_email, support_email, sla, object_write_mode, graph_id — Boot Flyway `V1` at `classpath:db/migration` |
| Domain DB `ar_collection_type` | accepted object types (1-*) + optional per-type `metadata` |
| objs | Object payloads, membership, in-collection edges — `bom_*` via objs-core Flyway, not this app’s locations |

## Type catalog (seed)

Library + product pattern — everything is an **asset object**; collections are shelves or assemblies.

| Domain | Types | Edges (in-collection) |
|--------|-------|------------------------|
| Data | `Database`, `Dataset` | `Database` `CONTAINS` `Dataset` |
| AI agents | `AiAgent`, `Prompt`, `Skill`, `Tool`, `McpServer` | `USES_PROMPT`, `HAS_SKILL`, `USES_TOOL`, `CONNECTS_TO`, … |
| Models | `ModelFamily`, `ModelVersion`, `Modality` | `HAS_VERSION`, `SUPPORTS` |

Demo profile seeds **library** collections (single-type inventories) and **product** collections (`dp-customers`, `agent-support`, `models-openai`) that assemble assets with edges (copy-on-assemble).

Only **`accepted_types`** may be written into a given collection.

## Identity

Per-collection `object_write_mode`: `UUID` | `IDENTIFIER` | `UUID_OR_IDENTIFIER` (default). See GAPS G-P3.

## Write pipeline

attempt → resolve identity (`findEntitiesByIdentity`, then keep members of this collection) → **PreprocessingExtension** (no-op v1) → persist → **EventExtension** (no-op v1)

## REST (sketch)

Prefix `/api/v1/asset-repository`:

- Collections CRUD-lite  
- `POST …/collections/{id}/copy` — live collection copy: clone collection metadata (accepted types, write mode, owner) + store **`copyGraph`** (same object ids, new `graph_id`). Default name `Copy of {name}`. Not a freeze snapshot.  
- `POST …/collections/{id}/objects`  
- `POST …/collections/{id}/compositions`  
- List/get objects; `GET …/objects/{objectId}/relations`; `POST …/collections/{id}/objects/search` (existing matchers, graph-scoped)  
- Schema reads: `GET …/schema-catalog` (latest ENTITY schema per type + collections that accept it — “used in” stays domain), `GET …/schema-catalog/{type}/allowed-edges` (core `allowedEdgesForType`, including `*`), `GET …/schemas`, `GET …/schemas/{type}`, `GET …/schemas/{type}/{version}`, `GET …/collections/{id}/schemas`  
- Object list uses graph members (not full graph + edges). `objectCount` uses graph-scoped `countByType`. Object relations use `listIncidentEdges`. Object list paging uses `listMembers(graphId, page)`. Object text `q` is C-20. Collection name `LIKE` stays domain.  

OpenAPI group **`asset-repository`** in Swagger UI (`/swagger-ui.html`, `/v3/api-docs/asset-repository`). Topic tags: **collections**, **objects**, **schemas**.

**Run:** `./gradlew :asset-repository-service:run` → http://localhost:8080/swagger-ui.html (select **asset-repository**). Domain operations document request/response DTO schemas (`CollectionDto`, `ObjectDto`, …). Port **8080** by default — do not run `:sbom-service` on the same port concurrently. Codegen readiness: `scripts/openapi-client-codegen-smoke.md` (`--groups asset-repository --readiness-only`).

## Domain UI

Mantine SPA (workbench look: dark/light, AppShell). Top nav: **Collections**, **Schemas** (read-only), optional **Workbench** sidecar.

Collections: portal (cards/list); left pane lists collections with incremental search when browsing a collection; content pane has an **obj-expr** query bar and object **grid** or raw **JSON**. Collection create/edit is a separate view; accepted types are picked from registered object schemas. Type pills link to the schema view. Object create/edit and object view share **Visual / JSON / YAML**. Object view also lists **related objects** (in-collection edges) as links. Object type / schema version link to that schema version.

Schemas: portal of latest object schemas (used-in collections); left pane type list with search; hierarchical read-only schema tree with **linkable versions** (latest by default). The type detail page also lists inbound and outbound **allowed edges** (cardinality, properties policy, description/verbs/tags when set); that list is page chrome, not part of the JSON/YAML schema dump. Domain API only — not workbench REST.

## Python client

`examples/asset-repository/scripts/ar_client.py` — producer and consumer against domain REST (`--base-url`, default `http://localhost:8080`).

## See also

- [`GAPS.md`](../../workitems/completed/20260814-asset-repository-example/GAPS.md)  
- Graph foundation: [`docs/design/graph/model.md`](../graph/model.md), [`annotations-and-matchers.md`](../graph/annotations-and-matchers.md)  
