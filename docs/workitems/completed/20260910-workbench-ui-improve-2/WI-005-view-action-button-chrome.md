# WI-005 — View action button chrome (Note 5)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** Intake  
**Status:** done  
**Depends on:** WI-004  
**Examples:** **workbench**  
**Notes:** Intake Note removed at story closure
**Gaps:** [`GAPS.md`](GAPS.md) G-WU2-N5

## Goal

1. Query: replace gear Options with **Exec** split button (main = Exec; menu = Options).  
2. Unify top-level view action button styles across Explorer / Objects / Query / Policy / Composer / Schema; size one step smaller.

## Deliverables

- [x] Query Exec split + Options as menu item (Options modal for timeout)
- [x] Shared view-action size `xs`; secondary `VIEW_ACTION_VARIANT` (`default`); primary remain filled
- [x] Tour + `ui.md`

## Acceptance

- [x] No standalone settings gear on Query title row
- [x] View action rows use one secondary style and one primary style consistently
- [x] Buttons are one size step smaller than pre-Note-5 `sm`
