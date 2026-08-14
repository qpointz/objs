# Foundation mini-backlog — sbom-inventory-app (D-2)

Findings from this story that need **objs-core (or related foundation) extension**.  
Product/domain work continues with stopgaps where noted; **do not** use `objs-service` REST as a workaround.

**Owner process:** append rows when WI-002/003/MI/export uncover gaps. Promote to repo [`BACKLOG.md`](../../BACKLOG.md) / foundation WIs when scheduling work. Refine in **WI-003**.

**Status:** `parked` | `open` | `in-story` | `done` | `wontfix`

| ID | Need (user / product) | Foundation gap | Suggested API / change | Driven by | Status | Notes |
|----|----------------------|----------------|------------------------|-----------|--------|-------|
| FB-1 | Who uses this asset, and how? | No reverse: entity → graphs + incident edges | `listGraphIdsForEntity`, `listIncidentEdges` | J2 usage, **MI-2/MI-3**, G-P4 inference | **parked** | Was G-F1. Domain stopgap = scan SBOM draft/version graphs only; record debt |
| FB-2 | Find duplicate assets by identifier | Identity projection exists; no find-by-identity query | `findEntitiesByIdentity` / `findDuplicateGroups` | J2 duplicates, **MI-4** | **parked** | Was G-F2. Stopgap = pool select by type + group in memory; demo scale only |
| FB-3 | Advanced asset search from schema form | Pushdown may not cover operators on **searchable** fields | Extend matcher pushdown for searchable paths | J2 advanced search (`searchable` only; **slow path** if no pushdown) | **open** | Product locked; slow path accepted stopgap; FB-3 = faster/complete pushdown |
| FB-4 | Portfolio MI via Gremlin over selection | Example should traverse selected union, not hand-fold raw graphs | Programmatic `selectAndEval` + report scripts (objs-gremlin-core) | **MI-1…MI-4** | **open** | Was G-F7. Prefer Gremlin; domain DTO only |
| FB-5 | Select graphs for portfolio level | No matcher for explicit graph-id set | `graphs-in` / id-set matcher (+ optional `obj-expr`) | **All MI** (R21→R22) | **open** | Was G-F8. Domain: portfolio→apps→**latest version** graph_ids; core: id-set only |

## Parked detail — FB-1 (ex G-F1)

**Parked:** foundation **should be extended**; not implementing the reverse API in the first pass of example coding unless WI-003 reopens it as `in-story`.

**User need:** Pick asset → applications that include it + relation labels (usage inspect / shared hotspots).

**Stopgap for example:** Restrict scan to `graph_id`s known from `sbom_application_draft` / `sbom_application_version`; map hits to applications. Acceptable for demo scale only.

**When unparking:** implement public store APIs, tests in `objs-core`, then thin domain join for labels — see story [`GAPS.md`](GAPS.md) G-F1 narrative.

## Parked detail — FB-2 (ex G-F2)

**Parked:** foundation **should be extended**; identity **query** is missing (projection already exists for immutability).

**User need:** Find-only duplicate groups by schema identifier fields (G-P7).

**Stopgap for example:** `selectFromPool` by type → `BoMIdentityProjection` → group equal identity maps in memory. Demo scale only.

**When unparking:**

```text
findEntitiesByIdentity(type, schemaVersion, identityMap): List<BoMEntity>
// and/or findDuplicateGroups(type, schemaVersion): List<DuplicateGroup>
```

Plus tests in `objs-core`; then WI-008 / MI-4 use the API instead of the scan.

## How to add a finding

1. Add a row with next `FB-n` id.  
2. Mirror a short pointer in [`GAPS.md`](GAPS.md) foundation table (`parked` / `open` + “see FOUNDATION-BACKLOG”).  
3. Do **not** silently close the product feature — ship stopgap or block the WI explicitly.

## Revision

| Date | Note |
|------|------|
| 2026-08-12 | Created; FB-1 parked (ex G-F1); FB-2…5 seeded from G-F2/4/7/8 |
| 2026-08-12 | FB-2 parked (ex G-F2) — identity query foundation gap |
| 2026-08-12 | G-F4 searchable-only; FB-3 scoped to pushdown on searchable fields |
| 2026-08-13 | FB-4/FB-5 retargeted: Gremlin MI + graph-id-set matcher; latest-version graphs; all MI portfolio-scoped |
