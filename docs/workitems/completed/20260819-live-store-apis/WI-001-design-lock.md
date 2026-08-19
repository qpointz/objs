# WI-001 — Design lock: store APIs vs examples

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design lock  
**Status:** complete  
**Depends on:** WI-000  
**Examples:** **docs** (workbench + SBOM + AR mapping; no Java/Kotlin yet)

## Goal

Write **C-17 (pre-version lookups)** into living design. Sequence: C-17 → C-18 versions → C-19 leftover foundation. Sibling: C-20 text search. `copyGraph` = single-source live share; `mergeGraph` = persist-union with `GraphMergePolicy`; snapshot/fingerprint = C-18; clocks = C-19; `q` = C-20.

## Core / examples

No runtime code. Document the APIs and consumers ([`EXAMPLES.md`](EXAMPLES.md), [`WORKBENCH.md`](WORKBENCH.md)).

## Docs (this WI — first write-up of the contract)

- [x] [`docs/design/graph/apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md) — C-17 vs C-18 vs C-19 vs C-20; **FB-1/FB-2 + `copyGraph` + `mergeGraph` + paging live** this story; **`q` is C-20**; **no WI-009 clocks**
- [x] [`docs/design/sbom/example.md`](../../../design/sbom/example.md) — usage, duplicates, keep-split `copyGraph`, combine-on-new-draft `mergeGraph`, Combined GET = `BomUnion`, paged assets vs domain application `LIKE` (asset text `q` = C-20)
- [x] [`docs/design/asset-repository/example.md`](../../../design/asset-repository/example.md) — schema catalog, relations, identity upsert, collection copy, paged list/count (object `q` = C-20; collection-name `LIKE` stays domain)
- [x] Touch graph [`persistence.md`](../../../design/graph/persistence.md) / [`model.md`](../../../design/graph/model.md) only if they still describe `clone` as the example snapshot path
- [x] Confirm [`GAPS.md`](GAPS.md) + [`EXAMPLES.md`](EXAMPLES.md) + [`WORKBENCH.md`](WORKBENCH.md) still match

## Out of scope

- Product code (WI-002+)
- FB-3 contains/`q` (C-20) and remaining operators (C-19)
- Versions and snapshots (C-18 [`versions-and-snapshots`](../../planned/versions-and-snapshots/STORY.md))

## Acceptance

- An embedder reading design + RULES + `EXAMPLES.md` + `WORKBENCH.md` would add the APIs on core **and** know workbench / SBOM / AR classes to rewire
- `copyGraph` is single-source **live** share; `mergeGraph` is persist-union (default first-seen); Combined GET stays `BomUnion`; fingerprint freeze is **C-18**; `clone()` unchanged this story
- Text `q` / contains is **C-20**, not this WI
