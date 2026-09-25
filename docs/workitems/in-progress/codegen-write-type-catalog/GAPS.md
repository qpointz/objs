# Gaps — codegen-write-type-catalog (C-43)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**WI-001 confirms every design row** (resolve or defer) before generator code. Planning locks below are **resolved** unless WI-001 explicitly revises them.

---

## Open (design)

_(none — all G-W* design rows resolved or deferred)_

---

## Locked (design)

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-W1 | Catalog name / package | **resolved** | **Same package** as `*Node` / `*Type`. Prefer **`EntityCatalog`** (app-generated; not `GeneratedTypeCatalog`). Optional longer name `ObjsEntityCatalog` only if collision risk in consumer package — default short `EntityCatalog` |
| G-W2 | Surface API | **resolved** | **Ship full conversion surface** (not minimal). Exact overload set locked below; WI-001 may only tweak names/visibility, not drop conversions. |
| G-W3 | Lookup key | **resolved** | **Exact `Class<?>` match only** (`payload.getClass()` / passed `Class`). No interface walk, no superclass walk. Rationale: generated payloads are concrete jsonschema2pojo classes; no shared payload interfaces today; subclasses are unlikely and would be a deliberate app choice (register explicitly later if ever needed). Proxies / enhanced subclasses → miss → **IAE** (fail closed) |
| G-W4 | Multi-version / Lane B | **resolved** | **Lane A only** (`generated=true` latest ENTITY / EDGE_PROPERTIES pins). Same Java payload class for multiple versions → **latest wins** (one `Class` → one META); do not emit duplicate Class keys. **No Lane B** classes in these catalogs. **Optionally coupled** with C-35 migrate-to-latest ([`schema-evolution.md`](../../../design/graph/schema-evolution.md) / `HydrationPolicy.UPGRADE_TO_LATEST`): apps *may* upgrade an older stored pin then call catalog `toNode`/`toTyped`; not required for catalog use (exact-pin `fromEntity` / raw path remains valid). Document both paths in `typed-conversion-recipes.md`. No mandatory catalog→upgrade dependency |
| G-W5 | EDGE_PROPERTIES catalog | **resolved** | **Separate `EdgeCatalog`** (same package). `EntityCatalog` = ENTITY only; `EdgeCatalog` = EDGE_PROPERTIES only. No joined map / `kind` discriminant. Edge catalog: meta + `toMap` (no `toEntity` / `toNode`). **Later (out of C-43 unless WI-001 pulls in):** optional facade **`GraphCatalog`** wrapping `EntityCatalog` + `EdgeCatalog` under the hood — not required for v1; apps can compose manually |
| G-W6 | Builder `add` | **resolved** | **Both in-story (WI-003):** generated `GraphMutationBuilder.add(Object payload)` / `add(UUID id, Object payload)` via `EntityCatalog`, **and** `add(GeneratedNode<?> node)` / `add(TypedEntity<?> typed)` (register via `toEntity`). Keep existing typed `addProduct` / `addDataset` methods. Unknown payload → IAE (G-W8). Edge property DTOs are **not** `add(Object)` targets (use relation methods + `EdgeCatalog`) |
| G-W7 | Consumers | **resolved** | **Prove** in `:examples/codegen/jsonschema` **and** `jsonschema-draft07` (full conversion + identity path tests). **Smoke** in **SBOM** + **AR** (minimal: regenerate + one catalog conversion / identity lookup test each — not a full service rewrite). **Docs:** dedicated design doc with convert recipes (WI-005) — see G-W7a |
| G-W7a | Conversion recipes doc | **resolved** | Add living design doc under `docs/design/graph/` (suggested name **`typed-conversion-recipes.md`**) covering EntityCatalog/EdgeCatalog convert paths: payload↔map, →Entity/Node/TypedEntity, ←fromEntity, identity lookup, validate/create sketch; **optional** older-pin → upgrade-to-latest → catalog (G-W4 / C-35). Cross-link `codegen-and-builder.md`, `schema-evolution.md`, `programmatic-recipes.md`, `persist-sketch.md`. Not a foundation Spring bean |
| G-W8 | Failure type | **resolved** | Catalog misuse (unknown `Class`, null payload where required, calling entity conversion on edge catalog / vice versa) → **`IllegalArgumentException`**. Do **not** invent a catalog-specific `ObjsException` subclass for ordinary lookup/convert failures. **`ObjsException`** (and subtypes) remain for **objs-runtime / domain-specific** errors elsewhere — catalogs may *propagate* them if a callee throws, but catalog entrypoints themselves use IAE |
| G-W9 | Ownership | **resolved** | Catalog is **application-generated** (`objs-codegen-java` tool; output in consumer source set). Not a root `objs-*` ontology class (G-22 / G-24) |
| G-W10 | Motivation | **resolved** | jsonschema2pojo beans lack meta; per-type service methods are the pain; read catalog already exists — write inverse is missing |
| G-W11 | Identity path | **resolved** | Catalog does **not** replace `IdentityProjection` / `findEntitiesByIdentity`; it only supplies meta + `Entity` / node from a payload |
| G-W12 | Payload common base | **resolved** | **Out of story** by default; catalog alone unblocks generic services. Optional jsonschema2pojo superclass is a later story if needed |
| G-W13 | Store layer | **resolved** | No new `GraphStore` API in this story |

