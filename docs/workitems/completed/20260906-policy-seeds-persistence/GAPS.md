# Gaps — policy-seeds-persistence (C-28)

Close in this story’s **WI-001** only. **Catalog + seed rows below are resolved** (2026-09-06). G-P11r / G-P32r stay deferred.

**Boundary:** This story is **catalog** persistence (Policy / Category / Suite) + **seeds**.  
It is **not** evaluation-result persistence. C-27 locked result **API shape** and deferred store/input-persist — see [`RESULTS-MODEL.md`](../../completed/20260905-policy-suites/RESULTS-MODEL.md), G-P11s / G-P32s in [`policy-suites/GAPS.md`](../../completed/20260905-policy-suites/GAPS.md).

## Catalog + seeds (WI-001 — closed)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P13p | JPA / Flyway backing store | **resolved** | **objs Flyway** only (`flyway_schema_history_objs`, next `VN` under `classpath:org/poc/objs/core/db/migration/{vendor}/`). **Folder strategy** — keep vendor folders; no separate policy history / no Boot-app locations for catalog DDL. **Consolidate** Flyway SQL + JPA for Policy / Category / Suite (incl. suite folders & matchers) in `:objs-persistence` (not `:objs-policy-*` SQL line). Not result tables (G-P11r). |
| G-P14p | Persistent repository API | **resolved** | **Same ports as in-memory** — `PolicyRepository`, `CategoryRepository`, `SuiteRepository`. JPA behind those ports; no new pagination / optimistic locking in C-28. Identity field rename to **`key`** is G-P36seed (ports stay; field names align). |
| G-P34seed | Seed envelope | **resolved** | Same `apiVersion: objs.poc.org/v1` as graph/object seeds ([`seeds.md`](../../../design/graph/seeds.md)). No policy-specific apiVersion. |
| G-P35seed | Seed kinds | **resolved** | **Independent kinds:** `Category`, `Policy`, `PolicySuite`. **Drop kinds:** `DropCategory`, `DropPolicy`, `DropPolicySuite` (by `key`). **apply/MERGE** vs **replace** for all three content kinds (symmetric). Category apply → keep policies; replace → wipe policies in/under category. PolicySuite apply → keep unspecified tree; replace → wipe suite tree then load. Cascades: DropCategory wipes policies then category; DropPolicy drops policy identity; DropPolicySuite drops suite tree only. DB→seeds round-trip. Setup wipe may clear results/history when G-P11r exists. Extends graph “MERGE-only” for these kinds. |
| G-P36seed | MERGE keys | **resolved** | Human identity = **`key`** for Policy / Category / PolicySuite (and Drop*). Replaces `slug` and name-as-identity; **`name`** = display. UUID optional on export. Policy apply-by-`key` mints **serial** (G-P3). SuiteFolder keeps folder `key`. API align in C-28. `key` charset/rules per G-P40seed. |
| G-P37seed | Body embedding | **resolved** | Inline **and** file ref; file ref loads **filesystem** and **classpath**. Full-catalog REPLACE export uses **inline** bodies (WI-005). |
| G-P38seed | Import path | **resolved** | Always [`SeedDocumentHandler`](../../../../objs-api/src/main/kotlin/org/poc/objs/api/seed/SeedDocumentHandler.kt); handlers in **objs-policy\***; no dedicated importer. |
| G-P39seed | Apply order | **resolved** | Independent docs; **file + document order only** (user-owned). No forced category→policy→suite pipeline. **WI-005:** REPLACE export of all present objects + **workbench** full policy-setup export. |
| G-P40seed | Validation | **resolved** | Unknown `engineKind`; dangling refs; suite cycles; `key` charset/rules. Fail **whole file/resource** (rollback). **Do not update seed ledger** on failure. |

## Explicitly out of C-28 (do not close as “in story”)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P11r | Evaluation result persistence | **deferred→C-33** | Shipped in [C-33 `policy-results-persistence`](../20260907-policy-results-persistence/GAPS.md). |
| G-P32r | Input / full-config replay persist | **deferred→C-33** | Shipped in [C-33](../20260907-policy-results-persistence/GAPS.md). |

## Philosophy (inherited)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P36s | Seed format required | **resolved** (intent) | Content in apps |
| G-P41 | Dedicated store | **resolved** | Not `bom_entity` |
| G-P31s | Split repos | **resolved** (C-27) | `SuiteRepository` ≠ `PolicyRepository`; C-28 backs both (+ categories) |

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| Boundary | Catalog vs results | 2026-09-06 | C-28 = Policy/Category/Suite store + seeds. Results / input persist deferred. |
| G-P13p | Objs Flyway + consolidated persistence | 2026-09-06 | Catalog DDL on objs Flyway vendor folders; SQL + JPA in `:objs-persistence`. |
| G-P14p | Same repository ports | 2026-09-06 | In-memory SPI ports; identity → `key` via G-P36seed. |
| G-P34seed | Same seed apiVersion | 2026-09-06 | `apiVersion: objs.poc.org/v1`. |
| G-P35seed | Kinds + apply/replace + Drop* | 2026-09-06 | Content + `DropCategory` / `DropPolicy` / `DropPolicySuite`. |
| G-P36seed | `key` as human identity | 2026-09-06 | MERGE/resolve on `key`; name = display; serial on policy apply. |
| G-P37seed | Inline + file/classpath body | 2026-09-06 | Inline and file/classpath refs. |
| G-P38seed | SeedDocumentHandler only | 2026-09-06 | objs-policy* handlers; no dedicated importer. |
| G-P39seed | User order + export WI | 2026-09-06 | File/doc order only; WI-005 REPLACE + workbench export. |
| G-P40seed | Fail file; no ledger update | 2026-09-06 | Whole-resource fail; ledger not updated on failure. |
| — | — | — | Catalog + seed GAPS **closed** for WI-001 |
