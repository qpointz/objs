# WI-007 — Living docs and export

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 7 — Docs  
**Status:** done  
**Depends on:** WI-006 (done)  
**Examples:** **docs**

## Goal

Document the split and update tooling so export and AGENTS reflect the new module graph.

## Scope

- [x] [`docs/design/core/README.md`](../../../design/core/README.md) — `:objs-core` vs `:objs-autoconfigure`
- [x] [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md) — Flyway / autoconfig ownership
- [x] [`AGENTS.md`](../../../../AGENTS.md) — module list includes `:objs-autoconfigure`
- [x] [`scripts/export/generate-config.py`](../../../../scripts/export/generate-config.py) + fixture per WI-001 export naming (G-A6)
- [x] [`docs/design/graph/README.md`](../../../design/graph/README.md) module table if it lists objs-core alone

## Out of scope

- Story closure / MILESTONE § Completed (user-requested closure only)
- Public user docs under `docs/public/` unless REST-facing behaviour text references objs-core as Boot entry

## Acceptance

- Docs match shipped Gradle graph
- `./gradlew :objs-service:test :sbom-service:test :asset-repository-service:test`
