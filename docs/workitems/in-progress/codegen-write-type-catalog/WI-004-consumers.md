# WI-004 — Consumer proof

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Consumers  
**Status:** completed  
**Depends on:** WI-002; WI-003  
**Examples:** **codegen (prove) + SBOM + AR (smoke)** — [`EXAMPLES.md`](EXAMPLES.md) / G-W7

## Done

- [x] Both codegen example modules green with catalog proof tests
- [x] `:sbom-service` and `:asset-repository-service` smoke tests green
- [x] At least one codegen test shows **no** per-type `switch` for the conversion / identity check
- [x] `./gradlew` for those modules + `:objs-codegen-java:test`

## Notes

- Prove: `EntityCatalogProofTest` in jsonschema + draft07 (catalog convert, identity harness via `IdentityProjection`, generic `add`)
- Smoke: SBOM `GeneratedSbomEntityCatalogSmokeTest`; AR method on `GeneratedAssetRepositoryBindingsTest`
- Fixed generic `add(Object)` to assign `UUID.randomUUID()` before register (parity with typed `add*`)
