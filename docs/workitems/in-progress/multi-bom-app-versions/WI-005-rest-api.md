# WI-005 — REST + OpenAPI

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — API  
**Status:** pending  
**Depends on:** WI-004

## Goal

Expose constituent, multi-draft, tags, and fingerprint APIs on the inventory domain REST surface; keep product language; update OpenAPI.

## Deliverables

- [ ] Create application: required `targetVersion`; bootstrap DRAFT uses it
- [ ] `tags` on application, version, and BOM payloads; Combined SBOM / version view `combinedTags` = App+Ver+BOMs unique union
- [ ] `GET/POST .../versions/{id}/sboms`; `GET/PUT/PATCH/DELETE .../sboms/{sbomId}` (DELETE forbid last)
- [ ] `DELETE .../versions/{id}` DRAFT only; cascade BOMs, fingerprints, and **dependent drafts** (G-Q12); preview dependents for the confirm dialog
- [ ] Version Combined SBOM GET = ephemeral union (read-only); PUT Combined → 405
- [ ] Create draft: `targetVersion`, `fromVersionId` **or** `fromFingerprintId`, `combineConstituents`; summaries include based-on version or fingerprint
- [ ] PATCH DRAFT metadata including `version` (rename target); uniqueness CONFLICT
- [ ] Promote: required `version` in body (confirmation / override); uniqueness CONFLICT
- [ ] Create fingerprint: required `name` + `category` (`approval` \| `history` \| `unknown`); summaries drop `note`
- [ ] Application portal stats: **per-app** `GET` (lazy): `versionCount` (all versions), `bomCount` (BOMs across all versions), `latestVersion` + `latestMultiBom` (latest RELEASED has ≥ 2 BOMs). List applications stays thin (G-Q14)
- [ ] OpenAPI / springdoc updates
- [ ] Controller/MockMvc tests

## Out of scope

- SPA (WI-006)
- Demo seeder (WI-007)

## Acceptance

- OpenAPI documents new routes; aggregate writes fail clearly; fingerprint category rejected if not in enum
- Portal stats are per-app (list endpoint not required to join all counts)
