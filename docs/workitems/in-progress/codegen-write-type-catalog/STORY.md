# Story: Codegen write-side type catalog

**Slug:** `codegen-write-type-catalog`  
**Branch:** `codegen-write-type-catalog`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/codegen-write-type-catalog/`](.)  
**Backlog:** [C-43](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Depends on:** **C-23** ([`objs-api-codegen`](../../completed/20260828-objs-api-codegen/STORY.md)) shipped; **C-35** ([`codegen-schema-evolution`](../../completed/20260923-codegen-schema-evolution/STORY.md)) for Lane A / latest pin rules.  
**Independent of:** C-20 store text search; policy family.  
**Design:** [`docs/design/graph/codegen-and-builder.md`](../../../design/graph/codegen-and-builder.md), [`docs/design/graph/api-and-codegen.md`](../../../design/graph/api-and-codegen.md); recipes (WI-005): [`typed-conversion-recipes.md`](../../../design/graph/typed-conversion-recipes.md) (created in-story)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Consumers:** [`EXAMPLES.md`](EXAMPLES.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Close the write-side gap between **jsonschema2pojo payload beans** (`Dataset`, `Database`, …) and **objs typed bindings** (`DatasetNode` / `TypedEntity` / `Entity`).

Today Pass 1 emits plain POJOs with **no** common base and **no** `(type, schemaVersion)`. Pass 2 emits per-type `*Type` / `*Node` / `GraphMutationBuilder.add*` methods. Read side already has a generated `(type, schemaVersion) → hydrate` catalog in `GeneratedReadView`. There is **no** inverse: `Class` / payload instance → `EntityTypeMeta` / `toEntity` / `toNode`.

Without that catalog, a Spring object service that does **identity lookup**, **create**, and **validate** must either:

- accept only `TypedEntity<?>` / `*Node` (callers wrap every POJO), or  
- grow **per-type** methods (`findDataset`, `findDatabase`, …).

This story ships an **application-owned generated write catalog** from `:objs-codegen-java` so consumers can write one generic path:

```text
payload POJO → meta / TypedEntity / Entity
  → IdentityProjection + GraphStore.findEntitiesByIdentity
  → NamedGraphStore.validateMutate / mutate
```

## Normative (locked in planning — WI-001 confirms)

| Topic | Lock |
|-------|------|
| Layer | `:objs-codegen-java` emits catalog into consuming app source set (G-22 / G-24) |
| Name | **`EntityCatalog`** + **`EdgeCatalog`** in the **same** generated package as `*Type` / `*Node` (G-W1 / G-W5). Optional later **`GraphCatalog`** facade wrapping both — not required in v1 (G-X5) |
| Scope | `EntityCatalog` = generated **ENTITY** (Lane A / latest). `EdgeCatalog` = generated **EDGE_PROPERTIES** |
| Exclude | Do not mix kinds across catalogs; no `toEntity` / `toNode` / `toTyped` on `EdgeCatalog` |
| Lookup | Exact `Class` match (G-W3); no interface/superclass walk |
| API | **Full conversion surface** (G-W2): lookup (`meta` / `contains`), `toMap` / `fromMap`, `toEntity`, `toTyped`, `toNode`, `fromEntity` → node/typed; optional per-type overloads. Edge: lookup + `toMap` / `fromMap` only. Detail in [`GAPS.md`](GAPS.md) § G-W2 |
| Consumers | **Prove** codegen jsonschema + draft-07; **smoke** SBOM + AR (G-W7). Recipes: `docs/design/graph/typed-conversion-recipes.md` (G-W7a, WI-005) |
| Unknown class | **Fail closed** with **`IllegalArgumentException`** (G-W8). `ObjsException` only for objs-specific runtime errors elsewhere — not catalog lookup |
| Multi-version | **Lane A only; latest wins** (G-W4). **Optional** couple with C-35 upgrade-to-latest for older pins — see [`schema-evolution.md`](../../../design/graph/schema-evolution.md); recipes show both exact-pin and upgrade paths |
| Builder | **In** WI-003: both `GraphMutationBuilder.add(Object)` and `add(GeneratedNode<?>)` / `add(TypedEntity<?>)` (G-W6) |
| Payload base class | **Out** (G-X1); catalog alone is enough |
| Store / Spring | **Not** this story — catalog is codegen only; examples may show a typed object-service sketch in docs/tests |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Moved to `in-progress/`; backlog in-progress |
| 1 — Design lock | WI-001 | done | GAPS confirmed; design contract pointers |
| 2 — Generator | WI-002 | done | Emit `EntityCatalog` + `EdgeCatalog` |
| 3 — Builder | WI-003 | done | Generic `add(Object)` **and** `add(GeneratedNode\|TypedEntity)` (G-W6) |
| 4 — Consumers | WI-004 | done | Codegen **prove** + SBOM/AR **smoke** (G-W7) |
| 5 — Docs | WI-005 | done | Living design + **`typed-conversion-recipes.md`** (G-W7a) |

## Work Items

- [x] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock: catalog API, versioning, exclusions — examples: **docs** (`WI-001-design-lock.md`)
- [x] WI-002 — Generate write-side type catalog — examples: **—** (`WI-002-generate-catalog.md`)
- [x] WI-003 — Generic mutation builder `add` — examples: **—** (`WI-003-mutation-builder-add.md`)
- [x] WI-004 — Consumer proof (codegen prove + SBOM/AR smoke) — examples: **codegen + SBOM + AR** (`WI-004-consumers.md`)
- [x] WI-005 — Living docs + conversion recipes — examples: **docs** (`WI-005-living-docs.md`)

## Out of scope

- Implementation until the user starts this story **and** WI-001 is done
- Changing jsonschema2pojo to emit a shared payload superclass (optional follow-up only)
- Foundation Spring `ObjectService` in `objs-*` (stays application-owned)
- Persist gate / identity projection algorithm changes (already shipped C-14 / C-17)
- Schema upgrade / Lane B hydrate (C-35) except “latest META only” rule
- Workbench REST `/api/v1/objs/**` as app data API

## Acceptance (after implementation)

- [x] Generated **`EntityCatalog`** / **`EdgeCatalog`** with full G-W2 surface
- [x] Codegen examples **prove** conversions + identity path
- [x] SBOM + AR each have a **smoke** catalog conversion test after regenerate
- [x] [`typed-conversion-recipes.md`](../../../design/graph/typed-conversion-recipes.md) (or locked name) documents convert recipes
- [x] Unknown payload class fails closed with **`IllegalArgumentException`**
- [x] Open GAPS design rows remain **none** (confirmed in WI-001)
- [x] `./gradlew :objs-codegen-java:test` plus codegen example, `:sbom-service:test`, `:asset-repository-service:test` (scoped as needed)

## Process notes

1. One WI at a time; `[x]` + one commit + push per WI.  
2. Do not start WI-002 until WI-001 checkboxes are done.  
3. Do not close this story until the user asks.
