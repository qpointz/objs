# WI-001 — objs-gremlin-core module + materializer

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Core essentials  
**Status:** done (Stage 1)  
**Depends on:** WI-000

## Goal

Create Gradle leaf module **`:objs-gremlin-core`** and a **read-only** in-memory materializer: **objects → vertices**, **BoM edges → edges**, with a **pluggable strategy**. Implement **`envelope`** only.

## Scope

- `settings.gradle.kts`: `include(":objs-gremlin-core")`
- Module build (Kotlin JVM **21**, depends on `:objs-core` + TinkerPop **`4.0.0-beta.3`**: `gremlin-core`, `tinkergraph-gremlin`)
- Packages `org.poc.objs.gremlin.core.*` (not under `org.poc.objs.core`)
- `BoMGremlinMaterializationStrategy` + `EnvelopeMaterializationStrategy` + `BoMGremlinMaterializer` facade
- Topology mapping per STORY; nested payload preserved under `envelope`
- Unit tests in `:objs-gremlin-core:test`
- Stub/doc only for future `flatten` / `nested-vertices`
- Root aggregate `test` includes `:objs-gremlin-core:test`

## Out of scope

- Implementing `flatten` or `nested-vertices`
- Script execution / engine (WI-002)
- Matcher `selectAndEval` (Stage 2 / WI-002b)
- `:objs-gremlin-service`, REST, UI
- Putting Gremlin/TinkerPop into `:objs-core`

## Acceptance

- [x] `:objs-gremlin-core` builds and tests pass
- [x] Subgraph materializes under `envelope` with correct vertices/edges
- [x] Strategy interface present; default is `envelope`
- [x] Nested hierarchical payload remains nested map properties
- [x] G-G1 pin `4.0.0-beta.3` recorded in GAPS / catalog
