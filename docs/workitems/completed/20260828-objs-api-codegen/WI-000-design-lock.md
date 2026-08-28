# WI-000 — Design lock and foundation/application contract

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 0 — Design lock and baseline
**Status:** completed
**Depends on:** —
**Implementation commit:** [`a4ddbb3`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/a4ddbb3)

## Goal

Lock the root foundation boundary, package strategy, generated source ownership, public naming,
and staged execution sequence before moving runtime classes or adding codegen behavior. Establish
the evidence needed to stop safely before the intentional public API break.

## Scope

- Define the schema-agnostic public `:objs-api` surface and root `objs-*` dependency rules
- Decide which current graph/write/read runtime and typed classes move behind `objs-api`
- Record which server-bound catalog, matcher, validation, seed, persistence, and orchestration
  classes remain in the root foundation
- Define `EntityRef<P>`, `RelationSpec<S,T,P>`, graph construction, and mutation boundaries
- Define the JSON Schema relation manifest and generated symbol naming rules
- Define schema-level and relation-level codegen metadata for Java symbol overrides: schema
  attributes control `codegen.java.typeName`, relation attributes control
  `codegen.java.outboundMethod` and `codegen.java.inboundMethod`, and opt-in tags support
  `codegen.java.skip` and `codegen.java.noInverse`
- Define schema-level Java inheritance metadata: `codegen.baseClass` selects one accessible
  superclass and comma-separated `codegen.interfaces` selects implemented interfaces for generated
  entity/payload classes
- Define wildcard relation behavior: relation-level `codegen.baseClass` supplies the common Java
  type for a wildcard endpoint; without it, omit only the static binding for that wildcard side
  while retaining the runtime relation rule
- Define generic relation traversal returning all matching edge views by role/selector, including
  properties and raw/unresolved endpoints
- Define collection navigation as immutable and cardinality-independent, with a `1:1` singular
  convenience accessor that is empty/null for zero and throws `AmbiguousRelationException` for
  multiple matches
- Define optional lazy access to an existing schema registry/provider through a lightweight exact
  `(type, schemaVersion)` lookup callback or adapter; do not add a new `SchemaResolver` abstraction
- Introduce `ObjsException` as the root unchecked API exception and derive
  `AmbiguousRelationException` from it
- Define read/write capability composition: a common generated identity/payload handle may be
  adapted to independent read-navigation and write-mutation capabilities, with an optional
  application-owned combined facade
- Reserve the `codegen.java.*` key namespace, define precedence, preserve the current normalization
  fallback when no override is supplied, and fail on blank/invalid overrides or collisions after
  overrides
- Define `:objs-codegen-java` as the reusable Java-only codegen artifact for examples and external
  consuming applications; generated output remains application-owned
- Define examples as in-repository consumers using Gradle project dependencies, with generation
  attached to the normal build lifecycle and no checkout-time setup step
- Keep the relation manifest as a root `x-objs-relations` extension in the codegen-only export;
  retain dialect-native `$defs` / `definitions`, `$ref`, and draft-07 `allOf` semantics
- Ensure new mutation/codegen metadata changes are emitted only by the JSON-schema-codegen format,
  without changing the existing standard catalog export
- Define that schema-specific generated DTOs, nodes, relations, and catalogs are written only into
  a consuming application's generated source set
- Define the Jackson payload-conversion boundary: consuming code supplies the configured mapper or
  Jackson-backed codec; `objs-api` creates no global/default mapper
- Define validation as opt-in prevalidation/inspection: generated object-model construction and
  `build()` are non-blocking by default, while the object-store persist gate enforces strict
  schema, relation, endpoint, and persistence rules
- Define the Java builder style as conventional JavaBeans/withers for jsonschema2pojo payloads and
  ordinary Java methods for mutation/entity/relation construction; do not use staged or type-state
  builders
- Define UUID as the only builder identity: assign fresh provisional IDs when absent, never
  deduplicate by payload/business fields, and reject duplicate entity or edge UUID registration
- Define edge-property reference checks as non-blocking diagnostics; preserve unresolved relation
  metadata and provide a generic property fallback rather than preventing object-model creation
- Define relation property-policy precedence: `SCHEMA` requires property-schema participation;
  `NONE` overrides other property metadata and exposes only a bare-edge operation
- Define the public `BoM`-prefix removal mapping, collision handling, and preservation of wire/storage
  identifiers
- Define Java/Kotlin interoperability requirements
- Record the intentional breaking compatibility policy for current `org.poc.objs.core.*` imports
- Capture the pre-change public class/package inventory, dependency graph, wire/storage names, and
  repository test baseline
- Define the stage-by-stage readiness, risk, and stop-gate rules in `STORY.md`

## Baseline evidence

Captured on 2026-08-28 at revision `b0283fa` (`[docs] Add staged API codegen checkpoints`).

### Current module and dependency boundary

- `settings.gradle.kts` currently includes `:objs-core`, `:objs-service`,
  `:objs-gremlin-core`, `:objs-gremlin-service`, `:objs-service-app`, the UI modules, and the
  example service modules. There is no `:objs-api` or `:objs-codegen-java` module yet.
