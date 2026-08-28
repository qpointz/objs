# WI-006 — In-memory typed graph view and navigation

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 7 — In-memory typed read view
**Status:** completed
**Depends on:** WI-005

## Goal

Add the schema-agnostic read-side counterpart to the mutation builder: an immutable in-memory view
that accepts raw `Graph` / `GraphContents` values and exposes application-generated typed nodes and
navigable collections without persistence.

## Scope

- Define generic `TypedGraphView.from(rawGraph, generatedCatalog)` contracts in `objs-api`
- Define application-generated type bindings that map exact `(type, schemaVersion)` values to payload
  DTOs and node adapters; the generated catalog remains application-owned
- Pass the consuming application's configured Jackson mapper or payload codec into hydration
- Accept optional access through an exact-version lookup callback or adapter; the consuming
  application may adapt a persistence registry, but `objs-api` never depends directly on it
- Keep pure in-memory operation available through a preloaded immutable schema snapshot; raw reads
  never require the callback
- Index entities by UUID and edges by source/role and target/role
- Define a Java-compatible read-only `TypedCollection<T>` / node collection API
- Generate typed root collections such as `view.products()`
- Generate read-capability accessors such as `ProductReadNode.getContainsComponents()`
- Generate `1:1` singular convenience accessors that return empty/null for zero matches and throw
  `AmbiguousRelationException` for multiple matches
- Generate inverse accessors when linked relation metadata is available
- Preserve edge properties through a relation-edge view
- Expose a generic `edges(...)` relation query for every node that returns all matching edge views
  by role/selector, including properties and raw/unresolved endpoints
- Ensure inverse navigation reuses the original directed edge and never creates a synthetic reverse edge
- Preserve every supplied entity and edge when schemas evolve, including unknown types, missing
  endpoints, schema drift, and edges invalid under the current catalog
- Provide generic raw-node/raw-edge fallbacks when an exact historical adapter is unavailable
- Keep view construction non-blocking by default; expose payload/edge prevalidation and drift
  diagnostics as an explicit inspection operation
- Keep the view detached from object-store, REST, Spring, and persistence APIs
- Keep generated node classes, relation bindings, and catalogs out of all root `objs-*` modules

## Stop / review gate

Do not start WI-009 until lossless raw fallback, exact-version hydration, immutable navigation, and
the no-implicit-persistence boundary have been reviewed. Stop if a schema lookup is required for
basic reads or if a persistence-backed callback becomes an `objs-api` dependency.

## Out of scope

- Loading directly from a database or REST endpoint
- Persisting or mutating through the read view
- Recursive aggregate materialization
- New allow-list or cardinality enforcement

## Implementation evidence

- Extended `:objs-codegen-java` with application-owned `GeneratedReadView` and `<Type>ReadNode`
  facades over the schema-agnostic `TypedGraphView`.
- Generated roots expose typed collections such as `products()`, while typed nodes expose generic
  edge queries, relation-edge collections, typed outbound/inbound navigation, and `1:1` singular
  accessors backed by the API ambiguity check.
- Exact `(type, schemaVersion)` bindings use the caller-supplied `PayloadMapper`; unknown,
  historical, and dangling data remains available through the raw API and relation-edge views.
- Generated read navigation is detached from mutation and persistence APIs. Tests compile and load
  the generated classes, build a graph through the generated mutation builder, and navigate it
  through the generated read facade.

## Acceptance

- [x] A raw graph set becomes an in-memory typed view without persistence access
- [x] `view.products()` returns typed product nodes
- [x] Product navigation resolves only matching graph edges and returns typed component nodes
- [x] `1:1` singular accessors report ambiguity instead of silently selecting a target
- [x] Generic relation queries return every matching edge, including edge properties and unresolved
  endpoints
- [x] Inverse navigation follows the same edge without creating duplicate reverse edges
- [x] Callers can inspect edge properties when using a relation-edge collection
- [x] Read collections are immutable and do not alter the source graph
- [x] Unknown or dangling data follows the documented strict/lenient policy
- [x] Optional inspection diagnostics do not prevent creation of the in-memory view by default
- [x] Snapshot data remains readable after schema evolution without dropping entities or edges
- [x] Missing historical adapters produce a readable raw fallback
- [x] Optional schema-registry access is lazy and does not prevent raw snapshot reads
- [x] Java and Kotlin callers can navigate the view
- [x] Read nodes expose navigation without exposing mutation or persistence operations
- [x] A persistence-backed registry is usable only through an application-owned adapter/callback
- [x] A preloaded immutable schema snapshot supports fully in-memory operation
- [x] The read-view checkpoint is explicitly accepted before WI-009 begins

