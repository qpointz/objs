# Entity model

**Status:** early design  
**Parent:** [README.md](README.md)

## Entity store

The product is an **entity store**: it persists and manages informational elements without interpreting their business meaning.

- **Entities** are carriers of information.
- Entities are **independent**: created, edited, and managed on their own (not only as part of a larger aggregate).
- Entities of **different types / schemas coexist** in one system.

## Pool vs graphs

There is **no global graph**. Two distinct concepts share the store:

| Concept | Role |
|---------|------|
| **Entity pool** | All entities (`BoMEntity`), regardless of graph membership. Building blocks only — not itself a queryable graph. |
| **Graph** | A durable header (`id` + `annotations`, nothing else) plus its **member** entities and its **own** edges. The graph is what you open, query, and edit. |

- An entity may belong to **zero, one, or many** graphs (M2M membership). Zero graphs = **orphan**; orphans are allowed.
- An edge belongs to **exactly one** graph (`graph_id` NOT NULL) — edges are never shared across graphs. Both the source and target entity must already be members of that graph.
- A graph header carries **no** parent/lineage columns. An optional **clone** operation copies a graph's members and edges into a new, independent graph (new ids); any lineage tracking is an **application-level** concern (e.g. annotations), not part of this foundation model.
- See [persistence.md](persistence.md) for tables and [annotations-and-matchers.md](annotations-and-matchers.md) for how graphs and objects are selected.

### Why “Entity” not “Object”

Domain concept is **entity**; Java type is **`BoMEntity`** (and edges **`BoMEdge`**) — `Bo` prefix avoids clashes with `java.lang.Object` and `jakarta.persistence.Entity`. Domain docs still say entity / edge; code uses the `Bo*` types.

## Entity

Every entity (`BoMEntity`) has:

| Aspect | Requirement |
|--------|-------------|
| **Type + version** | Identifies the authoritative object-schema DSL definition for the payload |
| **Payload** | A **JSON object** (JSON document) |
| **Annotations** | Caller-defined metadata used for graph/object selection — see [annotations-and-matchers.md](annotations-and-matchers.md) |

Identity: plain **`UUID`**. Runtime creates use `UUID.randomUUID()`; configuration seeds use
deterministic **UUIDv5** from stable textual keys (see [seeds.md](seeds.md)).

**Create vs update:** if id is **absent** on persist → **create** (assign `UUID.randomUUID()`); if id is **present** → **update** (must exist). See [validation.md](validation.md) / G-20.

## Central schema repository

- **One catalog** for schemas used by **entities and edges** (not separate silos).
- Schema key: **`type` + `version`**.
- Lookup: given type+version → typed object-schema definition.
- The recursive DSL is documented in [object-schema-dsl.md](object-schema-dsl.md).
- PostgreSQL is authoritative; an in-memory cache serves validation lookups.
- JSON Schema 2020-12 is generated from the DSL for payload/property validation and tooling.
- Multiple versions of a type may coexist; stored entities/edges keep the type+version they were written with (non-conforming vs *current* rules remains allowed on read — see [validation.md](validation.md)).

## Relation / edge

- Entities form a **graph** via **relations (edges)** — Java type **`BoMEdge`**.
- Each edge is **graph-local**: it belongs to exactly one graph (persisted `graph_id` NOT NULL) — there is no shared/global edge pool. Both **source** and **target** must already be members of that same graph.
- Each edge has: **source**, **target**, **role**, and optionally **type + version** + **properties** (JSON).
- Prefer terminology **source** / **target** (directed); avoid “endpoints” in APIs and docs.
- **Bare edges** (no properties) are first-class: some roles are links only, in the graph-theory sense.
- Allowed edges: **in-memory allow-list** keyed by **`(sourceType, role, targetType)`**, each with a **properties policy**:
  - **none** — no properties allowed/expected
  - **schema** — properties validated against central schema `(type, version)`; policy also says if **empty** properties are allowed
- Any component may be **`*`** (wildcard). Example: `(* , depends_on , *)` allows that role between any entity types. Most specific matching rule wins.
- Rules are **directed**; **role** is a **free string**.
- **Cardinality** (optional metadata on each allow-list rule):

  | Wire / YAML / JSON | Meaning |
  |--------------------|---------|
  | `UNSPECIFIED` | Default when omitted; no singular/many declaration |
  | `1:1` | Singular: one target of that type via the role from a given source |
  | `1:*` | Many: multiple targets allowed |

  Cardinality is **schema metadata only** for authors and UI (tables, Visual labels
  `ROLE · 1:1` / `ROLE · 1:*`, role-only when `UNSPECIFIED`). It is **not** enforced as an
  edge-count check at persist.
- Persist/audit: not in allow-list → **deny**; then enforce properties policy (and schema when applicable).
- In-memory construction is unrestricted — see [validation.md](validation.md).
- Edge table / column design is **TBD** — see [persistence.md](persistence.md).
- Allowed-edge + schema catalog persistence in PostgreSQL: see seeds / catalog stories.

## Naming note

| Domain | Java (foundation) |
|--------|-------------------|
| Entity | `BoMEntity` |
| Edge / relation | `BoMEdge` |
| Schema catalog entry | `BoMSchema` — type + version + authoritative `BoMSchemaNode` DSL |
| Annotations map | Prefer plain key-value on `BoMEntity` |
| Matcher | `BoMMatcher` / `BoMSourceCapableMatcher`; DSL forms **`all`** / **`graph-expr`** / **`obj-expr`** / chained — see [annotations-and-matchers.md](annotations-and-matchers.md) |

Annotation type name may still avoid `java.lang.annotation` clash if a dedicated class is introduced.
