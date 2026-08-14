# WI-008 — Assets inventory service + domain API

**Story:** [`STORY.md`](STORY.md)  
**Journey:** 2 — under Applications chrome  
**Gaps:** G-F4 / FB-3; G-F1 / FB-1 parked; G-F2 / FB-2 parked; G-P5, G-P7  

## Goal

Assets inventory for the Application owner chrome: search by type; schema-driven advanced search (`searchable` only); usage inspect; duplicates by identifier (find-only); optional owning application.

Portfolio **MI-3/MI-4** reuse overlapping ideas at set scope but do not live in this API’s UI entry points.

## Deliverables (fill after Stage 1)

- [ ] `AssetInventoryService` (+ foundation APIs if WI-003 added them; else stopgaps per FOUNDATION-BACKLOG)  
- [ ] Domain DTOs / routes  
- [ ] Advanced search: **searchable fields only**; slow path OK  
- [ ] Tests  

## Acceptance

- Journey 2 capabilities available in product language  
- No foundation vocabulary in API/UI copy  
