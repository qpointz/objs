# WI-002 — Design lock: ValidationIssue

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-000  
**Gaps:** G-V1…G-V7 (**resolved** in [`GAPS.md`](GAPS.md))  

## Goal

Close every ValidationIssue design gap. Lock the addressable `ValidationSubject` / `ValidationSchemaRef` shape (redundancy allowed; no full Entity/Edge embed). Kotlin API types land in WI-003.

## Deliverables

- [x] All G-V* rows resolved (G-V1…G-V7)
- [x] Decision log dated
- [x] Defaults locked: JSON Pointer `fieldPath`; multi-field IDENTITY → one issue / `fieldPath` null; `index` on set items; seeds null OK v1
- [x] WI-003/004 may proceed against these locks

## Out of scope

- Implementation in `:objs-api` / `:objs-persistence` / workbench (WI-003+)
