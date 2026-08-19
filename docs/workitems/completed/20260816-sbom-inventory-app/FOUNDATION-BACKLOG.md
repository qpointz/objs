# Foundation mini-backlog — sbom-inventory-app (D-2)

Findings from this story that need **objs-core (or related foundation) extension**.  
Product/domain work continues with stopgaps where noted; **do not** use `objs-service` REST as a workaround.

**Owner process:** append rows when WI-002/003/MI/export uncover gaps. Promote to repo [`BACKLOG.md`](../../BACKLOG.md) / foundation WIs when scheduling work. **WI-003 audit complete (2026-08-13).** FB-1 / FB-2 closed by [C-17 `live-store-apis`](../20260819-live-store-apis/STORY.md) WI-003 / WI-004.

**Status:** `parked` | `open` | `in-story` | `done` | `wontfix`

| ID | Need (user / product) | Foundation gap | Suggested API / change | Driven by | Status | Notes |
|----|----------------------|----------------|------------------------|-----------|--------|-------|
| FB-1 | Who uses this asset, and how? | No reverse: entity → graphs + incident edges | `listGraphIdsForEntity` / `listIncidentEdges` | J2 usage, **MI-2/MI-3**, G-P4 inference | **done** | C-17 WI-003. SBOM usage + MI-2/3 join graph ids to applications |
| FB-2 | Find duplicate assets by identifier | Identity projection exists; no find-by-identity query | `findEntitiesByIdentity` / `findDuplicateGroups` | J2 duplicates, **MI-4** | **done** | C-17 WI-004. Grouping in the store |
| FB-3 | Advanced asset search from schema form | Pushdown may not cover all operators on **searchable** fields | Extend matcher pushdown | J2 advanced search | **open** | Contains/`q` → C-20; remaining operators → C-19. Slow path accepted |
| FB-4 | Portfolio MI via Gremlin over selection | Need programmatic traverse (not REST) | `BoMGremlinEngine.selectAndEval` already exists | **MI-1…MI-4** | **done** | Wire from `:sbom-service`; no new foundation WI |
| FB-5 | Select graphs for portfolio level | No matcher for explicit graph-id set | `graphs-in` / `BoMGraphIdsMatcher` | **All MI** (R21→R22) | **done** | WI-014 |

## Audit table (WI-003)

| Capability | Core API today | Decision |
|------------|----------------|----------|
| R1–R6, R9, R13–R16, R21–R22 | Domain + existing store APIs | **OK** — example only |
| R7–R8, R11, R18–R19 (reverse / shared) | `listGraphIdsForEntity` / `listIncidentEdges` | **done FB-1** (C-17 WI-003); domain still maps graph → application |
| R10 searchable search | Partial pushdown | **open FB-3** + slow path (`q` = C-20) |
| R12, R20 duplicates | `findDuplicateGroups` / `findEntitiesByIdentity` | **done FB-2** (C-17 WI-004) |
| R17–R20 MI selection | No graph-id-set matcher | **done FB-5 / WI-014** (`graphs-in`) |
| R17–R20 MI traverse | `BoMGremlinEngine.selectAndEval` | **done FB-4** |

## Shipped — FB-1 (ex G-F1)

**Done (C-17 WI-003):** `BoMNamedGraphStore.listGraphIdsForEntity` / `listIncidentEdges`. SBOM usage and MI-2/3 still join those graph ids to application/version rows (domain).

## Shipped — FB-2 (ex G-F2)

**Done (C-17 WI-004):** `BoMGraphStore.findEntitiesByIdentity` / `findDuplicateGroups`. Identifier immutability stays on the persist gate. Empty identity is omitted (G-A13).

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
| 2026-08-19 | C-17 WI-007: FB-1 / FB-2 **done**; FB-3 remains open (C-20 / C-19) |
