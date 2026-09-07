# Story: policy-results-persistence — evaluation result store

**Slug:** `policy-results-persistence`  
**Branch:** `policy-results-persistence`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/policy-results-persistence/`](.)  
**Backlog:** [C-33](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-32--normative-order) step **7 / 9**  
**Before:** [C-28 `policy-seeds-persistence`](../../completed/20260906-policy-seeds-persistence/STORY.md) (catalog durable); result **API shape** from [C-27](../../completed/20260905-policy-suites/STORY.md)  
**Next:** [C-29 `policy-batch`](../../planned/policy-batch/STORY.md) (advanced mass execution — **after** this story)  
**Gaps:** [`GAPS.md`](GAPS.md) — **all closed** (WI-001)  
**Design (read next):** [`PERSISTENCE-MODEL.md`](PERSISTENCE-MODEL.md) (concrete store) · [`RESULTS-MODEL.md`](../../completed/20260905-policy-suites/RESULTS-MODEL.md) § Persistence API · [`suites.md`](../../../design/policy/suites.md) · [`results.md`](../../../design/policy/results.md) · [`repository.md`](../../../design/policy/repository.md)

---

## Cold start (read this first)

**What exists today**

| Piece | State |
|-------|--------|
| Flat `evaluate` / suite `evaluateSuite` | Shipped — return **in-memory** results only |
| Policy / Category / Suite **catalog** | Durable (C-28 JPA + seeds) |
| Workbench Policy play / SBOM Assessment | Session results only (**EPHEMERAL**) |
| Evaluation **archive** tables / save API | **Shipped** — `EvaluationArchive` + Flyway V8 |

**What this story adds:** explicit `save` of evaluation archives with a **PersistSpec** (what to write), optional named presets, labeling, and run metadata. Evaluate paths stay unchanged unless the caller saves.

**Where you are on the branch**

- WI-000…WI-004 **done** (scaffold, design lock, Flyway/JPA, archive port + tests, living docs).
- Story remains **in-progress** until explicit closure / archive to `completed/`.
- Do **not** reopen [`GAPS.md`](GAPS.md) without amending this story.

**Modules you will touch (WI-002+)**

| Module | Role |
|--------|------|
| `:objs-policy-api` | Archive/result port + `PersistSpec` types (WI-003) |
| `:objs-persistence` | Flyway SQL + JPA (WI-002); port impl |
| `:objs-autoconfigure` / `:objs-policy-service` | Wiring as needed for tests (WI-003) |

**Not** `:objs-policy` catalog repos; **not** batch (C-29).

---

## Goal

Durable **evaluation archives** keyed by `evaluationId`. Caller chooses **what** is written via independent axes and filters. Default runtime behaviour remains EPHEMERAL (object model only) until an explicit save.

**Not hard-linked to suites:** FLAT / custom policy-set archives (`saveFlat`) are first-class; suite runs add an optional overlay on the same row. Archive port ≠ `SuiteRepository`; no catalog FK (see RESULTS-MODEL + G-P46r).

Wire front-to-back: **catalog (C-28) → run → optional archive**.

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
| EPHEMERAL | none | No durable write (today’s Workbench/SBOM) |
| STANDARD | `results` + `executionContext` | Reporting / historization workhorse |
| FULL | STANDARD + `input` | Audit reproducability |

Any archive with `results`+`executionContext` is **usable as STANDARD** even if `input` is stored (load-as-STANDARD omits pack).

**Labeling & runtime** (on persist options / stored meta):

| Field | Role |
|-------|------|
| `name` / `description` | Human labels for the run |
| `tags` / `annotations` | Correlation (same spirit as C-32 / evaluation meta) |
| `evaluatedAt` / `durationMs` | When the run happened + wall-clock duration |

---

## Boundary

| In C-33 | **Not** in C-33 |
|---------|-----------------|
| Durable archive of evaluation **results** (+ optional context / input) | Catalog Policy / Category / Suite store (C-28) |
| FLAT / custom policy-set persist (`saveFlat`) without a suite | Requiring a suite (or suite FK) to archive |
| Explicit `save(result, PersistSpec)` / presets | Auto-persist inside `evaluate` / `evaluateSuite` |
| Filter which outcome statuses / finding severities are stored | Changing evaluate semantics or suite roll-up |
| Archive name, description, tags, annotations, durationMs | Batch / FragmentsFactory (C-29); product matrix UI |
| objs Flyway + `:objs-persistence` JPA for archives | Requiring engine/software version in the input pack |

C-27 locked **in-memory** result shape only. C-28 locked **catalog** persistence only. This story owns former deferred **G-P11r** / **G-P32r**.

---

## Work Items

- [x] WI-000 — Story scaffold + resequence (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock — close GAPS (`WI-001-design-lock.md`)
- [x] WI-002 — Flyway + JPA archive tables (`WI-002-flyway-jpa.md`)
- [x] WI-003 — Archive port + round-trip tests (`WI-003-result-store.md`)
- [x] WI-004 — Living docs (`WI-004-living-docs.md`)

## Out of scope

- [C-29 `policy-batch`](../../planned/policy-batch/STORY.md) — FragmentsFactory / policies source / mass execution
- [C-30 `policy-consumer`](../../planned/policy-consumer/STORY.md) — optional REST / example consumer (beyond thin test wiring)
- Matrix UI; regulatory seed **content** in foundation
- SBOM/Workbench UX switch to durable archives (can stay EPHEMERAL until a later WI/story)
