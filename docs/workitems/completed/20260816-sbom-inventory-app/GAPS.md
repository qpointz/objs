# Gaps — sbom-inventory-app (D-2)

Summary tables first. **Open — questions to lock** has decision checklists (answer in chat; agent flips to **resolved**). **Resolved locks** and **Foundation watch** hold the durable detail. Engineer mapping: [`GRAPH-AND-RETRIEVAL.md`](GRAPH-AND-RETRIEVAL.md).

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

---

## Architecture (locked)

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-A1 | Deployable shape | **resolved** | `examples/sbom/` — `:sbom-service` (launchable) + `:sbom-service-ui` (Vite/node-gradle, same pattern as `:objs-service-ui`). Replaces `objs-sbom-example` |
| G-A2 | objs dependency | **resolved** | Programmatic `objs-core` only; no `objs-service` REST / workbench |
| G-A3 | Product language | **resolved** | Non-technical; hide all graph/entity/edge/matcher vocabulary |
| G-A4 | Versioning | **resolved** | Version graphs only; object versioning deferred |
| G-A5 | Foundation watch | **resolved** | Identify gaps during design/impl **and especially MI reports / export**; foundation work only via explicit WIs |
| G-A6 | Hybrid persistence | **resolved** | SBOM tables: application **1→\*** versions (draft is a status) **1→0..\*** fingerprints; each version/fingerprint has `graph_id`. Portfolios domain-only. Assets + relations only in graphs |
| G-A7 | Reuse SBOM graph schema | **resolved** | Prefer existing canonical ontology; extend for inventory gaps; **small additive tweaks OK** to help weak CDX demo |
| G-A8 | Object model from schema | **resolved** | App compiles object model from graph schema (user generates schema) |

## Product

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P1 | Primary surfaces | **resolved** | Non-technical UI + domain REST API; **OpenAPI** for domain API; **no CLI** |
| G-P2 | Draft vs versions | **resolved** | Draft is a **version status**. Promote in place sets the version identifier. New draft copies from a version. |
| G-P3 | Application inventory | **resolved** | SBOM table + link to graphs/assets by id/annotations |
| G-P4 | App→app dependency | **resolved** | **Inferred** from shared objs in BOM graphs — no ApplicationRef, no explicit app-dep edges (see resolved lock) |
| G-P5 | Owning application | **resolved** | Asset annotation **`owner`** = **application name** (see resolved lock) |
| G-P6 | Relation vocabulary | **resolved** | Humanize role names (beautifier); optional small fixed override map (see resolved lock) |
| G-P7 | Duplicate action | **resolved** | **Find-only** in v1 — list groups + navigate; no merge/link |
| G-P8 | CycloneDX/SPDX **import** | **cancelled** | **Not needed** — out of story |
| G-P9 | Object versioning | **deferred** | Explicitly later |
| G-P10 | Application portfolios | **resolved** | Subject-area tree; **applications only**; once per portfolio |
| G-P11 | CycloneDX **export** | **resolved** | **Weak demo** only (see resolved lock) |
| G-P12 | MI reports | **resolved** | Portfolio-owner only. UX: portfolio → level → report → Run. Graphs = **latest version** per in-scope app. Rewritten **MI-1…MI-4** (see resolved lock). Visual Apps/Portfolios tabs; **no auth** |
| G-P13 | Transactional version save | **deferred** | UI Save today: N `updateAsset` + `replaceBom` + optional `updateApplication`. Fold payloads + graph replace + app meta into **one** `PUT` version request, one transaction. Shared pool payload side effects stay explicit. Backlog **D-6** |
| G-P14 | File-based demo inventory | **deferred** | Replace `SbomDemoInventorySeeder` ApplicationRunner with seed documents under `examples/sbom/demo` (`kind: Application` / version graphs + existing Portfolio placements). Startup YAML import instead of 70-app Kotlin loop. Backlog **D-7** |

## Foundation watch-list

