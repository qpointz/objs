# Story: OpenAPI tags, completeness, and client codegen

**Slug:** `openapi-tags`  
**Branch:** `openapi-tags`  
**Status:** completed  
**Folder:** [`docs/workitems/completed/20260924-openapi-tags/`](.)  
**Backlog:** [C-42](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Depends on:** — (follows [D-9](../../completed/20260923-sbom-ar-openapi/STORY.md) domain OpenAPI; foundation polish was OOS there)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md) · SBOM/AR `example.md`  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)  
**Closed:** 2026-09-24

## Goal

Make **Swagger / OpenAPI the comprehensive API source** for in-scope surfaces:

1. **Scannable** — tags + springdoc groups so UI is not mega-lists  
2. **Complete** — operations document summaries, params, bodies, success/error schemas well enough to use without reading controller source  
3. **Client-checkable** — smoke that the published specs generate usable **Java** and **Python** clients (manual against localhost is fine)

### In scope (controllers)

| API | Controllers / modules |
|-----|------------------------|
| **`/api/v1/objs/**`** | All foundation REST on this prefix — `:objs-service`, `:objs-policy-service`, `:objs-gremlin-service`, `:objs-jgrapht-service` |
| **SBOM domain** | `:sbom-service` inventory controllers |
| **AR domain** | `:asset-repository-service` |

When SBOM/AR runners also expose `/api/v1/objs/**`, the same objs controllers/tags/docs apply.

| Surface | Pain today (approx.) |
|---------|----------------------|
| Workbench / objs | Groups only `graph` + `registry`; fat tags; docs invent `traverse` group; foundation OpenAPI depth uneven vs D-9 domain pass |
| SBOM | Fat `inventory` tag; D-9 annotated but re-check for gaps |
| AR | Single mega-tag; D-9 annotated but re-check for gaps |

**Not** path renames. **Not** shipping generated clients as a product artifact (smoke + recipe only).

## Normative (see WI-001 / GAPS)

Locked in [`GAPS.md`](GAPS.md) G-0…G-10. Tag/group matrix in [`WI-001-design-lock.md`](WI-001-design-lock.md).

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Moved to in-progress |
| 1 — Design lock | WI-001 | done | Tags/groups + completeness + codegen; GAPS closed |
| 2 — Tags/groups | WI-002 | done | `@Tag` + `GroupedOpenApi` |
| 3 — Completeness | WI-003 | done | Annotation / schema gaps |
| 4 — Codegen smoke | WI-004 | done | Harness + Java/Python compile |
| 5 — Living docs | WI-005 | done | Design / README truth |

## Work Items

- [x] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock: tags, completeness bar, codegen — examples: **docs** (`WI-001-design-lock.md`)
- [x] WI-002 — Apply tags and springdoc groups — examples: **objs + SBOM + AR** (`WI-002-apply-tags.md`)
- [x] WI-003 — OpenAPI completeness pass — examples: **objs + SBOM + AR** (`WI-003-openapi-completeness.md`)
- [x] WI-004 — Java + Python client codegen smoke — examples: **docs / scripts** (`WI-004-client-codegen-smoke.md`)
- [x] WI-005 — Living docs — examples: **docs** (`WI-005-living-docs.md`)

## Out of scope

- Controllers **outside** `/api/v1/objs/**`, SBOM domain, and AR domain
- Changing REST paths or DTOs (except annotation-only / OpenAPI model tweaks needed for honest schemas)
- Publishing generated clients as a maintained library/release artifact
- Full UI e2e of Swagger
- Store text search (C-20)
- Story closure / archive (user must ask)

## Acceptance (after implementation)

- [x] Swagger UI shows coherent tags/groups (no single mega-list for the fattest areas)
- [x] In-scope ops meet the locked completeness bar; Swagger usable as primary API reference
- [x] Documented recipe + harness: Java and Python clients compile from localhost OpenAPI
- [x] Living docs match real group/tag names and point at the codegen smoke recipe
- [x] `./gradlew :objs-service:test :objs-policy-service:test` (OpenAPI config tests); SBOM/AR annotation pass covered in WI-002/003
