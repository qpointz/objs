# Software BOM example

**Status:** completed (story [`sbom-typed-example`](../../workitems/completed/20260728-sbom-typed-example/STORY.md))  
**Module:** `objs-sbom-example` (**concrete app**)  
**Foundation:** low-level entity graph (`BoMEntity` / `BoMEdge` / `BoMGraphStore` / generic REST) — see [`../graph/`](../graph/README.md)  
**Ontology:** [`canonical-spec.md`](canonical-spec.md) (v1.0 Draft)

## Layering

| Layer | What it is | Examples |
|-------|------------|----------|
| **Foundation** | Generic, domain-agnostic graph platform | Typed JSON entities, role edges, annotation subgraphs, schema/allow-list catalogs, `BoMGraphStore`, `/api/v1/objs/**` |
| **Canonical ontology** | Technology-neutral software graph types & relationships | Product, Component, Build, … — [`canonical-spec.md`](canonical-spec.md) |
| **Concrete app (this example)** | SBOM product on that ontology + foundation | `app`/`appVersion`/provenance/`origin` annotations, `SbomService`, `/api/v1/example/sbom` |

### Mapping (summary)

| Canonical | objs foundation |
|-----------|-----------------|
| Object `id` / `type` | `BoMEntity.id` / `BoMEntity.type` (schema `1.0.0`) |
| `name`, `description`, `labels`, `attributes`, type fields | Entity **payload** |
| Edge `id` / relationship name | `BoMEdge.id` / `BoMEdge.role` |
| Edge `createdAt`, `source`, `confidence`, `attributes` | Edge **properties** via shared schema `CanonicalEdge` |
| Multi-app BOM partition | Entity **annotations** `app`, `appVersion`, … (not payload) |

**Ontology coverage:** classpath seed `seeds/sbom-ontology.yaml` registers the **entire** draft ontology (Waves A–D): 23 entity types + shared `CanonicalEdge` + all 28 relationship triples from [`canonical-spec.md`](canonical-spec.md). Typed `SbomRegistry.pack()` remains the parity/builder companion.

## Problem

An organisation tracks dependencies and related metadata for **many applications**. Each application has a string **slug** id. Each application release has its **own SBOM** (app version). Objects in an SBOM may arrive from different pipelines:

| Source | Meaning |
|--------|---------|
| **manual** | Captured by a user |
| **detected** | Inferred from application sources (scans, lockfiles, builds) |
| **enriched** | Filled from IT portfolio / catalog systems (multiple catalogs possible) |

The foundation stays domain-agnostic. The **concrete app** maps this domain onto typed payloads + annotations + allow-listed edges, persists via the foundation, fetches by application (and optionally version), and exposes `/api/v1/example/sbom`.

### Gremlin / Query smoke

`:objs-sbom-example` ontology and APIs are unchanged by Gremlin. When `:objs-app` runs with the
**sbom** profile (demo graph seeded), use workbench **Query** or
`POST /api/v1/objs/graph/traverse/gremlin` with a matcher such as `{ "anno": { "app": "app-00001" } }`
and scripts like `g.V().hasLabel('Service', 'Policy')`. Design: [`../graph/gremlin.md`](../graph/gremlin.md).

---

## Architecture

```mermaid
flowchart TB
  rest[SbomController /api/v1/example/sbom]
  domain[SbomGraphBuilder + typed Component]
  graph[BoMGraph]
  store[BoMGraphStore]
  db[(bom_graph_entity / bom_graph_edge)]
  fetch[SbomService]
  rest --> fetch
  domain --> graph
  graph --> fetch
  fetch --> store
  store --> db
```

| Layer | Responsibility |
|-------|----------------|
| **Payload** | Business fields of a graph object (e.g. Component name, version, purl) — JSON Schema validated |
| **Annotations** | BOM identity, provenance, and **caller channel** (`origin`) — used for subgraph selection (match-all) |
| **Edges** | Relationships (`DEPENDS_ON`, …) — allow-list; not annotated (foundation rule) |
| **Storage** | Only foundation entity/edge tables — **no** parallel SBOM schema |
| **REST** | Thin SBOM façade (`/api/v1/example/sbom`) over `SbomService` — not a second persistence model |

Reusable conversion/assembly helpers live in `objs-core` (`org.poc.objs.core.typed`); SBOM vocabulary lives only in `objs-sbom-example`.

---

## Annotations (domain → selection keys)

All values are **strings**. Keys are caller vocabulary (opaque to the store).

