# WI-006 — Design docs

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Composer + docs  
**Status:** done  
**Depends on:** WI-005  
**Modules:** docs only (no `:objs-sbom-example` code — G-S10 = no)

## Goal

Bring design docs in line with shipped soft-link subgraphs + snapshot. Keep dynamic annotation selection documented as the ephemeral path.

## Files to update

| Doc | Content |
|-----|---------|
| `docs/design/graph/annotations-and-subgraphs.md` | Soft-link subgraphs vs annotation/`ids` selection; matcher `subgraph`; stored edges vs induce; liveness; snapshot |
| `docs/design/graph/persistence.md` | Tables `bom_subgraph`, `bom_subgraph_entities`, `bom_subgraph_edges`; cascade behaviour |
| `docs/design/service/rest-api.md` | `/graph/subgraphs` CRUD + snapshot; query matcher example |
| `docs/design/ui.md` | Composer save / open / snapshot |
| Optional `docs/design/sbom/example.md` | Light cross-link only if useful; **no** demo seed (G-S10 = no) |

## Decisions to close in this WI

- **G-S10** / **G-S11** — already resolved (no SBOM seed; free-form annotations, app-level vocabulary). Document that in design docs.

## Out of scope

- Story closure / archive / C-12 `done` (explicit user request only)
- Implementing Gremlin flatten/nest docs beyond “deferred”

## Implementation checklist

- [x] Design docs updated
- [x] G-S10 remains **resolved (no)** in GAPS; no example module changes
- [x] STORY `[x]`; commit; push
- [x] Leave story **in-progress** until user asks to close

## Acceptance

- [x] Design docs match shipped behaviour
- [x] REST + matcher + snapshot examples present
- [x] Story remains open (not archived)

## Commit message hint

`[docs] Document soft-link subgraphs and snapshot (WI-006)`
