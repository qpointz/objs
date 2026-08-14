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

- [ ] Create `examples/sbom/` and move/rework modules to `:sbom-service` + `:sbom-service-ui`  
- [ ] `settings.gradle.kts` includes with `projectDir` under `examples/sbom/`  
- [ ] `:sbom-service` application plugin + main class; `bootRun` / `run` documented  
- [ ] Domain REST + OpenAPI for inventory API (no CLI)  
- [ ] `:sbom-service-ui` Gradle/node packaging parity with `:objs-service-ui` (stub OK until WI-009)  
- [ ] Remove `:objs-sbom-example`; decouple `:objs-app`  
- [ ] Update `AGENTS.md` / design pointers  

## Acceptance

- `./gradlew :sbom-service:run` (or documented equivalent) starts the inventory app without foundation workbench REST  
- `./gradlew :objs-app:run` still runs foundation without SBOM inventory  
- Domain OpenAPI documents inventory endpoints  
- App can call Gremlin engine in-process for MI without `/api/v1/objs/**`  
