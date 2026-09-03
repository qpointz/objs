# Objs — platform overview

**Status:** early design (scaffold + domain direction)  
**Coordinates (target):** Maven group / packages under `org.poc.objs.*`  
**Coordinates (scaffold today):** group `io.qpointz.poc.objs`, packages `io.qpointz.poc.objs.*` — rename planned

## Product intent

Objs is a **new** PoC (not a Mill fork) for an **entity store / graph** application.
Process and technology choices intentionally mirror Mill (agent RULES, Gradle multi-module,
Spring Boot, Java toolchain), but the product domain is independent.

Domain: independent **entities** (typed JSON payloads), **edges** (role + properties),
**annotations** for subgraph selection, validation at persistence, PostgreSQL storage.
See [`../graph/README.md`](../graph/README.md).

## Technology stack

| Concern | Choice |
|---------|--------|
| Language | Kotlin |
| JDK | Toolchain **21** |
| Build | Gradle **9.4** (Kotlin DSL), multi-module |
| Versions | Root [`VERSION`](../../../VERSION) file; override `-PprojectVersion=` |
| Catalog | Root [`libs.versions.toml`](../../../libs.versions.toml) |
| Framework | Spring Boot **4.0.x** (BOM via `spring-boot-dependencies`) |
| Persistence | Hibernate/JPA in `:objs-core`; Boot via `:objs-autoconfigure`; **PostgreSQL** primary DB; JSONB for payloads |
| HTTP | Spring WebMVC (`objs-service`) |
| Libraries | Lombok, Jackson 3, JUnit Jupiter, Mockito, AssertJ |
| Packaging style | Libraries + workbench/example apps; Maven/Docker publish reserved in CI |
| CI | GitLab child pipelines — [`ci-pipeline.md`](ci-pipeline.md) |

No custom Gradle convention plugins (`build-logic` was deliberately omitted). Shared
group/version/toolchain live in the root [`build.gradle.kts`](../../../build.gradle.kts).

## Module map

```mermaid
flowchart LR
  app[objs-service-app workbench :8081]
  gsvc[objs-gremlin-service]
  gcore[objs-gremlin-core]
  service[objs-service]
  ui[objs-service-ui]
  sbom[sbom-service :8080]
  sbomui[sbom-service-ui]
  ac[objs-autoconfigure]
  core[objs-core]
  api[objs-api]
  app -->|implementation| service
  app -->|implementation| gsvc
  app -->|runtimeOnly| ui
  gsvc --> gcore
  gsvc --> service
  gcore --> api
  service -->|api| ac
  ac --> core
  core --> api
  sbom --> ac
  sbom --> gcore
  sbom -->|runtimeOnly| sbomui
  core -->|JPA / JSON| db[(H2 local / PostgreSQL)]
```

| Module | Gradle path | Responsibility |
|--------|-------------|----------------|
| **objs-api** | `:objs-api` | Model, matcher/JEXL, validation contracts, seed parse, store ports |
| **objs-core** | `:objs-core` | Spring-free JPA persistence, Flyway SQL, seed apply |
| **objs-autoconfigure** | `:objs-autoconfigure` | Boot adapter (`spring.datasource` → store beans) |
| **objs-service** | `:objs-service` | Spring **REST** (foundation side) |
| **objs-service-ui** | `:objs-service-ui` | Foundation workbench SPA (`runtimeOnly` of `:objs-service-app` or example sidecars) |
| **objs-gremlin-core** | `:objs-gremlin-core` | BoM → TinkerGraph materialization + gremlin-lang eval |
| **objs-gremlin-service** | `:objs-gremlin-service` | Gremlin traverse REST + Boot autoconfig |
| **sbom-service** | `:sbom-service` (`examples/sbom/sbom-service`) | SBOM inventory app (launchable; **no** objs-service) |
| **sbom-service-ui** | `:sbom-service-ui` (`examples/sbom/sbom-service-ui`) | Inventory SPA |
| **objs-service-app** | `:objs-service-app` | Workbench-only runnable — see [`app.md`](app.md) |

Dependency rule: Gremlin-core depends on `:objs-api` only. Boot libraries depend on `:objs-autoconfigure` (which brings `:objs-core`). `objs-service-app` wires the workbench runner only and **must not** depend on `examples/`. Example apps under `examples/` are separate launchables and **must not** depend on `:objs-service` / `:objs-service-ui` / `:objs-gremlin-service` / `:objs-service-app` at compile time. Core must not depend on service/app/gremlin/Spring.

Object/graph capabilities that examples still reimplement (reverse lookup, identity query, paging, graph copy) are listed in [`../graph/apps-vs-foundation.md`](../graph/apps-vs-foundation.md).

## Process alignment with Mill

- Work items / stories: [`docs/workitems/RULES.md`](../../workitems/RULES.md)
- Agent guidelines: [`AGENTS.md`](../../../AGENTS.md)
- Branching: story branches from `dev`, per-WI commits, bracketed commit prefixes
- Design docs by component under `docs/design/` (this tree)
- CI: [`ci-pipeline.md`](ci-pipeline.md) (unit + gated `testIT`; Docker/Maven publish reserved)

## Explicitly out of scope (current scaffold)

- Security / OAuth
- Maven Central / Docker Hub credentials and publish jobs (CI stages reserved only)

**Persistence migrations:** **two Flyway lines** — objs-core `objs_*` (`flyway_schema_history_objs`)
then the embedding app’s Boot Flyway (G-10). See [`../graph/persistence.md`](../graph/persistence.md).

## Open questions

Capture answers under `graph/` when decided; remaining highlights:

1. Entity identity — plain **`UUID`** (resolved); type/schema registry — **in-memory** this story, **PostgreSQL tables later**
2. Annotation shape; confirm JSON storage/indexing
3. Allowed-edge rule model; edge table / property schema
4. Whether edges may carry annotations later (half-open)
5. Public HTTP API shape beyond `/api/v1/objs/status` (deferred past foundation story)
6. Whether autoconfig should also pull in JPA entity scanning from core
7. Apply package/group rename leftovers / docs that still mention `io.qpointz.poc.objs`
