# WI-007 — Consumer review: workbench, SBOM, AR

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Consumers  
**Status:** done  
**Depends on:** WI-004 (REST + persistence renames landed)  
**Examples:** **workbench + SBOM + AR**

## Goal

Check workbench, SBOM, and asset-repository for breakage or stale **member** vocabulary after C-40 renames; adjust only where needed.

## Deliverables

- [x] Ripgrep / review: `/members`, `apply-membership`, `GraphMembership`, `GraphVersionMember`, `objs_graph_version_member` under workbench, SBOM, AR
- [x] Workbench: rename `applyGraphVersionMembership` → `applyGraphVersionStructure` (path already apply-structure); OpenGraphModal comment
- [x] SBOM: no REST path / type renames needed (uses `attach`/`detach`); compile + test OK
- [x] AR: no references; compile + test OK
- [x] Fix only what is broken or user-visible stale paths

## Acceptance

- [x] No remaining production references to old REST paths / Kotlin types in those modules
- [x] `./gradlew :objs-service-ui:build :sbom-service:test :asset-repository-service:test`

## Out of scope

- Rewriting examples to use new graph-scoped edge REST (persistence mutate remains fine)
- Living foundation design docs (WI-005 / WI-006)
