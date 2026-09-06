# Evaluation results — indicative relational model (shared)

**Status:** indicative (aligned with C-27 APIs; not a persistence schema lock)  
**Story:** [`STORY.md`](STORY.md) · Gaps: [`GAPS.md`](GAPS.md) (**G-P11s**) · Design: [`suites.md`](../../../../design/policy/suites.md) · Flat results: [`results.md`](../../../../design/policy/results.md)

Sketches a **relational** mental model for evaluation results reused across:

| Kind | Entry | Uses |
|------|--------|------|
| **Flat** | `evaluate(fragment, policyRefs)` | Core only |
| **Suite** | `evaluateSuite(...)` | Core + suite taxonomy overlay |
| **App-owned** | Product wraps foundation | Same **`evaluationId`**; app may mint/store the row |
| **Batch** (C-29, later) | batch pack | Same core per slice; batch links many `evaluationId`s |

C-27 suite API: **`meta` + `tree` + `outcomes`**, **no input persist**.

---

## Central identity: `evaluationId`

Prefer **`evaluationId`** over `executionId` / `run_id`:

| Name | Why not / why |
|------|----------------|
| `executionId` | Collides mentally with **`ExecutionStrategy`** (how policies are run / deduped) |
| `run_id` | Vague; “run” is overloaded |
| **`evaluationId`** | Names the **result aggregate** of one evaluate / evaluateSuite (or app-equivalent): outcomes + optional overlays |

**Semantics:**

- One **`evaluationId`** = one cohesive result set: meta (**including tags & annotations**) + zero or more `policy_outcome` rows + optional suite overlay.
- **May or may not** be suite-based (`kind`).
- **May be foundation-assigned** or **application-minted** (app persistence / correlation); foundation types still key results by this id when present.
- Does **not** imply input snapshot or fragment freeze (C-27: no input persist).

```text
evaluationId  →  PolicyEvaluation (meta, tags, annotations)
                    ├─ PolicyOutcome*     (always, when policies ran)
                    │     └─ Finding*
                    └─ Suite overlay?     (only if kind = SUITE)
                          ├─ folders / tags / annotations
                          └─ leaves → outcome refs
```

---

## Layering

```text
┌─────────────────────────────────────────────┐
│  Suite overlay (optional)                   │
│  folder tree · policy leaves → outcome refs │
├─────────────────────────────────────────────┤
│  Core (always for policy results)           │
│  PolicyEvaluation · PolicyOutcome · Finding │
│  keyed by evaluationId                      │
└─────────────────────────────────────────────┘
```

---

## Entity-relationship (indicative)

```mermaid
erDiagram
  POLICY_EVALUATION ||--o{ POLICY_OUTCOME : has
  POLICY_EVALUATION ||--o{ EVALUATION_TAG : tagged
  POLICY_EVALUATION ||--o{ EVALUATION_ANNOTATION : annotated
  POLICY_OUTCOME ||--o{ FINDING : has
  FINDING ||--o{ FINDING_ENTITY : binds
  FINDING ||--o{ FINDING_EDGE : binds

  POLICY_EVALUATION ||--o| SUITE_EVALUATION : suite_meta
  POLICY_EVALUATION ||--o{ SUITE_FOLDER_RESULT : tree
  SUITE_FOLDER_RESULT ||--o{ SUITE_FOLDER_RESULT : parent_of
  SUITE_FOLDER_RESULT ||--o{ SUITE_POLICY_LEAF : places
  SUITE_FOLDER_RESULT ||--o{ FOLDER_TAG : tagged
  SUITE_FOLDER_RESULT ||--o{ FOLDER_ANNOTATION : annotated
  SUITE_POLICY_LEAF }o--|| POLICY_OUTCOME : refs

  POLICY_EVALUATION {
    uuid evaluation_id PK
    string kind
    timestamptz evaluated_at
    string execution_strategy_kind
    string overall_status
    string overall_severity
  }

  EVALUATION_TAG {
    uuid evaluation_id FK
    string tag
  }

  EVALUATION_ANNOTATION {
    uuid evaluation_id FK
    string key
    string value
  }

  SUITE_EVALUATION {
    uuid evaluation_id PK_FK
    uuid suite_id
    string suite_name
    string scope_kind
    string rollup_strategy_kind
  }

  POLICY_OUTCOME {
    uuid outcome_id PK
    uuid evaluation_id FK
    string policy_name
    long policy_serial
    string status
    string severity
  }

  SUITE_POLICY_LEAF {
    uuid leaf_id PK
    uuid folder_result_id FK
    uuid outcome_id FK
  }
```

