# WI-001 — Add shared graph-fragment normalization and typed/codegen adapters

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Shared fragment foundation  
**Status:** complete
**Depends on:** WI-000

## Goal

Make one framework-free fragment contract usable by raw graph values, persistence-selected graph
contents, and generated typed code without introducing a second entity/edge model.

## Scope

- Add `GraphFragment` to `objs-api` as the structural `(entities, edges)` contract.
- Make `GraphContents` implement `GraphFragment`; update affected REST and example consumers to
  the clean fragment contract without requiring compatibility wrappers.
- Add `ResolvedGraphFragment` and structured `GraphFragmentDiagnostic` values.
- Add the single `GraphFragmentPolicy.resolve(fragment)` extension point.
- Return normalized candidates together with diagnostics; consumers decide whether error diagnostics
  make the result unusable, and native materializers reject error-bearing results.
- Define diagnostic severity (`INFO`, `WARNING`, `ERROR`), free-text messages, source `nodes` and
  `edges` UUID lists, and no ordering guarantee for diagnostic entries.
- Define semantic duplicate equality over graph fields, excluding timestamps, version counters, and
  `Edge.graphId` provenance; clear edge provenance when equivalent records disagree on its value.
- Do not make `GraphFragmentPolicy.resolve(...)` responsible for cloning or isolation. Document
  that the initial-input producer/application chooses ownership and immutability strategy.
- Implement the default UUID-based policy:
  - preserve UUID identity when present;
  - permit absent IDs in resolved output; ID assignment, retention, or diagnostics are selected
    policy behavior;
  - deterministically deduplicate identical entity/edge records;
  - report conflicting records instead of silently selecting first/last;
  - reject dangling endpoints with deterministic error diagnostics by default.
- Keep graph output ordering deterministic; diagnostic entry ordering is intentionally unspecified.
- Add conversion from mutable `Graph` and the set portion of `GraphMutation`; update callers to
  consume the resolved-fragment contract.
- Add typed adapters after `TypedEntity.toEntity` / `TypedEdge.toEdge` conversion.
- Add resolved-fragment entry points or adapters for `TypedGraphView` and generated
  `GeneratedReadView`.
- Ensure generated typed code remains application-owned and Java-compatible.
- Migrate SBOM `BomUnion.of(...)` to compose selected graph contents as a `GraphFragment` and
  resolve it through the selected `GraphFragmentPolicy`; its return type and affected callers may
  change, while tag helpers remain separate.
- Migrate the asset-repository composition path to resolve its assembled entities and relations
  through a `GraphFragmentPolicy` before graph mutation or native materialization. Collection
  `object_write_mode` identity lookup and persistence write validation remain domain concerns; the
  composition API may change without a compatibility shim.
- Treat `GraphStore` cross-graph selection as an existing persistence-defined UUID union over
  globally unique records. Apply `GraphFragmentPolicy` to caller-supplied/composite fragments
  and migrate affected REST selection consumers as needed for the clean contract.
- Exercise the changed API through the existing typed/codegen consumer path and the SBOM backend
  builder where required by the public API change. Do not add SBOM algorithm endpoints.

## Important constraint

Typed `NodeRef` values capture UUID endpoints when they are created. Policy execution must therefore
occur after a complete typed fragment is assembled, unless an application supplies IDs before
creating typed references. Any custom ID reassignment after assembly must remap entity IDs and all
edge endpoints atomically.

## Expected touchpoints

- `objs-api/src/main/kotlin/org/poc/objs/api/domain/GraphPrimitives.kt`
- new fragment/diagnostic types under `org.poc.objs.api.domain`
- `objs-api/src/main/kotlin/org/poc/objs/api/typed/GraphBuilder.kt`
- `objs-api/src/main/kotlin/org/poc/objs/api/typed/TypedGraphView.kt`
- `objs-api/src/main/kotlin/org/poc/objs/api/domain/GraphMutation.kt`
- `objs-core/src/main/kotlin/org/poc/objs/core/persistence/GraphStore.kt`
- `objs-codegen-java/src/main/kotlin/org/poc/objs/codegen/java/JavaCodeGenerator.kt`
- `examples/sbom/sbom-service/src/main/kotlin/org/poc/objs/sbom/domain/BomUnion.kt`
- `examples/sbom/sbom-service/src/main/kotlin/org/poc/objs/sbom/service/BomGraphSupport.kt`
- `examples/asset-repository/asset-repository-service/src/main/java/org/poc/objs/assetrepository/service/ObjectWriteService.java`
- generated-consumer, SBOM, and asset-repository tests

## Tests

- empty and ordinary fragments;
- repeated identical records;
- same-UUID conflicting entities and edges;
- multiple graph sources with global UUID union;
- present and absent IDs, with custom policy ID assignment/retention;
- dangling endpoints;
- diagnostic content plus deterministic graph output ordering;
- policy resolution behavior when the caller retains or transfers input ownership;
- Kotlin and Java callers;
- typed write conversion and typed read hydration after normalization;
- SBOM multi-graph union conflict, duplicate, and dangling-endpoint behavior;
- asset-repository composition conflict, relation endpoint, and persistence handoff behavior;
- graph selection and persistence behavior at their explicitly retained domain boundaries.

## Acceptance

- [x] A caller can pass one fragment to the default or a custom policy and receive a resolved
      fragment plus diagnostics.
- [x] `GraphContents` is accepted without copying into a parallel model, and affected consumers
      use the clean fragment contract without compatibility wrappers.
- [x] Multiple graph collections compose by policy rather than frontend or materializer logic.
- [x] Typed generated values enter and leave the same canonical fragment boundary.
- [x] Conflicts cannot be silently resolved by first/last insertion order.
- [x] Diagnostics have the locked machine-readable fields; consumers do not depend on diagnostic
      entry ordering.
- [x] Input ownership and mutability expectations are documented as caller/application decisions,
      not resolver guarantees.
- [x] SBOM `BomUnion` and asset-repository composition use the shared fragment-policy path rather
      than application-local topology-key or first-seen graph deduplication.
- [x] Affected example and REST consumers are migrated; breaking API changes are acceptable where
      they remove redundant composition abstractions.
- [x] API, typed/codegen, and required SBOM consumer tests pass.

## Out of scope

- JGraphT implementation or dependency changes.
- Gremlin materializer changes beyond preparing the common input signature.
- Algorithm endpoints or SBOM cycle visualization.
