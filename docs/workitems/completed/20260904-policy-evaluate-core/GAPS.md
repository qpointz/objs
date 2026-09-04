# Gaps — policy-evaluate-core (C-24 / S1)

Status values: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**WI-001 process:** **complete** 2026-09-04 — all S1 rows `resolved`. See Decision log.  
Follow-up stories own their own GAPS — ignore those here.

Related: [`STORY.md`](STORY.md) · Design: [`docs/design/policy/`](../../../design/policy/README.md) ([`overview`](../../../design/policy/overview.md), [`model`](../../../design/policy/model.md), [`pipeline`](../../../design/policy/pipeline.md), [`evaluation-sequences`](../../../design/policy/evaluation-sequences.md), [`modules`](../../../design/policy/modules.md), [`results`](../../../design/policy/results.md), [`repository`](../../../design/policy/repository.md))

---

## Resolved (S1 — confirmed)

| # | Topic | Status | Decision | Your call |
|---|--------|--------|----------|-----------|
| G-P1 | Module split | **resolved** | `:objs-policy-api` = model + SPIs + result DTOs; `:objs-policy-core` = in-memory repo + orchestrator + ALWAYS_APPLY gate + CUSTOM stub. S1 only these two modules | **confirmed** |
| G-P2 | Package root | **resolved** | `org.poc.objs.policy` (`…api` / `…core` subpackages) | **confirmed** |
| G-P3 | Policy identity & versioning | **resolved** | See Decision log: serial revision on each create/update; **no** policy-level `enabled`; results must cite executed version; default ref = **latest** | **confirmed** |
| G-P4 | Policy body | **resolved** | Opaque UTF-8 `String body` + optional `contentType` (encoding hint only); engine association is **`engineKind`** (G-P5), not contentType; no content-hash in S1 | **confirmed** |
| G-P5 | `engineKind` | **resolved** | **String** (no enum); S1 implements **`CUSTOM`** only; other kinds are plain strings for later adapters; unknown/unimplemented → per-policy ERROR | **confirmed** |
| G-P6 | Applicability artefact | **resolved** | Policy **has** optional `applicabilityKind` + `applicabilityBody` (forward-looking). S1 only implements **`ALWAYS_APPLY`** (also when kind blank); body ignored for that kind. Other kinds extensible later; unimplemented kind → ERROR (unless overridden) | **confirmed** |
| G-P7 | Applicability bound to eval | **resolved** | Engine/evaluator may expose **`applicability(...)`** for preview. **`evaluate(...)` always runs applicability first** — no evaluate-without-gate. One outcome space: N/A(+reason) and/or PASS/FAIL/ERROR | **confirmed** |
| G-P8 | Default gate | **resolved** | Inside applicability/evaluate: **`ALWAYS_APPLY`** (+ missing kind); extensible kinds later | **confirmed** |
| G-P9 | Context wiring SPI | **resolved** | Introduce **`PolicyEvaluationContext`** (resolved fragment + mutable sidecar bag). Optional **`PolicyContextWirer`** SPI (**PolicyContextWiring**); only wires values into context — no built-in wirers, no topology rewrite, no product logic in S1. **Not** named Enricher | **confirmed** |
| G-P10 | Wiring vs apply order | **resolved** | `PolicyContextWirer` chain runs at the **very beginning** (after resolve, before applicability/evaluate) so applicability can use wired context data; empty chain = fragment-only context | **confirmed** |
| G-P11 | Flat `EvaluationResult` | **resolved** | **Primary:** ordered per-policy outcomes (must include executed policy version). Callers must be able to inspect each. **Overall status** is optional flat **aggregation helper** only — not suite semantics. A failed policy does **not** imply a failed suite; suite/app infer roll-up later (C-27) | **confirmed** |
| G-P11f | Finding model | **resolved** | severity INFO\|WARNING\|ERROR, message, optional ruleId; `entities`/`edges` 0..n (empty OK); engine order; ids need not exist in fragment | **confirmed** |
| G-P12 | Aggregate status (flat) | **resolved** | Optional convenience utility only: ERROR if any ERROR; else FAIL if any FAIL; else PASS if any PASS; else NOT_APPLICABLE — **not** suite roll-up | **confirmed** |
| G-P12f | Findings vs status | **resolved** | Findings **optional** on every status (incl. FAIL with zero findings — e.g. absence-of-evidence). If present: **soft validation** only (warn/diagnostics; never reject outcome for missing/odd findings or unbound ids) | **confirmed** |
| G-P13 | In-memory repository | **resolved** | SPI + `InMemoryPolicyRepository` in core; JPA/Flyway deferred to C-28 | **confirmed** |
| G-P14 | Repository API | **resolved** | `save` (always new serial version) / `get` / `list` / `resolve(ref)` where ref = name+`latest`\|version or id; no policy `enabled`; no suite APIs; no pagination in S1 | **confirmed** |
| G-P15 | Orchestrator entry | **resolved** | **Fixed core contract:** `evaluate(fragment, policyRefs)` (always gated) + optional `applicability(fragment, policyRefs)` preview. No parameterless `evaluate()`. Suite/batch/other input shapes = **wrappers** (or later stories) that map into this contract — not alternate core overloads in S1 | **confirmed** |
| G-P16 | ERROR vs FAIL | **resolved** | **FAIL** = policy evaluation failed (rule/condition not satisfied). **ERROR** = engine/body/unknown-kind/infrastructure failure. Continue remaining policies; overall helper uses G-P12 | **confirmed** |
| G-P17 | Fragment ERROR diagnostics | **resolved** | After resolve, if `hasErrors()` → **refuse**: throw `PolicyEvaluationException` (like materializers); do not evaluate | **confirmed** |
| G-P25 | `settings.gradle.kts` | **resolved** | Include both modules when WI-002 lands | **confirmed** |

