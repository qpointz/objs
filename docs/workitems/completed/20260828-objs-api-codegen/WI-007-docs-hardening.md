# WI-007 — Documentation, compatibility, and hardening

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 9 — Documentation and hardening
**Status:** completed
**Depends on:** WI-009

## Goal

Document the schema-agnostic foundation contract and application-local generated graph contract,
then close the implementation risks that could make generated entities, edges, or navigable views
misleading or unsafe to use.

## Scope

- Document `objs-api` dependency and package boundary
- Document the root `objs-*` foundation versus `examples/*` application layering
- Document jsonschema2pojo versus second-generator responsibilities
- Document that generated DTOs, nodes, relations, and catalogs are owned only by the consuming app
- Document write DTO versus linked read DTO profiles
- Document typed exact relations and dynamic wildcard relations
- Document schema/relation-level Java naming overrides, recognized tags, and current-name fallbacks
- Document `codegen.baseClass` and `codegen.interfaces`, including their generated-class scope and
  validation rules
- Document wildcard relation `codegen.baseClass` requirements and runtime-only fallback behavior
- Document mutation construction and server-side validation
- Document opt-in, non-blocking prevalidation and explicit inspection operations
- Document non-blocking edge-property reference diagnostics and generic-property fallback
- Document `SCHEMA` versus `NONE` relation property behavior, including empty-property rules
- Document conventional Java payload/mutation construction and the absence of staged builders
- Document UUID-only identity, provisional ID assignment, duplicate rejection, and the absence of
  business-field deduplication
- Document the in-memory typed graph view and collection/navigation semantics
- Document lossless snapshot reads across schema evolution, exact-version hydration, and raw
  fallback behavior
- Document optional lazy use of an existing schema registry/provider without introducing a new
  registry abstraction
- Document collection navigation, singular `1:1` ambiguity behavior, generic `edges(...)`
  traversal, and the `ObjsException` hierarchy
- Document common node handles, separate read/write capabilities, and optional application-owned
  combined facades
- Add collision, dangling edge-property reference, malformed-manifest, and dangling-view-data tests
- Clarify cardinality metadata versus persist-time enforcement
- Resolve or explicitly defer all story-local gaps
- Correct historical codegen documentation where it conflicts with shipped behavior

## Out of scope

- Story closure or archival
- New persistence semantics
- New REST endpoints
- Generated aggregate materializer

## Implementation evidence

- Added the generated API contract and consumer lifecycle documentation to the codegen module and
  both standalone examples.
- Added [`docs/design/graph/codegen-and-builder.md`](../../../design/graph/codegen-and-builder.md)
  as the dedicated reference for all codegen stages, generated artifacts, relation policies, and
  Java usage examples.
- Documented the distinction between jsonschema2pojo payload DTOs, generated write nodes and
  mutation builders, generated read nodes and views, and identity-only references.
- Documented application-owned composite-build integration, caller-supplied Jackson configuration,
  exact relation generation, `NONE`/`SCHEMA` edge-property behavior, and raw snapshot access.
- WI-009's intentionally unverified policy, wildcard, override, and schema-evolution scenarios are
  retained as explicit hardening work rather than treated as silently complete.
- Existing exporter, generator, API, and consumer tests cover symbol collisions, malformed manifests,
  dangling edge references, dangling endpoints, duplicate UUIDs, and ambiguity diagnostics.

## Acceptance

- [x] Design and user documentation describe the final generated contract
- [x] Existing codegen READMEs distinguish payload, linked, graph-write, and graph-view exports
- [x] The intentional breaking `BoM` rename and wire/storage-name preservation are documented
- [x] The object model's non-blocking validation policy and strict object-store boundary are
  documented
- [x] Edge-property reference drift is documented as diagnostic/readable rather than a construction
  blocker
- [x] Generated property-policy signatures and property-free `NONE` edge creation are documented
- [x] Unknown, dangling, obsolete, and historical snapshot data is documented as readable rather
  than silently discarded
- [x] Cardinality and generic relation traversal behavior is documented, including ambiguity errors
- [x] Read/write capability boundaries and the absence of implicit persistence are documented
- [x] Failure messages identify the offending schema, relation, node, or symbol
- [x] Override precedence, invalid values, and collision failures are documented
- [x] `GAPS.md` contains no unresolved blocker without an explicit deferral
- [x] Full relevant unit and integration test commands are documented

