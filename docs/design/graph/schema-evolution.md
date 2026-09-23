# Schema evolution — payload upgrade (L2)

Living design for C-35. Normative detail: [`docs/workitems/planned/codegen-schema-evolution/DESIGN.md`](../../workitems/planned/codegen-schema-evolution/DESIGN.md) and [`GAPS.md`](../../workitems/planned/codegen-schema-evolution/GAPS.md).

## Intent

- Keep **latest-only** consumer object models (Lane A codegen).
- Upgrade older stored pins for **typed read / examine** without rewriting the store.
- Fingerprints / frozen versions remain **evidence** (as-saved); APIs may also return **examine** (latest projection).

## Step kinds

Shared runtime: `SchemaUpgradeStep.apply(Map, PayloadMapper) → Map`.

| Kind | Author implements | Use when |
|------|-------------------|----------|
| `ClassToClassUpgradeStep<F,T>` | `upgrade(F): T` | Renames, splits, semantic remaps |
| `MapToClassUpgradeStep<T>` | `upgrade(Map, mapper): T` | Additive defaults, light map surgery toward typed `T` |

Reject app-facing Map→Map. Compaction / decomposition (graph topology) are out of scope (L1/L3).

## Hydrate order (`UPGRADE_TO_LATEST`)

1. Exact `(type, schemaVersion)` binding
2. Explicit upgrade chain
3. `DefaultAdditiveMapToLatest` (strict Map → latest class)
4. Raw + diagnostic (fail-open)

## Lane B

Historical snapshot DTOs (`Type_1_0_0`) from a **second snapshot export** (B1 all ENTITY versions). Separate package from Lane A. Latest class is Lane A only.

## Regression packs

Frozen `evidence.json` + `expect-latest.json` fixtures; accumulate across ontology bumps.

Fingerprint BOM GET (SBOM):

```http
GET .../fingerprints/{id}?representation=both
```

| Value | Behavior |
|-------|----------|
| `saved` | Evidence only (as-stored pin + payload) |
| `latest` | Examine projection only |
| `both` | Evidence fields + `latestSchemaVersion` / `latestPayload` (fingerprint default) |

Hand steps live outside generated trees (e.g. SBOM `org.poc.objs.sbom.migration`).

## Recipes (sketches)

### ClassToClass rename

```text
class Product_1_0_0_to_2_0_0 : ClassToClassUpgradeStep<Product_1_0_0, Product>() {
  type = "Product"; from = "1.0.0"; to = "2.0.0"
  upgrade(from) = Product(displayName = from.name, tier = "STANDARD")
}
```

Register in `InMemorySchemaUpgradeRegistry`. Callers use `HydrationPolicy.UPGRADE_TO_LATEST`.

### MapToClass additive defaults

```text
class Product_1_0_0_to_2_0_0_Additive : MapToClassUpgradeStep<Product>() {
  upgrade(map, mapper) {
    val p = mapper.fromMap(map, Product::class)   // or lenient bind
    if (p.tier == null) p.tier = "STANDARD"
    return p
  }
}
```

Use when shapes are mostly compatible; prefer ClassToClass for renames.

### Additive-only bump (no hand step)

If property names are unchanged and new fields are optional, omit the step.
`UPGRADE_TO_LATEST` falls through to **DefaultAdditiveMapToLatest** (strict Map → latest class).
Rename without a step → fail-open raw.

### Graph HTTP: as-stored vs latest (two routes)

Evidence and examine can be **separate mappings** (clearer than a query flag when clients always want one side).

```text
# Evidence — pin + payload exactly as stored (fingerprint / frozen graph)
GET  /api/v1/.../graphs/{graphId}/versions/{n}
GET  /api/v1/.../graphs/{graphId}/versions/{n}/raw     # alias if useful

# Examine — same fragment, entities projected to latest schema via L2
GET  /api/v1/.../graphs/{graphId}/versions/{n}/latest
```

Sketch flow:

```text
load GraphFragment from store (unchanged)

raw handler:
  return entities/edges as stored
    (schemaVersion + payload = evidence)

latest handler:
  for each entity:
    PayloadUpgrade.hydrate(..., UPGRADE_TO_LATEST, registry, targets)
    emit { id, type, schemaVersion: stored,   # honesty
           effectiveSchemaVersion, payload: upgradedMap | null }
  edges unchanged (L2 does not invent membership)
```

Controller sketch:

```text
@GetMapping(".../versions/{n}")
fun raw(...): GraphView = toView(fragment, representation = SAVED)

@GetMapping(".../versions/{n}/latest")
fun latest(...): GraphView = toView(fragment, representation = LATEST)
```

Same idea with one resource + query (SBOM fingerprints today):

```text
GET .../fingerprints/{id}?representation=saved|latest|both
```

**Do not** write upgraded payloads back on these GETs (read projection only).

### TypedGraphView (in-process)

```text
TypedGraphView.from(
  fragment, bindings, mapper,
  HydrationPolicy.UPGRADE_TO_LATEST,
  upgrades, targets)
// ReadNode.schemaVersion      = stored
// ReadNode.effectiveSchemaVersion / hydratedPayload = examine
```

## Related

- [`api-and-codegen.md`](api-and-codegen.md) — ownership / GeneratedReadView
- [`codegen-and-builder.md`](codegen-and-builder.md) — generator pipeline
- [`validation.md`](validation.md) — pin-and-keep persist rules
- SBOM example: `examples/sbom/sbom-service/.../migration/README.md`
