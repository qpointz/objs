# WI-006 — Explorer action chrome (Note 3)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Explorer actions  
**Status:** done  
**Depends on:** WI-004  
**Examples:** **workbench**  
**Source:** [`UX-NOTES/Note3/Note 3.md`](UX-NOTES/Note3/Note%203.md); gap **G-UX-eact**

## Goal

Slim Explorer chrome now that graph context is shared:

1. **Delete** the duplicate **nodes / edges** count on the type-highlight row (already on the graph-context bar).
2. **Delete** **Open in Query** (Query uses the same graph context — handoff button is redundant).
3. **Move** **Open in Composer** / **New graph from selection** and **Apply layout** (with direction menu) to the **same row as the Explorer title** (Note 3 Pic **(5)** — right side of the title row).

## Deliverables

- [x] Type row shows type pills only (no `N nodes / M edges` text)
- [x] Open in Query removed; tour/tests updated
- [x] Composer / New-graph-from-selection + Apply layout on Explorer title row
- [x] Selection-mode New graph from selection still available in the same slot

## Out of scope

- Objects / Query chrome beyond shared context
- Living docs / full tour sweep (WI-005)
