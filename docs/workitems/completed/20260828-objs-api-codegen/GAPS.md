# Gaps — schema-agnostic foundation and app-local graph codegen

Track decisions and implementation risks for `objs-api-codegen`. Promote unresolved items into
the relevant WI before implementation starts.

## Open

| ID | Topic | Status | Notes |
|---|---|---|---|

## Resolved

| ID | Topic | Decision |
|---|---|---|
| G-1 | Existing package compatibility | Every root-library type identifier using the `BoM`/`Bom` prefix is renamed, including catalog, matcher, persistence, seed, validation, versioning, and Gremlin types; this is an intentional source/binary break with no aliases or compatibility facades |
| G-2 | API serialization dependency | Use Jackson, but never construct a global or default mapper inside the API. Payload conversion accepts a consuming application's configured mapper (or a caller-supplied Jackson-backed codec), so modules retain full control over modules, naming, null handling, and unknown-property behavior |
| G-3 | API primitive ownership | Move the schema-agnostic graph/write/read runtime kernel; retain server-bound catalogs, matchers, validation, seeds, persistence, and orchestration in root foundation modules |
| G-4 | Relation manifest shape | The codegen-only export carries a root `x-objs-relations` extension. Entity and edge-property schemas remain under dialect-native `$defs` (2020-12) or `definitions` (draft-07), with dialect-correct `$ref` and `allOf` handling |
| G-5 | Generator distribution | A reusable root `:objs-codegen-java` artifact is consumed by examples and external applications; generated output remains in each consuming application's source set |
| G-6 | Generated language | The first generator artifact emits Java only. A future Kotlin generator is a separate Kotlin-specific module and is not part of this story |
| G-7 | Naming collisions | The codegen-only export carries `x-objs-codegen` metadata. Schema attributes support `codegen.java.typeName`, `codegen.baseClass`, and comma-separated `codegen.interfaces`; relation attributes support `codegen.java.outboundMethod` and `codegen.java.inboundMethod`. Tags support opt-in `codegen.java.skip` and `codegen.java.noInverse`. Missing overrides use the current normalization; blank/invalid names, invalid inheritance metadata, and post-override collisions fail deterministically |
| G-8 | Wildcard rules | A wildcard endpoint may use relation-level `codegen.baseClass` to supply the common Java type for the `*` side. If it is absent, the generator omits only that static wildcard relation binding; the runtime rule remains available and concrete bindings are unaffected |
| G-9 | Cardinality meaning | Keep cardinality as metadata without persist-time count enforcement. Generate collection navigation for all relations, plus a `1:1` singular convenience accessor that returns empty/null for zero matches and throws `AmbiguousRelationException` for multiple matches |
| G-10 | Edge rule references | Codegen performs basic non-blocking checks for missing, unknown-version, or non-`EDGE_PROPERTIES` references and exposes diagnostics. Relation metadata is preserved and generic edge-property handling may be used; strict acceptance remains at the object-store boundary |
| G-11 | Standalone example dependency | Examples use root Gradle project dependencies on `:objs-api` and `:objs-codegen-java`; generation is wired into the normal example build so a fresh full checkout compiles without publishing, credentials, or manual generation steps |
| G-25 | Edge property policy | `SCHEMA` relations include their edge-property schema and generated property input; `NONE` is authoritative and generates a bare-edge operation with no property input, even if other metadata exists. Supplied/read-side property data is diagnosed but never discarded |
| G-26 | Generic relation traversal | Every node exposes a generic relation-edge query by role/selector that returns all matching edge views, including properties and raw/unresolved endpoints, regardless of generated bindings or cardinality |
| G-27 | API exception hierarchy | `ObjsException` is the root unchecked API exception; `AmbiguousRelationException` extends it and identifies the node/relation whose singular accessor was ambiguous |
| G-16 | Typed collection contract | Generated navigation returns immutable collections; generated typed accessors return nodes, while generic `edges(...)` returns all relation-edge views with properties and raw/unresolved endpoints. A singular `1:1` convenience accessor is optional/empty for zero and ambiguity-throwing for multiple |
| G-28 | Lazy snapshot schema resolution | `TypedGraphView` accepts optional access to an existing schema registry/provider through a lightweight exact-version lookup callback or adapter; no new `SchemaResolver` type is added. Raw entities/edges are immediately readable, schema lookup is lazy, and unresolved schemas retain raw data with diagnostics |
| G-12 | Local schema validation level | Prevalidation is optional and explicitly invoked for inspection or diagnostics. It must not block object-model construction by default; strict schema, relation, endpoint, and persistence validation remains the object-store boundary |
| G-13 | Required-field builder strategy | Use conventional JavaBeans/withers from jsonschema2pojo and ordinary generated Java mutation methods. Do not use staged/type-state builders; explicit inspection reports required-field issues, while `build()` remains non-blocking |
| G-14 | POJO node reuse | UUID is the only builder identity. Missing IDs receive fresh provisional UUIDs; payload/business-field equality never deduplicates nodes. Registering a second entity or edge with an existing UUID is rejected as a structural builder error |
| G-15 | Read/write node capabilities | Use a common generated node identity/payload handle with separate read-navigation and write-mutation capabilities. Applications may expose a combined facade, but read views never become implicitly mutable and write handles never imply persistence |
| G-17 | Non-conforming read data | Snapshot reads are lossless and lenient by default. Unknown types, dangling endpoints, schema drift, and edges that violate current catalog rules remain visible as raw/unresolved data; optional inspection reports diagnostics without dropping data |
| G-18 | Read hydration registry | Read bindings are keyed by exact `(type, schemaVersion)`. Generated adapters hydrate known versions, while missing historical adapters use a generic raw fallback so every stored entity and edge remains readable |
| G-24 | Foundation versus application layering | All root `objs-*` modules are generic foundation; `examples/*` own application schemas, ontology classes, generated sources, and generated catalogs |
| G-22 | Generated source ownership | The reusable generator may live in foundation tooling, but generated DTOs, nodes, relations, and catalogs are written only to the consuming application's source set |
| G-23 | Wire and storage naming | Public class renames do not rename JSON mutation fields, REST paths, or JPA entity names. Objs persistence tables use the `objs_*` namespace after forward migration V6; SBOM and other application-owned tables retain their own namespaces |

## Deferred

| ID | Topic | Status | Notes |
|---|---|---|---|
| G-19 | Aggregate materializer | deferred | A later explicit materializer may turn object graphs into entities plus edges; direct POJO serialization remains out of scope |
| G-20 | Generated HTTP client | deferred | The API module and generated bindings do not contain REST client behavior |
| G-21 | Persist-time cardinality checks | deferred | Requires a separate integrity design and is not part of this story |
| G-29 | Full consumer policy matrix | deferred | The consumer smoke test covers a valid `NONE` relation; exhaustive `SCHEMA`, wildcard, override, and invalid-endpoint scenarios remain a follow-up hardening task |
| G-30 | Evolved-snapshot consumer fixture | deferred | Core API tests cover raw/dangling reads; a dedicated generated-consumer fixture for historical adapters and schema drift remains outside the current integration smoke test |

