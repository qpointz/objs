# Story: SBOM typed example

**Slug:** `sbom-typed-example`  
**Branch:** `sbom-typed-example`  
**Status:** closed (2026-07-28)  
**Backlog:** D-1  
**Design:** [`docs/design/sbom/example.md`](../../../design/sbom/example.md), [`docs/design/sbom/canonical-spec.md`](../../../design/sbom/canonical-spec.md), [`docs/design/graph/typed-domain.md`](../../../design/graph/typed-domain.md)  
**Gaps:** [`GAPS.md`](GAPS.md)

## Goal

**Layering:** the graph foundation (`objs-core` / `objs-service`) exposes **low-level** primitives — `BoMEntity`, `BoMEdge`, catalogs, `BoMGraphStore`, generic `/graph` + `/registry`. This story builds a **concrete application** on top: Software BOM for many apps/versions, typed from the **Canonical Software Graph** draft.

- Reusable typed-domain toolkit in `objs-core` (`org.poc.objs.core.typed`)
- Example module `objs-sbom-example`: concrete app — full canonical ontology (Waves A–D), annotations, `SbomService`, `/api/v1/example/sbom` REST
- Durable data only in the foundation store

**Out of scope:** C-3; auth; importer merge-by-identity; prune-on-PUT; SBOM DTO ≠ `BoMGraph`.

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 1 — Module shell | WI-001 | done | |
| 2 — Typed toolkit | WI-002 | done | |
| 3 — Component + service | WI-003 | done | |
| 4 — SBOM REST | WI-004 | done | |
| 5 — Wave A types | WI-005 | done | |
| 6 — Docs + seed | WI-006 | done | |
| 7 — Full ontology | (G-S39) | done | Waves B–D types + all 28 edge rules |
| 8 — Python CRUD script | WI-007 | done | Random graphs + create/get/update/delete |
| 9 — List apps/versions | WI-008 | done | `GET /api/v1/example/sbom/apps` |
| 10 — Graph explorer SPA | WI-009 | done | `/ui/` — foundation graph by annotations |

## Work Items

- [x] WI-001 — Module shell `objs-sbom-example` (`WI-001-module-shell.md`)
- [x] WI-002 — Core typed-domain toolkit (`WI-002-typed-toolkit.md`)
- [x] WI-003 — Component, annotations, `SbomService` (`WI-003-component-sbom-fetch.md`)
- [x] WI-004 — SBOM REST API (`WI-004-sbom-rest.md`)
- [x] WI-005 — Canonical Wave A types + edges (`WI-005-additional-types.md`)
- [x] WI-006 — Design docs, effort notes, optional app seed (`WI-006-docs-and-seed.md`)
- [x] WI-007 — Python SBOM REST CRUD / random graphs (`WI-007-python-sbom-crud.md`)
- [x] WI-008 — List applications and versions API (`WI-008-list-apps-versions.md`)
- [x] WI-009 — Graph explorer SPA (`WI-009-graph-explorer-spa.md`)
