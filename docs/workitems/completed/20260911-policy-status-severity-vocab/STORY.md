# Story: policy-status-severity-vocab — disambiguate status vs finding severity

**Slug:** `policy-status-severity-vocab`  
**Branch:** `policy-status-severity-vocab`  
**Status:** completed  
**Closed:** 2026-09-11  
**Folder:** [`docs/workitems/completed/20260911-policy-status-severity-vocab/`](.)  
**Backlog:** [C-34](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Before:** [C-33 `policy-results-persistence`](../20260907-policy-results-persistence/STORY.md)  
**Independent of:** [C-29 `policy-batch`](../../planned/policy-batch/STORY.md), [C-30 `policy-consumer`](../../planned/policy-consumer/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md) (G-P50v–G-P56v **resolved**)  
**Normative picture:** [`VOCAB-MATRIX.md`](VOCAB-MATRIX.md) · **UI labels:** [`VOCAB-MATRIX.md` §10](VOCAB-MATRIX.md#10-ui-presentation-normative-for-wi-002--consumers) · **Business guide:** [`business-indicators.md`](../../../design/policy/business-indicators.md) · **Strategy implementers:** [`suite-strategy-implementers.md`](../../../design/policy/suite-strategy-implementers.md)  
**Design:** [`results.md`](../../../design/policy/results.md) · [`suites.md`](../../../design/policy/suites.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Align **vocabulary** and **suite compute** so outcome status and finding/reported severity do not share tokens, and aggregation is not hard-coded outside a suite strategy pack.

| Axis | Locked tokens / role |
|------|----------------------|
| **A. Outcome status** | `PASS` \| `FAIL` \| **`EXEC_ERROR`** \| `NOT_APPLICABLE` (was `ERROR`) |
| **B. Finding severity** | `CRITICAL` \| `HIGH` \| `MEDIUM` \| `LOW` \| `INFO` \| ∅ — closed enum, no soft aliases |
| **C. Reported severity** | Same tokens as B; **strategy-computed** (not authored) |

**Semantics stay (G-P16):** FAIL = not satisfied; EXEC_ERROR = could not run.

**UI:** display names and surfaces are locked in [`VOCAB-MATRIX.md` §10](VOCAB-MATRIX.md#10-ui-presentation-normative-for-wi-002--consumers) (Pass / Fail / Exec error / N/A vs Critical…Info; never paint status as severity).

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock: close GAPS G-P50v–G-P56v (`WI-001-design-lock.md`)
- [x] WI-002 — API + engines: `EXEC_ERROR`, `FindingSeverity`, engine defaults, UI tokens (`WI-002-api-engines.md`)
- [x] WI-005 — SuiteStrategy pack SPI: leaf C + folder roll-up + overall; no hard-coded agg (`WI-005-suite-strategy-pack.md`)
- [x] WI-003 — Archive filters / dual-read compat (`WI-003-persist-compat.md`)
- [x] WI-004 — Living docs (`results.md`, `suites.md`) (`WI-004-living-docs.md`)

## Out of scope

- Changing FAIL vs EXEC_ERROR **semantics** (token rename only)
- Policy batch / consumer product work (C-29 / C-30)
- Pure workbench chrome unless labels must follow renamed API tokens
- Regulatory / domain severity vocabularies outside policy results
- Shipping a second non-builtin `SuiteStrategy` implementation (SPI + BUILTIN only)
- Flyway rewrite of archived evaluation rows
