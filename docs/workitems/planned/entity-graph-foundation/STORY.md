# Story: Entity graph foundation

**Slug:** `entity-graph-foundation`  
**Branch:** `entity-graph-foundation` (from `origin/dev`)  
**Status:** planned  
**Design:** [`docs/design/graph/`](../../../design/graph/README.md)

## Goal

Establish the **foundational core** of the entity store in `objs-core`: domain types for
**entities**, **edges**, **annotations**, and **subgraphs**; validation at the persistence
boundary; PostgreSQL/JPA persistence with generic columns and JSONB. Align packages to
`org.poc.objs`.

**Out of scope:** REST API expansion in `objs-service` (status stub may only move with package
rename). No UI, security, or runnable app.

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 1 — Coordinates | WI-001 | yes | Package/group rename |
| 2 — In-memory domain | WI-002, WI-003 | after WI-001 | Entity/edge/annotation + subgraph select |
| 3 — Validation | WI-004 | after WI-002 | Persist-gate + audit APIs in core |
| 4 — Persistence | WI-005 | after WI-003, WI-004 | PostgreSQL/JPA; no REST |

## Work Items

- [ ] WI-001 — Align packages to `org.poc.objs` (`WI-001-package-rename.md`)
- [ ] WI-002 — Core domain types: Entity, EntityType, Relation, Annotation (`WI-002-domain-types.md`)
- [ ] WI-003 — Subgraph selection by annotations (`WI-003-subgraph-selection.md`)
- [ ] WI-004 — Validation APIs in core (`WI-004-validation.md`)
- [ ] WI-005 — PostgreSQL/JPA persistence for entities and edges (`WI-005-persistence.md`)
