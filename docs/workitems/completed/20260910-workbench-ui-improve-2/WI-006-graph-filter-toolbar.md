# WI-006 — Graph filter toolbar (Note 6)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** Intake  
**Status:** done  
**Depends on:** WI-005  
**Examples:** **workbench**  
**Notes:** Intake Note removed at story closure
**Gaps:** [`GAPS.md`](GAPS.md) G-WU2-N6

## Goal

Replace wide type-pill rows with a hover-reveal **Filter toolbar** (Apply-layout optics): Types menu, Edges-by-role menu, Reset; Policy/Suites also expose Severity.

## Deliverables

- [x] `GraphFilterToolbar` top-left on Visual canvases
- [x] Types + Edges + Reset on Explorer, Query, Composer, Policy Visual
- [x] Severity filter on Policy/Suites Visual (context-specific)
- [x] Remove Explorer/Composer type pill rows; remove Policy title-row severity pills
- [x] Tour + `ui.md`

## Acceptance

- [x] Type pills no longer occupy a full title/sub row on Explorer / Composer
- [x] Filters dim canvas (do not remove geometry); Reset clears all toolbar filters
- [x] Policy Visual can filter Types, Edges, and Severity from the same toolbar