Normative detail and scheduling: **[`FOUNDATION-BACKLOG.md`](FOUNDATION-BACKLOG.md)** (mini-backlog `FB-*`). Summary:

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-F1 / **FB-1** | Who uses this asset? (reverse lookup) | **parked** | Foundation **to be extended**; example uses stopgap scan — see FOUNDATION-BACKLOG |
| G-F2 / **FB-2** | Find duplicates by identifier | **parked** | Foundation **to be extended**; in-memory group stopgap — see FOUNDATION-BACKLOG |
| G-F3 | Application catalog search | **resolved** | SBOM application table |
| G-F4 / **FB-3** | Schema-field advanced search | **resolved** | Form = **`searchable` only**; if no pushdown → **slow path** OK; FB-3 tracks better pushdown |
| G-F5 | Version snapshot semantics | **resolved** | **Fingerprint** copies the version graph (governance snapshot). Promote does not copy. |
| G-F6 | Cross-version compare | **deferred** | Nice-to-have |
| G-F7 / **FB-4** | MI over selection (Gremlin) | **resolved** | `selectAndEval` exists — **FB-4 done**; wire from example |
| G-F8 / **FB-5** | Graph-id-set matcher (portfolio → graphs) | **resolved** | **`graphs-in`** / WI-014 done |

## Process

| #    | Topic                   | Status       | Resolution                                   |
| ---- | ----------------------- | ------------ | -------------------------------------------- |
| G-X1 | Example packaging vs `objs-app` | **resolved** | See resolved lock — `examples/sbom/`; `objs-app` optional; runtimeOnly workbench sidecar OK |
| G-X2 | WI-004+ detail bodies   | **resolved** | Titles in STORY; fill when Stage 1 completes |

---

## Deferred / cancelled / parked (no questions this story)

- **G-P8** CycloneDX/SPDX import — cancelled (not needed)
- **G-P9** Object/component versioning lifecycle
- **G-P13** Transactional version save (payloads + BoM + app meta as one request) — backlog **D-6**
- **G-P14** File-based demo inventory (`examples/sbom/demo`) — backlog **D-7**
- **G-F6** Cross-version BOM compare UI/report
- **G-F1 / FB-1** Who uses this asset (reverse lookup) — **parked**; foundation extension tracked in FOUNDATION-BACKLOG
- **G-F2 / FB-2** Find duplicates by identifier — **parked**; foundation extension tracked in FOUNDATION-BACKLOG
- Auth / multi-tenant
- Custom BI builder, scheduled/email reports
- Strong/certified CycloneDX or SPDX product export
- Portfolio as a single CycloneDX BOM

---

## Open — questions to lock

### G-P1 — Primary surfaces

**Status:** resolved

**Lock:**

1. **Non-technical UI** (SPA) for Journeys 1–3 — product language only.  
2. **Domain REST API** under the example app (not `/api/v1/objs/**`).  
3. **OpenAPI** published for the domain API (springdoc / equivalent).  
4. **No CLI** in this story.

**Implement in:** WI-004 (packaging + OpenAPI wiring), WI-006…013 (routes), WI-009 (UI).

---

### G-P4 — App → app dependency modeling

**Status:** resolved

**Lock:**

1. **No ApplicationRef** (or other synthetic app objs) in the BOM graph for dependency.  
2. **No explicit app→app edges** encoded for “depends on application”.  
3. **Inference:** Application A depends on application B when A’s draft/version graph **uses the same objects (assets)** that B uses (shared pool entities appearing in both apps’ BOM graphs). Dependency is derived from graph membership / shared assets, not stored as a separate relation.  
4. **Version preservation:** Asking “what did version V depend on?” means: take V’s graph members; find other applications whose **latest version** graphs also include those members; report those apps + the shared assets. The version graph already freezes which objects were used. Portfolio **MI-2** uses the same inference **within the selected portfolio level’s latest-version set**.  
5. Optional owning-application (G-P5) may refine “shared vs owned” labelling in MI-2 but is **not** required to invent explicit app-dep edges.

**Implement in:** WI-006/007 read paths; MI-2 within portfolio set (WI-013); GRAPH-AND-RETRIEVAL R7/R14/R18.

---

### G-P5 — Owning application storage

**Status:** resolved

**Lock:**

