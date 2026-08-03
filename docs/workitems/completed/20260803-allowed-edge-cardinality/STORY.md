# Story: Allowed-edge cardinality

**Slug:** `allowed-edge-cardinality`  
**Branch:** `schema-workbench-unify` (delivered with workbench; originally `allowed-edge-cardinality`)  
**Status:** completed  
**Backlog:** C-6  
**Design:** [`docs/design/graph/model.md`](../../../design/graph/model.md), [`docs/design/graph/validation.md`](../../../design/graph/validation.md), [`docs/design/graph/seeds.md`](../../../design/graph/seeds.md), [`docs/design/ui.md`](../../../design/ui.md), [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md)

## Goal

Add **cardinality** to allowed-edge rules (`bom_graph_edge_schema` / `BoMAllowedEdgeRule`) so each
rule declares whether the relation from source → target is **singular** (`1:1`), **many** (`1:*`),
or left **`UNSPECIFIED`**.

Cardinality is **schema metadata** on the allow-list rule. Authors set it in the schema workbench
edge-relation editor; schema tables and the visual relationship graph display it. It is **not**
graph integrity enforcement (no edge-count checks at persist).

## Confirmed decisions

| Decision | Choice |
|----------|--------|
| Values | `UNSPECIFIED`, `1:1`, `1:*` only |
| Meaning | Declares singular vs many for the source→target side of the rule |
| Direction | Outgoing along the rule: from **source** via **role** to **target** type |
| Default | `UNSPECIFIED` (backward compatible) |
| Persist validation | Out of scope — do not count edges or reject on min/max |
| UI | Edit + display on edge definitions **and all schema visuals** that show allowed edges |

### Enum / wire shape

Kotlin enum `BoMEdgeCardinality`:

| Enum constant | Wire / YAML / JSON |
|---------------|--------------------|
| `UNSPECIFIED` | `UNSPECIFIED` |
| `ONE_TO_ONE` | `1:1` |
| `ONE_TO_MANY` | `1:*` |

Optional helpers: `isSingular` / `isMany` (`UNSPECIFIED` → neither).

Rule identity remains `(sourceType, role, targetType)`; cardinality is an attribute of that rule.

### Visual label convention

Schema relationship graph edge labels:

- `1:1` / `1:*` → `ROLE · 1:1` / `ROLE · 1:*`
- `UNSPECIFIED` → role only (`ROLE`)

## Stages

| Stage | Work items | Readiness | Exit condition |
|-------|------------|-----------|----------------|
| 0 — Domain + persistence | WI-000 | Done | Catalog/JPA round-trip cardinality with default `UNSPECIFIED` |
| 1 — Seeds + REST | WI-001 | Done | Seeds and registry APIs accept and emit wire values |
| 2 — Schema UI | WI-002 | Done | Editor, tables, and visual graph show/edit cardinality |
| 3 — Typed meta + docs | WI-003 | Done | Typed meta + design docs + example defaults aligned |

## Work Items

- [x] WI-000 — Domain + persistence (`WI-000-domain-persistence.md`)
- [x] WI-001 — Seeds + REST (`WI-001-seeds-rest.md`)
- [x] WI-002 — Schema UI editor, tables, and visuals (`WI-002-schema-ui.md`)
- [x] WI-003 — Typed meta, example, and graph design docs (`WI-003-typed-meta-docs.md`)

## Scope

- `cardinality` on `BoMAllowedEdgeRule` and `bom_graph_edge_schema`
- Flyway migration; JPA record mapping
- Seed import/export for `AllowedEdgeRule`
- Registry REST: edge PUT/GET and schema-edge replace (`EdgeRelationRequest`)
- Schema UI: relation editor, explorer tables, `SchemaRelationshipGraph` labels
- `TypedEdgeMeta.cardinality`
- Design docs: model, validation, seeds, UI, REST as needed

## Out of scope

- Persist-time edge count checks (`1:1` must not block creating a second edge)
- Extra multiplicities (`1:0..1`, `1:0..*`, incoming cardinality)
- Changing rule identity or most-specific wildcard matching
- Graph explorer **instance** edge labels / inspector (cardinality is on rules, not `BoMEdge`)

## Constraints

- Existing rules and seeds without the field behave as `UNSPECIFIED`
- Wire values are `UNSPECIFIED` / `1:1` / `1:*` (not only Kotlin enum names)
- SBOM ontology may omit cardinality (default) or set illustrative values in WI-003
