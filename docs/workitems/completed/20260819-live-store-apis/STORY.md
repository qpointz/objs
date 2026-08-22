# Story: Live store APIs

**Slug:** `live-store-apis`  
**Branch:** `live-store-apis`  
**Status:** closed  
**Folder:** [`docs/workitems/completed/20260819-live-store-apis/`](.)  
**Backlog:** [C-17](../../BACKLOG.md) (done)  
**Base:** `origin/dev`  
**GitLab:** [sandbox/bom-poc#1](https://gitlab.qpointz.io/sandbox/bom-poc/-/issues/1)  
**Design:** [`docs/design/graph/apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md)  
**Examples matrix:** [`EXAMPLES.md`](EXAMPLES.md)  
**Workbench:** [`WORKBENCH.md`](WORKBENCH.md)  
**Prior tracker:** [`FOUNDATION-BACKLOG.md`](../../completed/20260816-sbom-inventory-app/FOUNDATION-BACKLOG.md) (`FB-1` / `FB-2` **done**, `FB-3` open)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

**Slice 1 of 3** (versioning sequence) — live-graph lookups **before** versioning. Next: [C-18 versions](../../completed/20260819-versions-and-snapshots/STORY.md), then [C-19 after versions](../../completed/20260822-foundation-after-versions/STORY.md) (done). Text search is a **sibling** story: [C-20 `store-text-search`](../../planned/store-text-search/STORY.md). Sequence: [`SEQUENCE.md`](../../SEQUENCE.md).

Put **object-store** APIs in `:objs-core` for **live** catalog/graph use: catalog helpers, reverse membership, identity query, membership `copyGraph`, persist-union `mergeGraph`, paged pool select, label/filter helpers. **Each feature WI rewires every labeled consumer** in the same commit.

**Not this story:** snapshot/fingerprint freeze, pool `clone()` replacement, entity/edge audit clocks (C-19), text `q` / contains (**C-20**).

## Normative locks

| Topic | Lock |
|-------|------|
| Layer | New APIs live in **`:objs-core`** (Java-callable). Workbench REST may *expose* them; examples **must not** call `/api/v1/objs/**` as the app data API |
| Examples | **Every feature WI = core + all listed consumers** in one commit. Labels: **SBOM + AR**, **workbench + SBOM + AR**. Maps: [`EXAMPLES.md`](EXAMPLES.md), [`WORKBENCH.md`](WORKBENCH.md) |
| Docs | **Same WI** as the code: living `apps-vs-foundation`, product `example.md` if the domain API changes, workbench help if Objects/query UX changes, example README if a route changes, KDoc |
| `FB-1` | **Done** — `listGraphIdsForEntity` / `listIncidentEdges` |
| `FB-2` | **Done** — `findEntitiesByIdentity` / `findDuplicateGroups` |
| `FB-3` | **Not this story.** Contains / `q` → [C-20](../../planned/store-text-search/STORY.md). Other operators → C-19. Equality/`&&`/`||` stay as today |
| `copyGraph` vs `mergeGraph` vs `clone` | **`copyGraph(sourceId, annotations)`**: one source → new **live** graph, **same** entity ids, copied graph-local edges (new edge ids). **Keep-split new draft** and **AR collection copy**. **`mergeGraph(sourceIds, annotations, policy)`**: 1..n sources → new live graph; collisions via **`GraphMergePolicy`** (default first-seen). **Combine-on-new-draft**. Combined SBOM **GET** stays ephemeral `BomUnion` (not persist). **`clone()`** unchanged this story. **Fingerprint / freeze = C-18 pins**, not copy or merge |
| Identifier immutability | Already persist-gate (`IDENTIFIER_IMMUTABLE`, C-14). Delete the SBOM app-side re-check |
| Product stay | Apps/versions/BOMs/fingerprints/portfolios; AR collections + write modes + SPI; CycloneDX; MI *report meaning*; D-6 / D-7 |
| SPA / Gradle | Appendix only — no shared UI kit or boot starter in this story |

Indicative store names (lock exact signatures in WI-001):

- Catalog: latest ENTITY schema per type; identifier vs searchable paths; inbound/outbound allow-list for a type including `*`
- `listGraphIdsForEntity(entityId)` / `listIncidentEdges(entityId, graphId?)`
- `findEntitiesByIdentity(type, identityMap)` / `findDuplicateGroups(type)`
- `copyGraph(sourceGraphId, annotations) → newGraphId`
- `mergeGraph(sourceGraphIds, annotations, policy) → newGraphId` (overload without policy = first-seen)
- `selectFromPool(matcher, page)` + `countByType()` (pool; optional graph-scoped count)
- `displayLabel(payload, type, schema?)` + `filterMapToObjExpr` (equality filter map → `obj-expr`)
- *(not this story)* text `q` / contains → C-20; entity/edge/graph clocks → C-19

## Stages

| Stage | WIs | Examples | Ready | Notes |
|-------|-----|----------|-------|-------|
| 0 — Scaffold | WI-000 | — | done | Trackers + GAPS |
| 1 — Design lock | WI-001 | **docs** (workbench + SBOM + AR) | after WI-000 | Living design + [`EXAMPLES.md`](EXAMPLES.md) + [`WORKBENCH.md`](WORKBENCH.md) |
| 2 — Catalog helpers | WI-002 | **workbench + SBOM + AR** | after WI-001 | Latest schema, field hints, allow-list-for-type |
| 3 — Reverse lookup | WI-003 | **SBOM + AR** | after WI-001 | **FB-1** (workbench has no where-used screen) |
| 4 — Identity query | WI-004 | **SBOM + AR** | after WI-001 | **FB-2** |
| 5 — Membership copy / merge | WI-005 | **SBOM + AR** | after WI-001 | `copyGraph` + `mergeGraph`; workbench keeps hard `clone()` |
| 6 — Page + counts | WI-006 | **workbench + SBOM + AR** | after WI-001 | Store-side page/count |
| 7 — Docs sweep | WI-007 | **docs** | after WI-002…006 | Consistency + `FOUNDATION-BACKLOG` |

Order: WI-001 → WI-002 → WI-006 → (WI-003, WI-004, WI-005) → WI-007.

## Work Items

- [x] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock: store APIs vs examples — examples: **docs** (`WI-001-design-lock.md`)
- [x] WI-002 — Catalog helpers — examples: **workbench + SBOM + AR** (`WI-002-catalog-helpers.md`)
- [x] WI-003 — Reverse lookup FB-1 — examples: **SBOM + AR** (`WI-003-reverse-lookup.md`)
- [x] WI-004 — Identity query FB-2 — examples: **SBOM + AR** (`WI-004-identity-query.md`)
- [x] WI-005 — `copyGraph` + `mergeGraph` live membership copy/union — examples: **SBOM + AR** (`WI-005-copy-graph.md`)
- [x] WI-006 — Paged pool select + `countByType` — examples: **workbench + SBOM + AR** (`WI-006-page-and-counts.md`)
- [x] WI-007 — Living docs sweep — examples: **docs** (`WI-007-living-docs.md`)

## Out of scope

- Matcher pushdown beyond equality/`&&`/`||` — contains/`q` is **C-20**; remaining FB-3 is **C-19**
- Text `q` / contains — **C-20** [`store-text-search`](../../planned/store-text-search/STORY.md)
- D-6 transactional inventory Save; D-7 file demo seeds
- Hard `clone()` behaviour change
- Product REST *vocabulary* redesign, ontology packs, CycloneDX, MI report *definitions*
- AR write-mode pipeline and SPI (except calling identity/copy/merge/count APIs)
- Extracting `SpaRoutingFilter` / example compile-dep on `:objs-service` (platform follow-up)
- Shared Mantine schema UI kit
- Versions + snapshots — **C-18** [`versions-and-snapshots`](../../completed/20260819-versions-and-snapshots/STORY.md)
- Entity/edge/graph store clocks, pin reverse lookup, remaining matcher ops — **C-19** [`foundation-after-versions`](../../completed/20260822-foundation-after-versions/STORY.md) (done; clocks shipped in C-18)
- SBOM **fingerprint as freeze snapshot** (C-18). C-17 `copyGraph` is **keep-split new draft / collection copy**; `mergeGraph` is **combine-on-new-draft** only

## Acceptance (story)

- [x] `:objs-core` exposes the locked store APIs with tests (H2; Postgres IT where SQL-specific)
- [x] SBOM, AR, and workbench (where labeled) call those APIs; stopgaps in [`EXAMPLES.md`](EXAMPLES.md) / [`WORKBENCH.md`](WORKBENCH.md) are gone
- [x] `clone()` still new-ids; `copyGraph` / `mergeGraph` keep pool ids
- [x] Living design + both product `example.md` files match shipped names; parked FB-1/FB-2 marked done in the SBOM mini-backlog
- [x] `./gradlew :objs-core:test :sbom-service:test :asset-repository-service:test`

## Process notes

1. One WI at a time; `[x]` + one commit + push per WI.  
2. Feature WIs: **core + every labeled consumer + docs** in that commit.  
3. Closed 2026-08-19 (UTC).
