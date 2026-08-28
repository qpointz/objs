# Story: Schema-agnostic API and app-local graph codegen

**Slug:** `objs-api-codegen`  
**Branch:** `objs-api-codegen`  
**Status:** completed
**Closed:** 2026-08-28
**Folder:** [`docs/workitems/completed/20260828-objs-api-codegen/`](.)
**Backlog:** [C-23](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Design:** [`docs/design/graph/object-schema-dsl.md`](../../../design/graph/object-schema-dsl.md), [`docs/design/graph/json-schema-to-seeds.md`](../../../design/graph/json-schema-to-seeds.md), [`docs/design/graph/api-and-codegen.md`](../../../design/graph/api-and-codegen.md), [`docs/design/graph/codegen-and-builder.md`](../../../design/graph/codegen-and-builder.md), [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md)
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../RULES.md)

## Goal

Introduce a standalone `:objs-api` Kotlin/JVM module as a schema-agnostic runtime for application
code generated from Objs schemas. Generated payload classes continue to come from JSON Schema and
`jsonschema2pojo`; a reusable, metadata-driven Gradle generator emits typed entity bindings,
references, relation factories, and mutation helpers only into the consuming application.

Every root `objs-*` module remains foundational and schema/ontology-agnostic. Application schemas,
ontology-specific payloads, generated bindings, and generated catalogs belong only under
`examples/*` (or a future consuming application).

The generated write model must represent entities and edges separately. Linked properties remain
read/navigation projections and must never be expected to deserialize into graph edges
automatically.

## Problem

The current generic typed wrappers and graph builder live under `objs-core`, which also contains
Spring, JPA, persistence, catalogs, and validation. That prevents application-generated code from
depending on a small runtime API. The current linked JSON Schema export generates nested entity
properties, but those properties do not carry the UUID endpoint and are not transformed into graph
edge values.

## Normative locks

