# Story: Build system cleanup

**Slug:** `build-system-cleanup`  
**Branch:** `build-system-cleanup`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/build-system-cleanup/`](.)  
**Backlog:** [P-1](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Aggressively clean the Gradle multi-module build: minimize plugins, replace Spring
dependency-management with Gradle `platform()`, prune unused catalog entries and
module dependencies down to what each leaf module actually requires.

## Normative locks

| Topic | Lock |
|-------|------|
| BOM | Gradle `platform(libs.boot.dependencies)` — **no** `io.spring.dependency-management` |
| Catalog SoT | `libs.versions.toml` remains version source of truth |
| Plugins | Keep only required: `kotlin-jvm` (all Kotlin modules); `kotlin-spring` only on Spring modules; `kotlin-jpa` only on `:objs-core`; built-ins (`java-library` / `application` / `base`); foojay in settings; jacoco via root |
| Do not | Replace `kotlin-spring` / `kotlin-jpa` with manual `open` / no-arg churn |
| Drop unused | Catalog plugins/deps with no consumers (`spring-boot-plugin`, `lombok`, …) |
| `api` vs `implementation` | `api` only for types on the module’s public API |
| Transitive OK | Do not redeclare a dep a direct project dependency already exposes when not needed on this module’s API |
| jsonschema2pojo | Remove from **default compile** classpath; keep schema export / optional codegen path if useful, or drop if unused by main model |
| Scope | Build/config only — no domain/API behaviour changes |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | yes | Docs + trackers |
| 1 — Inventory | WI-001 | after WI-000 | Keep/drop matrix |
| 2 — Platform + prune | WI-002 … WI-004 | after WI-001 | DM → platform; deps; plugins; verify |

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Dependency + plugin inventory (`WI-001-inventory.md`)
- [x] WI-002 — Drop Spring DM; adopt `platform()` (`WI-002-platform-bom.md`)
- [ ] WI-003 — Prune catalog and module dependencies (`WI-003-prune-deps.md`)
- [ ] WI-004 — Minimize plugins + verify build (`WI-004-plugins-verify.md`)

## Out of scope

- Applying the Spring Boot Gradle plugin / Boot packaging redesign  
- Moving `libs.versions.toml` under `gradle/` (optional follow-up)  
- npm / workbench SPA dependency cleanup  
- Changing TinkerPop / Kotlin / Boot major versions  
