# JSON Schema (codegen, draft-07) → Java POJOs

Standalone Gradle example for objs **`json-schema-codegen`** with **`dialect=draft-07`**.

Sibling of [`../jsonschema`](../jsonschema) (default `2020-12` / `$defs`). This
variant uses draft-07 `definitions` and `#/definitions/…` refs — often a better fit for older
jsonschema2pojo / OpenAPI tooling.

## Layout

| Path | Role |
|------|------|
| `src/jsonschema/registry-catalog.codegen.draft07.schema.json` | Committed snapshot from a running registry |
| `src/jsonschema/graph-builder.codegen.draft07.schema.json` | Small Product → Component typed graph contract |
| `build/generated/sources/jsonschema2pojo/` | Generated Java under `org.poc.objs.codegen.draft07.generated` |
| `build/generated/sources/typed-bindings/` | Generated mutation and read bindings |

Not part of the root multi-module build — own `settings.gradle.kts`.

## Commands

From the **repository root** (backend on `:8080` or override URL):

```bash
# Refresh committed schema from a running instance
./gradlew -p examples/codegen/jsonschema-draft07 fetchRegistrySchema

# Custom host/port
./gradlew -p examples/codegen/jsonschema-draft07 fetchRegistrySchema \
  -Pobjs.schemaUrl='http://localhost:8081/api/v1/objs/registry/export?format=json-schema-codegen&dialect=draft-07&includeEdgePropertySchemas=true'

# Generate POJOs and graph bindings, compile, smoke-test
./gradlew -p examples/codegen/jsonschema-draft07 test
```

Or in workbench: set dialect **draft-07**, **Export → JSON Schema (codegen)**, and copy the file
to `src/jsonschema/registry-catalog.codegen.draft07.schema.json`.

## Endpoint

```
GET /api/v1/objs/registry/export?format=json-schema-codegen&dialect=draft-07
```

Singular relation `$ref`s are wrapped in `allOf` so title / `x-objs-*` siblings stay meaningful
under draft-07 `$ref` exclusivity.

The test also demonstrates `GraphMutationBuilder`: pass generated `Product` and `Component` POJOs,
call `containsComponent`, then construct a `GeneratedReadView` from the resulting graph.

The catalog's linked relation properties are read/navigation projections. They are not written as
nested mutation payloads; graph-write code uses the generated builder's explicit edge methods.
