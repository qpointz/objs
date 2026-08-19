# WI-004 — Identity query (FB-2)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Identity query  
**Status:** complete  
**Depends on:** WI-001  
**Examples:** **SBOM + AR**

## Goal

Query pool entities by schema identifier projection (`BoMIdentityProjection`). Grouping/find live in the store. Remove SBOM’s duplicate identifier-immutability check (G-A12).

## Core

- [x] `findEntitiesByIdentity(type, identityMap)` — empty identity → empty (G-A13)
- [x] `findDuplicateGroups(type)` — size > 1; omit empty identity
- [x] May scan-by-type then group (G-A4); not in example services
- [x] Tests: match, no-match, empty identity, two groups

## SBOM

- [x] `AssetInventoryService.findDuplicates` and MI-4 grouping call the store
- [x] Delete update-path identifier freeze that duplicates the persist gate
- [x] Tests in `:sbom-service`

## AR

- [x] `ObjectWriteService.findByIdentity`: store find, then keep entities that are members of `collection.graphId` (conflict if several in-collection)
- [x] Tests in `:asset-repository-service`

## Docs (same commit)

- [x] `apps-vs-foundation.md` — FB-2 row **shipped**
- [x] `docs/design/sbom/example.md` — duplicates find-only via store identity query
- [x] `docs/design/asset-repository/example.md` — IDENTIFIER write mode uses store find
- [x] KDoc

## Out of scope

- Unique index / merge (G-X7); changing persist-gate immutability

## Acceptance

- Duplicate groups match `BoMIdentityProjection.project` grouping for the type
- AR identifier write mode still resolves one collection member (or conflict)
- `./gradlew :objs-core:test :sbom-service:test :asset-repository-service:test`
