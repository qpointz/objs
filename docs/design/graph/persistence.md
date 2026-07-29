# Persistence

**Status:** early design  
**Parent:** [README.md](README.md)

## Database

- **Primary database: PostgreSQL.**
- Access from Java today: Spring Data **JPA** in `objs-core` (mapping details TBD).
- Package root for domain/persistence types (target): `org.poc.objs…`.

## Entity storage

- Entities live in **relational tables** that expose only **generic / high-level attributes** (type, schema reference, **id**, … — exact column set TBD beyond id).
- **Primary key / identity: `UUID`** (Java `UUID.randomUUID()`, PostgreSQL `uuid`).
- Domain-specific fields from the informational model are **not** first-class columns.
- Entity **payload** is stored as **JSON / JSONB**.
- **Annotations** are **most probably** also stored as **JSON** (working assumption; confirm when query/index needs are known).

### Graph read path

- Filtered subgraph reads use a dedicated JDBC reader with fetch sizing rather than `findAll()` hydration.
- JSON columns remain raw strings wrapped in lazy maps; deserialization occurs on first field access or when serializing the final selected subgraph.
- **Pushable** matchers with supported expressions (annotation equality/conjunction) compile to parameterized PostgreSQL `@>` predicates and select induced edges in SQL.
- **Non-pushable** matchers and non-PostgreSQL backends scan raw rows in memory using the same lazy maps.

Local benchmark against ~85,656 entities / ~71,380 edges (`GET /api/v1/objs/graph?appVersion=1.0.0` → 6,001 entities / 5,000 edges, ~3.0 MB JSON):

| Run | Total | TTFB |
|-----|-------|------|
| Before (full `findAll` hydration) | ~60 s (reported) | n/a |
| After pushable + lazy JSON (cold) | ~0.86 s | ~0.65 s |
| After pushable + lazy JSON (warm) | ~0.40–0.46 s | ~0.35–0.37 s |

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
- `bom_graph_edge_schema` stores directed allow-list rules, property policies, and nullable
  `properties_schema_type + properties_schema_version` references. One property schema may be
  shared by many source–role–target rules.
- PostgreSQL is authoritative; application memory is a hydrated read cache.
- Registry writes persist first and then update the cache.

## Testing

- Foundation story tests use **H2** (G-11).
- **Primary runtime database remains PostgreSQL**; document H2 vs JSONB/dialect limitations if any appear during WI-005.

- **Flyway from day one** — migrations are the source of truth for PostgreSQL DDL.
- Do **not** rely on Hibernate `ddl-auto` to invent/evolve production schema (dev may use validate / none against migrated DB).

## Open

- Exact DDL / column lists beyond **UUID** identity
- JSONB indexing strategy for payload and annotations
- Whether annotations JSON storage is confirmed vs normalized tables
