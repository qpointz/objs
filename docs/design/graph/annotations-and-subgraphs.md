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

Hierarchy:

| Type | Role |
|------|------|
| `BoMMatcher` | Common contract over lightweight entity/edge candidates |
| `BoMPushableMatcher` | Exposes structured `BoMMatchExpression` (never raw SQL) |
| `BoMNonPushableMatcher` | Evaluated in memory against raw/lazy candidates |
| `BoMAnnotationMatcher` | Compatibility adapter for legacy lambdas |

- **Foundation default strategy: match-all** (`MatchAllAnnotationMatcher`) — given a filter key-value map, an entity matches iff **every** filter entry is **present** on the entity’s annotations (same key and equal value). Extra annotations on the entity are allowed.
- Match-all is **pushable**: stores may compile its expression to PostgreSQL JSON containment while preserving identical Kotlin evaluation.
- Legacy / custom `BoMAnnotationMatcher` implementations are adapted as **non-pushable** scans.
- Other strategies (e.g. match-any) remain extension points; implement as pushable when the expression model can represent them, otherwise as non-pushable.

## Subgraph selection (working rule)

1. Match **entities** via a matcher (default: match-all on a filter map) → entity subset.
2. **Add edges** whose **source** and **target** are **both** in that subset (**induced** subgraph), unless the matcher overrides edge acceptance.

Edges are **additive** to entity selection:

**Subgraph** = selected entities + induced edges (source ∈ set ∧ target ∈ set).

## Retrieval

- Primary query capability: **select subgraph by annotation matcher / filter**.
- Result: matched **entities** plus **additive edges**.
- Retrieval returns **whatever exists**, including non-conforming entities/edges — non-conformance does not block read. See [validation.md](validation.md).
- Reads use fetch-sized JDBC and keep JSON columns raw until a matcher or the final response accesses them. Pushable matchers that compile successfully avoid full-table hydration on PostgreSQL.

## Open

- Free-form vs registered annotation keys
- Confirm JSON storage and indexing — see [persistence.md](persistence.md)
- Revisit edge annotations if requirements demand it
- Value type of map entries (string-only vs richer JSON values) — assume string values unless decided otherwise
- Additional matcher strategies beyond match-all
- Expression coverage beyond annotation equality/conjunction
