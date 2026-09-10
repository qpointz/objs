# Story: Workbench UI improve 2

**Slug:** `workbench-ui-improve-2`  
**Branch:** `workbench-ui-improve-2`  
**Status:** completed  
**Closed:** 2026-09-10  
**Folder:** [`docs/workitems/completed/20260910-workbench-ui-improve-2/`](.)  
**Backlog:** [U-11](../../BACKLOG.md)  
**Base:** `origin/dev`  
**MR:** https://gitlab.qpointz.io/sandbox/bom-poc/-/merge_requests/63  
**Design:** [`docs/design/ui.md`](../../../design/ui.md), [`docs/design/policy/workbench.md`](../../../design/policy/workbench.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)  
**Prior:** [`workbench-cosmetic`](../20260903-workbench-cosmetic/STORY.md) (U-8) — first cosmetic/intake pass; this story continues deferred polish  

## Goal

Continue workbench (`:objs-service-ui`) **UI polish and small UX improvements** where functionality is already good. No SBOM / asset-repository UX.

**Intake:** Issues were appended as new WIs on this branch as the user listed them (same model as U-8). Intake Notes were removed at story closure.

## Normative

| Topic | Lock |
|-------|------|
| Module | `:objs-service-ui` (+ thin `:objs-api` / `:objs-service` when workbench needs a new read API) |
| Behavior | Prefer presentation / feedback / small inspect or chrome fixes; no wholesale feature programs |
| Policy Output | Note 1 area (3)/(7): relocated **tabbed** Policy\|Evaluations (selection-sensitive), not a new Output chrome — see [`GAPS.md`](GAPS.md) G-WU2-N1 |
| Tour / `ui.md` | Update in the same WI when chrome copy or documented empty/loading/inspect states change ([RULES](../../RULES.md) § Workbench product tour) |
| Examples | Workbench only unless a WI explicitly needs a foundation API |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Branch + folder + backlog |
| 1 — Policy/suite layout | WI-001 | done | Note 1 shared chrome + Output |
| 2 — Compact context bar | WI-002 | done | Note 2 graph-context chrome |
| 3 — Single-row view chrome | WI-003 | done | Note 3 title+actions+context |
| 4 — Graph layout overlay | WI-004 | done | Note 4 Apply layout on canvas |
| 5 — View action buttons | WI-005 | done | Note 5 Exec split + unify styles |
| 6 — Graph filter toolbar | WI-006 | done | Note 6 Types/Edges/Severity overlay |
| 7 — Canvas chrome polish | WI-007 | done | Fit / selector UX / Suites Object |
| 8 — Note 7 Data filters + chrome | WI-008 | done | Column funnels; suite/query title chrome |
| 9 — Composer Visual L2 | WI-009 | done | Note 8 toolbar chrome |

## Work Items

- [x] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [x] WI-001 — Policy / suite editor layout (Note 1) — examples: **workbench** (`WI-001-policy-suite-layout.md`)
- [x] WI-002 — Compact graph-context chrome (Note 2) — examples: **workbench** (`WI-002-compact-graph-context-bar.md`)
- [x] WI-003 — Single-row view chrome (Note 3) — examples: **workbench** (`WI-003-single-row-view-chrome.md`)
- [x] WI-004 — Graph Apply-layout overlay (Note 4) — examples: **workbench** (`WI-004-graph-layout-overlay.md`)
- [x] WI-005 — View action button chrome (Note 5) — examples: **workbench** (`WI-005-view-action-button-chrome.md`)
- [x] WI-006 — Graph filter toolbar (Note 6) — examples: **workbench** (`WI-006-graph-filter-toolbar.md`)
- [x] WI-007 — Canvas chrome polish (fit / selector / Suites Object) — examples: **workbench** (`WI-007-canvas-chrome-polish.md`)
- [x] WI-008 — Policy Data column filters + title chrome (Note 7) — examples: **workbench** (`WI-008-policy-data-filters-chrome.md`)
- [x] WI-009 — Composer Visual L2 toolbar (Note 8) — examples: **workbench** (`WI-009-composer-visual-toolbar.md`)

## Out of scope

- SBOM / asset-repository product UX
- Wholesale theme rebrand
- Shared loading-architecture rewrite (React Query / Suspense)
- Policy matrix / consumer product UI (policy family stories)

## Acceptance

- [x] Each completed WI is `[x]`, committed, and pushed per RULES
- [x] Tour / `ui.md` reviewed when chrome changes
- [x] Story closed only at explicit user request
