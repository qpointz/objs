# WI-004 — OpenAPI annotations + springdoc (qpointz pattern)

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-002, WI-003  
**Gaps:** G-R13 (**3.0.3**), G-R14 (qpointz placement)

## Goal

Document every endpoint with `io.swagger.v3.oas.annotations` and wire SpringDoc the same way as qpointz/Mill.

## Notes

- Catalog: `springDoc = "3.0.3"` → `libs.springdoc.openapi.starter.webmvc.ui`
- `implementation` on `:objs-service` and `:objs-app`
- `ObjsOpenApiConfiguration` groups: `graph`, `registry`
- Tags: `status`, `graph`, `registry`

## Acceptance

- [x] Controllers annotated; springdoc on service + app via catalog
- [x] Catalog pin **3.0.3**; `:objs-service:test` and `:objs-app:compileKotlin` green
