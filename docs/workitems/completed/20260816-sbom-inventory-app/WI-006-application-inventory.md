# WI-006 — Application inventory service + domain API

**Story:** [`STORY.md`](STORY.md)  
**Journey:** 1 — Application owner (Applications tab)  
**Gaps:** G-P3, G-P4  
**Status:** complete  

## Goal

Domain service + HTTP API in product language: search applications; load/edit draft BOM (assets + relations); reuse or create assets. App→app dependency is **inferred** (G-P4), not authored.

No portfolio or MI endpoints here (those are WI-011 / WI-013). Versions are WI-007.

## Deliverables

- [x] `ApplicationInventoryService` via objs-core APIs  
- [x] Flyway `V3__sbom_application_inventory` + JPA for `sbom_application` / `sbom_application_draft`  
- [x] Domain DTOs / routes under `/api/v1/inventory/applications/**` (no BoM* leakage)  
- [x] Inferred deps helper (`GET …/depends-on`) over **draft** graphs (version scope in WI-007)  
- [x] Tests  

## Acceptance

- [x] Application owner can search and edit without graph vocabulary  
- [x] Public API shapes use product language only  
