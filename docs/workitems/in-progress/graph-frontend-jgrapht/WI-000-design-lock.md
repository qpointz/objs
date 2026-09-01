# WI-000 — Lock graph-fragment contract and story boundaries

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 0 — Contract and boundaries  
**Status:** complete  
**Depends on:** none

## Goal

Record the decisions that all implementation WIs must follow, preventing frontend concerns,
JGraphT-specific types, generated application classes, and persistence conflict semantics from
leaking across boundaries.

## Scope

- Confirm `:objs-jgrapht-core` and `:objs-jgrapht-service` module names and dependency direction.
- Confirm capability and cycle endpoint paths and their absence semantics.
- Define `GraphFragment`, `ResolvedGraphFragment`, `GraphFragmentDiagnostic`, and
  `GraphFragmentPolicy` in `objs-api`.
- Confirm that `GraphContents` implements the structural `GraphFragment` contract.
- Define UUID identity, missing-ID, duplicate, conflict, and dangling-endpoint behavior. Absent IDs
  are valid in resolved fragments; assignment, retention, or diagnostics belong to the selected
  policy. Frontend-facing adapters still require usable IDs, while dangling endpoints are rejected
  with error diagnostics by default.
- Define how multiple graph selections compose before materialization.
- Define the current `GENERIC` materialization mode and the optional `JGraphTGraphFactory<V, E>`
  JVM extension; neither typed/custom construction nor the factory is required by REST/workbench
  consumers.
- Lock directed SCC cycle semantics, deterministic ordering, and result limits.
- Record that frontend code receives DTOs only and never performs identity/conflict resolution.
- Define diagnostics as severity (`INFO`, `WARNING`, `ERROR`), free-text message, and source
  `nodes`/`edges` UUID lists with no ordering guarantee; also define partial-output behavior.
- Record that cloning, immutability, and ownership of the initial input are application-level
  decisions, not `GraphFragmentPolicy` responsibilities.
- Define semantic duplicate equality and confirm that `Edge.graphId` is provenance rather than
  identity; ambiguous provenance is cleared on the resolved edge.
- Keep graph ID, graph version, matcher, source-graph selection, and other container context outside
  the fragment; the consuming application/service owns it.
- Define the distinction between persistence-backed cross-graph UUID union and
  caller-supplied/composite fragment policy resolution without changing existing REST selection
  behavior.
- Choose unweighted `DirectedPseudograph` as the simplest default directed graph structure that
  preserves self-loops and parallel edges, and define the graph lifecycle/ownership contract.
- Keep specialized graph structures and caller-supplied construction logic behind the optional
  factory; the default materializer remains the required implementation.
- Define deterministic SCC component IDs using the smallest internal entity UUID under unsigned
  128-bit (RFC 4122 byte) order and lock the capability/cycle endpoint DTO shapes.
- Exclude JGraphT `GraphImporter` adapters from this story; any future file/import adapter is a
  separate concrete follow-up and must map into `GraphFragment` before analysis.

## API decision

The public shape is one policy operation:

```text
GraphFragment(entities, edges)
    → GraphFragmentPolicy.resolve(...)
    → ResolvedGraphFragment(entities, edges, diagnostics)
```

`GraphFragment` is a structural interface implemented by `GraphContents`. Existing `Graph` and
`GraphMutation` remain existing write-side forms. Identity and conflict behavior are
inside the selected policy; no separate identity-policy, conflict-policy, resolver, or source
hierarchy is introduced.

## Boundaries

- `objs-api` is Kotlin/JVM, Java-compatible, Spring-free, persistence-free, and owns only the
  fragment contract and diagnostic value types.
- `objs-jgrapht-core` is Spring-free and may expose JGraphT graph types through its JVM API for
  custom application iterations; those types remain absent from REST JSON and browser assets.
- `objs-jgrapht-service` owns Spring HTTP wiring and depends on `objs-core` for graph selection.
- Gremlin and JGraphT consume the same resolved fragment but create different native graphs.
- Generated typed classes remain application-owned.
- React Flow and Dagre remain browser-only rendering dependencies.
- Existing persistence `GraphMergePolicy` remains separate from fragment normalization.

## Acceptance

- [x] The story folder and ordered WI tracker exist under the story directory.
- [x] Module and dependency boundaries are documented without a base-service dependency on the
      optional algorithm service.
- [x] The fragment policy shape and relationship to `GraphContents` are explicit.
- [x] Default UUID/conflict/dangling-edge behavior is explicit.
- [x] Generic/typed materialization and directed SCC semantics are explicit.
- [x] Diagnostics, initial-input ownership, graph context, selection distinction, and JGraphT
      lifecycle are explicit.
- [x] Component IDs and endpoint DTO shapes are explicit.
- [x] Importer scope is explicit.
- [x] No implementation WI contains an unresolved architecture choice from this WI.

## Out of scope

- Production code, Gradle module creation, REST controllers, UI changes, or JGraphT dependency
  changes.
- Creating a GitLab issue or branch; those are story-delivery operations, not this documentation
  scaffold.
