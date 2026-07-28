# WI-002 — Core domain types: Entity, EntityType, Relation, Annotation

**Story:** [`STORY.md`](STORY.md)  
**Status:** pending  
**Depends on:** WI-001

## Goal

Introduce the in-memory **entity SDK** types in `objs-core` per
[`docs/design/graph/model.md`](../../../design/graph/model.md).

## Scope

- **Entity** — type, JSON payload, annotations; independent create/mutate in memory
- **Entity type** — association with a JSON Schema for the payload (registry shape may be minimal)
- **Relation / edge** — endpoints, role, properties
- **Annotation** — caller-defined opaque metadata on entities (shape: prefer simple key-value unless design says otherwise; document choice in design if decided here)
- Unit tests for construction and basic invariants (no persistence, no validation enforcement on construct)

## Out of scope

- Subgraph query API (WI-003)
- Persist-time validation (WI-004)
- JPA mappings (WI-005)
- REST

## Acceptance

- [ ] Domain types live under `org.poc.objs.core…`
- [ ] SDK can build arbitrary in-memory graphs without validation
- [ ] Design docs use **Entity** (not Object); Java naming avoids `java.lang.Object` clash
- [ ] Tests cover entity + edge construction
