# WI-002 — Suite model + repository APIs

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-001  

## Goal

Add suite / folder / matcher contracts and in-memory `SuiteRepository` in `:objs-policy-api` / `:objs-policy-core`, per WI-001 locks (see [`suites.md`](../../../../design/policy/suites.md)).

## Scope

- [x] Suite + folder tree (tags, annotations, severityConfig, participation ENABLED/DISABLED/IGNORED); suite `rollUpStrategyKind`
- [x] Policy matcher types (include/exclude; kinds from **G-P27s**)
- [x] `SuiteRepository` + in-memory implementation + tree/matcher validation (per-step uniqueness from G-P27s)
- [x] `SuiteRollUpStrategy` + `ExecutionStrategy` SPI surfaces (built-ins may land in WI-003)
- [x] Shared result types keyed by **`evaluationId`** (meta tags/annotations; tree refs outcomes) as needed for API compile
- [x] Unit tests

## Out of scope

- Full `evaluateSuite` orchestration (WI-003)
- JPA / seeds (C-28)
- HTTP / UI
- Input persist

## Acceptance

- [x] Suites persist in-memory with folder tree + matchers
- [x] Flat `evaluate` unchanged
