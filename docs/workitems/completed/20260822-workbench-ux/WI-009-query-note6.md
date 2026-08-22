# WI-009 — Query view (Note 6)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 9 — Query Note 6  
**Status:** done  
**Depends on:** WI-008  
**Examples:** **workbench**  
**Source:** [`UX-NOTES/Note6/Note 6.md`](UX-NOTES/Note6/Note%206.md)  
**Gaps:** `G-UX-qchrome`, `G-UX-qstruct`, `G-UX-qview`, `G-UX-qcreate` (**resolved**)

## Goal

Ship Note 6 Query UX: chrome row + Options popover (drop right pane), Structured grids (table-alike / V+E), in-tab object viewer, Open in Composer from `result.contents`.

## Deliverables

- [x] Query chrome per **G-UX-qchrome**
- [x] Structured grids per **G-UX-qstruct** (virtualize >200)
- [x] Object viewer inside Graph/Structured tabs per **G-UX-qview**
- [x] Open in Composer per **G-UX-qcreate**
- [x] Tour steps updated for Query
- [x] Unit tests for structured mode helpers / result-tab resolve as needed

## Out of scope

- Slim ObjectViewer variant (reuse Note 5 first)
- WI-005 full living-docs sweep (tour for Query only here; full `ui.md` remains WI-005)
