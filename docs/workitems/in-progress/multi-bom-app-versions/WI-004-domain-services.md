# WI-004 — Aggregate rebuild + domain services

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Persistence + domain  
**Status:** pending  
**Depends on:** WI-003

## Goal

Implement constituent CRUD, aggregate rebuild, multi-draft create/promote, and fingerprint-always-Combined. Remove single-draft guards.

## Deliverables

- [ ] Aggregate rebuild (union membership + edges; collapse duplicates)
- [ ] `combinedTags` helper: App+Ver+SBOMs unique union
- [ ] Constituent create/update metadata/save BOM/delete (forbid last)
- [ ] `createDraft(targetVersion, fromVersionId, combineConstituents?)` — keep split or flatten to one constituent from aggregate; set `version` + `based_on_version_id`; copy version tags from based-on
- [ ] `fingerprint(name, category)` copies **aggregate only**; validate category
- [ ] `promote` uses draft target; uniqueness checks
- [ ] `latest` / `latestReleased` by **semver**
- [ ] Remove `draft(appId).firstOrNull()` single-draft API; fix callers (seeder, CDX, portfolio resolver, etc.)
- [ ] Unit/integration tests (parallel drafts; flatten vs keep; fingerprint not copying constituents)

## Out of scope

- REST controllers (WI-005)
- UI (WI-006)

## Acceptance

- Parallel drafts with different targets work
- Flatten vs keep-split matches G-P7
- Fingerprint copies aggregate only
- Saving a constituent updates the version aggregate
- Writes to aggregate graph rejected / not exposed as mutable
