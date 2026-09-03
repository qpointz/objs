# Objs

### A schema-driven object store for connected domain data

> **Experimental** — Objs is an early-stage proof of concept and its APIs, modules, and
> persistence model may change.

Most useful information is not a row in isolation. A product uses components, a service
implements an API, a deployment runs on a host, and an application may belong to several
different views of the same world. Objs gives those objects a durable home and makes their
relationships explicit, typed, searchable, and navigable.

Objs is short for **objects**. The name is deliberately broader than a bill of materials:
a BOM is one valuable use case, while an object graph can represent the whole domain around it.

## The idea

Objs separates the reusable object-graph foundation from the vocabulary of any particular
application:

```text
application schema
       │
       ├── JSON Schema + generated Java bindings
       │
       ▼
  objects ── relationships ── objects
       │
       ├── annotations and named graph views
       └── schema-aware persistence and queries
```

An **entity** is a typed object with a `(type, schema version)`, JSON payload, identity, and
optional annotations. An **edge** connects two entities with a role such as `CONTAINS`,
`IMPLEMENTS`, or `DEPENDS_ON`. Edge properties are governed by the same schema and allow-list
rules as the entities they connect.

This keeps the write model honest: relationships are graph edges, not nested payload collections.
The result is a model that can be validated at the persistence boundary, traversed as a graph,
and evolved without making historical snapshots unreadable.

## Why Objs?

- **Schema first** — an authoritative object schema drives validation, JSON Schema export, and
  generated application types.
- **Graph native** — entities and edges are first-class; named graphs and collections can
  provide different views over shared objects.
- **Safe writes, flexible reads** — applications can assemble graphs in memory, while the object
  store applies strict schema and relation validation when data is persisted.
- **Built for change** — type versions, explicit identities, and raw graph access keep older or
  partially conforming data readable as schemas evolve.
- **Application-owned codegen** — generated DTOs, typed nodes, relation methods, and mutation
  builders live in the consuming application, never in the schema-agnostic foundation.
- **Composable runtime** — the core API is Kotlin/JVM, Java-compatible, Spring-free, and
  persistence-free; Spring and database integrations are separate modules.

## A typed write looks like this

Given a schema describing `Product CONTAINS Component`, the Java generator creates payload DTOs
and a type-safe graph builder:

```java
GraphMutationBuilder builder = new GraphMutationBuilder(mapper);

ProductNode product = builder.addProduct(new Product().withName("Payments"));
ComponentNode component = builder.addComponent(new Component().withName("Jackson"));

builder.containsComponent(product, component);
GraphMutation mutation = builder.build();
```

The generated relation method can only express an allowed edge. The builder turns the POJOs into
graph entities and emits an explicit edge; linked properties remain read/navigation projections
instead of being serialized as invalid nested mutations.

The same model can be read as a typed, in-memory view:

```java
GeneratedReadView view = GeneratedReadView.from(graph, mapper);
ProductReadNode product = view.products().get(0);
TypedCollection<ComponentReadNode> components =
    product.getContainsComponents();
```

The normal code-generation flow is:

```text
jsonschema2pojo → objs-codegen-java → Java compilation → application tests
```

See the full boundary and generated API in
[`docs/design/graph/api-and-codegen.md`](docs/design/graph/api-and-codegen.md).

## Project structure

Objs is a multi-module Gradle project. Root `objs-*` modules are generic foundation; concrete
domain vocabulary belongs under `examples/`.

| Module | Purpose |
|--------|---------|
| [`objs-api`](objs-api/) | Spring-free, Java-compatible graph and mutation primitives |
| [`objs-codegen-java`](objs-codegen-java/) | Reusable Java generator for typed bindings |
| [`objs-persistence`](objs-persistence/) | Spring-free JPA persistence, Flyway SQL, seed apply, networknt Validator |
| [`objs-service`](objs-service/) | Foundation REST API and Spring Boot autoconfiguration |
| [`objs-service-ui`](objs-service-ui/) | Foundation workbench for schemas and graph operations |
| [`objs-gremlin-core`](objs-gremlin-core/) | In-process graph materialization and Gremlin evaluation |
| [`objs-gremlin-service`](objs-gremlin-service/) | Gremlin REST integration |
| [`objs-service-app`](objs-service-app/) | Standalone workbench runner on port `8081` |

## Examples

| Example | What it demonstrates |
|---------|----------------------|
| [`examples/sbom`](examples/sbom/) | A schema-driven SBOM inventory application |
| [`examples/asset-repository`](examples/asset-repository/) | Objs as a centralized object store with collections, domain REST, and UI |
| [`examples/codegen/jsonschema`](examples/codegen/jsonschema/) | Offline Java codegen with JSON Schema 2020-12 |
| [`examples/codegen/jsonschema-draft07`](examples/codegen/jsonschema-draft07/) | The same pipeline with draft-07 schemas |

Generated output stays inside each consuming application. The examples demonstrate that domain
applications can use Objs without placing application-specific generated classes in `objs-persistence`.

## Quick start

Requirements: JDK 21 and a network connection for Gradle dependencies.

```bash
# Build and test the project
./gradlew test

# Start the foundation workbench on :8081
./gradlew :objs-service-app:run

# Start the SBOM example on :8080
./gradlew :sbom-service:run

# Run the standalone codegen examples
./gradlew -p examples/codegen/jsonschema test
./gradlew -p examples/codegen/jsonschema-draft07 test
```

The workbench is a foundation tool. The SBOM and asset-repository applications own their domain
REST and UI surfaces; they do not depend on the workbench as their domain API.

## Learn more

- [Graph and entity design](docs/design/graph/)
- [API and application-local codegen](docs/design/graph/api-and-codegen.md)
- [SBOM application](examples/sbom/README.md)
- [Asset repository application](examples/asset-repository/README.md)
- [Source export](scripts/export/README.md)

Version comes from the root [`VERSION`](VERSION) file and can be overridden with
`-PprojectVersion=`.
