# objs-service

**Module:** `:objs-service`  
**Packages (target):** `org.poc.objs.service`, `org.poc.objs.service.web`  
**Packages (scaffold today):** `io.qpointz.poc.objs.service`, `io.qpointz.poc.objs.service.web` — rename in a later WI

## Role

Publishable Spring library that exposes:

- **REST** controllers for the objs HTTP API
- **Boot autoconfiguration** so a consuming application can pick up objs beans by classpath

Depends on `:objs-core`. Domain: [`../graph/`](../graph/README.md).

## Current scaffold

| Type / resource | Purpose |
|-----------------|---------|
| `ObjsServiceAutoConfiguration` | `@AutoConfiguration` + `@ComponentScan` of the service package |
| `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Registers the autoconfig class |
| `ObjsStatusController` | `GET /api/v1/objs/status` → `{ state, module }` smoke endpoint |

`testIT` suite is registered and empty (ready for slice / MockMvc / full-context tests).

## Autoconfiguration contract

Consumers add `objs-service` on the classpath (and provide a DataSource / JPA / PostgreSQL
setup when using core persistence). Autoconfig currently only scans the **service** package; it does **not** yet
declare `@AutoConfigureAfter` / entity scan for core — that should be decided with the domain
design.

## API surface (known)

| Method | Path | Response |
|--------|------|----------|
| `GET` | `/api/v1/objs/status` | `ObjsStatus(state, module)` |

Further resources (entities, edges, subgraph-by-annotation) are TBD under `/api/v1/objs/**` (after foundation story).

## Design notes / next steps

1. Align packages with `org.poc.objs`
2. Expand REST under `/api/v1/objs/**` per graph design (subgraph retrieve, persist with validation) — **not** in the first foundation story
3. Decide whether autoconfig should `@Import` core JPA config / `@EntityScan`
4. Add a thin runnable app module only when local/runtime demos need it
5. Prefer MockMvc / `@WebMvcTest` for controller tests; use `testIT` for broader wiring
