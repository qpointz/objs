# Story: SBOM + AR OpenAPI / Swagger UI

**Slug:** `sbom-ar-openapi`  
**Branch:** `sbom-ar-openapi`  
**Status:** done  
**Folder:** [`docs/workitems/completed/20260923-sbom-ar-openapi/`](.)  
**Backlog:** [D-9](../../BACKLOG.md)  
**GitLab:** [#6](https://gitlab.qpointz.io/sandbox/bom-poc/-/work_items/6)  
**MR:** https://gitlab.qpointz.io/sandbox/bom-poc/-/merge_requests/74  
**Base:** `origin/dev`  
**Depends on:** —  
**Design:** [`docs/design/sbom/example.md`](../../../design/sbom/example.md) · [`docs/design/asset-repository/example.md`](../../../design/asset-repository/example.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Make the **domain** REST APIs for `:sbom-service` and `:asset-repository-service` fully described in OpenAPI (operations, request/response types, status codes), and keep **Swagger UI** usable when each app is run.

Springdoc is already on both classpaths and groups exist (`inventory`, `asset-repository`). This story tightens annotations and verifies the UI — not a greenfield wire-up.

## Normative

| Topic | Decision |
|-------|----------|
| Scope | Domain routes only (`/api/v1/inventory/**`, `/api/v1/asset-repository/**`) — not foundation `/api/v1/objs/**` |
| Docs bar | Every public domain operation has summary (+ description when non-obvious), documented request body / params, and success + important error responses with schema types |
| Groups | Keep existing groups; add tags if needed so Swagger UI is scannable (e.g. inventory vs assessment vs portfolios) |
| Swagger UI | `./gradlew :sbom-service:run` and `./gradlew :asset-repository-service:run` each serve `/swagger-ui.html` + `/v3/api-docs` (and group docs). Both default to port **8080** — run one at a time (or override `server.port`) |
| Tests | Prefer lightweight OpenAPI/customizer or MockMvc smoke that the documented paths/schemas stay present; no full UI e2e |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Branch + issue + in-progress folder |
| 1 — SBOM | WI-001 | done | Annotations + Swagger smoke on `:sbom-service` |
| 2 — AR | WI-002 | done | Same for `:asset-repository-service` |
| 3 — Docs | WI-003 | done | Living `example.md` pointers |
| 4 — Demo portfolios | WI-004 | done | Purpose-named LOB / stack / security portfolios |
| 5 — Result layout | WI-005 | done | Flat / By category on Apps, Assets, Assessment |

## Work Items

- [x] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [x] WI-001 — SBOM domain OpenAPI + Swagger UI — examples: **SBOM** (`WI-001-sbom-openapi.md`)
- [x] WI-002 — AR domain OpenAPI + Swagger UI — examples: **AR** (`WI-002-ar-openapi.md`)
- [x] WI-003 — Living docs — examples: **docs** (`WI-003-living-docs.md`)
- [x] WI-004 — SBOM demo purpose-named portfolios — examples: **SBOM** (`WI-004-demo-portfolios.md`)
- [x] WI-005 — Portfolio result layout Flat / By category — examples: **SBOM UI** (`WI-005-portfolio-result-layout.md`)

## Out of scope

- Changing domain route shapes or DTOs except where needed for honest schemas
- Foundation workbench OpenAPI polish (`:objs-service` / `:objs-service-app`)
- Auth, rate limits, or Try-it-out credentials
- Running SBOM and AR on the same port at once

## Acceptance (after implementation)

- [x] Swagger UI for SBOM lists domain operations with request/response types (group `inventory` / tags as locked)
- [x] Swagger UI for AR lists domain operations with request/response types (group `asset-repository`)
- [x] `./gradlew :sbom-service:test :asset-repository-service:test` green
- [x] Design `example.md` files point at Swagger UI URLs and OpenAPI groups

## Process notes

Archived 2026-09-23 (merge-ready). Merging into `dev` is manual.
