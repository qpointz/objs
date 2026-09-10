# Story: policy-status-severity-vocab — disambiguate status vs finding severity

**Slug:** `policy-status-severity-vocab`  
**Branch:** (not started — **planned** only; pick up later)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/policy-status-severity-vocab/`](.)  
**Backlog:** [C-34](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Before:** [C-33 `policy-results-persistence`](../../completed/20260907-policy-results-persistence/STORY.md) (status + severity filters locked as-is)  
**Independent of:** [C-29 `policy-batch`](../policy-batch/STORY.md), [C-30 `policy-consumer`](../policy-consumer/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`results.md`](../../../design/policy/results.md) · [`suites.md`](../../../design/policy/suites.md) (severity roll-up) · G-P11f / G-P16  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Review and align **vocabulary** across the policy results API so the same token is not used for two different axes:

| Axis                        | Today                                                                           | Intended meaning                                                          |
| --------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------- |
| **Outcome status**          | `PolicyOutcomeStatus.ERROR`                                                     | Policy could **not** execute correctly (engine/body/unknown-kind/runtime) |
| **Finding severity**        | `Finding.severity = "ERROR"` (G-P11f: INFO \| WARNING \| ERROR)                 | How serious a finding message is (often under **FAIL**)                   |
| **Suite reported severity** | Draft CRITICAL > HIGH > MEDIUM > LOW > INFO; synthetic outcome ERROR → **HIGH** | Roll-up “how bad” — already a third scale                                 |

**Semantics stay:** FAIL = not satisfied; ERROR (status) = could not run. This story fixes **names, types, and engine defaults** so callers and filters do not confuse the two.

## Why now (parked)

Observed in API / implementation (not a UI-only concern):

- Persist filters use `ERROR` on **both** `outcomeStatuses` and `findingSeverities`.
- `Finding.severity` is free `String?`; suite `SeverityRank` only ranks CRITICAL…INFO — finding `ERROR` / `WARNING` / `OK` rank **0**.
- Drools `scratch.error()` sets status **ERROR** and finding severity `"ERROR"`; `fail()` uses null severity; `pass()` emits `"OK"` (outside G-P11f).
- Leaf severity: bare status ERROR → synthetic `"HIGH"`; Drools `error()` finding often overrides with `"ERROR"`.

## Work Items

- [ ] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [ ] WI-001 — Design lock: rename vs dual enums; close GAPS
- [ ] WI-002 — API + engines + `SeverityRank` / leaf severity alignment
- [ ] WI-003 — Archive filters / compatibility (persist + any migration notes)
- [ ] WI-004 — Living docs (`results.md`, `suites.md`, GAPS decision log)

## Out of scope

- Changing FAIL vs ERROR **semantics** (G-P16 stays)
- Policy batch / consumer product work (C-29 / C-30)
- Pure workbench chrome unless labels must follow a renamed API token
- Regulatory / domain severity vocabularies outside policy results
