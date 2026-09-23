# Schema upgrades (C-35)

Hand migration steps live under `org.poc.objs.sbom.migration` — **never** under generated sources.

- `Component_1_0_0_to_2_0_0` — ClassToClass rename `name`→`displayName` + default `tier`
- `SbomSchemaUpgradeConfiguration` — registry, targets, strict `PayloadMapper`

## Fingerprint BOM (shipped)

```http
GET /api/v1/inventory/applications/{id}/versions/{versionId}/fingerprints/{fingerprintId}?representation=both
```

| `representation` | Meaning |
|------------------|---------|
| `saved` | Evidence: as-stored pin + payload |
| `latest` | Examine: L2-upgraded payload only |
| `both` | Evidence fields + `latestSchemaVersion` / `latestPayload` (default) |

## Sketch: two graph routes (raw vs latest)

Prefer separate mappings when clients always pick one side:

```http
GET /api/v1/.../graphs/{graphId}/versions/{n}          → raw / as stored
GET /api/v1/.../graphs/{graphId}/versions/{n}/latest   → migrated to latest
```

```text
fragment = namedGraphs.getGraphVersion(graphId, n)   # immutable evidence

GET .../versions/{n}
  assets = fragment.entities.map { asStored(it) }

GET .../versions/{n}/latest
  assets = fragment.entities.map { examine(it) }     # PayloadUpgrade / AssetExamine
  # keep stored schemaVersion on the wire for honesty; put upgraded shape in payload
  # or return effectiveSchemaVersion + latestPayload explicitly
```

Do not persist the examine projection on GET. See living doc
[`schema-evolution.md`](../../../../../../docs/design/graph/schema-evolution.md) recipes.
