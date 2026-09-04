# Evaluation sequence diagrams (S1)

**Audience:** WI-002+ implementers and the living-docs / documenting pass  
**Normative:** C-24 WI-001 · [`pipeline.md`](pipeline.md) · [`GAPS.md`](../../workitems/completed/20260904-policy-evaluate-core/GAPS.md)  
**Shipped types:** `DefaultPolicyEvaluator`, `InMemoryPolicyRepository`, `AlwaysApplyApplicabilitySelector`, `CustomPolicyEngine`

Use these sequences as the **behavioural checklist**. They match the shipped orchestrator; prefer updating this page if behaviour is clarified further.

---

## Participants (shared legend)

| Alias | Role |
|-------|------|
| Caller | App, test, or later wrapper (suite/batch) |
| Evaluator | `DefaultPolicyEvaluator` (`:objs-policy-core`) |
| Resolve | `GraphFragmentPolicy` (from `:objs-api`) |
| Repo | `InMemoryPolicyRepository` / `PolicyRepository` |
| Wirer | `PolicyContextWirer` chain (0..n) — PolicyContextWiring |
| Gate | `ApplicabilitySelector` (default `AlwaysApplyApplicabilitySelector`) |
| Engine | `PolicyEngine` map (default `CUSTOM` → `CustomPolicyEngine`) |

---

## 1. Happy path — `evaluate(fragment, policyRefs)`

Multi-policy run: resolve once, wire context once, then **per ref** resolve policy → gate → engine. Failures on one policy **do not** stop others.

```mermaid
sequenceDiagram
  autonumber
  actor Caller
  participant Ev as Evaluator
  participant GFP as GraphFragmentPolicy
  participant Repo as PolicyRepository
  participant Wirer as PolicyContextWirers
  participant Gate as Applicability
  participant Eng as PolicyEngine

  Caller->>Ev: evaluate(fragment, policyRefs)
  Ev->>GFP: resolve(fragment)
  GFP-->>Ev: resolved + diagnostics (no ERROR)
  Ev->>Ev: build PolicyEvaluationContext(resolved)
  loop each PolicyContextWirer (optional)
    Ev->>Wirer: wire(context)
    Wirer-->>Ev: context updated (wire only)
  end

  loop each policyRef
    Ev->>Repo: resolve(policyRef)
    Repo-->>Ev: Policy(name, version, engineKind, body, …)
    Ev->>Gate: decide(context, policy)
    alt NOT_APPLICABLE
      Gate-->>Ev: N/A + reason
      Note over Ev: append PolicyOutcome(NOT_APPLICABLE, version)
    else in scope
      Ev->>Eng: evaluate(context, policy)
      Eng-->>Ev: PASS | FAIL | ERROR + findings?
      Note over Ev: append PolicyOutcome(status, version, findings)
    end
  end

  Ev->>Ev: optional overall = ERROR > FAIL > PASS > N/A
  Ev-->>Caller: EvaluationResult(outcomes[, overall])
```

**Implement / document:**

- Outcomes **must** cite executed **name + serial version**.  
- Overall status is **optional convenience** only ([`results.md`](results.md)).  
- Empty wirer list is valid.

---

## 2. Fragment resolve ERROR — refuse evaluate

Do **not** run PolicyContextWiring, resolve policies, or call engines when fragment resolve reports ERROR diagnostics.

```mermaid
sequenceDiagram
  autonumber
  actor Caller
  participant Ev as Evaluator
  participant GFP as GraphFragmentPolicy

  Caller->>Ev: evaluate(fragment, policyRefs)
  Ev->>GFP: resolve(fragment)
  GFP-->>Ev: diagnostics include ERROR
  Ev-->>Caller: throw PolicyEvaluationException
  Note over Ev,Caller: No wiring, no PolicyOutcome list
```

Same refuse rule applies to **`applicability(...)`** if it shares the resolve preamble (recommended).

---

## 3. Applicability preview — `applicability(fragment, policyRefs)`

Same resolve → PolicyContextWiring → gate path as evaluate; **no** engine invocation and **no** evaluation side effects.

```mermaid
sequenceDiagram
  autonumber
  actor Caller
  participant Ev as Evaluator
  participant GFP as GraphFragmentPolicy
  participant Repo as PolicyRepository
  participant Wirer as PolicyContextWirers
  participant Gate as Applicability

  Caller->>Ev: applicability(fragment, policyRefs)
  Ev->>GFP: resolve(fragment)
  GFP-->>Ev: resolved (or refuse on ERROR)
  Ev->>Ev: build context
  loop wirers
    Ev->>Wirer: wire(context)
  end
  loop each policyRef
    Ev->>Repo: resolve(policyRef)
    Repo-->>Ev: Policy
    Ev->>Gate: decide(context, policy)
    Gate-->>Ev: IN_SCOPE | NOT_APPLICABLE + reason
  end
  Ev-->>Caller: ApplicabilityResult (per-policy decisions)
  Note over Ev,Caller: No PolicyEngine calls
```

---

## 4. Per-policy branches inside one evaluate

