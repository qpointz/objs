# Story: Multi-BOM application versions

**Slug:** `multi-bom-app-versions`  
**Branch:** `multi-bom-app-versions`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/multi-bom-app-versions/`](.)  
**Backlog:** [D-8](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Design (product):** [`docs/design/sbom/example.md`](../../../design/sbom/example.md)  
**Design (engineer mapping):** [`GRAPH-AND-RETRIEVAL.md`](GRAPH-AND-RETRIEVAL.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Extend the SBOM inventory app so an **application version** is a **combination of SBOM constituents** (partial BOMs), with a **materialized Combined SBOM** used for fingerprint, MI, depends-on, and CycloneDX of “the version”. Support **multiple parallel drafts**, each with a **target version** and **based-on** lineage — still on `sbom_application_version` (no new draft table). Progressive UI: single-constituent apps behave as today; multi-constituent chrome appears only when count ≥ 2.

## Normative locks

| Topic | Lock |
|-------|------|
| Cardinality | Application **1→\*** versions; version **1→1..\*** SBOM constituents. Cannot delete the last constituent. |
| Constituent | Own named graph; editable only when parent version is **DRAFT**. Metadata: **name**, **description**, **tags**. |
| Aggregate | Version `graph_id` = **materialized Combined SBOM** (union of membership + edges). Rebuild on constituent save/create. Always **read-only** in UI. |
| Multi-draft | Many `DRAFT` rows per app. Target semver in `version` while DRAFT. `based_on_version_id` on same table. Unique `(application_id, version)` when version set. |
| Progressive UI | Count = 1 → today’s chrome. Count ≥ 2 → Combined list, left-pane SBOM switch, `App / SBOM` root. **Create SBOM** on overview is the 1→2 entry (DRAFT only). |
| Combined label | Product name **Combined SBOM** (not “Root”). Virtual; no metadata row. |
| Latest (portal / API) | Highest **RELEASED** by **semver**, not `promotedAt`. |
| Demo | ~50% apps single BOM; ~50% 2–3 constituents. Portal shows latest pill + SBOMs/versions stats. |
| Out of story | CDX/SPDX **import**; transactional Save (**D-6**); file demo seeds (**D-7**); foundation `copyGraph` API; UI kit. |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | yes | Story folder, GAPS, trackers |
| 1 — Product + graph design | WI-001, WI-002 | after WI-000 | Glossary; GRAPH-AND-RETRIEVAL; lock open GAPS |
| 2 — Persistence + domain | WI-003, WI-004 | after Stage 1 | Flyway; rebuild; multi-draft services |
| 3 — API | WI-005 | after WI-004 | REST + OpenAPI |
| 4 — UI | WI-006 | after WI-005 | Progressive disclosure + drafts |
| 5 — Demo + portal + living docs | WI-007, WI-008 | after WI-006 | Seeder, Applications portal, design README |

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [ ] WI-001 — Product design + glossary (`WI-001-product-glossary.md`)
- [ ] WI-002 — Graph and retrieval mapping (`WI-002-graph-and-retrieval.md`)
- [ ] WI-003 — Flyway constituents + multi-draft columns (`WI-003-persistence.md`)
- [ ] WI-004 — Aggregate rebuild + domain services (`WI-004-domain-services.md`)
- [ ] WI-005 — REST constituents + multi-draft (`WI-005-rest-api.md`)
- [ ] WI-006 — Progressive UI + drafts + metadata (`WI-006-ui.md`)
- [ ] WI-007 — Demo seeder + Applications portal (`WI-007-demo-portal.md`)
- [ ] WI-008 — Living docs (`WI-008-living-docs.md`)

## Out of scope

- CycloneDX / SPDX **import** of partial SBOMs  
- Transactional Save (backlog **D-6**)  
- File-based demo inventory (backlog **D-7**)  
- Foundation `copyGraph` store API (keep app-local helper)  
- Auth / multi-tenant  
- UI kit extraction
