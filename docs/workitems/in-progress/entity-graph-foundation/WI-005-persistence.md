# WI-005 — PostgreSQL/JPA persistence for entities and edges

**Story:** [`STORY.md`](STORY.md)  
**Status:** pending  
**Depends on:** WI-003, WI-004  
**Gaps:** G-5 UUID v7, G-9 same gate, G-10 Flyway, G-11 **H2** tests

## Goal

Persist entities and edges via JPA against **PostgreSQL** per
[`docs/design/graph/persistence.md`](../../../design/graph/persistence.md): generic high-level
columns; payload (and annotations) as **JSONB**; **Flyway** migrations from day one; invoke
validation **before** persist on create/update/delete.

## Scope

- **Flyway** migrations as DDL source of truth (Hibernate `ddl-auto` not used to invent schema)
- Relational mapping for entities (generic attributes + **UUID v7** PK + JSONB payload + annotations JSON)
- Mapping for edges (**source** / **target** UUID FKs, role, properties JSONB; edge UUID v7 id preferred)
- Repositories / save path that runs WI-004 persist gate on **create, update, delete**
- **Batch persist** of entities + edges; **two-stage** validation; **create vs update by id presence** (G-19, G-20); transactional all-or-nothing preferred
- Load path returns stored graph as-is (including non-conforming historical data)
- Tests: **H2** (G-11). Note any JSONB/PostgreSQL dialect gaps in WI notes; runtime target remains PostgreSQL.

## Out of scope

- REST controllers / `objs-service` graph APIs
- Full production indexing strategy (document follow-ups)
- Persisting central schema catalog / allow-list to PostgreSQL (C-3)

## Acceptance

- [ ] Flyway migrations create required tables; tests run on **H2**; runtime designed for PostgreSQL
- [ ] Entities and edges round-trip with JSONB payload / properties
- [ ] Invalid create/update/delete rejected at persist; valid writes succeed
- [ ] No-id items created with UUID v7; id-present items update existing rows; unknown id rejects
- [ ] Batch create: new entity + edge to existing persisted entity succeeds when allow-list/schema OK
- [ ] Batch rejects edge whose source/target is neither in payload nor in store
