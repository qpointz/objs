# DESIGN — Codegen schema evolution (typed deserialize + migrations)

**Story:** [`STORY.md`](STORY.md) · **Backlog:** C-35 · **Status:** aligned with [`GAPS.md`](GAPS.md) (WI-001)  
**Audience:** foundation (`objs-api`, `objs-codegen-java`) and application authors who own ontology + generated bindings  
**Related:** [C-23 objs-api-codegen](../../completed/20260828-objs-api-codegen/STORY.md), [G-18 / G-30](../../completed/20260828-objs-api-codegen/GAPS.md), [`api-and-codegen.md`](../../../design/graph/api-and-codegen.md), [`codegen-and-builder.md`](../../../design/graph/codegen-and-builder.md), [`model.md`](../../../design/graph/model.md), [`validation.md`](../../../design/graph/validation.md)

This document captures the design intent for **multi-version payload serialization/deserialization** with **controlled upgrades**, while keeping a **minimal typed object model** for consumers and a **hard split** between generated code and hand-maintained migrations.

---

## 1. Problem statement

### 1.1 What objs already does

Objs treats **schema version** as a first-class catalog and persistence pin:

- Catalog: many `Schema(type, version, …)` rows may coexist for one logical type.
- Instances: `Entity` / edge properties carry `schemaVersion` and a JSON **payload map**.
- Writes validate against the **exact** pinned schema; reads are lenient and do not rewrite payloads.
- Workbench / catalog helpers can compute “latest” for UX; that does **not** migrate stored data.

This is intentional **pin-and-keep** coexistence (see graph model / validation docs).

### 1.2 What codegen does today (C-23)

Application codegen is a two-pass pipeline:

1. **jsonschema2pojo** — payload DTOs from the codegen JSON Schema export.
2. **`objs-codegen-java`** — graph bindings (`*Node`, `*ReadNode`, `GraphMutationBuilder`, `GeneratedReadView`).

The export puts **latest-only** entity schemas into `$defs` / `definitions`. Generated Java types are named by type (`Product`), not by version. Each binding hard-codes one `SCHEMA_VERSION` (the latest at export time).

Typed read hydration (`TypedEntityBindingRegistry`) is **exact** `(type, schemaVersion)`. Unknown or historical pins stay **raw** (`hydratedPayload == null`) while the underlying `Entity` remains readable (G-17 / G-18).

### 1.3 Gap

When a graph (or pool) still contains `Product@1.0.0` after the app has moved codegen to `Product@2.0.0`:

- consumers cannot treat every node as typed `Product`;
- there is no supported place to put **semantic** upgrade logic (rename field, split object, supply default, reinterpret enum);
- generating a full object-model family per historical version would inflate API surface and still would not encode upgrades.

**Deferred C-23 G-30** (evolved-snapshot consumer fixture) explicitly left this unproven.

### 1.4 Desired outcomes (product language)

| Capability                | Intent                                                                                                     |
| ------------------------- | ---------------------------------------------------------------------------------------------------------- |
| Serialize any version     | Persistence and raw APIs can still write/read any catalog pin; typed **write** API stays latest-centric    |
| Deserialize with control  | Older stored pins can be presented as the **current** typed model when the app supplies upgrade steps      |
| Minimal consumer model    | Callers primarily program against **one** generated type family per entity type (latest)                   |
| Transparent consumption   | Normal read path: `view.products()` / `payload()` yields latest-shaped typed objects when upgrades succeed |
| Safe evolution authorship | Upgrade logic is **typed and hand-reviewed**, not stringly-typed map surgery                               |

---

## 2. Design principles

1. **Storage maps are not an authoring API.** JSONB/`MutableMap` is the wire and persistence boundary. Application migrations must not be written primarily as `Map → Map` (too flexible, too error-prone: typos, silent drops, no rename checking).
2. **Migrations are domain code.** Field remaps, defaults, splits/merges, and semantic reinterpretation cannot be inferred safely from JSON Schema alone. They belong in **hand-maintained** sources.
3. **Codegen is overwrite-safe; migrations are accumulate-safe.** Re-exporting the catalog may regenerate DTOs and bindings. It must **never** delete or overwrite migration implementations.
4. **Two lanes of generation, one lane of hand code.** Object-model codegen (consumer API) stays latest-only. Optional **schema-snapshot** DTOs may be generated solely as typed migration endpoints. Migration **bodies** are never generated.
5. **Transparency with honesty.** Consumers see latest-shaped payloads when upgrades run; diagnostics must still expose the **stored** pin so debugging and identity rules stay truthful.
6. **Lossless fallback.** If a chain is incomplete or a step fails, retain G-17 behavior: raw `Entity` remains available; do not drop graph members.
7. **Read upgrade ≠ persist migrate.** v1 of this design upgrades for **typed views**. Rewriting stored `schemaVersion` + payload is a separate, explicit operation (deferred / GAP).

