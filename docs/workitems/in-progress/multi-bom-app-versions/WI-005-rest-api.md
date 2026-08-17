# WI-005 — REST + OpenAPI

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — API  
**Status:** pending  
**Depends on:** WI-004

## Goal

Expose constituent, multi-draft, tags, and fingerprint APIs on the inventory domain REST surface; keep product language; update OpenAPI.

## Deliverables

- [ ] `tags` on application, version, and constituent payloads; version/BOM view `combinedTags` = App+Ver+SBOMs unique union
- [ ] `GET/POST .../versions/{id}/sboms`; `GET/PUT/PATCH/DELETE .../sboms/{sbomId}`
- [ ] Version BOM GET = aggregate (read-only); PUT aggregate → 405
- [ ] Create draft: `targetVersion`, `fromVersionId`, `combineConstituents`; summaries include `basedOnVersionId`
- [ ] Create fingerprint: required `name` + `category` (`approval` \| `history` \| `unknown`); summaries drop `note`
- [ ] List applications enriched: `latestVersion`, `versionCount`, `constituentCount` (and `draftCount` if locked)
- [ ] OpenAPI / springdoc updates
- [ ] Controller/MockMvc tests

## Out of scope

- SPA (WI-006)
- Demo seeder (WI-007)

## Acceptance

- OpenAPI documents new routes; aggregate writes fail clearly; fingerprint category rejected if not in enum
- List apps returns portal fields without N+1 in normal use
