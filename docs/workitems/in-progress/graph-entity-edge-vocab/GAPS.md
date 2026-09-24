# Gaps — graph-entity-edge-vocab (C-40)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**WI-001 must close every `open` row** (resolve or defer) before persistence/REST code.  
**Status:** no `open` rows — design locked for implementation (confirm in WI-001 commit).

---

## Open (design)

_(none — WI-001 may still tighten wording)_

---

## Provisional (likely keep)

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-1 | Edge `POST` | **resolved** | **Only** under `/graphs/{id}/edges` (create in that graph). Body = one mutate `edges.set[]` item; force `graphId` from path. **Do not** add pool-style `POST /edges` mirroring `/entities`. |
| G-2 | Edge `PUT` | **resolved** | **Only** `PUT /graphs/{id}/edges/{edgeId}`. Same fields as mutate set for one edge; reject path/body `graphId` mismatch. **Do not** add `PUT /edges/{id}`. |
| G-3 | Edge `DELETE` | **resolved** | **Only** `DELETE /graphs/{id}/edges/{edgeId}` — same as mutate `edges.unset` (drop live edge). **Do not** add `DELETE /edges/{id}`. |
| G-4 | Attach / entity existence | **resolved** | As close as possible to **GraphMutation** MERGE on the named graph: `POST …/entities/{entityId}` ≡ today’s `attach` (membership for existing pool id); `DELETE …/entities/{entityId}` ≡ `detach` / `entities.unset` (idempotent detach + drop incident edges). Same fail-closed / codes as existing store — no new attach semantics. |
| G-5 | Kotlin live M2M types | **resolved** | `GraphMembership*` → **`GraphEntities*`** (`Record` / `Id` / `Dao`; Hibernate `BoMGraphEntitiesRecord`). Entities-only M2M (`objs_graph_entity`); not edges. Prefer over `EntityLink*`. Pool type stays `EntityRecord`. |
| G-6 | Vocabulary | **resolved** | **entity** / **edge** only; reject **member** for SQL freeze pin + REST paths |
| G-7 | Freeze entity pin SQL | **resolved** | `objs_graph_version_member` → `objs_graph_version_entity`; index rename in Flyway **V9** |
| G-8 | Live SQL | **resolved** | `objs_graph_entity` unchanged |
| G-9 | Edge table names | **resolved** | Leave `objs_graph_edge` / `objs_graph_edge_version` / `objs_graph_version_edge` as-is (`objs_edge_version` rejected: no live `objs_edge`) |
| G-10 | REST entity paths | **resolved** | `/graphs/{id}/members/{entityId}` → `/graphs/{id}/entities/{entityId}` |
| G-11 | REST edge paths | **resolved** | Graph-scoped `POST/PUT/DELETE /graphs/{id}/edges…` only (G-1…G-3); thin MERGE wrappers. Mirror graph `/entities/{entityId}` scope — **not** pool `/entities/*` |
| G-12 | Apply freeze op | **resolved** | `apply-membership` → `apply-structure`; Kotlin `applyGraphVersionStructure` |
| G-13 | Breaking aliases | **resolved** | No soft aliases (pre-1.0) |
| G-14 | Domain asymmetry | **resolved** | Entities: pool then attach. Edges: graph-owned create/update/delete (no edge pool) |
| G-15 | Layer | **resolved** | Persistence rename in `:objs-persistence`; REST in `:objs-service`; workbench only for `apply-structure` caller |
| G-16 | Examples / UI | **resolved** | **WI-007** reviews workbench + SBOM + AR and adjusts if needed; no forced migrate to graph edge REST |
| G-17 | ER diagrams | **resolved** | Dedicated **WI-006** after prose docs (WI-005) |

---

## Out of story

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | Top-level `/edges/{id}` history/compact | **deferred** | Keep existing version/compact routes only. **No** pool-style edge CRUD mirroring `/entities/*` (G-1…G-3). |
| G-X2 | — | **cancelled** | Superseded by G-5 (`GraphEntities*` in this story) |
| G-X3 | C-20 store text search | **cancelled** | Separate story |
| G-X4 | Freeze-scoped edge history | **deferred** | [**C-41**](../../BACKLOG.md) backlog — collapse pin + `objs_graph_edge_version` into graph-version children; **not** pursuing in C-40; not a storage/perf project; no payload COW |
