# Gaps — versions-and-snapshots (C-18)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**WI-001 design locked.** Implementation starts at WI-002.  
Normative schema: [`ER.md`](ER.md).

---

## Architecture

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-A1 | Mechanism | **resolved** | HEAD tables + `*_version` history. Freeze = **deep graph version**, not a second snapshot graph, not pool flags |
| G-A2 | Identity vs version | **resolved** | Stable UUID id. HEAD **content** stays on `bom_entity` / `bom_graph` / `bom_graph_edge`. Version key = `(parent_id, version BIGINT)` growing `max(epochMillis, prev+1)`. **`version` is not unique**; only the pair is. Pin that pair. HEAD is the HEAD row, not `max(version)` |
| G-A3 | Persist default | **resolved** | **Same as today:** in-place HEAD, no version row. `head_version` NULL until capture |
| G-A4 | Live GET | **resolved** | HEAD tables only. No `*_version` join. GIN stays on HEAD |
| G-A5 | Freeze API | **resolved** | `createDeepGraphVersion` on the **same** `graph_id`. Pins member/edge versions. Reconstruct is slower OK. Replaces the “new SNAPSHOT graph” idea. Does **not** replace C-12 `clone()` |
| G-A6 | Edges | **resolved** | Versioned like entities. Live edges = HEAD only. Deep freeze pins `(edge_id, edge_version)` |
| G-A7 | `copyGraph` / `mergeGraph` | **resolved** | Unchanged live share. Draft-from-fingerprint = copyGraph of **live HEAD**, not restore of freeze |
| G-A8 | Provenance | **resolved** | Pins keep **original** `entity_id` / `edge_id` + version. No new pool ids |
| G-A9 | Pool / catalog | **resolved** | Live catalog = identities at HEAD (HEAD columns) |
| G-A10 | Version immutability | **resolved** | Version rows never `UPDATE`d |
| G-A11 | C-17 split | **resolved** | Lookups / copy / merge stay C-17. This story is versions + deep freeze |
| G-A12 | AR | **resolved** | No freeze product here; collections stay live HEAD |
| G-A13 | History growth | **accepted-risk** | GC later (G-X4) |
| G-A14 | When versions are written | **resolved** | Default persist writes none. Capture copies **current** HEAD at freeze time |
| G-A15 | Audit clocks | **resolved** | `created_at` / `updated_at` on every `bom_*` (WI-002 V3; version rows at V4). Pulled from C-19 WI-002 |
| G-A16 | Delete HEAD | **resolved** | Physical `DELETE` HEAD keeps `*_version` and deep pins. Reconstruct still works |
| G-A17 | Graph header history | **resolved** | C-18: no auto header version. Header is captured as part of deep freeze (`graph_annotations`) |
| G-A18 | H2 / DIY SQL | **accepted-risk** | H2 demo-only. Direct `*_version` or HEAD bypass: **unsupported — at your own risk** ([`ER.md`](ER.md) § Design limitations) |
| G-A19 | HEAD→version FK | **resolved** | `head_version` **nullable**. When set, `(id, head_version) REFERENCES *_version(parent_id, version)`. No FK from version parent to HEAD. Versions may exist without HEAD |
| G-A20 | Versioning strategy | **resolved** (SPI) | `BomVersioningStrategy.shouldCapture(ctx)`. C-18 ships **`ExplicitOnly`** only. `createDeepGraphVersion` **always** captures |
| G-A21 | Shared nodes / which graph’s policy | **resolved** (future) | **Write-context graph is golden** (`ctx.graphId`). Pool write → store default. Not union of all membership graphs. Avoids extra versions |
| G-A22 | `head_version` vs live payload | **resolved** | `head_version` = last **capture**, not “equals HEAD bytes.” After in-place edits, content may diverge until next capture. Live GET reads HEAD columns |
| G-A23 | Snapshot UX | **resolved** | Composer **Snapshot** = `createDeepGraphVersion` (same graph, milestone). Composer **Clone** = `clone()` (new graph, new ids). Fingerprint freeze = Snapshot, not clone |
| G-A24 | Mermaid `obj_type` | **cancelled** | Diagram-only alias; SQL column stays `type` |
| G-A25 | Combined SBOM freeze source | **resolved** | WI-006: persist Combined union as a live graph if needed, then `createDeepGraphVersion`; or capture-from-contents helper. Fingerprint stores `(graph_id, graph_version)` |
| G-A26 | Gremlin | **resolved** | Live: store HEAD subgraph as today. Deep version: reconstruct contents then eval (no version-aware Gremlin in C-18) |
| G-A27 | Seeds | **resolved** | Seed persist uses default strategy (no auto-version). Same as today until an explicit freeze |
| G-A28 | First freeze | **resolved** | If `head_version` is NULL, capture inserts the first `*_version` rows from current HEAD |
| G-A29 | Dual-write | **resolved** | Ordinary persist = one HEAD write. Capture = version insert + HEAD `head_version` in **one TX**. Not two independent commits |
| G-A30 | Compat / migration | **resolved** | **None.** Greenfield recreate DB. No history backfill. Keep `POST /graphs/{id}/clone` (not an alias for freeze) |
| G-A31 | As-built schema doc | **resolved** | WI-007 writes [`docs/design/graph/database-model.md`](../../../design/graph/database-model.md) from shipped Flyway/JPA |
| G-A32 | Explorer time travel | **resolved** | C-18: right pane, versions newest first, click reconstructs, **Latest** = HEAD. Slider/from–to is G-D9 |
| G-A33 | clone vs Snapshot | **resolved** | `clone()` stays C-12 deep copy: new graph + new entity/edge ids from **current HEAD only**. Does **not** copy source `*_version`. New rows start `head_version` NULL. Later Snapshot on the clone is that graph’s own history line |

