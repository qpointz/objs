# SBOM applications inventory

**Status:** completed — story [`sbom-inventory-app`](../../workitems/completed/20260816-sbom-inventory-app/STORY.md) (D-2)  
**Modules:** `:sbom-service` + `:sbom-service-ui` under [`examples/sbom/`](../../../examples/sbom/)  
**Run:** `./gradlew :sbom-service:run` → UI **`http://localhost:8080/ui/`** (demo seeds via `demo` profile)  
**Engineer mapping:** [`GRAPH-AND-RETRIEVAL.md`](../../workitems/completed/20260816-sbom-inventory-app/GRAPH-AND-RETRIEVAL.md)  
**Ontology:** [`canonical-spec.md`](canonical-spec.md) (reuse; extend only when journeys require)

This document is the **product** design for the inventory app. End-user UI and **domain** API use the glossary below — never graph / entity / edge / matcher vocabulary.

Foundation REST (`/api/v1/objs/**`) and the workbench are the **`:objs-app` side service**
(port **8081**). This product **must not** depend on or call them.

---

## Personas and chrome

Visual split only (**no auth / roles**). Keep the UI **clean and obvious**: two top tabs, one primary job per screen.

| Tab | Persona | Owns |
|-----|---------|------|
| **Applications** | Application owner | Applications, edit drafts, versions, assets, CycloneDX export |
| **Portfolios** | Portfolio owner | Portfolio taxonomy + **MI reports only here** |

---

## Glossary

| Term | Meaning |
|------|---------|
| **Application** | Named software product/system in the inventory (domain row). |
| **Edit draft** | Working BOM for an application. Points at its own objs graph. Mutable. |
| **Application version** | Captured release of an application’s BOM. Points at its **own** objs graph (typically copied from the draft). Immutable for inventory purposes. |
| **Latest version** | The most recent application version for an app (ordering locked in WI-007 / R22). Used for all portfolio MI graphs. |
| **Asset** | A reusable software object in the global pool (e.g. component, library) that can appear in many BOMs. |
| **Relation** | How two assets connect in a BOM (user sees a friendly label; API may also carry the canonical role code). |
| **Owning application** | Optional owner of an asset (`owner` annotation = application **name**). |
| **Depends on (app)** | **Inferred** link: application A depends on B when they share assets in their BOM graphs. Not authored as an app→app relation. |
| **Shared asset** | An asset that appears in more than one application’s BOM (within the current scope). |
| **Duplicate** | Possible same asset found via schema **identifier** fields. v1 is **find-only** (list + navigate). |
| **Portfolio** | Taxonomy that groups **applications** (not versions) for management views. Domain-only. |
| **Subject area** | Folder node in a portfolio tree. |
| **Portfolio level** | Selected subject-area node **or** the portfolio root. Defines which applications a report covers. |
| **MI report** | Management information report. Portfolio owner only. Always scoped by portfolio → level. |
| **CycloneDX export (demo)** | Weak export of a draft or version BOM to CycloneDX-shaped JSON. Demo of “same BOM, different format” — not a certified exporter. |

---

## Journeys (v1)

### Journey 1 — Application owner

1. Search applications  
2. Edit **edit draft**: add/remove assets; amend relations (reuse from pool or create → pool)  
3. Create an **application version** (new graph, usually from draft)  
4. See **depends on (app)** inferred from shared assets  
5. Export draft/version as weak CycloneDX  

No portfolios or MI here.

### Journey 2 — Assets inventory

Under **Applications** chrome.

1. Search assets by type  
2. Advanced search: dynamic form from schema — **`searchable` fields only**  
3. Inspect asset: which applications use it, and how  
4. Find possible **duplicates** (find-only)  
5. Optional **owning application**

### Journey 3 — Portfolio owner

1. Maintain portfolios / subject areas; place applications (once per portfolio)  
2. Run MI:

```text
Select portfolio → select level → select report → Run → results
```

| ID | Report | Answers (in-scope apps / latest versions) |
|----|--------|-------------------------------------------|
| MI-1 | Portfolio composition | Apps; asset counts by type; relation density |
| MI-2 | Application dependency map | Inferred app→app deps within the selected set |
| MI-3 | Shared asset hotspots | Assets in multiple in-scope apps |
| MI-4 | Duplicate & risk signals | Identifier duplicates + lightweight risk signals |

**Graphs for MI:** latest version only. Apps with no version are omitted from graph selection (may still list as “no version” in MI-1). Drafts are never used for MI.

---

## Hybrid persistence

| Layer | Owns |
|-------|------|
| **Domain tables** (`sbom-service`) | Application, edit draft, application version, portfolio + subject areas + membership |
| **objs graphs** (`objs-core`) | Assets + relations inside each draft/version graph |

Rules:

