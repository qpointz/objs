# WI-009 — Store `createdAt` / `updatedAt` (graph, node, edge)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 8 — Audit timestamps  
**Status:** cancelled — moved to **C-19** [`foundation-after-versions`](../../planned/foundation-after-versions/STORY.md) (clocks on version rows after C-18)  
**Depends on:** WI-001  
**Examples:** **workbench + SBOM + AR**

## Goal

Every **named graph**, **pool entity (node)**, and **graph-local edge** carries store-managed `createdAt` and `updatedAt` (`Instant`, UTC). Workbench REST and both examples **read** those fields; they must not invent payload/annotation clocks for the same job.

Catalog tables already have `created_at` / `updated_at`. Domain tables (`sbom_application`, `ar_collection`, …) keep their own clocks.

## Core

- [ ] Flyway **V3** `h2` **and** `postgresql` (RULES **Flyway (library + derived apps)**): `bom_entity`, `bom_graph`, `bom_graph_edge` add `created_at` / `updated_at` NOT NULL; backfill `CURRENT_TIMESTAMP`
- [ ] JPA records + domain `BoMEntity` / `BoMEdge` / graph header (`BoMResolvedGraph` / list item) expose `createdAt` / `updatedAt`
- [ ] Persist gate **owns** the clocks:
  - insert → both = now
  - update of that row → `createdAt` unchanged, `updatedAt` = now
  - client-supplied values **ignored** (not identity-style errors)
- [ ] Graph `updatedAt` also bumps when membership or any edge in that graph is inserted/updated/deleted
- [ ] Entity `updatedAt` bumps on payload/annotation/type/schemaVersion write, not merely because another graph linked it
- [ ] `copyGraph` / `mergeGraph`: new graph + new edges get now; **shared pool entities keep their timestamps**
- [ ] `clone()`: new ids → new timestamps
- [ ] Tests (H2 + Postgres IT for SQL defaults): create, update, ignore client stamps, copy vs clone, membership bump

## Workbench

- [ ] Entity / edge / graph JSON includes `createdAt` / `updatedAt`
- [ ] Objects table and graph header/open UI can show them (minimal: columns or detail)
- [ ] Tests in `:objs-service`

## SBOM

- [ ] Asset (and relation if the DTO is store-backed) views include store timestamps — **not** ontology `CanonicalEdgePayload.createdAt`
- [ ] Tests in `:sbom-service`

## AR

- [ ] `ObjectDto` includes store timestamps
- [ ] `CollectionStatisticsDto.lastUpdated` = **graph** `updatedAt` (today reserved/null)
- [ ] Tests in `:asset-repository-service`

## Docs (same commit)

- [ ] `apps-vs-foundation.md` — audit columns **shipped**; distinct from domain-table clocks and from payload `createdAt`
- [ ] Persistence design: V3 columns + persist rules
- [ ] `docs/design/sbom/example.md` + `docs/design/asset-repository/example.md`
- [ ] [`WORKBENCH.md`](WORKBENCH.md) / [`EXAMPLES.md`](EXAMPLES.md)
- [ ] KDoc: store-owned, UTC, client cannot set

## Out of scope

- `created_by` / users / audit log table
- Timestamping `bom_graph_entity` membership rows
- Changing SBOM/AR **product** row clocks
- Sorting/filtering inventories by `updatedAt` (nice follow-up; expose fields first)

## Acceptance

- Fresh persist: both instants set; second write keeps `createdAt`, advances `updatedAt`
- Workbench, SBOM asset, AR object JSON all show the same store instants for one entity
- AR collection stats `lastUpdated` tracks graph mutations (add object / edge)
- `./gradlew :objs-core:test :objs-core:testIT :objs-service:test :sbom-service:test :asset-repository-service:test`
