# Story: Entity REST API

**Slug:** `entity-rest-api`  
**Branch:** `entity-rest-api` (from closed foundation tip)  
**Status:** in-progress  
**Backlog:** C-2  
**Design:** [`docs/design/service/`](../../../design/service/README.md), [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md)  
**Gaps:** [`GAPS.md`](GAPS.md)

## Goal

Expose the entity graph and in-memory registries over HTTP under `/api/v1/objs`:

- **`/graph`** — batch upsert, dry-run validate, annotation-filtered subgraph select, batch delete
- **`/registry/*`** — register and maintain object types via schemas `(type, version)`, and edge allow-list rules / properties policies
- **OpenAPI** — Swagger annotations on controllers; springdoc **3.0.3** on `:objs-service` + `:objs-app` (qpointz / G-R14)

**Out of scope:** load-all graph dump; per-id `/entities` / `/edges` / `/subgraphs` resources; auth/RBAC; PostgreSQL persistence of catalogs (**C-3**).

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 1 — Core helpers | WI-001 | done | Store + catalog APIs |
| 2 — Graph HTTP | WI-002 | done | `/graph` + **unit tests** (G-R19) |
| 3 — Registry HTTP | WI-003 | done | `/registry/*` + **unit tests** (G-R19) |
| 4 — OpenAPI | WI-004 | done | springdoc **3.0.3** on service + app (G-R13/G-R14) |
| 5 — Tests + docs | WI-005, WI-006 | done | cross-check; design docs |

## Work Items

- [x] WI-001 — Core graph + catalog helpers (`WI-001-core-graph-catalog-helpers.md`)
- [x] WI-002 — `/graph` REST controller + error mapping (`WI-002-graph-controller.md`)
- [x] WI-003 — `/registry/*` REST controllers (`WI-003-registry-controllers.md`)
- [x] WI-004 — OpenAPI annotations + springdoc in app (`WI-004-openapi.md`)
- [x] WI-005 — Controller / API tests (`WI-005-tests.md`)
- [x] WI-006 — Design docs + gap resolutions (`WI-006-design-docs.md`)