---

## Philosophy (locked with WI-001)

| # | Topic | Status | Decision |
|---|--------|--------|----------|
| G-P34 | Family naming | **resolved** | `objs-policy*` not assessment (**confirmed**) |
| G-P35 | Applicability first-class | **resolved** | Bound into evaluate (+ optional applicability API); N/A ≠ pass/fail (**confirmed**) |
| G-P36f | Findings bind nodes/edges | **resolved** | `entities`/`edges` 0..n; empty OK (confirmed with G-P11f) |
| G-P38 | No product rules in foundation | **resolved** | Examples/apps own content (**confirmed**) |
| G-P39 | Not default on `:objs-service` | **resolved** | Opt-in later (C-31/C-30) (**confirmed**) |
| G-P40 | Input is GraphFragment | **resolved** | Same resolve path as Gremlin/JGraphT (**confirmed**) |
| G-P41 | Policy ≠ graph entity | **resolved** | Dedicated `PolicyRepository` (**confirmed**) |

---

## Owned by follow-up stories (skip in S1 review)

| Story | Gaps |
|-------|------|
| [C-26 `policy-drools`](../../in-progress/policy-drools/GAPS.md) | G-P18–20 |
| [C-31 `policy-workbench`](../../planned/policy-workbench/GAPS.md) | G-P23* |
| [C-27 `policy-suites`](../../planned/policy-suites/GAPS.md) | G-P26s–33 |
| [C-28 `policy-seeds-persistence`](../../planned/policy-seeds-persistence/GAPS.md) | persistence + seeds |
| [C-29 `policy-batch`](../../planned/policy-batch/GAPS.md) | G-P41b–43b |
| [C-30 `policy-consumer`](../../planned/policy-consumer/GAPS.md) | G-P21–22 |

---

## Out of sequence / deferred

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | OPA / Rego adapter | **deferred** | After Drools |
| G-X2 | Compliance scoring product | **deferred** | Product/UX |
| G-X3 | Fingerprint approval workflows | **deferred** | SBOM product |
| G-X4 | Replace persist-gate validation | **cancelled** | Orthogonal |
| G-X5 | Full policy authoring / suite tree UI | **deferred** | Beyond C-31 |
| G-X6 | Multi-tenant ACL / sync | **deferred** | Ops |
| G-X7 | Reuse SBOM portfolio tables for suites | **cancelled** | Separate stores |
| G-X8 | Portfolio × suite matrix UI | **deferred** | Product |
| G-P24 | SBOM ontology Policy rename | **deferred** | Docs-only if ever |

