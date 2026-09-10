# WI-009 — Composer Visual L2 toolbar (Note 8)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** Intake  
**Status:** done  
**Depends on:** WI-008  
**Examples:** **workbench**  
**Notes:** Intake Note removed at story closure
**Gaps:** [`GAPS.md`](GAPS.md) G-WU2-N8

## Goal

Composer Visual row: move draft actions to the **right**, shrink one step and match view-action colors; remove **N on canvas**; place **Changes only** immediately left of those actions.

## Deliverables

- [x] Right-align New / Link / Add objects / Remove / Delete / Annotations
- [x] `compact-xs` + `VIEW_ACTION_VARIANT` (New remains filled primary)
- [x] Remove “N on canvas” badge
- [x] Changes only left of the action cluster
- [x] `ui.md` + GAPS / STORY

## Acceptance

- [x] Visual L2 no longer shows on-canvas count badge
- [x] Draft actions sit on the right; Changes only sits just left of them
- [x] Secondary actions use bordered default (not `light`)
