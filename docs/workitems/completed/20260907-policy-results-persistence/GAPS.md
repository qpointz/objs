# Gaps — policy-results-persistence (C-33)

**Status:** all rows **resolved** in WI-001 (2026-09-07). Do not reopen without amending [`STORY.md`](STORY.md).

**Cold start:** Read [`STORY.md`](STORY.md) § Cold start → this file → [`PERSISTENCE-MODEL.md`](PERSISTENCE-MODEL.md) → [`RESULTS-MODEL.md`](../../completed/20260905-policy-suites/RESULTS-MODEL.md) § Persistence API. Then implement [WI-002](WI-002-flyway-jpa.md).

**Why these gaps exist:** C-27 locked the **in-memory** result shape (`evaluationId`, meta, outcomes, suite tree) but **not** durable storage. C-28 persisted the **catalog** only and deferred **G-P11r** / **G-P32r** here.

**Sources:** [`RESULTS-MODEL.md`](../../completed/20260905-policy-suites/RESULTS-MODEL.md) · [`suites.md`](../../../design/policy/suites.md) (G-P11s / G-P32s) · [C-28 GAPS](../../completed/20260906-policy-seeds-persistence/GAPS.md) (deferred ownership)

---

## Persist API

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P48r | Persist spec + presets | **resolved** | **Primary API:** `PersistSpec` = independent **content axes** + **result filters** + **labeling/runtime**. **Presets** `EPHEMERAL` / `STANDARD` / `FULL` = named configs over **content axes only** (caller may still override filters/labels). |
| G-P49r | Result / finding filters | **resolved** | Fine-tune which sets persist when `results` is on. **Statuses:** `PASS` \| `FAIL` \| `ERROR` \| `NOT_APPLICABLE`. **Severities:** `INFO` \| `WARNING` \| `ERROR` \| `UNSPECIFIED` (null). Empty status set ⇒ no outcome rows. Empty severity set ⇒ outcomes may persist with **no** findings. Suite tree leaves follow kept outcomes. |
| G-P44r | Write path | **resolved** | Explicit `save(result, spec)` (or equivalent). Spec **or** preset (+ optional overrides). No save ⇒ **EPHEMERAL**. No silent `input` freeze. |
| G-P45r | Archive labeling | **resolved** | Optional **name**, **description**, **tags**, **annotations**. App- or foundation-minted **`evaluationId`**. Persist **effective** axes + filter sets (+ optional preset name). |
| G-P50r | Runtime metadata | **resolved** | At least **`evaluatedAt`** and **`durationMs`**. Extra engine hints via annotations OK. Software/engine version **not** required for FULL completeness. |

### Content axes (G-P48r)

| Axis | Persists |
|------|----------|
| `results` | RESULTS-MODEL payload (`meta` + `outcomes` [+ suite `tree`]), filtered by G-P49r |
| `executionContext` | Policies executed, matcher defs, policy/suite bodies at T₀ (no input) |
| `input` | Replay pack — frozen fragment |

### Presets (convenience only)

| Preset | Content axes | Default filters |
|--------|----------------|-----------------|
| **EPHEMERAL** | none (no durable write) | n/a |
| **STANDARD** | `results` + `executionContext` | all statuses + all severities (incl. UNSPECIFIED) |
| **FULL** | `results` + `executionContext` + `input` | same as STANDARD |

**STANDARD view:** archive has `results` + `executionContext` ⇒ usable as STANDARD even if `input` is stored (omit pack on load-as-STANDARD).

---

## Store & modules

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P11r | Result tables | **resolved** | objs Flyway + JPA in `:objs-persistence` for RESULTS-MODEL shape + tags/annotations + **extensible `persist_profile` JSON** (axes/filters/preset/labeling). Tables/blobs for context and input are **axis-optional**. |
| G-P32r | Context vs input | **resolved** | Separate optional axes. STANDARD-style replay: stored policy content vs **any** fragment (input not guaranteed). FULL: + frozen input for audit. |
| G-P47r | Module split | **resolved** | Port in `:objs-policy-api`; JPA in `:objs-persistence`; wiring autoconfigure / policy-service. **Not** `PolicyRepository`. |
| G-P46r | Retention / cascade | **resolved** | When any durable content axis was written. Catalog Drop*/REPLACE **may** wipe archives; **no FK that blocks catalog Drop** (WI-002). |

---

## Philosophy (inherited — do not reopen)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P11s | Result **API** shape | **resolved** (C-27) | `meta` + `tree` + `outcomes`; EPHEMERAL object model |
| G-P32s | Replay intent | **resolved** (C-27) | Full config on recorded execution → C-33 axes |
| Catalog ≠ results | Boundary | **resolved** (C-28) | C-28 = Policy/Category/Suite only |
| Evaluation ≠ suite | Overlay | **resolved** (C-27 shape / C-33 store) | Archives are **not** hard-linked to suites. `kind` FLAT \| SUITE; FLAT / programmatic policy sets persist via `saveFlat` with null suite overlay. Suite id/name/tree are denormalized optional columns — **no catalog FK** (G-P46r). |

---

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| G-P48r | Spec + presets | 2026-09-07 | Content axes `results` / `executionContext` / `input`. Presets map content axes; filters + labeling are orthogonal. |
| G-P49r | Fine-tune sets | 2026-09-07 | Include-sets for outcome status and finding severity (incl. UNSPECIFIED). |
| G-P44r | Explicit save | 2026-09-07 | No auto-persist; no silent FULL/input. |
| G-P45r | Labeling | 2026-09-07 | name, description, tags, annotations; record effective spec. |
| G-P50r | Runtime | 2026-09-07 | evaluatedAt + durationMs. |
| G-P11r | Flyway shape | 2026-09-07 | RESULTS-MODEL + C-33 columns; axis-optional context/input storage. |
| G-P32r | Context ≠ input | 2026-09-07 | Independent axes; engine version out of pack completeness. |
| G-P47r | Ports | 2026-09-07 | Dedicated archive port; STANDARD view without requiring input. |
| G-P46r | Cascade | 2026-09-07 | Durable rows only; catalog Drop not blocked by result FKs. |
| Overlay | Evaluation ≠ suite | 2026-09-07 | FLAT/custom first-class; suite overlay optional denormalized metadata only. |
