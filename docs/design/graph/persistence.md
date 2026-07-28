# Persistence

**Status:** early design  
**Parent:** [README.md](README.md)

## Database

- **Primary database: PostgreSQL.**
- Access from Java today: Spring Data **JPA** in `objs-core` (mapping details TBD).
- Package root for domain/persistence types (target): `org.poc.objs…`.

## Entity storage

- Entities live in **relational tables** that expose only **generic / high-level attributes** (type, schema reference, **id**, … — exact column set TBD beyond id).
- **Primary key / identity: UUID v7** (Java `UUID`, PostgreSQL `uuid`). Prefer v7 over v4 for insert/index locality.
- Domain-specific fields from the informational model are **not** first-class columns.
- Entity **payload** is stored as **JSON / JSONB**.
- **Annotations** are **most probably** also stored as **JSON** (working assumption; confirm when query/index needs are known).

## Edges

- Edge / relation table shape is **TBD** (expect generic columns for **source**, **target**, role, and properties).
- **Source** / **target** reference entity ids as **UUID v7**.
- Whether edges have their own UUID id: prefer **yes** (UUID v7) unless decided otherwise during WI-005.
- Whether edge properties use JSONB: **yes** when properties present (bare edges may store null/empty)

## Validation gate

Persistence is the **enforcement** point for payload schema and allowed edges — see [validation.md](validation.md). The entity SDK must be usable without hitting that gate until save/persist.

## Testing

- Foundation story tests use **H2** (G-11).
- **Primary runtime database remains PostgreSQL**; document H2 vs JSONB/dialect limitations if any appear during WI-005.

- **Flyway from day one** — migrations are the source of truth for PostgreSQL DDL.
- Do **not** rely on Hibernate `ddl-auto` to invent/evolve production schema (dev may use validate / none against migrated DB).

## Open

- Exact DDL / column lists beyond **UUID v7** identity
- JSONB indexing strategy for payload and annotations
- Type/schema catalog storage — **central `(type, version)` → JSON Schema** in-memory for foundation; **later** PostgreSQL tables (entities + edges share catalog)
- Allowed-edge rules storage
- Whether annotations JSON storage is confirmed vs normalized tables
