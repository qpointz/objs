# WI-002 — objs-core domain, Flyway V2, JPA, seeds

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — objs-core  
**Status:** complete  
**Depends on:** WI-001

## Goal

Persist and round-trip catalog metadata in objs-core: domain types, vendor SQL V2, JPA, seed parse/serialize, tests.

## Deliverables

- [x] `BoMAllowedEdgeRule`: `description`, `sourceVerb`, `targetVerb`, `tags`, `attributes`
- [x] `BoMSchema` envelope + `BoMSchemaField`: `tags`, `attributes`
- [x] Drop STRING format allow-list; blank → `null`
- [x] Flyway `V2__catalog_metadata.sql` for **postgresql** and **h2**
- [x] JPA records + catalog mapping (envelope columns; field metadata in `definition_doc`)
- [x] `ObjectSchemaSeedHandler` / `AllowedEdgeRuleSeedHandler` parse + serialize (omit empties)
- [x] Unit / slice tests for normalize, seeds, JPA mapping

## Out of scope

- Workbench UI (WI-003)
- Example ontology wording (WI-004) except tests that need fixtures

## Acceptance

- Empty H2/Postgres: objs V1 then V2; catalogs round-trip the new fields
- Unknown `format` (e.g. `purl`) is accepted
- `./gradlew :objs-core:test`
