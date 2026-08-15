# WI-005 — Domain model + ontology alignment

**Story:** [`STORY.md`](STORY.md)  
**Depends on:** WI-001, WI-002  
**Status:** complete  

## Goal

Align / extend existing SBOM graph schema only as needed; wire application to **compile object model from schema** (G-A7 / G-A8). Application inventory and portfolios remain SBOM tables (G-P3 / G-P10).

## Deliverables

- [x] Reuse `sbom-ontology` / catalog; document additive encodings (`owner`, no ApplicationRef)  
- [x] Runtime model from schema catalog: `AssetTypeCatalogService` + `GET /api/v1/inventory/asset-types`  
- [x] Wave* / `SbomRegistry` documented as **non-SoT** builder helpers (full codegen deferred)  
- [x] Mapping in GRAPH-AND-RETRIEVAL §6 + `docs/design/sbom/example.md`  
- [x] Relation beautifier (`RelationLabels`) for G-P6  
- [x] Tests  

## Acceptance

- [x] Asset types for product UI come from schema catalog (seeds SoT)  
- [x] G-P4/G-P5 covered by documented encodings  
- [x] Portfolio / MI stay domain-table driven  
