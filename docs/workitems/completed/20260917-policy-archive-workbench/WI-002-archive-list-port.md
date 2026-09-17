# WI-002 — Archive list + load port / JPA

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-001  

## Cold start

- Port today: [`EvaluationArchive.kt`](../../../../objs-policy-api/src/main/kotlin/org/poc/objs/policy/api/EvaluationArchive.kt) (`load` exists; **add `list` + summary**).  
- Impl: [`JpaEvaluationArchive.kt`](../../../../objs-persistence/src/main/kotlin/org/poc/objs/core/persistence/policy/JpaEvaluationArchive.kt) · [`PolicyEvaluationDaos.kt`](../../../../objs-persistence/src/main/kotlin/org/poc/objs/core/persistence/policy/PolicyEvaluationDaos.kt).  
- Tests: [`JpaEvaluationArchiveTest.kt`](../../../../objs-persistence/src/test/kotlin/org/poc/objs/core/persistence/policy/JpaEvaluationArchiveTest.kt).  
- No Flyway — list over existing V8 tables. Normative list fields: [`GAPS.md`](GAPS.md) Decision log (after WI-001).

## Goal

Extend `EvaluationArchive` with `EvaluationArchiveSummary` + `list(...)`. Implement in `JpaEvaluationArchive` / `PolicyEvaluationDao`.

## Scope

- API types + port method(s) per GAPS Decision log
- JPA list by `evaluated_at DESC`; optional kind/tag filters
- Unit tests (save → list → load round-trip)

## Acceptance

- [x] Port compiles; JPA list returns summaries without hydrating outcomes
- [x] Existing load/delete behaviour unchanged
- [x] Tests cover empty list, ordered list, kind filter
