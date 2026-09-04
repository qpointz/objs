# WI-002 — `:objs-policy-api` contracts

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — API  
**Status:** done  
**Depends on:** WI-001  
**Examples:** **—**

## Goal

Spring-free **`:objs-policy-api`**: Policy model, flat SPIs, findings, flat `EvaluationResult`. No Drools, suites, batch types, or repository impl.

**Behavioural sequences (contracts must support):** [`evaluation-sequences.md`](../../../design/policy/evaluation-sequences.md)

## Scope

- [x] Gradle include + module skeleton (`G-P1` / `G-P25`)
- [x] Policy artefact types (`G-P3`–`G-P5`) — name + **serial version** on create/update; no `enabled`; outcomes carry executed version
- [x] `ApplicabilitySelector` (+ decision / N/A reason)
- [x] `PolicyContextWirer` SPI (PolicyContextWiring; not Enricher)
- [x] `PolicyEngine` SPI
- [x] `PolicyRepository` interface (no suite APIs)
- [x] Flat `EvaluationResult` + `Finding` (`entities`/`edges` 0..n)
- [x] Orchestrator interface if it lives in api (`G-P15`)
- [x] Unit tests for value-type invariants if any (`aggregateOverall`)

## Boundaries

- Depends on `:objs-api` only
- No Spring, JPA, Drools, SBOM, AR
- No suite / batch / seed types in this story

## Out of scope

- Core orchestration / in-memory impl (WI-003)
- Follow-up modules (C-26+)

## Acceptance

- [x] Module compiles; tests green
- [x] No types that force suite or Drools dependencies