1. Store ownership on the asset as an objs **annotation**.  
2. Annotation key: **`owner`** (product copy may show `owner:`).  
3. Annotation value: the owning application’s **name** (string from `sbom_application`), **not** the application UUID.  
4. No second SBOM asset table; no required OWNED_BY edge for v1.  
5. Resolve/display: look up `sbom_application` by **name** when labelling; if rename breaks matches, treat as accepted risk for v1 (or update annotations on rename in a later polish — not required now).  
6. Distinct from “used by” (membership in draft/version graphs) and from inferred app→app deps (G-P4).

**Implement in:** WI-005 (convention), WI-008 create/edit asset, Journey 2 UI, MI shared-asset labelling.

---

### G-P6 — Relation vocabulary for users

**Status:** resolved

**Lock:**

1. Users see **friendly relation labels**, not raw allow-list codes as primary copy.  
2. **Default:** a **beautifier** of the canonical role string — e.g. `DEPENDS_ON` → `Depends on`, `HAS_VULNERABILITY` → `Has vulnerability` (split on `_`, title-case / sentence-case consistently).  
3. **Optional:** a small **fixed override map** only where the beautifier is wrong or too awkward (do not maintain a full hand map of all 28 roles unless needed).  
4. Machine/API payloads may still carry the canonical role code alongside the display label.  
5. No foundation change — domain/UI concern.

**Implement in:** WI-001 glossary note; shared label helper used by WI-009 and MI reports.

---

### G-P7 — Duplicate action

**Status:** resolved

**Lock:**

1. v1 is **find-only**: detect possible duplicates by identifier fields; list groups; navigate to assets.  
2. **No** merge, link, delete-other, or override-identity flows in this story.  
3. Merge/link (if ever) is a later backlog item — not blocking WI-008 / MI-4.

**Implement in:** WI-008 duplicates UI/API; MI-4 may surface duplicate counts/groups as signals only.

---

### G-F1 / FB-1 — “Who uses this asset?” (foundation reverse lookup)

**Status:** **parked** — foundation needs extension; tracked as **[FB-1](FOUNDATION-BACKLOG.md)**.

**In one sentence:** objs can answer “what’s inside application X’s BOM?” easily, but **not** the reverse — “given asset Y, which applications’ BOMs include Y, and with which relations?”

#### User-facing need

On Assets inventory (Journey 2), the user picks an asset (e.g. component `jackson-core`) and must see applications that use it and how (friendly relation). Portfolio **MI-3** asks a related question over a **selected level’s apps** (shared hotspots), not a single-asset picker.

#### Parking decision

- **Do not** block the whole story on a new core API in the first implementation pass.  
- Example may use a **stopgap**: scan only known SBOM draft/version `graph_id`s.  
- **Must** keep FB-1 on the foundation mini-backlog until a proper `listGraphIdsForEntity` / `listIncidentEdges` (or equivalent) lands.  
- Reopen as `in-story` only via WI-003 / explicit user request.

#### Proposed objs-core API (when unparked)

```text
listGraphIdsForEntity(entityId): List<UUID>
listIncidentEdges(entityId, graphId?): List<BoMEdge>
```

**Blocks when unparked:** WI-003 foundation WI; then WI-008 / WI-013 MI-3 harden against stopgap.

---

### G-F2 / FB-2 — Find duplicates by identifier (foundation)

**Status:** **parked** — foundation needs extension; tracked as **[FB-2](FOUNDATION-BACKLOG.md)**.

**Needed by:** Journey 2 duplicates (find-only, G-P7); **MI-4** risk signals; R12.

**Today:** `BoMIdentityProjection` for immutability; no first-class find-by-identity query (ex C-14 G-12).

**Parking decision:** same as FB-1 — ship find-only UX with in-memory grouping stopgap; keep FB-2 on the foundation mini-backlog until a proper identity query API lands. Reopen only via WI-003 / user request.

**Propose (when unparked):**

```text
findEntitiesByIdentity(type, schemaVersion, identityMap): List<BoMEntity>
// or findDuplicateGroups(type, schemaVersion): List<DuplicateGroup>
```

**Blocks when unparked:** WI-003 foundation WI; then harden WI-008 / WI-013 MI-4.

---

### G-F4 / FB-3 — Schema-field advanced search

**Status:** **resolved** (product lock) + foundation pushdown tracked as **[FB-3](FOUNDATION-BACKLOG.md)** if operators are incomplete.

