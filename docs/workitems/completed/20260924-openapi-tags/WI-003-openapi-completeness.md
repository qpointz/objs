# WI-003 — OpenAPI completeness pass

**Status:** done  
**Examples:** objs + SBOM + AR  
**Depends on:** WI-001

## Goal

Bring in-scope OpenAPI up to the G-7 completeness bar.

## Done

- Enhanced `@Operation` / `@Parameter` / `@ApiResponses` / `@Schema` on objs graphs, entities, edges, graph-seeds, registry, status, and policy (+ DTOs)
- Gremlin / jgrapht already had solid ApiResponses (spot-checked)
- SBOM/AR: D-9 baseline + topic tags from WI-002; existing inventory/AR OpenAPI smoke tests remain
- Unit tests: `ObjsOpenApiConfigurationTest`, `ObjsPolicyOpenApiConfigurationTest`

## Remaining (acceptable)

- Some nested domain types outside controller DTOs may still lack field-level `@Schema` — improve when touched
- Full MockMvc `/v3/api-docs` Boot smoke deferred to codegen harness (WI-004) against localhost

## Acceptance

- [x] In-scope public ops largely meet G-7 bar on objs + policy
- [x] SBOM/AR re-audit: no mega-regression; existing smokes
- [x] Config unit tests green
