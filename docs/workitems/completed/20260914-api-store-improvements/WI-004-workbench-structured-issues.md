# WI-004 — Workbench structured validation issues

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-003  

## Goal

Surface structured `ValidationIssue` in TS. Prefer `subject` for targeting; path fallback for legacy/seed.

## Deliverables

- [x] `BoMValidationIssue` (+ subject / schema types) updated
- [x] Targeting helpers prefer structured fields; path-regex fallback retained
- [x] Object Linter focus still uses helpers (unchanged call sites)
- [x] UI unit tests updated

## Out of scope

- Changing linter layout / chrome beyond targeting
- Product tour (hooks unchanged)
