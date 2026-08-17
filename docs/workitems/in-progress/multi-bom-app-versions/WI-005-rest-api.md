# WI-005 — REST + OpenAPI

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — API  
**Status:** pending  
**Depends on:** WI-004

## Goal

Expose constituent and multi-draft APIs on the inventory domain REST surface; keep product language; update OpenAPI.

## Deliverables

- [ ] `GET/POST .../versions/{id}/sboms`; `GET/PUT/PATCH/DELETE .../sboms/{sbomId}`
- [ ] Version BOM GET = aggregate (read-only); PUT aggregate → 405
- [ ] Create draft body: `targetVersion`, `fromVersionId`; summaries include `basedOnVersionId` / based-on label
- [ ] List applications enriched: `latestVersion`, `versionCount`, `constituentCount` (and `draftCount` if locked)
- [ ] OpenAPI / springdoc updates
- [ ] Controller/MockMvc tests

## Out of scope

- SPA (WI-006)
- Demo seeder (WI-007)

## Acceptance

- OpenAPI documents new routes; aggregate writes fail clearly
- List apps returns portal fields without N+1 in normal use
