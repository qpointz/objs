# WI-003 — Persistence (Flyway + records)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Persistence + domain  
**Status:** pending  
**Depends on:** WI-002

## Goal

Add `sbom_application_sbom` and multi-draft columns/constraints on `sbom_application_version`. Migrate existing versions to one constituent + new aggregate graph.

## Deliverables

- [ ] Flyway: `sbom_application_sbom` (`id`, `version_id`, `name`, `description`, `tags`, `graph_id`, `sort_order`)
- [ ] Flyway: `based_on_version_id` on `sbom_application_version`; unique `(application_id, version)` where version not null
- [ ] Backfill: existing graph stays on first constituent; version gets rebuilt/copied aggregate
- [ ] Existing DRAFT rows with null `version` get a default target (per G-Q1)
- [ ] JPA records + repositories
- [ ] Migration tests (H2)

## Out of scope

- Rebuild service logic beyond migration helper (WI-004)
- REST (WI-005)

## Acceptance

- Fresh and migrated schemas satisfy uniqueness and 1..\* constituents per version
- Demo / existing DBs upgrade cleanly in tests