---

## 3. Rejected approaches

### 3.1 App-facing `Map → Map` migration SPI

**Rejected as the primary authoring model.**

Why it looks attractive: one SPI, no historical classes, maximum flexibility.

Why it fails in practice:

- Keys are strings; renames do not fail the compiler.
- Nested objects/arrays are easy to mishandle.
- Required fields and type changes are unchecked until hydrate or persist validation.
- Reviews cannot see intent (`old.name → new.displayName`) as clearly as typed field access.

Maps remain the **internal** form between Jackson `convertValue` hops and the database. Authors do not implement `upgrade(Map): Map` as the supported API.

### 3.2 Full object-model per historical version

**Rejected as the default consumer surface.**

Generating `ProductNode` / `ProductReadNode` / builder methods per version:

- explodes API surface;
- forces callers to branch on version everywhere;
- still does not define how to *prefer* a single programming model;
- collides with today’s `definitionKey = PascalCase(type)` uniqueness rules unless naming is versioned everywhere.

Historical **payload DTOs** (Lane B) may still exist; historical **graph handles** do not.

### 3.3 Jackson `@JsonTypeInfo` polymorphism on entities

**Rejected as the graph hydrate mechanism.**

Objs already dispatches on `(type, schemaVersion)` via bindings. Entity identity and relations live outside the payload POJO. Polymorphic Jackson on a single payload class hierarchy fights the pin-and-keep model and couples codegen to a sealed subtype tree.

### 3.4 Silent auto-rewrite on every mutate

**Rejected for v1.**

Automatically rewriting every touched entity to latest on persist mixes read convenience with durable migration, complicates identity immutability (C-14), and surprises operators who relied on pin-and-keep. An **explicit** rewrite API may come later.

### 3.5 Inferred migrations from schema diff

**Rejected as a substitute for hand steps.**

Schema diffs can *suggest* renames; they cannot decide semantics. Tooling hints are out of scope; authors write steps.

---

## 4. Architecture overview

### 4.1 Lanes

```text
┌─────────────────────────────────────────────────────────────────────────┐
│ Lane A — Object-model codegen (consumer API)                            │
│   jsonschema2pojo (latest) + objs-codegen-java                          │
│   Product, ProductNode, ProductReadNode, GeneratedReadView, …           │
│   Overwrite on re-export. NO migration logic.                           │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ Lane B — Schema-snapshot DTOs (migration I/O only)                      │
│   Versioned payload classes: Product_1_0_0, Product_2_0_0, …            │
│   OR package-per-version. Generated from catalog schemas.               │
│   Overwrite on re-export. NO migration logic. NOT a graph API.          │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ Hand lane — Migrations (application-owned)                              │
│   …migration / …upgrades package under src/main (never generated/)      │
│   SchemaUpgradeStep<From,To> implementations + registry assembly        │
│   Accumulate over time. Survive Lane A/B regeneration.                  │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ Foundation — objs-api SPI + PayloadMapper                               │
│   Contracts, chain composition, hydrate policy, diagnostics             │
│   fromMap / toMap at boundaries only                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Read path (provisional default: upgrade-to-latest)

```text
Stored Entity
  type = "Product"
  schemaVersion = "1.0.0"          ← stored pin (unchanged in DB)
  payload = { … shape of 1.0.0 … }

        │
        ▼
┌───────────────────┐
│ Exact binding?    │──yes──► PayloadMapper.fromMap(…, Product.class)  [Lane A]
│ (pin == latest)   │
└─────────┬─────────┘
          │ no
          ▼