| Topic | Lock |
|---|---|
| API module | `:objs-api` is Kotlin/JVM, `java-library`, Spring-free, JPA-free, persistence-free, and Java-compatible |
| Root foundation | All root `objs-*` modules are generic and schema/ontology-agnostic |
| API contents | Schema-agnostic graph/write primitives plus generic typed wrappers, references, graph builder, relation metadata, and mutation/read helpers |
| Read API contents | Immutable in-memory graph view and generic adapter contracts; generated node types, root collections, and relation-edge bindings are app-local |
| Dependency direction | Application generated code → `objs-api`; root foundation modules may depend on `objs-api`; `objs-api` never → `objs-core` |
| Package target | New public API package is `org.poc.objs.api.*`; the public rename is intentional and breaking, with no old aliases |
| Payload generation | The consuming application's build runs `jsonschema2pojo` for entity and edge-property data classes |
| Behavior generation | Root `:objs-codegen-java` is a reusable Java-only artifact; it reads an application's Objs relation manifest and writes bindings into that application only |
| Generated language | This story generates Java. A future Kotlin generator is a separate Kotlin-specific module |
| Example build | In-repository examples consume `:objs-api` and `:objs-codegen-java` through Gradle project dependencies, with generation attached to the normal build lifecycle |
| Codegen export | Relation/mutation metadata is emitted only by the JSON-schema-codegen format under root `x-objs-relations`; the existing standard catalog export remains unchanged |
| JSON Schema dialects | 2020-12 uses `$defs`; draft-07 uses `definitions`; all refs and draft-07 `allOf` wrappers remain dialect-correct and jsonschema2pojo-compatible |
| Codegen naming overrides | Codegen-only `x-objs-codegen` metadata accepts schema `codegen.java.typeName`, relation `codegen.java.outboundMethod` / `codegen.java.inboundMethod`, and opt-in `codegen.java.skip` / `codegen.java.noInverse` tags; absent overrides retain current normalization |
| Java inheritance overrides | Schema-level `codegen.baseClass` selects one superclass and comma-separated `codegen.interfaces` selects interfaces for generated entity/payload classes; absent metadata adds no inheritance |
| Wildcard codegen | Relation-level `codegen.baseClass` supplies the common Java type for a wildcard endpoint; without it, that relation receives no static generated binding but remains available through runtime metadata |
| Payload conversion | Jackson is the supported codec; consuming code supplies its configured mapper or codec, and `objs-api` creates no hidden global/default mapper |
| Java construction style | Payloads use conventional jsonschema2pojo JavaBeans/withers; generated mutation/entity/relation APIs use ordinary Java methods, not staged or type-state builders |
| Builder identity | UUID is the only identity; ID-less additions receive fresh provisional UUIDs, payload/business-field equality never deduplicates, and duplicate entity/edge UUID registration is rejected |
| Mutation builder | The generated fluent mutation builder is schema-aware: entity methods select a fixed type/version and payload builder; relation methods select only allowed exact rules |
| Schema-aware checks | Generated APIs may expose explicit prevalidation/inspection diagnostics, but object-model construction and `build()` are not blocked by schema validation by default; strict JSON Schema, relation, endpoint, and persistence validation remains at the object-store boundary |
| Edge-property references | Missing, unknown-version, or wrong-usage edge-property schemas produce explicit diagnostics and generic property handling; they do not block candidate object-model construction |
| Edge property policy | `SCHEMA` relation bindings account for the configured edge-property input and `emptyPropertiesAllowed`; `NONE` is authoritative and creates a property-free edge without a property argument |
| POJO ergonomics | Public mutation methods accept generated payload POJOs and wrap them internally into typed entity nodes; callers do not construct `TypedEntity` manually |
| Relation model | Exact non-wildcard allow-list rules generate typed relation bindings; wildcard rules use a dynamic API |
| Write semantics | Application-generated writes emit separate entities and edges through `GraphBuilder` and `GraphMutation` |
| Read model | `objs-api` provides an immutable in-memory typed graph view that accepts raw graph data and app-supplied adapters |
| Read/write separation | The typed graph view reads a `Graph` / `GraphContents`; it does not persist or mutate and is the read-side counterpart to the mutation builder |
| Navigation | Application-generated node accessors resolve edges by role and direction into typed node collections; relation-property views preserve edge properties |
| Snapshot readability | Reads are lossless and evolution-tolerant: exact `(type, schemaVersion)` adapters are preferred, while missing adapters, unknown types, dangling endpoints, schema drift, and obsolete relations remain available through raw/unresolved fallbacks |
| Snapshot schema access | The read view may receive an existing schema registry/provider through a lightweight exact-version lookup callback or adapter; lookup is lazy and no new `SchemaResolver` abstraction is introduced |
| Cardinality navigation | Collections are available for every relation; `1:1` adds a singular convenience accessor that is empty/null for zero matches and throws `AmbiguousRelationException` for multiple matches |
| Generic relation access | Every node exposes a generic `edges(...)` query returning all matching edge views, including properties and raw/unresolved endpoints |
| API exceptions | `ObjsException` is the root unchecked API exception; `AmbiguousRelationException` extends it |
| Node capabilities | Generated nodes share a common identity/payload handle and expose independent read-navigation and write-mutation capabilities; an application may add a combined facade without implying persistence |
| Linked properties | `includeEdges=linked` is a read/navigation projection only, never the write DTO contract |
| Validation | Generated metadata improves construction-time typing; optional prevalidation is explicitly invoked and non-blocking, while the server persist gate remains authoritative |
| Cardinality | `1:1` / `1:*` remains metadata until persist-time cardinality enforcement is explicitly designed |
| Generated-source location | No generated application class is placed in `objs-api`, `objs-core`, or any other root `objs-*` module |
| HTTP | `objs-api` contains no HTTP client or Spring controller code |

## Stages

Every stage is an independent review and stop point. Each completed WI must be committed and
pushed according to [`docs/workitems/RULES.md`](../RULES.md), but implementation must not start the
next stage until its exit condition has been reviewed. A stage may be stopped without taking any
later-stage changes.