### G-W2 — locked conversion surface (detail)

**`EntityCatalog`** (ENTITY only) — generate **all** of:

| Family | Methods (names may tweak in WI-001) |
|--------|-------------------------------------|
| Lookup | Exact `Class` only (G-W3): `contains`/`supports(Class)`; `meta(Class)`; `meta(Object)` via `payload.getClass()` |
| Class handles | `Class<?> payloadClass(EntityTypeMeta)` (or `meta` → payload `Class`); `Class<? extends GeneratedNode<?>> nodeClass(EntityTypeMeta)` when useful |
| Payload ↔ map | `Map<String,Object> toMap(Object payload, PayloadMapper)`; ` <P> P fromMap(Map, Class<P>, PayloadMapper)` |
| → Entity | `Entity toEntity(Object payload, PayloadMapper)`; `toEntity(UUID id, Object payload, PayloadMapper)`; `toEntity(TypedEntity<?>, PayloadMapper)`; `toEntity(GeneratedNode<?>, PayloadMapper)` |
| → TypedEntity | `TypedEntity<?> toTyped(Object payload)`; `toTyped(UUID id, Object payload)`; reuse `TypedEntity.fromEntity` path: `toTyped(Entity, PayloadMapper)` |
| → Node | `GeneratedNode<?> toNode(Object payload)`; `toNode(UUID id, Object payload)` via `*Type.node(...)`; `toNode(Entity, PayloadMapper)` (hydrate payload then wrap) |
| ← Entity (read back) | `fromEntity(Entity, PayloadMapper)` → `GeneratedNode<?>` / `TypedEntity<?>` (exact return type: prefer **`GeneratedNode<?>`** for generic callers; document cast or typed helpers) |

**Return typing:** generic methods return `GeneratedNode<?>` / `TypedEntity<?>` / `Entity`. Optional generated **per-type overloads** (`DatasetNode toDatasetNode(...)`) are **in** if cheap (codegen loop); not a substitute for the generic surface.

**`EdgeCatalog`** (EDGE_PROPERTIES only) — generate **all** of:

| Family | Methods |
|--------|---------|
| Lookup | `contains` / `supports`; `EntityTypeMeta meta(Class\|Object)` (property schema key) |
| Payload ↔ map | `toMap(Object, PayloadMapper)`; `fromMap(Map, Class, PayloadMapper)` |
| **Not** | `toEntity`, `toNode`, `toTyped` — fail closed if ever added by mistake |

Unknown class / wrong catalog → fail closed with **`IllegalArgumentException`** (G-W8).

---

## Out of story

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | Shared jsonschema2pojo superclass for all payloads | **deferred** | Optional follow-up; not required for C-43 |
| G-X2 | Foundation Spring `TypedObjectService` in `objs-autoconfigure` | **deferred** | Apps own services; recipes/docs only |
| G-X3 | Auto upsert-by-identity in codegen | **deferred** | Application policy (AR write modes); catalog + store APIs suffice |
| G-X5 | `GraphCatalog` facade (`EntityCatalog` + `EdgeCatalog`) | **deferred** | Optional later wrapper; C-43 ships the two catalogs only |
