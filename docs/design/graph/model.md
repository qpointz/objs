# Entity model

**Status:** early design  
**Parent:** [README.md](README.md)

## Entity store

The product is an **entity store**: it persists and manages informational elements without interpreting their business meaning.

- **Entities** are carriers of information.
- Entities are **independent**: created, edited, and managed on their own (not only as part of a larger aggregate).
- Entities of **different types / schemas coexist** in one system.

### Why “Entity” not “Object”

Domain concept is **entity**; Java type is **`BoEntity`** (and edges **`BoEdge`**) — `Bo` prefix avoids clashes with `java.lang.Object` and `jakarta.persistence.Entity`. Domain docs still say entity / edge; code uses the `Bo*` types.

## Entity

Every entity (`BoEntity`) has:

| Aspect | Requirement |
|--------|-------------|
| **Type + version** | Identifies the **JSON Schema** for the payload in the central schema repository |
| **Payload** | A **JSON object** (JSON document) |
| **Annotations** | Caller-defined metadata used for subgraph selection — see [annotations-and-subgraphs.md](annotations-and-subgraphs.md) |

Identity: **UUID version 7** (`UUID` / PostgreSQL `uuid`), same in-memory and persisted. v7 for better B-tree index locality than random v4.

**Create vs update:** if id is **absent** on persist → **create** (assign UUID v7); if id is **present** → **update** (must exist). See [validation.md](validation.md) / G-20.

## Central schema repository

- **One catalog** for schemas used by **entities and edges** (not separate silos).
- Schema key: **`type` + `version`**.
- Lookup: given type+version → JSON Schema used to validate entity **payload** or edge **properties**.
- **Foundation:** catalog is **in-memory**.
- **Later:** same catalog persisted as **PostgreSQL tables** (backlog C-3).
- Multiple versions of a type may coexist; stored entities/edges keep the type+version they were written with (non-conforming vs *current* rules remains allowed on read — see [validation.md](validation.md)).

## Relation / edge

- Entities form a **graph** via **relations (edges)** — Java type **`BoEdge`**.
- Each edge has: **source**, **target**, **role**, and optionally **type + version** + **properties** (JSON).
- Prefer terminology **source** / **target** (directed); avoid “endpoints” in APIs and docs.
- **Bare edges** (no properties) are first-class: some roles are links only, in the graph-theory sense.
- Allowed edges: **in-memory allow-list** keyed by **`(sourceType, role, targetType)`**, each with a **properties policy**:
  - **none** — no properties allowed/expected
  - **schema** — properties validated against central schema `(type, version)`; policy also says if **empty** properties are allowed
- Rules are **directed**; **role** is a **free string**; **cardinality** unlimited for now.
- Persist/audit: not in allow-list → **deny**; then enforce properties policy (and schema when applicable).
- In-memory construction is unrestricted — see [validation.md](validation.md).
- Edge table / column design is **TBD** — see [persistence.md](persistence.md).
- Allowed-edge + schema catalog persistence in PostgreSQL: later (C-3).

## Naming note

| Domain | Java (foundation) |
|--------|-------------------|
| Entity | `BoEntity` |
| Edge / relation | `BoEdge` |
| Schema catalog entry | TBD (`BoSchema` / similar — type + version + JSON Schema document) |
| Annotations map | Prefer plain key-value on `BoEntity` |
| Annotation matcher | TBD (`BoAnnotationMatcher` base + match-all impl) |

Annotation type name may still avoid `java.lang.annotation` clash if a dedicated class is introduced.
