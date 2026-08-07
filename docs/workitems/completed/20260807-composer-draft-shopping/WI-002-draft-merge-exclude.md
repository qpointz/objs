# WI-002 — Draft merge / exclude

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Draft model  
**Status:** done  
**Depends on:** WI-000  
**Gaps:** G-S7, G-S8, G-S10, G-S16

## Goal

Support **merge** of store entities/edges into the draft without replace, and **exclude** items from the draft without Apply deletes.

## Scope

- `graphDraft.ts` / `useGraphDraft.ts`: `mergeEntitiesIntoDraft`, `mergeEdgesIntoDraft`, `excludeEntityFromDraft`, `excludeEdgeFromDraft`
- Id conflict: **keep draft** (G-S7)
- Exclude: drop from document/baseline; **no** `pendingDelete*`; **no** soft-deleted “deleted” chrome (G-S8, G-S16)
- Exclude entity: cascade-exclude incident draft edges (unless already pending-delete)
- Merge store entities/edges into baseline maps/ids for unchanged detection (store-backed = in `baselineEntityIds`, G-S10)
- Canvas / selection: **Remove from draft** vs existing **Delete**
- Unit tests: merge skip, exclude cascade, Delete path unchanged

## Out of scope

- Add objects modal UI
- `obj-expr` / `ids` matchers

## Acceptance

- [x] Merge appends; existing ids unchanged
- [x] Exclude removes from document/baseline without pending deletes or deleted chrome
- [x] Delete path still marks baseline ids for Apply
