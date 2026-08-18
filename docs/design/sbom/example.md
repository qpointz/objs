# SBOM applications inventory

**Status:** living — D-2 inventory plus [`multi-bom-app-versions`](../../workitems/completed/20260817-multi-bom-app-versions/STORY.md) (D-8: multi-BOM versions, parallel drafts, tags, fingerprints)  
**Modules:** `:sbom-service` + `:sbom-service-ui` under [`examples/sbom/`](../../../examples/sbom/)  
**Run:** `./gradlew :sbom-service:run` → UI **`http://localhost:8080/sbom/`** (demo seeds via `demo` profile)  
**Engineer mapping:** [`GRAPH-AND-RETRIEVAL.md`](../../workitems/completed/20260817-multi-bom-app-versions/GRAPH-AND-RETRIEVAL.md)  
**Gaps:** [`GAPS.md`](../../workitems/completed/20260817-multi-bom-app-versions/GAPS.md)  
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

**User guide:** how to operate this model in the inventory SPA is [`user.md`](user.md). Engineer storage/union mapping is [`GRAPH-AND-RETRIEVAL.md`](../../workitems/completed/20260817-multi-bom-app-versions/GRAPH-AND-RETRIEVAL.md).

---

## Multi-BOM model

An **application version is never “one graph.”** It is always **1..\*** named **BOMs**. Each BOM is an incomplete bill (its own named graph plus name, description, tags). The **complete** bill for that version is the **Combined SBOM**: the ephemeral union of **all** of those BOM graphs. Combined SBOM is computed at read time. It is **not** stored on the version row, has **no** metadata row of its own, and is **always read-only**.

This is the product lock for D-8. Progressive UI hides multi-BOM chrome when a version has only one BOM, but the storage model is the same: that single row is still a BOM (default name `BOM`).

### Why split a version into BOMs

Typical splits in the demo (and in intended use) are **lifecycle or packaging slices** of the same application version, for example:

| Example BOM name | Typical content |
|------------------|-----------------|
| **Build** | Compilers, plugins, CI artifacts that produce the product |
| **Runtime** | Libraries and components the running application depends on |
| **Image** | Container / distribution artifacts |

Other splits are valid (team, supplier, environment) as long as each BOM stays an **incomplete** part of one version. The Combined SBOM is what fingerprint, MI, depends-on, and “the version” export mean.

**BOM** (incomplete part) and **Combined SBOM** (complete union) must not be confused:

| | BOM | Combined SBOM | Subset union |
|--|-----|---------------|--------------|
| What | One constituent row + graph | Union of **all** BOM graphs of the open version | Union of **some** BOM graphs (multi-select, not all) |
| Stored? | Yes (`sbom_application_sbom` + named graph) | No | No |
| Editable? | Yes, only when parent version is **DRAFT** and this BOM is the **edit target** | Never | Never |
| Product label | **BOM** (name hidden when count = 1) | **Combined SBOM** | Not labeled SBOM |
| Name `Combined SBOM` | Not reserved; a BOM may use that string | Virtual label only | — |

### Cardinality and identity

```text
Application  1 ── *  Application version (DRAFT | RELEASED)
                       │
                       ├── 1..*  BOM  (each: name unique within that version, graph, tags)
                       ├── Combined SBOM  (ephemeral; select all BOMs)
                       └── 0..*  Fingerprint  (snapshot of the full Combined SBOM only)
```

Locks:

- A version **cannot** have zero BOMs. Delete the **last** BOM is refused; delete the **DRAFT** instead.
- BOM **name** is unique **within that version** (not globally). Default name on bootstrap / combine is `BOM`.
- Many **DRAFT** rows per application are allowed (parallel work). `version` on a DRAFT is the **target** semver and may be renamed while unique.
- **RELEASED** `version` is the published semver. This story does not delete RELEASED versions.
- Unique `(application_id, version)` when `version` is set.

### Union (Combined SBOM and subset)

Same algorithm for Combined SBOM, left-pane multi-select, fingerprint materialization, combine-on-new-draft, MI / depends-on / CDX-of-latest:

1. Take **entity membership** from each selected BOM graph. The same pool **asset** appears **once**.
2. Take **relations**. Duplicate edges with the same source, target, and role **collapse** to one.
3. Return an in-memory subgraph. **Do not** write it onto the version.

Fingerprint / flatten-copy then **copy** that subgraph into a **new** named graph (fingerprint row, or a single combined BOM on a new draft).