| Stage | Work items | Readiness | Risk / stop gate | Exit condition |
|---|---|---|---|---|
| 0 — Design lock and baseline | WI-000 | Ready after story creation | Stop if ownership, rename mapping, or compatibility policy is unclear | Baseline evidence and the staged execution contract are fixed |
| 1 — API boundary | WI-001 | After WI-000 | Reversible; stop if dependency isolation or Java/Kotlin consumption fails | Empty `objs-api` boundary is consumable and framework-free |
| 2 — Kernel extraction and public rename | WI-002 | After WI-001 | **Destructive source/binary API break**; stop for explicit review before proceeding | Schema-agnostic primitives are in `objs-api`, renamed consumers compile, and server behavior is unchanged |
| 3 — Renamed API stabilization | WI-008 | After WI-002 | Stop if migration fallout, dependency leakage, or capability boundaries are incorrect | The renamed API passes repository-wide migration checks and is safe for generator consumers |
| 4 — Codegen export contract | WI-003 | After WI-008 | Reversible metadata-only change; stop if standard export or either JSON Schema dialect regresses | Codegen-only relation metadata is deterministic and standards-compatible |
| 5 — Generator scaffolding | WI-004 | After WI-003 | Stop if generated sources leak into root modules or symbol policy is unsafe | Reusable Java generator emits compiling application-owned type/reference bindings |
| 6 — Generated mutation builder | WI-005 | After WI-004 | Behavioral checkpoint; stop if allowed-relation or property-policy behavior is wrong | Generated POJO/entity/edge APIs construct schema-aware mutations without persistence |
| 7 — In-memory typed read view | WI-006 | After WI-005 | Stop if reads lose data, perform I/O, or become implicitly mutable | Raw graph sets become lossless, immutable, typed navigable views |
| 8 — Consumer integration | WI-009 | After WI-006 | Stop if a real consuming build needs manual or hidden setup | Example and external-consumer paths exercise generation, mutation, and read behavior |
| 9 — Documentation and hardening | WI-007 | After WI-009 | Final review gate; unresolved blockers remain open | Public contract, failure cases, stage evidence, and deferred gaps are documented |

## Work Items

- [x] WI-000 — Design lock and generated API contract (`WI-000-design-lock.md`) — [`a4ddbb3`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/a4ddbb3)
- [x] WI-001 — `objs-api` module scaffold and dependency boundary (`WI-001-api-module.md`) — [`0962f76`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/0962f76)
- [x] WI-002 — Move generic graph runtime into `objs-api` (`WI-002-api-runtime.md`) — [`200367c`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/200367c)
- [x] WI-008 — Renamed API stabilization and migration gate (`WI-008-api-stabilization.md`) — [`420b54c`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/420b54c)
- [x] WI-003 — Graph-codegen JSON Schema and relation manifest (`WI-003-export-contract.md`) — [`ef54992`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/ef54992)
- [x] WI-004 — Java generator scaffolding and typed bindings (`WI-004-gradle-generator.md`) — [`ef54992`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/ef54992)
- [x] WI-005 — Generated schema-aware mutation builder (`WI-005-mutation-builder.md`) — [`ef54992`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/ef54992)
- [x] WI-006 — In-memory typed graph view and navigation (`WI-006-read-view.md`) — [`ef54992`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/ef54992)
- [x] WI-009 — Consumer integration and end-to-end verification (`WI-009-consumer-integration.md`) — [`ef54992`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/ef54992)
- [x] WI-007 — Documentation, compatibility, and hardening (`WI-007-docs-hardening.md`) — [`ef54992`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/ef54992)

## Dependencies

```text
WI-000
  ↓
WI-001 → WI-002 → WI-008 → WI-003 → WI-004 → WI-005 → WI-006 → WI-009 → WI-007
```

## Generated surface

```text
jsonschema2pojo
├── Product.java                 (example generated source)
├── Component.java               (example generated source)
└── CanonicalEdge.java           (example generated source)

objs-codegen-java (runs from the example/application build)
├── ProductType.java
├── ComponentType.java
├── ProductRef.java
├── ProductContainsComponentRelation.java
├── ProductNode.java
└── GeneratedCatalog.java
```

The generated relation binding fixes role, source type, target type, edge-property schema, and
cardinality metadata. It creates a `TypedEdge` / `Edge`; it does not add a nested relation
property to the entity payload. None of these schema-specific classes are part of a root
`objs-*` module.

The generated read-side API is conceptually:

```kotlin
val view = TypedGraphView.from(rawGraph, GeneratedCatalog.INSTANCE)

for (product in view.products()) {
    for (component in product.getContainsComponents()) {
        println("${product.value().getName()} → ${component.value().getName()}")
    }
}
```

`TypedGraphView` is a generic `objs-api` runtime that indexes supplied entities and edges in memory.
`ProductNode` is an application-generated read wrapper
around a typed entity plus the view context; `getContainsComponents()` follows graph edges and
returns typed nodes, rather than reading a nested payload property. A relation-edge view is also
available when callers need edge properties. Generated navigation is read-only and does not imply
that the underlying store is mutable. The generated catalog binds `(type, schemaVersion)` to the
generated payload and node adapter required by the view.

The generated fluent mutation builder also takes the catalog schema into account:

```kotlin
val tx = CatalogMutationBuilder.merge()
val product = tx.addProduct(
    Product()
        .withName("Payments API")
        .withVersion("2.3.1")
)
val component = tx.addComponent(
    Component()
        .withName("Spring Boot")
        .withVersion("3.3.0")
)
product.containsComponent(component, CanonicalEdge().withSource("detected"))
val mutation = tx.build()
```

