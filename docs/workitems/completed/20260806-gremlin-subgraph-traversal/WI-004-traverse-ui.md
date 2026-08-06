# WI-004 — Workbench Query view

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — UI  
**Status:** done  
**Depends on:** WI-003

## Goal

Add a peer workbench view **Query** for matcher + **gremlin-lang** script execution (UI stays in `:objs-service` workbench; API from `:objs-gremlin-service`).

## Scope

- Route `/workbench/query` in `App.tsx`
- Header nav link **Query** (`IconRoute`) as **3rd** peer after Composer (Explorer → Composer → Query → Schema)
- Header chrome: Workbench brand links home; nav group left after brand; compact dark/light switch on the right
- Page top tabs: **Query** (script only, Groovy highlight) \| **Matcher** \| **Options** (timeout)
- Exec → `POST /api/v1/objs/graph/traverse/gremlin`
- Result tabs: **Structured** (tactical: graph / table / scalar by envelope) \| **Raw** (pretty JSON)
- Explorer: **Open in…** menu → Composer | Query (matcher handoff)
- Light update to `docs/design/ui.md`

## Out of scope

- Final Structured result UX (demo-grade only)
- Replacing Explorer or Composer
- Moving the workbench SPA into `:objs-gremlin-service`
- Full spreadsheet UX; gremlin-lang grammar highlighting (Groovy highlighter only)

## Acceptance

- [x] Query appears in top nav (3rd after Composer) and loads at `/workbench/query`
- [x] Top tabs Query | Matcher | Options; Matcher UI parity with Explorer/Composer
- [x] Structured / Raw result tabs work for graph and analytic scripts
- [x] Explorer Open in… → Composer and Query carry matcher
- [x] Workbench home link + left nav cluster + dark/light toggle
