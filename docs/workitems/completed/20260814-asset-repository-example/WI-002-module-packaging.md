# WI-002 — Module packaging under `examples/asset-repository/`

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  

## Goal

Create Boot-runnable example modules under `examples/asset-repository/`: **Java 21** domain service (with objs-service / workbench) + domain UI shell.

## Deliverables

- [x] Create `examples/asset-repository/` modules (service + UI shell)  
- [x] `settings.gradle.kts` includes with `projectDir` under `examples/asset-repository/`  
- [x] Service: **Java 21** toolchain; Spring Boot plugins; **no Kotlin plugins**  
- [x] Service depends on `:objs-core` + `:objs-service` (**workbench sidecar** at `/workbench/`; app must not use foundation REST)  
- [x] UI: Vite/React + node-gradle packaging; `runtimeOnly` into service  
- [x] `:objs-app` does **not** depend on this example  
- [x] `./gradlew :asset-repository-service:compileJava -PskipUi=true` succeeds  

## Out of scope

- Domain ontology, REST resources, real explore screens (stubs OK)  

## Acceptance

- Modules build; service compiles; Java-only sources; packaging matches G-A1 / G-A2 / G-A7  
