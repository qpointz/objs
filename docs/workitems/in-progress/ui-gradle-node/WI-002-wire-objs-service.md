# WI-002 — Wire `:objs-service` / drop Exec tasks

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Wire service  
**Status:** done  
**Depends on:** WI-001

## Goal

Make `:objs-service` consume the UI JAR and remove the hand-rolled npm `Exec`/`Sync` pipeline.

## Delivered

- Removed `npmInstallUi` / `npmBuildUi` / `syncUiStatic` from `:objs-service`
- `runtimeOnly(project(":objs-service-ui"))` (G-3)
- Clean `:objs-service:jar` does not embed SPA; assets come from the UI module JAR on the runtime classpath

## Acceptance

- [x] No Exec-based UI tasks remain on `:objs-service`  
- [x] `:objs-service:build` pulls UI assets via `:objs-service-ui`  
- [x] `-PskipUi=true` still allows Kotlin service build  
- [x] G-3 resolved in `GAPS.md`
