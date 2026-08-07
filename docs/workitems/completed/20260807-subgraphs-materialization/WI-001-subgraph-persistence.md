# WI-001 — Flyway + JPA membership tables

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Persistence + domain API  
**Status:** done  
**Depends on:** WI-000  
**Modules:** `:objs-core`

## Goal

Add durable tables and JPA mappings for subgraph headers and soft-link membership (G-S1, G-S6, G-S8). No store API yet (WI-002).

## Context

- Existing entity/edge tables: Flyway `V1__bom_entity_edge.sql`; JPA in `BoMEntityRecord.kt` / `BoMEdgeRecord`.
- Next migration: **`V5__bom_subgraph.sql`** under `objs-core/src/main/resources/db/migration/` (if V5 already taken, use next free `V{N}`).
- Postgres JSONB GIN for graph annotations is V4 (Java migration) — **do not** require GIN on subgraph annotations in v1 unless cheap; H2 must still apply SQL.

## Scope

### DDL

Implement exactly the sketch in [`STORY.md`](STORY.md) (table/column names normative):

| Table | Columns | FKs |
|-------|---------|-----|
| `bom_subgraph` | `id UUID PK`, `annotations JSON NOT NULL` | — |
| `bom_subgraph_entities` | `subgraph_id`, `entity_id` composite PK | → `bom_subgraph(id)` CASCADE, → `bom_graph_entity(id)` CASCADE |
| `bom_subgraph_edges` | `subgraph_id`, `edge_id` composite PK | → `bom_subgraph(id)` CASCADE, → `bom_graph_edge(id)` CASCADE |

Indexes on `entity_id` and `edge_id` for reverse lookup / cascade performance.

### JPA

New records (same package / style as `BoMEntityRecord`):

- `BoMSubgraphRecord` → `bom_subgraph`
- `BoMSubgraphEntityRecord` → `bom_subgraph_entities` (embeddable / `@IdClass` or `@EmbeddedId`)
- `BoMSubgraphEdgeRecord` → `bom_subgraph_edges`

Repositories: `JpaRepository` interfaces.

### Tests

- Spring Data / Flyway test that migration applies (reuse existing H2 test slice pattern from `BoMGraphStoreTest`).
- Insert subgraph + membership; delete entity → membership row gone; delete subgraph → membership gone; entity row remains when only subgraph deleted.

## Out of scope

- Domain service API (WI-002)
- REST, matcher, UI, snapshot logic
- Annotation vocabulary — **none** in objs (G-S11); store opaque free-form map
- Promoting subgraph header to `bom_graph_entity` type (rejected for v1; separate table)

## Implementation checklist

- [x] Flyway SQL added and listed
- [x] JPA entities + repos compile
- [x] Cascade delete verified in test
- [x] `:objs-core:test` (relevant) green
- [x] Mark this WI `[x]` in STORY tracker; commit; push

## Acceptance

- [x] Migration applies on H2 (unit) and does not break Postgres profile expectations
- [x] Repositories can CRUD header + membership rows
- [x] Deleting entity/edge removes membership via FK cascade
- [x] Deleting subgraph removes membership; does not delete graph entities/edges

## Commit message hint

`[feat] Add bom_subgraph membership tables (WI-001)`
