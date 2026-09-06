# Story: policy-seeds-persistence — catalog store + seed format

**Slug:** `policy-seeds-persistence`  
**Branch:** `policy-seeds-persistence`  
**Status:** completed  
**Closed:** 2026-09-06  
**Folder:** [`docs/workitems/completed/20260906-policy-seeds-persistence/`](.)  
**Backlog:** [C-28](../../BACKLOG.md)  
**Base:** `origin/dev`  
**MR:** https://gitlab.qpointz.io/sandbox/bom-poc/-/merge_requests/61  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-32--normative-order) step **6 / 8**  
**Before:** [C-27 `policy-suites`](../20260905-policy-suites/STORY.md) (suite seed kinds; result shape locked, **not** result store); hard require [C-24](../20260904-policy-evaluate-core/STORY.md) (+ C-32 categories)  
**Next:** [C-29 `policy-batch`](../../planned/policy-batch/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md) — catalog/seed rows **closed** (WI-001); G-P11r / G-P32r **deferred**  
**Design:** [`docs/design/policy/overview.md`](../../../design/policy/overview.md), [`docs/design/policy/repository.md`](../../../design/policy/repository.md), [`docs/design/graph/seeds.md`](../../../design/graph/seeds.md), [`docs/design/policy/workbench.md`](../../../design/policy/workbench.md)  
**C-27 results sketch (not this story):** [`RESULTS-MODEL.md`](../20260905-policy-suites/RESULTS-MODEL.md)

## Goal

Durable **catalog** persistence for Policy, Category, and Suite via **objs Flyway** (vendor folders) with SQL + JPA **consolidated in `:objs-persistence`**.

Portable **independent** seed kinds via objs **`SeedDocumentHandler`** (handlers in objs-policy\*):

| Content | Drop | Modes |
|---------|------|--------|
| `Category`, `Policy`, `PolicySuite` | `DropCategory`, `DropPolicy`, `DropPolicySuite` | apply/MERGE vs replace |

Human identity = **`key`** (display `name` separate). Bodies: **inline** and **file/classpath** refs. Apply order = **file + document order only** (user-owned). Fail whole file → **no ledger update**. **REPLACE** export of full catalog + **workbench** export action (WI-005). Product packs stay in examples/apps.

Round-trip the live configuration C-24 / C-32 / C-27 expose (`PolicyRepository`, `CategoryRepository`, `SuiteRepository`). Flat `evaluate` / `evaluateSuite` contracts stay unchanged.

## Boundary (vs C-27 results)

| In C-28 | **Not** in C-28 |
|---------|-----------------|
| Persist / load **Policy**, **Category**, **Suite** (live CRUD store) | Persist **evaluation results** (`evaluationId`, outcomes, suite result tree) |
| Seed import / REPLACE export; **workbench export** of full policy setup | **Input / snapshot persist** (fragment freeze, full-config replay) — C-27 G-P32s / G-P11s deferred |
| JPA behind existing repository ports (`key` identity align) | Foundation `ExecutionArchive` / result Flyway tables |

C-27 locked **result API shape** only. [`RESULTS-MODEL.md`](../20260905-policy-suites/RESULTS-MODEL.md) does **not** mandate result Flyway in this story.

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock — GAPS already resolved; design notes (`WI-001-design-lock.md`)
- [x] WI-002 — Flyway + JPA catalog + `key` model align (`WI-002-flyway-jpa.md`)
- [x] WI-003 — Seed handlers + import tests (`WI-003-seed-handlers.md`)
- [x] WI-005 — Catalog REPLACE export + workbench (`WI-005-catalog-export.md`)
- [x] WI-004 — Living docs (`WI-004-living-docs.md`)
- [x] WI-006 — SBOM Assessment demo (`WI-006-sbom-assessment.md`)

## Out of scope

- Evaluation result store / `RESULTS-MODEL` tables / input persist / G-P32s replay archive
- Batch (C-29), full C-30 consumer REST, concrete regulatory seed **content** in foundation
- Changing flat `evaluate` / `evaluateSuite` semantics
