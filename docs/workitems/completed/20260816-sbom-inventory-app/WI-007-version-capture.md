# WI-007 — Version capture

**Story:** [`STORY.md`](STORY.md)  
**Journey:** 1 — Application owner  
**Gaps:** G-P2, G-P4, G-F5  
**Consumers:** WI-013 MI uses **latest version** graph per app (R22)  
**Status:** complete  

## Goal

Create application versions from draft; copy memberships/edges into a **new** graph. App→app dependency at a version is **inferred** later from that version’s shared objects with other apps (G-P4) — not copied as explicit app-dep edges.

Define **latest version** ordering so R22 is deterministic (e.g. capture timestamp / version sequence).

## Deliverables

- [x] Version create/get APIs (`POST/GET …/versions`, `…/versions/latest`, `…/depends-on`)  
- [x] Copy strategy: **same pool entity ids** (membership) + new edge rows — not hard `clone` (preserves G-P4)  
- [x] Query helpers: version-scoped inferred deps (R7); peers prefer latest version else draft  
- [x] Stable “latest version for application” helper + `latestGraphIds` (R22)  
- [x] Flyway `V4__sbom_application_version`  
- [x] Tests  

## Acceptance

- [x] User can inspect a specific application version and see inferred app deps / shared assets from frozen graph members  
- [x] R22 can resolve one latest `graph_id` per application (or none)  