**Latest** for portal content, MI, depends-on, and CDX-of-latest is the highest **RELEASED** version by **SemVer 2.0** (`version_serial`). Drafts are never latest. No RELEASED → no latest.

### Tags

| Object | Own tags? | Notes |
|--------|-----------|--------|
| Application | Yes | Edited on the **application** pane |
| Version | Yes | Edited on the **version** pane when DRAFT |
| Each BOM | Yes | Edited on that **BOM** pane when DRAFT |
| Combined SBOM | No stored tags | **App ∪ version ∪ all BOMs** of the **open version**, unique, first-seen order (app, version, BOMs by sort). Shown **below the application name in view mode only** |

Tags are domain columns (Postgres `TEXT[]` / H2 `VARCHAR ARRAY`): trim, drop blanks, de-dupe **case-sensitive**. They are **not** in graphs and **not** in the fingerprint hash. Portal does **not** search/filter by tag this story.

### Lifecycle

| Action | Effect on BOMs / Combined |
|--------|---------------------------|
| **New application** | Required **target version**. One empty BOM named `BOM` (name hidden). Lands on that DRAFT. |
| **Create BOM** | DRAFT only. Modal: name, description, tags. After create, that BOM is **Open** (edit target). This is the 1→2 entry that turns multi-BOM chrome on. |
| **Delete BOM** | DRAFT only; not Combined; **not the last**. Confirm. 2→1: multi chrome **off immediately**; remaining BOM is Open. |
| **New draft from version** | Copy **keep-split** (each BOM graph + metadata) **or**, if source has **>1** BOM, optionally **combine** into one BOM named `BOM` whose graph is a copy of the computed full union. |
| **New draft from fingerprint** | Always **one** BOM copied from the fingerprint snapshot. **No** combine question. |
| **Promote** | Modal: **re-type** version to confirm (may override stored target if unique). DRAFT → RELEASED. Graphs stay as they are (still 1..\* BOMs). |
| **Fingerprint** | Always a snapshot of the **full Combined SBOM** (all BOMs), never a subset. Required **name** + **category** (`approval` \| `history` \| `unknown`). |
| **Delete DRAFT** | Deletes that draft’s BOMs + its fingerprints. If other drafts are based on it (or its fingerprints), **confirm the list** then **cascade**. Empty 204. |

### Progressive disclosure (view)

| BOM count | Left tree | Graph shown | Multi-select |
|-----------|-----------|-------------|--------------|
| **1** | App → version → one row labeled **BOM** (no checkboxes) | That BOM | Hidden |
| **≥ 2** | App → version (checkbox = select all) → named BOM rows (checkboxes) | Combined when all selected; one BOM when one selected; **subset union** when 2+ but not all | Yes |
| Fingerprint open | No BOM list / no BOM switch | Fingerprint snapshot graph | Hidden |

Title click on a tree row **navigates** (sets `focus` / `bom`) **without** changing checkboxes. Checkboxes (`sbom=` query) control which graphs are **unioned** for the Assets/Graph canvas. `bom=` is the Open constituent for metadata.

### Application detail: three foci

The right **Assets** tab (when no asset is selected) is one of:

| `focus` | Pane | Shows | Writable in Edit (DRAFT) |
|---------|------|-------|--------------------------|
| `app` | Application | Name, description, app tags; versions table (incl. **BOM** column Single/Multi); fingerprints table | App metadata |
| `sbom` | Version | Target/version + version tags; **Create BOM** / **Delete BOM** when editing; BOM list if count ≥ 2 | Version metadata; BOM create/delete |
| `bom` | One BOM | BOM name, description, tags | That BOM’s metadata **and** its graph (assets/relations) |

Graph/assets mutation is allowed **only** on `focus=bom` while editing a DRAFT. Combined / subset unions stay read-only.

### Edit changeset (DRAFT)

**Edit** is an **application-level** session on the open DRAFT (not per-BOM Stay/Leave):

- Enter Edit only on a **DRAFT**. RELEASED has no Edit; create a **New draft** first.
- Each BOM has a **stash** of working assets/relations/meta. Switching BOM (radios on constituents) **flushes** the current stash and loads the other — **no** Stay/Leave.
- App / version / BOM metadata edits sit in the same session. Changed BOM rows (and dirty version) highlight **blue**.
- **Save** persists the whole changeset (app, version, BOM meta, graph writes, soft-created/deleted BOMs). **Discard** reverts stash + session.
- **Stay / Leave** only when leaving the **application/version snapshot** (pathname change), not when changing `focus` / query inside the draft.
- Edit tree: **radios** on constituents only (exactly one edit target). No check/radio on the application or version rows.

