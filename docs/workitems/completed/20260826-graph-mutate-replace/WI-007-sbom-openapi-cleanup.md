# WI-007 — Remove legacy `example-sbom` REST/OpenAPI

**Status:** done  
**Examples:** SBOM  
**Parallel:** may run anytime after WI-000 (does not block MERGE/REPLACE design)

## Goal

`:sbom-service` published two OpenAPI groups: stale **`example-sbom`**
(`/api/v1/example/sbom/**`) and product **`inventory`** (`/api/v1/inventory/**`).
Removed the legacy façade so only inventory remains.

## Done

- Removed `SbomController`, `SbomDomainOpenApiCustomizer`, and tests
- OpenAPI: only `inventory` group
- Stubbed `random_sbom_crud.py`; updated `docs/design/sbom/example.md` + `platform/app.md`

## Acceptance

- [x] Only `inventory` GroupedOpenApi on `:sbom-service`
- [x] No `/api/v1/example/sbom` mappings
- [x] Design note drops transitional legacy façade
- [x] `./gradlew :sbom-service:test`
