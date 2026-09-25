# Typed conversion recipes

Copy-paste patterns for **application-generated** write catalogs (`EntityCatalog`, `EdgeCatalog`)
produced by `:objs-codegen-java` next to `*Node` / `*Type` / `GraphMutationBuilder`.

**Not a foundation Spring bean.** Apps regenerate catalogs into their own package and own any
service that wraps them. Companion docs: [codegen-and-builder.md](codegen-and-builder.md),
[api-and-codegen.md](api-and-codegen.md), [programmatic-recipes.md](programmatic-recipes.md),
[persist-sketch.md](persist-sketch.md), [schema-evolution.md](schema-evolution.md).

**Locks (C-43):** exact `Class` match; Lane A latest pin only; catalog misuse →
`IllegalArgumentException` (not a catalog-specific `ObjsException`).

**Smoke / prove:** codegen `EntityCatalogProofTest` (jsonschema + draft-07);
SBOM `GeneratedSbomEntityCatalogSmokeTest`; AR `GeneratedAssetRepositoryBindingsTest`.

---

## Lookup

```java
if (!EntityCatalog.supports(payload.getClass())) {
    throw new IllegalArgumentException("unsupported payload");
}
EntityTypeMeta meta = EntityCatalog.meta(payload); // or meta(Product.class)
Class<?> payloadClass = EntityCatalog.payloadClass(meta);
Class<? extends GeneratedNode<?>> nodeClass = EntityCatalog.nodeClass(meta);
```

Unknown class → `IllegalArgumentException`. No interface or superclass walk.

---

## Payload ↔ map

```java
PayloadMapper mapper = new PayloadMapper(objectMapper);

Map<String, Object> map = EntityCatalog.toMap(payload, mapper);
Product roundTrip = EntityCatalog.fromMap(map, Product.class, mapper);
```

`EdgeCatalog` exposes the same `toMap` / `fromMap` surface for EDGE_PROPERTIES DTOs only.

---

## → Entity / TypedEntity / Node

```java
UUID id = UUID.randomUUID();

Entity entity = EntityCatalog.toEntity(payload, mapper);
Entity withId = EntityCatalog.toEntity(id, payload, mapper);

TypedEntity<?> typed = EntityCatalog.toTyped(payload);
GeneratedNode<?> node = EntityCatalog.toNode(id, payload);
// optional typed helper when useful:
ProductNode product = EntityCatalog.toProductNode(id, (Product) payload);
```

`toEntity(TypedEntity|GeneratedNode, mapper)` delegates to `typed.toEntity(mapper)`.

---

## ← Entity (read back)

```java
GeneratedNode<?> node = EntityCatalog.fromEntity(entity, mapper);
// same as:
GeneratedNode<?> also = EntityCatalog.toNode(entity, mapper);
TypedEntity<?> typed = EntityCatalog.toTyped(entity, mapper);
```

Requires an exact `(type, schemaVersion)` pin present in the catalog (Lane A latest). Older stored
pins are not catalog keys — see optional upgrade path below.

---

## Edge properties

```java
assert EdgeCatalog.supports(CanonicalEdge.class);
EntityTypeMeta edgeMeta = EdgeCatalog.meta(edgeProps);
Map<String, Object> props = EdgeCatalog.toMap(edgeProps, mapper);
```

`EdgeCatalog` does **not** provide `toEntity` / `toNode` / `toTyped`. Relation methods on
`GraphMutationBuilder` accept edge-property DTOs where the allow-list uses `SCHEMA` policy.

---

## Generic mutation builder

```java
GraphMutationBuilder builder = new GraphMutationBuilder(mapper);

// Same registration as addProduct(payload) — UUID assigned automatically
GeneratedNode<?> node = builder.add(payload);
builder.add(id, payload);
builder.add(existingNode);
builder.add(typedEntity);

GraphMutation mutation = builder.build();
```

Per-type `addProduct` / `addDataset` remain. Edge-property classes are **not** `add(Object)`
targets.

---

## Identity lookup sketch

Catalog supplies meta + map; identity algorithm stays on objs runtime:

```java
Map<String, Object> map = EntityCatalog.toMap(payload, mapper);
EntityTypeMeta meta = EntityCatalog.meta(payload);

// contentSchema from ObjectSchemaCatalog for meta.type @ meta.schemaVersion
Map<String, Object> identity = IdentityProjection.INSTANCE.project(contentSchema, map);

List<Entity> hits = graphStore.findEntitiesByIdentity(meta.getType(), identity);
```

Catalog does **not** replace `IdentityProjection` or store APIs.

---

## Validate / create sketch

```java
GraphMutationBuilder builder = new GraphMutationBuilder(mapper);
builder.add(payload);
// … relation methods …
GraphMutation mutation = builder.build();

namedGraphs.validateMutate(graphId, mutation); // or mutate(...)
```

Or hand-build an `Entity` via `EntityCatalog.toEntity(...)` and pass it into a `graphMutation { }`
block ([programmatic-recipes.md](programmatic-recipes.md) / [persist-sketch.md](persist-sketch.md)).

---

## Optional: older pin → upgrade → catalog

Lane A catalogs only index the **latest** generated Java payload class. An older stored
`(type, schemaVersion)` will not resolve in `fromEntity` until upgraded.

```text
stored Entity (old pin)
  → PayloadUpgrade.hydrate(..., UPGRADE_TO_LATEST, …)   // C-35
  → latest payload class / map
  → EntityCatalog.toNode / toEntity
```

Exact-pin `fromEntity` / raw `Entity` paths remain valid when the pin is still the catalog’s
latest. Details: [schema-evolution.md](schema-evolution.md).

---

## Ownership reminder

| Layer | Owns |
|-------|------|
| `:objs-codegen-java` | Emits `EntityCatalog` / `EdgeCatalog` into the **app** source set |
| App package | Catalog classes, payload POJOs, typed services |
| `:objs-api` / persistence | `Entity`, `IdentityProjection`, `GraphStore`, validation |

Do not put app ontologies into root `objs-*` modules.
