# WI-002 — `:objs-policy-api` contracts

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — API contracts  
**Status:** planned  
**Depends on:** WI-001 (all open GAPS closed or deferred)  
**Examples:** **—**

## Goal

Add Spring-free **`:objs-policy-api`** with the locked data model, SPI interfaces, and result DTOs. No repository implementation, no Drools.

## Scope

- [ ] Gradle include + module skeleton (per locked `G-P1` / `G-P25`)
- [ ] Policy artefact types (identity, engineKind, bodies, metadata) per WI-001
- [ ] **PolicySuite / suite node / membership** types (hierarchy + M:N) per WI-001
- [ ] `ApplicabilitySelector` (+ decision / N/A reason types)
- [ ] `FragmentEnricher` (or locked equivalent)
- [ ] `PolicyEngine` SPI
- [ ] `PolicyRepository` (+ suite repository interface if split) — no impl
- [ ] `EvaluationResult` / suite result / **Finding** (`entities` 0..n, `edges` 0..n) / **per-node roll-up** types
- [ ] **Batch / result-pack** request + response types (opaque `subjectKey`; no matrix types)
- [ ] Orchestrator interface if public API lives here (else core-only — per `G-P15`)
- [ ] Unit tests for value-type invariants only (if any)

## Boundaries

- Depends on `:objs-api` for `GraphFragment` / `ResolvedGraphFragment` only
- Must not depend on Spring, JPA, Drools, SBOM, or AR
- Must not embed product rule content or product suite trees

## Out of scope

- Repository backing store (WI-003)
- Drools adapter (WI-004)
- REST (WI-005)

## Acceptance

- [ ] `:objs-policy-api` compiles and tests pass
- [ ] SPI method signatures match WI-001 locks (incl. suite + roll-up + **finding bindings** + **batch pack**)
- [ ] No engine implementation in this module
- [ ] No portfolio/matrix types in the API module
