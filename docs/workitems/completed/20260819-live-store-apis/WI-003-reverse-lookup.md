# WI-003 — Reverse lookup (FB-1)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Reverse lookup  
**Status:** complete  
**Depends on:** WI-001  
**Examples:** **SBOM + AR**

## Goal

Store API: entity → graph ids and incident edges. SBOM usage / MI-2/MI-3 and AR object relations stop scanning graphs in the app.

## Core

- [x] `listGraphIdsForEntity(entityId)` (G-A5)
- [x] `listIncidentEdges(entityId, graphId: UUID? = null)` → `BoMEdge` including `graphId` (G-A14)
- [x] Tests: many graphs; orphan; in/out edges; `graphId` filter

## SBOM

- [x] `AssetInventoryService` usage: store reverse lookup, then map graph ids → application/version/BOM **domain** rows
- [x] `MiReportService` MI-2/MI-3: shared-asset / app-dep using store membership, not N× `get(graphId)` scans
- [x] Tests in `:sbom-service`

## AR

- [x] `ObjectWriteService.listRelations`: `listIncidentEdges(objectId, collection.graphId)` (still in-collection)
- [x] Tests in `:asset-repository-service`

## Docs (same commit)

- [x] `apps-vs-foundation.md` — FB-1 row **shipped**
- [x] `docs/design/sbom/example.md` — asset detail “used in” / depends-on: store reverse + domain join
- [x] `docs/design/asset-repository/example.md` — object relations via incident edges
- [x] KDoc

## Out of scope

- Identity duplicates (WI-004); app→app *meaning* (stays SBOM); new Flyway tables

## Acceptance

- Usage/relations do not iterate all product graphs to find membership
- Orphan pool entities → no graph ids
- `./gradlew :objs-core:test :sbom-service:test :asset-repository-service:test`