---

## Deferred (not C-18)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-D1 | `OnWriteVersioningStrategy` | **deferred** | Capture on create/update; swap SPI bean |
| G-D2 | `PerGraphVersioningStrategy` | **deferred** | Graph config `nodes` / `edges` / `graph` = `explicit` \| `on_write` |
| G-D3 | Union-of-members resolver | **deferred** / likely skip | “If any graph says on_write, always capture.” Extra versions; not default |
| G-D4 | Version GC | **deferred** | G-X4 |
| G-D5 | Pin reverse lookup | **deferred** | C-19 |
| G-D6 | Postgres HEAD trigger / `REVOKE` on `*_version` | **deferred** | Optional hardening; H2 will not |
| G-D7 | As-of-timestamp without a deep freeze | **deferred** | Need event-sourced membership; out of C-18 |
| G-D8 | Restore freeze into live HEAD | **deferred** | Would rewrite shared identities; not draft-from-fingerprint |
| G-D9 | Explorer from/to + milestone slider | **deferred** | C-18: right-pane version list + Latest. Slider/time-box later |

---

## Out of story

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | Lock/visibility flags | **cancelled** | |
| G-X2 | C-17 store lookups | **cancelled** | Other story |
| G-X3 | AR freeze product | **deferred** | Can call `createDeepGraphVersion` later |
| G-X4 | Version GC | **deferred** | |
| G-X5 | AuthZ / `created_by` | **cancelled** | |
| G-X6 | Per-keystroke versions | **cancelled** | |
| G-X7 | Rename SQL `type` → `obj_type` | **cancelled** | Mermaid only |
| G-X8 | C-19 clocks WI | **cancelled** | Done in C-18 WI-002 / version-row clocks at capture |
| G-X9 | Upgrade existing populated DBs | **cancelled** | Recreate; no migration scripts |

---

## Open (must close in WI-001 or the implementing WI)

None that block the ER. Left to implementing WIs:

- (closed WI-001) Freeze REST = `POST /graphs/{id}/versions`; keep `POST /graphs/{id}/clone`
- (closed) Explorer C-18 = version list + Latest; slider is G-D9
- Combined union persist vs in-memory capture helper (WI-006, see G-A25)

If WI-001 finds a contradiction with [`ER.md`](ER.md), add a row here; do not fork a second model.
