# WI-002 — SPA routing filters

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete

## Goal

Browser refresh of client routes under `/app` and `/workbench` must return `index.html`, including paths with version segments (`1.0.0`).

## Deliverables

- [x] `SpaRoutingFilter` + `WorkbenchSpaRoutingFilter` (`:objs-service`)
- [x] `DomainSpaRoutingFilter` (`:asset-repository-service`)
- [x] Resource resolvers treat letter-start extensions as static files, not semver
- [x] `SpaRoutingFilterTest`

## Acceptance

- `GET /workbench/explorer` and `GET /app/collections/:id` forward to the packaged index
- `GET /workbench/assets/*.js` is not forwarded
