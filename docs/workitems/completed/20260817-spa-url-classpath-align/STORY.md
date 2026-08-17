# Story: Align SPA URL and classpath prefixes

**Slug:** `spa-url-classpath-align`  
**Branch:** `spa-url-classpath-align`  
**Status:** completed  
**Folder:** [`docs/workitems/completed/20260817-spa-url-classpath-align/`](.)  
**Backlog:** [P-2](../../BACKLOG.md) (done)  
**Base:** `origin/dev`  
**Design:** [`docs/design/ui.md`](../../../design/ui.md), [`docs/design/platform/build-system.md`](../../../design/platform/build-system.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Make each packaged SPA’s **public URL prefix** match its **classpath folder** under `static/`, and point SPA routing filters at that same prefix.

## Normative locks

| Topic | Lock |
|-------|------|
| Convention | HTTP prefix = `static/<same-name>/` |
| Workbench | URL `/workbench/` — classpath `static/workbench/` (was `static/ui/`) |
| Asset repository | URL `/ar/` (was `/app/`) — classpath `static/ar/` (unchanged) |
| SBOM | URL `/sbom/` (was `/ui/`) — classpath `static/sbom/` (unchanged) |
| Old URLs | **No** compatibility redirects from `/app` or `/ui` |
| Root | Domain apps still redirect `/` to their SPA (`/ar/` or `/sbom/`). Workbench does **not** claim `/` |
| Filter | Adjust `SpaRoutingFilter` / SBOM copy; do **not** extract SBOM’s copied filter into core |
| Workbench runner | `:objs-service-app` (`objs-service-app/`) — workbench + foundation REST only; **no** example dependencies |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | yes | Docs + trackers |
| 1 — Align | WI-001 | after WI-000 | Vite, Gradle, handlers, filters, tests |
| 2 — Docs | WI-002 | after WI-001 | Living design / AGENTS / example READMEs |
| 3 — Runner | WI-003, WI-004 | after WI-002 | Rename `:objs-app` → `:objs-service-app`; UI wiring |

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Align SPA URL and classpath prefixes (`WI-001-align-prefixes.md`)
- [x] WI-002 — Living docs (`WI-002-docs.md`)
- [x] WI-003 — Rename objs-app to objs-service-app (`WI-003-rename-service-app.md`)
- [x] WI-004 — Workbench runner wiring (`WI-004-runner-wiring.md`)

## Out of scope

- UI kit extraction  
- Merging SBOM’s copied SPA filter into `objs-core`  
- Gradle convention plugin  
- Old-URL 301s
