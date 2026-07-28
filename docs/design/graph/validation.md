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
| Entity/edge **with id** | **Update** — id must already exist in the store; else **reject** |
| **Delete** | Explicit API; requires id (not inferred from a create/update payload) |

Batch subgraph payloads may **mix** creates and updates.

## What is validated

| Concern | Rule |
|---------|------|
| Entity **payload** | Must conform to JSON Schema for entity **`(type, version)`** from the **central** schema repo |
| **Edge** allow-list | Must match **`(sourceType, role, targetType)`**; else **deny** |
| Edge **properties policy** | From the allow-list rule: **none** (bare edge — reject non-empty properties) or **schema** (validate against `(type, version)`; honor empty allowed/forbidden on the rule) |

Schema lookup (when properties policy = **schema**): **type + version** → schema document (shared repository for entities and edges).

## Allowed-edge rules

- Identity: **`(sourceType, role, targetType)`** (entity types of source/target); each part may be **`*`**
- Plus **properties policy**: `none` | `schema` (+ empty allowed/forbidden when `schema`)
- **Directed**; role is a **free string**; **no cardinality** limits in this story
- Wildcards: e.g. `(* , depends_on , *)` permits that role for any types; most specific match wins
- Catalog: **in-memory**; not in catalog → **deny**
- Later: persist rules as PostgreSQL tables (follow-up with schema catalog / C-3)

## Two modes

### 1. Enforced at persistence (reject) — create, update, delete

- Applies to **create**, **update**, and **delete** of entities and edges (same gate).
- **Batch / subgraph write:** callers may submit **one payload** containing a **set of entities and edges**. Validation is **two-stage**:
  1. **Entities only** — schema-validate each entity in the payload (`type + version`). Stop if invalid.
  2. **Edges, right before persistence** — allow-list / properties checks; resolve source/target types from entities **in the payload** and entities **already in the store**. Example: new entity + edge to an existing store entity. Endpoint in neither → **reject**.
- Single-entity / single-edge writes use the same rules (stage 1 and/or 2 as applicable).
- **Delete**: must still pass the gate — e.g. edge delete allowed only if the `(sourceType, role, targetType)` rule exists (allow-list); entity delete as defined by the same persist-boundary API (reject if gate fails). Exact delete checks beyond “same gate applies” follow allow-list / referential integrity chosen in WI-005.
- Does **not** run on pure in-memory SDK construction.
- Prefer **transactional all-or-nothing** for a batch subgraph write (confirm in WI-005).

### 2. Basic / audit validation (non-blocking)

- Validate an existing (or in-memory) graph against current schemas and allowed-edge rules.
- Report conformance; do not imply historical writes were valid under today’s rules.
- API shape (subgraph vs whole store; report format) is **TBD** (G-18 default-ok).

## Open

- Audit validation API surface (G-18 default-ok)
- Fine-grained delete semantics (e.g. cascade, dangling edges) — under same gate; detail in WI-005
- Batch update/delete of mixed subgraphs beyond create-focused examples — same resolution rules unless narrowed in WI-005