**Needed by:** Journey 2 dynamic form → filters.

**Lock:**

1. Advanced search form shows **only** fields marked **`searchable`** on the object schema — not all scalars.  
2. Compile filters to `obj-expr` (equality / contains as supported).  
3. Prefer Postgres / matcher **pushdown** when available.  
4. If pushdown cannot support a chosen operator on a searchable field: **allow a slow path** (evaluate in domain / post-filter after a broader candidate fetch) so the UI still works — do not hide the searchable field solely for lack of pushdown.  
5. Incomplete pushdown remains a **foundation** finding (**FB-3**) to improve later; slow path is an accepted example stopgap, not a reason to query non-searchable fields.

**Questions closed:** searchable-only; slow path when no pushdown.

**Blocks:** WI-008; WI-009 search form; FB-3 if pushdown gaps appear.

---

### G-F7 / FB-4 — MI over selection (Gremlin)

**Status:** resolved — **[FB-4](FOUNDATION-BACKLOG.md) done**.  
**Needed by:** All portfolio MI reports (MI-1…MI-4).

**Lock:** Use programmatic `BoMGremlinEngine.selectAndEval` in `:sbom-service` (no gremlin REST). Selection comes from FB-5 / WI-014.

**Blocks:** WI-013 (after WI-014).

---

### G-F8 / FB-5 — Graph-id-set matcher (portfolio → graphs)

**Status:** in-story — **[FB-5](FOUNDATION-BACKLOG.md)** / **[WI-014](WI-014-graph-id-set-matcher.md)**.  
**Needed by:** Every MI run over the selected portfolio level.

**Product locks (resolved):**

1. **Graphs:** **latest application version** graph per in-scope app (R22). Drafts never used for MI. Apps with no version omitted from graph set.  
2. **Selection:** Domain does **not** teach core about portfolios. Flow: R21 apps → R22 `graph_id`s → foundation **graph-id-set matcher** (optional chained `obj-expr`) → subgraph for Gremlin.

**Remaining open (non-blocking):**

1. Cap / warn when selected level covers too many apps?  

**Blocks:** WI-013; implement WI-014 first.


---

### G-X1 — Example packaging vs `objs-app`

**Status:** resolved

**Layout (normative):**

```text
examples/
  sbom/
    sbom-service/       # domain + Boot-launchable app (objs-core, objs-gremlin-core; runtimeOnly workbench sidecar)
    sbom-service-ui/    # Vite/React SPA; Gradle node-gradle packaging like :objs-service-ui
  <other-examples>/     # future example apps (sibling folders under examples/)
```

Gradle project names: `:sbom-service`, `:sbom-service-ui` with `projectDir` under `examples/sbom/`.

**Locks:**

1. **SBOM product** lives only under `examples/sbom/` — not under the foundation leaf modules.  
2. **`objs-app`** is the **foundation side service** (objs-service + objs-service-ui + gremlin REST, port **8081**). It does **not** depend on `:sbom-service`.  
3. **`:sbom-service`** is the inventory **core + launchable** application (`bootRun` / `run`, port **8080**).  
4. **`:sbom-service-ui`** mirrors `:objs-service-ui` build (node-gradle, Vite → `processResources` → `static/sbom` at `/ui/`). Consumed as `runtimeOnly` by `:sbom-service`.  
5. Full rework of former `objs-sbom-example` allowed; no backward compatibility. **`:sbom-service` must not `implementation`-depend** on `:objs-service` / `:objs-service-ui` / `:objs-gremlin-service` (Gradle guard). **`runtimeOnly`** of those modules is allowed so Workbench can run on the same JVM for demo.  
6. Domain REST/UI must not call `/api/v1/objs/**`. Workbench at `/workbench/` is optional sidecar.

**Implement in:** WI-004 (move + packaging); WI-009 (product UI content).

---

## Resolved locks (detail)

### G-A5 — Foundation watch

**Status:** resolved

Implementation **must** prefer `objs-core` programmatic APIs. When a journey (especially **MI-3/MI-4** and usage/duplicates) is awkward:

1. Record the deficit here / in GRAPH-AND-RETRIEVAL.  
2. Classify in **WI-003**: example stopgap | foundation WI same story | deferred backlog.  
3. **Never** call `objs-service` REST as a workaround.

