# Objs codegen and graph builder

This document is the implementation guide for generating application-owned payloads, typed graph
handles, mutations, and in-memory read navigation from an Objs JSON Schema export.

## Design boundary

The root `objs-*` modules are schema-agnostic foundation modules. They contain the generic
`objs-api` runtime and the reusable `objs-codegen-java` tool, but they do not contain application
ontology classes or generated application bindings.

The consuming application owns:

- its JSON Schema export or snapshot;
- payload and edge-property DTOs;
- generated entity nodes, references, read nodes, relation methods, and catalogs; and
- the application code that ultimately sends a `GraphMutation` to an object store.

Generated code assembles objects in memory. It does not persist, call REST, depend on Spring, or
replace strict object-store validation.

## End-to-end stages

The story is deliberately split into independently reviewable stages:

| Execution order | Work item | Responsibility | Main output |
|---:|---|---|---|
| 0 | WI-000 | Lock ownership, API, naming, identity, and validation decisions | Stable design contract |
| 1 | WI-001 | Create the framework-free Kotlin/JVM API boundary | `:objs-api` module |
| 2 | WI-002 | Extract generic graph runtime and remove the public `BoM` prefix | API domain, typed, and graph primitives |
| 3 | WI-008 | Stabilize renamed consumers and dependency boundaries | Compiling renamed foundation |
| 4 | WI-003 | Add the codegen-only JSON Schema contract | `x-objs-codegen`, `x-objs-relations`, mutation definitions |
| 5 | WI-004 | Add the reusable Java generator and typed scaffolding | `:objs-codegen-java` and typed node/reference sources |
| 6 | WI-005 | Generate schema-aware write behavior | `GraphMutationBuilder`, entity registration, relation methods |
| 7 | WI-006 | Generate schema-aware read behavior | `GeneratedReadView`, typed read nodes, navigation |
| 8 | WI-009 | Prove consumer integration in both dialects | Example Gradle lifecycle and graph smoke tests |
| 9 | WI-007 | Document, harden, and explicitly defer remaining risks | This document, READMEs, tests, and `GAPS.md` |

Each stage has a review gate. In particular:

- the public rename is a deliberate source/binary break;
- mutation construction must be reviewed before read navigation;
- read navigation must preserve raw and unresolved data; and
- consumer integration must pass before final documentation hardening.

## Two generator passes

### Pass 1: payload DTO generation

The consuming build runs `jsonschema2pojo` against the codegen export. This pass owns ordinary
payload classes and edge-property classes:

```text
build/generated/sources/jsonschema2pojo/
└── org/poc/objs/codegen/generated/
    ├── Product.java
    ├── Component.java
    └── CanonicalEdge.java
```

These classes use conventional JavaBeans/withers. A relation property that appears in a linked
read projection is not treated as a graph edge during mutation construction.

### Pass 2: Objs behavioral bindings

`objs-codegen-java` consumes the same application-owned schema plus the Objs metadata extensions.
It writes only to the consuming application's generated source set:

```text
build/generated/sources/typed-bindings/
└── org/poc/objs/codegen/generated/
    ├── GeneratedNode.java
    ├── GeneratedNodeHandle.java
    ├── ReadNodeCapability.java
    ├── MutationNodeCapability.java
    ├── GeneratedRelationMetadata.java
    ├── GraphMutationBuilder.java
    ├── GeneratedReadView.java
    ├── ProductType.java
    ├── ProductRef.java
    ├── ProductNode.java
    ├── ProductReadNode.java
    ├── ComponentType.java
    ├── ComponentRef.java
    ├── ComponentNode.java
    └── ComponentReadNode.java
```

The exact list is determined by the exported definitions and relation manifest. Application
classes are never emitted into `objs-api`, `objs-core`, or another root `objs-*` module.

## Codegen export contract

The `json-schema-codegen` format adds metadata without changing the standard catalog export:

```json
{
  "x-objs-codegen": {
    "version": 1,
    "language": "java",
    "definitions": []
  },
  "x-objs-relations": []
}
```

Entity and edge-property schemas remain in the dialect-native location:

```text
2020-12  → $defs and #/$defs/...
draft-07 → definitions and #/definitions/...
```

Each relation entry identifies its source and target definitions, role, cardinality,
`propertiesPolicy`, property-schema reference, direction metadata, and generated method names.
Historical `(type, schemaVersion)` entries remain available for snapshot reads.

Recognized schema and relation overrides include:

```text
codegen.java.typeName
codegen.baseClass
codegen.interfaces
codegen.java.outboundMethod
codegen.java.inboundMethod
```

The `codegen.java.skip` and `codegen.java.noInverse` tags suppress selected generated behavior.
Missing overrides use the existing normalization. Invalid identifiers, invalid type references,
blank values, and collisions fail with diagnostics naming the offending schema or relation.

Wildcard relations remain in runtime metadata. A static wildcard binding is generated only when
the wildcard endpoint supplies a safe relation-level `codegen.baseClass`.

## Generated write artifacts

### Typed entity node

`ProductNode` is a write-side typed handle. It fixes the entity type and schema version and wraps a
payload DTO:

```java
ProductNode product = mutations.addProduct(
    new Product().withName("Payments API")
);
```

The overload accepting a `UUID` allows callers to supply an identity. The POJO overload assigns a
provisional UUID. UUID is the only identity; equal payloads with different UUIDs remain separate.
Registering another entity with an existing UUID fails.

### Identity reference

`ProductRef` is an identity-only generated record:

