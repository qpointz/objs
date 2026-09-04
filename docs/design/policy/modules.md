# Policy modules (S1 shipped)

**Normative:** G-P1, G-P2, G-P25 · [`GAPS.md`](../../workitems/completed/20260904-policy-evaluate-core/GAPS.md)

---

## Module map

```mermaid
flowchart TB
  api_mod[":objs-api"]
  papi[":objs-policy-api"]
  pcore[":objs-policy-core"]
  apps[Example apps / tests]
  api_mod --> papi
  papi --> pcore
  pcore --> apps
  papi --> apps
```

| Module | Responsibility | Depends on |
|--------|----------------|------------|
| `:objs-policy-api` | Model, refs, context **types**, result DTOs, SPI **interfaces**, exceptions | `:objs-api` |
| `:objs-policy-core` | In-memory repo, PolicyContextWiring, orchestrator, ALWAYS_APPLY gate, CUSTOM stub | `:objs-policy-api` |

**Packages:** `org.poc.objs.policy.api.*` / `org.poc.objs.policy.core.*`  
**Settings:** both included in root `settings.gradle.kts`.

**Not in S1:** `:objs-policy-drools`, `:objs-policy-service`, suite modules, JPA.

### Shipped types (C-24)

| Layer | Types |
|-------|--------|
| api | `Policy`, `PolicyWrite`, `PolicyRef`, `PolicyEvaluationContext`, `PolicyOutcome` / `EvaluationResult`, `Finding`, `aggregateOverall`, `PolicyContextWirer`, `ApplicabilitySelector`, `PolicyEngine`, `PolicyRepository`, `PolicyEvaluator`, `PolicyEvaluationException` |
| core | `InMemoryPolicyRepository`, `DefaultPolicyEvaluator`, `AlwaysApplyApplicabilitySelector`, `CustomPolicyEngine` |

---

## What lives where

```mermaid
flowchart LR
  subgraph apiLayer [objs-policy-api]
    Pol[Policy / PolicyRef]
    Ctx[PolicyEvaluationContext]
    Res[EvaluationResult / Finding]
    SPI[PolicyRepository / PolicyContextWirer / Engine / Evaluator interfaces]
  end
  subgraph coreLayer [objs-policy-core]
    Mem[InMemoryPolicyRepository]
    Orch[DefaultPolicyEvaluator]
    Gate[AlwaysApplyApplicabilitySelector]
    Custom[CustomPolicyEngine]
  end
  SPI -.->|implemented by| coreLayer
  Orch --> Mem
  Orch --> Gate
  Orch --> Custom
```

### `:objs-policy-api`

- Policy artefact + PolicyRef  
- `PolicyEvaluationContext`  
- `EvaluationResult`, per-policy outcome, `Finding`  
- Optional aggregate helper **function** (`aggregateOverall`) — see [`results.md`](results.md)  
- Ports: repository, PolicyContextWirer, engine, evaluator  
- `PolicyEvaluationException` (fragment refuse, etc.)

### `:objs-policy-core`

- `InMemoryPolicyRepository` (serial versions)  
- Orchestrator: resolve → PolicyContextWiring → gated evaluate  
- Default ALWAYS_APPLY behaviour  
- CUSTOM stub for tests (`PASS`/`FAIL`/`ERROR` body + optional `FINDING|…` lines)  
- No Spring; no Drools on classpath  

---

## Classpath / product boundary (G-P38, G-P39)

| Rule | Meaning |
|------|---------|
| No product rules in foundation | Concrete DRL / “IT Governance” packs live in examples/apps |
| Not on `:objs-service` by default | Opt-in later (C-31 workbench / C-30 consumer), like jgrapht-service |

---

## Later modules (roadmap)

| Module / story | When |
|----------------|------|
| `:objs-policy-drools` | C-26 |
| Workbench UI + thin HTTP | C-31 |
| Suites | C-27 (may stay in api/core types + core orchestration) |
| JPA + seeds | C-28 |
| Batch pack | C-29 |
| Example/REST extras | C-30 |
