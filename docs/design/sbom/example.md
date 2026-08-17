# SBOM applications inventory

**Status:** living — D-2 inventory plus [`multi-bom-app-versions`](../../workitems/in-progress/multi-bom-app-versions/STORY.md) (D-8: multi-BOM versions, parallel drafts, tags, fingerprints)  
**Modules:** `:sbom-service` + `:sbom-service-ui` under [`examples/sbom/`](../../../examples/sbom/)  
**Run:** `./gradlew :sbom-service:run` → UI **`http://localhost:8080/sbom/`** (demo seeds via `demo` profile)  
**Engineer mapping:** [`GRAPH-AND-RETRIEVAL.md`](../../workitems/in-progress/multi-bom-app-versions/GRAPH-AND-RETRIEVAL.md)  
**Gaps:** [`GAPS.md`](../../workitems/in-progress/multi-bom-app-versions/GAPS.md)  
**Ontology:** [`canonical-spec.md`](canonical-spec.md) (reuse; extend only when journeys require)

This document is the **product** design for the inventory app. End-user UI and **domain** API use the glossary below — never graph / entity / edge / matcher vocabulary.

Foundation REST (`/api/v1/objs/**`) and the workbench are the **`:objs-service-app` workbench runner**
(port **8081**). This product **must not** depend on or call them.

---

## Personas and chrome

Visual split only (**no auth / roles**). Keep the UI **clean and obvious**: two top tabs, one primary job per screen.

| Tab | Persona | Owns |
|-----|---------|------|
| **Applications** | Application owner | Applications, versions (incl. parallel drafts), BOMs, Combined SBOM, fingerprints |
| **Portfolios** | Portfolio owner | Portfolio taxonomy + **MI reports only here** |

---

## Glossary

| Term | Meaning |
|------|---------|
| **Application** | Named software product/system in the inventory (domain row). Metadata: name, description, **tags**. |
| **BOM** | One **incomplete** bill for a version (a constituent). Own named graph + name, description, **tags**. Default name `BOM` (hidden when the version has only one). |
| **Combined SBOM** | Complete union of **all** BOMs of a version (**select all**). **Ephemeral** (not stored on the version). Read-only. Combined **tags** show **below the application name** in view mode (unique App ∪ version ∪ all BOMs). |
| **Application version** | DRAFT or RELEASED row. While DRAFT, `version` is the **target** semver (required; may rename). Own **tags**. Many DRAFTs per app are allowed. Each version has **1..*** BOMs. |
| **Based on** | Lineage of a subsequent draft: another **version** (RELEASED or DRAFT) or a **fingerprint**. Bootstrap draft on new application has none. |
| **Fingerprint** | Immutable snapshot of the **Combined SBOM** union (persisted graph). **name** + **category** (`approval` \| `history` \| `unknown`). |
| **Latest version** | Highest **RELEASED** by **SemVer 2.0** (`version_serial`). Drafts never latest. Used for portal content, MI, depends-on, CDX-of-latest. |
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
| **CycloneDX export (demo)** | Weak export of a Combined SBOM (or fingerprint snapshot) to CycloneDX-shaped JSON. UI link hidden on application detail. |

---

## Journeys (v1)

### Journey 1 — Application owner

1. Search applications (portal: latest RELEASED + multi-BOM cue in content; footer totals all BOMs · all versions, lazy per app)  
2. **New application:** name, description, **required target version**, tags → lands on that DRAFT (one empty BOM named `BOM`, name hidden)  
3. Open a version. Count = 1: same chrome as a single BOM (app/version tags visible; **Create BOM** on overview). Count ≥ 2: Combined SBOM (select all, read-only) + left-pane **multi-select** of BOMs  
4. Edit a **single selected BOM** when the version is DRAFT (assets/relations). Combined SBOM and multi-select unions are read-only  
5. **Create BOM** / delete BOM (not last; not Combined) on overview. Shrink to 1: multi chrome off immediately  
6. **New draft:** based-on (RELEASED, DRAFT, or fingerprint) + target. If based-on version has >1 BOM, ask whether to combine into a single BOM  
7. **Promote:** re-type version to confirm (may override target). **Fingerprint:** header Button → name + category; always Combined SBOM snapshot  
8. See **depends on (app)** inferred from shared assets on the **latest RELEASED** Combined SBOM  

No portfolios or MI here. CycloneDX download is hidden on application detail (export API may remain).

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

**Graphs for MI:** **latest RELEASED** Combined SBOM (ephemeral union of that version’s BOMs). Apps with no RELEASED are omitted from graph selection (may still list as “no version” in MI-1). Drafts are never used for MI.

---

## Hybrid persistence

| Layer | Owns |
|-------|------|
| **Domain tables** (`sbom-service`) | Application, version (incl. tags, based-on, `version_serial`), BOM rows (`sbom_application_sbom`), fingerprint metadata, portfolio + subject areas + membership |
| **objs graphs** (`objs-core`) | Each **BOM** has a named graph. Combined SBOM is computed at read time. Fingerprint stores a snapshot graph. |

Rules:

