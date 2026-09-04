# Graph fragments and JGraphT analysis

**Modules:** `:objs-api` (contract) · `:objs-jgrapht-core` (JVM algorithms) · `:objs-jgrapht-service`
(optional REST) · `:objs-gremlin-core` (TinkerGraph materialization) · `:objs-service-ui`
(capability-driven UI)

**Story:** [`graph-frontend-jgrapht`](../../workitems/completed/20260903-graph-frontend-jgrapht/STORY.md)

Native graph engines (Gremlin and JGraphT) consume the same **resolved fragment** after a shared
normalization policy. The browser and REST JSON expose only implementation-neutral DTOs — never
JGraphT types or generated application classes.

## Layering (do not conflate)

| Layer | Responsibility | Examples |
|-------|----------------|----------|
| **`GraphFragment` normalization** | Identity, duplicate, conflict, dangling-endpoint rules on caller-supplied `(entities, edges)` | `DefaultGraphFragmentPolicy.resolve` |
| **Persistence `GraphMergePolicy`** | Survivor choice when **persisting** a union of live graphs with overlapping keys | `FirstSeenGraphMergePolicy` on `mergeGraph` |
| **Validation / mutation** | Schema, allow-list, cardinality at the persist gate | `BoMGraphStore.persist`, MERGE/REPLACE |
| **Gremlin result projection** | Map TinkerPop traversal hits back to `BoMGraphContents` / table / scalar views | `BoMGremlinEngine` subgraph2 rules |
| **Frontend graph rendering** | React Flow + Dagre canvas; type filter dimming; optional analysis highlight | `:objs-service-ui` Explorer |
| **Policy evaluation (C-24 shipped)** | Executable policy artefacts over a resolved fragment (`DefaultPolicyEvaluator`: wiring → applicability → engine) — **not** `GraphFragmentPolicy` | [`docs/design/policy/`](../policy/), [`policy-evaluate-core`](../../workitems/in-progress/policy-evaluate-core/STORY.md); follow-ups C-26…C-31 |

`GraphStore.selectAcrossGraphs` already deduplicates by globally unique entity/edge ids when
reading from persistence. **`GraphFragmentPolicy`** is required for **caller-assembled** fragments
(SBOM multi-select union, asset-repository composition batches, codegen builders) — not as a
replacement for store selection invariants. **`:objs-policy-api` / `:objs-policy-core`** consume the same resolved
fragment for business/governance evaluation; do not conflate them with fragment normalization.

## Fragment contract (`:objs-api`)

```text
GraphFragment(entities, edges)
        |
        v
GraphFragmentPolicy.resolve(...)
        |
        v
ResolvedGraphFragment(entities, edges, diagnostics)
        |                         |
        v                         v
  Gremlin materializer     JGraphT materializer
        |                         |
        v                         v
    TinkerGraph             DirectedPseudograph (default)
```

- **`GraphFragment`** — structural `(entities, edges)`; implemented by `GraphContents` and
  mutable `Graph` / builder output.
- **`ResolvedGraphFragment`** — same collections plus `GraphFragmentDiagnostic` entries
  (`INFO` / `WARNING` / `ERROR`, message, related node/edge UUID lists).
- **`DefaultGraphFragmentPolicy`** — UUID identity; deduplicates identical records; emits
  **ERROR** on conflicts and dangling endpoints; clears ambiguous `Edge.graphId` provenance.
- **Materializers reject** resolved fragments that contain **ERROR** diagnostics
  (`ResolvedGraphMaterialization.requireMaterializable`).

Semantic duplicate comparison ignores timestamps, version counters, and graph provenance. See
story [`GAPS.md`](../../workitems/completed/20260903-graph-frontend-jgrapht/GAPS.md) for locked
decisions.

## Module boundaries

| Module | Owns |
|--------|------|
| `:objs-api` | `GraphFragment`, `ResolvedGraphFragment`, `GraphFragmentPolicy`, diagnostics, materialization guard |
| `:objs-jgrapht-core` | JGraphT 1.5.3 materializer (`DirectedPseudograph` default), optional `JGraphTGraphFactory`, SCC cycle-region analyzer, capability/result DTOs |
| `:objs-jgrapht-service` | Optional Spring REST (`graph-algorithms` OpenAPI tag); **not** on `:objs-service` classpath |
| `:objs-gremlin-core` | TinkerGraph materialization from `ResolvedGraphFragment`; Gremlin eval |
| `:objs-gremlin-service` | `POST /graph/traverse/gremlin` |
| `:objs-service-app` | Workbench runner; depends on `:objs-gremlin-service` and `:objs-jgrapht-service` explicitly |
| `:objs-service-ui` | Capability fetch + cycle analysis UI; **no** JVM graph libraries |

