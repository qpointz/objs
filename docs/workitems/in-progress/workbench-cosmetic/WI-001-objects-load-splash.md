# WI-001 — Objects results loading splash

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Objects splash  
**Status:** planned  
**Depends on:** WI-000  
**Examples:** **workbench**  
**Gap:** [`G-WC-objload`](GAPS.md)

## Goal

Show an Explorer-style **loading splash** in the Objects results content area while search is in progress so slow connections do not leave a blank pane.

## Deliverables

- [ ] `ObjectsPage` results stack (`data-tour="objects-results"`): `position: relative`, enough min-height for a centered overlay
- [ ] While `searchBusy`: absolute overlay with Mantine `Loader` + “Loading objects…” (match Explorer body-wash style)
- [ ] Empty-context prompt and “No entities matched” only when `!searchBusy`
- [ ] Re-search with prior rows: overlay on top of the table
- [ ] Mark `G-WC-objload` resolved in `GAPS.md`; tracker `[x]` in `STORY.md`

## Out of scope

- Query / Composer / Add Objects loading overlays
- API or matcher behavior changes
- Extracting a shared `LoadingSplash` component (unless a later WI needs it)

## Acceptance

- [ ] Throttled network + bound context: splash appears immediately on open, then results or empty message
- [ ] Empty context: open-graph prompt; no splash
- [ ] Manual Search with existing rows: splash overlays table until replace
