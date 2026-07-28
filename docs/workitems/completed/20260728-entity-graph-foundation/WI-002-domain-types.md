# WI-002 — Core domain types: Entity, EntityType, Relation, Annotation

**Story:** [`STORY.md`](STORY.md)  
**Status:** pending  
**Depends on:** WI-001

## Goal

Introduce the in-memory **entity SDK** types in `objs-core` per
[`docs/design/graph/model.md`](../../../design/graph/model.md).

## Scope

- **Entity** (`BoMEntity`) — optional id (**UUID**; absent → create, present → update on persist), **type + version**, JSON payload, annotations; independent create/mutate in memory
- **Central schema catalog** — in-memory map of `(type, version)` → JSON Schema (entities and edges)
- **Relation / edge** (`BoMEdge`) — optional id (same create/update-by-id rule); **source**, **target** (UUID refs), **role**; optional **type + version** + JSON properties when allow-list policy requires them; bare edges supported
- **Annotation** — caller-defined opaque metadata on entities as a **key-value map** (value types TBD; see design)
- Unit tests for construction and basic invariants (no persistence, no validation enforcement on construct)

## Out of scope

- Subgraph query API (WI-003)
- Persist-time validation (WI-004)
- JPA mappings (WI-005)
- REST

## Acceptance

- [ ] Domain types live under `org.poc.objs.core…` as **`BoMEntity`**, **`BoMEdge`** (and related); ids optional until persist (**UUID**)
- [ ] SDK can build arbitrary in-memory graphs without validation
- [ ] Design docs use domain **entity** / **edge**; Java uses `BoM*` names (Bill of Materials)
- [ ] Tests cover `BoMEntity` + `BoMEdge` construction