- Each draft and each version has its **own** `graph_id`.  
- Portfolios are **domain-only** (no portfolio graph).  
- Assets live in the objs **pool**; BOM membership is per draft/version graph.  
- Domain tables must not become a parallel asset store.

See story [`GRAPH-AND-RETRIEVAL.md`](../../workitems/completed/20260816-sbom-inventory-app/GRAPH-AND-RETRIEVAL.md).

---

## Domain services (sketch)

| Service | Responsibility |
|---------|----------------|
| `ApplicationInventoryService` | Applications + edit draft BOM |
| `ApplicationVersionService` | Version create/get; latest-version helper (R22) |
| `AssetInventoryService` | Pool search, usage, duplicates, owner |
| `PortfolioService` | Taxonomy + membership; R21 app set for a level |
| `CycloneDxExportService` | Weak CDX from draft/version |
| `MiReportService` | MI-1…MI-4; portfolio → level → latest graphs → Gremlin |

---

## Domain API sketch (product language)

Base path (evolving): `/api/v1/inventory/...`  
Public shapes use glossary terms — **no** `BoM*` / graph / matcher names.

Illustrative routes (WI-006+; exact paths evolve with later WIs):

| Method | Path | Notes |
|--------|------|-------|
| GET | `/applications?q=` | Search / list |
| POST | `/applications` | Create app + empty edit draft |
| GET/PUT | `/applications/{id}` | Metadata |
| GET | `/applications/{id}/draft` | Edit draft BOM (assets + relations) |
| POST | `/applications/{id}/draft/assets` | Reuse (`assetId`) or create (`type`+`payload`) |
| DELETE | `/applications/{id}/draft/assets/{assetId}` | Detach from draft |
| POST/DELETE | `/applications/{id}/draft/relations…` | Amend relations |
| GET | `/applications/{id}/depends-on` | Inferred shared-asset deps (draft scope until WI-007) |
| POST | `/applications/{id}/versions` | Capture version from draft (WI-007) |
| GET | `/applications/{id}/versions/{versionId}` | Version BOM + inferred deps |
| GET | `/assets?type=` | List by type |
| POST | `/assets/search` | Type + searchable field filters |
| POST | `/assets` | Create pool asset (optional owner name) |
| GET | `/assets/{id}` | Detail + usage (draft/version scan stopgap) |
| PUT | `/assets/{id}/owner` | Set/clear owning application name |
| GET | `/assets/duplicates?type=` | Find-only identifier groups |
| CRUD | `/portfolios…` | Tree + place applications (WI-011) |
| POST | `/portfolios/{id}/reports` | Body: `{ "level": "root"\|nodeId, "report": "MI-1"… }` |
| GET | `/applications/{id}/draft/export/cyclonedx` | Weak demo |
| GET | `/applications/{id}/versions/{versionId}/export/cyclonedx` | Weak demo |

OpenAPI is published for these domain endpoints on `:sbom-service`.

**Transitional:** until later WIs replace it, the legacy façade `/api/v1/example/sbom/**` may still exist for demos/scripts — not the product vocabulary target.

---

## Relation labels

Users see **friendly** labels (beautifier of role codes, e.g. `DEPENDS_ON` → “Depends on”). Optional small override map where needed. APIs may include both `role` and `label`.

---

## Out of scope (product)

- Auth / multi-tenant  
- Object/component versioning lifecycle  
- Foundation workbench / `/api/v1/objs/**` on this app (side service `:objs-app` only)  
- CycloneDX/SPDX **import**; strong/certified export  
- Custom BI builder / scheduled reporting  

---

## Ontology and object model (WI-005)

| Concern | Lock |
|---------|------|
| Asset types / relations | Reuse canonical ontology seed (`seeds/sbom-ontology.yaml`) |
| Runtime UI / search forms | Read **`BoMSchemaCatalog`** via `GET /api/v1/inventory/asset-types` (`searchable` only) |
| Typed Wave* / `SbomRegistry` | Builder parity helpers — **not** SoT |
| Owning application | Annotation `owner` = application **name** |
| App→app depends | Inferred shared assets — no ApplicationRef type |
| Portfolios | Domain tables only |

Full schema→Kotlin codegen remains optional when the modeling team supplies generators; until then seeds + catalog are authoritative for product behaviour.

---

## Related

- Story + gaps: [`sbom-inventory-app`](../../workitems/completed/20260816-sbom-inventory-app/STORY.md)  
- Canonical ontology: [`canonical-spec.md`](canonical-spec.md)  
- Seeds: [`../graph/seeds.md`](../graph/seeds.md)  
- Engineer mapping: [`GRAPH-AND-RETRIEVAL.md`](../../workitems/completed/20260816-sbom-inventory-app/GRAPH-AND-RETRIEVAL.md)  
- Foundation workbench (separate app): [`../ui.md`](../ui.md) · `:objs-app`  
