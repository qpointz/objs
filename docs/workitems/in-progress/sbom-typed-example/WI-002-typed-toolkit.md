# WI-002 — Core typed-domain toolkit

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-001 (parallel OK if only touching `objs-core`)  
**Gaps:** G-S1, G-S2, G-S3

## Goal

Add reusable typed façades and graph assembly helpers in `objs-core` so concrete domains (SBOM and future) convert to/from `BoMEntity` / `BoMEdge` / `BoMGraph` without subclassing foundation data classes.

## Scope

Package `org.poc.objs.core.typed`:

- `EntityTypeMeta` — `type` + `schemaVersion` (+ optional schema resource path)
- `TypedEntity<P>` — envelope id, annotations, typed payload; `toBoMEntity` / `fromBoMEntity` via Jackson map convert
- `TypedEdgeMeta` / `TypedEdge<R>` — role + optional properties; `toBoMEdge`
- `GraphBuilder` — add entities (**provisional UUID** if missing, G-S3), local string keys, edges by ref, optional **default annotations** on add; `build(): BoMGraph`
- `RegistryPack` — schemas + allow-list rules; `registerInto(…)`; load schema JSON from classpath when practical
- Shared `PayloadMapper` (JsonMapper + Kotlin module)
- Lightweight annotation merge helpers (vocabulary-agnostic)
- Unit tests in `objs-core`

## Out of scope

- SBOM-specific types, keys, or services
- REST changes

## Acceptance

- [x] Toolkit compiles and is usable from another module without SBOM dependency
- [x] Round-trip typed payload ↔ `BoMEntity` covered by tests
- [x] `GraphBuilder` can produce a valid `BoMGraph` with provisional ids and default annotations
- [x] `RegistryPack.registerInto` registers schemas and edge rules
