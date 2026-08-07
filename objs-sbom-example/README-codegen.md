# SBOM JSON Schema → Java codegen

[jsonschema2pojo](https://www.jsonschema2pojo.org/) generates **Java** only (not Kotlin).

## Layout

| Path | Role |
|------|------|
| [`src/jsonschema/sbom-catalog-linked.schema.json`](src/jsonschema/sbom-catalog-linked.schema.json) | Linked full-catalog schema (committed; input to the plugin) |
| [`src/jsonschema/types/*.json`](src/jsonschema/types/) | Payload-only per-type schemas (Wave*-shaped; no relation props) |
| `build/generated/sources/jsonschema2pojo/` | Generated Java under `org.poc.objs.sbom.generated` |

Linked schema includes outbound + inbound relation props (e.g. `Database.containsDataset`, `Dataset.containsFromDatabase`) for object-model codegen.

## Commands

```bash
# Refresh committed schema from SbomRegistry (after ontology / exporter changes)
./gradlew :objs-sbom-example:exportSbomJsonSchema

# Generate Java + compile + smoke-test linked relations
./gradlew :objs-sbom-example:test --tests org.poc.objs.sbom.generated.GeneratedSbomModelTest
```

`generateJsonSchema2Pojo` runs automatically before `compileKotlin` / `compileJava`.

## Replacing WaveATypes / WaveBCDTypes

Hand-written Kotlin `*Payload` / `*Type` helpers remain the TypedEntity source of truth for now.

Generated linked types are a **superset** (payload fields + graph navigation). Migrating off Wave* means:

1. Point `TypedEntity` / builders at generated Java classes (or payload-only generation from `types/*.json`).
2. Decide how relation fields map — they are **not** stored inside entity payloads today; strip or `@JsonIgnore` them when writing to the graph, or treat generated types as DTOs outside TypedEntity.
3. Keep thin Kotlin `EntityTypeMeta` facades if useful.

Do not delete Wave* until that migration is done and tests (including seed parity) stay green.
