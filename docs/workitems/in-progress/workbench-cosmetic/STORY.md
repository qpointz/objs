# Story: Workbench cosmetic polish

**Slug:** `workbench-cosmetic`  
**Branch:** `workbench-cosmetic`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/workbench-cosmetic/`](.)  
**Backlog:** [U-8](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Design:** [`docs/design/ui.md`](../../../design/ui.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)  
**Prior:** [`workbench-ux`](../../completed/20260822-workbench-ux/STORY.md) (U-7) — functionality kept; this story is visual/feedback polish only

## Goal

Polish workbench (`:objs-service-ui`) **cosmetics and in-progress feedback** where functionality is already good, plus small inspect enhancements listed by the user. No SBOM / asset-repository UX.

**Intake:** Further cosmetic issues are appended as new WIs on this branch as the user lists them.

## Normative

| Topic | Lock |
|-------|------|
| Module | `:objs-service-ui` (+ thin `:objs-core` / `:objs-service` when workbench needs a new read API) |
| Behavior | Prefer presentation / loading feedback; WI-002 adds live graph-membership inspect only |
| Loading splash | Match Explorer overlay pattern (`Loader` + dimmed label on body wash) |
| Graphs usage (WI-002) | Live HEAD membership only; ignore pins; Versions-style chrome; click → shared context Latest |
| Tour / `ui.md` | Update only when chrome copy or documented empty/loading/inspect states change |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Branch + folder + backlog |
| 1 — Objects splash | WI-001 | done | Results pane load overlay |
| 2 — Object Graphs | WI-002 | done | Live graphs containing entity |
| *n* — Further cosmetics | (TBD) | as listed | Append WIs when user defines more issues |

## Work Items

- [x] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [x] WI-001 — Objects results loading splash — examples: **workbench** (`WI-001-objects-load-splash.md`)
- [x] WI-002 — Object detail Graphs usage — examples: **workbench** (`WI-002-object-graphs-usage.md`)

## Out of scope

- Pin / deep-version graph membership in object inspect
- SBOM / asset-repository product UX
- Wholesale theme rebrand
- Shared loading-architecture rewrite (React Query / Suspense)
- Query / Composer / Add Objects blank-while-busy unless pulled in as a later WI

## Acceptance

- [x] Objects view shows a clear in-progress splash while search runs and the results pane would otherwise be blank
- [x] Empty context and “No entities matched” still behave as today when not busy
- [x] Entity inspect shows Graphs section when used in live graphs; hidden when unused
- [x] Each completed WI is `[x]`, committed, and pushed per RULES
- [ ] Story stays open until the user asks to close it

## Process notes

1. One WI at a time; `[x]` + one commit + push per WI.  
2. On first `[x]`, move folder `planned/` → `in-progress/`.  
3. Do not close this story until the user asks.
