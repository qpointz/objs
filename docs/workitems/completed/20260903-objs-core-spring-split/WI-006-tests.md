# WI-006 — Tests

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 6 — Tests  
**Status:** done — [`8db3766`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/8db3766)  
**Depends on:** WI-005 (done)  
**Examples:** **—**

## Goal

Restore full regression coverage with a spring-free objs-core test harness and Boot slice tests in objs-autoconfigure.

## Scope

- [x] `ObjsPersistenceTestSupport` — Flyway + Hibernate EMF + DAO + store construction with EM-backed UoW
- [x] Migrate objs-core persistence/seed tests off `@DataJpaTest` / `ObjsCoreAutoConfiguration`
- [x] Move `ObjsFlywayAutoConfigurationTest` Boot slice to objs-autoconfigure (`ApplicationContextRunner`)
- [x] Keep `ObjsFlywayIntoExistingAppSchemaTest` on JDBC/Flyway only (no Spring)
- [x] objs-core `testIT` (Postgres) on harness + Testcontainers (`ObjsPostgresPersistenceFixture`)
- [x] objs-autoconfigure `test` (wiring + Flyway vendor SQL)

## Out of scope

- Example app test rewrites beyond compile (sbom/ar tests run in acceptance)
- Living docs / export (WI-007)

## Acceptance

- `./gradlew :objs-core:test :objs-core:testIT :objs-autoconfigure:test`
- Same behavioural coverage as pre-split for store, catalog, flyway, seed paths
