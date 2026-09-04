# Story: policy-evaluate-core — flat policy evaluation MVP

**Slug:** `policy-evaluate-core`  
**Branch:** `policy-evaluate-core`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/policy-evaluate-core/`](.)  
**Backlog:** [C-24](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-31--normative-order) step **1 / 7**  
**Before:** `GraphFragment` / resolve path (shipped — [`graph-frontend-jgrapht`](../../completed/20260903-graph-frontend-jgrapht/STORY.md))  
**Next:** [C-26 `policy-drools`](../../planned/policy-drools/STORY.md)  
**Does not block:** C-20 store text search  
**Issue:** [#3](https://gitlab.qpointz.io/sandbox/bom-poc/-/work_items/3)  
**Umbrella design:** [`docs/design/policy/`](../../../design/policy/)  

**Gaps (this story only):** [`GAPS.md`](GAPS.md)  
**Examples / scenarios:** [`EXAMPLES.md`](EXAMPLES.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

---

## Cold start

1. **Goal + glossary + Normative (locked)** below — flat evaluate only.  
2. [`GAPS.md`](GAPS.md) — **all S1 gaps resolved** (WI-001).  
3. [`docs/design/policy/`](../../../design/policy/) — design folder: [`README`](../../../design/policy/README.md) index, S1 pages ([`model`](../../../design/policy/model.md), [`pipeline`](../../../design/policy/pipeline.md), [`evaluation-sequences`](../../../design/policy/evaluation-sequences.md), [`modules`](../../../design/policy/modules.md), [`results`](../../../design/policy/results.md), [`repository`](../../../design/policy/repository.md)), [`overview`](../../../design/policy/overview.md) for full-family vision.  
4. Follow-up stories for Drools / workbench / suites / seeds / batch / consumer — do **not** reopen here.

**Split note:** the former mega-story `objs-policy` was sliced so each story can **lock few gaps → implement → test**.

---

## Goal

Ship a **testable** foundation flat-evaluation path:

1. Spring-free `:objs-policy-api` + `:objs-policy-core`
2. Policy artefact model + in-memory `PolicyRepository`
3. Pipeline: resolve fragment → optional **PolicyContextWiring** → **`evaluate` (always includes applicability)** (`CUSTOM` stub engine)
4. Unified flat `EvaluationResult` with findings (`entities` / `edges` 0..n)
5. Explicit `NOT_APPLICABLE` (not pass/fail)

**Out of this story:** Drools, suites/roll-up, seeds, JPA/Flyway, batch pack, REST, example apps, OPA.

---

## Normative (locked — WI-001)

| Topic | Lock |
|-------|------|
| Modules | `:objs-policy-api` (contracts) + `:objs-policy-core` (impls); packages `org.poc.objs.policy.{api,core}` |
| Policy identity | **name** + immutable **serial version** on each create/update; no policy-level `enabled`; default resolve **latest**; outcomes cite executed version |
| Body | Opaque UTF-8 `String` + optional `contentType`; **`engineKind`** is String (`CUSTOM` only in S1) |
| Applicability | Optional `applicabilityKind`/`Body` on Policy; S1: blank or `ALWAYS_APPLY`; bound into `evaluate` (optional `applicability` preview); no skip-gate |
| Context | `PolicyEvaluationContext`; **`PolicyContextWirer`** only wires into context; wiring **first** after resolve |
| Results | Per-policy outcomes authoritative; optional flat aggregate helper (ERROR>FAIL>PASS>N/A); not suite semantics |
| Findings | Optional on all statuses; soft validation only; entities/edges 0..n |
| ERROR vs FAIL | FAIL = not satisfied; ERROR = engine/body/unknown-kind; continue other policies |
| Fragment ERRORs | Refuse evaluate (`PolicyEvaluationException`) |
| Repository | In-memory SPI+impl; save→new version; resolve latest\|version\|id |
| Orchestrator | Fixed `evaluate(fragment, policyRefs)`; wrappers later for suite/etc. |
| Product / service | No product rules in foundation; not on `:objs-service` by default |

---

## Glossary (S1)

| Term | Meaning here |
|------|----------------|
| **Policy** | Foundation artefact: metadata + `engineKind` + evaluation body + optional **`applicabilityKind`/`applicabilityBody`**. Identified by **name** + immutable **serial version**. **No** policy-level enabled flag. |
| **PolicyRepository** | In-memory store for policies (persistence = C-28); resolve **latest** or a specific version |
| **Graph fragment** | `(entities, edges)` via existing `GraphFragment` / resolve |
| **Applicability** | Gate **bound to** `evaluate` (always runs; cannot skip). Optional `applicability()` API for preview. S1: missing kind or `ALWAYS_APPLY` → in scope. |
| **PolicyContextWirer** | SPI that performs **PolicyContextWiring**: writes facts into `PolicyEvaluationContext` after resolve (not Enricher) |
| **Evaluation** | Core `evaluate(fragment, policyRefs)` always gated; outcomes cite policy version. Other entry shapes = wrappers later |
| **Finding** | Message with optional `entities` / `edges` UUID lists |
| **PolicyEngine** | Adapter SPI; this story ships **CUSTOM** stub only |

---

## Pipeline (S1)

```text
PolicyCollection (refs or loaded artefacts)
        │
        │     GraphFragment → resolve → (refuse if ERROR)
        │              │
        │              v
        │     PolicyContextWirer chain (optional) — FIRST: PolicyContextWiring
        │              │
        └──────────────┤
                       v
         evaluate(context, ...)  ← always includes applicability (sees wired context)
              │
              ├── NOT_APPLICABLE (+ reason)
              └── engine run → PASS / FAIL / ERROR (+ findings)
                       │
                       v
              EvaluationResult — **per-policy outcomes** (authoritative; cite version)
                                 optional flat overall aggregation (not suite semantics)

