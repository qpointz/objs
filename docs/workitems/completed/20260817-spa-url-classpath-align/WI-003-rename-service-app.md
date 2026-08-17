# WI-003 — Rename objs-app to objs-service-app

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Workbench runner  
**Status:** complete  
**Depends on:** WI-002

## Goal

Rename `:objs-app` / `objs-app/` to **`:objs-service-app` / `objs-service-app/`**. It is the **workbench-only** runnable: foundation REST + workbench SPA + Gremlin Query. It must not depend on examples or other concrete product modules.

## Deliverables

- [x] Directory and Gradle include `objs-service-app`
- [x] No project deps on `:sbom-*` or `:asset-repository-*`
- [x] Living docs / AGENTS / RULES use `:objs-service-app:run`
- [x] Package stays `org.poc.objs.app` (module rename only)

## Out of scope

- Closing the story
- Archived `docs/workitems/completed/**` rewrites
