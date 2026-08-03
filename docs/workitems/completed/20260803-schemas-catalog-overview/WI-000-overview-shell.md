# WI-000 — Overview shell as Schemas landing

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 0 — Shell  
**Status:** done  
**Depends on:** —

## Goal

Make `/schemas` (no type selected) a first-class Schemas **overview** landing instead of an empty
detail pane, and provide a clear way back from type detail to that overview.

## Scope

- Replace empty “Select a schema type…” state with an overview shell (toolbar placeholder + canvas area)
- Keep left type list; clicking a type still opens detail
- From type detail, add **Full schema** (or equivalent) that navigates to `/schemas`
- Do not change per-type editor behaviour yet

## Out of scope

- Ontology graph content (WI-001)
- Import/export (WI-002)

## Acceptance

- [x] Opening Schemas without a type shows the overview shell
- [x] Type list / create flows still open detail routes
- [x] Detail has a control to return to `/schemas`