Optional: applicability(context, ...) preview — same gate, no engine side effects.
```

---

## Module map (S1)

| Module | Role |
|--------|------|
| `:objs-policy-api` | Model, SPIs, flat result DTOs; Spring-free; depends on `:objs-api` |
| `:objs-policy-core` | In-memory repository + orchestration; Spring-free |

Not in this story: `:objs-policy-drools`, `:objs-policy-service`, suite modules.

---

## Stages

| Stage | WIs | Ready | Stop gate |
|-------|-----|-------|-----------|
| 0 — Scaffold | WI-000 | done | Folder + backlog |
| 1 — Design lock | WI-001 | **done** | All S1 GAPS resolved |
| 2 — API | WI-002 | **done** | `:objs-policy-api` compiles + unit tests |
| 3 — Core | WI-003 | **done** | CUSTOM engine + in-memory repo tests green |
| 4 — Living docs | WI-004 | **done** | Design index marks S1 shipped vs follow-ups |

---

## Work Items

- [x] WI-000 — Story scaffold (umbrella start; later split) — (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock (S1 gaps only) — (`WI-001-design-lock.md`)
- [x] WI-002 — `:objs-policy-api` contracts — (`WI-002-policy-api.md`)
- [x] WI-003 — `:objs-policy-core` in-memory + pipeline — (`WI-003-policy-core.md`)
- [x] WI-004 — Living docs for S1 — (`WI-004-living-docs.md`)

---

## Out of scope

- Closing gaps owned by C-26…C-31
- Drools / OPA / suite trees / seed import / batch / REST / workbench
- Product regulations or SBOM ontology `Policy` entities
- Default dependency from `:objs-service`

---

## Acceptance (story-level)

- [x] S1 GAPS resolved or deferred
- [x] Caller loads policies in-memory, evaluates a fragment, gets per-policy PASS/FAIL/ERROR/NOT_APPLICABLE (inspectable individually)
- [x] Each per-policy outcome identifies the **executed policy version** (traceability; default resolve = latest)
- [x] Optional flat overall aggregation does **not** define suite success/failure
- [x] Findings support empty and multi entity/edge bindings
- [x] Fragment ERROR diagnostics refuse evaluation
- [x] Foundation types never require SBOM/AR domain classes
- [x] `./gradlew :objs-policy-api:test :objs-policy-core:test`

---

## Process notes

1. One WI at a time; `[x]` + commit + push per WI ([`RULES.md`](../../RULES.md)).
2. All WIs complete — story remains **in-progress** until user asks to close/archive.
3. Do not close this story until the user asks.
4. Do **not** implement follow-up stories on this branch.
