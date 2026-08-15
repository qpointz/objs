# Foundation mini-backlog — sbom-inventory-app (D-2)

Findings from this story that need **objs-core (or related foundation) extension**.  
Product/domain work continues with stopgaps where noted; **do not** use `objs-service` REST as a workaround.

**Owner process:** append rows when WI-002/003/MI/export uncover gaps. Promote to repo [`BACKLOG.md`](../../BACKLOG.md) / foundation WIs when scheduling work. **WI-003 audit complete (2026-08-13).**

**Status:** `parked` | `open` | `in-story` | `done` | `wontfix`

| ID | Need (user / product) | Foundation gap | Suggested API / change | Driven by | Status | Notes |
|----|----------------------|----------------|------------------------|-----------|--------|-------|
| FB-1 | Who uses this asset, and how? | No reverse: entity → graphs + incident edges | `listGraphIdsForEntity`, `listIncidentEdges` | J2 usage, **MI-2/MI-3**, G-P4 inference | **parked** | Stopgap = scan SBOM draft/version graphs only |
| FB-2 | Find duplicate assets by identifier | Identity projection exists; no find-by-identity query | `findEntitiesByIdentity` / `findDuplicateGroups` | J2 duplicates, **MI-4** | **parked** | Stopgap = pool select by type + group in memory |
| FB-3 | Advanced asset search from schema form | Pushdown may not cover all operators on **searchable** fields | Extend matcher pushdown | J2 advanced search | **open** | Slow path accepted; not in-story |
| FB-4 | Portfolio MI via Gremlin over selection | Need programmatic traverse (not REST) | `BoMGremlinEngine.selectAndEval` already exists | **MI-1…MI-4** | **done** | Wire from `:sbom-service`; no new foundation WI |
| FB-5 | Select graphs for portfolio level | No matcher for explicit graph-id set | `graphs-in` / `BoMGraphIdsMatcher` | **All MI** (R21→R22) | **done** | WI-014 |

## Audit table (WI-003)

| Capability | Core API today | Decision |
|------------|----------------|----------|
| R1–R6, R9, R13–R16, R21–R22 | Domain + existing store APIs | **OK** — example only |
| R7–R8, R11, R18–R19 (reverse / shared) | Forward select only | **parked FB-1** + domain scan stopgap |
| R10 searchable search | Partial pushdown | **open FB-3** + slow path |
| R12, R20 duplicates | Identity projection only | **parked FB-2** + in-memory group stopgap |
| R17–R20 MI selection | No graph-id-set matcher | **done FB-5 / WI-014** (`graphs-in`) |
| R17–R20 MI traverse | `BoMGremlinEngine.selectAndEval` | **done FB-4** |

## Parked detail — FB-1 (ex G-F1)

**Parked:** foundation **should be extended**; not implementing the reverse API unless user unparks.

**Stopgap:** scan `graph_id`s from `sbom_application_draft` / `sbom_application_version` only.

## Parked detail — FB-2 (ex G-F2)

**Parked:** identity **query** missing (projection exists).

**Stopgap:** `selectFromPool` by type → group equal identity maps in memory.

## In-story — FB-5 / WI-014

Add matcher (DSL key TBD, e.g. `graphs-in: [uuid…]`) that selects headers/members for an explicit set of graph ids. Wire into `BoMGraphStore.select` and thus `selectAndEval`. Optional chain with `obj-expr`.

Portfolio → ids stays in domain (R21/R22).

## How to add a finding

1. Add a row with next `FB-n` id.  
2. Mirror a short pointer in [`GAPS.md`](GAPS.md) foundation table.  
3. Do **not** silently close the product feature — ship stopgap or block the WI explicitly.

## Revision

| Date | Note |
|------|------|
| 2026-08-12 | Created; FB-1 parked (ex G-F1); FB-2…5 seeded from G-F2/4/7/8 |
| 2026-08-12 | FB-2 parked (ex G-F2) — identity query foundation gap |
| 2026-08-12 | G-F4 searchable-only; FB-3 scoped to pushdown on searchable fields |
| 2026-08-13 | FB-4/FB-5 retargeted: Gremlin MI + graph-id-set matcher |
| 2026-08-13 | WI-003: FB-4 done (engine exists); FB-5 in-story as WI-014; FB-1/2 parked confirmed |
| 2026-08-13 | WI-014: `graphs-in` matcher shipped; FB-5 done |
| 2026-08-13 | WI-013: MI-1 via `selectAndEval` + FB-5; MI-2/3/4 keep FB-1/FB-2 domain stopgaps |
