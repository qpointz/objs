# WI-002 — Catalog helpers

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Catalog helpers  
**Status:** complete  
**Depends on:** WI-001  
**Examples:** **workbench + SBOM + AR**

## Goal

One core catalog façade: latest ENTITY schema per type, identifier/searchable field hints, inbound/outbound allow-list including `*`, display label, equality filter → `obj-expr`. Workbench and both examples drop local copies.

## Core

- [x] Helper(s) on `BoMSchemaCatalog` / `BoMAllowedEdgeCatalog` (or a small dedicated type) — Java-callable
- [x] Latest version per type via G-A3 comparer (not string max)
- [x] Field hints (G-A9); `displayLabel` (G-A10); filter map → `obj-expr` (G-A11)
- [x] `allowedEdgesForType(type)` inbound/outbound including `*` (G-A8)
- [x] `:objs-core` tests

## Workbench

- [x] `edgesForType` (and any type-catalog listing the workbench duplicates) delegates to the core helper
- [x] Object result labels may use `displayLabel` where the UI currently picks `payload.name` ad hoc
- [x] Tests in `:objs-service`

## SBOM

- [x] `AssetTypeCatalogService` uses core latest + field hints (keep product DTO)
- [x] `SchemaBrowseService.allowedEdgesForType` uses core (wildcard parity with workbench)
- [x] Asset search form / list labels use `displayLabel` + filter helper where the app currently builds `obj-expr` by hand
- [x] Tests in `:sbom-service`

## AR

- [x] `SchemaQueryService` uses core latest + hints + `allowedEdgesForType` (“used in collections” stays domain)
- [x] Collection object search builds `obj-expr` via the core filter helper
- [x] Tests in `:asset-repository-service`

## Docs (same commit)

- [x] `apps-vs-foundation.md` — catalog helpers marked **shipped** with method names
- [x] `docs/design/sbom/example.md` — runtime model from catalog (WI-005 ontology section) points at core helpers
- [x] `docs/design/asset-repository/example.md` — schema-catalog / allowed-edges sketch
- [x] KDoc on the public core types

## Out of scope

- Reverse lookup (WI-003); type → applications/collections (G-A15); example UI restyle

## Acceptance

- Both examples’ schema-browse allow-list includes `*` (parity with workbench)
- Latest-per-type: `1.10.0` > `1.2.0`
- No remaining local “walk all schemas / all edge rules” for those jobs
- `./gradlew :objs-core:test :objs-service:test :sbom-service:test :asset-repository-service:test`
