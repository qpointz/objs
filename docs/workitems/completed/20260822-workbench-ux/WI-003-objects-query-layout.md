# WI-003 — Objects + Query layouts

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Objects + Query  
**Status:** done  
**Depends on:** WI-002  
**Examples:** **workbench**  
**Source:** [`UX-NOTES/Note 1.md`](UX-NOTES/Note1/Note%201.md) Pic2, Pic3; gaps **G-UX5**, **G-UX6**, **G-UX-q**

## Goal

Consume shared graph context on **Objects** and **Query**, and ship the right-pane layouts from Note 1 / GAPS.

### Objects

- Matcher runs **chained within** current context → **`obj-expr` only**
- Right pane tabs **Matcher \| Shelf** + vertical splitter
- **Search** in Matcher tab; **Clear shelf** / **New graph from shelf** in Shelf tab

### Query

- Same shared graph-context chrome
- **Options** → right-side tab only
- **Remove Matcher** from Query — the only query means is the Query script/traversal itself (`G-UX-q`)

## Deliverables

- [x] Objects layout + context-scoped search (`scopeObjectsSearch`)
- [x] Query: Options right tab; Matcher tab/flow removed; Exec uses `matcherFromGraphContext`
- [x] Tour steps for Objects / Query chrome updated in **this** WI
- [x] Tests: `objectsSearchScope.test.ts`, `queryGraphContext.test.ts`, tour steps

## Out of scope

- Explorer node cap / node/edge version inspect (WI-004)
- Composer write UX; Schema editor polish
- Pool-wide Objects search as default (context-scoped is the new default)
- Re-adding matcher as a Query input path
