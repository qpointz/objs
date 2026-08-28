# WI-005 — Generated schema-aware mutation builder

**Story:** [`STORY.md`](STORY.md)
**Stage:** 6 — Generated mutation builder
**Status:** completed
**Depends on:** WI-004

## Goal

Add the generated write-side behavior on top of the stable Java bindings. The generated builder
must make allowed entity and edge operations convenient while keeping linked properties out of the
write payload and leaving strict acceptance to the object store.

## Scope

- Generate `add<Type>(Pojo)` methods that wrap payload POJOs into typed nodes internally
- Bind entity methods to fixed `(type, schemaVersion)` values
- Generate ordinary Java mutation methods for MERGE and REPLACE construction
- Generate exact relation methods only for allowed source/role/target triples
- Create explicit `Edge` values separate from entity payloads
- Apply schema-aware `SCHEMA` and authoritative `NONE` edge-property signatures
- Preserve caller-supplied Jackson configuration without creating a hidden mapper
- Assign provisional UUIDs when absent and reject duplicate entity/edge UUID registration
- Keep equal payloads with different UUIDs as distinct nodes
- Provide explicit, non-blocking inspection/prevalidation diagnostics
- Keep wildcard relations dynamic when no safe static binding was generated

## Stop / review gate

Do not start WI-006 until the generated builder has been reviewed independently from read
navigation. Stop if a disallowed relation is constructible through the typed API, linked payload
data is serialized as an edge, property policy is wrong, or builder construction implicitly persists.

## Out of scope

- In-memory read navigation
- Direct database or REST access
- Persist-time validation or cardinality enforcement
- Recursive linked-object materialization
- Generated HTTP clients

## Implementation evidence

- Extended `:objs-codegen-java` to emit an application-owned `GraphMutationBuilder`.
- Generated `add<Type>(Pojo)`, `add<Type>(UUID, Pojo)`, and node-reuse methods assign provisional
  UUIDs and reject duplicate entity UUIDs.
- Exact non-wildcard relation methods emit separate API `Edge` values, with `NONE` producing bare
  edges and `SCHEMA` accepting the resolved edge-property DTO or generic map fallback.
- The generated builder accepts `MERGE` / `REPLACE`, exposes explicit diagnostics, preserves the
  caller-supplied `PayloadMapper`, and performs no persistence or remote validation.
- Generator tests compile the generated builder against `objs-api` and verify deterministic output.

## Acceptance

- [x] Callers pass generated POJOs directly without constructing typed entity wrappers
- [x] Generated entity methods fix the expected type and schema version
- [x] Only allowed exact relation methods are generated
- [x] Generated relations create separate edges
- [x] `NONE` creates a bare edge without a property argument
- [x] `SCHEMA` exposes the configured property input and empty-property behavior
- [x] MERGE and REPLACE mutations can be constructed and serialized
- [x] Duplicate UUID registration is rejected and equal payloads do not deduplicate
- [x] Explicit diagnostics do not block default object-model construction
- [x] No operation implies persistence or requires Spring
- [x] The mutation-builder checkpoint is explicitly accepted before WI-006 begins
