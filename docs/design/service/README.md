# objs-service

**Module:** `:objs-service`  
**Packages:** `org.poc.objs.service`, `org.poc.objs.service.web`

## Role

Publishable Spring library that exposes:

- **REST** controllers for the objs HTTP API (`/graph`, `/registry`, `/status`)
- **Boot autoconfiguration** so a consuming application can pick up objs beans by classpath
- **SpringDoc** OpenAPI annotations + grouped API beans (UI also on `:objs-app`)

Depends on `:objs-core`. Runnable assembly: [`../platform/overview.md`](../platform/overview.md) / `:objs-app`. Domain: [`../graph/`](../graph/README.md).

**Normative endpoint tables:** [`rest-api.md`](rest-api.md).

## Current surface

| Area | Controllers |
|------|-------------|
| Status | `ObjsStatusController` |
| Graph | `ObjsGraphController` |
| Registry | `ObjsRegistryController` |
| OpenAPI | `ObjsOpenApiConfiguration` (`GroupedOpenApi` graph / registry) |

`testIT` suite is registered (ready for broader wiring tests).

## Autoconfiguration contract

Consumers add `objs-service` on the classpath (and provide a DataSource / JPA setup when using core persistence). Autoconfig scans the **service** package; core persistence is picked up via `objs-core` autoconfig imports.

## Design notes / next steps

1. Persist registry catalogs to PostgreSQL (backlog **C-3**)
2. Prefer MockMvc unit tests for controllers (done for graph/registry); use `testIT` for broader wiring when needed
