# WI-001 — Workbench UI in objs-service + fix npm build

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Manual-test readiness (prerequisite)  
**Status:** done  
**Depends on:** WI-000

## Goal

Complete **Stage 1** only: move the workbench SPA (and Spring static/SPA routing) from `:objs-sbom-example` into `:objs-service`, and fix `npm run build`, so you are **ready for manual testing** before any performance WI (Stage 2+).

## Why (this story)

Not a performance WI. Separate stage so C-8 backend work is not started until `/workbench/` is usable without the SBOM example module and backend restarts are not blocked by a failing UI build.

## Scope

- Move `objs-sbom-example/ui/` → `objs-service/ui/`.
- Move Gradle npm install/build/sync tasks into [`objs-service/build.gradle.kts`](../../../../objs-service/build.gradle.kts) (`-PskipUi=true` still honored).
- Move SPA serving + legacy redirects into [`ObjsWorkbenchUiConfiguration`](../../../../objs-service/src/main/kotlin/org/poc/objs/service/web/ObjsWorkbenchUiConfiguration.kt) / `LegacyWorkbenchUiRedirectController`.
- Ensure `/workbench/**` (and `/ui/**` redirects) work from `:objs-service` without `:objs-sbom-example` for static UI.
- Fix `npm run build` (`SchemaCatalogOverview.tsx` typed `useEdgesState<Edge>([])`).
- Update design docs (`ui.md`, `sbom/example.md`, `service/README.md`).

## Out of scope

- Graph query performance / candidate sources (**Stage 2+**)
- Removing `:objs-sbom-example` from `:objs-app`
- New workbench features

## Acceptance

- [x] SPA sources and Gradle UI tasks live under `:objs-service`
- [x] `SbomUiWebConfiguration` removed from example; service owns serving
- [x] `npm run build` / `:objs-service:npmBuildUi` succeeds
- [x] Design docs updated for new UI module path
- [x] **Stage 1 readiness gate** in [`STORY.md`](STORY.md) verified by operator (workbench + query smoke) — Stage 2+ unlocked
