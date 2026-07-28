# objs-service

**Module:** `:objs-service`  
**Packages:** `org.poc.objs.service`, `org.poc.objs.service.web`

## Role

Publishable Spring library that exposes:

- **REST** controllers for the objs HTTP API
- **Boot autoconfiguration** so a consuming application can pick up objs beans by classpath

Depends on `:objs-core`. Runnable assembly: [`../platform/overview.md`](../platform/overview.md) / `:objs-app`. Domain: [`../graph/`](../graph/README.md).

## Current scaffold

| Type / resource | Purpose |
|-----------------|---------|
| `ObjsServiceAutoConfiguration` | `@AutoConfiguration` + `@ComponentScan` of the service package |
| `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Registers the autoconfig class |
| `ObjsStatusController` | `GET /api/v1/objs/status` → `{ state, module }` smoke endpoint |

`testIT` suite is registered and empty (ready for slice / MockMvc / full-context tests).

## Autoconfiguration contract

Consumers add `objs-service` on the classpath (and provide a DataSource / JPA setup when using core persistence). Autoconfig scans the **service** package; core persistence is picked up via `objs-core` autoconfig imports.

## API surface (known)

| Method | Path | Response |
|--------|------|----------|
| `GET` | `/api/v1/objs/status` | `ObjsStatus(state, module)` |

Further resources (entities, edges, subgraph-by-annotation) are TBD under `/api/v1/objs/**`.

## Design notes / next steps

1. Expand REST under `/api/v1/objs/**` per graph design
2. Prefer MockMvc / `@WebMvcTest` for controller tests; use `testIT` for broader wiring
