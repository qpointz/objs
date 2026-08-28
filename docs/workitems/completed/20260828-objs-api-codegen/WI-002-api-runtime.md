# WI-002 — Extract and rename the schema-agnostic graph runtime

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Kernel extraction and public rename
**Status:** completed
**Depends on:** WI-001
**Implementation commit:** [`200367c`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/200367c)

## Goal

Make the schema-agnostic entity, edge, graph, typed-wrapper, graph-builder, mutation-construction,
and read-view primitives available from `objs-api`, while keeping server-bound behavior in the root
foundation modules. No application schema or generated class is introduced by this WI.

This is the story's first intentionally destructive stage: the public `BoM` rename and package
migration are applied without compatibility aliases. It must be implemented in reviewable
sub-checkpoints and may stop before the next stage.

## Scope

- Move or re-home the entity / edge / graph / graph-mutation model and remove the public `BoM` prefix
- Move mutation halves and mutate mode under the unprefixed API names
- Move or re-home `TypedEntity`, `TypedEdge`, `EntityTypeMeta`, and relation metadata
- Add Java-compatible typed references such as `EntityRef<P>`
- Move/adapt `GraphBuilder` and mutation helpers
- Move/adapt the generic `TypedGraphView` runtime and its adapter contracts without generated node
  classes
- Define Jackson-backed payload conversion with a caller-supplied configured mapper or codec;
  `objs-api` must not hide mapper configuration in a singleton or default instance
- Define null and unknown-property behavior at the supplied mapper/codec boundary
- Keep runtime object-model construction non-blocking by default; expose optional explicit
  inspection/prevalidation diagnostics without replacing the strict object-store persist gate
- Assign fresh provisional UUIDs for ID-less nodes, treat UUID as the only identity, and reject
  duplicate entity or edge UUID registration without deduplicating payload/business fields
- Provide common node identity/payload abstractions that can be adapted to separate read-navigation
  and write-mutation capabilities without coupling either capability to persistence
- Update all root `objs-*` modules and example consumer imports
- Keep schema/catalog authoring, generated catalogs, and application ontology classes out of the API
- Execute sub-checkpoints in this order: raw entity/edge/graph types, mutation types/builders, typed
  wrappers/relation metadata, then read-view contracts
- After each sub-checkpoint, compile affected consumers and record the result before continuing
- Require explicit review of the complete source/binary break before WI-008 begins

## Out of scope

- Entity or edge persistence behavior changes
- New validation rules
- Generated entity, node, relation, or catalog bindings
- REST endpoint changes

## Acceptance

- [x] Root foundation modules depend on `objs-api` without a dependency cycle
- [x] Existing persistence and validation behavior is unchanged
- [x] New entities can receive provisional IDs before edge construction
- [x] Duplicate entity/edge UUIDs are rejected while distinct payloads are never merged implicitly
- [x] Read and write capabilities can be composed or used independently without implicit persistence
- [x] Java and Kotlin callers can construct graphs and MERGE/REPLACE mutations
- [x] Runtime APIs expose no Spring types or application-specific schemas
- [x] No generated application source is added to a root `objs-*` module
- [x] Core, service, Gremlin, and example tests pass after package migration
- [x] The old `BoM` public aliases are not retained, as required by WI-000
- [x] Wire fields, REST paths, JPA names, and database identifiers are unchanged
- [x] Each extraction sub-checkpoint has a separately reviewable, passing state
- [x] The complete rename is explicitly accepted before the next WI starts

## Checkpoint evidence

- Raw entity, edge, graph, and relation metadata extraction compiled with `:objs-core:compileKotlin`.
- Mutation and typed-wrapper extraction passed `:objs-core:test`, `:objs-gremlin-core:compileKotlin`,
  `:sbom-service:compileKotlin`, and `:asset-repository-service:compileJava`.
- Read-view contracts passed `:objs-api:test`, including Java/Kotlin construction, supplied-mapper
  hydration, immutable snapshot navigation, dangling endpoints, and singular ambiguity.
- The full unit suite passed with `.\gradlew.bat test --no-configuration-cache`.
- `:objs-api` runtime dependencies contain only Jackson, Kotlin stdlib, and transitive Jackson
  annotations/core; no Spring, JPA, or persistence dependencies.

