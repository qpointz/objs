# WI-006 — Validate and Apply UX

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 6 — Apply UX  
**Status:** done  
**Depends on:** WI-001, WI-005

## Goal

Wire Object linter **Validate** and **Apply** to the graph mutation API so upserts and pending deletes are dry-run and persisted transactionally.

## Scope

- `api.ts`: `validateGraphMutation` / `putGraphMutation`
- Validate sends current draft + pending delete ids
- Apply sends the same mutation; on success clear pending deletes and refresh baseline
- Dirty / pending-delete UX in the toolbar

## Out of scope

- Two-step PUT then DELETE
- Seed export from this page

## Acceptance

- [x] Validate covers upserts and pending deletes without writing
- [x] Apply persists upserts and deletes in one call
- [x] Successful Apply clears pending deletes and updates baseline
