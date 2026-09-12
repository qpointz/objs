# Story: policy-results-persistence — evaluation result store

**Slug:** `policy-results-persistence`  
**Branch:** `policy-results-persistence`  
**Status:** completed  
**Closed:** 2026-09-07  
**Folder:** [`docs/workitems/completed/20260907-policy-results-persistence/`](.)  
**Backlog:** [C-33](../../BACKLOG.md)  
**Base:** `origin/dev`  
**MR:** https://gitlab.qpointz.io/sandbox/bom-poc/-/merge_requests/62  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-32--normative-order) step **7 / 9**  
**Before:** [C-28 `policy-seeds-persistence`](../20260906-policy-seeds-persistence/STORY.md) (catalog durable); result **API shape** from [C-27](../20260905-policy-suites/STORY.md)  
**Next:** [C-29 `policy-batch`](../20260912-policy-batch/STORY.md) (advanced mass execution — **after** this story)  
**Gaps:** [`GAPS.md`](GAPS.md) — **all closed** (WI-001)  
**Design:** [`PERSISTENCE-MODEL.md`](PERSISTENCE-MODEL.md) (concrete store) · [`RESULTS-MODEL.md`](../20260905-policy-suites/RESULTS-MODEL.md) § Persistence API · [`suites.md`](../../../design/policy/suites.md) · [`results.md`](../../../design/policy/results.md) · [`repository.md`](../../../design/policy/repository.md)

---

## Goal

Durable **evaluation archives** keyed by `evaluationId`. Caller chooses **what** is written via independent axes and filters. Default runtime behaviour remains EPHEMERAL (object model only) until an explicit save.

**Not hard-linked to suites:** FLAT / custom policy-set archives (`saveFlat`) are first-class; suite runs add an optional overlay on the same row. Archive port ≠ `SuiteRepository`; no catalog FK (see RESULTS-MODEL + G-P46r).

Wire front-to-back: **catalog (C-28) → run → optional archive**. Workbench Suites: **Persist result** dialog (tags, annotations, axes) → `POST /evaluations/suite`.

### Persist spec (normative — see GAPS)

**Content axes** (combine freely):

| Axis | Content |
|------|---------|
| `results` | RESULTS-MODEL payload (`meta` + `outcomes` [+ suite `tree`]) |
| `executionContext` | Policies executed, matchers, policy/suite bodies at T₀ |
| `input` | Replay pack (frozen fragment) |

**Result filters** — fine-tune which sets persist when `results` is on:

| Filter | Include independently |
|--------|------------------------|
| Outcome status | `PASS`, `FAIL`, `ERROR`, `NOT_APPLICABLE` |
| Finding severity | `INFO`, `WARNING`, `ERROR`, `UNSPECIFIED` (null) |

**Presets** (named configs over **content axes** only; default filters = all statuses + all severities):

| Preset | Axes | Typical use |
|--------|------|-------------|
| EPHEMERAL | none | No durable write |
| STANDARD | `results` + `executionContext` | Reporting / historization workhorse |
| FULL | STANDARD + `input` | Audit reproducability |

Effective PersistSpec fields live in extensible **`persist_profile`** JSON on `objs_policy_evaluation`.

**Labeling & runtime:** `name` / `description` / `tags` / `annotations` / `evaluatedAt` / `durationMs` (tags/annotations also as top-level JSON columns).

---

## Boundary

| In C-33 | **Not** in C-33 |
|---------|-----------------|
| Durable archive of evaluation **results** (+ optional context / input) | Catalog Policy / Category / Suite store (C-28) |
| FLAT / custom policy-set persist (`saveFlat`) without a suite | Requiring a suite (or suite FK) to archive |
| Explicit `save(result, PersistSpec)` / presets; workbench Persist result | Auto-persist inside `evaluate` / `evaluateSuite` |
| Filter which outcome statuses / finding severities are stored | Changing evaluate semantics or suite roll-up |
| objs Flyway V8 + `:objs-persistence` JPA for archives | Batch / FragmentsFactory (C-29); product matrix UI |

---

## Work Items

- [x] WI-000 — Story scaffold + resequence (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock — close GAPS (`WI-001-design-lock.md`)
- [x] WI-002 — Flyway + JPA archive tables (`WI-002-flyway-jpa.md`)
- [x] WI-003 — Archive port + round-trip tests (`WI-003-result-store.md`)
- [x] WI-004 — Living docs (`WI-004-living-docs.md`)

## Out of scope

- [C-29 `policy-batch`](../20260912-policy-batch/STORY.md) — FragmentsFactory / policies source / mass execution
- [C-30 `policy-consumer`](../../planned/policy-consumer/STORY.md) — optional REST / example consumer (beyond thin archive HTTP)
- Matrix UI; regulatory seed **content** in foundation
