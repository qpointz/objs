# Graph fragments, JGraphT analysis, and workbench support

**Slug:** `graph-frontend-jgrapht`  
**Branch:** `graph-frontend-jgrapht`  
**Status:** completed  
**Closed:** 2026-09-03  
**Folder:** [`docs/workitems/completed/20260903-graph-frontend-jgrapht/`](.)  
**Base:** `origin/dev`  
**Design:** [`docs/design/graph/fragments-and-analysis.md`](../../../design/graph/fragments-and-analysis.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Create a reusable, framework-free graph-fragment boundary that normalizes one or more collections
of entities and edges before native graph materialization. Gremlin and JGraphT must consume the same
resolved fragment while creating independent native graphs. The workbench may expose algorithms only
when an optional algorithm service is present; JGraphT and generated JVM types must never enter the
browser bundle.

## Work Items

- [x] WI-000 — Lock graph-fragment contract and story boundaries (`WI-000-design-lock.md`)
- [x] WI-001 — Add shared graph-fragment normalization and typed/codegen adapters (`WI-001-fragment-normalization.md`)
- [x] WI-002 — Add JGraphT core and align Gremlin materialization (`WI-002-jgrapht-core.md`)
- [x] WI-003 — Add optional algorithm service endpoints (`WI-003-algorithm-service.md`)
- [x] WI-004 — Add capability-driven workbench analysis support (`WI-004-workbench-analysis.md`)
- [x] WI-005 — Harden documentation and verification (`WI-005-hardening.md`)

## Stages and dependencies

### Stage 0 — Contract and boundaries

WI-000 is complete. It locks module names, endpoint shapes, cycle semantics, materialization modes,
and the `GraphFragment` contract. Implementation WIs may now start against these decisions.

### Stage 1 — Shared fragment foundation

WI-001 depends on WI-000 and is complete. It adds the reusable contract in `objs-api` and proves
that raw, persistence-selected, and generated typed values can enter the same normalization path.
It also migrates SBOM and asset-repository composition paths to that policy boundary.

### Stage 2 — Native graph engines

WI-002 depends on WI-001 and is complete. It adds Spring-free `objs-jgrapht-core`, aligns Gremlin
with the resolved fragment, and verifies equivalent topology, identity, diagnostics, and cycle
behavior.

### Stage 3 — Optional HTTP integration

WI-003 depends on WI-002 and is complete. It adds `objs-jgrapht-service`, capability discovery, and the
implementation-neutral cycle endpoint without making the base service depend on it. The workbench
runner includes the module explicitly.

### Stage 4 — Workbench integration

WI-004 depends on WI-003 and is complete. It adds capability-driven algorithm actions and node/edge
highlighting using the existing React Flow and Dagre graph canvas. The SBOM UI is not migrated by
this story.

### Stage 5 — Hardening

WI-005 depends on all previous WIs and is complete. Design docs, REST tables, and workbench tour
describe fragment boundaries and optional algorithm integration. Focused and repository-level tests
pass; browser and REST boundary checks confirm no JGraphT leakage.

## Locked architecture

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
    TinkerGraph             JGraphT graph
```

`GraphFragment` is the structural contract. Existing `GraphContents` should implement it, while
mutable `Graph` and `GraphMutation` remain write-side forms. The policy is
the single extension point for identity, duplicate, conflict, and dangling-endpoint behavior:

```kotlin
interface GraphFragment {
    val entities: List<Entity>
    val edges: List<Edge>
}

data class ResolvedGraphFragment(
    val entities: List<Entity>,
    val edges: List<Edge>,
    val diagnostics: List<GraphFragmentDiagnostic>,
) : GraphFragment

fun interface GraphFragmentPolicy {
    fun resolve(fragment: GraphFragment): ResolvedGraphFragment
}
```

The default policy uses UUID identity, deduplicates identical records, and emits error diagnostics
for conflicts. It returns normalized candidates alongside those diagnostics; native materializers
reject error-bearing results. Dangling endpoints are rejected by default. Materializers must not
silently choose first/last records. Custom policies may retain or assign absent IDs and provide
application-specific identity, merge, and dangling-endpoint rules.

Semantic duplicate comparison uses UUID, type/schema, payload/annotations, endpoints, role, and
edge properties. It ignores timestamps, version counters, and graph provenance. Any existing
`Edge.graphId` value is optional source metadata rather than fragment/container context or edge
identity; when equivalent records have different provenance, the resolved edge clears `graphId`
because provenance is not unambiguous.

## Module boundaries

- `:objs-api` owns the framework-free fragment contract and diagnostics.
- `:objs-jgrapht-core` owns JGraphT materialization, algorithm identifiers, DTOs, and SCC analysis.
- `:objs-jgrapht-service` is an optional Spring HTTP adapter over the core.
- `:objs-gremlin-core` consumes the same resolved fragment but retains TinkerPop privately.
- `:objs-service-ui` consumes capability and analysis DTOs only.
- Generated typed classes remain application-owned; they are adapters and typed hydration consumers,
  not dependencies of foundation modules.

## Materialization and analysis

`GENERIC` is the mandatory REST/workbench mode and represents generic entity/edge instances.
The default `GENERIC` implementation in `objs-jgrapht-core` uses JGraphT's unweighted
`DirectedPseudograph`, the simplest directed structure that preserves self-loops and parallel
edges. It consumes the resolved fragment, creates and populates an in-memory graph, and returns
caller-owned mutable state. JGraphT remains JVM-only and never appears in REST JSON or browser
assets.

`TYPED` materialization and caller-supplied graph construction are optional JVM extensions. The
default path does not require them, while an optional `JGraphTGraphFactory` can define a specialized
graph structure, vertex/edge representation, or additional construction logic above the resolved
fragment:

```kotlin
interface JGraphTGraphFactory<V, E> {
    fun createGraph(): org.jgrapht.Graph<V, E>
    fun createVertex(entity: Entity): V
    fun createEdge(edge: Edge, source: V, target: V): E
}
```

The factory is not used by the REST/workbench default path and must not perform selection,
normalization, identity resolution, or conflict handling.

The workbench-first v1 wire DTOs are:

```kotlin
data class GraphAlgorithmCapabilities(
    val algorithms: List<GraphAlgorithmCapability>,
)

data class GraphAlgorithmCapability(
    val id: String,
    val materializationModes: List<String>,
)

data class GraphCycleAnalysis(
    val algorithm: String,
    val components: List<GraphCycleComponent>,
    val stats: GraphAnalysisStats,
    val diagnostics: List<GraphFragmentDiagnostic> = emptyList(),
)

data class GraphCycleComponent(
    val id: UUID,
    val entityIds: List<UUID>,
    val edgeIds: List<UUID>,
)

data class GraphAnalysisStats(
    val entityCount: Int,
    val edgeCount: Int,
    val cyclicComponentCount: Int,
)
```

The workbench compatibility target is the algorithm ID, component ID, entity IDs, edge IDs, and
counts. Diagnostics and future consumer-oriented fields are best-effort additive data; other
consumers must not be treated as a stronger compatibility target.

The first algorithm is directed cycle-region analysis using strongly connected components:
components of more than one node are cyclic, as are singleton components with a self-loop.
The component ID is the smallest internal entity UUID in that component under unsigned 128-bit
(RFC 4122 byte) UUID order; results contain sorted entity/edge IDs and deterministic component
ordering.

## Decisions to lock during implementation

- `GraphFragmentDiagnostic` contains severity (`INFO`, `WARNING`, or `ERROR`), free-text message,
  and `nodes` / `edges` lists of source UUIDs. Diagnostic entry ordering is unspecified.
- Absent entity/edge IDs are valid in both fragment types; frontend-facing DTOs require IDs and
  must handle absent IDs at the application/service boundary.
- `GraphFragmentPolicy.resolve(...)` resolves the supplied node and edge values; ownership,
  immutability, and cloning of the initial input are application-level decisions. The policy does
  not promise a detached snapshot.
- Graph selection, graph version, matcher, source-graph provenance, and other container context
  belong to the consuming application/service. `GraphFragment` and `ResolvedGraphFragment` contain
  only entities and edges plus diagnostics; intrinsic entity/edge UUIDs remain part of those
  records. The core contract does not require context propagation into results.
- Persistence-backed cross-graph selection already applies its UUID union over globally unique
  entity records and graph-owned edge IDs. `GraphFragmentPolicy` is required for caller-supplied or
  composite fragments, not as a replacement for that existing store invariant.
- SBOM `BomUnion` and asset-repository composition use the shared fragment-policy path for
  assembled entities and edges. Example-specific identity resolution and persistence write
  validation remain at their domain boundaries. Example-facing APIs may change for a cleaner
  design; affected callers are migrated and compatibility shims are not required.
- JGraphT’s directed graph implementation must support self-loops and parallel edges, and its
  lifecycle/ownership must be explicit for JVM callers.
- The default `DirectedPseudograph` materializer is usable without a factory. The optional
  `JGraphTGraphFactory` extension is available for callers that need specialized graph structures
  or typed construction, but is not an architectural requirement for consumers.
- Component IDs and endpoint DTOs must be deterministic and implementation-neutral. A component ID
  is the smallest internal entity UUID under unsigned 128-bit (RFC 4122 byte) order, not a
  synthetic ID or graph-context identifier.
- The default JGraphT graph is an unweighted `DirectedPseudograph`; the materializer creates and
  populates it, then returns caller-owned mutable state. An optional graph factory may supply a
  different graph, but no close/dispose operation is needed for the in-memory graph.
- Gremlin and JGraphT share the `ResolvedGraphFragment` input. Gremlin entry points, materializer
  APIs, result projection, and wire DTOs may change for clean alignment; affected callers are
  migrated in this story and no Gremlin backward-compatibility shim is required.

## Non-goals

- Running JGraphT or generated JVM classes in the browser.
- Replacing React Flow or Dagre.
- Enumerating every elementary cycle.
- Persisting layouts, analysis results, or cycle annotations.
- Changing persistence or graph-mutation semantics; Gremlin API and wire-contract changes are
  allowed when required for clean alignment.
- Adding algorithm endpoints or cycle UI to the SBOM application.
- Introducing a graph-loader framework or JGraphT `GraphImporter` adapter. Any future importer is a
  separate concrete follow-up and must map into `GraphFragment` before analysis.

## Acceptance

- Gremlin and JGraphT consume the same `ResolvedGraphFragment` and create independent native graphs.
- Multiple graph selections can be composed before materialization without frontend conflict logic.
- SBOM `BomUnion` and asset-repository composition resolve assembled entities and edges through
  `GraphFragmentPolicy` before analysis, native materialization, or graph mutation handoff.
- Example consumers and their tests are migrated to the resolved-fragment contract; breaking
  application-facing changes are acceptable when they remove redundant composition abstractions.
- UUID identity, duplicate handling, conflict diagnostics, and dangling-edge behavior are policy
  controlled; graph output is deterministic but diagnostic entry ordering is unspecified.
- Fragment normalization does not require ID assignment; application policies and frontend-facing
  adapters decide how ID-less values are handled.
- Error diagnostics are returned with normalized candidates, while native materializers reject
  error-bearing results.
- Initial-input ownership and any snapshot/isolation strategy are chosen by the application; graph
  output ordering remains deterministic.
- The consuming application owns live/pinned graph context and supplies it only where needed for
  selection or presentation.
- JGraphT supports directed self-loops and parallel edges for custom JVM iterations.
- Generated typed builders and read views can participate through canonical entities and edges.
- The optional service advertises available algorithms and materialization modes.
- The workbench hides unavailable algorithms and highlights returned entity/edge IDs when available.
- No JGraphT or generated application type appears in browser assets or REST JSON.
- Affected Gremlin callers and result consumers are migrated to the aligned contract; Gremlin
  backward compatibility is not required.
