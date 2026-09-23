# WI-002 — AR domain OpenAPI + Swagger UI

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — AR  
**Status:** done  
**Depends on:** WI-000  
**Examples:** **AR** (`:asset-repository-service`)

## Goal

Same bar as WI-001 for `/api/v1/asset-repository/**`: complete OpenAPI operation + response typing, Swagger UI verified on `:asset-repository-service:run`.

## Deliverables

- [x] `AssetRepositoryController` (and any sibling domain controllers) document methods, request DTOs, and success/error responses with schema types
- [x] Keep group `asset-repository`; tighten `AssetRepositoryOpenApiConfiguration` info/description if needed
- [x] Smoke: run `:asset-repository-service` (demo profile OK) → `/swagger-ui.html` + `/v3/api-docs/asset-repository`
- [x] Module test(s) for a representative OpenAPI subset

## Out of scope

- SBOM (`WI-001`)
- Python client generation from OpenAPI (follow-up if wanted)
