# WI-003 — Full-catalog JSON Schema export

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — JSON Schema  
**Status:** done  
**Depends on:** WI-001

## Goal

Export the full ontology as one JSON Schema 2020-12 document suitable for object-model codegen.

## Scope

- `FullCatalogJsonSchemaExporter` in `objs-core`
- Latest ENTITY schema per type (lexicographic max version)
- `$defs` per type; relation props on source from allow-list edges
- Property name = camelCase(role + PascalCase(targetType))
- Cardinality: `1:1` → singular `$ref`; `1:*` / `UNSPECIFIED` → array; skip `*` endpoints
- `GET /api/v1/objs/registry/export?format=json-schema`
- Unit tests on a small catalog (Product CONTAINS Component, OWNED_BY Organization)

## Out of scope

- JSON Schema import
- UI download button

## Acceptance

- [ ] Export includes all ENTITY types at latest version
- [ ] Relation properties match naming + cardinality rules
- [ ] Wildcard rules omitted