---

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| SPLIT | Vertical story split | 2026-09-04 | Mega `objs-policy` → S1 + follow-ups |
| UI | C-31 after Drools | 2026-09-04 | Tactical workbench before suites |
| PROC | Gap-by-gap confirm | 2026-09-04 | Proposals listed; no S1 locks until user confirms each |
| G-P2 | Package root | 2026-09-04 | `org.poc.objs.policy` (+ `api` / `core` subpackages) |
| G-P1 | Module split | 2026-09-04 | api = contracts/DTOs/SPIs; core = in-memory repo + gated evaluate + CUSTOM + ALWAYS_APPLY; S1 only these two |
| G-P3 | Policy identity & versioning | 2026-09-04 | No policy-level `enabled`. Name + serial version on create/update; evaluate **latest** by default; outcomes cite executed version; suites may ref `latest` or a version |
| G-P4 | Policy body | 2026-09-04 | Opaque UTF-8 `String body` + optional `contentType`; engine via **`engineKind`** (G-P5) |
| G-P5 | `engineKind` | 2026-09-04 | **String**, not enum. S1: only **`CUSTOM`** |
| G-P6 | Applicability artefact | 2026-09-04 | Optional `applicabilityKind` + `applicabilityBody` on Policy. S1: blank or `ALWAYS_APPLY` only |
| G-P7 | Applicability bound to eval | 2026-09-04 | Optional `applicability()` preview API; `evaluate()` **always** gated (no skip). N/A and eval results in one outcome space |
| G-P8 | Default gate | 2026-09-04 | `ALWAYS_APPLY` (+ missing kind) inside those APIs |
| G-P9 | Enricher / context | 2026-09-04 | S1: introduce `PolicyEvaluationContext` + wiring SPI only; no special enricher behaviour beyond putting things into context |
| G-P9 | Rename Enricher → Wirer | 2026-09-04 | SPI = **`PolicyContextWirer`**; noun = **PolicyContextWiring** (avoid Enricher — used elsewhere) |
| G-P10 | Enrich order | 2026-09-04 | Enrich **first** (after resolve) so applicability/evaluate see enriched context |
| G-P10 | Wiring order (rename) | 2026-09-04 | **Wire** first (after resolve); same order lock, Enricher naming dropped |
| G-P11 | Flat EvaluationResult | 2026-09-04 | Per-policy outcomes are authoritative (incl. version). Overall status = optional flat aggregation only; suite failure inference is suite/app (C-27), not S1 |
| G-P11f | Finding model | 2026-09-04 | INFO\|WARNING\|ERROR + message + optional ruleId; entities/edges 0..n; empty OK; ids need not be in fragment |
| G-P12 | Flat aggregate helper | 2026-09-04 | Convenience only: ERROR > FAIL > PASS > N/A; not suite semantics |
| G-P12f | Findings vs status | 2026-09-04 | Findings optional (FAIL may have none). Soft validation only — never hard-fail an outcome over findings shape/bindings |
| G-P13 | In-memory repository | 2026-09-04 | `PolicyRepository` SPI + in-memory impl in core; persistence = C-28 |
| G-P14 | Repository API | 2026-09-04 | save→new serial version; get/list/resolve(latest\|version\|id); no enabled; no suite APIs; no S1 pagination |
| G-P15 | Orchestrator entry | 2026-09-04 | Fixed `evaluate(fragment, policyRefs)` + optional `applicability(...)`. Wrappers adapt suite/other inputs later; no no-arg evaluate |
| G-P16 | ERROR vs FAIL | 2026-09-04 | FAIL = evaluation failed (not satisfied); ERROR = engine/body/unknown-kind failure; continue other policies |
| G-P17 | Fragment ERROR diagnostics | 2026-09-04 | Refuse evaluate when resolved fragment has ERROR diagnostics (`PolicyEvaluationException`) |
| G-P25 | settings.gradle.kts | 2026-09-04 | Include `:objs-policy-api` and `:objs-policy-core` when WI-002 lands |
| G-P34 | Family naming | 2026-09-04 | `objs-policy*` (not assessment) |
| G-P35 | Applicability first-class | 2026-09-04 | Bound into evaluate (+ optional applicability preview); N/A ≠ pass/fail |
| G-P38 | No product rules | 2026-09-04 | Foundation has no regulatory/product policy content; examples/apps own packs |
| G-P39 | Not on objs-service by default | 2026-09-04 | Opt-in later (workbench C-31 / consumer C-30); not on `:objs-service` classpath by default |
| G-P40 | Input GraphFragment | 2026-09-04 | Resolve via GraphFragmentPolicy first (same as Gremlin/JGraphT) |
| G-P41 | Policy ≠ graph entity | 2026-09-04 | Dedicated PolicyRepository; not bom_entity / graph pool |
