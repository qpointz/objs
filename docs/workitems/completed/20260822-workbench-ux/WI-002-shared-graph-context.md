# WI-002 — Shared graph context + nav

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Shared context  
**Status:** done  
**Depends on:** WI-001  
**Examples:** **workbench**  
**Source:** [`UX-NOTES/Note 1.md`](UX-NOTES/Note1/Note%201.md) Pic1 **(1)–(5)**, Pic4, Pic5; gap **G-UX-ctx**

## Goal

Introduce one shared **graph context** for Explorer / Objects / Query, ship the slim graph-context chrome (Graph vs Matcher modes per Pic4/Pic5), and reorder L0 nav to **Explorer · Objects · Query · Composer · Schema**.

Composer and Schema remain **unbound** to the context.

## Deliverables

- [x] Shared client state for graph context across the three routes (last context across sessions if cheap; New/Save/Recent is `G-UX-hist` deferred)
- [x] Slim graph-context component in the **same position** on Explorer / Objects / Query
- [x] **Graph mode** chrome: icon, UUID + copy, small annotations, node/edge stats, **Open** split (Graph \| Matcher) — Pic4
- [x] **Matcher mode** chrome: selection icon, truncated expression + copy, stats, same **Open** split — Pic5
- [x] Colors match workbench (white/black/blue); Pic4/5 colors are demo only
- [x] L0 nav order updated; tour hooks for nav + context chrome updated in **this** WI
- [x] Tests for context sharing / mode switch / reset behaviour (`graphContext.test.ts`, tour steps)

## Out of scope

- Objects Matcher\|Shelf / Query Options right panes (WI-003)
- Explorer 300-node cap and node/edge version dialog (WI-004)
- Multi-graph / multi-component chain editor; named context library (`G-UX-hist`)

## Hygiene

Prefer a clean shared graph-context module + bar. Remove stale per-view Explore-scope / open-graph chrome when replaced. Rewrite tangled state rather than dual-pathing old and new context.

**Done:** deleted `ExploreScopeBar`; Composer keeps `CurrentGraphBar` + `useCurrentGraphId` unbound.
