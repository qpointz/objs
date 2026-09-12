# Gaps — policy-batch (C-29)

All batch gaps **locked** (2026-09-12). WI-001 records these formally and promotes living docs.

## Batch gaps

| # | Topic | Status | Lock |
|---|--------|--------|------|
| G-P41b | Batch / result-pack API | **locked** | Sync `evaluateBatch(subjects, target) → BatchEvaluationResult`. Subject = opaque `subjectKey` + `GraphFragment`. Target = suite (`PolicySuite` + `SuiteEvaluateScope`) **or** `List<PolicyRef>`. No hard max subjects in v1. **Continue** on per-subject failure. Caller builds the subject list (no FragmentsFactory SPI). |
| G-P42b | Pack diagnostics | **locked** | Subject ERROR cell when that subject’s run fails (message retained); other subjects continue. Per-policy outcomes stay inside normal evaluate/suite results. Optional pack-level `diagnostics: List<String>` only — no cross-subject roll-up. |
| G-P43b | Batch parallelism / execution | **locked** | Pluggable **`PolicyBatchExecutor`** owns **plan + execute**. C-29 ships **`SequentialPolicyBatchExecutor`** only. Parallel deferred. |

## G-P42b (locked detail)

```text
BatchEvaluationResult
  diagnostics?          ← optional pack-level notes
  cells[]               ← one per subject, input order
    subjectKey
    ok / error          ← subject-level: did evaluate complete?
    result?             ← EvaluationResult or SuiteEvaluationResult when ok
         outcomes[]     ← existing PASS/FAIL/EXEC_ERROR/… (unchanged)
```

| Layer | Behavior |
|-------|----------|
| Inside evaluate/suite | Unchanged — missing refs / engine errors are normal outcomes |
| Subject cell failed | Catch around that subject; ERROR cell + message; pack continues |
| Pack `diagnostics` | Optional batch-wide strings only; not scores or matrix aggregates |

## G-P41b (locked detail)

```text
evaluateBatch(
  subjects = [ { subjectKey, fragment }, ... ],
  target   = suite-or-policy-refs
) → BatchEvaluationResult
```

Caller-facing port: **`PolicyBatchEvaluator`**. Plan+run strategy: **`PolicyBatchExecutor`**.

## G-P43b (locked detail)

**`PolicyBatchExecutor`** — plan (resolve target → work items) + execute.  
**`SequentialPolicyBatchExecutor`** — only C-29 impl (sequential, continue-on-error).

```text
PolicyBatchEvaluator.evaluateBatch(...)
        │
        ▼
PolicyBatchExecutor.execute(subjects, target)
        │
        └── SequentialPolicyBatchExecutor   ← C-29
```

## Philosophy (inherited — already locked)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P36b | Thin batch pack | **resolved** | Opaque subjectKey; no matrix in foundation |
| G-P43 | Matrix is product | **resolved** | Apps own axes/layout |

## Related non-goals

| Topic | Notes |
|-------|-------|
| Auto-persist | Caller uses C-33 `EvaluationArchive` |
| FragmentsFactory SPI | Product builds `List<BatchSubject>` |
| REST | C-30 |
| Parallel executor | Deferred after C-29 |

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| G-P41b | API shape | 2026-09-12 | Sync evaluateBatch(subjects, target); opaque subjectKey; suite or refs; continue-on-error; no hard max |
| G-P43b | Executor | 2026-09-12 | `PolicyBatchExecutor` plans+executes; `SequentialPolicyBatchExecutor` is the only C-29 impl |
| G-P42b | Pack diagnostics | 2026-09-12 | Subject ERROR cells + optional pack diagnostics; no cross-subject roll-up; policy outcomes unchanged |