┌───────────────────┐
│ Registry.findChain│──missing──► raw ReadNode (hydratedPayload = null)
│ Product 1.0→2.0   │
└─────────┬─────────┘
          │
          ▼
  for each typed step:
      From f = mapper.fromMap(payload, step.fromClass)   // Lane B (or prior To)
      To   t = step.upgrade(f)                           // HAND CODE
      payload = mapper.toMap(t)
          │
          ▼
  PayloadMapper.fromMap(payload, Product.class)          // Lane A latest
          │
          ▼
  ProductReadNode.payload() → Product                    // transparent to consumer
  diagnostics: storedSchemaVersion=1.0.0, effective=2.0.0
```

### 4.3 Write / serialize path

| Path                                  | Behavior                                                                                               |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| Typed mutation builder (`addProduct`) | Emits **latest** pin + latest-shaped map (Lane A). Unchanged from C-23.                                |
| Raw / hand-built `Entity`             | Any catalog pin; persist validation as today. This is “serialize any version.”                         |
| Optional helper (later)               | `mapper.toMap(laneBDto)` + explicit `schemaVersion` for tests/tools — not required for v1 consumer API |
| Downgrade (latest → older pin)        | **Out of v1.** Harder than upgrade; lossy; not required for “read old as new.”                         |

### 4.4 Ownership matrix

| Artefact | Owner | May contain remap logic? | Regenerated? |
|----------|-------|--------------------------|--------------|
| Lane A latest DTO + nodes / ReadView / builder | Codegen | No | Yes |
| Lane A exact binding for latest pin | Codegen | No | Yes |
| Lane B versioned snapshot DTOs | Codegen (preferred) or hand (GAP) | No | Yes if generated |
| `SchemaUpgradeStep` implementations | **Hand** | **Yes** | **No** |
| Upgrade registry assembly | **Hand** | Registration only | **No** |
| `SchemaUpgradeRegistry` / chain engine / hydrate policy | `objs-api` | Contracts + orchestration | N/A |
| `PayloadMapper` | App-configured Jackson via API | Codec only | N/A |

---

## 5. Separation rules (normative intent)

These rules should survive WI-001 as invariants unless explicitly revised in GAPS.

1. **Migration source roots** live under ordinary application `src/main` (or a dedicated non-generated source set). Never under `build/generated/sources/jsonschema2pojo`, `typed-bindings`, or similar.
2. **Codegen must not emit migration method bodies**, TODO stubs, or “fill in this upgrade” classes inside generated trees. Those get wiped and blur ownership.
3. **Lane A may accept an injected registry** (`GeneratedReadView.from(graph, mapper, upgrades)`). Injection is a seam; registration content is hand-owned.
4. **Lane B types are not object-model types.** No per-version `*Node`, no versioned root collections on `GeneratedReadView`.
5. **Latest duality:** Prefer a **single** physical class for “latest Product” shared by Lane A and the newest Lane B snapshot (or an explicit alias policy locked in WI-001) so the last hop does not convert between two identical shapes accidentally.
6. **Tests for migrations** are hand-written fixtures (feeds G-30). Generators do not invent migration tests.

### 5.1 Why Map→Map is still visible internally

Persistence and Jackson interop require maps. The foundation may:

```text
Map  --fromMap-->  FromDto  --upgrade()-->  ToDto  --toMap-->  Map  --> …
```

Authors only implement `upgrade(FromDto): ToDto`. The map hops are mechanical and owned by the runtime + `PayloadMapper`.

---

## 6. Typed migration SPI (normative)

Package: `org.poc.objs.api.typed.upgrade`.

### 6.1 Shared runtime + kinds

```text
interface SchemaUpgradeStep {
  String type();
  String fromVersion();
  String toVersion();
  Map<String, ?> apply(Map<String, ?> payload, PayloadMapper mapper);
}

abstract class ClassToClassUpgradeStep<F, T> implements SchemaUpgradeStep {
  Class<F> fromClass();
  Class<T> toClass();
  T upgrade(F from);
  // apply: strict fromMap(F) → upgrade → toMap
}

