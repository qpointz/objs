# Story: Workbench UX

**Slug:** `workbench-ux`  
**Branch:** `workbench-ux`  
**Status:** completed  
**Folder:** [`docs/workitems/completed/20260822-workbench-ux/`](.)  
**Backlog:** [U-7](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Design:** [`docs/design/ui.md`](../../../design/ui.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**UX notes:** [`UX-NOTES/Note 1.md`](UX-NOTES/Note1/Note%201.md), [`UX-NOTES/Note2.md`](UX-NOTES/Note2/Note2.md), [`UX-NOTES/Note 3.md`](UX-NOTES/Note3/Note%203.md), [`UX-NOTES/Note4.md`](UX-NOTES/Note4/Note4.md), [`UX-NOTES/Note5`](UX-NOTES/Note5/Object%20details%20view.md), [`UX-NOTES/Note6`](UX-NOTES/Note6/Note%206.md), [`UX-NOTES/Note7`](UX-NOTES/Note7/Note7.md), [`UX-NOTES/Note8`](UX-NOTES/Note8/Note%208.md), [`UX-NOTES/Note9`](UX-NOTES/Note9/Note%209.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Make Explorer / Objects / Query feel like **one read-only exploration product**: a single shared **graph context**, slim consistent chrome, and layouts that keep the canvas/results dominant.

**Drivers:** Note 1–8. Primarily **`:objs-service-ui`**. No foundation API redesign unless a locked gap requires a thin client-facing contract.

## Normative (from Note 1–4 — locked in WI-001 + Note folds)

| Topic | Lock |
|-------|------|
| Shared context | One graph context across **Explorer · Objects · Query**; change anywhere → reused everywhere |
| Unbound | **Composer** and **Schema** stay unbound to graph context |
| Context meaning (v1) | **Graph** *or* **Matcher** only (`G-UX-ctx` Pic4/Pic5); multi-graph / multi-component expand later |
| Chrome | Slim graph-context bar; **same position** on the three views; **Open** split = Graph \| Matcher; app colors |
| View chrome order | Same row: title · space · context; actions below; help icon off (`G-UX-vchrome`) |
| Graph version pin | Graph-mode bar: version dropdown + pin shared across Explorer / Objects / Query; drop Explorer Versions pane (`G-UX-gver`) |
| Nav (L0) | **Explorer · Objects · Query · Composer · Schema** |
| Objects | Note 7: Query Data-style grid; chrome row (stats + shelf actions); right pane = object viewer **or** **Shelf \| Matcher**; Search in Matcher |
| Query | Shared context chrome; Note 6 chrome row + Options popover (**G-UX-qchrome**); no Matcher |
| Explorer | **disable graph canvas** above **~300** nodes; Note 3/4 actions; Note 5 object viewer (`G-UX-odetail` / `G-UX-over`) |
| Composer | Note 8: `ComposerGraphBar` (visual match only); **New ▾** Blank/Matcher + **Open**; **never** bound to shared context |
| Module | `:objs-service-ui` (+ `:objs-service-app` only if runner wiring needed) |
| Tour | Any WI that moves chrome / primary actions updates the product tour in the **same** WI |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Branch + folder + backlog |
| 1 — Design lock | WI-001 | done | Note 1 → GAPS/STORY; WI list locked |
| 2 — Shared context | WI-002 | done | Graph-context state + Pic4/Pic5 chrome + L0 nav |
| 3 — Objects + Query | WI-003 | done | Objects Matcher\|Shelf; Query Options right tab; drop Query Matcher |
| 4 — Graph version + Explorer | WI-004 | done | Note 2 version pin on context bar; drop Versions pane; 300-node cap; node/edge inspect |
| 5 — Explorer actions | WI-006 | done | Note 3: drop count + Open in Query; Composer/layout as view actions |
| 6 — View chrome | WI-007 | done | Note 4: context first; drop titles; actions below; uniform button size |
| 7 — Object viewer | WI-008 | done | Note 5: `G-UX-odetail` / `G-UX-over` |
| 8 — Query Note 6 | WI-009 | done | Chrome row, Structured grids, in-tab viewer, Open in Composer |
| 9 — Objects Note 7 | WI-010 | done | Grid chrome, chrome row, inspect pane swap, Shelf \| Matcher |
| 10 — Composer Note 8 | WI-011 | done | ComposerGraphBar, New split, Open |
| 11 — Schema Note 9 | WI-012 | done | SchemaContextBar, view chrome, catalog lift |
| 12 — Docs | WI-005 | done | Tour sweep + `ui.md` |

## Work Items

- [x] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock from Note 1 — examples: **docs** (`WI-001-design-lock.md`)
- [x] WI-002 — Shared graph context + nav — examples: **workbench** (`WI-002-shared-graph-context.md`)
- [x] WI-003 — Objects + Query layouts — examples: **workbench** (`WI-003-objects-query-layout.md`)
- [x] WI-004 — Graph version in context + Explorer polish — examples: **workbench** (`WI-004-explorer-polish.md`)
- [x] WI-006 — Explorer action chrome (Note 3) — examples: **workbench** (`WI-006-explorer-action-chrome.md`)
- [x] WI-007 — View chrome order (Note 4) — examples: **workbench** (`WI-007-view-chrome-order.md`)
- [x] WI-008 — Object details viewer (Note 5) — examples: **workbench** (`WI-008-object-details-viewer.md`)
- [x] WI-009 — Query view (Note 6) — examples: **workbench** (`WI-009-query-note6.md`)
- [x] WI-010 — Objects view (Note 7) — examples: **workbench** (`WI-010-objects-note7.md`)
- [x] WI-011 — Composer chrome (Note 8) — examples: **workbench** (`WI-011-composer-note8.md`)
- [x] WI-012 — Schema view chrome (Note 9) — examples: **workbench** (`WI-012-schema-chrome.md`)
- [x] WI-005 — Tour + living docs — examples: **docs** (`WI-005-living-docs.md`)

## Out of scope

- Multi-graph composition as context (deferred after v1)
- Context history New/Save/Recent (`G-UX-hist`) unless pulled in later
- C-19 / C-20 foundation; store text `q`
- SBOM / asset-repository product UX
- Wholesale theme rebrand; Snapshot *hierarchy* UI
- Composer write-flow redesign (only L0 nav position)

## Acceptance (after implementation)

- [x] Open **in-scope** GAPS resolved or explicitly deferred
- [x] Explorer / Objects / Query share one graph context; chrome position stable across those routes
- [x] Objects Matcher\|Shelf and Query Options right-pane layouts shipped; Query Matcher removed
- [x] Objects Note 7 (`G-UX-objgrid` / `G-UX-objchrome` / `G-UX-objview` / `G-UX-objshelf`)
- [x] Composer Note 8 (`G-UX-cgbar` / `G-UX-cnew` / `G-UX-copen`) — visual-only graph bar; never bound to shared context
- [x] Schema Note 9 (`G-UX-sbar` / `G-UX-schrome` / `G-UX-sctx`) — SchemaContextBar; never bound to shared context
- [x] Graph-mode version pin on shared context bar (`G-UX-gver`); Explorer Versions pane removed
- [x] Graph canvas gated at ~300 nodes; Note 1 version inspect shipped (superseded by Note 5)
- [x] Explorer action chrome per Note 3 (`G-UX-eact`): no duplicate count, no Open in Query; Composer + Apply layout as view actions
- [x] View chrome order per Note 4 (`G-UX-vchrome`): context first; no view titles; actions below; uniform view-level button size
- [x] Object details viewer per Note 5 (`G-UX-odetail` / `G-UX-over`)
- [x] Query Note 6 (`G-UX-qchrome` / `G-UX-qstruct` / `G-UX-qview` / `G-UX-qcreate`)
- [x] Tour + [`docs/design/ui.md`](../../../design/ui.md) match shipped UI (WI-005)
- [x] `./gradlew :objs-service-ui:test`

## Process notes

1. One WI at a time; `[x]` + one commit + push per WI.
2. Do not start WI-002 until WI-001 is done (done).
3. Do not close this story until the user asks.
4. **Code hygiene** — Prefer clean, readable `:objs-service-ui` changes. If a surface becomes messy or leaves **stale** chrome/state after the new graph-context model, **rewrite** that area rather than stacking patches. Delete dead Explore-scope / per-view context paths when the shared bar replaces them.
