# WI-006 — Application inventory service + domain API

**Story:** [`STORY.md`](STORY.md)  
**Journey:** 1 — Application owner (Applications tab)  
**Gaps:** G-P3, G-P4  

## Goal

Domain service + HTTP API in product language: search applications; load/edit draft BOM (assets + relations); reuse or create assets. App→app dependency is **inferred** (G-P4), not authored.

No portfolio or MI endpoints here (those are WI-011 / WI-013).

## Deliverables (fill after Stage 1)

- [ ] `ApplicationInventoryService` via objs-core APIs  
- [ ] SBOM Flyway/JPA for application (+ draft) tables linked to graph ids  
- [ ] Domain DTOs / routes (no BoM* leakage)  
- [ ] Inferred deps read helper for a draft/version (feeds UI; MI-2 reuses idea within portfolio set)  
- [ ] Tests  

## Acceptance

- Application owner can search and edit without graph vocabulary  
- Public API shapes use product language only  
