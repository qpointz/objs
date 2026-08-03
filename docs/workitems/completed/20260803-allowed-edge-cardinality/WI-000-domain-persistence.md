# WI-000 — Domain + persistence

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 0 — Domain + persistence  
**Status:** done  
**Depends on:** —

## Goal

Add `cardinality` to the allowed-edge domain model and persist it on `bom_graph_edge_schema`,
defaulting to `UNSPECIFIED`.

## Scope

- Introduce `BoMEdgeCardinality` (`UNSPECIFIED`, `ONE_TO_ONE`, `ONE_TO_MANY`) with wire values
  `UNSPECIFIED` / `1:1` / `1:*`
- Add `cardinality` to `BoMAllowedEdgeRule` (default `UNSPECIFIED`)
- Map on `BoMAllowedEdgeRuleRecord` / JPA catalogs
- Flyway migration adding a non-null column with default `UNSPECIFIED`
- Optional helpers `isSingular` / `isMany`
- Unit / IT coverage for catalog and JPA round-trip

## Out of scope

- Seeds and REST (WI-001)
- UI (WI-002)
- Persist-time edge count validation

## Acceptance

- [x] New and existing rules without an explicit value resolve to `UNSPECIFIED`
- [x] JPA / Flyway round-trip stores and loads all three wire values
- [x] Rule identity remains `(sourceType, role, targetType)`
