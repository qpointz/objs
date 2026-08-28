# `objs-codegen-java`

Reusable Java binding scaffolding for an Objs `json-schema-codegen` export.

The generator consumes the dialect-native `$defs` or `definitions` section together with the
root `x-objs-codegen` and `x-objs-relations` metadata. It writes deterministic, application-owned
Java sources:

- a typed node, identity-only reference, and metadata factory for each generated entity;
- independent read-navigation and mutation capability markers;
- a common `GeneratedNode` identity/payload handle; and
- deterministic relation metadata for later behavioral generator stages.

It also emits an application-owned `GraphMutationBuilder` with POJO-to-node helpers, exact
non-wildcard relation methods, separate API `Edge` construction, duplicate UUID checks, and
`MERGE` / `REPLACE` mutation assembly. It does not generate persistence code, HTTP clients, or
application classes into the root modules.

The generated `GeneratedReadView` and `<Type>ReadNode` facades provide typed root collections,
outbound/inbound relation navigation, immutable relation-edge collections, generic edge queries,
and singular accessors for `1:1` metadata. Hydration is exact-version and uses the consuming
application's supplied `PayloadMapper`; raw `TypedGraphView` access remains available for unknown
or schema-evolved data.

## Java API

```kotlin
JavaCodeGenerator().generate(
    JavaCodegenOptions(
        schemaFile = schema,
        outputDirectory = generatedSources,
        targetPackage = "com.example.generated",
    ),
)
```

The output directory must be owned by the consuming application. Paths under a root `objs-*`
module are rejected. Configured custom node base classes must expose an accessible no-argument
constructor. The generated builder requires a caller-supplied `PayloadMapper`; it never creates a
Jackson mapper or performs persistence. The standalone CLI entry point is `JavaCodegenMain`.