`addProduct` fixes the entity type and schema version, the application-generated payload builder exposes the
schema fields, and `containsComponent` exists only because the allow-list contains that exact
relation. The `add<Type>(pojo)` methods perform the POJO → typed node wrapping internally. The
builder performs only the structural checks required to construct its object model by default.
An explicit inspection/prevalidation operation may return diagnostics without preventing a candidate
mutation from being built; server-side schema, allow-list, endpoint, and persistence validation
remains authoritative.

## Out of scope

- Automatic recursive serialization of linked entity collections into graph edges
- Replacing `jsonschema2pojo` as the payload DTO generator
- Generated Spring controllers, repositories, or HTTP clients
- A Kotlin-specific generator module
- Generated application DTOs, nodes, relations, or catalogs in any root `objs-*` module
- Application-specific ontology or schema fixtures in a root `objs-*` module
- Kotlin Multiplatform; the first API target is Kotlin/JVM plus Java interoperability
- Persist-time enforcement of cardinality counts
- Statically typed bindings for wildcard endpoint rules
- JSON Schema import or reverse conversion into the Objs DSL
- Story closure, branch creation, commits, or pushes as part of planning

## Acceptance

- [x] `:objs-api` has no Spring/JPA/Boot/persistence dependencies and is usable from Kotlin and Java
- [x] Every root `objs-*` module remains schema/ontology-agnostic
- [x] `objs-core` can consume the API module without a dependency cycle
- [x] Generated payloads contain attributes only; application-generated relation bindings create explicit edges
- [x] A consuming application's Gradle build runs `jsonschema2pojo` followed by the Objs binding generator
- [x] `:objs-codegen-java` is reusable by applications outside this repository
- [x] A fresh full checkout builds the examples without publishing artifacts or manually running
  code generation
- [x] The first generator emits Java only; Kotlin generation is not part of this story
- [x] No generated application source is written to or compiled as part of a root `objs-*` module
- [x] Generated POJO conversion accepts the consuming application's specifically configured Jackson mapper
- [x] Generated schema-aware fluent builder compiles against `objs-api` and can build MERGE and REPLACE mutations
- [x] Generated Java construction uses conventional payload and mutation APIs without staged builders
- [x] Builder UUID identity and duplicate rejection are enforced without business-field deduplication
- [x] Prevalidation/inspection is explicitly invoked and diagnostic; it does not block object-model
  construction by default
- [x] Missing or mismatched edge-property references are diagnosable without blocking candidate
  mutation construction
- [x] `NONE` generates property-free edge creation and `SCHEMA` generates the configured
  edge-property input behavior
- [x] A mutation containing new entities and valid generated edges passes server validation
- [x] A raw `Graph` / `GraphContents` can be wrapped as a typed, navigable in-memory view
- [x] Generated navigation returns typed target nodes and preserves edge-property access
- [x] The read view resolves payload classes by `(type, schemaVersion)` through generated catalog bindings
- [x] Snapshot data remains readable after schema evolution, with raw/unresolved fallbacks for
  unknown types, dangling endpoints, obsolete relations, and missing historical adapters
- [x] Optional existing schema-registry access is lazy and never required for raw snapshot reads
- [x] Collections remain available for all cardinalities, and ambiguous `1:1` singular access throws
  `AmbiguousRelationException`
- [x] Generic relation traversal returns all matching edge views and preserves edge properties
- [x] Read and write node capabilities can be used independently or through an explicit combined
  facade without implicit persistence
- [x] Read navigation performs no persistence and does not mutate the supplied graph
- [x] Wrong source/target relation and generated-name collisions fail clearly; missing edge-property
  schemas produce explicit diagnostics and generic handling
- [x] Existing linked export remains available as a read model and is documented as non-write
- [x] Relation and mutation metadata changes are limited to the JSON-schema-codegen format
- [x] Both 2020-12 and draft-07 codegen documents remain standards-valid and jsonschema2pojo-compatible
- [x] Schema/relation metadata can override colliding Java names, while absent metadata retains
  current generated names and invalid overrides fail clearly
- [x] Generated Java entity/payload classes can inherit a configured base class and implement
  configured interfaces
- [x] Wildcard relations generate static bindings only when the wildcard endpoint has a configured
  base class
- [x] `./gradlew :objs-api:test :objs-core:test :objs-service:test`

