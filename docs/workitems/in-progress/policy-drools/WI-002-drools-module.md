# WI-002 — Drools module + fixture tests

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  
**Depends on:** WI-001  

## Goal

Ship `:objs-policy-drools` with maps-first facts, KB cache by policy revision, and fixture DRL tests.

## Deliverables

- [x] `libs.versions.toml` — `drools` **10.2.0**, `drools-bom`, `drools-engine`, `drools-xml-support`
- [x] `:objs-policy-drools` in `settings.gradle.kts`
- [x] `PolicyEngineKinds.DROOLS` on api
- [x] `FactMap`, `DroolsEvaluationScratch`, `PolicyKnowledgeBaseCache`, `DroolsPolicyEngine`
- [x] Fixture tests (PASS / FAIL / compile ERROR / KB reuse / orchestrator + wirer)
- [x] JaCoCo disabled on this module’s tests (Drools lexer `MethodTooLargeException`)

## Acceptance

- [x] `./gradlew :objs-policy-api:test :objs-policy-core:test :objs-policy-drools:test` passes
- [x] Drools not on api/core classpaths
