# Story: policy-suites — suite hierarchy + folder roll-up

**Slug:** `policy-suites`  
**Branch:** `policy-suites`  
**Status:** completed  
**Closed:** 2026-09-05  
**Folder:** [`docs/workitems/completed/20260905-policy-suites/`](.)  
**Backlog:** [C-27](../../BACKLOG.md)  
**Base:** `origin/dev`  
**MR:** _(pending)_  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-32--normative-order) step **5 / 8**  
**Before:** [C-32 `policy-metadata`](../../completed/20260905-policy-metadata/STORY.md); hard require [C-24](../../completed/20260904-policy-evaluate-core/STORY.md) (+ C-26 for Drools); C-31 workbench shipped  
**Next:** [C-28 `policy-seeds-persistence`](../../planned/policy-seeds-persistence/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md) — **all resolved**  
**Design:** [`docs/design/policy/suites.md`](../../../../design/policy/suites.md)  
**Results model (indicative):** [`RESULTS-MODEL.md`](RESULTS-MODEL.md)

## Goal

Add **PolicySuite** folder trees, **policy-matcher** assignment, `evaluateSuite` as a **wrapper** over flat `evaluate`, suite-level roll-up **engine** (`BUILTIN`; folder `rollUpMode` inputs), and **`ExecutionStrategy`**. Results use shared **`evaluationId`** (`meta` + `tree` + `outcomes`; no input persist). Workbench: **Policy** subnav **Evaluate** \| **Suites**.

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock (suite GAPS) (`WI-001-design-lock.md`)
- [x] WI-002 — Suite model + repository APIs (`WI-002-model-repo.md`)
- [x] WI-003 — Suite execution + roll-up tests (`WI-003-suite-execution.md`)
- [x] WI-004 — Workbench Policy > Suites basic UI (`WI-004-suite-workbench.md`)
- [x] WI-005 — Living docs (`WI-005-living-docs.md`)
- [x] WI-006 — Correct G-P29s suite engine vs folder rollUpMode (`WI-006-rollup-split.md`)

## Locked (C-27)

| Topic | Lock |
|-------|------|
| Shape (G-P26s) | Folder **tree** (DAG, single parent); cycles forbidden; folder tags/annotations; suite `rollUpStrategyKind` (engine); folder `rollUpMode` + `severityConfig`; participation ENABLED \| DISABLED \| IGNORED |
| Matchers (G-P27s) | Extensible SPI; **STATIC_LIST** + **METADATA**; `policyId`=`Policy.id`; version string omit / `major.minor.serial` / `major.*`; missing→ERROR or skip; category **slug**; metadata AND |
| Scope (G-P28s) | full \| subfolder \| set of subfolders \| policy subset |
| Roll-up (G-P29s) | Suite selects **engine** (`BUILTIN`; future DROOLS); folder **`rollUpMode`** ALL_PASS\|ANY_PASS as inputs; status+severity in strategy; PASS+sev warning; synthetic sev (ERROR→HIGH) |
| Execution (G-P30s) | Taxonomy ≠ execution; `ExecutionStrategy` owns dedupe; C-29 reuse |
| Repos (G-P31s) | Split `SuiteRepository` |
| Versioning (G-P32s) | No suite versioning; regulatory replay intent later; not first-class API in C-27 |
| Applicability (G-P33) | Policy-only; N/A ≡ DISABLED for suite results (no leaf, no vote) |
| Entry (G-P15s) | Wrapper over `evaluate(fragment, policyRefs)` |
| Results (G-P11s) | `evaluationId` + meta (tags/annotations) + tree (refs→outcomes) + outcomes; **no input persist** |
| Workbench (WI-004) | Policy subnav **Evaluate** \| **Suites**; Evaluate look/feel; **edit-first layout** (graph secondary); CRUD + selection examine + evaluate (full/subfolder); tactical / rework-OK |

## Out of scope

- Seeds (C-28), batch matrix (C-29), product suite content
- Full C-30 consumer REST beyond play-service needs for Suites UI
- Polished reporting UX / SBOM matrix
- Input persist / frozen fragment graph (deferred)
- Drools-backed roll-up implementation (future engine kind)
