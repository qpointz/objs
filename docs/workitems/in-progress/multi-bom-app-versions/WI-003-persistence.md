# WI-003 — Persistence (Flyway + records)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Persistence + domain  
**Status:** pending  
**Depends on:** WI-002

## Goal

Persist BOMs, multi-draft lineage, tags on app/version/BOM, and fingerprint name/category. Migrate existing versions to one BOM named `BOM`; **do not** persist Combined SBOM on the version.

## Deliverables

- [ ] Flyway: `tags` on `sbom_application`, `sbom_application_version`, and `sbom_application_sbom` — Postgres `TEXT[]`, H2 `VARCHAR ARRAY` (G-Q8); JPA `String[]` + `SqlTypes.ARRAY` (no `columnDefinition = "text[]"`)
- [ ] Flyway: `sbom_application_sbom` (`id`, `version_id`, `name`, `description`, `tags`, `graph_id`, `sort_order`); **unique `(version_id, name)`** (G-Q9)
- [ ] Flyway: `based_on_version_id` and `based_on_fingerprint_id` on `sbom_application_version` (exactly one null for bootstrap drafts; subsequent drafts one of the two set); **ON DELETE CASCADE** (G-Q12); unique `(application_id, version)` where version not null; drop single-draft uniqueness if any
- [ ] Backfill: existing `version.graph_id` becomes the first BOM (`name=BOM`); **drop** `sbom_application_version.graph_id` (Combined is ephemeral)
- [ ] Existing DRAFT rows with null `version` get a migration default (e.g. `0.1.0`) — new apps never omit target (G-Q1)
- [ ] Flyway: `version_serial NUMERIC` on `sbom_application_version`; backfill from `version` (G-Q7); index for latest RELEASED
- [ ] Flyway: fingerprint `name` + `category` (`approval` \| `history` \| `unknown`); migrate `note` → `name`, category `unknown`; drop `note`
- [ ] JPA records + repositories
- [ ] Migration tests (H2)

## Out of scope

- Rebuild / persist Combined on the version (forbidden — G-A2)
- REST (WI-005)

## Acceptance

- Fresh and migrated schemas: 1..\* constituents per version; unique version strings; fingerprint categories constrained
- Demo / existing DBs upgrade cleanly in tests
