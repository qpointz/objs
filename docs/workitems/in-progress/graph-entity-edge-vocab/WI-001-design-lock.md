# WI-001 — Design lock

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design lock  
**Status:** done  
**Depends on:** WI-000  
**Examples:** **docs**

## Goal

Lock the rename map and REST contracts; close every `open` row in [`GAPS.md`](GAPS.md) before code.

## Locked (normative)

### Freeze SQL (Flyway V9) — entity pin only

| From | To |
|------|-----|
| `objs_graph_version_member` | `objs_graph_version_entity` |
| `idx_objs_graph_version_member_entity_id` | `idx_objs_graph_version_entity_entity_id` |

H2 + PostgreSQL. Do not rewrite V1–V6.

**Unchanged:** `objs_graph_edge`, `objs_graph_edge_version`, `objs_graph_version_edge`.

### Kotlin (live M2M + freeze entity pins)

| From | To |
|------|-----|
| `GraphMembershipRecord` / `Id` / `Dao` | `GraphEntitiesRecord` / `Id` / `Dao` |
| Hibernate `BoMGraphMembershipRecord` | `BoMGraphEntitiesRecord` |
| `GraphVersionMemberRecord` / `Id` / `Dao` | `GraphVersionEntityRecord` / `Id` / `Dao` |
| Hibernate `BoMGraphVersionMemberRecord` | `BoMGraphVersionEntityRecord` |
| `applyGraphVersionMembership` | `applyGraphVersionStructure` |

### REST

Graph-scoped only (mirror `/graphs/{id}/entities/{entityId}` style — **not** pool `/entities/*`).

| Method | Path |
|--------|------|
| `POST`/`DELETE` | `/api/v1/objs/graphs/{id}/entities/{entityId}` |
| `POST` | `/api/v1/objs/graphs/{id}/edges` |
| `PUT`/`DELETE` | `/api/v1/objs/graphs/{id}/edges/{edgeId}` |
| `POST` | `/api/v1/objs/graphs/{id}/versions/{version}/apply-structure` |

No aliases for `/members` or `apply-membership`.

**Do not** add `POST`/`PUT`/`DELETE` under `/api/v1/objs/edges`. Existing `/edges/{id}/versions*` / `compact` stay as-is (G-X1).

Edge create/update/delete are thin wrappers over `NamedGraphStore.mutate` MERGE (`edges.set` / `edges.unset`); body for POST/PUT = one mutate `edges.set[]` item; path forces `graphId`.

### Docs split

- WI-007 — consumers  
- WI-005 — prose / REST path tables  
- WI-006 — persistence ER mermaid  

## Acceptance

- [x] Every GAPS row is `resolved`, `deferred`, or `cancelled` (no `open`)
- [x] Path + V9 rename tables locked in this WI or `STORY.md`
- [x] G-1…G-5 locked (incl. `GraphEntities*` Kotlin rename)
- [x] G-X4 remains deferred to C-41 (no freeze-scoped edge redesign in this story)

## Out of scope

- Flyway or controller code (WI-002+)
- C-41 design beyond a one-line deferral
