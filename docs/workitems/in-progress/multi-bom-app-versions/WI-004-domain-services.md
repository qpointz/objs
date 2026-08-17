# WI-004 — Aggregate rebuild + domain services

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Persistence + domain  
**Status:** pending  
**Depends on:** WI-003

## Goal

Implement constituent CRUD, aggregate rebuild, and multi-draft create/promote in domain services. Remove single-draft guards.

## Deliverables

- [ ] Aggregate rebuild (union membership + edges; collapse duplicates)
- [ ] Constituent create/update metadata/save BOM/delete (forbid last)
- [ ] `createDraft(targetVersion, fromVersionId)` — deep copy per G-Q5; set `version` + `based_on_version_id`
- [ ] `promote` uses draft target; uniqueness checks
- [ ] `latest` / `latestReleased` by **semver**
- [ ] Remove `draft(appId).firstOrNull()` single-draft API; fix callers (seeder, CDX, portfolio resolver, etc.)
- [ ] Unit/integration tests

## Out of scope

- REST controllers (WI-005)
- UI (WI-006)

## Acceptance

- Parallel drafts with different targets work
- Saving a constituent updates the version aggregate
- Writes to aggregate graph rejected / not exposed as mutable
