# WI-004 — Add capability-driven workbench analysis support

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Workbench integration  
**Status:** complete  
**Depends on:** WI-003

## Goal

Give the workbench a reusable graph-rendering and analysis-highlight foundation while keeping
algorithm availability capability-driven and backend-owned.

## Scope

- Extract reusable workbench graph concerns from `objs-service-ui/src/GraphCanvas.tsx`:
  - React Flow conversion;
  - Dagre layout direction;
  - measured node sizing;
  - closest-side handles;
  - edge markers;
  - selection/focus;
  - fit-to-view;
  - node and edge highlight sets.
- Add an analysis-highlight model that emphasizes returned entity and edge IDs without mutating
  graph data, selection state, or draft status.
- Preserve the current renderer, Dagre layout, and 300-node cap.
- Add capability loading in `objs-service-ui/src/api.ts`.
- Add conditional cycle-analysis action to `GraphExplorerPage`.
- Call the generic cycle endpoint using the current graph/matcher/version context.
- Hide algorithm actions on capability 404 or network unavailability.
- Render component highlights and clear stale results when context, version, or matcher changes.
- Provide clear no-cycle and failure states.
- Keep the SBOM UI unchanged; no second SPA migration is part of this WI.
- Review the workbench tour for any changed controls or graph interaction.

## Browser boundary

The browser receives only algorithm capability and result DTOs. It does not import JGraphT,
generated JVM classes, `GraphFragment`, or normalization policies.

## Expected touchpoints

- `objs-service-ui/src/GraphCanvas.tsx`
- new or extracted workbench graph model/component files
- `objs-service-ui/src/api.ts`
- `objs-service-ui/src/GraphExplorerPage.tsx`
- `objs-service-ui/src/types.ts`
- graph conversion and UI tests
- `docs/design/ui.md` / `WorkbenchTour.tsx` when controls change

## Tests

- graph conversion and deterministic IDs;
- layout direction and measured sizing;
- self-loops and dangling-edge filtering;
- selection/focus and fit-to-view;
- independent node/edge highlights;
- capability present, absent, and network failure;
- conditional action rendering;
- generic cycle request with live and pinned scopes;
- no-cycle response;
- component highlight mapping;
- stale-result clearing;
- unchanged Explorer, Composer, Query, and schema behavior.

## Acceptance

- [x] Existing graph rendering behavior remains intact.
- [x] Analysis highlighting is independent from domain graph data and selection state.
- [x] Algorithm actions appear only when capabilities advertise them.
- [x] The workbench uses `GENERIC` and never attempts JVM typed materialization.
- [x] Returned entity and edge IDs map deterministically to highlights.
- [x] Missing algorithm service leaves the workbench fully usable.
- [x] The tour and UI design documentation remain accurate.

## Out of scope

- JGraphT or Gremlin implementation.
- SBOM UI migration or SBOM cycle actions.
- Browser graph normalization or conflict resolution.
