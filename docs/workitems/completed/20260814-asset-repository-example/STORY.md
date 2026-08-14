# Story: Asset repository example

**Slug:** `asset-repository-example`  
**Branch:** `asset-repository-example`  
**Status:** completed  
**Folder:** [`docs/workitems/completed/20260814-asset-repository-example/`](.)  
**Backlog:** [D-3](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Design (product):** [`docs/design/asset-repository/example.md`](../../../design/asset-repository/example.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Build a **second concrete objs example** under [`examples/asset-repository/`](../../../examples/asset-repository/) — **`:asset-repository-service`** (Java 21 Boot) + **`:asset-repository-service-ui`** (domain SPA) — demonstrating objs as a **centralized object store**.

Asset information is scattered across systems and formats. This app holds that information in **collections** of typed **objects**, with domain REST, a domain UI (explore / search / create / edit), foundation **workbench** for schemas, and a Python producer/consumer client.

**Independent of** [`sbom-inventory-app`](../../in-progress/sbom-inventory-app/STORY.md) (D-2): packaging pattern and adapted type seeds only — no shared modules.

## Normative locks

| Topic | Lock |
|-------|------|
| Module | **`examples/asset-repository/asset-repository-service`** + **`…/asset-repository-service-ui`** |
| Language | Example service **Java 21 only — no Kotlin**. UI TypeScript/React. Foundation stays Kotlin |
| objs usage | **`objs-core` programmatic** for all domain reads/writes. **`objs-service` + workbench** included as **sidecar only** (schema UI at `/workbench/`) — domain code/UI/Python **must not** call `/api/v1/objs/**`. Gremlin not required for domain v1 |
| Packaging | `objs-app` = foundation only (no dependency on this example) |
| objs role | **Object store.** Collection → named graph; objects → pool entities + membership |
| Graphs / edges | Mostly membership containers; edges only for related-type collections (e.g. Database `CONTAINS` Dataset). No cross-collection edges |
| Accepted types | Collection declares **`accepted_types`**; only those types may be written/created (REST + UI) |
| Hybrid persistence | Domain table **`ar_collection`** for metadata; **object payloads only in objs** (same pattern as SBOM D-2) |
| Collection metadata | `name`, `description`, `owner`, `owner_email`, `support_email`, `sla` (text), **`object_write_mode`**, plus `graph_id`; accepted types in **`ar_collection_type`** (1-*, optional per-type metadata) |
| Identity | UUID and/or schema **identifier** resolve per `object_write_mode` (`UUID` / `IDENTIFIER` / `UUID_OR_IDENTIFIER`) |
| Write pipeline | Resolve identity → **PreprocessingExtension** → persist → **EventExtension** (dummy/no-op SPIs in v1) |
| Search | Within collection via **existing objs matchers** scoped to `graph_id` (G-P7) |
| Ontology seed | Data / AI-agent / model asset types; library + product collections (see G-P1) |
| Surfaces | Domain REST + OpenAPI; domain SPA; workbench for schema; Python client. **No CLI** |
| Audience | Integrators and operators; domain UI hides foundation vocabulary |
| Forbidden | Shared SBOM Gradle modules; CycloneDX/SPDX product; live ingest connectors; auth / multi-tenant |

See [`GAPS.md`](GAPS.md) for full resolved detail (G-A*, G-P1…G-P7, G-X1).

## User journeys (v1 scope)

### Journey 1 — Collection owner

1. Create a **collection** (name, description, owner, emails, SLA, accepted types, `object_write_mode`)  
2. List/filter collections (name, owner, accepted type)  
3. Update collection metadata  
4. Manage **schemas** via workbench (`/workbench/`) when types need authoring  

### Journey 2 — Writer (API / Python producer)

1. Write an **object** into a collection (UUID and/or identifier resolution)  
2. Write a **composition** (objects + in-collection edges) for related-type collections  
3. Search / list / get objects in a collection  
4. Optional delete through the write pipeline  

### Journey 3 — Explorer / editor (domain UI)

1. Browse collections; open a collection  
2. **Search** objects (searchable-field filters → matcher-backed API)  
3. Inspect object payload (+ sparse relations)  
4. **Create / edit** objects via **dynamic forms** (accepted types only)  
5. Link to workbench for schemas  

## Out of scope (this story)

- Auth / multi-tenant / real RBAC  
- Cross-collection relations or search  
- Live connectors to source systems (ingest via this app’s REST / Python client)  
- SBOM / portfolio / MI features from D-2  
- Real preprocessing enrichers or event bus (SPI stubs only)  
- Domain SPA schema authoring (use workbench)  
- Using foundation **`/api/v1/objs/**`** from domain services, domain UI, or Python client (sidecar only)  

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Docs + trackers |
| 1 — Design | WI-001 | done | Product design; GAPS locked |
| 2 — Packaging | WI-002 | done | Java 21 service + UI shell; objs-service |
| 3 — Domain + ontology | WI-003 | done | `ar_collection` + `ar_collection_type`; SBOM-adapted seed types |
| 4 — REST | WI-004 | done | CRUD, writes, search, write SPI, OpenAPI |
| 5 — Domain UI | WI-005 | done | Explore, search, dynamic forms, workbench link |
| 6 — Seeds + docs | WI-006 | done | Demo data; README |
| 7 — Python client | WI-007 | done | Producer + consumer script |

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Product design + REST sketch (`WI-001-product-design.md`)
- [x] WI-002 — Module packaging under `examples/asset-repository/` (`WI-002-module-packaging.md`)
- [x] WI-003 — Collection registry + asset type ontology (`WI-003-domain-ontology.md`)
- [x] WI-004 — Domain REST API + OpenAPI (`WI-004-rest-api.md`)
- [x] WI-005 — Domain UI: explore, search, create/edit (`WI-005-explore-ui.md`)
- [x] WI-006 — Seeds, demo data, docs (`WI-006-seeds-docs.md`)
- [x] WI-007 — Python producer / consumer client (`WI-007-python-client.md`)

## Acceptance (story)

- [x] Runnable `:asset-repository-service` (Java 21) with domain UI + workbench; `:objs-app` foundation-only  
- [x] Journeys 1–3 supported (API + domain UI + workbench for schema)  
- [x] Hybrid persistence: `ar_collection` metadata; objects in objs pool/graphs  
- [x] Accepted-types gate; identity modes; compositions for related types  
- [x] Collection-scoped search via existing matchers  
- [x] Dummy write pipeline extensions wired  
- [x] Python producer/consumer exercises domain REST  
- [x] Design docs updated; end-to-end objs-as-object-store demo  

## Process notes

1. One WI at a time; mark `[x]` before next; one commit + push per WI.  
2. Prefer example-layer solutions; escalate platform holes into `GAPS.md`.  
3. Domain UI stays clear (WI-005); schema management via workbench link.  
4. Never call foundation REST from the application path (G-A2 sidecar rule).  
5. Story archived 2026-08-14.  
