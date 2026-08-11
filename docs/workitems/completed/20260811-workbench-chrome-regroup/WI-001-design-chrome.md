# WI-001 — Design: modes + explore-scope in ui.md

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design  
**Status:** done  
**Depends on:** WI-000  
**Modules:** docs

## Goal

Document in [`docs/design/ui.md`](../../../design/ui.md):

- Explorer **Graph** vs **non-graph** modes
- Explorer **read-only** (no store mutations; Composer owns writes)
- United **Explore-scope** fragment (Open graph ∪ Matcher)
- Always-visible mode + summary (never hide what is explored)
- Mode-gated **Open in Composer** / **New graph from selection** (handoffs only)
- **Shared Open-graph** dialog (all views) + search API (no FTS)
- L0/L1/L2 placement for Explorer / Composer / Query
- L1: title + help-icon **popover** (ship current subtitle copy inline; **no docs links** — package self-sufficient)
- Composer: title **Composer**; no Browse schemas; Visual **New** ▾ / **Link** / **Add objects…**; **Save** vs **Snapshot** (separate; Clone menu retired); schema type links **new tab**
- Composer edit form: no duplicate Payload/Annotations headers; field delete (omit key + deleted mark); Schema ▾ + simple key→key migrate

## Acceptance

- [x] `ui.md` matches STORY locks
- [x] G-U1…G-U3 reflected; deferred items called out
