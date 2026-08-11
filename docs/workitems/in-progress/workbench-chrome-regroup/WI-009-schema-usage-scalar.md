# WI-009 — Schema usage scalar (prerequisite)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 0b — Prerequisite (before chrome WIs)  
**Status:** done  
**Depends on:** WI-000  
**Modules:** `:objs-core`, `:objs-service`, UI, `:objs-sbom-example`, design docs

## Goal

A catalog schema is used for **exactly one** kind of item: **entity** or **edge** (properties). Replace JSON-array **`usages`** with a **single scalar** **`usage`**.

Today (to change):

- Table `bom_entity_schema.usages` — JSON array ([`V1__bom_schema.kt`](../../../../objs-core/src/main/kotlin/db/migration/V1__bom_schema.kt))
- Domain `BoMSchema.usages: Set<BoMSchemaUsage>`
- API / seed / UI treat usage as a multi-select set

## Target model

| Layer | Change |
|-------|--------|
| DB | `usage VARCHAR(32) NOT NULL` (values `ENTITY` / `EDGE_PROPERTIES`); drop JSON `usages` |
| Domain | `BoMSchema.usage: BoMSchemaUsage` (required scalar) |
| API | Request/response field `usage` (string enum); reject multi-value bodies |
| Seed YAML | `usage: ENTITY` (accept legacy `usages: [X]` only if length 1 during transition, or hard-break — prefer hard-break + update seeds) |
| UI | Single select / radio; remove “both usages” UX |

**Migration note:** Not deployed to prod — **edit V1** in place (and wipe/recreate local DBs) rather than a data-preserving V2, unless a V2 is already required for other reasons.

**Dual-usage rows:** Forbidden. Any seed/test that registered both must become two schemas or one usage.

## Touch (non-exhaustive)

- `V1__bom_schema.kt`, `BoMSchemaCatalogRecord`, `JpaBoMCatalogs`, `BoMSchema` / normalizer
- `ObjectSchemaSeedHandler`, registry controller + tests
- UI `types.ts`, `SchemaExplorerPage`, filters using `.usages.includes(...)`
- `docs/design/graph/object-schema-dsl.md`, `ui.md`, persistence notes
- SBOM / seed fixtures

## Acceptance

- [x] DB + domain + API use scalar `usage`
- [x] No UI/API path accepts multiple usages
- [x] Seeds, SBOM, unit/IT tests green
- [x] Design docs updated
