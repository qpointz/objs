# WI-005 — PostgreSQL/JPA persistence for entities and edges

**Story:** [`STORY.md`](STORY.md)  
**Status:** pending  
**Depends on:** WI-003, WI-004

## Goal

Persist entities and edges via JPA against **PostgreSQL** per
[`docs/design/graph/persistence.md`](../../../design/graph/persistence.md): generic high-level
columns; payload (and annotations) as **JSONB**; invoke validation **before** persist.

## Scope

- Relational mapping for entities (generic attributes + JSONB payload + annotations JSON)
- Mapping for edges (endpoints, role, properties — shape documented)
- Repositories / save path that runs WI-004 persist gate
- Load path returns stored graph as-is (including non-conforming historical data)
- Tests: preferably Testcontainers PostgreSQL or documented equivalent; H2 only if JSONB strategy is explicitly constrained and noted

## Out of scope

- REST controllers / `objs-service` graph APIs
- Flyway unless required to land a minimal schema (prefer explicit decision in WI notes)
- Full production indexing strategy (document follow-ups)

## Acceptance

- [ ] Entities and edges round-trip through PostgreSQL with JSONB payload
- [ ] Invalid writes rejected at persist; valid writes succeed
- [ ] Subgraph selection works against loaded data (reuse WI-003)
- [ ] Design persistence open items updated for chosen columns/identity
