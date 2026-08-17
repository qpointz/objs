# Gaps — flyway-module-isolation (P-3)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

Locks below are the story contract. Durable process text is
[`docs/workitems/RULES.md`](../../RULES.md) **Flyway (library + derived apps)**;
[`docs/design/graph/persistence.md`](../../../design/graph/persistence.md) records the objs SQL layout.

---

## Architecture

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-A1 | One vs two Flyway | **resolved** | Two independent Flyway beans on the same DataSource. Not reserved version ranges. Not merging objs SQL into Boot locations |
| G-A2 | History tables | **resolved** | objs: `flyway_schema_history_objs`. App: Boot default `flyway_schema_history` |
| G-A3 | Version numbers | **resolved** | Unique **per history table**. Both lines may ship `V1`. Order is Spring, not version comparison |
| G-A4 | Startup order | **resolved** | objs first, then Boot Flyway, then JPA validate. `@AutoConfigureBefore(FlywayAutoConfiguration)`. Boot Flyway `baselineOnMigrate` + `baselineVersion` `0` (objs already created `bom_*`; app `V1` still applies). No app-first knob |
| G-A5 | How apps include objs | **resolved** | Depend on `objs-core` + leave autoconfig on. Do **not** add objs locations to `spring.flyway.locations` |
| G-A6 | objs SoT | **resolved** | Full vendor SQL in the JAR. Drop Java `db.migration.V1` / `V2`. Fold current V1+V2 into one `V1__bom_schema.sql` per dialect |
| G-A7 | Vendor id | **resolved** | Spring Boot `{vendor}` / `DatabaseDriver.fromJdbcUrl` (`postgresql`, `h2`). Not JDBC `databaseProductName` (`"PostgreSQL"`). Unknown driver → fail fast |
| G-A8 | SQL paths | **resolved** | `classpath:org/poc/objs/core/db/migration/{vendor}/V1__bom_schema.sql` |
| G-A9 | Examples | **resolved** | Derived apps, same contract as an external product. SBOM: one `V1` = current inventory schema. AR: `V100` → `V1`. Workbench: `spring.flyway.enabled=false` |
| G-A10 | Existing databases | **resolved** | Greenfield only. Recreate DB or drop both history tables + domain tables. No repair of a split merged history |
| G-A11 | Process home | **resolved** | [`RULES.md`](../../RULES.md) **Flyway (library + derived apps)** (after Module Reference) plus a Persistence / Flyway row under Concrete example integration |

---

## Open

_(none — remaining work is implementation, not product questions)_

---

## Out of story

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | Flyway Teams namespaces | **cancelled** | Not used |
| G-X2 | `bom_*` DDL changes | **cancelled** | Baseline is current V1+V2 end state only |
| G-X3 | App-first order | **cancelled** | App DDL that FKs to `bom_*` would fail |
| G-X4 | Transactional Save | **deferred** | Backlog **D-6** |
