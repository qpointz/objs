# Story: Objects view + shelf

**Slug:** `objects-shelf`  
**Branch:** `objects-shelf` (track `origin/objects-shelf`)  
**Status:** closed  
**Archive:** [`docs/workitems/completed/20260813-objects-shelf/`](.)  
**Backlog:** [U-5](../../BACKLOG.md)  
**Base:** `origin/dev` (post pool `obj-expr` + `||`/`!=` pushdown)  
**Design:** [`ui.md`](../../../design/ui.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Add a top-level **Objects** workbench view to search and list pool entities (Add-objects-like grid),
park them on a client **shelf**, then **New graph from shelf** → Composer with a replaced draft
(entities only; edges empty in v1).

## Normative locks

| Topic | Lock |
|-------|------|
| Route / nav | `/workbench/objects`; header order **Explorer · Objects · Composer · Query · Schema** |
| Store | Read-only — no graph mutate/create/delete from Objects |
| Search | Shared `MatcherQueryForm`; default `obj-expr`; pool routing (`POST /entities/query` for bare obj-expr) |
| Content | Paginated result table (id, type, scalar payload cols) |
| Shelf | Client-only, unique by entity id, `localStorage` key `objs.ui.objects.shelf` |
| New graph | `navigate('/composer', { state: { graphId: null, replaceDraft: true, graphContents: { entities, edges: [] } } })` |
| Edges | Not on shelf / not induced in Objects v1 |

## Out of scope

- FTS / `searchable` runtime
- Graph-scoped Objects search / Open-graph chrome on this page
- Server-side shelf
- Detail editor on Objects

## Work Items

- [x] WI-001 — Design + story trackers (`WI-001-design.md`)
- [x] WI-002 — Shared result table + Objects search page (`WI-002-search-page.md`)
- [x] WI-003 — Shelf localStorage + pane UI (`WI-003-shelf.md`)
- [x] WI-004 — Nav after Explorer + Composer handoff + tests (`WI-004-nav-handoff.md`)

## Acceptance (story)

- [x] Objects appears in L0 nav **immediately after Explorer**
- [x] Search lists pool entities; add/remove shelf works across navigation
- [x] **New graph from shelf** opens Composer with replaced draft containing shelf entities
- [x] Design docs and UI tests green
