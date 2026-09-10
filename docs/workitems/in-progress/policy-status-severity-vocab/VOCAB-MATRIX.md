# Vocabulary matrix — outcomes, findings, suite severity (C-34)

**Story:** [`STORY.md`](STORY.md) · **Gaps:** [`GAPS.md`](GAPS.md)  
**Status:** **normative for C-34** (G-P50v–G-P56v locked in WI-001)  
**Living targets after WI-004:** [`results.md`](../../../design/policy/results.md), [`suites.md`](../../../design/policy/suites.md)

Cross-product of outcome status × finding severity × reported severity: today vs **locked future**.

---

## 1. Three axes (must stay distinct)

| Axis | Field(s) | Meaning | Who sets / computes |
|------|----------|---------|---------------------|
| **A. Outcome status** | `PolicyOutcome.status` / suite node `status` | Did the policy **run** and was it **satisfied**? | Gate + engine; folder status via **SuiteStrategy.rollUp** |
| **B. Finding severity** | `Finding.severity` | How serious is **this message**? | Engine / author |
| **C. Reported severity** | Suite leaf / folder / `meta.overallSeverity` | Roll-up “how bad” | **SuiteStrategy** (leaf + folder) — not authored |

**Invariant:** FAIL ≠ EXEC_ERROR on axis A (G-P16). Findings never become a second status channel (G-P12f).

```text
                    Axis A (status)                    Axis B (finding)              Axis C (reported)
PolicyOutcome ──► PASS|FAIL|EXEC_ERROR|N/A   +   Finding.severity?   ──strategy──► leaf reported sev
Suite folder   ──► same status enum          +   (no findings)       ◄──strategy── child reported sevs
```

---

## 2. Closed sets — today vs locked future

### 2.1 Axis A — outcome status

| Token | Today | Future | Meaning |
|-------|:----:|:------:|---------|
| `PASS` | ✓ | ✓ | In scope; satisfied |
| `FAIL` | ✓ | ✓ | In scope; not satisfied |
| `ERROR` | ✓ | → **`EXEC_ERROR`** | Could not execute correctly |
| `EXEC_ERROR` | — | ✓ | Renamed execution-failure status |
| `NOT_APPLICABLE` | ✓ | ✓ | Gate out of scope (flat; suite drops N/A leaves) |

### 2.2 Axis B — finding severity (G-P51v)

| Token | Today | Future |
|-------|-------|--------|
| `CRITICAL` … `INFO` | free-form / partial | **canonical closed set only** |
| `MODERATE` / `INFORMATIONAL` | soft aliases in SeverityRank | **removed** — not accepted |
| `WARNING` / `WARN` / `ERROR` / `OK` | used | **retired** as severity writes; dual-read only (G-P54v) |
| `null` | common | ✓ allowed |

```text
CRITICAL > HIGH > MEDIUM > LOW > INFO > ∅
```

### 2.3 Axis C — reported severity (G-P52v)

Same closed set as B. Computed by **SuiteStrategy** (G-P56v). Builtin bare `EXEC_ERROR` → `HIGH` (G-P55v).

---

## 3. Collision map (why C-34 exists)

| Token | Problem today | Locked fix |
|-------|---------------|------------|
| `ERROR` | Status **and** finding severity | Status → `EXEC_ERROR`; not a severity token |
| `WARNING` / `OK` | Outside suite scale / rank 0 | Retire writes; dual-read map |
| `FAIL` as severity | SBOM remaps | Reject as severity |

---

## 4. Engine defaults (G-P53v)

| API | Future status (A) | Future finding sev (B) |
|-----|-------------------|------------------------|
| `pass(msg)` | unchanged | **null** |
| `fail(msg)` | `FAIL` | **null** |
| `error(msg)` | **`EXEC_ERROR`** | **null** |
| `finding(…, sev)` | unchanged | closed-set token only |

CUSTOM body `ERROR` / Drools `error()` → status `EXEC_ERROR` (authoring sugar).

---