| Key | Required | Description |
|-----|----------|-------------|
| `app` | yes (on write) | Application id (**slug**), e.g. `payments-api` |
| `appVersion` | yes (on write of a versioned BOM) | Application / SBOM version, e.g. `2.3.1` |
| `source` | yes (typical) | How the object was captured: `manual` \| `detected` \| `enriched` |
| `sourceDetail` | when applicable | Qualifier: for `enriched`, the catalog id (`catalog1`, `catalog2`, …) |
| `capturedBy` | when `source=manual` | User who captured the object |
| `origin` | optional | **Caller channel** for the write: e.g. `ui`, `batch`, `api` — simulates different integration paths; distinct from `source` |

### Provenance rules (`source`)

| `source` | Extra annotations |
|----------|-------------------|
| `manual` | **`capturedBy` required**; `sourceDetail` optional |
| `detected` | `sourceDetail` optional (scanner / pipeline id) |
| `enriched` | **`sourceDetail` required** (enrichment catalog id) |

### Caller channel (`origin`)

`origin` answers *which system invoked the update* (UI form, nightly batch, external API), not *how the component fact was obtained* (`source`). Both may be set on the same entity. PUT SBOM accepts `origin` (and other free-form annotation query params) so tests and demos can filter later by caller.

Example entity annotations for a manually added component in payments-api 2.3.1:

```text
app=payments-api
appVersion=2.3.1
source=manual
capturedBy=alice
```

### Why annotations (not payload)

- Same Component **type** appears in many apps/versions; membership is contextual.
- `POST /api/v1/objs/graph/query` selects by matcher DSL (`anno`, `anno-expr`, or chained).
+ `POST /api/v1/objs/graph/query` selects by matcher DSL (`anno`, `anno-expr`, or chained).
- Payload stays aligned with supply-chain object schemas; partition keys stay orthogonal.

---

## Fetch and update via SBOM REST

Base path: **`/api/v1/example/sbom`** — the `/example/` segment flags this as the concrete demo app, distinct from foundation `/api/v1/objs/**`. Persistence still goes through `BoMGraphStore`.

| Method | Path | Behaviour |
|--------|------|-----------|
| `GET` | `/apps` | Distinct applications and their versions (sorted), from entity `app` / `appVersion` annotations |
| `GET` | `/apps/{appId}` | Induced subgraph for annotation filter `{app=appId}` — **all versions** for that app. Optional extra query annotations narrow the slice (e.g. `origin=batch`, `source=manual`). |
| `GET` | `/apps/{appId}/versions/{version}` | Subgraph for `{app, appVersion=version}` (+ optional extra annotation query params). |
| `PUT` | `/apps/{appId}/versions/{version}` | Upsert body **`BoMGraph`** (`entities` + `edges`, same as foundation `/graph`). Path sets `app` / `appVersion`. Query annotation params are **defaults**; **body entity annotations override** on conflict. Free-form `origin` (e.g. `ui`, `batch`) and other keys supported. Upsert-only — does not delete omitted objects. |

### Examples

```http
GET /api/v1/example/sbom/apps
GET /api/v1/example/sbom/apps/payments-api
GET /api/v1/example/sbom/apps/payments-api/versions/2.3.1
GET /api/v1/example/sbom/apps/payments-api/versions/2.3.1?source=manual
GET /api/v1/example/sbom/apps/payments-api/versions/2.3.1?origin=batch

PUT /api/v1/example/sbom/apps/payments-api/versions/2.3.1?origin=ui&source=detected
Content-Type: application/json

{ "entities": [ … ], "edges": [ … ] }
```

**PUT semantics:** body is foundation `BoMGraph`; upsert batch only (no prune). Query annotations default onto entities; body annotations win on key conflict.

**Query annotations:** optional filters/defaults (`source`, `origin`, `sourceDetail`, `capturedBy`, or any other key). Leave them blank in Swagger — do not send placeholder `additionalProp*` values; those are ignored server-side because they would otherwise empty match-all GETs.

**Service layer:** `SbomService.listApplications()`, `getSbom(app, appVersion?)`, `save(…)`, plus filter overlays for extra annotations. Controllers delegate here.

**Generic foundation REST** remains available (`POST /api/v1/objs/graph/query` with matcher DSL) for low-level access; SBOM REST is the domain-facing API.

### Isolation guarantee

One store holds many apps and versions. After writing fixtures for ≥2 apps × ≥2 versions, versioned GET returns **only** that BOM’s entities and induced edges.

---

## Graph objects (canonical)

All types and the relationship table live in [`canonical-spec.md`](canonical-spec.md). Example: Component requires `name`, `version`, `ecosystem`, `kind`; licenses via edge `LICENSED_UNDER` only.

Edges use shared **`CanonicalEdge`** properties (`createdAt`, `source`, `confidence`, `attributes`); empty properties allowed. Allow-list matches the canonical relationship table exactly.

