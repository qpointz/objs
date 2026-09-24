# WI-002 — Persistence rename

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Persistence  
**Status:** done  
**Depends on:** WI-001  
**Examples:** **—**

## Goal

Rename freeze **entity** pin table/index and align JPA/DAO names (`GraphEntities*`, `GraphVersionEntity*`). No REST changes. No edge table renames.

## Deliverables

- [x] Flyway **V9** (H2 + PostgreSQL): rename `objs_graph_version_member` → `objs_graph_version_entity` + index per WI-001
- [x] Rename live M2M Kotlin: `GraphMembership*` → `GraphEntities*` (`@Table` stays `objs_graph_entity`)
- [x] Update freeze pin `@Table` / records / DAOs: `GraphVersionMember*` → `GraphVersionEntity*`
- [x] Wire renamed beans in `:objs-autoconfigure` if needed
- [x] Fix raw SQL / assertions that reference `objs_graph_version_member`
- [x] Rename `applyGraphVersionMembership` → `applyGraphVersionStructure` in persistence API + tests
- [x] Ripgrep clean for `GraphMembership` / `GraphVersionMember` in production sources

## Acceptance

- [x] Fresh migrate creates `objs_graph_version_entity` only (no `_member` pin table)
- [x] Edge tables still named `objs_graph_edge_version` / `objs_graph_version_edge`
- [x] `./gradlew :objs-persistence:test :objs-autoconfigure:test`
- [x] Ripgrep clean for `objs_graph_version_member` in production sources/tests (docs wait for WI-005/006)

## Out of scope

- REST path changes (WI-003 / WI-004)
- Living design docs / ER (WI-005 / WI-006)
- Renaming edge history or pin tables
- Freeze-scoped edge redesign (C-41)
