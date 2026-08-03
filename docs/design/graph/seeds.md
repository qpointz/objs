# Graph configuration seeds

Extensible multi-document YAML configuration for schemas, allowed-edge rules, and graphs.

## Format

Each YAML document:

```yaml
apiVersion: objs.poc.org/v1
kind: ObjectSchema | AllowedEdgeRule | Graph
# kind-specific fields follow directly at the document root
```

Seed documents deliberately follow Mill's flat format. `apiVersion` and `kind` are the only
common envelope fields; there is no Kubernetes-style `metadata` / `spec` split.

Documents in one resource may appear in any order. The importer validates all documents first,
then applies them in dependency order: `ObjectSchema` → `AllowedEdgeRule` → `Graph`.

Unsupported `apiVersion` or `kind` values fail the whole resource.

## Identity and MERGE

- Schemas upsert by `(type, version)`.
- Allowed-edge rules upsert by `(sourceType, role, targetType)`. Optional `cardinality` wire
  values: `UNSPECIFIED` (default when omitted), `1:1`, `1:*`. Export always emits `cardinality`.

  Example:

  ```yaml
  apiVersion: objs/v1
  kind: AllowedEdgeRule
  sourceType: Product
  role: CONTAINS
  targetType: Component
  propertiesPolicy: SCHEMA
  propertiesSchemaType: ContainsEdge
  propertiesSchemaVersion: "1.0.0"
  emptyPropertiesAllowed: true
  cardinality: "1:*"
  ```
- Graph entities/edges use stable textual `key` values. Default ids are **UUIDv5** over
  `graphName/entity|edge/key` in the Objs seed namespace. Optional explicit `id` overrides that
  (used by REST export of existing rows).
- Omission never deletes (`MERGE` only; no `REPLACE` in v1).

## Startup loading

Configuration (`objs.seeds`):

```yaml
objs:
  seeds:
    enabled: true
    on-failure: FAIL_FAST   # or CONTINUE
    resources:
      - classpath:seeds/sbom-ontology.yaml
```

- Locations: `classpath:` and `file:` only.
- The normalized resource location is the ledger key; seed resources need no separate name.
- Ledger table `bom_seed_ledger` stores SHA-256 fingerprints (`sha256:…`).
- Identical successful fingerprints are skipped on restart.
- Failed attempts never overwrite the last successful fingerprint.
- Catalog hydration runs before seed loading.

## REST

| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/api/v1/objs/seeds/import` | Multipart YAML; same importer; no ledger write |
| `GET` | `/api/v1/objs/seeds/export` | Catalogs always; Graph when annotation params present |
| `GET` | `/api/v1/objs/seeds/export/graph` | Requires annotation filter |

## Extension

Register additional Spring beans implementing `SeedDocumentHandler` for new `kind` values.
The importer discovers handlers by kind; export uses the canonical serializer registry.

## SBOM example

Canonical ontology YAML (`classpath:seeds/sbom-ontology.yaml`) is the registry source of truth at
runtime. `objs-app` configures SBOM seed resources explicitly in its `sbom` Spring profile.
Typed `SbomRegistry.pack()` remains for builders and parity tests. Add
`classpath:seeds/sbom-demo-graph.yaml` to the profile's ordered resource list when sample data is
wanted.