## 5. Leaf reported severity — BuiltinSuiteStrategy (G-P55v)

```text
leafReported(outcome):
  sevs = non-null finding severities (closed set)
  if sevs.nonEmpty → maxBy rank
  else if status == EXEC_ERROR → HIGH
  else → ∅
```

Custom `SuiteStrategy` packs may differ. Not hard-coded in the evaluator outside the pack (G-P56v).

---

## 6. Folder roll-up — BuiltinSuiteStrategy only

Status matrices (ALL_PASS / ANY_PASS): escalate on any child **`EXEC_ERROR`** (same shape as former ERROR). Severity: max of voting children’s reported C; `severityConfig` when parent non-passing. See living [`suites.md`](../../../design/policy/suites.md) after WI-004.

---

## 7. Overall aggregate — SuiteStrategy.aggregateOverall

```text
EXEC_ERROR > FAIL > PASS > NOT_APPLICABLE
```

Flat evaluate uses the same builtin overall function (no reported C).

---

## 8. Persist filters (G-P54v)

| Filter | Future |
|--------|--------|
| `outcomeStatuses` | PASS/FAIL/**EXEC_ERROR**/N/A; dual-read `ERROR`→`EXEC_ERROR` |
| `findingSeverities` | CRITICAL…INFO + `UNSPECIFIED`; dual-read ERROR→HIGH, WARNING→MEDIUM, OK→null |

---

## 9. SuiteStrategy pack (G-P56v)

```text
SuiteStrategy (suiteStrategyKind)
  ├── ExecutionStrategy
  ├── leafReportedSeverity(outcome) → FindingSeverity?
  ├── rollUp(folder, votingChildren) → { status, severity }
  └── aggregateOverall(outcomes) → PolicyOutcomeStatus
```

`BUILTIN` = matrices + synthetic HIGH + overall precedence above. No aggregation hard-coded outside the pack.

---

## 10. UI presentation (normative for WI-002 / consumers)

API token → display label → surface. Status chrome and severity chrome stay distinct (G-P52v). Exact palette hex is out of scope.

### 10.1 Axis A — outcome status labels

Status pill / suite status column only:

| API token | UI label | Short / filter |
|-----------|----------|----------------|
| `PASS` | Pass | Pass |
| `FAIL` | Fail | Fail |
| `EXEC_ERROR` | Exec error | Exec error |
| `NOT_APPLICABLE` | Not applicable | N/A |

### 10.2 Axis B/C — severity labels

Finding pill, suite severity column, Data severity funnel, SBOM assessment pill:

| API token | UI label | Filter chip |
|-----------|----------|-------------|
| `CRITICAL` | Critical | Critical |
| `HIGH` | High | High |
| `MEDIUM` | Medium | Medium |
| `LOW` | Low | Low |
| `INFO` | Info | Info |
| `null` / omit | *(no pill)* | None |

Retired tokens (`ERROR`, `WARNING`, `OK`, …) are **not** shown as labels on new results; dual-read archives map before display (G-P54v).

### 10.3 Surface matrix

| UI surface | Shows | Axis | Notes |
|------------|-------|------|-------|
| Outcome / suite **status** pill | Pass / Fail / Exec error / N/A | A | Never use severity colors for status alone |
| Finding row **severity** pill | Critical…Info or hidden | B | Independent of parent outcome status |
| Suite tree **severity** column | reported severity label or empty | C | From strategy; bare Exec error leaf → High under Builtin |
| Graph / Data **severity** funnel | max finding sev on node/edge | B | Options = closed set + None; drop ERROR/WARN/OK ladder |
| SBOM assessment pills | same closed set | B | Stop mapping FAIL/ERROR status into severity `ERROR` |

### 10.4 Forbidden (anti-patterns)

- Synthesizing finding severity from status (`PASS`→`OK`, `FAIL`→`ERROR`).
- Using label `ERROR` for either status or severity after rename.
- Soft-alias display names (`Warn`, `OK`, `Moderate`).
