# WI-001 — Align SPA URL and classpath prefixes

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Align  
**Status:** complete  
**Depends on:** WI-000

## Goal

Ship the locked convention: HTTP prefix = `static/<same-name>/` for workbench, asset repository, and SBOM.

## Deliverables

- [x] Workbench classpath `static/ui` → `static/workbench` (URL stays `/workbench/`)
- [x] Asset repository public URL `/app` → `/ar` (classpath stays `static/ar`)
- [x] SBOM public URL `/ui` → `/sbom` (classpath stays `static/sbom`)
- [x] SPA filters, resource handlers, Vite `base` / router `basename`
- [x] `SpaRoutingFilter.normalizeBasePath` blank fallback is `/workbench`
- [x] Tests updated; `:objs-service:test`, `:sbom-service:test`, `:asset-repository-service:test`

## Out of scope

- Living docs (WI-002)
- Extracting the SBOM filter copy

## Acceptance

- Deep-link refresh works under `/workbench/**`, `/ar/**`, `/sbom/**`
- Skip-UI stubs live at the matching classpath folder
- G-1 / G-2 resolved in `GAPS.md`
