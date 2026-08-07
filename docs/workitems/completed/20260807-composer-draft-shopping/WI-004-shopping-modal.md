# WI-004 — Composer Add objects modal

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Add objects UI  
**Status:** done  
**Depends on:** WI-001, WI-002, WI-003, WI-006  
**Gaps:** G-S9, G-S11–G-S15, G-S17, G-S18, G-S23

## Goal

Replace replace-style **Load…** with **Add objects…**: matcher search (incl. hand-authored `obj-expr` via shared form), results table, immediate add/exclude, Done → induced edge merge via `ids`.

## Scope

- Use shared `MatcherQueryForm` from WI-006 (includes `obj-expr` + visual chain); Composer modal **defaults to `obj-expr`** (G-S13)
- `ObjectLinterPage`: **Add objects…** button/modal; retire wipe+confirm Load as primary path (G-S12)
- Naming: components / labels use Add objects — **not** shop/shopping/cart (G-S23)
- Results table: id, type, ≤6 top-level scalar payload columns (G-S11)
- Add selected / In-draft toggle → **immediate** merge/exclude (G-S9)
- **Client-side pagination**, page size **20** (G-S17)
- On Done: `ids` query for **store-backed** draft entity ids → `mergeEdgesIntoDraft` (G-S10)
- Explorer → Composer handoff: open modal + Search (append, never replace) (G-S14)
- On successful Search: `beginQueryResult()` → **new `qid`** (G-S15)

## Out of scope

- Field-builder for `obj-expr` (users write JEXL)
- Server-side query limit/offset
- Matcher form internals (WI-006)

## Acceptance

- [x] User can search with typed `obj-expr` (and chains), add individual rows, exclude without Delete
- [x] Results table paginates locally at 20 rows per page
- [x] Draft updates immediately on Add / In-draft; Done only refreshes edges then closes
- [x] Done merges induced edges among store-backed draft ids only
- [x] Draft is never wiped by Search in the Add objects modal
- [x] Successful Search assigns a new `qid`
- [x] No shop/shopping/cart in UI copy or component identifiers
