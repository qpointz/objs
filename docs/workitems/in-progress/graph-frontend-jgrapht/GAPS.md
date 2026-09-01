# Gaps — graph fragments, JGraphT analysis, and workbench support

Track unresolved decisions and implementation risks for this story. Promote open items into the
relevant WI before that WI starts.

## Open

| ID      | Topic                        | Status | WI             | Notes                                                                                                                                 |
| ------- | ---------------------------- | ------ | -------------- | ------------------------------------------------------------------------------------------------------------------------------------- |

## Resolved

| ID | Topic | Decision |
|---|---|---|
| G-JG-R1 | Structural fragment | `GraphFragment` is the `(entities, edges)` interface. Existing `GraphContents` implements it; `ResolvedGraphFragment` implements it and adds diagnostics. |
| G-JG-R2 | Policy layering | One `GraphFragmentPolicy.resolve(fragment)` operation owns identity, missing-ID, duplicate, conflict, and dangling-endpoint behavior. No separate resolver/source/identity/conflict hierarchy. |
| G-JG-R3a | Diagnostic shape | Each `GraphFragmentDiagnostic` has `severity` (`INFO`, `WARNING`, or `ERROR`), free-text `message`, `nodes: List<UUID>`, and `edges: List<UUID>`. Diagnostic entry ordering is unspecified. |
| G-JG-R3 | Error output | Policy returns normalized candidates together with diagnostics. Native materializers reject results containing error diagnostics. |
| G-JG-R4 | Missing IDs | Absent entity/edge IDs are valid in both fragment types. ID assignment, retention, or diagnostics belong to the selected policy; frontend-facing adapters still require usable IDs. |
| G-JG-R5 | Dangling endpoints | Dangling endpoints produce error diagnostics and are rejected by default; graph output ordering remains deterministic, but diagnostic entry ordering is unspecified. |
| G-JG-R6 | Edge provenance | Any existing `Edge.graphId` value is optional source metadata, not fragment/container context or identity. Semantic equality ignores it, timestamps, and version counters. Equivalent records with ambiguous provenance resolve with `graphId == null`. |
| G-JG-R7 | Duplicate equality | Entity equality uses UUID, type/schema, payload, and annotations. Edge equality uses UUID, endpoints, role, type/schema, and properties, excluding provenance and bookkeeping fields. |
| G-JG-R8 | Typed JVM exposure | TYPED materialization is an optional JVM path through the caller-supplied `JGraphTGraphFactory<V, E>`; it is not required by the default or REST path. JGraphT and generated types remain absent from REST JSON and browser assets. |
| G-JG-R9 | Frontend boundary | The browser consumes capability and analysis DTOs only. It does not construct fragments or resolve identity/conflicts. |
| G-JG-R10 | Module split | `objs-api` owns the fragment contract; `objs-jgrapht-core` owns JVM JGraphT algorithms; `objs-jgrapht-service` is optional Spring HTTP wiring; Gremlin remains separately materialized. |
| G-JG-R11 | Input ownership | Cloning, immutability, and snapshot isolation belong to the initial-input producer/application. `GraphFragmentPolicy.resolve(...)` only resolves supplied nodes and edges and makes no deep-copy guarantee. |
| G-JG-R12 | Persistence-backed union | `GraphStore.selectAcrossGraphs` is an existing persistence union: entities are globally unique pool records and edges have globally unique IDs with one graph owner. Its UUID-keyed deduplication is not the general conflict-policy boundary; caller-supplied/composite fragments use `GraphFragmentPolicy`. |
| G-JG-R13 | Optional graph construction | `objs-jgrapht-core` exposes an optional `JGraphTGraphFactory<V, E>` above `ResolvedGraphFragment`. It creates the native graph and its vertex/edge values; the default materializer remains usable without it. The factory is not a policy or REST requirement, and generated classes remain application-owned. |
| G-JG-R14 | Workbench-first DTO | The stable v1 workbench contract is `GraphAlgorithmCapabilities`, `GraphAlgorithmCapability`, `GraphCycleAnalysis`, `GraphCycleComponent`, and `GraphAnalysisStats`. Required cycle fields are algorithm ID, component ID, entity UUIDs, edge UUIDs, and counts. Diagnostics and other consumer-oriented fields are additive best-effort data; no JGraphT or generated types appear on the wire. |
| G-JG-R15 | Gremlin alignment and migration (resolves G-JG-10) | Gremlin and JGraphT consume the same `ResolvedGraphFragment`; the Gremlin materializer, entry points, result projection, and wire envelope may change when required for clean alignment. Existing Gremlin backward compatibility is not a constraint; affected callers and tests are migrated in this story. |
| G-JG-R16 | Example composition migration (resolves G-JG-12) | SBOM `BomUnion` and asset-repository composition must build a `GraphFragment` and apply a `GraphFragmentPolicy`, rejecting error-bearing results. The SBOM union replaces topology-key/first-seen deduplication; asset-repository object identity and persistence write policies remain domain concerns before/after fragment resolution. Example-facing APIs may change and compatibility shims are not required; affected callers and tests are migrated in this story. No SBOM algorithm endpoint or UI migration is included. |
| G-JG-R17 | Graph context ownership (resolves G-JG-07) | `GraphFragment` and `ResolvedGraphFragment` contain only entity and edge collections plus diagnostics; they do not own graph ID, graph version, matcher, source-graph selection, or other container context. Intrinsic entity/edge UUIDs remain part of the records. The consuming application/service owns selection context and may use it to produce the fragment or shape its own HTTP request/response. |
| G-JG-R18 | Internal UUID SCC IDs (resolves G-JG-09) | Each cyclic component ID is the smallest internal entity UUID in that component, using unsigned 128-bit (RFC 4122 byte) UUID order and serialized as a UUID. Entity UUIDs and internal edge UUIDs are sorted in the same order; component order is deterministic and independent of native graph iteration. No graph context or synthetic component UUID is introduced. |
| G-JG-R19 | Default graph structure (resolves G-JG-06) | The concrete default materializer in `objs-jgrapht-core` uses JGraphT’s unweighted `DirectedPseudograph`, the simplest structure that supports the story’s directed self-loops and parallel edges. It creates and populates an in-memory graph and returns it as caller-owned mutable state. There is no close/dispose operation. Callers may optionally replace graph and element construction through `JGraphTGraphFactory`. |

## Deferred

| ID | Topic | Status | Notes |
|---|---|---|---|
| G-JG-D1 | General graph loader framework | deferred | File/importer loading is not a foundation abstraction. Concrete importer adapters may be added after the core fragment contract is stable. |
| G-JG-D2 | SBOM graph UI migration | deferred | The existing SBOM UI remains a separate consumer; this story does not consolidate its canvas or add cycle actions. |
| G-JG-D3 | Elementary-cycle enumeration | deferred | The first algorithm reports directed SCC cycle regions, not every elementary cycle. |

## Cancelled

| ID | Topic | Status | Notes |
|---|---|---|---|
| G-JG-11 | JGraphT importer scope | cancelled | Standard `GraphImporter` loading is eager and in-memory and is not required by this story. Any future file/import adapter belongs to G-JG-D1 and must enter through `GraphFragment` before analysis. |
