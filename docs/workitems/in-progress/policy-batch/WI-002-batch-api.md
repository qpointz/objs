# WI-002 — Batch API + sequential impl + tests

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Implement  
**Status:** done  
**Depends on:** WI-001  
**Examples:** **SBOM** (`SequentialPortfolioAssessmentRunner`)

## Deliverables

- [x] `:objs-policy-api` — batch types, `PolicyBatchEvaluator`, `PolicyBatchExecutor`
- [x] `:objs-policy-core` — `SequentialPolicyBatchExecutor` + `DefaultPolicyBatchEvaluator`
- [x] Autoconfig beans in `:objs-policy-service`
- [x] Unit tests (order, continue-on-error, suite vs refs)
- [x] SBOM runner → batch API

## Acceptance

- [x] `./gradlew :objs-policy-core:test --tests '*SequentialPolicyBatchExecutorTest*'`
- [x] One failing subject does not discard others
