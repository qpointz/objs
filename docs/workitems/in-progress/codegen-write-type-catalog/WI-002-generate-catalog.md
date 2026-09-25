# WI-002 — Generate write-side type catalog

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Generator  
**Status:** completed  
**Depends on:** WI-001  
**Examples:** **—** (unit tests in `:objs-codegen-java`; consumer modules regenerate in WI-004)

## Goal

Extend `:objs-codegen-java` so each consuming app’s typed-bindings output includes **`EntityCatalog`** (ENTITY) and **`EdgeCatalog`** (EDGE_PROPERTIES) in the same package as `*Node`.

## Done

- [x] Fresh generate produces `EntityCatalog` + `EdgeCatalog` (edge may be empty-bodied if no EDGE_PROPERTIES)
- [x] Lookup + full G-W2 conversions covered by generator tests (`toEntity` / `toNode` / `toTyped` / `fromEntity` / map round-trip)
- [x] Unknown class throws **`IllegalArgumentException`** (G-W8)
- [x] Lane A latest-wins when the same Java payload class spans schema versions
- [x] `./gradlew :objs-codegen-java:test`