---

### G-A6 / G-P2 / G-P3 / G-F5 — Hybrid persistence + draft/version

**Status:** resolved

| Domain entity | Points to | Mutable? |
|---------------|-----------|----------|
| `sbom_application` | metadata only (name, description, …) | yes |
| `sbom_application_draft` | **own** `graph_id` | yes (application owner edits) |
| `sbom_application_version` | **own** `graph_id` | prefer immutable after create |
| Portfolio / subject area | no graph | taxonomy only |

**Create version:** new version row + **new** named graph; **copy** memberships/edges from draft graph (not soft-link share).

**Assets** never stored as SBOM table payloads — only objs pool entities + graph membership/edges.

See GRAPH-AND-RETRIEVAL §1–3.

---

### G-A7 / G-A8 — Schema reuse + compiled model

**Status:** resolved

- Start from existing SBOM ontology seeds / catalog (Waves A–D).  
- User **generates** schema; app **compiles** object model from it (Wave* hand models are not long-term SoT).  
- **Additive** schema changes allowed for inventory encoding and **weak CDX demo** clarity (e.g. purl) — not a CDX-driven ontology rewrite.

---

### G-P10 — Application portfolios

**Status:** resolved

- Editable tree of **subject areas** (folders).  
- Each folder: 0+ **applications** (membership = application id **only** — **no version**).  
- An application appears **at most once** in a given portfolio (may appear in other portfolios).  
- Purpose: combine apps and (via them) their SBOMs/graphs.  
- **Domain tables only** — no objs graph for the portfolio structure.

**Implement in:** WI-011, WI-009.

---

### G-P11 — Weak CycloneDX export (demo)

**Status:** resolved

- **Export only** (import cancelled — G-P8).  
- Purpose: demo **graph → another format**, not a certified SBOM product.  
- Source: draft and/or version `graph_id`.  
- Format: CycloneDX JSON (~1.6).  
- **Minimum mapping:** application → `metadata.component`; `Component` → `components[]`; `DEPENDS_ON` → `dependencies[]`; omit the rest freely.  
- No completeness claims in UX (demo labelling OK).  
- Small schema tweaks allowed to improve the demo.

**Implement in:** WI-012, WI-009 Export control. See GRAPH-AND-RETRIEVAL R16.

---

### G-P12 — MI reports

**Status:** resolved

**Persona:** Portfolio owner only (Portfolios tab). Application owner has **no** MI entry. Visual tabs only — **no auth**.

**UI:** Keep the Portfolios experience **clean and obvious** — taxonomy edit and the MI run flow should not compete; prefer a clear “Reports” step after (or beside) the tree, not a crowded multi-panel dashboard.

**Run UX:**

```text
Select portfolio → select level (node or root) → select report → Run → results
```

Each step visible; one **Run** control; results in product language.
**Scope + graphs:**

1. Application set = apps under selected node (subtree); root = entire portfolio (R21).  
2. Graphs = each app’s **latest version** `graph_id` only (R22). No drafts. Apps with no version omitted from graphs (may list as “no version” in MI-1).  
3. Empty node / zero apps → empty result.

**v1 set** (rewritten; may extend after testing):

| ID | Report | Primary foundation pressure |
|----|--------|-----------------------------|
| MI-1 | Portfolio composition | Multi-graph select + Gremlin aggregates (**G-F7**, **G-F8**) |
| MI-2 | Application dependency map | Shared-object inference within set (G-P4); multi-graph (**G-F8**); may pressure **G-F1** |
| MI-3 | Shared asset hotspots | Multi-graph overlap; may pressure **G-F1** |
| MI-4 | Duplicate & risk signals | Duplicates (**G-F2**) + multi-graph (**G-F8**) |

Out of v1: custom report builder, schedules, PDF polish, app-scoped / draft-scoped MI.

**Implement in:** WI-013, WI-009 Portfolios tab. See R17–R22.

---

### G-F3 — Application catalog search

**Status:** resolved

Search/list applications via **SBOM application table** (domain), not foundation graph search.

---

## Foundation watch (detail)

### G-F1 / FB-1 — “Who uses this asset?” (reverse lookup)

