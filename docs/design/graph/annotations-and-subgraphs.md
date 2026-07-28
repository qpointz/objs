# Annotations and subgraphs

**Status:** early design  
**Parent:** [README.md](README.md)

## Role of annotations

**Annotations are the means of selecting subgraphs.**

- Vocabulary is **caller-defined and fluid** — the store provides no fixed taxonomy (draft, “describes item X”, capture source, etc. are caller examples only).
- The store treats annotations as **opaque** criteria; it does not interpret business meaning.
- Callers can define graphs **on the fly** by choosing annotation sets.

## What is annotated

- **Entities** carry annotations.
- **Edges are not annotated** — **provisional / half-open** decision; may be revisited. Working assumption only.

## Subgraph selection (working rule)

Under the current assumption:

1. Match **entities** by annotation filter → entity subset.
2. **Add edges** that persist / exist on (among) those selected entities → edge subset.

Edges are **additive** to entity selection:

**Subgraph** = selected entities + edges that exist on those entities.

Exact inclusion (e.g. only edges with **both** endpoints in the selected set) is **TBD**; the usual reading is an induced subgraph on the matched entities.

## Retrieval

- Primary query capability: **select subgraph by annotation filter**.
- Result: matched **entities** plus **additive edges**.
- Retrieval returns **whatever exists**, including non-conforming entities/edges — non-conformance does not block read. See [validation.md](validation.md).

## Open

- Annotation shape (key-value, multi-valued, matching semantics)
- Free-form vs registered annotation keys
- Confirm JSON storage and indexing — see [persistence.md](persistence.md)
- Revisit edge annotations if requirements demand it
