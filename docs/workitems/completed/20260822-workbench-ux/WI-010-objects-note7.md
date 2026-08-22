# WI-010 — Objects view (Note 7)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 10 — Objects Note 7  
**Status:** done  
**Depends on:** WI-009  
**Examples:** **workbench**  
**Source:** [`UX-NOTES/Note7/Note7.md`](UX-NOTES/Note7/Note7.md)  
**Gaps:** `G-UX-objgrid`, `G-UX-objchrome`, `G-UX-objview`, `G-UX-objshelf` (**resolved**)

## Goal

Ship Note 7 Objects UX: Query Data-style grid, stats/actions chrome row, dual-purpose right pane (object viewer or Shelf/Matcher).

## Deliverables

- [x] Objects grid per **G-UX-objgrid** (`QueryResultGrid` chrome, Id link, page 25)
- [x] Chrome row per **G-UX-objchrome** (stats left; shelf actions right)
- [x] Object viewer in right pane per **G-UX-objview** (replaces tabs on select; close restores)
- [x] Shelf/Matcher per **G-UX-objshelf** (Shelf \| Matcher; bold + count; actions on chrome row)
- [x] Tour steps updated for Objects

## Out of scope

- WI-005 full living-docs sweep (`ui.md` remains WI-005)
