# Persistence

**Status:** early design  
**Parent:** [README.md](README.md)

## Database

- **Primary database: PostgreSQL.**
- Access from Java today: Spring Data **JPA** in `objs-core` (mapping details TBD).
- Package root for domain/persistence types (target): `org.poc.objs…`.

## Pool vs graphs

No global graph: an entity **pool** (`bom_entity`) shared by many **graphs** (`bom_graph`). See
[model.md](model.md) and [annotations-and-matchers.md](annotations-and-matchers.md).

| Table | Role |
|-------|------|
| `bom_entity` | Global entity pool — payload + annotations. Membership in 0..n graphs (orphans OK). |
| `bom_graph` | Graph header: **`id` + `annotations` only** — no `parent_graph_id` / `kind` |
| `bom_graph_entity` | Membership M2M `(graph_id, entity_id)` |
| `bom_graph_edge` | Graph-local edge; **`graph_id` NOT NULL** — always owned by exactly one graph |
| `bom_entity_schema` | Entity/payload schema catalog `(type, version)` |
| `bom_edge_schema` | Edge allow-list `(source_type, role, target_type)` + properties policy + cardinality |
| `bom_seed_ledger` | Startup seed fingerprints |

**Flyway:** single migration `V1__bom_schema` (Java) creates this schema for empty databases.
PostgreSQL uses JSONB (+ GIN on `bom_entity.annotations`); H2 uses JSON. There is no
intermediate rename/backfill history — greenfield apply only (reset `flyway_schema_history`
/ recreate the DB if an older V1–V6 history is present).

```mermaid
erDiagram
  bom_entity_schema ||--o{ bom_entity : "type+version"
  bom_entity_schema ||--o{ bom_edge_schema : "optional props schema"
  bom_edge_schema }o..o{ bom_graph_edge : "allow-list"
  bom_graph ||--o{ bom_graph_entity : members
  bom_entity ||--o{ bom_graph_entity : "in 0..n graphs"
  bom_graph ||--o{ bom_graph_edge : owns
  bom_entity ||--o{ bom_graph_edge : source
  bom_entity ||--o{ bom_graph_edge : target

  bom_graph {
    uuid id PK
    json annotations
  }
  bom_graph_entity {
    uuid graph_id PK_FK
    uuid entity_id PK_FK
  }
  bom_entity {
    uuid id PK
    varchar type
    varchar schema_version
    jsonb payload
    jsonb annotations
  }
  bom_graph_edge {
    uuid id PK
    uuid graph_id FK
    uuid source_id FK
    uuid target_id FK
    varchar role
    jsonb properties
  }
```

**Invariant:** edge endpoints must be members of `edge.graph_id`.

## Entity storage

- Entities live in **relational tables** that expose only **generic / high-level attributes** (type, schema reference, **id**, …).
- **Primary key / identity: `UUID`** (Java `UUID.randomUUID()`, PostgreSQL `uuid`).
- Domain-specific fields from the informational model are **not** first-class columns.
- Entity **payload** and **annotations** are **JSONB** on PostgreSQL; edge **properties** likewise.
- GIN index on `bom_entity.annotations` uses `jsonb_path_ops` for containment (`@>`) sources.
  Pushdown predicate is `WHERE annotations @> $filter::jsonb` (no cast on the column) so the planner can use GIN; `SELECT` may still cast columns to `text` for JDBC without affecting index use.
- H2 remains acceptable for non-pushdown unit smoke only; **graph-query SQL pushdown assumes PostgreSQL**.

### Graph read path

