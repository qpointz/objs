# Gaps — codegen-aggregate-materializer (C-44)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**WI-001 must close every `open` row** (resolve or defer) before implementation. Drafts below are **not** locks.

**Prior:** C-23 [G-19](../../completed/20260828-objs-api-codegen/GAPS.md) deferred aggregate materializer.

---

## Open (design)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-M1 | API shape | **open** | Standalone `GeneratedAggregateMaterializer` vs `GraphMutationBuilder.addAggregate(root)` vs both? Return `GraphMutation` only or also `MaterializeResult` (roots, diagnostics)? |
| G-M2 | Which nests count | **open** | Only fields that correspond to `x-objs-relations` outbound methods? Ignore unknown collection fields? |
| G-M3 | Cycles / DAG sharing | **open** | Same POJO instance twice → one entity? Equal payloads different instances → two entities (G-14)? Reject cycles vs allow DAG? |
| G-M4 | Strip nested props | **open** | How to ensure `toEntity` / `PayloadMapper.toMap` does not persist relation collections on the parent (filter by relation metadata vs Jackson views)? |
| G-M5 | Edge properties | **open** | If nest is only a child entity, edge props empty/`NONE`. If POJO encodes edge props separately — supported in v1? |
| G-M6 | Inbound-only / inverse | **open** | Walk outbound only? Materialize from child upward out? |
| G-M7 | Depth / caps | **open** | Unlimited recursion vs max depth diagnostic? |
| G-M8 | Tie-in to C-43 | **open** | Require write catalog for `meta(child.getClass())`, or only generated per-type walk? Soft prefer C-43 first in SEQUENCE |
| G-M9 | Consumers | **open** | `:examples/codegen/jsonschema` (+ draft-07) required; SBOM/AR optional |

---

## Provisional (likely keep)

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-M10 | Explicit only | **resolved** | No silent materialize inside `toMap` / persist; callers opt in (C-23 design boundary) |
| G-M11 | UUID identity | **resolved** | Same as builder (C-23 G-14); no payload-equality dedupe |
| G-M12 | Persist gate | **resolved** | Materializer builds mutation only; `validateMutate` / `mutate` stay store-side |
| G-M13 | Ownership | **resolved** | Generated (+ optional tiny runtime helper in `objs-api` if schema-agnostic); output stays app-owned |

---

## Out of story (other codegen follow-ups)

Tracked on BACKLOG when promoted; not implemented here.

| # | Topic | Status | Backlog / source |
|---|--------|--------|------------------|
| G-X1 | Generated HTTP client | **deferred** | [C-47](../../BACKLOG.md); C-23 G-20 |
| G-X2 | Kotlin codegen module | **deferred** | [C-46](../../BACKLOG.md); C-23 G-6 |
| G-X3 | Edge-property migrations + persist rewrite | **deferred** | [C-45](../../BACKLOG.md); C-35 G-E5 / G-E6 / G-X3 |
| G-X4 | Persist-time cardinality enforcement | **deferred** | C-23 G-21 (no C-xx yet) |
| G-X5 | Full relation policy consumer matrix | **deferred** | C-23 G-29 |
| G-X6 | L1 compaction / L3 decomposition | **deferred** | C-35 G-X8 |
| G-X7 | jsonschema2pojo shared payload base | **deferred** | C-43 G-X1 |
| G-X8 | Inferred migrations from schema diff | **deferred** | C-35 G-X4 |
