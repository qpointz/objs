# WI-001 — Design lock: DSL, seeds, persistence

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design lock  
**Status:** complete  
**Depends on:** WI-000

## Goal

Write the C-16 contract into living design so implementers do not keep the STRING format allow-list or omit edge/schema metadata.

## Deliverables

- [x] [`docs/design/graph/object-schema-dsl.md`](../../../design/graph/object-schema-dsl.md) — free-text `format`; field `tags` / `attributes`; envelope tags/attributes
- [x] [`docs/design/graph/seeds.md`](../../../design/graph/seeds.md) — ObjectSchema + AllowedEdgeRule YAML fields (`description`, `sourceVerb`, `targetVerb`, `tags`, `attributes`)
- [x] [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md) — `bom_entity_schema` / `bom_edge_schema` columns; `definition_doc` still = `contentSchema`
- [x] Confirm [`GAPS.md`](GAPS.md) matches design (no new open questions)

## Out of scope

- Product code / SQL (WI-002)

## Acceptance

- An embedder reading only design + RULES would author free-text `format` and the new seed fields without putting objs SQL on Boot Flyway
