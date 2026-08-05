# Persistence

**Status:** early design  
**Parent:** [README.md](README.md)

## Database

- **Primary database: PostgreSQL.**
- Access from Java today: Spring Data **JPA** in `objs-core` (mapping details TBD).
- Package root for domain/persistence types (target): `org.poc.objs…`.

## Entity storage

- Entities live in **relational tables** that expose only **generic / high-level attributes** (type, schema reference, **id**, …).
- **Primary key / identity: `UUID`** (Java `UUID.randomUUID()`, PostgreSQL `uuid`).
- Domain-specific fields from the informational model are **not** first-class columns.
- Entity **payload** and **annotations** are **JSONB** on PostgreSQL (Flyway `V4__bom_graph_jsonb_gin`); edge **properties** likewise.
- GIN index `idx_bom_graph_entity_annotations_gin` uses `jsonb_path_ops` for containment (`@>`) sources.
  Pushdown predicate is `WHERE annotations @> $filter::jsonb` (no cast on the column) so the planner can use GIN; `SELECT` may still cast columns to `text` for JDBC without affecting index use.
- H2 remains acceptable for non-pushdown unit smoke only; **graph-query SQL pushdown assumes PostgreSQL**.

### Graph read path

- Filtered subgraph reads use a dedicated JDBC reader with fetch sizing rather than `findAll()` hydration.
- Selection plans (`BoMEntitySelectionPlan` / `BoMEntityColumnProjection`) omit unused JSON columns during matching (payload never selected for filters); survivors hydrate deferred columns before domain mapping.
- JSON that is loaded stays as raw strings in `LazyJsonMap` until accessed; equality filters can use tree checks without building a full `MutableMap`.
- Graph queries enter through `POST /api/v1/objs/graph/query` with JSON/YAML matcher DSL.
- Selection is **candidate source → filters → induced edges**. A source-capable first stage
  (DSL `anno` / `MatchAllAnnotationMatcher`, or lowerable DSL `anno-expr`) may supply a Postgres
  JSONB `@>` candidate source; otherwise the reader uses an all-entities source and applies
  `matches` in order (**local eval**).
- In an ordered matcher chain, only the first matcher may provide a source. Later matchers filter
  retained candidates in memory.
- When the entity source is annotation containment (`BoMCandidateSourceWithEdges`), edges load via
  SQL joins on the same `@>` predicate (max induced on the source set), then retain edges among
  survivors. AllEntities / local-eval falls back to id-bounded `IN` induced-edge queries.
- Filter-only matchers (non-lowerable `anno-expr`, legacy adapters) and non-PostgreSQL backends
  scan raw rows in memory using the same lazy maps.

**API response shape** (pagination, result caps, sparse projection) is **out of scope** for this execution-plan work; track as a compensating follow-up if backend gains are insufficient.

Local benchmark against ~85,656 entities / ~71,380 edges selected 6,001 entities / 5,000 edges
(~3.0 MB JSON) by annotation `appVersion=1.0.0`. Captured through the former GET transport
(measurement baseline; not re-run after candidate-source / JSONB work — 2026-08-05). Equivalent query:

```http
POST /api/v1/objs/graph/query
Content-Type: application/json

{"anno":{"appVersion":"1.0.0"}}
```

| Run | Total | TTFB |
|-----|-------|------|
| Before (full `findAll` hydration) | ~60 s (reported) | n/a |
| After anno source + lazy JSON (cold) | ~0.86 s | ~0.65 s |
| After anno source + lazy JSON (warm) | ~0.40–0.46 s | ~0.35–0.37 s |

Remaining time is dominated by selected-row transfer and HTTP JSON serialization of the matched subgraph, not full-table materialization.

## Seed ledger

Flyway `V2__bom_seed_ledger.sql` adds `bom_seed_ledger` for startup seed fingerprints.
See [`seeds.md`](seeds.md).

## Edges

- Edge / relation table shape is **TBD** (expect generic columns for **source**, **target**, role, and properties).
- **Source** / **target** reference entity ids as **`UUID`**.
- Whether edges have their own UUID id: prefer **yes** unless decided otherwise during WI-005.
- Whether edge properties use JSONB: **yes** when properties present (bare edges may store null/empty)

## Validation gate

Persistence is the **enforcement** point for payload schema and allowed edges — see [validation.md](validation.md). The entity SDK must be usable without hitting that gate until save/persist.

## Schema and edge-rule catalogs

- `bom_graph_entity_schema` stores `(type, version)`, authoritative DSL in `definition_doc`,
  and schema `usages` (`ENTITY` / `EDGE_PROPERTIES`); see [object-schema-dsl.md](object-schema-dsl.md).
- Generated JSON Schema is not persisted. It is projected from the DSL for validation and tooling.
- `bom_graph_edge_schema` stores directed allow-list rules, property policies, nullable
  `properties_schema_type + properties_schema_version` references, and **cardinality**
  (`UNSPECIFIED` / `1:1` / `1:*`, column default `UNSPECIFIED`). Flyway
  `V3__bom_edge_cardinality.sql` adds the column. One property schema may be shared by many
  source–role–target rules.
- PostgreSQL is authoritative; application memory is a hydrated read cache.
- Registry writes persist first and then update the cache.

## Testing

- Foundation story tests use **H2** (G-11).
- **Primary runtime database remains PostgreSQL**; document H2 vs JSONB/dialect limitations if any appear during WI-005.

- **Flyway from day one** — migrations are the source of truth for PostgreSQL DDL.
- Do **not** rely on Hibernate `ddl-auto` to invent/evolve production schema (dev may use validate / none against migrated DB).

## Open

- Exact DDL / column lists beyond **UUID** identity
- Whether a GIN (or other) index on **payload** is warranted for future payload pushdown
- Whether annotations JSON storage should ever move to normalized tables
