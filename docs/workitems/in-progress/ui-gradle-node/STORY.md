# Story: UI Gradle Node plugin

**Slug:** `ui-gradle-node`  
**Branch:** `ui-gradle-node`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/ui-gradle-node/`](.)  
**Backlog:** [U-6](../../BACKLOG.md)  
**Base:** `origin/dev` (post P-1 / objects-shelf merges)  
**Design:** [`docs/design/platform/build-system.md`](../../../design/platform/build-system.md), [`docs/design/ui.md`](../../../design/ui.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Replace the hand-rolled `Exec` npm install/build/sync tasks in `:objs-service` with
[`com.github.node-gradle.node` 7.0.2](https://plugins.gradle.org/plugin/com.github.node-gradle.node/7.0.2).
Because that plugin must not share a module with Kotlin/JVM plugins (DSL / repository conflicts),
introduce a dedicated top-level **`:objs-service-ui`** leaf module and drop all SPA sync copying.

## Normative locks

| Topic | Lock |
|-------|------|
| Plugin | `com.github.node-gradle.node` **7.0.2** (version catalog) |
| Module | Top-level leaf `:objs-service-ui` at `objs-service-ui/` (moved from `objs-service/ui/`); **no** Kotlin JVM / `kotlin {}` |
| Layout | Normal Gradle include — **not** nested under `:objs-service` |
| Assets | Vite writes to `build/generated/vite`; `processResources` copies into `build/resources/main/static/ui` (project deps use resource dirs, not jar extras) — **no** `syncUiStatic` on `:objs-service` |
| Consumer | `:objs-service` `runtimeOnly(project(":objs-service-ui"))`; remove `npmInstallUi` / `npmBuildUi` / `syncUiStatic` |
| Serving | Unchanged — `ObjsWorkbenchUiConfiguration` serves `classpath:/static/ui/` at `/workbench/` |
| Node | Plugin **downloads** Node **22.14.0** — no host `npm` required for Gradle builds |
| Escape | `-PskipUi=true` still skips npm and yields an empty/minimal UI JAR |
| Dev | `cd objs-service-ui && npm run dev` |
| Related | P-1 (`build-system-cleanup`) explicitly out-of-scopes npm/UI — this story owns it |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | yes | Docs + trackers |
| 1 — Module + plugin | WI-001 | after WI-000 | `:objs-service-ui` + node plugin + JAR |
| 2 — Wire service | WI-002 | after WI-001 | Drop Exec tasks; project dependency |
| 3 — Docs + verify | WI-003 | after WI-002 | AGENTS / design docs; build smoke |

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — `:objs-service-ui` + node-gradle plugin (`WI-001-ui-module-node-plugin.md`)
- [x] WI-002 — Wire `:objs-service` / drop Exec tasks (`WI-002-wire-objs-service.md`)
- [x] WI-003 — Docs + verify (`WI-003-docs-verify.md`)

## Out of scope

- SPA feature work / npm dependency upgrades (aside from `outDir` / path fixes for the module move)  
- Changing `/workbench/` Spring mapping  
- Closing/archiving this story (explicit user request only)
