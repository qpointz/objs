# WI-002 — Chained matcher store execution

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Chained execution  
**Status:** done  
**Depends on:** WI-000, WI-001; G-M4–G-M7 and G-J5 in [`GAPS.md`](GAPS.md)

## Goal

Execute every `BoMMatcher`, including the chained implementation, through the existing abstract
graph-selection entry point. Push down only an eligible first child, pass retained entity candidates
through later children in memory, and resolve induced edges after the final child.

## Scope

- Keep `BoMGraphStore.selectSubgraph(matcher: BoMMatcher)` as the public store contract.
- Add an internal matcher executor/planner that can:
  - execute a single matcher using current pushable/non-pushable behavior;
  - decompose a chained matcher internally without exposing chain-specific store APIs;
  - compile only the first child to PostgreSQL when it is pushable and supported;
  - evaluate every remaining child in memory over the preceding child’s retained candidates.
- Separate entity-stage execution from induced-edge loading in `BoMRawGraphReader`; the current
  reader returns entities and edges together after one matcher.
- Preserve lazy annotation JSON behavior and avoid payload deserialization.
- Load/scan edges once, after the final selected entity-id set is known.
- Apply the single 3-minute selection budget across the complete chain (including first-child
  pushdown, later in-memory children, and final edge load); do not reset the budget per child.
- Keep `BoMSubgraphSelector` behavior equivalent for in-memory `BoMGraph` selection.
- Preserve compatibility overloads for `BoMAnnotationMatcher` and match-all filters.

## Execution cases

| First child | Later children | Expected execution |
|-------------|----------------|--------------------|
| `anno` | none | Existing PostgreSQL pushdown where available |
| `anno-expr` | none | Full entity scan, then JEXL in memory |
| `anno` | `anno-expr` / `anno` | First `anno` pushdown, then ordered in-memory filtering |
| `anno-expr` | `anno` / `anno-expr` | Full entity scan; every child evaluates in memory |

H2 remains non-pushable and must preserve the same final result as PostgreSQL.

## Out of scope

- Pushing down child matchers after index zero
- Reordering matchers for optimization
- Changing chain order based on estimated selectivity
- Edge matching stages or graph traversal
- New caller-facing pipeline/store methods

## Acceptance

- [x] Store/controller consumers pass only `BoMMatcher`, including chained selections
- [x] A pushable first child reduces the candidate set before later in-memory children
- [x] No matcher after the first is pushed down or reordered
- [x] A non-pushable first child causes a scan while retaining ordered result semantics
- [x] Induced edges are resolved exactly once from the final selected entity ids
- [x] Empty intermediate results short-circuit later entity evaluation and edge loading safely
- [x] The chain-wide 3-minute budget cannot be bypassed by adding stages
- [x] H2 and PostgreSQL integration tests prove equivalent results across single and chained cases
- [x] Existing match-all PostgreSQL pushdown and lazy-read performance tests remain green
