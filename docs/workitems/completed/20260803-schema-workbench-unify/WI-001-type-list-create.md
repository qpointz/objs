# WI-001 — Type list + create menu

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-000

## Goal

Replace Entity / Edge props / All filtering with a flat type list, Object/Edge pills, denser
narrow column, click-opens-latest-version, and Create New Object / Create New Edge.

## Scope

- Flat `listSchemas()` (no usage filter control)
- Pill: Object if `ENTITY` in usages else Edge
- Narrower sidebar (~240–260px), smaller type labels
- Click type → latest version route
- Create menu → object draft (`ENTITY`) or edge draft (`EDGE_PROPERTIES`)
- Remove version badges from the left list (versions move to toolbar in WI-002)

## Acceptance

- [x] No Entity/Edge props/All segmented control
- [x] Types show O/E indicator; click opens latest version
- [x] Create New Object / Create New Edge available from the list header
