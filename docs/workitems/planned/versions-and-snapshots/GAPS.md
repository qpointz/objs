# Gaps — versions-and-snapshots (C-18)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**Planned story only. No implementation until asked.**

---

## Architecture

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-A1 | Mechanism | **resolved** | Versions + snapshot pins. Not pool flags, not annotations |
| G-A2 | Identity vs version | **resolved** | Stable id; payload on append-only version rows |
| G-A3 | Persist | **resolved** | One save = one version + HEAD move |
| G-A4 | Live graph | **resolved** | Members = identities; read HEAD |
| G-A5 | Snapshot graph | **resolved** | Pins `(identity, versionId)`; reconstruct; pins do not move |
| G-A6 | Edges | **resolved** | Versioned like nodes (graph-local id + versions). Snapshot pins edge versions; endpoints via entity pins |
| G-A7 | `copyGraph` / `mergeGraph` | **resolved** | Live share / persist-union (C-17). Not a snapshot |
| G-A8 | C-12 `clone()` | **resolved** | Replaced by snapshot pins. Breaking vs today’s clone |
| G-A9 | Pool / FB-2 | **resolved** | Catalog = identities at HEAD |
| G-A10 | Freeze | **resolved** | Version rows never updated |
| G-A11 | Story split | **resolved** | C-17 lookups stay C-17. This story is versions + snapshots only |
| G-A12 | AR | **resolved** | No snapshot product here; collections remain live HEAD |
| G-A13 | History growth | **accepted-risk** | GC later |
| G-A14 | Lazy version-only-on-snapshot | **deferred** | Same API; optimize later if needed |

---

## Out of story

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | Lock/visibility flags | **cancelled** | |
| G-X2 | C-17 store lookups | **cancelled** | Other story |
| G-X3 | AR freeze snapshot | **deferred** | Can use pins later |
| G-X4 | Version GC | **deferred** | |
| G-X5 | AuthZ / `created_by` | **cancelled** | |
| G-X6 | Per-keystroke versions | **cancelled** | |
