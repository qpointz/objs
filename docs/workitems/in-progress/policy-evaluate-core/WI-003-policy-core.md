# WI-003 — `:objs-policy-core` in-memory + pipeline

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Core  
**Status:** done  
**Depends on:** WI-002  
**Examples:** **—**

## Goal

Implement resolve → **PolicyContextWiring** → **gated** evaluate with **in-memory** `PolicyRepository`, S1 ALWAYS_APPLY gate, and a **CUSTOM** stub `PolicyEngine`. Prove S1 acceptance with unit tests mapped to [`evaluation-sequences.md`](../../../design/policy/evaluation-sequences.md).

## Scope

- [x] `:objs-policy-core` module
- [x] In-memory `PolicyRepository`
- [x] Orchestrator `evaluate(fragment, policyRefs)`
- [x] Refuse evaluation when resolved fragment has ERROR diagnostics
- [x] CUSTOM stub engine fixtures (PASS/FAIL/ERROR)
- [x] Applicability partitioning → N/A entries
- [x] Findings empty + multi-binding fixtures
- [x] Aggregate status per locked `G-P12`

## Out of scope

- JPA / Flyway / seeds (C-28)
- Drools (C-26)
- Suites (C-27)
- Batch (C-29)

## Acceptance

- [x] `./gradlew :objs-policy-api:test :objs-policy-core:test` green
- [x] Story acceptance bullets for runtime behaviour covered by tests
