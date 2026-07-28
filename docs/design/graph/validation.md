# Validation

**Status:** early design  
**Parent:** [README.md](README.md)

## Principles

1. **SDK may construct any graph** — including non-conforming entities and edges — with **no enforcement** on in-memory build/mutate.
2. **Validation is enforced at the persistence boundary** — immediately before / as part of persist. Invalid data is **rejected** (not written).
3. **Stored and in-memory graphs may be non-conforming** relative to *current* rules (e.g. after schema/rule change). That is permitted.
4. **Reads** return whatever exists; conformance is not required to retrieve.
5. **Audit / basic validation** can check a graph against current rules without persisting.

Same policy applies for the **entity SDK** save path. REST (later stories) uses the same persistence gate.

## What is validated

| Concern | Rule |
|---------|------|
| Entity **payload** | Must conform to the **JSON Schema** of the entity’s **type** when persisting |
| **Edge** create (persist) | Must be **allowed** for endpoint **entity types** + **role** |

## Two modes

### 1. Enforced at persistence (reject)

- Persisting an entity: fail if payload does not validate against the type schema.
- Persisting an edge: fail if the type–role combination is not permitted.
- Does **not** run on pure in-memory SDK construction.

### 2. Basic / audit validation (non-blocking)

- Validate an existing (or in-memory) graph against current JSON Schemas and allowed-edge rules.
- Report conformance; do not imply historical writes were valid under today’s rules.
- API shape (subgraph vs whole store; report format) is **TBD**.

## Open

- Enforcement details for **update** and **delete** at persistence
- Whether edge **properties** are schema-validated
- How allowed-edge rules are declared (directionality, cardinality, ownership of the catalog)
- Audit validation API surface