### Demo / portal

~50% of seeded apps have one BOM; ~50% have 2–3 (`Build` / `Runtime` / `Image`). Portal **content** = latest RELEASED + **Multi-BOM** badge if that version has ≥ 2 BOMs. **Footer** = total BOMs (all versions) · total versions, **lazy** per card.

---

## Journeys (v1)

### Journey 1 — Application owner

1. Search applications (portal: latest RELEASED + **Multi-BOM** cue; footer totals all BOMs · all versions, lazy per app).  
2. **New application:** name, description, **required target version**, tags → lands on that DRAFT (one empty BOM named `BOM`, name hidden).  
3. Left tree: **Application** → **version** (`v. x` or `x DRAFT`) → constituent(s). Click a row to set **focus** (app / version / BOM). With ≥ 2 BOMs, checkboxes select graphs for the union; Combined = select all.  
4. **View:** Combined and subset unions are read-only. Open a BOM (`focus=bom`) to inspect its graph.  
5. **Edit** (DRAFT): radios pick the BOM to mutate; switch freely (changeset). Create/delete BOMs on the **version** pane. Graph writes only on the Open BOM. Save or Discard the whole session.  
6. **New draft:** based-on (RELEASED, DRAFT, or fingerprint) + target. If based-on **version** has >1 BOM, ask whether to **combine into a single BOM**.  
7. **Promote:** re-type version to confirm (may override target). **Fingerprint:** header Button → name + category; always full Combined SBOM snapshot.  
8. See **depends on (app)** inferred from shared assets on the **latest RELEASED** Combined SBOM.

No portfolios or MI here. CycloneDX download is hidden on application detail (export API may remain). Step-by-step UI: [`user.md`](user.md).

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
| **Domain tables** (`sbom-service`) | Application, version (incl. tags, based-on, `version_serial`), BOM rows (`sbom_application_sbom`), fingerprint metadata, portfolio + subject areas + membership. Boot Flyway `V1` at `classpath:db/migration/{vendor}` (`flyway_schema_history`). |
| **objs graphs** (`objs-core`) | Each **BOM** has a named graph. Combined SBOM is computed at read time. Fingerprint stores a snapshot graph. `bom_*` is objs Flyway (`flyway_schema_history_objs`), not this app’s locations. |

Rules:

- Combined SBOM is **not** a `graph_id` on the version.  
- Portfolios are **domain-only** (no portfolio graph).  
- Assets live in the objs **pool**; BOM membership is per BOM graph.  
- Tags live on domain rows only (not in graphs / fingerprint hash).  
- Domain tables must not become a parallel asset store.

See [`GRAPH-AND-RETRIEVAL.md`](../../workitems/completed/20260817-multi-bom-app-versions/GRAPH-AND-RETRIEVAL.md).

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
| Schema browse allow-list | `GET /api/v1/inventory/schema-catalog/{type}/allowed-edges` — inbound/outbound rules including `*` wildcards; shown on the schema detail page, not inside JSON/YAML |
| Typed Wave* / `SbomRegistry` | Builder parity helpers — **not** SoT |
| Owning application | Annotation `owner` = application **name** |
| App→app depends | Inferred shared assets — no ApplicationRef type |
| Portfolios | Domain tables only |

Full schema→Kotlin codegen remains optional when the modeling team supplies generators; until then seeds + catalog are authoritative for product behaviour.

---

## Related

- Story + gaps: [`multi-bom-app-versions`](../../workitems/completed/20260817-multi-bom-app-versions/STORY.md) · prior D-2 [`sbom-inventory-app`](../../workitems/completed/20260816-sbom-inventory-app/STORY.md)  
- User guide (inventory SPA): [`user.md`](user.md)  
- Canonical ontology: [`canonical-spec.md`](canonical-spec.md)  
- Seeds: [`../graph/seeds.md`](../graph/seeds.md)  
- Engineer mapping: [`GRAPH-AND-RETRIEVAL.md`](../../workitems/completed/20260817-multi-bom-app-versions/GRAPH-AND-RETRIEVAL.md)  
- Foundation workbench (separate app): [`../ui.md`](../ui.md) · `:objs-service-app`  
