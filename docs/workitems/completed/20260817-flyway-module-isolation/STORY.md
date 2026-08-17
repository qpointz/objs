# Story: Isolate objs Flyway from derived-app Flyway

**Slug:** `flyway-module-isolation`  
**Branch:** `flyway-module-isolation`  
**Status:** closed  
**Folder:** [`docs/workitems/completed/20260817-flyway-module-isolation/`](.)  
**Backlog:** [P-3](../../BACKLOG.md) (done)  
**Base:** `origin/dev`  
**Design:** [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md), [`docs/design/core/README.md`](../../../design/core/README.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Make objs usable as a **library**: objs-core owns `bom_*` DDL on its own Flyway line; derived apps (including in-repo examples) keep an independent `V1` on Boot Flyway. Stop merging both into one `flyway_schema_history`.

## Normative locks

| Topic | Lock |
|-------|------|
| Model | **Two Flyway runs** on the same DataSource — not reserved version ranges, not extra objs locations on Boot Flyway |
| objs line | SQL in the JAR under `classpath:org/poc/objs/core/db/migration/{vendor}`; history `flyway_schema_history_objs`; versions `V1…VN` |
| App line | Boot Flyway `classpath:db/migration` only; history `flyway_schema_history`; versions `V1…VN` as if objs did not exist |
| `{vendor}` | Spring Boot Flyway placeholder (`DatabaseDriver` id from JDBC URL: `postgresql`, `h2`, …). Unknown driver fails fast. Not JDBC `databaseProductName` |
| SoT | objs-core ships **full vendor SQL** (`V1__bom_schema.sql` per dialect = today’s V1+V2 end state). Drop Java `package db.migration` |
| Order | objs Flyway **first**, then Boot Flyway, then JPA `ddl-auto: validate`. Version numbers do not control order. Both lines may use `V1` |
| Inclusion | Depend on objs-core + autoconfig. **Do not** add objs paths to `spring.flyway.locations`. **Do not** copy objs SQL into app `db/migration` |
| Examples | Derived apps: squash SBOM Java `V3`–`V8` → one app `V1`; AR `V100` → `V1`. Workbench (`:objs-service-app`): `spring.flyway.enabled=false` |
| Future schema | `bom_*` changes = objs `V2+` in the JAR (`postgresql` **and** `h2` in the same WI). App table changes = that app’s next version only |
| Existing DBs | **Greenfield only** — recreate the database or drop both history tables plus domain tables. No history-split repair |
| RULES | WI-001 writes the contract into [`docs/workitems/RULES.md`](../../RULES.md) so later stories cannot merge sequences again |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Story folder, GAPS, trackers |
| 1 — Process + design lock | WI-001 | done | RULES.md Flyway section; persistence.md; GAPS |
| 2 — objs-core | WI-002 | done | Vendor SQL V1 + autoconfig; drop Java migrations |
| 3 — Derived apps | WI-003 | done | SBOM / AR own `V1`; disable Boot Flyway on workbench runner |
| 4 — Living docs | WI-004 | done | persistence / core / embedder docs |

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — RULES.md Flyway section + design lock (`WI-001-rules-and-design.md`)
- [x] WI-002 — objs-core vendor SQL + Flyway autoconfig (`WI-002-objs-flyway.md`)
- [x] WI-003 — Examples as derived apps (`WI-003-derived-apps.md`)
- [x] WI-004 — Living persistence / embedder docs (`WI-004-living-docs.md`)

## Out of scope

- Flyway Teams namespaces  
- Changing `bom_*` DDL beyond folding current V1+V2 into one baseline  
- History-split repair for existing merged `flyway_schema_history`  
- Transactional Save (**D-6**)  
- App-first Flyway order (no config knob)
