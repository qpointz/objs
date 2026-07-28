# Annotations and subgraphs

**Status:** early design  
**Parent:** [README.md](README.md)

## Role of annotations

**Annotations are the means of selecting subgraphs.**

- **Shape:** a **key-value map** (caller-defined keys and values).
- Vocabulary is **caller-defined and fluid** — the store provides no fixed taxonomy (draft, “describes item X”, capture source, etc. are caller examples only).
- The store treats annotations as **opaque** criteria; it does not interpret business meaning.
- Callers can define graphs **on the fly** by choosing annotation maps / filters.

## What is annotated

- **Entities** carry annotations.
- **Edges are not annotated** — **provisional / half-open** decision; may be revisited. Working assumption only.

## Annotation matching

Matching is **strategy-based** so additional strategies can be added later.

- Introduce a **base matcher** (abstract type / interface) for “does this entity match?”
- **Foundation delivers one default strategy: match-all** — given a filter key-value map, an entity matches iff **every** filter entry is **present** on the entity’s annotations (same key and equal value). Extra annotations on the entity are allowed.
- Other strategies (e.g. match-any) are **out of scope** for this story; only the extension point is required.

## Subgraph selection (working rule)

1. Match **entities** via a matcher (default: match-all on a filter map) → entity subset.
2. **Add edges** whose **source** and **target** are **both** in that subset (**induced** subgraph).

Edges are **additive** to entity selection:

**Subgraph** = selected entities + induced edges (source ∈ set ∧ target ∈ set).

## Retrieval

- Primary query capability: **select subgraph by annotation matcher / filter**.
- Result: matched **entities** plus **additive edges**.
- Retrieval returns **whatever exists**, including non-conforming entities/edges — non-conformance does not block read. See [validation.md](validation.md).

## Open

- Free-form vs registered annotation keys
- Confirm JSON storage and indexing — see [persistence.md](persistence.md)
- Revisit edge annotations if requirements demand it
- Value type of map entries (string-only vs richer JSON values) — assume string values unless decided otherwise
- Additional matcher strategies beyond match-all
