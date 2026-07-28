# Persistence

**Status:** early design  
**Parent:** [README.md](README.md)

## Database

- **Primary database: PostgreSQL.**
- Access from Java today: Spring Data **JPA** in `objs-core` (mapping details TBD).
- Package root for domain/persistence types (target): `org.poc.objs…`.

## Entity storage

- Entities live in **relational tables** that expose only **generic / high-level attributes** (examples: type, schema reference, identity — **exact column set TBD**).
- Domain-specific fields from the informational model are **not** first-class columns.
- Entity **payload** is stored as **JSON / JSONB**.
- **Annotations** are **most probably** also stored as **JSON** (working assumption; confirm when query/index needs are known).

## Edges

- Edge / relation table shape is **TBD** (expect generic columns for endpoints, role, and properties).
- Whether edge properties use JSONB is **not stated** yet.

## Validation gate

Persistence is the **enforcement** point for payload schema and allowed edges — see [validation.md](validation.md). The entity SDK must be usable without hitting that gate until save/persist.

## Open

- Exact DDL / column lists and identity type
- JSONB indexing strategy for payload and annotations
- Type/schema catalog storage
- Allowed-edge rules storage
- Flyway (or other) migrations — currently out of scaffold scope
- Whether annotations JSON storage is confirmed vs normalized tables
