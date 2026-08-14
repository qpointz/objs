# Gaps — asset-repository-example (D-3)

Packaging and product locks for the asset repository example. Update status as WIs close them.

## Summary

| ID | Topic | Status | Notes |
|----|-------|--------|-------|
| G-A1 | Deployable shape | **resolved** | `examples/asset-repository/` — `:asset-repository-service` + `:asset-repository-service-ui` |
| G-A2 | objs dependency | **resolved** | `objs-core` programmatic; **`objs-service` + workbench as sidecar only** — domain must not call foundation REST |
| G-A3 | Collection ↔ graph | **resolved** | Collection = named graph; objects = pool entities + membership |
| G-A4 | Edges | **resolved** | Optional, in-collection only, for related-type collections |
| G-A5 | Owner model | **resolved** | Owner (and contact/SLA) on collection metadata; no auth |
| G-A6 | UI scope | **resolved** | Domain SPA: explore + create/edit (dynamic forms); workbench for schema |
| G-A7 | Language | **resolved** | Example service **Java 21 only** — no Kotlin; UI TypeScript/React |
| G-A8 | Accepted types gate | **resolved** | Only `accepted_types` may be written/created in a collection (REST + UI) |
| G-P1 | v1 type catalog | **resolved** | SBOM-adapted seed types; see detail |
| G-P2 | Hybrid persistence + metadata | **resolved** | Domain `ar_collection` + `ar_collection_type` (1-*); objs contents |
| G-P3 | Object identity / upsert | **resolved** | UUID and/or identifier; `object_write_mode` on collection |
| G-P4 | Composition write shape | **resolved** | Objects + compositions REST; through write pipeline |
| G-P5 | Write extensions | **resolved** | Dummy PreprocessingExtension + EventExtension SPIs |
| G-P6 | Python client | **resolved** | Producer + consumer script against domain REST |
| G-P7 | Search within collection | **resolved** | Domain search API + UI; programmatic objs matchers scoped to collection graph |
| G-X1 | Independence from D-2 | **resolved** | No shared modules/ontology with SBOM; packaging pattern only |

## Resolved locks (detail)

### G-A1 / G-A2 — Packaging

```text
examples/
  asset-repository/
    asset-repository-service/      # Java 21 Boot app: domain + objs-service (workbench)
    asset-repository-service-ui/   # Vite/React domain SPA; node-gradle like :objs-service-ui
```

1. Product lives only under `examples/asset-repository/`.  
2. **`objs-app`** does **not** depend on this example.  
3. Service depends on **`objs-core`** (programmatic object store) and **`objs-service`** (packs workbench at **`/workbench/`** via `:objs-service-ui`).  
4. Domain UI is a separate SPA (path `/` or `/app/`); consumed as `runtimeOnly` by the service.  
5. Gremlin not required for domain journeys in v1 (workbench Query may be present but unused by the product).  

**Sidecar rule (normative):** `:objs-service` / foundation REST (`/api/v1/objs/**`) and the workbench are included **only** so operators can manage schemas in `/workbench/`. The asset-repository **application** (domain Java services, domain REST, domain SPA, Python client) **must not** call foundation REST — reads/writes go through **`objs-core` programmatic APIs** and **domain** `/api/v1/asset-repository/**` only. Treat objs-service as a co-located sidecar, not as the app’s data API.

### G-A7 — Java 21

`:asset-repository-service` (and any example-owned JVM sources) are **Java 21 only — no Kotlin**. Foundation leaves remain Kotlin; this example consumes them from Java. Domain UI remains TypeScript/React.

### G-A3 / G-A4 — Object store mapping

| Product term | objs |
|--------------|------|
| Collection | `bom_graph` header + membership (+ optional graph-local edges) |
| Object | `bom_entity` in the global pool |
| Object in collection | membership row |
| Related types in one collection | graph-local edges (e.g. Database `CONTAINS` Dataset) |

Most collections are **single-type**. Related-type collections are the exception that uses edges.

### G-A5 — Owner / contacts

Collection product metadata includes **owner**, **owner email**, **support email**, **SLA** (plain text). Filtering by owner is supported. No login, roles, or enforcement.

