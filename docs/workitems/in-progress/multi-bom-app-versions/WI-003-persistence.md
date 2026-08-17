# WI-003 — Persistence (Flyway + records)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Persistence + domain  
**Status:** pending  
**Depends on:** WI-002

## Goal

Persist constituents, multi-draft lineage, tags on app/version/constituent, and fingerprint name/category. Migrate existing versions to one constituent + new aggregate graph.

## Deliverables

- [ ] Flyway: `tags` on `sbom_application` and `sbom_application_version` (same type as constituent — G-Q8)
- [ ] Flyway: `sbom_application_sbom` (`id`, `version_id`, `name`, `description`, `tags`, `graph_id`, `sort_order`)
- [ ] Flyway: `based_on_version_id` on `sbom_application_version`; unique `(application_id, version)` where version not null; drop single-draft uniqueness if any
- [ ] Backfill: existing graph stays on first constituent; version gets rebuilt/copied aggregate
- [ ] Existing DRAFT rows with null `version` get a default target (per G-Q1)
- [ ] Flyway: fingerprint `name` + `category` (`approval` \| `history` \| `unknown`); migrate `note` → `name`, category `unknown`; drop `note`
- [ ] JPA records + repositories
- [ ] Migration tests (H2)

## Out of scope

- Rebuild service logic beyond migration helper (WI-004)
- REST (WI-005)

## Acceptance

- Fresh and migrated schemas: 1..\* constituents per version; unique version strings; fingerprint categories constrained
- Demo / existing DBs upgrade cleanly in tests
