# Validation

**Status:** early design  
**Parent:** [README.md](README.md)

## Principles

1. **SDK may construct any graph** — including non-conforming entities and edges — with **no enforcement** on in-memory build/mutate.
2. **Validation is enforced at the persistence boundary** — immediately before / as part of persist for **create, update, and delete**. Invalid operations are **rejected** (not written). Same gate for all three.
3. **Stored and in-memory graphs may be non-conforming** relative to *current* rules (e.g. after schema/rule change). That is permitted.
4. **Reads** return whatever exists; conformance is not required to retrieve.
5. **Audit / basic validation** can check a graph against current rules without persisting.

Same policy applies for the **entity SDK** save path. REST (later stories) uses the same persistence gate.

## Create vs update vs delete (by id)

| Case | Rule |
|------|------|
| Entity/edge **without id** | **Create** — generate **`UUID.randomUUID()`** at persist |
| Entity/edge **with id** | **Update** if id exists, else **create** with client-supplied id (batch-friendly) |
| **Delete** | Explicit ids on `BoMGraphMutation.delete.entities` / `delete.edges` or deprecated `DELETE /graph`; never inferred from omission |

Batch subgraph payloads may **mix** creates, updates, and deletes in one `BoMGraphMutation`.

## What is validated

| Concern | Rule |
|---------|------|
| Entity **payload** | Must conform to JSON Schema generated from the object-schema DSL for entity **`(type, version)`** |
| **Edge** allow-list | Must match **`(sourceType, role, targetType)`**; else **deny** |
| Edge **properties policy** | From the allow-list rule: **none** (bare edge — reject non-empty properties) or **schema** (validate against the rule's property-schema `(type, version)`; honor empty allowed/forbidden) |

Schema lookup (when properties policy = **schema**): the edge's `type + schemaVersion` must match
the property-schema reference configured on the allowed relation. That reference resolves to the
authoritative DSL definition and deterministic JSON Schema 2020-12 projection. The repository is
shared by entities and edges; see [object-schema-dsl.md](object-schema-dsl.md).

## Allowed-edge rules

- Identity: **`(sourceType, role, targetType)`** (entity types of source/target); each part may be **`*`**
- Plus **properties policy**: `none` | `schema`; a schema rule carries property-schema
  `type + version` and empty allowed/forbidden
- Plus optional **cardinality** metadata: `UNSPECIFIED` | `1:1` | `1:*` (default `UNSPECIFIED`).
  Declares singular vs many for source→target; **not** enforced as edge-count limits at persist
- **Directed**; role is a **free string**
- Wildcards: e.g. `(* , depends_on , *)` permits that role for any types; most specific match wins
- Catalog: PostgreSQL-authoritative with an in-memory lookup cache; not in catalog → **deny**

## Two modes

### 1. Enforced at persistence (reject) — create, update, delete

- Applies to **create**, **update**, and **delete** of entities and edges (same gate).
- **Batch / subgraph write:** callers may submit **one payload** containing a **set of entities and edges**. Validation is **two-stage**:
  1. **Entities only** — schema-validate each entity in the payload (`type + version`). Stop if invalid.
  2. **Edges, right before persistence** — allow-list / properties checks; resolve source/target types from entities **in the payload** and entities **already in the store**. Example: new entity + edge to an existing store entity. Endpoint in neither → **reject**.
- Single-entity / single-edge writes use the same rules (stage 1 and/or 2 as applicable).
- **Delete**: must still pass the gate — e.g. edge delete allowed only if the `(sourceType, role, targetType)` rule exists (allow-list); entity delete as defined by the same persist-boundary API (reject if gate fails). Exact delete checks beyond “same gate applies” follow allow-list / referential integrity chosen in WI-005.
- Does **not** run on pure in-memory SDK construction.
- Prefer **transactional all-or-nothing** for a batch subgraph write / mutation.
- **`BoMGraphMutation`:** validate deletes + upserts against **projected** store state (deleted entity ids are invisible to edge lookup unless also present in the upsert payload); then apply explicit edge deletes, entity deletes (cascade incident edges), then upserts.

### 2. Basic / audit validation (non-blocking)

- Validate an existing (or in-memory) graph against current schemas and allowed-edge rules.
- Report conformance; do not imply historical writes were valid under today’s rules.
- API shape (subgraph vs whole store; report format) is **TBD** (G-18 default-ok).

## Open

- Audit validation API surface (G-18 default-ok)
- Fine-grained delete semantics (e.g. cascade, dangling edges) — under same gate; detail in WI-005
- Batch update/delete of mixed subgraphs beyond create-focused examples — same resolution rules unless narrowed in WI-005