---

## Builder sketch

```kotlin
val ctx = SbomContext(app = "payments-api", appVersion = "2.3.1")
val g = SbomGraphBuilder(ctx)
    .add(springBoot, Provenance.detected())
    .add(customLib, Provenance.manual(capturedBy = "alice"))
    .dependsOn(springBoot, jackson)
    .build()

sbomService.save(g)
val bom = sbomService.getSbom("payments-api", "2.3.1")
```

`SbomContext` supplies default `app` / `appVersion` annotations on every add; `Provenance` supplies `source` (+ `capturedBy` / `sourceDetail`).

---

## Module boundaries

| Module | Contents |
|--------|----------|
| `objs-core` | Typed toolkit (`TypedEntity`, `GraphBuilder`, `RegistryPack`, …) |
| `objs-service` | Foundation REST + Boot autoconfig + **workbench SPA** at **`/workbench/`** |
| `objs-sbom-example` | Full canonical types (A–D), annotation vocabulary, `SbomService`, registry pack, **`/api/v1/example/sbom` REST** |
| `objs-app` | Depends on example module for demo; ontology + optional demo graph via shared seed pipeline |

### Workbench SPA

Open **`http://localhost:8080/workbench/`** (packaged with `:objs-service`). Routes:
See the user-level [`Objs UI manual`](../ui.md) for complete operating instructions.

| Path | View |
|------|------|
| `/workbench/explorer` | Explorer — annotation query, canvas, selection inspector |
| `/workbench/model` | Schema — entity / edge-property catalogs, allowed edges, DSL + JSON Schema |
| `/workbench/composer` | Composer — draft graph mutation (Visual/Text), Validate / Apply |

Legacy `/ui/**` redirects into `/workbench/**`. Explorer type badges open Schema; **Create version**
uses a base-version + new-version dialog and saves via `PUT` to the exact version.

Dev: `cd objs-service/ui && npm install && npm run dev` (Vite proxies `/api` → `:8080`).  
Build: Gradle `:objs-service` builds the UI into `static/ui/` unless `-PskipUi=true`. The SPA does not require `:objs-sbom-example`.

### Python client script

`objs-sbom-example/scripts/random_sbom_crud.py` (stdlib only) talks to a running app (default **`http://localhost:8080`**):

| Action | API |
|--------|-----|
| Create / update | `PUT /api/v1/example/sbom/apps/{app}/versions/{version}` |
| Retrieve | `GET` same SBOM paths |
| List apps | `GET /api/v1/example/sbom/apps` |
| Delete | `DELETE /api/v1/objs/graph` (`entityIds` / `edgeIds`) |

```bash
python objs-sbom-example/scripts/random_sbom_crud.py apps
python objs-sbom-example/scripts/random_sbom_crud.py bulk
python objs-sbom-example/scripts/random_sbom_crud.py bulk --apps 100 --max-versions 5
python objs-sbom-example/scripts/random_sbom_crud.py bulk --tiny --apps 20000 --max-versions 30
python objs-sbom-example/scripts/random_sbom_crud.py bulk --app-number 500 --max-versions-per-app 10
python objs-sbom-example/scripts/random_sbom_crud.py demo
python objs-sbom-example/scripts/random_sbom_crud.py seed --app demo-app --entities 24 --edges 18
python objs-sbom-example/scripts/random_sbom_crud.py get --app demo-app --version 1.0.0 --summary
```

`bulk` defaults to **20 000 apps**, each with a random **1–30** versions; **each version is a random ontology graph** (8–20 entities, 6–16 allow-listed edges by default — same generator as `seed`/`demo`). Use `--tiny` for the old 2-node Product→Component stub when you only need partition volume. 16 concurrent workers.

---

## Out of scope for this example

- Auth / multi-tenant security
- Full replace / prune of an SBOM on PUT (upsert-only)
- Importer merge-by-natural-key
- BOM analytics beyond schema + allow-list validation

---

## Related

- Canonical ontology: [`canonical-spec.md`](canonical-spec.md)
- Story: [`docs/workitems/completed/20260728-sbom-typed-example/`](../../workitems/completed/20260728-sbom-typed-example/STORY.md)
- Gaps: [`GAPS.md`](../../workitems/completed/20260728-sbom-typed-example/GAPS.md)
- Annotations design: [`../graph/annotations-and-subgraphs.md`](../graph/annotations-and-subgraphs.md) (ephemeral annotation selection + soft-link packs; this example does **not** ship a pack demo seed)
- Persistence / lazy reads: [`../graph/persistence.md`](../graph/persistence.md)
- Typed toolkit (when written): [`../graph/typed-domain.md`](../graph/typed-domain.md)