### G-A6 — UI

**Domain SPA**

| Route | Job |
|-------|-----|
| `/` or `/collections` | List/filter collections (name, owner, accepted type) |
| `/collections/:id` | Metadata + object list; create object; edit collection metadata; **search objects** in collection |
| `/collections/:id/objects/new` | Dynamic create form (accepted types only) |
| `/collections/:id/objects/:objectId` | View payload (Visual / JSON / YAML) + related-object links; edit |
| `/collections/:id/objects/:objectId/edit` | Dynamic edit form |

**Workbench** at `/workbench/` — schema / registry management (foundation workbench). Domain chrome links “Schemas” → workbench. Domain screens use **domain REST** for persist; schema authoring is not duplicated in the domain SPA.

Dynamic forms are driven by the object schema for the chosen type.

### G-A8 — Accepted types gate

Each collection declares accepted object types via **`ar_collection_type`** rows (1-*). Only those types may be created or added (REST **400** otherwise). UI create picker limited to accepted types; single-type collections skip the picker. Each type row may carry **collection-level type metadata** (opaque text/JSON) for future per-type configuration.

### G-X1 — Independence

Shares subject matter (“assets”) with SBOM only at a vocabulary level. Separate Gradle projects, schemas, seeds, and APIs. Ontology seed is **copied/adapted** from SBOM — **no** Gradle dependency on SBOM modules.

### G-P1 — v1 type catalog

Seed [`asset-repository-ontology.yaml`](../../../examples/asset-repository/asset-repository-service/src/main/resources/seeds/asset-repository-ontology.yaml).

**Pattern:** objects = assets; collections = **library shelves** (mostly single-type) or **product assemblies** (related-type minigraphs). Edges in-collection only.

| Domain | Types | Edges |
|--------|-------|-------|
| Data | `Database`, `Dataset` | `CONTAINS` |
| AI agents | `AiAgent`, `Prompt`, `Skill`, `Tool`, `McpServer` | `USES_PROMPT`, `HAS_SKILL`, `USES_TOOL`, `CONNECTS_TO`, `Skill`→`USES_TOOL`, `Tool`→`BACKED_BY` |
| Models | `ModelFamily`, `ModelVersion`, `Modality` | `HAS_VERSION`, `SUPPORTS` |

**Demo:** library collections (`databases`, `datasets`, `prompts`, `skills`, `tools`, `mcp-servers`, `model-families`, `modalities`) + products (`dp-customers`, `agent-support`, `models-openai`).

Independent of SBOM type catalog (G-X1) — packaging pattern only.

**Implement in:** ontology seed + demo seeder.

### G-P2 — Hybrid persistence + collection metadata

Same hybrid pattern as SBOM D-2: **domain tables for inventory metadata**; **objs for asset content**. Never store object payloads in example tables.

| Layer | Owns |
|-------|------|
| **Domain DB** | `ar_collection` SoT for list/filter; **`ar_collection_type`** (1-*) for accepted types + per-type metadata |
| **objs** | Object payloads, membership, in-collection edges |

**Product columns (`ar_collection`):** `name`, `description`, `owner`, `owner_email`, `support_email`, `sla` (text), **`object_write_mode`** (see G-P3).

**Type rows (`ar_collection_type`):** `collection_id`, `object_type`, **`metadata`** (opaque text/JSON for collection-level type configuration), timestamps. Unique `(collection_id, object_type)`.

**Technical columns (`ar_collection`):** `id`, `graph_id`, timestamps.

Graph header annotations may mirror `owner` / `collection` for matcher convenience. List/filter reads the **domain table**.

**Implement in:** WI-003.

### G-P3 — Object identity / write mode

Controlled per collection via **`object_write_mode`**:

| Mode | Behavior |
|------|----------|
| `UUID` | Missing id → always create; updates require id |
| `IDENTIFIER` | Missing id → resolve by schema **identifier** fields; id honored when present |
| `UUID_OR_IDENTIFIER` | **Default** — UUID path when id present; identifier resolve when absent |

Resolution scope: within that collection’s membership / accepted type. Match counts: 1 → update; 0 → create; many → **409**.

