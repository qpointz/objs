# WI-001 — `:objs-service-ui` + node-gradle plugin

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Module + plugin  
**Status:** done  
**Depends on:** WI-000

## Goal

Introduce top-level `:objs-service-ui` with `com.github.node-gradle.node` **7.0.2**, build the workbench SPA into Gradle `build/`, and ship `static/ui/` via the module JAR (**no Sync**).

## Delivered

- Moved `objs-service/ui/` → `objs-service-ui/`
- Catalog plugin `node-gradle` **7.0.2**; Node **22.14.0** download; `npm ci` + `npm run build`
- Vite `outDir` = `build/generated/vite`; JAR packs → `static/ui/`
- `java` plugin (not `java-library`) to avoid Jacoco + node-gradle repo shadow (G-2)
- `-PskipUi=true` skips npm tasks

## Acceptance

- [x] Leaf module `objs-service-ui/` included; old `objs-service/ui/` gone  
- [x] Builds with node-gradle 7.0.2; Vite output under `build/generated/vite`  
- [x] Module JAR contains `static/ui/` **without** any Sync/copy-into-service task  
- [x] `-PskipUi=true` skips npm successfully  
- [x] G-1 / G-2 resolved in `GAPS.md`
