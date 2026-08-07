# Annotations and subgraphs

**Status:** early design  
**Parent:** [README.md](README.md)

## Role of annotations

**Annotations are the means of selecting subgraphs** (ephemeral matchers) **and of labelling soft-link packs** (persisted headers).

- **Shape:** a **key-value map** (caller-defined keys and values).
- Vocabulary is **caller-defined and fluid** — the store provides no fixed taxonomy (draft, “describes item X”, capture source, etc. are caller examples only). Platform-reserved keys: **none** (app-level vocabulary only).
- The store treats annotations as **opaque** criteria; it does not interpret business meaning.
- Callers can define graphs **on the fly** by choosing annotation maps / filters, or persist soft-link packs with free-form header annotations (see below).

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
| `BoMAnnoExprMatcher` | DSL `anno-expr` — JEXL over annotation keys; source-capable when lowerable to equality + `&&`/`||` |
| `BoMObjExprMatcher` | DSL `obj-expr` — JEXL over `id` / `type` / `schemaVersion` / `a` (annotations) / `p` (payload); shared engine; pushdown when lowerable |
| `BoMIdsMatcher` | DSL `ids` — exact entity id set + induced edges; source-capable |
| `BoMSubgraphIdMatcher` | DSL `subgraph: { id }` — one soft-link pack’s **stored** members (not re-induced) |
| `BoMSubgExprMatcher` | DSL `subg-expr` — JEXL over pack headers (`id`, `a` annotations); union of matching packs’ stored members |
| `BoMAnnotationMatcher` | Legacy fun-interface; adapted as filter-only |

- **Foundation default strategy: match-all** (`MatchAllAnnotationMatcher` / DSL key `anno`) — given a filter key-value map, an entity matches iff **every** filter entry is **present** on the entity’s annotations (same key and equal value). Extra annotations on the entity are allowed.
- On PostgreSQL, match-all’s `toCandidateSource` uses JSONB `@>` containment so selection runs in SQL; `matches` stays the in-memory semantic for filters and non-Postgres backends.
- **`anno-expr`** evaluates Boolean JEXL expressions with each annotation key bound as a top-level variable (for example `version == '1.0.0' && app == 'aapp-lala'`). Shared default-deny engine; no payload/edge access. On PostgreSQL, expressions that lower to identifier `==` / `===` string literals combined with `&&` / `||` become DNF containment maps (OR = union of `@>` disjuncts) and use the same JSONB containment candidate source as match-all; when conversion is impossible (e.g. `!=`, numeric compares) or the backend rejects the source, selection switches to **local eval** (all-entities scan + JEXL `matches`).
- **`obj-expr`** uses the same sandboxed JEXL engine with bindings `id`, `type`, `schemaVersion`, `a` (annotations map), `p` (payload map). Dot/bracket access on maps, e.g. `type == 'Product' && p.name == 'x' && a.app == 'y'`. Candidates expose those fields with **lazy** JSON deserialization (same as annotations). On PostgreSQL, equality/`&&`/`||` over `type`/`id`/`schemaVersion` columns and `a`/`p` JSONB `@>` push down when lowerable; otherwise local JEXL. Error codes are `MATCHER_OBJ_EXPR_*` (distinct from `anno-expr`). **`anno-expr` semantics are unchanged.**
- **`ids`** selects the given entity UUIDs and returns those entities plus induced edges among them. Invalid UUID → `400`.
- **`subgraph: { id }`** loads one soft-link pack by id and returns its **stored** member entities and edges (membership tables), not a freshly induced edge set. Unknown id → validation error on query.
- **`subg-expr`** evaluates Boolean JEXL over each pack header with bindings `id` and `a` (header annotations). Matching packs contribute the **union** of their stored members. Local eval over pack headers is acceptable for v1. Chainable as a source stage before object-level filters (`obj-expr`, `anno`, …).
- Ordered DSL arrays decode to `BoMChainedMatcher`. Only the first child may provide a candidate source; later children always filter in memory. If stage 0 is not source-capable (or returns null), the reader falls back to an all-entities source then applies filters. Induced edges resolve after the final entity stage **except** when the source stage already projected stored pack edges (`subgraph` / `subg-expr`).
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
obj-expr: "type == 'Dataset' && p.datasetType == 'table' && a.env == 'prod'"
```

```yaml
ids:
  - 11111111-1111-1111-1111-111111111111
  - 22222222-2222-2222-2222-222222222222
