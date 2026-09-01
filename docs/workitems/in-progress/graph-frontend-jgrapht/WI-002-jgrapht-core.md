# WI-002 — Add JGraphT core and align Gremlin materialization

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Native graph engines  
**Status:** complete  
**Depends on:** WI-001

## Goal

Add a Spring-free JGraphT implementation and make Gremlin and JGraphT consume the same
`ResolvedGraphFragment`, so native graph differences do not create different identity or topology
semantics.

## Scope

- Create `:objs-jgrapht-core` as a Java-compatible, Spring-free, persistence-free module.
- Add the JGraphT dependency through the repository version catalog.
- Expose the JVM JGraphT graph API needed for custom application iterations; keep JGraphT types
  out of REST DTOs and browser assets.
- Use JGraphT's unweighted `DirectedPseudograph` as the simplest default graph structure that
  supports self-loops and parallel edges. The default materializer creates and populates it, then
  returns caller-owned mutable state; no graph closure or separate lifecycle operation is needed.
- Add stable algorithm identifiers, capability descriptors, the current `GENERIC` materialization
  mode, and implementation-neutral cycle result DTOs.
- Materialize `GENERIC` graphs from resolved entity/edge fragments.
- Expose optional JVM typed/custom construction through one `JGraphTGraphFactory<V, E>` above the
  resolved fragment; the default materializer and REST/workbench path must not require it.
- Keep the factory responsible only for native graph, vertex, and edge construction. It must not
  perform selection, normalization, identity resolution, or conflict handling.
- Use the direct optional factory shape:
  ```kotlin
  interface JGraphTGraphFactory<V, E> {
      fun createGraph(): Graph<V, E>
      fun createVertex(entity: Entity): V
      fun createEdge(edge: Edge, source: V, target: V): E
  }
  ```
- Align `:objs-gremlin-core` materialization to accept the same resolved fragment.
- Migrate affected Gremlin materialization, traversal entry points, result projection, and wire
  DTOs to the aligned contract; breaking changes are allowed and compatibility shims are not
  required.
- Preserve entity UUIDs, edge UUIDs, directed source/target endpoints, roles, self-loops, parallel
  edges, type/schema metadata, payloads, annotations, and edge properties.
- Add shared fixtures and cross-engine parity assertions.
- Add directed SCC cycle-region analysis:
  - multi-node SCCs are cyclic;
  - singleton SCCs are cyclic only with a self-loop;
  - internal edges are returned;
  - component, entity, and edge ordering is deterministic;
  - each component ID is the smallest internal entity UUID in that component under unsigned
    128-bit (RFC 4122 byte) UUID order.
- Define component IDs from internal entity UUIDs rather than native object identity, synthetic
  identifiers, or graph context; sort UUIDs using unsigned 128-bit (RFC 4122 byte) order.
- Define deterministic handling and diagnostics for malformed or dangling records.

## Expected touchpoints

- `settings.gradle.kts` / root module registration
- `libs.versions.toml`
- new `objs-jgrapht-core/build.gradle.kts`
- new `objs-jgrapht-core/src/main/kotlin/...`
- `objs-gremlin-core/src/main/kotlin/org/poc/objs/gremlin/core/GremlinEngine.kt`
- `objs-gremlin-core/src/main/kotlin/org/poc/objs/gremlin/core/materialize/`
- `objs-gremlin-core/src/main/kotlin/org/poc/objs/gremlin/core/GremlinResultProjector.kt`
- shared and engine-specific tests

## Tests

- empty and acyclic graphs;
- one self-loop;
- two-node and larger cycles;
- overlapping cycles collapsing into one SCC;
- deterministic component IDs using the smallest internal entity UUID under the locked UUID order;
- parallel edges inside and outside cyclic components;
- multiple graph fragments with shared UUIDs;
- conflicting records and deterministic diagnostics;
- dangling endpoints;
- generic and typed materialization parity;
- default graph construction and direct JVM graph iteration;
- optional caller-defined typed vertex/edge and graph construction;
- graph lifecycle, self-loop, and parallel-edge behavior;
- Gremlin and JGraphT vertex/entity and edge ID parity;
- directed endpoint, role, metadata, and payload preservation;
- Kotlin and Java API behavior after the aligned contract migration.

## Acceptance

- [x] `:objs-jgrapht-core` builds without Spring or persistence dependencies.
- [x] The same `ResolvedGraphFragment` produces equivalent Gremlin and JGraphT topology.
- [x] JGraphT graph types are available to JVM callers for custom iterations but do not appear in
      REST DTOs or browser assets.
- [x] `GENERIC` is usable without generated bindings and uses the default
      `DirectedPseudograph`.
- [x] SCC cycle regions, self-loops, parallel edges, and graph output ordering are deterministic;
      diagnostic entry ordering is not required.
- [x] The default JGraphT graph type and ownership/closure contract are documented and tested.
- [x] Callers may optionally supply `JGraphTGraphFactory` for typed/custom construction, without
      making it required for default, REST, or workbench materialization.
- [x] Affected Gremlin callers, traversal entry points, and result consumers are migrated to the
      aligned `ResolvedGraphFragment` contract; no backward-compatibility shim is required.
- [x] Focused API, Gremlin, JGraphT, and parity tests pass.

## Out of scope

- Spring controllers or capability HTTP discovery.
- Browser graph rendering.
- Persistence schema or graph mutation changes.
