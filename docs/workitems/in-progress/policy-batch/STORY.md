# Story: policy-batch — thin batch / result pack

**Slug:** `policy-batch`  
**Branch:** `policy-batch`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/policy-batch/`](.)  
**Backlog:** [C-29](../../BACKLOG.md)  
**GitLab:** [#5](https://gitlab.qpointz.io/sandbox/bom-poc/-/work_items/5)  
**Base:** `origin/dev`  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-32--normative-order) step **8 / 9**  
**Before:** [C-33 `policy-results-persistence`](../../completed/20260907-policy-results-persistence/STORY.md); suite target needs [C-27](../../completed/20260905-policy-suites/STORY.md)  
**Next:** [C-30 `policy-consumer`](../policy-consumer/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md) — **all batch gaps locked** (WI-001)  
**Design:** [`docs/design/policy/overview.md`](../../../design/policy/overview.md) §15 · [`evaluation-sequences.md`](../../../design/policy/evaluation-sequences.md) §7 · [`modules.md`](../../../design/policy/modules.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Thin foundation **batch / result pack** for mass assessment: caller supplies many subjects
`{ opaque subjectKey, GraphFragment }` and a **target** (suite scope **or** flat policy refs).
Batch runs existing evaluate / evaluateSuite per subject and returns an ordered pack.

**No** portfolio×suite matrix, heatmaps, or cross-subject scores in foundation — products own that
(SBOM already has a sequential matrix runner ready to swap).

**No** second evaluation pipeline — wrapper only ([`evaluation-sequences.md`](../../../design/policy/evaluation-sequences.md) §7).

**No** auto-persist — C-33 `EvaluationArchive` remains explicit per cell if the caller wants archives.

## Normative locks

| Topic | Lock |
|-------|------|
| API (G-P41b) | **Locked:** sync `evaluateBatch(subjects, target) → BatchEvaluationResult`; opaque `subjectKey` + fragment; target = suite **or** policy refs; continue-on-error; no hard max; caller builds subject list |
| Executor (G-P43b) | **Locked:** `PolicyBatchExecutor` owns plan + execute; C-29 ships `SequentialPolicyBatchExecutor` only; parallel deferred |
| Diagnostics (G-P42b) | **Locked:** subject ERROR cell on subject-run failure; optional pack `diagnostics`; no cross-subject roll-up; policy outcomes unchanged |
| Persist | Caller archives; batch does not call `EvaluationArchive` |
| Modules | Contracts `:objs-policy-api`; impl `:objs-policy-core` |
| Example proof | Rewire SBOM `SequentialPortfolioAssessmentRunner` (HTTP/UI unchanged) |

All batch GAPS locked — implement in WI-002.

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Branch + this folder + GitLab #5 |
| 1 — Design lock | WI-001 | done | G-P41b/42b/43b locked |
| 2 — Implement | WI-002 | done | API + sequential impl + tests + SBOM rewire |
| 3 — Docs | WI-003 | done | Living design docs |

## Work Items

- [x] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock (batch GAPS) — examples: **docs** (`WI-001-design-lock.md`)
- [x] WI-002 — Batch API + sequential impl + tests — examples: **SBOM runner** (`WI-002-batch-api.md`)
- [x] WI-003 — Living docs — examples: **docs** (`WI-003-living-docs.md`)

## Out of scope

- Matrix UI, cross-subject roll-up, heatmaps
- REST / example HTTP consumer (**C-30**)
- Parallel batch execution
- Auto-persist inside batch
- Changing flat evaluate or suite roll-up semantics
- Fragments **SPI factory** inside foundation (caller builds the subject list; “factory” is product-owned)

## Acceptance (after WI-002+)

- [x] Multi-subject pack preserves order; one bad subject does not drop others
- [x] Suite target and flat-refs target both work
- [x] `./gradlew :objs-policy-core:test --tests '*SequentialPolicyBatchExecutorTest*'`
- [x] Living docs mark batch shipped; GAPS closed

## Process notes

1. One WI at a time; `[x]` + one commit + push per WI.  
2. Do not start WI-002 until WI-001 closes open GAPS.  
3. Do not close this story until the user asks.
