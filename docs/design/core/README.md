# objs-core

**Module:** `:core:objs-core`  
**Packages (target):** `org.poc.objs.core`, `org.poc.objs.core.persistence`  
**Packages (scaffold today):** `io.qpointz.poc.objs.core`, `io.qpointz.poc.objs.core.persistence` — rename in a later WI

## Role

Shared library for the entity store PoC:

- **Entity SDK** — in-memory construction of entities, edges, and graphs (no validation enforcement on construct)
- Spring Boot starter primitives (`spring-boot-starter`)
- **JPA persistence** (`spring-boot-starter-data-jpa`) against **PostgreSQL**
- Persistence boundary that **enforces** payload JSON Schema and allowed type–role edges on save — see [`../graph/validation.md`](../graph/validation.md)

Domain model: [`../graph/`](../graph/README.md).

## Current scaffold

| Type | Purpose |
|------|---------|
| `ObjsCore` | Module marker (`MODULE = "objs-core"`) |
| `PersistableEntity` | `@MappedSuperclass` with generated `Long` id — base for future persistence mappings |

No concrete domain entities, repositories, or Flyway migrations yet (Flyway required from WI-005 / G-10).

## Dependencies (notable)

- API: Spring Boot starter, Data JPA, Jackson
- Compile-only: Lombok
- Test: Boot test, Data JPA test, H2, AssertJ, Mockito

## Design notes / next steps

1. Align packages/group with `org.poc.objs`
2. Place domain + persistence per [`../graph/model.md`](../graph/model.md) and [`../graph/persistence.md`](../graph/persistence.md)
3. Implement persist-time validation gate; keep SDK free for arbitrary in-memory graphs
4. Wire repositories and any core `@Configuration` that service autoconfig should import
5. Decide ID strategy — **UUID v7** (resolved) — and PostgreSQL/JSONB mapping details
