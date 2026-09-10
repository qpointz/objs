# WI-008 — Policy Data column filters + title chrome (Note 7)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** Intake  
**Status:** done  
**Depends on:** WI-007  
**Examples:** **workbench**  
**Notes:** Intake Note removed at story closure
**Gaps:** [`GAPS.md`](GAPS.md) G-WU2-N7

## Goal

Note 7: move Policy Data severity filter into the **Severity** column header (funnel menu); add the same pattern for **Type**; drop redundant suite help text and the Policy result pill; right-align exec stats next to view actions.

## Deliverables

- [x] Data tab: remove top Severity MultiSelect; funnel on Severity + Type headers (`ColumnFilterHeader`)
- [x] Type/Severity filters shared with Visual toolbar state where applicable
- [x] Suites: remove suite roll-up instructional label
- [x] Policy: remove overall PASS/FAIL badge; stats left of action buttons with spacer
- [x] Query: same stats alignment (spacer + right cluster)
- [x] Tour / `workbench.md` / GAPS

## Acceptance

- [x] Data Severity/Type funnels open checklist menus; no standalone Severity MultiSelect above the grid
- [x] Policy title row has no result pill; stats sit immediately left of Export/Add/…
- [x] Suite editor no longer shows the Folder roll-up strategy blurb
