# WI-004 — Module packaging

**Story:** [`STORY.md`](STORY.md)  
**Gaps:** G-A1, G-A2, G-X1 (**resolved**)  

## Goal

Relocate the SBOM product under **`examples/sbom/`** and make it Boot-runnable without foundation REST/workbench.

## Target layout

```text
examples/sbom/sbom-service/      → :sbom-service       (domain + launchable)
examples/sbom/sbom-service-ui/   → :sbom-service-ui    (Vite SPA; node-gradle like :objs-service-ui)
```

## Locks (G-X1)

- `:sbom-service` depends on **`objs-core`** (+ **`objs-gremlin-core`** for MI); **`runtimeOnly` `:sbom-service-ui`**
- **No** `objs-service` / workbench on the product path
- **`objs-app`** stops depending on the SBOM example (foundation-only assembly)
- Former `objs-sbom-example` removed / migrated; no backward compatibility

## Deliverables

- [x] Create `examples/sbom/` and move/rework modules to `:sbom-service` + `:sbom-service-ui`  
- [x] `settings.gradle.kts` includes with `projectDir` under `examples/sbom/`  
- [x] `:sbom-service` application plugin + main class; `run` documented  
- [x] Domain REST + OpenAPI retained (`/api/v1/example/sbom`); inventory API evolves in later WIs  
- [x] `:sbom-service-ui` Gradle/node packaging parity with `:objs-service-ui` (Applications \| Portfolios stub)  
- [x] Remove `:objs-sbom-example`; decouple `:objs-app`  
- [x] Update `AGENTS.md` / design pointers  

## Acceptance

- [x] `./gradlew :sbom-service:run` starts inventory app without foundation workbench REST  
- [x] `./gradlew :objs-app:run` runs foundation without SBOM inventory  
- [x] Domain OpenAPI group `example-sbom` present on `:sbom-service`  
- [x] `:sbom-service` depends on `objs-gremlin-core` for in-process MI (no gremlin REST required)  

**Status:** complete  
