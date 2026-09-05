# WI-006 — Correct G-P29s: suite engine vs folder roll-up inputs

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-005  

## Goal

Reopen and correct G-P29s: suite `rollUpStrategyKind` selects the roll-up **engine** (`BUILTIN`; future `DROOLS`); each folder supplies **`rollUpMode`** (`ALL_PASS` \| `ANY_PASS`) and `severityConfig` as **inputs** that engine interprets.

## Scope

- [x] GAPS / suites.md / STORY locked table
- [x] API: `SuiteFolder.rollUpMode`; suite kind default `BUILTIN`
- [x] Core: `BuiltinSuiteRollUpStrategy`; tests
- [x] HTTP DTOs + Suites UI (mode on folder; engine on suite)

## Out of scope

- Drools-backed roll-up implementation
- ExecutionStrategy / matcher / participation changes

## Acceptance

- [x] Suite no longer uses ALL_PASS/ANY_PASS as strategy kinds
- [x] Per-folder mode drives Builtin matrices
- [x] Existing suite evaluate tests green