| | |
|--|--|
| **User need** | Pick an asset → list applications that use it and the relation (“how”) |
| **Example** | Asset jackson-core → payments-api (Depends on), billing-api (Contains) |
| **Consumers** | Journey 2 inspect; portfolio **MI-2/MI-3**; helps G-P4 shared-object inference |
| **Works today** | Open known `graph_id` → list members (forward) |
| **Gap** | No public “graphs containing entity” + incident edges (reverse) |
| **Status** | **Parked** — see **[FB-1](FOUNDATION-BACKLOG.md)**; stopgap scan of SBOM graph ids |
| **When unparked** | Foundation API + tests; then harden WI-008 / MI-3 |

### G-F2 / FB-2 — Identity / duplicate query

| | |
|--|--|
| **User need** | Possible duplicate assets by identifier fields (find-only) |
| **Consumers** | Journey 2; **MI-4** |
| **Gap** | Projection exists; query/index by identity map does not |
| **Status** | **Parked** — see **[FB-2](FOUNDATION-BACKLOG.md)**; in-memory group stopgap |
| **When unparked** | Foundation find API + tests; harden WI-008 / MI-4 |

### G-F4 / FB-3 — Advanced search pushdown

| | |
|--|--|
| **User need** | Per-type schema text form → filter assets |
| **Product lock** | Form = **`searchable` only**; **slow path** allowed when pushdown missing (G-F4) |
| **Consumers** | Journey 2 |
| **Remaining gap** | Prefer pushdown for performance — track as **[FB-3](FOUNDATION-BACKLOG.md)** |
| **WI-003 outcome** | Improve pushdown; keep slow path as fallback until then |

### G-F7 — MI over selection (Gremlin)

| | |
|--|--|
| **User need** | Portfolio MI results without shipping raw multi-graph dumps to the UI |
| **Gap** | Prefer Gremlin scripts over the selected union; wire programmatic path in example |
| **Default** | `objs-gremlin-core` over FB-5 selection; domain DTO projection |

### G-F8 — Graph-id-set matcher

| | |
|--|--|
| **User need** | Select latest-version graphs for apps under a portfolio level, then traverse |
| **Gap** | No first-class matcher for an explicit graph-id set (portfolio stays domain-only) |
| **Default** | Foundation `graphs-in` (TBD) matcher; stopgap N-select until WI-003 |

---

## Revision history

| Date | Note |
|------|------|
| 2026-08-12 | Initial gap table from story scaffold |
| 2026-08-12 | Hybrid, schema compile, portfolios, CDX demo, MI-1…MI-4 |
| 2026-08-12 | Elaborated open questions + resolved/foundation detail sections |
| 2026-08-12 | Portfolio report scope: selected node/root → application set (G-P12) |
| Date | Note |
|------|------|
| 2026-08-13 | G-P12 rewrite: portfolio-owner MI only; UX portfolio→level→report; latest version graphs; Apps/Portfolios tabs; clean UI; FB-4/FB-5 → Gremlin + graph-id-set matcher; WIs aligned |
| 2026-08-13 | G-X1/G-A1: `examples/sbom/{sbom-service,sbom-service-ui}`; objs-app foundation-only |
| 2026-08-13 | WI-001: durable `docs/design/sbom/example.md` rewritten for inventory product + glossary |
| 2026-08-13 | WI-003: FB-4 done; FB-5 → WI-014; FB-1/2 parked confirmed |
| 2026-08-12 | G-P1 surfaces: UI + domain REST + OpenAPI; no CLI |
| 2026-08-12 | G-P4 app deps inferred from shared graph objects; no ApplicationRef |
| 2026-08-12 | G-P5 owner annotation = application name |
| 2026-08-12 | G-P6 role beautifier + optional override map |
| 2026-08-12 | G-P7 duplicates find-only |
| 2026-08-12 | G-F1 parked as FB-1; FOUNDATION-BACKLOG.md mini-backlog created |
| 2026-08-12 | G-F2 / FB-2 parked — identity query foundation gap |
| 2026-08-12 | G-F4 searchable-only form; FB-3 = pushdown completeness |
| 2026-08-12 | G-F4: slow path allowed when no pushdown |