---

## Core tables

### `policy_evaluation` (identity = `evaluation_id`)

| Column | Type | Notes |
|--------|------|--------|
| **`evaluation_id`** | UUID PK | Central identity |
| `kind` | text | `FLAT` \| `SUITE` \| app-defined extension |
| `evaluated_at` | timestamptz | |
| `execution_strategy_kind` | text null | How policies were executed (dedupe, …); distinct from evaluation id |
| `overall_status` | text null | Flat optional aggregate; suite = root roll-up |
| `overall_severity` | text null | |
| `origin` | text null | Optional: `FOUNDATION` \| `APPLICATION` \| correlation hints |

### `evaluation_tag` / `evaluation_annotation`

Caller- or app-supplied metadata on the **evaluation instance** (same idea as policy/folder tags & annotations; scoped to `evaluationId`).

| Table | Columns | Notes |
|-------|---------|--------|
| `evaluation_tag` | `evaluation_id`, `tag` | e.g. trim/lowercase rules as C-32 when locked for this surface |
| `evaluation_annotation` | `evaluation_id`, `key`, `value` | Objs-shaped map; empty OK |

Use for correlation (env, trigger, ticket, portfolio id), reporting filters, and app-owned runs — independent of suite folder tags.

### `policy_outcome`

| Column | Type | Notes |
|--------|------|--------|
| `outcome_id` | UUID PK | |
| **`evaluation_id`** | UUID FK | → `policy_evaluation` |
| `policy_name` / `policy_serial` / `policy_id` | | As G-P27s |
| `engine_kind` | text | |
| `status` | text | Flat may include `NOT_APPLICABLE`; suite overlay omits N/A leaves |
| `severity` | text null | |
| `message` / `not_applicable_reason` | text null | |

### `finding` (+ bindings)

Findings **only** under outcomes; tree leaves **ref** `outcome_id`.

---

## Suite overlay (only if `kind = SUITE`)

### `suite_evaluation` (1:1 extension)

| Column | Type | Notes |
|--------|------|--------|
| **`evaluation_id`** | UUID PK/FK | |
| `suite_id` / `suite_name` | | |
| `scope_kind` / `scope_payload_json` | | |
| `rollup_strategy_kind` | text | |

### `suite_folder_result` · tags/annotations · `suite_policy_leaf`

Same semantics as before: DISABLED omitted; IGNORED present with `votes = false`; leaves FK → `policy_outcome`. Dedupe ⇒ many leaves, one `outcome_id`.

---

## Mapping to in-memory APIs

| Relational | Flat | Suite |
|------------|------|--------|
| `evaluation_id` + `policy_evaluation` (+ tags/annotations) | Optional wrapper / app correlation | `SuiteEvaluationResult.meta` incl. tags/annotations |
| `suite_evaluation` | — | suite id, scope, rollup kind |
| folder / leaf overlay | — | `tree` with `outcomeRef` |
| `policy_outcome` + `finding` | `EvaluationResult.outcomes` | `outcomes` |

Apps may allocate `evaluationId` before calling foundation, pass it through, and persist the same relational shape outside objs-policy.

---

## Example query shapes

- All outcomes: `WHERE evaluation_id = ?`  
- Suite tree: overlay tables for that `evaluation_id`  
- App report: join on `evaluation_id` without caring if suite overlay exists  

---

## Naming summary

| Concept | Name |
|---------|------|
| Result aggregate id | **`evaluationId`** / `evaluation_id` |
| How policies are run (dedupe, …) | `executionStrategyKind` (not the id) |
| Suite roll-up algorithm | `rollupStrategyKind` |

---

## Out of scope here

- Flyway / JPA for **catalog** (Policy / Category / Suite) — **C-28**  
- Flyway / JPA for **evaluation results** / input persist — **later** (not mandated by C-28; C-27 deferred G-P11s store + G-P32s)  
- G-P27s identity field names  
- Batch header detail (C-29)  
