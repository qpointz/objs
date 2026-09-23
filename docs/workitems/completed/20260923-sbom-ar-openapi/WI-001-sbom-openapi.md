# WI-001 — SBOM domain OpenAPI + Swagger UI

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — SBOM  
**Status:** done  
**Depends on:** WI-000  
**Examples:** **SBOM** (`:sbom-service`)

## Goal

Document every domain endpoint under `/api/v1/inventory/**` in OpenAPI (summaries, params/bodies, response codes + types) and confirm Swagger UI works on `:sbom-service:run`.

## Deliverables

- [x] Controllers (`ApplicationInventory`, `AssetInventory`, `Portfolio`, `InventorySchema`, `Assessment`, …) have `@Operation` / `@ApiResponses` (or equivalent) so springdoc publishes concrete schemas — not bare maps without types where a DTO exists
- [x] Tags keep the UI scannable (inventory / assets / portfolios / assessment / schema as needed)
- [x] Existing `inventory` `GroupedOpenApi` + `InventoryOpenApiCustomizer` still apply; extend customizer only when annotations cannot express a product note
- [x] Smoke: run `:sbom-service` (demo profile OK) → `/swagger-ui.html` + `/v3/api-docs/inventory` show operations with response schemas
- [x] Module test(s) asserting OpenAPI paths/schemas for a representative subset stay present

## Out of scope

- AR (`WI-002`)
- Foundation `/api/v1/objs/**` on the example classpath
