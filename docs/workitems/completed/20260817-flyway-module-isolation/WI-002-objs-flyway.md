# WI-002 — objs-core vendor SQL + Flyway autoconfig

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — objs-core  
**Status:** complete  
**Depends on:** WI-001

## Goal

Ship objs schema as vendor SQL in the JAR and apply it with a **second** Flyway bean **before** Boot Flyway / JPA.

## Deliverables

- [x] `objs-core/src/main/resources/org/poc/objs/core/db/migration/postgresql/V1__bom_schema.sql`
- [x] `objs-core/src/main/resources/org/poc/objs/core/db/migration/h2/V1__bom_schema.sql`  
  (each file = current V1 schema + V2 indexes end state)
- [x] Delete Java `db.migration.V1__bom_schema` / `V2__bom_graph_header_indexes`
- [x] `ObjsFlywayAutoConfiguration`: locations `…/db/migration/{vendor}`, table `flyway_schema_history_objs`, `@AutoConfigureBefore(FlywayAutoConfiguration)`, vendor from `DatabaseDriver.fromJdbcUrl`; unknown driver fails fast
- [x] Optional `objs.flyway.*` (enabled, table, locations override); defaults enough
- [x] Core tests: objs bean applies vendor SQL; Boot Flyway empty/disabled for core-only slices; keep H2 unit tests + Postgres IT
- [x] Boot Flyway `baselineOnMigrate` customizer (schema already has `bom_*` when the app line runs)
- [x] `:objs-service-app` Boot Flyway off (no app DDL; required once objs Java migrations left `classpath:db/migration`)

## Out of scope

- Example app `V1` squash (WI-003)
- Changing `bom_*` columns/tables beyond the current end state

## Acceptance

- Empty H2 and Postgres get `bom_*` from objs Flyway only
- Default `classpath:db/migration` in objs-core no longer contains objs scripts
- Autoconfig runs before Boot Flyway
- `:objs-core:test`, `:sbom-service:test`, `:asset-repository-service:test` green. `:objs-core:testIT` not run here (Docker unavailable)
