# objs-service

**Module:** `:objs-service`  
**Packages:** `org.poc.objs.service`, `org.poc.objs.service.web`

## Role

Publishable Spring library that exposes:

- **REST** controllers for the objs HTTP API (`/entities`, `/graphs`, `/graph` I/O, `/registry`, `/status`)
- **Boot autoconfiguration** so a consuming application can pick up objs beans by classpath
- **SpringDoc** OpenAPI annotations + grouped API beans (UI also on `:objs-service-app`)
- **Workbench SPA** serving (`ObjsWorkbenchUiConfiguration`) when `:objs-service-ui` is on the consuming app’s runtime classpath (`:objs-service-app` or an example sidecar)

Depends on `:objs-core` only (no `:objs-service-ui`). Gremlin traverse REST lives in **`:objs-gremlin-service`** (see [`../graph/gremlin.md`](../graph/gremlin.md)). Workbench runnable: [`../platform/overview.md`](../platform/overview.md) / `:objs-service-app` (`runtimeOnly` `:objs-service-ui`). Domain: [`../graph/`](../graph/README.md). Operator UI: [`../ui.md`](../ui.md).

**Normative endpoint tables:** [`rest-api.md`](rest-api.md).

## Current surface

| Area | Controllers / config |
|------|-------------|
| Status | `ObjsStatusController` |
| Entity pool | `ObjsEntitiesController` (`/entities`) |
| Graphs | `ObjsGraphsController` (`/graphs`) |
| Graph I/O | `ObjsGraphController` (import/export/validate under `/graph`) |
| Registry | `ObjsRegistryController` |
| Workbench SPA | `ObjsWorkbenchUiConfiguration` |
| OpenAPI | `ObjsOpenApiConfiguration` (`GroupedOpenApi` graph / registry; includes `/graphs/**` + `/entities/**`) |

`testIT` suite is registered (ready for broader wiring tests).

## Autoconfiguration contract

Consumers add `objs-service` on the classpath (and provide a DataSource / JPA setup when using core persistence). Autoconfig scans the **service** package; core persistence is picked up via `objs-core` autoconfig imports.

## Design notes / next steps

1. Persist registry catalogs to PostgreSQL (backlog **C-3**)
2. Prefer MockMvc unit tests for controllers (done for graph/registry); use `testIT` for broader wiring when needed
