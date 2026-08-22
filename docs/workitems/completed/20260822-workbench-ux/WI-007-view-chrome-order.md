# WI-007 — View chrome order (Note 4)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 6 — View chrome  
**Status:** done  
**Depends on:** WI-006  
**Examples:** **workbench**  
**Source:** [`UX-NOTES/Note4/Note4.md`](UX-NOTES/Note4/Note4.md); gap **G-UX-vchrome**

## Goal

Reorder graph-bound top-level views so **context comes first**, then **view actions**. Drop redundant view titles and help icons.

Applies to **Explorer · Objects · Query · Composer**.

## Deliverables

- [x] Explorer / Objects / Query: `GraphContextBar` is the **first** row in the view
- [x] Composer: `CurrentGraphBar` is the **first** row (what is being edited)
- [x] Remove view **Title** + help/doc **Popover** on those four views
- [x] View-level action buttons sit on a **row below** context (Explorer Composer/layout; Query Exec + New graph; Composer Reset/Clear/Validate/Save/…)
- [x] View-level buttons use one shared size (`sm` via `VIEW_ACTION_BUTTON_SIZE`) across the four views
- [x] Tour steps updated for new chrome order / selectors

## Out of scope

- Schema view
- Second-level buttons (Matcher Search, Shelf actions, Options pane, type pills, Visual/Text tabs)
- Living docs / full `ui.md` sweep (WI-005)
