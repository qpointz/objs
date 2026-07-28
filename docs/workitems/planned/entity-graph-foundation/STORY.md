# Story: Entity graph foundation

**Slug:** `entity-graph-foundation`  
**Branch:** `entity-graph-foundation` (from `dev` / `origin/dev` when remote exists)  
**Status:** planned  
**Design:** [`docs/design/graph/`](../../../design/graph/README.md)  
**Gaps:** [`GAPS.md`](GAPS.md) — decisions locked for G-1–G-12, G-19, G-20; see file for half-open / deferred

## Goal

Establish the **foundational core** of the entity store in `objs-core`:

- Domain: `BoEntity`, `BoEdge`, annotations, subgraph selection  
- Central in-memory schema catalog `(type, version)`  
- Allowed-edge allow-list with properties policy (bare vs schema)  
- Persist gate (create/update/delete; **no id → create**, **id → update**)  
- Batch subgraph writes with **two-stage** validation (entities, then edges vs payload∪store)  
- PostgreSQL + **Flyway**; tests on **H2**  
- Packages **`org.poc.objs`**

**Out of scope:** REST beyond status stub move with rename; UI; security; runnable app; persisting schema catalog to PostgreSQL (C-3).

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 1 — Coordinates | WI-001 | yes | Package/group → `org.poc.objs` |
| 2 — In-memory domain | WI-002, WI-003 | after WI-001 | Types + match-all subgraph |
| 3 — Validation | WI-004 | after WI-002 | Gate + audit + batch stages |
| 4 — Persistence | WI-005 | after WI-003, WI-004 | Flyway, JPA, H2 tests |

## Work Items

- [ ] WI-001 — Align packages to `org.poc.objs` (`WI-001-package-rename.md`)
- [ ] WI-002 — Core domain types: `BoEntity`, `BoEdge`, central schema `(type, version)` (`WI-002-domain-types.md`)
- [ ] WI-003 — Subgraph selection by annotations (`WI-003-subgraph-selection.md`)
- [ ] WI-004 — Validation APIs in core (`WI-004-validation.md`)
- [ ] WI-005 — PostgreSQL/JPA persistence for entities and edges (`WI-005-persistence.md`)
