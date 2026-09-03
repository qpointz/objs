# Story: objs-core Spring split (`objs-autoconfigure`)

**Slug:** `objs-core-spring-split`  
**Branch:** `objs-core-spring-split`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/objs-core-spring-split/`](.)  
**Backlog:** [C-25](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Depends on:** [C-23 `objs-api-codegen`](../../completed/20260828-objs-api-codegen/STORY.md) (`:objs-api` boundary)  
**Design:** [`docs/design/core/README.md`](../../../design/core/README.md), [`docs/design/core/spring-split.md`](../../../design/core/spring-split.md), [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)  
**Sibling:** [C-20 `store-text-search`](../../planned/store-text-search/STORY.md) — independent  
**Follow-up:** rename `:objs-core` → `:objs-persistence` (G-X7); validator on api when Jackson 3-ready (G-X8)

## Goal

Establish the **persistence setup pattern**: expand `:objs-api` as the foundational model (*everything except persistence*), make `:objs-core` a **Spring-free** persistence module (JPA DAOs + internal UoW), and add a tiny `:objs-autoconfigure` Boot adapter that wires `spring.datasource` into that store. Keep the Gradle name `:objs-core` this story; rename later.

## Normative (locked — see GAPS)

| Topic | Lock |
|-------|------|
| `:objs-api` | Model + store ports + matcher contract/in-memory eval + validation contracts + seed parse; **JEXL** OK; **no** networknt/Jackson 2 |
| `:objs-core` | Persistence role: DAOs, store impls, SQL pushdown, Flyway SQL, seed apply, networknt `Validator` impl — **no** `org.springframework.*` |
| `:objs-autoconfigure` | Tiny Boot adapter: DataSource/EMF → beans, `@ConfigurationProperties`, Spring UoW, seed startup |
| `:objs-gremlin-core` | `api(:objs-api)` only |
| Boot apps | `api`/`implementation(:objs-autoconfigure)` |
| Transactions | Internal UoW in core; Spring `TransactionTemplate` in autoconfigure; hidden from public API |
| DAOs | 1:1 mirror of current `JpaRepository` interfaces; no shim |
| Packages | Autoconfigure → `org.poc.objs.autoconfigure.*`; persistence packages stay under `org.poc.objs.core.*` until G-X7 |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Folder + backlog |
| 1 — Design lock | WI-001 | done | All GAPS locked |
| 2 — Api foundation | WI-002 | done | Move non-persistence → api |
| 3 — DAO + TX | WI-003 | done | EntityManager DAOs; UoW |
| 4 — Autoconfigure | WI-004 | done | Tiny Boot module |
| 5 — Consumers | WI-005 | done | Gradle realignment |
| 6 — Tests | WI-006 | done | Harness + Boot slices |
| 7 — Docs | WI-007 | done | Living design + export |

## Work Items

- [x] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock — examples: **docs** (`WI-001-design-lock.md`) — [`4092377`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/4092377)
- [x] WI-002 — Api foundation move + store ports — examples: **—** (`WI-002-api-foundation.md`) — [`ad8a9ac`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/ad8a9ac)
- [x] WI-003 — DAO layer + internal UoW — examples: **—** (`WI-003-dao-transactions.md`) — [`56e3997`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/56e3997)
- [x] WI-004 — `:objs-autoconfigure` module — examples: **—** (`WI-004-autoconfigure-module.md`) — [`fb52223`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/fb52223)
- [x] WI-005 — Consumer Gradle — examples: **SBOM** (`WI-005-consumer-gradle.md`) — [`3886329`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/3886329)
- [x] WI-006 — Test split — examples: **—** (`WI-006-tests.md`) — [`8db3766`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/8db3766)
- [x] WI-007 — Living docs + export — examples: **docs** (`WI-007-living-docs.md`) — [`4f2fd65`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/4f2fd65)

## Out of scope

- REST API or store semantic changes
- `store-text-search` (C-20)
- Gradle rename `:objs-core` → `:objs-persistence` (G-X7)
- Moving networknt `Validator` onto api (G-X8)
- Replacing Hibernate/JPA
- Spring-free `:objs-service`
- Public rename of `GraphStore` / `NamedGraphStore` (G-X6)

## Acceptance (after implementation)

- [x] `objs-core` compile classpath has zero `org.springframework` artifacts
- [x] `:objs-gremlin-core` depends on `:objs-api` only
- [ ] Boot apps work with `objs-autoconfigure` only; objs Flyway + stores behave as before
- [ ] `./gradlew :objs-api:test :objs-core:test :objs-core:testIT :objs-autoconfigure:test :objs-service:test :sbom-service:test :asset-repository-service:test`
- [ ] All GAPS design rows resolved or deferred

## Process notes

1. One WI at a time; `[x]` + one commit + push per WI.  
2. Do not close this story until the user asks.
