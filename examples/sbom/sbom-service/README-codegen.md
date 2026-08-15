# SBOM JSON Schema export

Committed linked catalog schema for the SBOM example ontology. Hand-written Kotlin `Wave*` /
`*Payload` types remain the TypedEntity source of truth (no jsonschema2pojo on the compile path).

## Layout

| Path | Role |
|------|------|
| [`src/jsonschema/sbom-catalog-linked.schema.json`](src/jsonschema/sbom-catalog-linked.schema.json) | Linked full-catalog schema (committed) |

`exportSbomJsonSchema` can also write payload-only per-type schemas under `src/jsonschema/types/`
(not committed; Wave*-shaped, no relation props).

## Commands

```bash
# Refresh committed schema from SbomRegistry (after ontology / exporter changes)
./gradlew :sbom-service:exportSbomJsonSchema
```
