# Gaps — edge-payload-parity (C-48)

Decision tracker. **Details, evidence, and impact per gap:** [`GAP-INVENTORY.md`](GAP-INVENTORY.md).

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.  
Severity: **B** blocker · **M** major · **m** minor. Scope: **edge** (edge-only) · **both**.

**WI-001 must close every `open` row** (resolve with owning WI, or defer) before implementation.

---

## Domain model — [§4](GAP-INVENTORY.md#4-domain-model-objs-api)

| # | Topic | Sev | Scope | Status |
|---|-------|:---:|:-----:|--------|
| G1 | Edge `type` / `schemaVersion` / `properties` nullable | M | edge | open |
| G2 | Edges have no annotations | M | edge | open |
| G3 | No latest edge-properties schema helper | m | edge | open |
| G4 | No display / field-hint helpers for edges | m | edge | open |
| G5 | Schema `usage` never enforced | m | both | open |

## Validation and schema evolution — [§5](GAP-INVENTORY.md#5-validation-and-schema-evolution)

| # | Topic | Sev | Scope | Status |
|---|-------|:---:|:-----:|--------|
| G6 | Allow-list rule pins one edge schema version | B | edge | open |
| G7 | Bumping a rule breaks stored edges on next write | B | edge | open |
| G8 | No upgrade-on-read for edge properties | M | edge | open |
| G9 | No typed edge binding registry | M | edge | open |
| G10 | No stored-payload migration / default upgrade wiring | M | both | open |
| G11 | `NONE` policy ignores stray edge schema refs | m | edge | open |
| G12 | Edge identifier immutability skipped for partially typed edges | m | edge | open |

## Persistence — [§6](GAP-INVENTORY.md#6-persistence-objs-persistence)

| # | Topic | Sev | Scope | Status |
|---|-------|:---:|:-----:|--------|
| G13 | No `(type, schema_version)` index on edges | M | edge | open |
| G14 | No GIN index on document columns | m | both | open |
| G15 | Store helpers exist only for entities | M | edge | open |
| G16 | `EdgeDao` cannot select by type / version | M | edge | open |
| G17 | Selection never returns edges on their own merit | M | edge | open |
| G18 | Versioning hook skipped for graph-scoped edge upserts | m | edge | open |

## Query / matcher — [§7](GAP-INVENTORY.md#7-query-and-matcher)

| # | Topic | Sev | Scope | Status |
|---|-------|:---:|:-----:|--------|
| G19 | `obj-expr` binds entity fields only | B | edge | open |
| G20 | `matchesEdge` endpoint-only and unused | M | edge | open |
| G21 | Edge matching infrastructure unused | m | edge | open |
| G22 | Matcher DSL has no edge clause | M | edge | open |
| G23 | SQL pushdown covers entity payload only | M | edge | open |
| G24 | No component can filter by edge payload | B | edge | open |

## Mutation / rewriter / merge — [§8](GAP-INVENTORY.md#8-mutation-rewriter-and-merge)

| # | Topic | Sev | Scope | Status |
|---|-------|:---:|:-----:|--------|
| G25 | Rewriter has no edge decisions | M | edge | open |
| G26 | Rewriter dedupe ignores properties (silent loss) | M | edge | open |
| G27 | Merge policy keys edges by triple, never merges properties | M | edge | open |
| G28 | Typed edges are write-only | M | edge | open |
| G29 | Writes replace whole documents | m | both | open |

## REST — [§9](GAP-INVENTORY.md#9-rest-api-objs-service)

| # | Topic | Sev | Scope | Status |
|---|-------|:---:|:-----:|--------|
| G30 | Edge create uses raw domain type | m | edge | open |
| G31 | No endpoint to read one live edge | M | edge | open |
| G32 | Property-only update needs full edge | m | edge | open |
| G33 | No edge list / search | M | edge | open |
| G34 | Relation lookup by type works for entity types only | m | edge | open |
| G35 | `next-major` for edge schemas cannot be adopted | M | edge | open |
| G36 | No edge-to-graph membership endpoint | m | edge | open |

## Gremlin — [§10](GAP-INVENTORY.md#10-gremlin-objs-gremlin-core-objs-gremlin-service)

| # | Topic | Sev | Scope | Status |
|---|-------|:---:|:-----:|--------|
| G37 | `hasLabel` = type on vertices, role on edges | M | edge | open |
| G38 | Absent edge properties serialize as `null` | m | edge | open |
| G39 | Edge hits lose metadata in result subgraphs | m | edge | open |
| G40 | Payloads nested, never flattened | m | both | open |
| G41 | No edge-property test coverage | m | edge | open |

## JGraphT — [§11](GAP-INVENTORY.md#11-jgrapht-objs-jgrapht-core-objs-jgrapht-service)

| # | Topic | Sev | Scope | Status |
|---|-------|:---:|:-----:|--------|
| G42 | Edge properties carried but never used (no weights) | M | edge | open |
| G43 | Analysis request has no edge parameters | M | edge | open |
| G44 | Analysis topological; results ids-only | m | both | open |
| G45 | No graph export | m | both | open |
| G46 | No test that edge properties survive | m | edge | open |

## Policies — [§12](GAP-INVENTORY.md#12-policies-objs-policy-)

| # | Topic | Sev | Scope | Status |
|---|-------|:---:|:-----:|--------|
| G47 | `EdgeFact` weaker than `EntityFact` | M | edge | open |
| G48 | No edge-property policy test / fixture | m | edge | open |
| G49 | Policy input selection entity-only | M | edge | open |
| G50 | No subject targeting / conditions / archive filters | m | both | open |

## Workbench UI — [§13](GAP-INVENTORY.md#13-workbench-ui-objs-service-ui)

| # | Topic | Sev | Scope | Status |
|---|-------|:---:|:-----:|--------|
| G51 | TS types lag backend | m | edge | open |
| G52 | No schema-version migrate for edges | M | edge | open |
| G53 | Edge editor lacks annotations / field delete | M | edge | open |
| G54 | New edges ignore schema defaults | M | edge | open |
| G55 | Canvas never shows edge properties | M | edge | open |
| G56 | Edge filters role-only | M | edge | open |
| G57 | Edge-schema relations UI not wired | M | edge | open |
| G58 | Edge viewer mislabels identity (`Node`) | m | edge | open |
| G59 | No version diff view | m | both | open |

## Examples — [§14](GAP-INVENTORY.md#14-examples)

| # | Topic | Sev | Scope | Status |
|---|-------|:---:|:-----:|--------|
| G60 | SBOM declares edge schema, never fills it | M | edge | open |
| G61 | SBOM DTOs drop edge type / properties | M | edge | open |
| G62 | SBOM no relation update; CycloneDX ignores edge properties | M | edge | open |
| G63 | SBOM evolution / schema pages entity-only | M | edge | open |
| G64 | AR forbids edge properties | M | edge | open |
| G65 | AR relation API property-less and append-only | M | edge | open |
