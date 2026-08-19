# WI-006 — Paged pool select + `countByType`

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 6 — Page + counts  
**Status:** complete  
**Depends on:** WI-001  
**Examples:** **workbench + SBOM + AR**

## Goal

Store-side paging and type counts so workbench Objects and example inventories do not load a full pool/graph then sort/slice/count in the app.

## Core

- [x] `selectFromPool(matcher, page)` (or equivalent): 1-based page, `size` 1..100 default 20, order `type,id`, total count (G-A6)
- [x] `countByType()` pool-wide; optional graph-scoped member counts (G-A7)
- [x] Pushdown when the matcher lowers; document if total is post-slow-path
- [x] Tests: page boundaries, empty, size cap

## Workbench

- [x] `POST /entities/query` (and graph-scoped query if unbounded today) accepts page/size; Objects page uses them
- [x] Tests in `:objs-service` / UI contract as needed

## SBOM

- [x] `AssetInventoryService.searchPage` / `statistics` use paged select + `countByType`
- [x] Tests in `:sbom-service`

## AR

- [x] Collection object list/search returns a **page** (domain DTO: items + total + page + size)
- [x] `CollectionService.objectCount` uses graph-scoped count, not `get(graph).entities.size`
- [x] Tests in `:asset-repository-service` (including `objectCount` API tests)
- [x] Python client / README only if the search JSON shape changes

## Docs (same commit)

- [x] `apps-vs-foundation.md` — paging/counts **shipped**
- [x] [`WORKBENCH.md`](WORKBENCH.md) Objects paging note
- [x] `docs/design/sbom/example.md` — assets list page
- [x] `docs/design/asset-repository/example.md` — collection object search page + stats
- [x] KDoc

## Out of scope

- Text `q` / contains (**C-20**); FB-3 extra operators; matcher-DSL page keys

## Acceptance

- SBOM asset list does not materialize the full type then slice in Kotlin
- AR `objectCount` / object list do not load the full graph to count or page
- Workbench Objects query is paged
- `./gradlew :objs-core:test :objs-service:test :sbom-service:test :asset-repository-service:test`