- Filtered reads use a dedicated JDBC reader with fetch sizing rather than `findAll()` hydration, scoped to the pool (`obj-expr`) or to graph headers (`graph-expr`).
- Selection plans (`BoMEntitySelectionPlan` / `BoMEntityColumnProjection`) omit unused JSON columns during matching (payload is selected when filters include `obj-expr`); survivors hydrate deferred columns before domain mapping.
- JSON that is loaded stays as raw strings in `LazyJsonMap` until accessed; equality filters can use tree checks without building a full `MutableMap`.
- Graph-scoped queries enter through `POST /api/v1/objs/graphs/{id}/query`; cross-graph queries through `POST /api/v1/objs/graphs/query` — both take the matcher DSL (`graph-expr` / `obj-expr` / chained).
- Selection is **candidate source → filters → induced/stored edges**. A source-capable first stage (`graph-expr` over headers, or lowerable `obj-expr`) may supply a Postgres candidate source; otherwise the reader uses an all-members source and applies `matches` in order (**local eval**).
- In an ordered matcher chain, only the first matcher may provide a source. Later matchers filter retained candidates in memory.
- When the entity source is annotation containment, edges load via SQL joins on the same `@>` predicate bounded to the active graph scope, then retain edges among survivors. `graph-expr` sources instead project the graph's **stored** edges directly (no re-induction). Local-eval falls back to id-bounded `IN` induced-edge queries.
- Filter-only matchers and non-PostgreSQL backends scan raw rows in memory using the same lazy maps.

**API response shape** (pagination, result caps, sparse projection) is **out of scope** for this execution-plan work; track as a compensating follow-up if backend gains are insufficient.

Local benchmark (pre-C-13 rename; pool then called `bom_graph_entity`) against ~85,656 entities / ~71,380 edges selected 6,001 entities / 5,000 edges (~3.0 MB JSON) by an annotation-containment filter equivalent to today's `obj-expr: "a.appVersion == '1.0.0'"`. Captured through the former GET transport (measurement baseline; not re-run after the C-13 rename — 2026-08-05).

| Run | Total | TTFB |
|-----|-------|------|
| Before (full `findAll` hydration) | ~60 s (reported) | n/a |
| After annotation-containment source + lazy JSON (cold) | ~0.86 s | ~0.65 s |
| After annotation-containment source + lazy JSON (warm) | ~0.40–0.46 s | ~0.35–0.37 s |

Remaining time is dominated by selected-row transfer and HTTP JSON serialization of the matched result, not full-table materialization.

## Seed ledger

Table `bom_seed_ledger` (created by `V1__bom_schema`) stores startup seed fingerprints.
See [`seeds.md`](seeds.md).

## Edges

- **Graph-local**: every edge row carries `graph_id` NOT NULL — there is no shared/global edge pool. An edge is owned by exactly one graph.
- **Source** / **target** reference entity ids as **`UUID`**, and both must already be members of `graph_id` (enforced at persist).
- Edges have their **own** UUID id.
- Edge properties use JSONB when properties are present (bare edges may store null/empty).
- Deleting a graph CASCADEs its `bom_graph_edge` rows; deleting a pool entity cascades its incident edges and its `bom_graph_entity` membership rows across every graph.

## Validation gate

Persistence is the **enforcement** point for payload schema and allowed edges — see [validation.md](validation.md). The entity SDK must be usable without hitting that gate until save/persist.

## Schema and edge-rule catalogs

- `bom_entity_schema` stores `(type, version)`, authoritative DSL in `definition_doc`,
  and schema `usage` (`ENTITY` / `EDGE_PROPERTIES`); see [object-schema-dsl.md](object-schema-dsl.md).
- Generated JSON Schema is not persisted. It is projected from the DSL for validation and tooling.
- `bom_edge_schema` stores directed allow-list rules, property policies, nullable
  `properties_schema_type + properties_schema_version` references, and **cardinality**
  (`UNSPECIFIED` / `1:1` / `1:*`, column default `UNSPECIFIED`). One property schema may be shared by
  many source–role–target rules.
- PostgreSQL is authoritative; application memory is a hydrated read cache.
- Registry writes persist first and then update the cache.
- Schema/edge-rule catalogs are **not** graphs — they are global allow-lists, unaffected by the pool/graph split.

## Testing

- Foundation story tests use **H2** (G-11).
- **Primary runtime database remains PostgreSQL**; document H2 vs JSONB/dialect limitations if any appear during implementation.

- **Flyway from day one** — migrations are the source of truth for PostgreSQL DDL.
- Do **not** rely on Hibernate `ddl-auto` to invent/evolve production schema (dev may use validate / none against migrated DB).

## Open

- Exact DDL / column lists beyond **UUID** identity
- Whether a GIN (or other) index on **payload** is warranted for future payload pushdown
- Whether annotations JSON storage should ever move to normalized tables
- Pushdown of `graph-expr` header predicates (currently local eval over `bom_graph`)
