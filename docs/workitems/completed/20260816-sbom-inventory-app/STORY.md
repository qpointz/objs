# Story: SBOM applications inventory

**Slug:** `sbom-inventory-app`  
**Branch:** `sbom-inventory-app`  
**Status:** completed  
**Folder:** [`docs/workitems/completed/20260816-sbom-inventory-app/`](.)  
**Backlog:** [D-2](../../BACKLOG.md) (done)  
**Base:** `origin/dev`  
**Design (product):** [`docs/design/sbom/example.md`](../../../design/sbom/example.md)  
**Design (engineer mapping):** [`GRAPH-AND-RETRIEVAL.md`](GRAPH-AND-RETRIEVAL.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Foundation mini-backlog:** [`FOUNDATION-BACKLOG.md`](FOUNDATION-BACKLOG.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Rework the SBOM inventory product under [`examples/sbom/`](../../../examples/sbom/) — **`:sbom-service`** (launchable) + **`:sbom-service-ui`** — demonstrating objs as a **library** (programmatic API only). Replaces former `objs-sbom-example`.

The app is for **non-technical** users. Product language is applications, versions, assets, relations, ownership, dependencies, portfolios, and reports — **never** graph/entity/edge/matcher concepts.

## Normative locks

| Topic | Lock |
|-------|------|
| Module | **`examples/sbom/sbom-service`** + **`examples/sbom/sbom-service-ui`**; full rework; no backward compatibility with `objs-sbom-example` |
| objs usage | Depends on **`objs-core`** (+ **`objs-gremlin-core`** for MI); **assets and their relations** live in the objs graph (programmatic stores/catalogs/typed toolkit/matchers) |
| Packaging (G-X1) | `objs-app` = foundation **side service** (port 8081). SBOM on 8080. Demo **runtimeOnly** workbench sidecar on the same JVM is allowed; inventory must not `implementation`-depend on objs-service |
| SBOM persistence | Example **may** own application-specific tables linked to graph data by **id and/or annotations** |
| Hybrid rule | Graph objs **must** describe BOM content (assets + relations). Domain tables are inventory metadata / indexes — **not** a parallel asset store |
| Domain BOM pointers | Application **1→\*** versions (status DRAFT/RELEASED) **1→0..\*** fingerprints; each row has its own named graph |
| Ontology | **Reuse existing SBOM graph schema**; extend only when journeys require it; small additive tweaks OK for weak CDX demo |
| Object model | App **compiles** object model **from the graph schema** (schema SoT; user generates schema) |
| Forbidden | Domain code/UI must not call **`objs-service` REST**; **no CycloneDX/SPDX import**. Workbench may be a **runtimeOnly** demo sidecar |
| Audience | Non-technical; hide foundation graph vocabulary |
| Surfaces | Non-technical UI + domain REST + **OpenAPI**; **no CLI** |
| Versioning | **Version graphs only**; object versioning deferred |
| Assets | Global objs pool; reuse or create (create → pool) |
| Ownership | Optional owning application via annotation **`owner`** = **app name** |
| CycloneDX | **Weak export demo** only (graph → format); not a product-grade exporter |
| Personas / UI | Visual split only (global tabs): **Applications** vs **Portfolios**. **No auth / roles.** Assets under Applications. MI **only** under Portfolios. **UI: clean and obvious** — one primary task per screen; no dense chrome, no foundation jargon, no hidden flows |
| MI reports | Portfolio-owner only. UX: **portfolio → level (node/root) → report → Run → results** (linear, obvious). Graphs = each in-scope app’s **latest version** graph. v1 set **MI-1…MI-4** (rewritten); extensible after testing |
| Foundation gaps | Track in [`FOUNDATION-BACKLOG.md`](FOUNDATION-BACKLOG.md). **FB-1/FB-2 parked**. **FB-4/FB-5 done** (`selectAndEval` + `graphs-in`). **FB-3 open** (slow path OK) |

## User journeys (v1 scope)

### Journey 1 — Application owner

Global tab: **Applications** (assets may be a sub-area or sibling under the same chrome).

1. Search for applications  
2. Open the application (portal lists **apps only**) and switch versions in the header  
   - Edit the **draft** working copy (Save/Discard); add/remove assets and relations  
   - Add = reuse from pool **or** create new (create → pool)  
3. **Promote** the draft (version identifier set then); **fingerprint** any version for a frozen snapshot  
4. See **app→app dependencies** inferred from **shared assets** in the BOM graph  
5. **Export** a version as **weak CycloneDX** JSON  

**Not in this journey:** portfolios, MI reports.

### Journey 2 — Assets inventory

Under the **Applications** chrome (same persona / no auth).

1. Search assets by type  
2. Advanced search per type: simple **dynamic form** from object schema — **`searchable` fields only** (G-F4)  
3. Inspect an asset: which applications use it, and how  
4. Find possible **duplicates** using schema **identifier** fields (**find-only** — G-P7)  
5. Optional **owning application** on an asset  

### Journey 3 — Portfolio owner (taxonomy + MI)

Global tab: **Portfolios**. Visual separation from Application owner only — **no authorization**.

1. Maintain **application portfolios** — subject-area taxonomy; place **applications** only (no version); app at most once per portfolio  
2. Run **management information (MI)** reports:

```text
Select portfolio → select level (subject-area node or root) → select report → Run → results
```

**Scope (every MI):** applications under the selected level (subtree; root = entire portfolio).  
**Graphs (every MI):** for each in-scope app, use the **latest application version** graph only. Apps with **no version** are omitted from graph selection (composition may still list them as “no version”). Draft graphs are **not** used for MI.

v1 reports (may extend after testing):

| ID | Report | Answers (over selected level’s apps / latest versions) |
|----|--------|--------------------------------------------------------|
| MI-1 | Portfolio composition | Which apps; asset counts by type; relation / DEPENDS_ON density across latest versions |
| MI-2 | Application dependency map | Inferred app→app deps **within the selected set** (shared objects in latest version graphs) |
| MI-3 | Shared asset hotspots | Assets appearing in **multiple** in-scope apps; which apps (and how, when cheap) |
| MI-4 | Duplicate & risk signals | Identifier duplicate groups + lightweight vulnerability / risk signals across the set |

Prefer **objs-core** + **Gremlin over a multi-graph selection** for graph reads; use reports to **surface foundation deficits** (G-A5). Domain resolves portfolio → apps → latest `graph_id`s; foundation supplies selection/traverse (see FB-4 / FB-5).

## Out of scope (this story)

- Auth / multi-tenant  
- Object/component versioning lifecycle  
- Exposing foundation workbench or `/api/v1/objs/**`  
- CycloneDX/SPDX **import**; full/strong CDX fidelity  
- Custom BI builder / scheduled reporting  
- Policy engines / compliance scoring product  
- Transactional Save: asset payloads + version graph + application name/description as **one** request (**G-P13** / backlog **D-6**)  
- File-based demo inventory under `examples/sbom/demo` replacing `SbomDemoInventorySeeder` (**G-P14** / backlog **D-7**)  

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Docs + trackers |
| 1 — Design | WI-001 … WI-003, WI-014 | after WI-000 | Personas; retrieval; foundation audit; **graph-id-set matcher** |
| 2 — Packaging + domain | WI-004 … WI-005 | after Stage 1 design docs | `examples/sbom/{sbom-service,sbom-service-ui}`; ontology |
| 3 — Domain services | WI-006 … WI-008, WI-011 … WI-013 | after WI-005 | Apps/versions/assets; portfolios; CDX; portfolio MI |
| 4 — UI + polish | WI-009 … WI-010, WI-015 | after Stage 3 | Clean Applications \| Portfolios UI; seeds; docs; Mantine chrome |

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Product design + glossary (`WI-001-product-design.md`)
- [x] WI-002 — Graph mapping + retrieval strategies (`WI-002-graph-retrieval-design.md`)
- [x] WI-003 — Foundation gap audit (`WI-003-foundation-gap-audit.md`)
- [x] WI-004 — Module packaging: `examples/sbom/` + drop objs-service (`WI-004-module-packaging.md`)
- [x] WI-005 — Domain model + ontology alignment (`WI-005-domain-ontology.md`)
- [x] WI-006 — Application inventory service + domain API (`WI-006-application-inventory.md`)
- [x] WI-007 — Version capture (copy graph; inferred deps via shared objects) (`WI-007-version-capture.md`)
- [x] WI-008 — Assets inventory service + domain API (`WI-008-assets-inventory.md`)
- [x] WI-009 — Non-technical UI (J1–J3; Applications / Portfolios tabs) (`WI-009-ui.md`)
- [x] WI-010 — Seeds, demo data, docs (`WI-010-seeds-docs.md`)
- [x] WI-011 — Application portfolios (subject-area tree) (`WI-011-portfolios.md`)
- [x] WI-012 — Weak CycloneDX export demo (`WI-012-cyclonedx-export.md`)
- [x] WI-013 — MI reports (portfolio-scoped MI-1…MI-4) (`WI-013-mi-reports.md`)
- [x] WI-014 — Graph-id-set matcher FB-5 (`WI-014-graph-id-set-matcher.md`)
- [x] WI-015 — Mantine chrome + workbench sidecar (`WI-015-mantine-chrome.md`)

## Acceptance (story)

- [x] Runnable `:sbom-service` without `objs-service` REST; `:objs-app` foundation-only  
- [x] Journeys 1–3 supported in domain language (API + UI); Applications vs Portfolios visual split  
- [x] Version graph freezes members so inferred app deps / shared assets are reproducible  
- [x] Weak CycloneDX export demo works for draft/version  
- [x] Portfolio MI flow works (portfolio → level → report → results); latest-version graphs only; MI-1…MI-4 runnable; foundation gaps recorded or closed  
- [x] Design docs updated; example demonstrates programmatic objs usage end-to-end  

## Process notes

1. One WI at a time; mark `[x]` before next; one commit + push per WI.  
2. During implementation: prefer example-layer solutions; escalate platform holes to foundation WIs (WI-003). **Portfolio MI (id-set matcher + Gremlin) is a primary pressure test** on objs-core / objs-gremlin-core.  
3. UI stays **clean and obvious** (WI-009); do not add chrome that blurs Applications vs Portfolios.  
4. Closed 2026-08-16; archive [`docs/workitems/completed/20260816-sbom-inventory-app/`](.).  
