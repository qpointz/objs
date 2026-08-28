# JSON Schema (codegen) → Java POJOs

Standalone Gradle example: drop (or fetch) an objs **`json-schema-codegen`** export and generate
Java classes with [jsonschema2pojo](https://www.jsonschema2pojo.org/).

Default dialect is **2020-12** (`$defs`). For draft-07 (`definitions`), see the sibling
[`../jsonschema-draft07`](../jsonschema-draft07).

No schema wrapping — workbench **Export → JSON Schema (codegen)** downloads a file that is
already POJO-ready.

## Layout

| Path | Role |
|------|------|
| `src/jsonschema/registry-catalog.codegen.schema.json` | Committed snapshot (`format=json-schema-codegen`, default dialect) |
| `src/jsonschema/graph-builder.codegen.schema.json` | Small Product → Component typed graph contract |
| `build/generated/sources/jsonschema2pojo/` | Generated Java under `org.poc.objs.codegen.generated` |
| `build/generated/sources/typed-bindings/` | Generated mutation and read bindings |

Not part of the root multi-module build — own `settings.gradle.kts`.

## Commands

From the **repository root**:

```bash
# Optional: refresh from a running instance (default :8080, dialect 2020-12)
./gradlew -p examples/codegen/jsonschema fetchRegistrySchema

# Custom URL
./gradlew -p examples/codegen/jsonschema fetchRegistrySchema \
  -Pobjs.schemaUrl='http://localhost:8081/api/v1/objs/registry/export?format=json-schema-codegen&includeEdgePropertySchemas=true'

# Generate POJOs and graph bindings, compile, smoke-test
./gradlew -p examples/codegen/jsonschema test
```

Or copy `objs-catalog.codegen.schema.json` from the workbench into
`src/jsonschema/registry-catalog.codegen.schema.json`.

Note: jsonschema2pojo may normalize all-caps type names (`API` → `Api`).

## Endpoint

```
GET /api/v1/objs/registry/export?format=json-schema-codegen
```

Same edge options as `json-schema` (`includeEdges`, `includeEdgePropertySchemas`, `dialect`).
`dialect=draft-07` emits `definitions` + `#/definitions/…` refs — use
[`jsonschema-draft07`](../jsonschema-draft07) for that path. Default `2020-12`
uses `$defs`. Adds a synthetic root (`ObjsCatalog`) that `$ref`s every catalog def and sets each
def `title` to the def key so class names stay PascalCase.

The test also demonstrates `GraphMutationBuilder`: pass generated `Product` and `Component` POJOs,
call `containsComponent`, then construct a `GeneratedReadView` from the resulting graph.

The catalog's linked relation properties are read/navigation projections. They are not written as
nested mutation payloads; graph-write code uses the generated builder's explicit edge methods.