Shows continue-after-FAIL/ERROR and S1 gate/engineKind behaviour in one run.

```mermaid
sequenceDiagram
  autonumber
  participant Ev as Evaluator
  participant Repo as PolicyRepository
  participant Gate as Applicability
  participant Eng as PolicyEngine

  Note over Ev: context already built (resolve + wiring done)

  Ev->>Repo: resolve(ref A — ALWAYS_APPLY, CUSTOM)
  Repo-->>Ev: Policy A v3
  Ev->>Gate: decide(A)
  Gate-->>Ev: in scope
  Ev->>Eng: evaluate(A)
  Eng-->>Ev: FAIL + 0 findings
  Note over Ev: outcome A: FAIL @ v3 — continue

  Ev->>Repo: resolve(ref B — blank applicability, CUSTOM)
  Repo-->>Ev: Policy B v1
  Ev->>Gate: decide(B)
  Gate-->>Ev: in scope (blank ⇒ ALWAYS_APPLY)
  Ev->>Eng: evaluate(B)
  Eng-->>Ev: PASS
  Note over Ev: outcome B: PASS @ v1 — continue

  Ev->>Repo: resolve(ref C — unknown applicabilityKind)
  Repo-->>Ev: Policy C v2
  Ev->>Gate: decide(C)
  Gate-->>Ev: ERROR (kind not implemented)
  Note over Ev: outcome C: ERROR @ v2 — no engine — continue

  Ev->>Repo: resolve(ref D — engineKind=DROOLS, no adapter)
  Repo-->>Ev: Policy D v1
  Ev->>Gate: decide(D)
  Gate-->>Ev: in scope
  Ev->>Eng: lookup DROOLS
  Note over Ev: no adapter → outcome D: ERROR @ v1 — continue
```

---

## 5. Repository resolve during evaluate

How `latest` / pinned version / id affect the cited outcome version.

```mermaid
sequenceDiagram
  autonumber
  actor Caller
  participant Ev as Evaluator
  participant Repo as PolicyRepository

  Note over Caller,Repo: Policies already saved: name=mongo-gate versions 1 and 2

  Caller->>Ev: evaluate(frag, [name=mongo-gate, latest])
  Ev->>Repo: resolve(name=mongo-gate, latest)
  Repo-->>Ev: version=2 revision
  Note over Ev: outcome cites version=2

  Caller->>Ev: evaluate(frag, [name=mongo-gate, version=1])
  Ev->>Repo: resolve(name=mongo-gate, version=1)
  Repo-->>Ev: version=1 revision
  Note over Ev: outcome cites version=1

  Caller->>Ev: evaluate(frag, [id=…])
  Ev->>Repo: resolve(id)
  Repo-->>Ev: that revision
  Note over Ev: outcome cites that serial
```

See [`repository.md`](repository.md) · [`model.md`](model.md).

---

## 6. Missing / unresolvable policy ref

If a ref cannot be resolved, record a per-ref **ERROR** outcome (or refuse — **prefer ERROR + continue** so one bad ref does not discard other outcomes). Confirm exact shape in WI-002; do not abort the whole batch unless product locks otherwise.

```mermaid
sequenceDiagram
  autonumber
  participant Ev as Evaluator
  participant Repo as PolicyRepository

  Ev->>Repo: resolve(unknown-name, latest)
  Repo-->>Ev: not found
  Note over Ev: append ERROR outcome (ref identity in message) — continue remaining refs
```

---

## 7. Wrapper → fixed contract (later stories)

Suite / batch **must not** invent a second evaluate pipeline; they reduce to `(fragment, policyRefs)`.

```mermaid
sequenceDiagram
  autonumber
  actor App
  participant Suite as Suite wrapper (C-27)
  participant Batch as Batch wrapper (C-29)
  participant Ev as Evaluator (S1)

  App->>Suite: evaluateSuite(fragment, suiteId)
  Suite->>Suite: expand memberships → policyRefs
  Suite->>Ev: evaluate(fragment, policyRefs)
  Ev-->>Suite: EvaluationResult
  Suite->>Suite: folder roll-up (suite concern)
  Suite-->>App: SuiteEvaluationResult

  App->>Batch: evaluateBatch(subjects, target)
  loop each subject
    Batch->>Ev: evaluate(subject.fragment, refs)
    Ev-->>Batch: EvaluationResult
  end
  Batch-->>App: BatchEvaluationResult
```

---

## Implementation checklist (map to tests)

| Sequence § | Assert |
|------------|--------|
| §1 | Order: resolve → wire → per-policy gate → engine |
| §2 | Fragment ERROR → exception; zero outcomes |
| §3 | Preview never calls engine |
| §4 | FAIL/ERROR continue; unknown kind/engine → ERROR |
| §5 | Outcome version matches resolved revision |
| §6 | Unresolvable ref → ERROR (continue) |
| §7 | Wrappers only call fixed `evaluate` |

Scenarios: [`EXAMPLES.md`](../../workitems/completed/20260904-policy-evaluate-core/EXAMPLES.md).
