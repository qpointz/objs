# Gaps & clarifications — matcher-query-language

Open decisions for [`STORY.md`](STORY.md). Resolve each blocking item before its dependent work
item. Do not invent unresolved behavior silently in code.

**Current state:** story planned. Core DSL, chaining, abstraction, shared minimal JEXL engine,
annotation-only scope, and simple evaluation limits are resolved. Remaining items are
`default-ok` REST/example defaults.

**Legend:** `blocking` = user decision required before implementation · `default-ok` = proposed
default may be accepted or changed · `resolved` = agreed

## Matcher DSL and execution

| # | Topic | Status | Clarification |
|---|-------|--------|---------------|
| G-M1 | Matcher keys | resolved | Use `anno` for existing annotation match-all and `anno-expr` for JEXL annotation expressions |
| G-M2 | Root shape | resolved | A root object represents one matcher; a root array represents an ordered matcher chain |
| G-M3 | Matcher object shape | resolved | Each matcher object has exactly one registered key; reject empty arrays, unknown keys, and multi-key objects |
| G-M4 | Chain semantics | resolved | Execute children in array order; each child receives only entities retained by the previous child |
| G-M5 | Pushdown | resolved | Only the first child may be pushed down; all later children evaluate in memory |
| G-M6 | Abstraction | resolved | Chaining is another `BoMMatcher` implementation; codecs/factories and all callers operate on `BoMMatcher` |
| G-M7 | Edge selection | resolved | Resolve induced edges once, after the final entity-matching child |
| G-M8 | Encodings | resolved | JSON and YAML are equivalent encodings of one matcher DSL and resolve to the same matcher contract |
| G-M9 | Existing GET query | resolved | Remove annotation matching from `GET /api/v1/objs/graph`; clients express the same match-all query with `anno` through the sole query endpoint |

## JEXL annotation expression

| # | Topic | Status | Clarification |
|---|-------|--------|---------------|
| G-J1 | Evaluation surface | resolved | Build a fresh read-only context from `BoMEntityMatchCandidate.annotations`; each annotation key is a top-level expression variable; expose no payload, scalar fields, map object, edges, or neighbors |
| G-J2 | Pushability | resolved | `anno-expr` is a `BoMNonPushableMatcher`; raw JEXL is not added to `BoMMatchExpression` |
| G-J3 | Variable syntax | resolved | Expressions address annotations directly, e.g. `version == '1.0.0' && app == 'aapp-lala'`; v1 `anno-expr` keys must be valid JEXL identifiers, while `anno` continues to support arbitrary string keys |
| G-J4 | Shared minimal engine | resolved | Use one immutable, thread-safe, application-scoped engine with a bounded compile cache and a fresh read-only context per candidate; parse expressions only (not scripts) under a default-deny sandbox; permit annotation-variable reads, literals, parentheses, Boolean logic, null checks, and comparisons only; disable assignments/side effects, methods, properties/indexing, constructors, classes/reflection, namespaces/functions, loops, lambdas, local variables, pragmas, annotations, and object creation |
| G-J5 | Evaluation limits | resolved | Keep limits simple for small in-memory selections: shared bounded compile cache (suggested capacity 256 entries, LRU or equivalent); fresh read-only context per candidate; no cooperative cancellation or per-expression interrupt machinery; apply one hard wall-clock budget of **3 minutes** for the whole selection request and fail the request if exceeded; no separate scan/result row caps in v1; reject obviously oversized expression text with a modest max length (suggested 4 KiB) |
| G-J6 | Failure semantics | default-ok | Syntax/compile errors return request validation failure; per-candidate evaluation errors fail the request rather than silently treating that candidate as non-matching |

## REST and example integration

| # | Topic | Status | Clarification |
|---|-------|--------|---------------|
| G-A1 | Sole graph query endpoint | resolved | Use `POST /api/v1/objs/graph/query` for JSON/YAML matcher DSL bodies and return the existing `BoMSubgraph`; remove matching via `GET /api/v1/objs/graph`; use `/query`, not `/select` |
| G-A2 | Empty/unbounded selection | resolved | Reject empty objects/arrays and any shape that would select without an effective matcher |
| G-A3 | Validation response | default-ok | Reuse `BoMValidationResult` with stable matcher DSL/expression issue codes |
| G-A4 | OpenAPI | default-ok | Document the JSON request schema and equivalent YAML examples; retain the existing graph response schema |
| G-A5 | SBOM integration | default-ok | Exercise the generic matcher API from the SBOM example and docs; do not add a separate SBOM-only matcher endpoint |
| G-A6 | Graph explorer matcher UI | resolved | Add explorer modes for `anno` (Mantine key/value editor), `anno-expr` (single-line text), and chained (JSON textarea); every mode posts to `POST /api/v1/objs/graph/query` |

## Resolution log

| Gap | Decision | Date |
|-----|----------|------|
| G-M1–G-M8 | Concise object-or-array DSL; ordered chain; first-child-only pushdown; abstract matcher contract; JSON/YAML parity | 2026-07-29 |
| G-J1–G-J4 | Shared default-deny JEXL expression engine; annotation entries are direct variables; only Boolean/comparison predicate evaluation; always non-pushable | 2026-07-29 |
| G-J5 | Simple compile cache + read-only contexts; no fancy cancellation; 3-minute hard fail for the whole selection; no scan/result row caps in v1 | 2026-07-29 |
| G-A2 | Empty or ineffective selections remain forbidden | 2026-07-29 |
| G-M9, G-A1 | Remove matching GET; make `POST /api/v1/objs/graph/query` the sole graph-query endpoint | 2026-07-29 |
| G-A6 | Graph explorer: `anno` / `anno-expr` / chained modes; all post to `/api/v1/objs/graph/query` | 2026-07-29 |
