# SBOM JSON Schema codegen

The committed codegen catalog schema drives the generated Java payload DTOs and typed graph
bindings. The generated model is application-owned; the hand-written Kotlin `Wave*` /
`*Payload` types remain available for the existing SBOM service code.

## Layout

| Path | Role |
|------|------|
| [`src/jsonschema/sbom-catalog.codegen.schema.json`](src/jsonschema/sbom-catalog.codegen.schema.json) | Full catalog with payload and Objs codegen metadata |

The schema is consumed offline by both `jsonschema2pojo` and `objs-codegen-java`; the backend is
not required during compilation.

## Commands

```bash
# Generate and compile all SBOM bindings from the committed schema
./gradlew :sbom-service:compileJava
```
