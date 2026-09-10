# Gaps — policy-status-severity-vocab (C-34)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**WI-001 must close every `open` row** before API/engine code (WI-002+).

Inherited locks (do not reopen unless this story explicitly supersedes):

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P11f | Finding model | **resolved** (C-24) | severity INFO\|WARNING\|ERROR today — **candidate to change here** |
| G-P16 | ERROR vs FAIL (status) | **resolved** (C-24) | FAIL = not satisfied; ERROR = engine/body/unknown-kind — **semantics keep** |
| G-P12f | Findings vs status | **resolved** (C-24) | Findings optional on all statuses; soft validation |
| G-P29s | Suite severity scale | **resolved** (C-27) | Draft CRITICAL>HIGH>MEDIUM>LOW>INFO; ERROR→HIGH synthetic |
| G-P49r | Persist filters | **resolved** (C-33) | outcomeStatuses + findingSeverities both may include token ERROR |

---

## Open (design)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P50v | Rename which axis? | **open** | Prefer: (A) rename finding severity away from ERROR (align CRITICAL/HIGH/…), (B) rename status ERROR → EXECUTION_ERROR / UNABLE, (C) keep both tokens but typed enums + docs only. Pick one primary. |
| G-P51v | Closed severity set | **open** | Enum vs string constants in `:objs-policy-api`; map once in `SeverityRank`; drop free-form or keep soft extras? |
| G-P52v | Align suite scale ↔ finding severity | **open** | One vocabulary for leaf + folder reported severity vs keep G-P11f INFO/WARNING/ERROR under outcomes |
| G-P53v | Engine defaults (Drools / CUSTOM) | **open** | `error()` must not reuse finding severity token for status; `fail()` default severity; retire `"OK"` or map to INFO/null |
| G-P54v | Persist filter compatibility | **open** | Rename filters / accept aliases / migrate archived JSON; document breaking vs dual-read |
| G-P55v | Synthetic leaf severity | **open** | Keep status ERROR → HIGH after rename; ensure finding-bearing ERROR outcomes don’t collapse to rank-0 tokens |

---

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| PARK | Story parked | 2026-09-09 | Vocab collision reviewed; implement later under C-34 |
| — | — | — | — |
