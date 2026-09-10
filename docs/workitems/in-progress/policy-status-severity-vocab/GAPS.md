# Gaps — policy-status-severity-vocab (C-34)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**Design lock (WI-001):** G-P50v–G-P56v are **resolved** below. API/engine code may proceed (WI-002+).

**Normative picture:** [`VOCAB-MATRIX.md`](VOCAB-MATRIX.md) (aligned to these locks).

Inherited locks (superseded where noted):

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P11f | Finding model | **resolved** (C-24); **superseded here** | Was INFO\|WARNING\|ERROR → closed `CRITICAL`\|`HIGH`\|`MEDIUM`\|`LOW`\|`INFO` (G-P51v) |
| G-P16 | ERROR vs FAIL (status) | **resolved** (C-24); **token renamed here** | Semantics keep (FAIL ≠ could-not-run); status token `ERROR` → `EXEC_ERROR` (G-P50v) |
| G-P12f | Findings vs status | **resolved** (C-24) | Findings optional on all statuses; soft validation — unchanged |
| G-P29s | Suite severity scale | **resolved** (C-27); **refined here** | Scale = closed set; matrices + bare `EXEC_ERROR`→`HIGH` are **BuiltinSuiteStrategy** rules (G-P55v / G-P56v), not hard-coded outside the pack |
| G-P49r | Persist filters | **resolved** (C-33); **superseded here** | Dual `ERROR` on both axes removed; new tokens + dual-read (G-P54v) |

---

## Design (this story)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P50v | Rename which axis? | **resolved** | **Hybrid:** (1) finding severity off `ERROR` → closed CRITICAL…INFO; (2) outcome status `ERROR` → **`EXEC_ERROR`** (execution failure). Axis A only for `EXEC_ERROR`. |
| G-P51v | Closed severity set | **resolved** | Kotlin `FindingSeverity` enum in `:objs-policy-api`: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO`. No soft aliases (`MODERATE`, `INFORMATIONAL`, `WARN`). Null allowed. |
| G-P52v | Align suite scale ↔ finding severity | **resolved** | One vocabulary. Axis C = **reported severity** (leaf/folder/`overallSeverity`); same tokens as B; **strategy-computed**, not authored. |
| G-P53v | Engine defaults (Drools / CUSTOM) | **resolved** | `pass` / `fail` / `error` helpers → finding severity **null**. Never emit `"ERROR"` / `"OK"` as severity. Authoring may keep `error()` / body `ERROR` → status `EXEC_ERROR`. |
| G-P54v | Persist filter compatibility | **resolved** | Dual-read one release; **no** Flyway rewrite. Status `ERROR`→`EXEC_ERROR`. Finding `ERROR`→`HIGH`, `WARNING`/`WARN`→`MEDIUM`, `OK`→null/no match. No `MODERATE`/`INFORMATIONAL` aliases. |
| G-P55v | Synthetic leaf severity | **resolved** | **BuiltinSuiteStrategy** only: bare `EXEC_ERROR` + no finding sevs → reported `HIGH`. No FAIL synthetic. Custom packs may differ. |
| G-P56v | SuiteStrategy pack | **resolved** | Composable `SuiteStrategy`: execution + leaf reported severity + folder roll-up + overall aggregate. Evaluator orchestrates only — **no** hard-coded aggregation outside the pack. Story matrices = Builtin. Field: `suiteStrategyKind` (legacy `rollUpStrategyKind` alias one release). |

---

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| PARK | Story parked | 2026-09-09 | Vocab collision reviewed; implement later under C-34 |
| G-P50v | Hybrid rename | 2026-09-10 | Status `EXEC_ERROR`; severity closed set without `ERROR` |
| G-P51v | Closed FindingSeverity | 2026-09-10 | CRITICAL…INFO; no soft aliases |
| G-P52v | Reported severity | 2026-09-10 | C = strategy output; tokens = B |
| G-P53v | Engine null sev | 2026-09-10 | Helpers emit null finding severity |
| G-P54v | Dual-read | 2026-09-10 | Legacy ERROR/WARNING/OK; no Flyway |
| G-P55v | Builtin synthetic | 2026-09-10 | Bare EXEC_ERROR → HIGH |
| G-P56v | SuiteStrategy pack | 2026-09-10 | Leaf + folder + overall (+ execution); no hard-coded agg |
