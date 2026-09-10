# WI-003 — Single-row view chrome (Note 3)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** Intake  
**Status:** done  
**Depends on:** WI-002  
**Examples:** **workbench**  
**Notes:** Intake Note removed at story closure
**Gaps:** [`GAPS.md`](GAPS.md) G-WU2-N3

## Goal

Align chrome: shared graph context on the AppShell header with L0 nav; page title row with quiet title + right-aligned view actions; Schema has no context bar; Composer New as view-level split.

## Deliverables

- [x] Shared `GraphContextBar` on AppShell header (Explorer / Objects / Query / Policy)
- [x] Page row: quiet title left + view actions **right-aligned**
- [x] Schema: remove `SchemaContextBar` entirely
- [x] Composer: New ▾ on view actions; `ComposerGraphBar` stays on page (local draft)
- [x] Tour + `ui.md`

## Acceptance

- [x] Graph context sits on the header row with main navigation
- [x] View actions are right-aligned on the page title row
- [x] Schema has no context component
- [x] Composer New is a view-level split button
