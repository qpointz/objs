# Story: Extensible matcher query language

**Slug:** `matcher-query-language`  
**Branch:** `matcher-query-language`  
**Status:** in-progress  
**Backlog:** C-5  
**Design:** [`docs/design/graph/annotations-and-subgraphs.md`](../../../design/graph/annotations-and-subgraphs.md), [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md), [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md)  
**Gaps:** [`GAPS.md`](GAPS.md)

## Goal

Replace the hard-coded annotation-filter request shape with a concise, extensible matcher DSL while
preserving existing annotation match-all behavior. Add an Apache Commons JEXL matcher for
annotation-only expressions and an ordered chained matcher that remains hidden behind the common
`BoMMatcher` contract.

The DSL accepts equivalent JSON and YAML. A single object represents one matcher; an array
represents a chained matcher whose children execute in order. Only the first child is eligible for
persistence pushdown, and induced edges are resolved after the final entity-matching stage.

## Confirmed shape

Single match-all matcher:

```yaml
anno:
  app: lalala
```

Ordered chain:

```yaml
- anno:
    environment: production
- anno-expr: "team != null"
```

Equivalent JSON uses the same object/array structure and matcher keys.

## Stages

| Stage | Work items | Readiness | Exit condition |
|-------|------------|-----------|----------------|
| 0 — DSL and abstraction | WI-000 | Ready; remaining API/security defaults tracked in `GAPS.md` | JSON/YAML codec resolves one object or an array to `BoMMatcher` |
| 1 — Annotation expression | WI-001 | Ready; remaining REST/example defaults tracked in `GAPS.md` | Sandboxed annotation-only JEXL matcher passes unit tests |
| 2 — Chained execution | WI-002 | Depends on WI-000–WI-001 | First-stage-only pushdown and ordered in-memory filtering pass H2/PostgreSQL tests |
| 3 — REST API | WI-003 | Depends on WI-000–WI-002 | Sole `POST /api/v1/objs/graph/query` operation accepts JSON/YAML DSL and passes controller tests |
| 4 — Example and documentation | WI-004 | Depends on WI-003 | SBOM example and graph/service design docs exercise and describe the feature |
| 5 — Graph explorer UI | WI-005 | Depends on WI-003 | Graph explorer matcher modes query subgraphs through `POST /api/v1/objs/graph/query` |

## Work Items

- [x] WI-000 — Matcher DSL and abstraction (`WI-000-matcher-dsl-abstraction.md`)
- [x] WI-001 — JEXL annotation expression matcher (`WI-001-jexl-annotation-matcher.md`)
- [x] WI-002 — Chained matcher store execution (`WI-002-chained-matcher-execution.md`)
- [x] WI-003 — Matcher selection REST API (`WI-003-matcher-rest-api.md`)
- [x] WI-004 — SBOM integration and design documentation (`WI-004-sbom-integration-docs.md`)
- [x] WI-005 — Graph explorer matcher UI (`WI-005-graph-explorer-matcher-ui.md`)

## Scope

- Concise matcher keys `anno` and `anno-expr`
- One matcher object or an ordered array of matcher objects
- JSON and YAML codecs producing the same matcher model
- `BoMChainedMatcher` as another `BoMMatcher` implementation
- Abstract `BoMMatcher` contracts for callers, consumers, controllers, and stores
- Existing annotation match-all matcher represented by `anno`
- Shared, default-deny Apache Commons JEXL expression engine with annotation entries exposed only
  as direct variables
- First-child-only PostgreSQL pushdown; later children evaluate in memory
- Final induced-edge resolution after all entity stages
- Strict DSL validation and bounded/sandboxed expression evaluation
- Remove annotation matching through `GET /api/v1/objs/graph`
- Use `POST /api/v1/objs/graph/query` as the sole graph-query endpoint; simple match-all requests
  use the `anno` DSL form
- Generic REST/OpenAPI support plus concrete SBOM example integration
- Graph explorer UI modes for `anno`, `anno-expr`, and chained JSON, all posting to the query
  endpoint

## Out of scope

- JEXL access to payload/content, entity scalar fields, edges, or neighboring entities
- SQL compilation or pushdown of JEXL
- Pushdown of any chained matcher after the first child
- OR composition, Boolean matcher trees, nested groups, or branching pipelines
- Graph traversal languages such as Cypher or GQL
- Edge annotations or changes to edge-selection semantics
- Authentication and authorization

## Constraints

- Every matcher object contains exactly one registered matcher key.
- Empty matcher arrays, unknown keys, and multi-key matcher objects are rejected.
- Chained children receive only entities retained by the previous child.
- DSL parsing/factories return `BoMMatcher`; no caller-facing pipeline-specific selection API is
  introduced.
- JSON and YAML are encodings of one DSL and must have parity tests.
- Core match-all callers and matcher compatibility overloads continue to work; the HTTP matching
  GET operation is intentionally removed.
