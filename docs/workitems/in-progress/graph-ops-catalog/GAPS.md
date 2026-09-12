# Gaps — graph-ops-catalog (C-36)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**WI-001:** every former `open` row is **resolved** or **deferred** (staged). No blocking open design rows for Stage A–C.

**Staging:** Stages A–C ship clear/purge/destroy/compact/backdate/REST/Composer/recipes/examples. **Stage D** (G-O16 travel-back + apply) **shipped** in WI-007.

---

## Locked (Stages A–C)

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-O0 | `clearGraph` | **resolved** | Clear HEAD membership + graph-local edges; keep header, annotations, history, pool entities |
| G-O0a | `destroyGraph` vs `delete` | **resolved** | `delete` = softish HEAD drop, history retained. `destroyGraph` = HEAD + all graph history |
| G-O0b | Migration backfill | **resolved** | Optional past `Instant` on freeze |
| G-O0c | Recipes doc | **resolved** | `programmatic-recipes.md` (WI-005); inject stores not DAOs |
| G-O1 | Backdate version key | **resolved** | Option B: `version = max(at.toEpochMilli(), prev+1)` + clocks from `at` on new freeze rows |
| G-O2 | Which rows get `at` | **resolved** | New freeze rows share `at`; pre-existing/reused entities keep clocks |
| G-O3 | `at` validation | **resolved** | Any non-null `Instant`; absent → `now` |
| G-O4 | `purgeGraphVersion` | **resolved** | Graph pins only; keep entity/edge version orphans |
| G-O4a | `compactEntity` / `compactEdge` | **resolved** | Explicit per-id orphan GC |
| G-O5 | Purge current head | **resolved** | Reject if `version == head_version`; no automagic |
| G-O6 | `purgeAllGraphVersions` | **resolved** | Delete all pins + null `head_version`; HEAD content intact |
| G-O7 | `destroyGraph` + SBOM | **resolved** | Explicit DAO deletes; fail if fingerprints reference versions |
| G-O8 | REST (A–C) | **resolved** | `/clear`, purge versions, `/destroy`, version `createdAt`, compact. **Reset path = Stage D** |
| G-O9 | Keep `delete` | **resolved** | Softish `delete` + full `destroyGraph` both stay |
| G-O10 | REPLACE-empty | **resolved** | `clearGraph` ≡ REPLACE empty sets |
| G-O11 | Workbench (A–C) | **resolved** | Composer: clear/purge/destroy/dated freeze/compact. **Reset UI = Stage D** |
| G-O12 | Examples | **resolved** | Maximize SBOM/AR; seed backdated versions |
| G-O13 | Exceptions | **resolved** | Existing `ObjsException` root; `GraphOperationException` + migrate `GraphException` |
| G-O14 | Naming | **resolved** | `clear` / `purge` / `destroy` (+ `delete` softish, `compact*`) |
| G-O15 | Recipes vs sketch | **resolved** | Keep `persist-sketch.md`; add recipes; cross-link only |

---

## Stage D — G-O16 reset / apply (two ops)

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| **G-O16** | Reset vs apply | **resolved** | **Two distinct ops** (do not overload one verb). Shipped in WI-007. |

### G-O16a — `resetGraphToVersion` (“travel back”) — **full materialize**

```text
resetGraphToVersion(graphId, version, truncateAfter: Boolean = false)
```

| Step | Behaviour |
|------|-----------|
| 1 | Freeze must exist → else `GRAPH_VERSION_NOT_FOUND` |
| 2 | **Full materialize:** restore **membership + graph-local edges + entity/edge payloads** from that freeze (true time-travel of HEAD content) |
| 3 | Set `objs_graph.head_version = version` |
| 4 | `truncateAfter == true` → purge graph versions with `version > target` (+ pins); orphans until compact |
| 5 | `truncateAfter == false` (default) → keep newer freezes; head may be behind max version |

Shared-pool rewrite of live entity/edge bytes is **accepted and documented**. Idempotent if already at that head with matching content.

### G-O16b — `applyGraphVersionMembership` (“version apply”) — **membership only**

```text
applyGraphVersionMembership(graphId, version)
```

| Step | Behaviour |
|------|-----------|
| 1 | Freeze must exist |
| 2 | Reconstruct **membership + graph-local edge topology** from that freeze’s pins |
| 3 | **Do not** overwrite entity/edge **payloads** from historical pins — live HEAD keeps **most recent** entity/edge content (current pool HEAD bytes); missing live rows fall back to freeze |
| 4 | `head_version` **unchanged** (do not pretend HEAD content equals that freeze) |

This is **not** travel-back. Use when callers want “structure as of version N, data as of now.”

### Shared Stage D notes

- Distinct from G-O6 (`purgeAllGraphVersions`: history wipe, no materialize).
- REST: `POST …/versions/{v}/reset?truncateAfter=` and `POST …/versions/{v}/apply-membership`.
- Composer: distinct Travel back vs Apply membership actions.
---

## Out of story / cancelled

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | Global batch entity/edge GC | **deferred** | Beyond per-id `compact*` |
| G-X2 | Restore freeze → HEAD | **cancelled** (moved) | Was C-18 G-D8; now **G-O16 Stage D** |
| G-X3 | Soft-delete / recycle bin | **cancelled** | Physical ops only |
| G-X4 | AuthZ | **deferred** | Same as rest of store |
| G-X5 | Backdate live HEAD clocks without freeze | **deferred** | Separate from freeze `at` |
| G-X6 | Rewrite existing `version` keys | **cancelled** | Purge + re-freeze if needed |

---

## Links to prior gaps

| Prior | Relation |
|-------|----------|
| C-18 G-A16 | `delete` vs `destroyGraph` |
| C-18 G-A30 / G-X4 | Backfill + purge + compact |
| C-18 G-D8 | → **G-O16 Stage D** |
| C-22 | REPLACE-empty → `clearGraph` |
