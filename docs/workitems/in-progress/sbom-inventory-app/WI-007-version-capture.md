# WI-007 — Version capture

**Story:** [`STORY.md`](STORY.md)  
**Journey:** 1 — Application owner  
**Gaps:** G-P2, G-P4, G-F5  
**Consumers:** WI-013 MI uses **latest version** graph per app (R22)

## Goal

Create application versions from draft; copy memberships/edges into a **new** graph. App→app dependency at a version is **inferred** later from that version’s shared objects with other apps (G-P4) — not copied as explicit app-dep edges.

Define **latest version** ordering so R22 is deterministic (e.g. capture timestamp / version sequence).

## Deliverables (fill after Stage 1)

- [ ] Version create/get APIs  
- [ ] Copy/snapshot strategy per GRAPH-AND-RETRIEVAL  
- [ ] Query helpers: apps/shared assets inferred from version graph members (R7/R8)  
- [ ] Stable “latest version for application” helper (R22)  
- [ ] Tests  

## Acceptance

- User can inspect a specific application version and see inferred app deps / shared assets from frozen graph members  
- R22 can resolve one latest `graph_id` per application (or none)  
