# WI-002 — ExploreScopeBar

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Explore-scope  
**Status:** done  
**Depends on:** WI-001  
**Modules:** `:objs-service` UI

## Goal

Replace the split **CurrentGraphBar + separate Matcher paper** on Explorer with one **ExploreScopeBar** (or equivalent):

- Open graph… / New graph entry points
- Matcher controls + Exec
- Always-visible **mode** + **summary**:
  - **Graph:** **id** (copy) + annotation **pills** beside graph-id chrome (Visual-node pill language); empty → **No annotations**; long values truncate + expand
  - **Selection:** N objects / M edges + matcher one-liner
- Prefer a **shared** graph-header readout component (also used by Composer/Query `CurrentGraphBar` area)

Matcher fields may compact; mode + summary/header must remain visible.

## Touch

- New component under `objs-service/ui/src/` (e.g. `ExploreScopeBar.tsx`)
- Wire into [`GraphExplorerPage.tsx`](../../../../objs-service/ui/src/GraphExplorerPage.tsx)
- Reuse [`MatcherQueryForm`](../../../../objs-service/ui/src/MatcherQueryForm.tsx) / [`OpenGraphModal`](../../../../objs-service/ui/src/OpenGraphModal.tsx) where practical

## Acceptance

- [x] Single fragment on Explorer for Open graph ∪ Matcher
- [x] Mode + summary always visible while canvas shown
- [x] Graph mode shows id (copy) + annotation pills (No annotations; truncate+expand)
- [x] Shared readout reusable by Composer/Query
- [x] No behavior change required beyond layout until WI-003 (mode gating)
