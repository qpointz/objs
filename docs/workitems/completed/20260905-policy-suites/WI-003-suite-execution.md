# WI-003 — Suite execution + roll-up tests

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-002  

Thin HTTP for suite CRUD / `evaluateSuite` / selection preview may land here or in WI-004; must not invent a second evaluate pipeline.

## Goal

Implement `evaluateSuite` **wrapper** over flat `evaluate`: expand effective policies (matchers + scope), run via **`ExecutionStrategy`**, map onto suite tree / **`evaluationId`** result, apply **`SuiteRollUpStrategy`**. CUSTOM-engine tests.

## Scope

- [x] Scope: full \| subfolder \| set of subfolders \| policy subset
- [x] `ExecutionStrategy` — dedupe vs non-dedupe; call flat `evaluate`
- [x] Built-in roll-up ALL_PASS / ANY_PASS; ENABLED / DISABLED / IGNORED; N/A ≡ no leaf/vote
- [x] Severity in strategy; severityConfig; PASS+sev warning; synthetic ERROR→HIGH
- [x] Result: `evaluationId` + meta (tags/annotations) + tree (refs→outcomes) + outcomes; **no** input persist
- [x] Unit tests (CUSTOM engine)
- [x] Thin HTTP on `:objs-policy-service` for suite CRUD / `evaluateSuite` / selection preview

## Out of scope

- Drools-backed roll-up strategy (future note only)
- UI / batch (C-29)
- Input persist / frozen fragment graph

## Acceptance

- [x] Suite run returns per-leaf and per-folder status (and severity)
- [x] Existing evaluate/Drools tests remain green
