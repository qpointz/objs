# WI-004 — Ephemeral Combined SBOM + domain services

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Persistence + domain  
**Status:** done  
**Depends on:** WI-003

## Goal

Implement BOM CRUD, ephemeral Combined SBOM union, multi-draft create/promote/delete, and fingerprint snapshots. Remove single-draft guards.

## Deliverables

- [x] Union helper (membership + edges; collapse duplicates) — **not persisted** on the version
- [x] `combinedTags` helper: App+Ver+all BOMs of the open version
- [x] BOM create/update metadata/save graph/delete (forbid last)
- [x] Delete DRAFT version (cascade BOMs + fingerprints of that draft); if other drafts are based on it or its fingerprints, delete those dependents too after confirm (G-Q12); forbid delete RELEASED
- [x] Application create sets bootstrap DRAFT `version` = required `targetVersion`; one empty BOM named **BOM** (hidden at count = 1)
- [x] `createDraft(targetVersion, fromVersionId | fromFingerprintId, combineConstituents?)` — version source: keep split or flatten; fingerprint source: always one BOM from fingerprint graph; set based-on FK; copy version tags from based-on version (fingerprint’s parent version)
- [x] `fingerprint(name, category)` materializes **full union snapshot**; validate category
- [x] PATCH DRAFT `version` (rename target); uniqueness CONFLICT; RELEASED version immutable
- [x] `promote(version)` — required re-typed version string; may differ from draft target; uniqueness check; then RELEASED
- [x] `fun interface VersionComparer` — strict SemVer 2.0 compare + `toSerial(String): Numeric`; write `version_serial` on every version-string change; `latestReleased` = max serial among RELEASED; MI / depends-on / CDX-of-latest / `GET …/latest` all use that (G-Q11); drafts never included
- [x] Remove `draft(appId).firstOrNull()` single-draft API; fix callers (seeder, CDX, portfolio resolver, etc.)
- [x] Unit/integration tests (parallel drafts; flatten vs keep; fingerprint not copying BOM rows)

## Out of scope

- REST controllers (WI-005)
- UI (WI-006)

## Acceptance

- Parallel drafts with different targets work
- Flatten vs keep-split matches G-P7
- Fingerprint stores a snapshot graph only
- Saving a BOM does **not** write a version Combined graph
- Combined SBOM / multi-select unions are read-only
