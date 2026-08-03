# WI-002 — Schema UI editor, tables, and visuals

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Schema UI  
**Status:** done  
**Depends on:** WI-001

## Goal

Authors can set cardinality on allowed entity relations, and every schema UI surface that shows
allowed edges displays it — editor, explorer tables, and the visual relationship graph.

## Scope

- `types.ts`: `BoMAllowedEdgeRule` and `EdgeRelationRequest` include `cardinality`
- `EdgeRelationsEditor`: Cardinality select (`UNSPECIFIED` | `1:1` | `1:*`); default on **Add relation**
- `SchemaLinterPage`: load/save mapping; expert `allowedRelations` round-trip
- `SchemaExplorerPage`: Cardinality column on all allowed-edges tables (outgoing, incoming,
  relations using this property schema)
- `SchemaRelationshipGraph`: edge labels per story convention (`ROLE · 1:1` / `ROLE · 1:*` /
  role-only when `UNSPECIFIED`); update unit tests
- Update [`docs/design/ui.md`](../../../design/ui.md) (relation fields + visual graph labelling)

## Out of scope

- Graph explorer instance-edge canvas / inspector
- Typed meta and SBOM ontology seeds (WI-003)

## Acceptance

- [x] New relations default to `UNSPECIFIED`
- [x] Linter save/reload and expert document preserve cardinality
- [x] Explorer tables show cardinality for every allowed-edge listing
- [x] Visual schema graph labels include cardinality per story convention
- [x] `ui.md` documents the field and visual behaviour
