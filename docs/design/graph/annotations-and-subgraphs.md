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

Execution model: **candidate source → filters → induced edges**.

| Type | Role |
|------|------|
| `BoMMatcher` | Common contract over lightweight entity/edge candidates (`matches`) |
| `BoMSourceCapableMatcher` | Optional: supplies a `BoMCandidateSource` for the backend (SQL / scan) |
| `BoMCandidateSource` | Materializes the initial entity candidate set |
| `BoMChainedMatcher` | Ordered composite; stage 0 may source, later stages filter only |
| `MatchAllAnnotationMatcher` | DSL `anno` — match-all map; source-capable on PostgreSQL |
| `BoMAnnoExprMatcher` | DSL `anno-expr` — JEXL; source-capable when lowerable to equality + `&&`/`||` |
| `BoMAnnotationMatcher` | Legacy fun-interface; adapted as filter-only |

- **Foundation default strategy: match-all** (`MatchAllAnnotationMatcher` / DSL key `anno`) — given a filter key-value map, an entity matches iff **every** filter entry is **present** on the entity’s annotations (same key and equal value). Extra annotations on the entity are allowed.
- On PostgreSQL, match-all’s `toCandidateSource` uses JSONB `@>` containment so selection runs in SQL; `matches` stays the in-memory semantic for filters and non-Postgres backends.
- **`anno-expr`** evaluates Boolean JEXL expressions with each annotation key bound as a top-level variable (for example `version == '1.0.0' && app == 'aapp-lala'`). Shared default-deny engine; no payload/edge access. On PostgreSQL, expressions that lower to identifier `==` / `===` string literals combined with `&&` / `||` become DNF containment maps (OR = union of `@>` disjuncts) and use the same JSONB containment candidate source as match-all; when conversion is impossible (e.g. `!=`, numeric compares) or the backend rejects the source, selection switches to **local eval** (all-entities scan + JEXL `matches`).
- Ordered DSL arrays decode to `BoMChainedMatcher`. Only the first child may provide a candidate source; later children always filter in memory. If stage 0 is not source-capable (or returns null), the reader falls back to an all-entities source then applies filters. Induced edges resolve after the final entity stage.
- Older / custom `BoMAnnotationMatcher` lambdas are adapted as filter-only scans.

## Matcher DSL

JSON and YAML are equivalent encodings. Root forms:

```yaml
anno:
  app: payments-api
```

```yaml
anno-expr: "app == 'payments-api'"
```

```yaml
- anno:
    app: payments-api
- anno-expr: "appVersion == '2.3.1'"
```

HTTP query uses `POST /api/v1/objs/graph/query` only.

## Subgraph selection (working rule)

1. Match **entities** via a matcher (default: match-all on a filter map) → entity subset.
2. **Add edges** whose **source** and **target** are **both** in that subset (**induced** subgraph), unless the matcher overrides edge acceptance.

Edges are **additive** to entity selection:

**Subgraph** = selected entities + induced edges (source ∈ set ∧ target ∈ set).

## Retrieval

- Primary query capability: **select subgraph by matcher DSL** (`POST /api/v1/objs/graph/query`).
- Result: matched **entities** plus **additive edges**.
- Retrieval returns **whatever exists**, including non-conforming entities/edges — non-conformance does not block read. See [validation.md](validation.md).
- Reads use fetch-sized JDBC and keep JSON columns raw until a matcher or the final response accesses them. A source-capable first stage (e.g. DSL `anno` or lowerable `anno-expr` on PostgreSQL) avoids full-table hydration.

## Open

- Free-form vs registered annotation keys
- Confirm JSON storage and indexing — see [persistence.md](persistence.md) (annotations GIN done; payload index still open)
- Revisit edge annotations if requirements demand it
- Value type of map entries (string-only vs richer JSON values) — assume string values unless decided otherwise
- Payload/content expression matchers
- Pushdown of expression predicates beyond equality/`&&`/`||` containment DNF (inequality, comparisons, functions)
