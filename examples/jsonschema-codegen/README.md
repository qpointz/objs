# JSON Schema (codegen) → Java POJOs

Standalone Gradle example: drop (or fetch) an objs **`json-schema-codegen`** export and generate
Java classes with [jsonschema2pojo](https://www.jsonschema2pojo.org/).

No schema wrapping — workbench **Export → JSON Schema (codegen)** downloads a file that is
already POJO-ready.

## Layout

| Path | Role |
|------|------|
| `src/jsonschema/registry-catalog.codegen.schema.json` | Committed snapshot (`format=json-schema-codegen`) |
| `build/generated/sources/jsonschema2pojo/` | Generated Java under `org.poc.objs.codegen.generated` |

Not part of the root multi-module build — own `settings.gradle.kts`.

## Commands

From the **repository root**:

```bash
# Optional: refresh from a running instance (default :8080)
./gradlew -p examples/jsonschema-codegen fetchRegistrySchema

# Custom URL
./gradlew -p examples/jsonschema-codegen fetchRegistrySchema \
  -Pobjs.schemaUrl='http://localhost:8081/api/v1/objs/registry/export?format=json-schema-codegen&includeEdgePropertySchemas=true'

# Generate POJOs, compile, smoke-test
./gradlew -p examples/jsonschema-codegen test
```

Or copy `objs-catalog.codegen.schema.json` from the workbench into
`src/jsonschema/registry-catalog.codegen.schema.json`.

Note: jsonschema2pojo may normalize all-caps type names (`API` → `Api`).

## Endpoint

```
GET /api/v1/objs/registry/export?format=json-schema-codegen
```

Same edge options as `json-schema` (`includeEdges`, `includeEdgePropertySchemas`, `dialect`).
Adds a synthetic root (`ObjsCatalog`) that `$ref`s every `$defs` entry and sets each def `title`
to the def key so class names stay PascalCase.