abstract class MapToClassUpgradeStep<T> implements SchemaUpgradeStep {
  Class<T> toClass();
  T upgrade(Map<String, ?> from, PayloadMapper mapper);
  // apply: upgrade(map) → toMap
}
```

- Prefer pure / deterministic steps; no graph topology (no invent membership).
- Adjacent hops preferred; explicit long-jump allowed as a registered edge; multi-path → error (G-E8).
- **Reject** Map→Map authoring kind (G-X2).

### 6.2 Registry

```text
interface SchemaUpgradeRegistry {
  List<SchemaUpgradeStep> findChain(String type, String fromVersion, String toVersion);
  // empty ⇒ no explicit path
}
```

`InMemorySchemaUpgradeRegistry` in `objs-api` indexes `(type, from, to)` and builds a path.

### 6.3 Hydration policy + additive fallback

```text
enum HydrationPolicy {
  EXACT_ONLY,
  UPGRADE_TO_LATEST,   // explicit chain, else DefaultAdditiveMapToLatest, else raw
}
```

`UPGRADE_TO_LATEST` resolution:

1. Exact binding for stored pin → hydrate
2. Complete explicit `findChain(stored → latest)` → apply steps → hydrate latest binding
3. Else `DefaultAdditiveMapToLatest` (strict Map → latest class) — safe for additive optional fields only
4. Else fail-open raw + diagnostic

Incomplete explicit chain: **no** mid-chain additive patch → raw.

### 6.4 Diagnostics

- `storedType`, `storedSchemaVersion` (also `ReadNode.schemaVersion` = stored)
- `effectiveSchemaVersion`
- `upgradeStepsApplied` / `additiveFallback`
- `upgradeFailure`

### 6.5 Evidence / examine (overwire)

Fingerprints / frozen versions are **evidence** (as-saved). Examine projects latest via L2.

- Fingerprint BOM GET default: `representation=both`
- Live inventory default: `saved`
- Override: `representation=saved|latest|both`

### 6.6 Regression packs

Frozen packs: `evidence.json` + `expect-latest.json` + meta. Accumulate across ontology bumps. Foundation unit packs + SBOM packs (G-30).

### 6.7 Endpoint recipes (sketch)

Evidence vs examine can be a query param (`representation=…`) **or** two route mappings:

```text
GET .../graphs/{id}/versions/{n}         → raw as stored
GET .../graphs/{id}/versions/{n}/latest  → L2 upgrade-to-latest projection
```

Load the fragment once; do not rewrite storage on GET. Full sketches/recipes:
[`schema-evolution.md`](../../../design/graph/schema-evolution.md).

---

## 7. Lane B — schema-snapshot DTOs

### 7.1 Purpose

Give migration authors **compile-time** field access for historical shapes without making those shapes the application’s programming model.

### 7.2 Naming / packaging (locked)

**Version-suffixed types** (`Product_1_0_0`) in a dedicated package / source set, separate from Lane A OM (G-E1, G-E13).

### 7.3 Generation scope (locked)

**B1:** generate Lane B for all catalog ENTITY versions. Latest physical class is Lane A only (G-E2).

### 7.4 Export contract impact

Today [`FullCatalogJsonSchemaExporter.exportForCodegen`](../../../design/graph/object-schema-dsl.md) places **latest-only** into `$defs`. Lane B needs either:

- versioned `$defs` keys for retained schemas, **or**
- a second export document / Gradle input dedicated to snapshots.

Lane A consumer export can remain latest-oriented for OM bindings. Do not force Lane A `definitionKey` collisions: historical entries must use versioned keys when present in the same document.

`x-objs-codegen.schemas` already lists historical pins for metadata; align Lane B generation with that index.

### 7.5 Relationship to jsonschema2pojo

Lane B is still “ordinary beans” from JSON Schema — same Pass-1 tool, different input subset and package/typeName overrides (`codegen.java.typeName` already exists for collisions). Lane A Pass-2 (`objs-codegen-java`) **ignores** Lane B definitions for node/builder emission (filter: only `generated` OM definitions / latest flag).

---

## 8. End-to-end examples (design sketches)

### 8.1 Rename + default

Catalog:

- `Product@1.0.0`: `{ "name": string }`
- `Product@2.0.0`: `{ "displayName": string, "tier": string }`  // tier required

Hand step:

```text
Product_2_0_0 upgrade(Product_1_0_0 from) {
  return new Product_2_0_0()
      .withDisplayName(from.getName())
      .withTier("STANDARD");  // intentional default
}
```

Stored row stays `schemaVersion=1.0.0` on read. Typed consumer sees `displayName` / `tier`.

### 8.2 Mixed graph

Graph contains `Product@1.0.0` and `Product@2.0.0`. With registry covering `1.0.0→2.0.0`:

- both appear in `view.products()` as Lane A `ProductReadNode` with latest-shaped payload;
- stored pins differ; diagnostics distinguish them.

Without the step: `2.0.0` hydrates; `1.0.0` is raw (or fail-closed — GAP).

### 8.3 Serialize any version

Integration test / admin tool builds:

```text
Entity(type="Product", schemaVersion="1.0.0", payload=mapper.toMap(productV1Dto))
```

Persist validates against `1.0.0`. No Lane A builder involvement. This satisfies “serialize any version” without versioned mutation builders.

---

## 9. Interaction with existing rules

### 9.1 Validation (persist)

Unchanged: writes validate against the **pinned** schema. Read-time upgraded maps are **not** automatically re-persisted; therefore read upgrade need not satisfy persist validation unless an explicit rewrite API runs (then validate against **target** pin).

### 9.2 Identifier immutability (C-14)

Applies when an update **changes** `schemaVersion` on a stored entity. Read-view upgrade does not change storage, so C-14 does not fire. Explicit rewrite must project identifiers per existing cross-version rules.

### 9.3 In-place catalog Save (same version, new definition)

Workbench can revise a schema document **without** bumping version. That is **not** what migration chains address. Migrations are **version-to-version**. Same-pin definition drift remains “reads may be non-conforming; later writes may fail” (validation principles). Document this limit clearly for operators.

### 9.4 Graph snapshots / HEAD versions (C-18)

Orthogonal. A frozen graph may contain entities with various schema pins; upgrade-on-read still applies when building a typed view over that fragment.

### 9.5 Exact bindings (G-18)

Remain. Upgrade policy **extends** hydrate when exact binding is absent but a chain reaches a bound version (typically latest). It does not remove exact-match behavior for pins that still have bindings.

---

## 10. Transparency for object-model consumers

**Happy path:** application code looks like today’s C-23 read code. Authors pass an upgrade registry into the view factory once at the composition root.

```text
GeneratedReadView view = GeneratedReadView.from(graph, mapper, upgrades);
Product p = view.products().get(0).payload();
// p is latest-shaped even if stored pin was older
```

**Unhappy path:** no silent partial POJO. Either raw node + diagnostic, or fail-closed policy. Do not hydrate a latest class from an unmigrated old map via lenient Jackson (that reintroduces Map→Map hazards at the boundary). Provisional rule: **only hydrate Lane A after a successful chain or exact pin match.**

---

## 11. Testing strategy (for later implementation WIs)

1. **Unit:** chain composition; missing hop; step exception; `fromMap` failure mid-chain.
2. **Unit:** single rename/default step round-trip map → From → To → map → Lane A.
3. **Consumer fixture (G-30):** catalog with Product 1.0 + 2.0; store entity at 1.0; OM codegen at 2.0; hand migration; assert typed read.
4. **Regeneration safety:** document/check that migration sources are outside generated dirs (convention test or README in example).
5. **Raw fallback:** delete step from registry → entity still listed via raw API.

---

## 12. Implementation sketch (future — not this park)

Ordered only for orientation; WI-001 may reshuffle.

1. Lock GAPS; promote this DESIGN into living `docs/design/graph/` (section or sibling doc).
2. `objs-api`: SPI types, in-memory registry, hydrate integration, diagnostics, tests.
3. Export: versioned defs or snapshot export for Lane B; keep Lane A latest-only for OM.
4. `objs-codegen-java`: accept upgrade registry on generated facade; do not emit steps; ignore Lane B for nodes.
5. Example under `examples/codegen/…`: hand migration + G-30 fixture.
6. Docs: api-and-codegen, codegen-and-builder, close G-30.

---

## 13. Deferred decisions

See [`GAPS.md`](GAPS.md): G-E5, G-E6, G-E7, G-X8, etc.

---

## 14. Summary

Objs already coexists schema versions in storage. Codegen keeps a **small, latest-centric object model**. Bridging older pins uses **step kinds** (ClassToClass / MapToClass), an **additive default fallback**, and **evidence/examine** dual wire for fingerprints. Generated Lane B DTOs support typed hops; hand steps own semantics; store pins stay put.