- `objs-core` currently applies Kotlin Spring/JPA plugins and exposes Spring Boot, Spring Data JPA,
  Flyway, Jackson, and Kotlin reflection dependencies. It also uses JSON Schema validator,
  Commons JEXL, and Caffeine. This confirms that `objs-core` cannot be the dependency of a
  persistence-free API runtime.
- The schema-agnostic extraction candidates are currently under
  `org.poc.objs.core.domain` (`BoMEntity`, `BoMEdge`, `BoMGraph`, `BoMGraphContents`, mutation
  halves, mutation envelope, and mutation builder) and
  `org.poc.objs.core.typed` (`TypedEntity`, `TypedEdge`, `TypedEdgeMeta`, `EntityTypeMeta`,
  `GraphBuilder`, and `NodeRef`).
- Server-bound or catalog-owned areas remain in `objs-core`: schema authoring/catalogs, JSON Schema
  export, matchers, validation/persist gates, seed handling, JPA entities/repositories, graph
  stores, and orchestration services.

### Public rename and wire/storage inventory

The initial kernel rename mapping is:

| Current public type | Target public type |
|---|---|
| `BoMEntity` | `Entity` |
| `BoMEdge` | `Edge` |
| `BoMGraph` | `Graph` |
| `BoMGraphContents` | `GraphContents` |
| `BoMEntityMutation` | `EntityMutation` |
| `BoMEdgeMutation` | `EdgeMutation` |
| `BoMGraphMutation` | `GraphMutation` |
| `BoMMutateMode` | `MutationMode` |
| `BoMGraphMutationBuilder` | `GraphMutationBuilder` |
| `BoMEntityMutationBuilder` | `EntityMutationBuilder` |
| `BoMEdgeMutationBuilder` | `EdgeMutationBuilder` |
| `BoMPropertiesPolicy` | `PropertiesPolicy` |
| `BoMEdgeCardinality` | `EdgeCardinality` |
| `BoMAllowedEdgeRule` | `AllowedEdgeRule` |

The rename applies to public source and package names only. Mutation JSON remains
`entities` / `edges` with `set` / `unset`; REST paths remain under `/api/v1/objs/**`; and existing
`bom_*` JPA/storage identifiers remain unchanged.

### Test baseline

`.\gradlew.bat test` completed successfully:

- `BUILD SUCCESSFUL`
- 47 actionable tasks: 15 executed and 32 up-to-date
- Elapsed time: 2 minutes 16 seconds

Existing non-blocking warnings were recorded but not changed by WI-000: the Kotlin Gradle plugin
is loaded in multiple subprojects, Gradle configuration cache reports nine Node-task problems,
and the UI build reports a bundle-size warning.

## Out of scope

- Module implementation
- Exporter implementation
- Generator implementation
- Generated application source
- REST or persistence changes
- Any public rename or class movement

## Acceptance

- [x] `STORY.md` normative locks and `GAPS.md` decisions are internally consistent
- [x] Root `objs-*` modules are schema/ontology-agnostic and the API dependency graph has no cycle
- [x] `objs-api` has no Spring/JPA/persistence dependency
- [x] `:objs-codegen-java` is a reusable Java codegen artifact and does not contain application
  ontology or generated application classes
- [x] A fresh full checkout can compile examples through the standard Gradle lifecycle without
  publishing artifacts or manually running generation
- [x] No application-generated source is owned by a root `objs-*` module
- [x] Payload, edge-property, relation-binding, and mutation surfaces are distinct
- [x] Exact, wildcard, and cardinality behavior is explicitly defined
- [x] Codegen naming overrides, fallback normalization, reserved keys, and post-override collision
  behavior are explicit
- [x] Generated Java inheritance metadata has defined scope, validation, ordering, and behavior when
  absent
- [x] Wildcard endpoint codegen has defined base-class requirements and omission behavior
- [x] Generic relation traversal, singular ambiguity behavior, and the API exception hierarchy are
  explicit
- [x] Snapshot schema access is optional, lazy, exact-version, and independent of a new registry
  abstraction
- [x] Read/write capability composition and the no-implicit-persistence rule are explicit
- [x] Public rename mappings and wire/storage-name preservation are explicit
- [x] Payload conversion uses caller-supplied Jackson configuration without a hidden singleton mapper
- [x] Optional prevalidation and authoritative object-store validation have distinct contracts
- [x] Required-field handling follows the conventional Java API and remains non-blocking until
  explicit inspection or object-store validation
- [x] UUID assignment, duplicate rejection, and non-deduplication of logical payloads are explicit
- [x] Missing or mismatched edge-property schemas produce diagnostics and a readable generic fallback
- [x] `SCHEMA` versus `NONE` generated signatures and supplied/read-side property behavior are explicit
- [x] 2020-12 and draft-07 codegen documents are standard-valid and jsonschema2pojo-compatible
- [x] Existing non-codegen JSON Schema export remains unchanged by the new metadata
- [x] A Kotlin caller and Java caller can be sketched against the locked API
- [x] The consuming-application task ordering is fixed as jsonschema2pojo → Objs generator → compilation
- [x] Java is the only generated language locked for this story; Kotlin generation is a future
  separate module
- [x] Baseline inventory, dependency evidence, wire/storage-name inventory, and test results are
  recorded before implementation
- [x] Each later stage has an explicit prerequisite, exit condition, and stop/review gate

