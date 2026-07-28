# Gaps & clarifications — sbom-typed-example

Open decisions for [`STORY.md`](STORY.md). Normative example behaviour: [`docs/design/sbom/example.md`](../../../design/sbom/example.md).  
Canonical ontology: [`docs/design/sbom/canonical-spec.md`](../../../design/sbom/canonical-spec.md) (v1.0 Draft, 2026-07-28).

**Legend:** `blocking` · `default-ok` · `half-open` · `resolved` · `out of scope`

---

## Typed toolkit / identity

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-S1 | **Where toolkit lives** | resolved | `objs-core` package `org.poc.objs.core.typed` |
| G-S2 | **Payload `id` vs envelope** | resolved | UUID on `BoMEntity.id` / `BoMEdge.id`; not required inside JSON Schema payload |
| G-S3 | **Provisional ids in GraphBuilder** | resolved | Assign UUID when linking if missing; persist as create-with-id |
| G-S36 | **Importer identity merge** | out of scope | Canonical “merge by natural key” — not implemented in this story; client/importer concern |

## Annotations (concrete SBOM app)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-S4 | **Annotation key names** | resolved | `app`, `appVersion`, `source`, `sourceDetail`, `capturedBy`, `origin` |
| G-S5 | **`source` values** | resolved | `manual` \| `detected` \| `enriched` |
| G-S6 | **`manual` requires `capturedBy`** | resolved | Builder validates |
| G-S7 | **`enriched` requires `sourceDetail`** | resolved | Catalog id |
| G-S8 | **Cross-(app,appVersion) edges** | resolved | Builder enforces same context |
| G-S21 | **`origin`** | resolved | Free-form string (`ui`, `batch`, `api`, …) |
| G-S28 | **PUT annotation merge** | resolved | Query defaults; body overrides |
| G-S37 | **`app` vs Product** | resolved | Path/annotation `app` = **application slug** for BOM partition. Canonical **Product** is a typed entity (with its own `version` property). A BOM for slug `payments-api` / `2.3.1` may contain a Product entity whose payload `name`/`version` align with that release, but partition keys remain **annotations** |

## Canonical domain ↔ foundation mapping

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-S33 | **Canonical object → `BoMEntity`** | resolved | `type` → `BoMEntity.type`; schema version `1.0.0` for all types this story; `name`/`description`/`labels`/`attributes` + type-specific fields → **payload**; `id` → envelope. Foundation does **not** use separate columns for name/labels |
| G-S34 | **Canonical edge → `BoMEdge`** | resolved | `role` = relationship name (`DEPENDS_ON`, …). Shared edge property schema **`CanonicalEdge` / `1.0.0`**: optional `createdAt`, `source`, `confidence`, `attributes`. Allow-list **`SCHEMA`** + `emptyPropertiesAllowed=true`. Edge `id` → envelope |
| G-S10 | **Edge property schemas** | resolved | Superseded by G-S34 (was bare `NONE`; now shared `CanonicalEdge`) |
| G-S35 | **Canonical `labels` vs annotations** | resolved | Payload `labels` = domain tags. Selection/partition uses **annotations** (`app`, …). Do not put `app` in payload `labels` |

## SBOM / ontology scope

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-S9 | **Enum value lists** | half-open | Spec allows open strings; stub small enums (or `string`) in JSON Schema — does not block |
| G-S11 | **Licenses on Component** | resolved | Canonical Component has **no** `licenses` list — use `LICENSED_UNDER` → License only |
| G-S12 | **Object type specs** | resolved | Full draft in [`canonical-spec.md`](../../../design/sbom/canonical-spec.md) — see G-S12 detail |
| G-S13 | **Role string casing** | resolved | UPPER_SNAKE per canonical relationship table |
| G-S38 | **Implementation waves** | resolved | Wave A via WI-005; Waves B–D pulled in (G-S39) — full pack in `SbomRegistry` |
| G-S39 | **Waves B–D types/edges** | resolved | Full canonical entity set + all 28 relationship triples registered |

### G-S12 detail — types in the draft

All of the following have Description + Properties + Allowed Relationships in the canonical draft:

**Wave A — SBOM / supply-chain core:**  
Product, Component, Organization, License, Vulnerability, Build  

**Wave B — build & packaging:**  
Source Repository, Source Module, Artifact, Container Image, Container Layer  

**Wave C — runtime & deploy:**  
Runtime, Operating System, Deployment, Environment, Host, Kubernetes Cluster, Namespace  

**Wave D — architecture & compliance:**  
Service, API, Database, Dataset, Policy  

**Registry:** `SbomRegistry.pack()` registers all 23 entity schemas (+ shared `CanonicalEdge`) and the full relationship table (28 triples) from [`canonical-spec.md`](../../../design/sbom/canonical-spec.md).

### G-S38 detail — why waves (historical)

Waves staged delivery: WI-005 shipped Wave A first; Waves B–D were then pulled into the same story (G-S39) so the example app exposes the entire draft ontology.

---

## Persistence / API

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-S14 | **Storage** | resolved | Foundation only |
| G-S15 | **Fetch by app + version** | resolved | `SbomService` + `/api/v1/example/sbom` |
| G-S16 | **SBOM REST** | resolved | See G-S16 detail |
| G-S17 | **Catalog durability** | out of scope | In-memory pack; **C-3** later |
| G-S22 | **GET without version** | resolved | All versions for `app` |
| G-S40 | **List apps + versions** | resolved | `GET /api/v1/example/sbom/apps` → `SbomApplicationCatalog` |
| G-S41 | **Graph explorer SPA** | resolved | `/ui/` in example module; queries foundation `GET /api/v1/objs/graph` by annotation JSON |
| G-S23 | **PUT semantics** | resolved | Upsert batch only |
| G-S24 | **Extra annotation params** | resolved | Open query map; Swagger `additionalProp*` placeholders ignored (`SbomQueryAnnotations`) |
| G-S25 | **REST base path** | resolved | `/api/v1/example/sbom` |
| G-S29 | **Body shape** | resolved | `BoMGraph` |
| G-S30 | **Module web dependency** | resolved | Web deps in WI-004 |

### G-S16 detail — SBOM REST

| Method | Path | Behaviour |
|--------|------|-----------|
| `GET` | `/api/v1/example/sbom/apps` | Distinct applications + sorted version lists (from `app` / `appVersion` annotations) |
| `GET` | `/api/v1/example/sbom/apps/{appId}` | `{app}` (+ optional annotation query params) |
| `GET` | `/api/v1/example/sbom/apps/{appId}/versions/{version}` | `{app, appVersion}` (+ optional params) |
| `PUT` | `/api/v1/example/sbom/apps/{appId}/versions/{version}` | Body `BoMGraph`; path identity; query defaults / body overrides |

---

## Ready vs blocked

| WIs | Clarifications |
|-----|----------------|
| **WI-001–004** | Ready (Component + `DEPENDS_ON`; edge policy per G-S34 when toolkit/edges land) |
| **WI-005–006** | Done; full ontology (G-S39) registered in example module |

---

## Intentionally out of scope

| # | Topic | Notes |
|---|--------|-------|
| G-S18 | Auth / RBAC | |
| G-S19 | Business rules beyond schema + allow-list | No merge engine, acyclicity, policy engines |
| G-S20 | Edge annotations | Foundation: entities only; edge metadata → edge **properties** (G-S34) |
| G-S26 | Full replace / prune on PUT | |
| G-S27 | Version-index-only API | |
| G-S31 | Closed `origin` enum | |
| G-S32 | Typed SBOM DTO ≠ `BoMGraph` | |
