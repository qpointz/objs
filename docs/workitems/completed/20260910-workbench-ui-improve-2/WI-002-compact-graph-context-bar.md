# WI-002 — Compact graph-context chrome (Note 2)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** Intake  
**Status:** done  
**Depends on:** WI-001  
**Examples:** **workbench**  
**Notes:** Intake Note removed at story closure
**Gaps:** [`GAPS.md`](GAPS.md) G-WU2-N2

## Goal

Compact and right-align shared graph-context chrome (and matching Composer bar): free space between title and bar for future nav, `N/E: n/e` stats, annotation pills replaced by `Annotations: X` + hover list.

## Deliverables

- [x] Title row: title left, flex spacer, content-sized bar right (Explorer / Objects / Query / Policy / Composer / Schema)
- [x] `GraphContextBar`: no annotation pills; `Annotations: {count}` + hover; `N/E: …`; no stretch
- [x] `ComposerGraphBar`: same optics
- [x] `SchemaContextBar`: same layout optics; `T/E:`; Tags/Attributes counts + hover (no pills)
- [x] Tour + [`docs/design/ui.md`](../../../design/ui.md) if chrome copy changes

## Acceptance

- [x] Context bar is compact and right-aligned on all graph-context hosts **and Schema**
- [x] Annotation pills gone from bar; hover shows k=v list
- [x] Stats show `N/E: n/e` (Schema catalog `T/E:`)
- [x] Composer + Schema bars match
