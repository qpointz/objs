# Entity model

**Status:** early design  
**Parent:** [README.md](README.md)

## Entity store

The product is an **entity store**: it persists and manages informational elements without interpreting their business meaning.

- **Entities** are carriers of information.
- Entities are **independent**: created, edited, and managed on their own (not only as part of a larger aggregate).
- Entities of **different types / schemas coexist** in one system.

### Why “Entity” not “Object”

Domain type **Entity** avoids ubiquitous clashes with `java.lang.Object` in method signatures and collections. Java type lives under `org.poc.objs…`; watch imports vs `jakarta.persistence.Entity` on persistence mappings.

## Entity

Every entity has:

| Aspect | Requirement |
|--------|-------------|
| **Type** | An **entity type** that classifies the entity |
| **Payload** | A **JSON object** (JSON document) |
| **Annotations** | Caller-defined metadata used for subgraph selection — see [annotations-and-subgraphs.md](annotations-and-subgraphs.md) |

Identity strategy is **not decided** yet.

## Entity type and JSON Schema

- Each **entity type** has an associated **JSON Schema** for its payload.
- Payload is validated against that schema at **persistence** (not on in-memory construction) — see [validation.md](validation.md).
- How types and schemas are registered, versioned, or referenced from stored rows is **TBD**.

## Relation / edge

- Entities form a **graph** via **relations (edges)**.
- Each relation has a **role** and **properties**.
- The system uses rules for **permitted / allowed edges**: which **entity types** may be linked under which **roles**.
- Persisting an edge is checked against those rules; in-memory construction is unrestricted — see [validation.md](validation.md).
- Whether edge **properties** have their own schema is **not stated** yet.
- Edge table / column design is **TBD** — see [persistence.md](persistence.md).

## Naming note

Domain type for annotations may be `Annotation` under `org.poc.objs…` or `ObjAnnotation` to avoid clash with `java.lang.annotation`.
