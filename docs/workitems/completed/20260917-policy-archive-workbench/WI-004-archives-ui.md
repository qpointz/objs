# WI-004 — Read-only Archives UI

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-003  

## Cold start

- Modes today: [`PolicyPage.tsx`](../../../../objs-service-ui/src/PolicyPage.tsx) · [`PolicyModeTabs.tsx`](../../../../objs-service-ui/src/PolicyModeTabs.tsx) — add **Archives** peer to Policies\|Suites.  
- Persist write UI: [`PolicySuitesPage.tsx`](../../../../objs-service-ui/src/PolicySuitesPage.tsx) — add **Open archive** after save.  
- Reuse (read-only): [`SuiteEvaluationTree.tsx`](../../../../objs-service-ui/src/SuiteEvaluationTree.tsx), graph Visual/Data patterns from Policy/Query.  
- Client: [`policyApi.ts`](../../../../objs-service-ui/src/policyApi.ts) · [`policyTypes.ts`](../../../../objs-service-ui/src/policyTypes.ts).  
- Soft-fail when capabilities lack `"archive"`. See STORY Cold start axis→tab table.

## Goal

**Read-only** Archives **mode inside the existing Policy view** (`/policy`): list archives, open one, inspect present axes (Results / Policies / Input) feature-fully. No new top-level workbench nav or route.

## Scope

- Extend Policy in-page mode tabs: `Policies | Suites | Archives` → Archives panel/component
- Client: `listEvaluations` / `loadEvaluation` / `deleteEvaluation`
- List pane + inspector header (axis chips)
- Conditional tabs: Results, Policies (`executionContext`), Input
- Suites Persist dialog: **Open archive** → switch Policy mode to Archives with that id

## Acceptance

- [x] No new L0 nav; Archives only reachable under Policy
- [x] Archives mode is read-only (no evaluate/save suite/policy authoring)
- [x] Each present axis has a usable inspector; absent axes have no tab
- [x] Input view does not mutate shared graph context
- [x] Open-from-Persist switches to Archives mode with the saved id
