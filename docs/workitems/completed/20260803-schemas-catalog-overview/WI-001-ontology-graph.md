# WI-001 — Ontology graph all types + edges

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Ontology graph  
**Status:** done  
**Depends on:** WI-000

## Goal

Render a registry-wide ontology graph: ENTITY types as nodes, allow-list rules as edges, with
navigation into type detail and a light edge inspector.

## Scope

- Load `listSchemas` + `listEdges` for the overview
- React Flow graph: ENTITY nodes (latest version label optional); `*` node for wildcards
- Edge labels: role · cardinality convention
- Node click → latest version of that type
- Edge click → inspector (role, cardinality, properties policy / schema ref)
- Extract pure builder helpers for vitest

## Out of scope

- Editing rules on the canvas
- Edge-property schemas as nodes

## Acceptance

- [x] Overview shows all ENTITY types and allow-list links
- [x] Clicking a type node opens its latest schema version
- [x] Wildcard endpoints use a single `*` node
- [x] Selected edge shows metadata in an inspector
