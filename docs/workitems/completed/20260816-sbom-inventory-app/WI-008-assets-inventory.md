# WI-008 — Assets inventory service + domain API

**Story:** [`STORY.md`](STORY.md)  
**Journey:** 2 — under Applications chrome  
**Gaps:** G-F4 / FB-3; G-F1 / FB-1 parked; G-F2 / FB-2 parked; G-P5, G-P7  
**Status:** complete  

## Goal

Assets inventory for the Application owner chrome: search by type; schema-driven advanced search (`searchable` only); usage inspect; duplicates by identifier (find-only); optional owning application.

Portfolio **MI-3/MI-4** reuse overlapping ideas at set scope but do not live in this API’s UI entry points.

## Deliverables

- [x] `AssetInventoryService` with FB-1/FB-2 stopgaps (scan draft/version graphs; in-memory identity groups)  
- [x] Domain routes under `/api/v1/inventory/assets/**`  
- [x] Advanced search: **searchable fields only**; equality via `selectFromPool`  
- [x] Ontology field flags (`identifier` / `searchable`) on `SbomRegistry` + regenerated `sbom-ontology.yaml`  
- [x] Tests  

## Acceptance

- [x] Journey 2 capabilities available in product language  
- [x] No foundation vocabulary in API shapes  
