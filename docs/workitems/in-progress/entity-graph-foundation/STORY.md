# Story: Entity graph foundation

**Slug:** `entity-graph-foundation`  
**Branch:** `entity-graph-foundation` (from `dev`)  
**Status:** in-progress  
**Design:** [`docs/design/graph/`](../../../design/graph/README.md), [`docs/design/core/`](../../../design/core/README.md), [`docs/design/platform/kotlin.md`](../../../design/platform/kotlin.md)  
**Gaps:** [`GAPS.md`](GAPS.md)

## Goal

Foundational **Kotlin** entity store in `objs-core`: `BoEntity` / `BoEdge`, annotations, subgraphs,
central schemas, allow-list, persist gate (batch two-stage), Flyway + JPA (H2 tests), packages
`org.poc.objs`.

**Out of scope:** REST beyond status stub rename; schema catalog persistence (C-3).

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 1 — Coordinates | WI-001 | done | Kotlin + `org.poc.objs` |
| 2 — In-memory domain | WI-002, WI-003 | done | Domain + match-all subgraph |
| 3 — Validation | WI-004 | done | Gate + audit + batch stages |
| 4 — Persistence | WI-005 | done | Flyway, JPA, H2 tests |

## Work Items

- [x] WI-001 — Align packages to `org.poc.objs` + Kotlin (`WI-001-package-rename.md`)
- [x] WI-002 — Core domain types: `BoEntity`, `BoEdge`, central schema `(type, version)` (`WI-002-domain-types.md`)
- [x] WI-003 — Subgraph selection by annotations (`WI-003-subgraph-selection.md`)
- [x] WI-004 — Validation APIs in core (`WI-004-validation.md`)
- [x] WI-005 — PostgreSQL/JPA persistence for entities and edges (`WI-005-persistence.md`)
