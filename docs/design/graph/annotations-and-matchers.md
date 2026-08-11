# Annotations, graphs, and matchers

**Status:** early design  
**Parent:** [README.md](README.md)

There is **no global graph** and **no "subgraph pack" product concept**. The entity **pool** holds all
entities; a **graph** (`bom_graph`) is a durable header (`id` + `annotations` only) plus its member
entities and its own graph-local edges. See [model.md](model.md) for the pool-vs-graph split.

## Role of annotations

**Annotations select graphs** (via `graph-expr` over graph headers) **and select objects within a
graph** (via `obj-expr` over entities).

- **Shape:** a **key-value map** (caller-defined keys and values).
- Vocabulary is **caller-defined and fluid** — the store provides no fixed taxonomy (draft, "describes
  item X", capture source, etc. are caller examples only). Platform-reserved keys: **none**.
- The store treats annotations as **opaque** criteria; it does not interpret business meaning.
- Both **entities** and **graph headers** carry annotations. Callers open/select graphs on the fly by
  matching header annotations, then filter the objects inside.

## What is annotated

- **Entities** carry annotations.
- **Graph headers** (`bom_graph`) carry annotations (their only content besides `id`).
- **Edges are not annotated** — **provisional / half-open** decision; may be revisited.

## Matchers

| DSL | Matches | Returns |
|-----|---------|---------|
| **`all: true`** | Every graph | Union of **stored** member entities + **graph-local** edges across all graphs (**distinct by id**). Orphans (no membership) are excluded. |
| **`graph-expr`** | Graphs: JEXL over header `id` + `a` | **Stored** member entities + **graph-local** edges of the matching graph(s) (union if many; distinct by id) |
| **`obj-expr`** | Objects: JEXL over `id`, `type`, `schemaVersion`, `a.*`, `p.*` | Matching entities; edges among survivors **within the active graph scope** (stored edges with both ends kept — never a whole-pool induce) |
| **chained** (JSON/YAML array) | Ordered stages | Stage 0 may be `all` / `graph-expr` (source); later stages typically `obj-expr` filters |

```yaml
# Every graph (union, distinct by id)
all: true

# Open / select graph(s) by header
graph-expr: "id == '…' || a.env == 'prod'"

# Filter objects (same bindings as today)
obj-expr: "type == 'Component' && a.app == 'payments' && p.kind == 'library'"

# All graphs then filter objects inside those graphs' members
- all: true
- obj-expr: "type == 'Component'"

# Graph then filter objects inside those graphs' members
- graph-expr: "a.decisionId == 'D-42'"
- obj-expr: "type == 'Component'"
```

- `all` is a stage-0 graph-scope matcher (boolean `true` only). It selects every `bom_graph`
  and unions members/edges with distinct entity and edge ids.
- `graph-expr` evaluates Boolean JEXL over each graph header with bindings `id` and `a` (header
  annotations). Matching graphs contribute the **union** of their stored member entities and
  graph-local edges. Local eval over headers is acceptable for v1; pushdown is an open item.
- `obj-expr` uses the sandboxed JEXL engine with bindings `id`, `type`, `schemaVersion`, `a`
  (annotations map), `p` (payload map). Dot/bracket access on maps, e.g.
  `type == 'Product' && p.name == 'x' && a.app == 'y'`. Candidates expose those fields with **lazy**
  JSON deserialization. On PostgreSQL, `==` / `!=` combined with `&&` / `||` over
  `type`/`id`/`schemaVersion` columns and `a`/`p` JSON (`@>` for equals, `->>` for not-equals)
  push down when lowerable; otherwise local JEXL. Same operators for `graph-expr` over `id` / `a.*`.
- Ordered DSL arrays decode to `BoMChainedMatcher`. Only the first child may provide a candidate
  source; later children always filter in memory. Edges resolve after the final entity stage, scoped
  to the graph(s) selected by a stage-0 `all` / `graph-expr` (or the graph fixed by the request path).
- **Scope rule (fail closed):** with no global graph, bare `obj-expr` on `/graphs/query` is **not**
  "scan the whole pool as a graph". A request must fix the graph — either the API path
  (`/graphs/{id}/query`) or a stage-0 `all` / `graph-expr` in a chained array. Bare `obj-expr`
  without either → rejected (`400`), lock G-G16. **Pool search** (orphans included) uses
  `POST /entities/query` with `obj-expr` instead — that is entity selection, not a whole-pool graph.

### Retired matchers (parity with pre-C-13 keys)

Superseded by the four forms above; retired keys reject with `MATCHER_DSL_RETIRED_KEY` (handlers kept short-term for migrate UX).

| Old | Express with |
|-----|----------------|
| `subg-expr` | `graph-expr` (same header bindings `id`, `a`) |
| `subgraph: { id }` | `graph-expr: "id == '<uuid>'"` |
| `anno: {k: v}` | `obj-expr: "a.k == 'v' && …"` |
| `anno-expr: "k == 'v'"` | `obj-expr: "a.k == 'v'"` (annotation keys live under `a`, not top-level) |
| `ids: […]` | `obj-expr: "id == '…' \|\| id == '…'"` (or chained equals) |

HTTP query uses `POST /api/v1/objs/graphs/query` (header-scoped, `graph-expr`/chained),
`POST /api/v1/objs/graphs/{id}/query` (fixed graph, `obj-expr`/chained), or
`POST /api/v1/objs/entities/query` (pool `obj-expr`, orphans included, no edges) — see
[rest-api.md](../service/rest-api.md).

## Graphs (persisted)

A **graph** (`bom_graph`) is the only persisted grouping concept — there is no separate "soft-link
pack" beside it.

| Concern | Behaviour |
|---------|-----------|
| Header | `bom_graph(id, annotations)` — **no** other columns (no `parent_graph_id`, no `kind`) |
| Membership | M2M `bom_graph_entity` — same entity may be a member of **many** graphs; zero graphs = orphan |
| Edges | `bom_graph_edge` with `graph_id` NOT NULL — owned by exactly one graph; both endpoints must be members of that graph |
| Resolve | Get-by-id / `graph-expr` return the graph's **current** member entities and graph-local edges |
| Clone (optional) | Copies members + edges into a **new** independent graph (new entity/edge ids); source graph unchanged; **no** parent/lineage column is written |
| Delete graph | Removes header + membership + edges (CASCADE); entity rows in the pool are **kept** |
| Delete entity (pool) | Cascades that id out of every graph's membership and drops its incident edges |

**Snapshot hierarchy is out of foundation scope.** If an application wants a lineage tree over clones
(e.g. SBOM decision snapshots), it records that itself — in its own tables or in graph-header
annotations (e.g. `a.parentGraphId`). Objs core stores no parent/lineage column on `bom_graph`.

Programmatic apps that hold a graph id use **get-by-id**
(`GET /api/v1/objs/graphs/{id}`). Discovery across graphs uses **`graph-expr`** (or list).

## Object selection within a graph (working rule)

1. Fix the **graph scope** — request path `/graphs/{id}/...` or a stage-0 `graph-expr`.
2. Match **entities** within that scope via `obj-expr` (or take all members if no filter stage).
3. **Add edges** whose **source** and **target** are **both** in the resulting entity set and whose
   `graph_id` is the scoped graph (**induced**, but never beyond the graph's own edges).

`graph-expr` alone skips re-induction and returns the graph's **stored** membership and edges directly
(no whole-pool scan is ever needed, because the graph already stores exactly its own members/edges).

## Retrieval

- Primary query capability: matcher DSL, either graph-scoped (`/graphs/{id}/query`) or across graph
  headers (`/graphs/query` with `graph-expr` / chained) — see [rest-api.md](../service/rest-api.md).
- Result: matched **entities** plus **additive** (obj-expr) or **stored** (graph-expr) edges as above.
- Retrieval returns **whatever exists**, including non-conforming entities/edges — non-conformance
  does not block read. See [validation.md](validation.md).
- Reads use fetch-sized JDBC and keep JSON columns raw until a matcher or the final response accesses
  them. A source-capable first stage (`graph-expr`, or lowerable `obj-expr`) avoids full-table
  hydration.

## Open

- Confirm JSON storage and indexing — see [persistence.md](persistence.md) (entity + graph-header
  annotations GIN done; payload index still open)
- Revisit edge annotations if requirements demand it
- Value type of map entries (string-only vs richer JSON values) — assume string values unless decided
  otherwise
- Pushdown of `graph-expr` / `obj-expr` beyond `==`/`!=` + `&&`/`||` (comparisons, functions,
  richer JSON predicates)
- Gremlin strategies over graph membership (deferred)