```java
ProductRef reference = product.ref();
UUID id = reference.id();
```

It is useful for identity-level APIs and does not contain payload or navigation behavior.

### Generated mutation builder

`GraphMutationBuilder` is application-owned and receives the caller's configured `PayloadMapper`:

```java
PayloadMapper mapper = new PayloadMapper(configuredObjectMapper);
GraphMutationBuilder mutations = new GraphMutationBuilder(mapper);

ProductNode product = mutations.addProduct(
    new Product().withName("Payments API")
);
ComponentNode component = mutations.addComponent(
    new Component().withName("Jackson")
);

mutations.containsComponent(product, component);
GraphMutation mutation = mutations.build();
```

The builder produces separate entity and edge mutation lists:

```text
entities.set → Product, Component
edges.set    → (Product)-[CONTAINS]->(Component)
```

`MERGE` is the default. `REPLACE` is selected explicitly:

```java
GraphMutationBuilder replacement =
    new GraphMutationBuilder(mapper, MutationMode.REPLACE);
GraphMutation mutation = replacement.build();
```

The builder performs structural checks needed to construct its model, including duplicate UUID
rejection. Endpoint and schema acceptance remains the object-store boundary. Explicit
`diagnostics()` inspection is available and does not turn ordinary construction into a persistence
operation.

### Relation methods

Only exact, generated allow-list relations receive typed methods. A disallowed relation has no
corresponding typed method. A wildcard relation without a safe static binding remains accessible
only through runtime metadata.

`NONE` is authoritative and has no property parameter:

```java
mutations.containsComponent(product, component);
```

The resulting `Edge` has `properties == null`.

`SCHEMA` relations receive the resolved edge-property DTO as an argument:

```java
mutations.hasDependency(product, component, dependencyProperties);
```

If the property schema cannot be resolved, generation retains the relation and uses a generic map
representation with a diagnostic. `emptyPropertiesAllowed` controls whether a property-free
overload is generated. Neither policy performs strict persist-time validation.

## Generated read artifacts

### Typed read view

`GeneratedReadView` wraps the generic, immutable `TypedGraphView`:

```java
GeneratedReadView view = GeneratedReadView.from(graph, mapper);
```

The generated facade supplies exact `(type, schemaVersion)` hydration bindings using the supplied
mapper. A caller can also create a raw view without hydration:

```java
GeneratedReadView rawView = GeneratedReadView.from(graph);
```

### Typed read node

`ProductReadNode` is a read-only facade. It is distinct from `ProductNode`:

```java
ProductReadNode product = view.products().get(0);
TypedCollection<ComponentReadNode> components =
    product.getContainsComponents();
TypedCollection<RelationEdgeView> relationEdges =
    product.getContainsComponentEdges();
```

Read nodes expose:

- typed root collections such as `products()`;
- typed outbound and inbound relation collections;
- relation-edge collections that preserve edge properties;
- generic `edges(role, direction)` traversal; and
- singular accessors for `1:1` relations.

The singular accessor returns `null` for zero matches and throws
`AmbiguousRelationException` for multiple resolved matches. Cardinality remains navigation
metadata, not a persist-time count check.

Inverse navigation follows the original directed edge. It does not manufacture a reverse edge.
Unknown types, dangling endpoints, obsolete relations, schema drift, and missing historical
bindings remain visible through raw nodes and relation-edge views.

## Complete consumer example

The in-memory round trip is:

```java
PayloadMapper mapper = new PayloadMapper(configuredObjectMapper);
GraphMutationBuilder builder = new GraphMutationBuilder(mapper);

ProductNode product = builder.addProduct(new Product().withName("Payments"));
ComponentNode component = builder.addComponent(new Component().withName("Jackson"));
builder.containsComponent(product, component);

GraphMutation mutation = builder.build();
Graph graph = new Graph(
    new ArrayList<>(mutation.getEntities().getSet()),
    new ArrayList<>(mutation.getEdges().getSet())
);

GeneratedReadView view = GeneratedReadView.from(graph, mapper);
ProductReadNode readProduct = view.products().get(0);
ComponentReadNode readComponent =
    readProduct.getContainsComponents().get(0);
```

The object store can receive `mutation` after applying its own strict schema, endpoint, allow-list,
and persistence validation. Constructing `mutation` or `view` does not contact that store.

## Example projects

The repository contains two standalone consumers:

```text
examples/codegen/jsonschema/
examples/codegen/jsonschema-draft07/
```

Both use a Gradle composite build to consume the root `objs-api` and `objs-codegen-java` artifacts
without publishing. Their standard lifecycle is:

```text
generateJsonSchema2Pojo
        ↓
generateObjsJava
        ↓
compileJava
        ↓
test
```

Run them from the repository root:

```text
./gradlew -p examples/codegen/jsonschema test
./gradlew -p examples/codegen/jsonschema-draft07 test
```

Each example contains a small Product → Component graph contract so the consumer test can verify
generated mutation and read behavior independently of a running registry. The catalog snapshot
continues to verify ordinary jsonschema2pojo payload generation. A registry snapshot can be
refreshed with the example's `fetchRegistrySchema` task.

## Deferred work

The following are explicit deferrals, not hidden requirements of the generated builder:

- exhaustive consumer coverage for every `SCHEMA` policy combination;
- complete wildcard and override behavior matrix in the consumer examples;
- generated-consumer fixtures for historical schema versions and evolved snapshots;
- recursive aggregate materialization;
- generated HTTP clients; and
- persist-time cardinality enforcement.

