# Story: Store text search

**Slug:** `store-text-search`  
**Branch:** (not started — **planned** only; no implementation until asked)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/store-text-search/`](.)  
**Backlog:** [C-20](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Depends on:** **C-17 paging** ([`live-store-apis`](../../completed/20260819-live-store-apis/STORY.md) WI-006) before implementation WIs. Design (WI-001) may run in parallel.  
**Does not block:** C-18 versions.  
**Design:** [`docs/design/graph/apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Consumers:** [`EXAMPLES.md`](EXAMPLES.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)  
**Prior tracker:** [`FOUNDATION-BACKLOG.md`](../../completed/20260816-sbom-inventory-app/FOUNDATION-BACKLOG.md) **FB-3** (contains / `q`)

## Goal

Design, then ship, one store **text search** over live pool/graph entities so workbench Objects, SBOM asset inventory, and AR collection object search stop using equality-only `obj-expr`, domain `LIKE` on product tables, or load-then-filter.

This is the **contains / `q` slice of FB-3**. Other matcher operators stay **C-19**. Linguistic FTS (`tsvector`) is out.

**Not ready to implement** until WI-001 locks the DSL, field set, and SQL strategy. Open questions live in [`GAPS.md`](GAPS.md).

## Normative (provisional — lock in WI-001)

| Topic | Draft (may change in WI-001) |
|-------|------------------------------|
| Layer | `:objs-core` programmatic API; workbench may expose; examples call core, not `/api/v1/objs/**` |
| Consumers | **workbench + SBOM + AR** in the implementation WI |
| Domain `LIKE` | Application name / collection name stay on product tables |
| Graph headers | `GET /graphs/search?q=` header substring is **not** payload search |
| Snapshots | Search **live** graphs / pool. Snapshot-pin search is C-18+ if needed |
| Not this story | Remaining FB-3 (`>`, regex, …); `tsvector`; C-17 lookups/copy/merge/paging |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | planned | This folder + backlog row |
| 1 — Design lock | WI-001 | after WI-000 | **Required before code.** Close open GAPS |
| 2 — Store pushdown | WI-002 | after WI-001 **and** C-17 WI-006 | Core API + tests |
| 3 — Consumers | WI-003 | after WI-002 | workbench + SBOM + AR |
| 4 — Docs | WI-004 | after WI-003 | Living design + product `example.md` |

## Work Items

- [ ] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [ ] WI-001 — Design lock: DSL, fields, SQL, consumers — examples: **docs** (`WI-001-design-lock.md`)
- [ ] WI-002 — Store substring pushdown — examples: **—** (`WI-002-store-pushdown.md`)
- [ ] WI-003 — Rewire workbench + SBOM + AR — examples: **workbench + SBOM + AR** (`WI-003-consumers.md`)
- [ ] WI-004 — Living docs — examples: **docs** (`WI-004-living-docs.md`)

## Out of scope

- Implementation until the user starts this story **and** WI-001 is done
- C-17 catalog / reverse / identity / `copyGraph` / `mergeGraph` / paging
- C-18 versions and snapshot pins
- C-19 clocks, pin reverse lookup, FB-3 remainder
- Domain-table FTS; schema-catalog client `includes`; Composer `clone()`

## Acceptance (after implementation)

- [ ] One store search API; same `q` (or locked equivalent) hits the same pool/graph members in workbench, SBOM assets, and AR collection objects
- [ ] No example loads a full graph to implement substring search
- [ ] Open GAPS from WI-001 are **resolved** or explicitly deferred
- [ ] `./gradlew :objs-core:test :objs-service:test :sbom-service:test :asset-repository-service:test`

## Process notes

1. One WI at a time; `[x]` + one commit + push per WI.  
2. Do not start WI-002 until WI-001 checkboxes are done.  
3. Do not close this story until the user asks.