```

```yaml
subgraph:
  id: 33333333-3333-3333-3333-333333333333
```

```yaml
subg-expr: "a.decisionId == 'D-42'"
```

```yaml
- subg-expr: "a.env == 'prod'"
- obj-expr: "type == 'Component'"
```

```yaml
- anno:
    app: payments-api
- obj-expr: "type == 'Component' && p.kind == 'library'"
```

HTTP query uses `POST /api/v1/objs/graph/query` only.

## Soft-link subgraph packs (persisted)

Annotation / `ids` / `obj-expr` selection remains the **ephemeral** path: match live graph state, induce edges, no pack header.

**Soft-link packs** are a separate persisted concept (Flyway `V5__bom_subgraph.sql`):

| Concern | Behaviour |
|---------|-----------|
| Header | `bom_subgraph(id, annotations)` — free-form string map (app vocabulary; platform reserves none) |
| Membership | M2M `bom_subgraph_entities` / `bom_subgraph_edges` store existing entity/edge UUIDs |
| Soft save | Pointers only — members stay live; no historization |
| Hard snapshot | Clone members to **new** UUIDs, then soft-link the clones under a **new** pack; required annotations stamp the new header and merge-overlay cloned entities |
| Resolve | Get-by-id / matchers return **current** member rows (soft) or clone rows (after snapshot) |
| Delete pack | Removes header + membership only; graph objects remain |
| Delete entity/edge | Cascades that id out of all packs |

Programmatic apps should hold a `subgraphId` and use **get-by-id** (`GET /api/v1/objs/graph/subgraphs/{id}`). Explorer / Composer discovery uses **`subg-expr`** (and list). Optional matcher sugar `subgraph: { id }` matches get-by-id for query handoff.

Snapshot is for **governance evidencing**: the application may treat clones as immutable; the objs store does not enforce immutability.

## Subgraph selection (working rule)

1. Match **entities** via a matcher (default: match-all on a filter map) → entity subset.
2. **Add edges** whose **source** and **target** are **both** in that subset (**induced** subgraph), unless the matcher overrides edge acceptance.

Edges are **additive** to entity selection for annotation / expression / `ids` matchers:

**Subgraph** = selected entities + induced edges (source ∈ set ∧ target ∈ set).

Pack matchers (`subgraph` / `subg-expr`) skip re-induction and project **stored** membership edges instead.

## Retrieval

- Primary query capability: **select subgraph by matcher DSL** (`POST /api/v1/objs/graph/query`).
- Soft-link packs: CRUD + snapshot under `/api/v1/objs/graph/subgraphs` (see [rest-api.md](../service/rest-api.md)).
- Result: matched **entities** plus **additive** or **stored** edges as above.
- Retrieval returns **whatever exists**, including non-conforming entities/edges — non-conformance does not block read. See [validation.md](validation.md).
- Reads use fetch-sized JDBC and keep JSON columns raw until a matcher or the final response accesses them. A source-capable first stage (e.g. DSL `anno`, lowerable `anno-expr` / `obj-expr`, or `ids` on PostgreSQL) avoids full-table hydration.

## Open

- Confirm JSON storage and indexing — see [persistence.md](persistence.md) (annotations GIN done; payload index still open)
- Revisit edge annotations if requirements demand it
- Value type of map entries (string-only vs richer JSON values) — assume string values unless decided otherwise
- Pushdown of expression predicates beyond equality/`&&`/`||` containment DNF (inequality, comparisons, functions)
- Induce-on-save helper for packs (deferred)
- Gremlin strategies over pack membership (deferred)