# WI-003 — Archive port + round-trip tests

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-002  

## Goal

Ship the **archive/result port** in `:objs-policy-api`, JPA-backed impl, and tests that prove PersistSpec / presets / filters / labeling / STANDARD view.

## Scope

- [x] Types: `PersistSpec`, axes, filters, presets, `EvaluationArchiveDocument`, `EvaluationArchive`
- [x] `JpaEvaluationArchive` in `:objs-persistence`
- [x] Beans in `ObjsPolicyPersistenceAutoConfiguration`
- [x] Tests: ephemeral no-op; STANDARD flat + filters/labeling; FULL→STANDARD omits input; suite tree; results-only axes

## Acceptance

- [x] `JpaEvaluationArchiveTest` green
- [x] EPHEMERAL save returns null / no durable requirement
- [x] Evaluate APIs unchanged (explicit save only)
