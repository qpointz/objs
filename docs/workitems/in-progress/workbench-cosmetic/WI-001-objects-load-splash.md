# WI-001 — Objects results loading splash

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Objects splash  
**Status:** done  
**Depends on:** WI-000  
**Examples:** **workbench**  
**Gap:** [`G-WC-objload`](GAPS.md) (resolved)

## Goal

Show an Explorer-style **loading splash** in the Objects results content area while search is in progress so slow connections do not leave a blank pane.

## Deliverables

- [x] `ObjectsPage` results stack (`data-tour="objects-results"`): `position: relative`, `minHeight: 280` for a centered overlay
- [x] While `searchBusy`: absolute overlay (`data-tour="objects-results-loading"`) with Mantine `Loader` + “Loading objects…”
- [x] Empty-context prompt and “No entities matched” only when `!searchBusy`
- [x] Re-search with prior rows: overlay on top of the table
- [x] Mark `G-WC-objload` resolved in `GAPS.md`; tracker `[x]` in `STORY.md`

## Out of scope

- Query / Composer / Add Objects loading overlays
- API or matcher behavior changes
- Extracting a shared `LoadingSplash` component (unless a later WI needs it)

## Acceptance

- [x] Throttled network + bound context: splash appears while `searchBusy`, then results or empty message
- [x] Empty context: open-graph prompt; no splash
- [x] Manual Search with existing rows: splash overlays table until replace