**Delete** is a first-class intent in the write pipeline (resolved id + op=DELETE).

At identity stage, payload need only be valid enough to resolve identity; **full schema validation runs after preprocessing** (or at persist).

**Implement in:** WI-004.

### G-P4 — Write shape (REST)

Prefix e.g. `/api/v1/asset-repository` (OpenAPI group `asset-repository`):

- Collections CRUD-lite (create/list/get/patch metadata)  
- `POST …/collections/{id}/objects` — one object (`id?`, `type`, `version`, `payload`)  
- `POST …/collections/{id}/compositions` — `{ objects: [...], relations: [{sourceKey, role, targetKey}] }` through the write pipeline; local keys resolved after identity resolution  
- List/get objects in collection  
- **Search objects within a collection** (G-P7)  
- Deletes via dedicated endpoint or composition delete ops (exact path in design doc / WI-004)

Public docs use product terms — not foundation `BoM*` DTO names.

**Implement in:** WI-004.

### G-P5 — Write pipeline + extension points

Example-owned pipeline for every collection write. **v1: interfaces + no-op defaults only** (reserve space for future enrichers/events). Lives in `:asset-repository-service`, **not** objs-core.

```text
attempt mutate/create/delete
  → resolve identity (create | update | delete)
  → PreprocessingExtension  (may mutate objects/edges; may add objects/edges)
  → persist to objs graph
  → EventExtension  (collection/object change analysis; selectively emit — v1 emits nothing)
```

- Spring beans / interfaces; ordered lists so future plugs compose without pipeline changes.  
- Preprocessing: after identity, before persist. Newly added batch members must carry resolved ids or in-batch local keys.  
- Events: domain-level only; no broker / async in v1.  
- Out of scope v1: real enrichment, message bus, objs-core hooks.

**Implement in:** WI-004.

### G-P6 — Python producer / consumer client

Python 3 script under `examples/asset-repository/scripts/` (e.g. `ar_client.py`):

| Role | Behavior |
|------|----------|
| **Producer** | Create/patch collections; write objects (with and without UUID); write Database+Dataset composition; optional delete |
| **Consumer** | List/filter collections; list/get objects; print summaries |

Domain OpenAPI only — not `/api/v1/objs/**`. Runnable against local `bootRun`.

**Implement in:** WI-007.

### G-P7 — Search objects within a collection

Domain API + UI to **search objects inside one collection**, implemented with **existing objs matcher DSL** (programmatic `objs-core` — same `obj-expr` / chained matchers as foundation), **scoped to that collection’s `graph_id`** (equivalent to graph-scoped query: members of that named graph only).

| Surface | Behavior |
|---------|----------|
| REST | e.g. `POST …/collections/{id}/objects/search` with matcher body (and optional simple helpers for searchable fields). Returns matching objects in product DTO shape. |
| UI | On collection detail: search control — prefer **simple dynamic filters** from schema **`searchable`** fields for the accepted type(s); advanced/raw matcher optional or deferred. Results replace or filter the object list. |
| Python client | Consumer can call search (G-P6). |

**Locks:**
- Do **not** invent a parallel query language; wrap existing matchers.  
- Scope is **always** the collection graph (never cross-collection in v1).  
- Domain REST hides foundation path names from product docs; implementation uses **programmatic** graph-scoped select/matchers on `objs-core` (equivalent capability to foundation `POST /graphs/{id}/query`) — **never** by HTTP-calling `/api/v1/objs/**` from the app.

**Implement in:** WI-004 (API); WI-005 (UI); WI-007 (client).

## Changelog

| Date | Change |
|------|--------|
| 2026-08-13 | Story scaffold: packaging, object-store locks, open G-P1…G-P4 |
| 2026-08-13 | WI-001 planning: resolve G-P1…G-P6; revise G-A2/G-A6; add G-A7/G-A8; workbench, hybrid metadata, write SPI, Java 21, Python client |
| 2026-08-13 | G-P7: search objects within collection via existing matchers |
| 2026-08-13 | G-P2: accepted types moved to `ar_collection_type` (1-*) with per-type metadata |
| 2026-08-13 | G-P1: library + product asset ontology (data / agents / models); drop SBOM leftover types |
