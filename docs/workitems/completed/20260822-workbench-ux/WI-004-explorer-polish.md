# WI-004 — Graph version in context + Explorer polish

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Graph version + Explorer  
**Status:** done  
**Depends on:** WI-003  
**Examples:** **workbench**  
**Source:** [`UX-NOTES/Note2.md`](UX-NOTES/Note2/Note2.md), Note 1 Explorer; gaps **G-UX-gver**, **G-UX7**, **G-UX-objver**

## Goal

1. **Graph-context version** (Note 2) — light dropdown on the shared bar (Graph mode only), between copy-id and annotations: shows **Latest** or selected version; open → ~10 recent versions with paging + optional from/to datetime filter; selecting a version **pins it on the shared graph context** so Explorer / Objects / Query all see that freeze. **Remove** the Explorer left Versions pane.
2. **Node cap** — disable the graph canvas when context exceeds **~300** nodes (`G-UX7`).
3. **Node/edge version inspect** — visually similar, shared logic; version or **LATEST**; on-demand versions dialog (`G-UX-objver`).

## Deliverables

- [x] `graphVersion: number | null` (null = Latest/HEAD) on shared graph context; persist with context snapshot
- [x] Version dropdown in `GraphContextBar` (Graph mode); Composer-style light control
- [x] List ~10 versions, paging, optional datetime from/to filter
- [x] Explorer left Versions pane removed; tour hooks updated
- [x] Canvas gated at ~300 nodes
- [x] Node and edge detail: version readout + on-demand versions dialog
- [x] Tests for context version pin, paging/filter helpers, cap
- [x] Thin APIs: version-scoped graph query, traverse pin, entity/edge version list+get; `headVersion` on entities/edges

## Out of scope

- Eager version list on every node/edge select
- Forcing one shared React component for node vs edge version UI
- Snapshot hierarchy UI; Composer freeze/clone redesign
- Version pin for Matcher-mode context (Graph mode only)