Generated typed classes (`ProductNode`, `GeneratedReadView`, …) remain **application-owned**.
Codegen emits `GeneratedReadView.from(GraphFragment)` / `from(ResolvedGraphFragment)` adapters;
foundation modules do not embed generated sources.

## JGraphT materialization modes

| Mode | Scope | Behaviour |
|------|-------|-----------|
| **`GENERIC`** | REST + workbench (mandatory) | Unweighted `DirectedPseudograph`; preserves self-loops and parallel edges |
| **`TYPED`** | Optional JVM extension | Caller-supplied `JGraphTGraphFactory<V,E>`; advertised in capabilities when a bean is present |

The optional factory creates graph/vertex/edge instances only. It does **not** perform selection,
normalization, or conflict handling.

## REST analysis endpoints (optional module)

| Method | Path | Response |
|--------|------|----------|
| `GET` | `/api/v1/objs/graph/algorithms/capabilities` | `GraphAlgorithmCapabilities` |
| `POST` | `/api/v1/objs/graph/algorithms/cycles` | `GraphCycleAnalysis` |

**Request** (`POST …/cycles`) uses the same matcher DSL as graph query / Gremlin traverse, plus
optional `graphId`, `graphVersion`, `materialization` (default **`GENERIC`**), and optional
`algorithm` (default directed cycle regions).

**Wire DTOs** (UUIDs as strings in JSON):

- `GraphAlgorithmCapabilities` / `GraphAlgorithmCapability` — algorithm id + supported materialization modes
- `GraphCycleAnalysis` — `algorithm`, `components[]`, `stats`, optional `diagnostics[]`
- `GraphCycleComponent` — stable component `id` (smallest entity UUID in the SCC), sorted `entityIds`, `edgeIds`
- `GraphAnalysisStats` — `entityCount`, `edgeCount`, `cyclicComponentCount`

**Service absence:** when `:objs-jgrapht-service` is not on the classpath, capabilities return
**404**; the workbench treats missing or failed capability fetch as “algorithms unavailable” and
hides **Analyze cycles** without blocking Explorer, Query, or Composer.

## Cycle semantics (v1)

Algorithm id: **`directed-cycle-regions`**.

- Strongly connected components with **>1** node are cyclic; singleton components with a **self-loop**
  are cyclic.
- Component id = smallest internal **entity** UUID under unsigned RFC 4122 byte order.
- Entity and edge ids in each component are sorted deterministically; component order is
  deterministic.

Elementary-cycle enumeration is **deferred** (see story GAPS G-JG-D3).

## Example consumer reuse

| Consumer | Fragment path |
|----------|---------------|
| SBOM `BomUnion.of` | Flatten selected `ResolvedGraph` contents → `DefaultGraphFragmentPolicy.resolve` → `ResolvedGraphFragment` (ephemeral; not persisted) |
| Asset repository `ObjectWriteService.writeComposition` | Assemble pending objects → policy resolve → reject ERROR diagnostics before graph mutation handoff |
| Codegen `GeneratedReadView` | `from(GraphFragment)` / `from(ResolvedGraphFragment)` over typed bindings |
| Gremlin `BoMGremlinEngine` | Store select → policy resolve → TinkerGraph materialize |
| Workbench Explorer | Capabilities + `POST …/cycles` → violet node/edge highlight (independent of selection) |

Domain-specific identity resolution and persistence write validation stay at example boundaries.

## Browser and REST boundary

- **No** `jgrapht` dependency in `:objs-service-ui` `package.json` or bundled assets.
- **No** JGraphT or generated application class names in REST JSON — only the DTOs above plus
  standard `BoMEntity` / `BoMEdge` graph shapes elsewhere.

## Deferred

- **JGraphT `GraphImporter` / file loaders** — not part of this story; any future importer must
  map into `GraphFragment` before analysis (G-JG-D1, G-JG-11 cancelled).
- **SBOM UI cycle actions** — workbench only (G-JG-D2).

## Related

- Runnable examples: [`cycle-analysis-examples.md`](cycle-analysis-examples.md)
- Gremlin alignment: [`gremlin.md`](gremlin.md)
- Codegen read adapters: [`api-and-codegen.md`](api-and-codegen.md)
- Workbench Explorer: [`../ui.md`](../ui.md)
- REST table: [`../service/rest-api.md`](../service/rest-api.md)