- Combined SBOM is **not** a `graph_id` on the version.  
- Portfolios are **domain-only** (no portfolio graph).  
- Assets live in the objs **pool**; BOM membership is per BOM graph.  
- Tags live on domain rows only (not in graphs / fingerprint hash).  
- Domain tables must not become a parallel asset store.

See [`GRAPH-AND-RETRIEVAL.md`](../../workitems/in-progress/multi-bom-app-versions/GRAPH-AND-RETRIEVAL.md).

---

## Domain services (sketch)

| Service | Responsibility |
|---------|----------------|
| `ApplicationInventoryService` | Applications + tags; bootstrap DRAFT + empty BOM |
| `ApplicationVersionService` | Multi-draft create/promote/rename/delete; BOM CRUD; ephemeral Combined SBOM union; fingerprints; latest RELEASED by `version_serial` |
| `AssetInventoryService` | Pool search, usage, duplicates, owner |
| `PortfolioService` | Taxonomy + membership; R21 app set for a level |
| `CycloneDxExportService` | Weak CDX from Combined SBOM union (or fingerprint snapshot) |
| `MiReportService` | MI-1…MI-4; portfolio → level → latest RELEASED unions → Gremlin |

---

## Domain API sketch (product language)

Base path (evolving): `/api/v1/inventory/...`  
Public shapes use glossary terms — **no** `BoM*` / graph / matcher names.

Illustrative routes (inventory OpenAPI group on `:sbom-service`):

| Method | Path | Notes |
|--------|------|-------|
| GET | `/applications?q=` | Search / list |
| POST | `/applications` | Create app + bootstrap DRAFT (`targetVersion` required) + one empty BOM |
| GET/PUT | `/applications/{id}` | Metadata + tags |
| GET | `/applications/{id}/stats` | Portal lazy stats: versionCount, bomCount (all versions), latestVersion, latestMultiBom |
| GET | `/applications/{id}/depends-on` | Inferred shared-asset deps (latest RELEASED Combined SBOM) |
| POST | `/applications/{id}/versions` | New draft: target + based-on version or fingerprint; optional combine |
| GET | `/applications/{id}/versions/{versionId}` | Version + Combined SBOM view (ephemeral; read-only) |
| PATCH | `/applications/{id}/versions/{versionId}` | DRAFT: rename target, version tags |
| DELETE | `/applications/{id}/versions/{versionId}` | DRAFT only; cascade dependents after confirm |
| GET | `/applications/{id}/versions/{versionId}/combined` | Ephemeral Combined SBOM (optional `sbomIds`); PUT → 405 |
| GET | `/applications/{id}/versions/{versionId}/dependents` | DRAFT delete preview (cascade list) |
| GET/POST | `/applications/{id}/versions/{versionId}/sboms` | List / create BOM |
| GET/PUT/PATCH/DELETE | `.../sboms/{sbomId}` | One BOM (DELETE not last) |
| GET/POST | `/applications/{id}/versions/{versionId}/fingerprints` | List / create (`name` + `category`) |
| GET | `/assets?type=` | List by type |
| POST | `/assets/search` | Type + searchable field filters |
| POST | `/assets` | Create pool asset (optional owner name) |
| GET | `/assets/{id}` | Detail + usage (draft/version scan stopgap) |
| PUT | `/assets/{id}/owner` | Set/clear owning application name |
| GET | `/assets/duplicates?type=` | Find-only identifier groups |
| CRUD | `/portfolios…` | Tree + place applications (WI-011) |
| POST | `/portfolios/{id}/reports` | Body: `{ "level": "root"\|nodeId, "report": "MI-1"… }` |
| GET | `/applications/{id}/versions/{versionId}/export/cyclonedx` | Weak demo of Combined SBOM (hidden in UI) |

OpenAPI is published for these domain endpoints on `:sbom-service`.

**Transitional:** until later WIs replace it, the legacy façade `/api/v1/example/sbom/**` may still exist for demos/scripts — not the product vocabulary target.

---

## Relation labels

Users see **friendly** labels (beautifier of role codes, e.g. `DEPENDS_ON` → “Depends on”). Optional small override map where needed. APIs may include both `role` and `label`.

---

## Out of scope (product)

- Auth / multi-tenant  
- Object/component versioning lifecycle  
- Foundation workbench / `/api/v1/objs/**` on this app (workbench runner `:objs-service-app` only)  
- CycloneDX/SPDX **import**; strong/certified export  
- Transactional Save (backlog D-6); file demo seeds (D-7)  
- Custom BI builder / scheduled reporting  
- Tag search/filter on Applications portal  

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

- Story + gaps: [`multi-bom-app-versions`](../../workitems/in-progress/multi-bom-app-versions/STORY.md) · prior D-2 [`sbom-inventory-app`](../../workitems/completed/20260816-sbom-inventory-app/STORY.md)  
- Canonical ontology: [`canonical-spec.md`](canonical-spec.md)  
- Seeds: [`../graph/seeds.md`](../graph/seeds.md)  
- Engineer mapping: [`GRAPH-AND-RETRIEVAL.md`](../../workitems/in-progress/multi-bom-app-versions/GRAPH-AND-RETRIEVAL.md)  
- Foundation workbench (separate app): [`../ui.md`](../ui.md) · `:objs-service-app`  
