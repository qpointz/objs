# Story sequence (foundation)

1. **C-17** [`live-store-apis`](completed/20260819-live-store-apis/STORY.md) — **done.** Before versions: catalog helpers, reverse membership, identity query, live `copyGraph` + `mergeGraph`, paging. No snapshots, no entity/edge clocks, **no text `q`**.
2. **C-18** [`versions-and-snapshots`](completed/20260819-versions-and-snapshots/STORY.md) — **done.** HEAD+history, clocks, `createDeepGraphVersion` (Snapshot freeze, same graph). `clone()` kept as new-id deep copy. Not a snapshot graph.
3. **C-19** [`foundation-after-versions`](completed/20260822-foundation-after-versions/STORY.md) — **done:** reverse lookup of deep-version pins, remaining matcher ops (`>`, prefix). contains/`q` = C-20.
4. **C-20** [`store-text-search`](planned/store-text-search/STORY.md) — FB-3 contains/`q`. **Design first** (open GAPS). Implementation after C-17 paging. **Does not block C-18.** Independent of the policy family.
5. **C-25** [`objs-core-spring-split`](completed/20260903-objs-core-spring-split/STORY.md) — **done.** Spring-free `:objs-persistence` + `:objs-autoconfigure`; expand `:objs-api`. Independent of C-20 / policy family.

C-17, C-18, and C-25 are **done**. Do not start C-20 implementation until C-20 WI-001 closes open GAPS.

---

## Policy family (C-24…C-32) — normative order

**Canonical order** (each story’s `STORY.md` repeats **Before** / **Next**):

| Step | Id | Story | Status | Before | Next |
|------|----|--------|--------|--------|------|
| 1 | **C-24** | [`policy-evaluate-core`](completed/20260904-policy-evaluate-core/STORY.md) | **done** | `GraphFragment` resolve path (shipped) | **C-26** |
| 2 | **C-26** | [`policy-drools`](completed/20260904-policy-drools/STORY.md) | **done** | **C-24** | **C-31** |
| 3 | **C-31** | [`policy-workbench`](completed/20260904-policy-workbench/STORY.md) | **done** | **C-26** | **C-32** |
| 4 | **C-32** | [`policy-metadata`](completed/20260905-policy-metadata/STORY.md) | **done** | **C-31** | **C-27** |
| 5 | **C-27** | [`policy-suites`](completed/20260905-policy-suites/STORY.md) | **done** | **C-32** | **C-28** |
| 6 | **C-28** | [`policy-seeds-persistence`](completed/20260906-policy-seeds-persistence/STORY.md) | **done** | **C-27** | **C-33** |
| 7 | **C-33** | [`policy-results-persistence`](completed/20260907-policy-results-persistence/STORY.md) | **done** | **C-28** | **C-29** |
| 8 | **C-29** | [`policy-batch`](completed/20260912-policy-batch/STORY.md) | **done** | **C-33** | **C-30** |
| 9 | **C-30** | [`policy-consumer`](completed/20260915-policy-consumer/STORY.md) | **superseded** | **C-29** | — (end of family) |

```text
C-24 flat evaluate
  → C-26 Drools
    → C-31 workbench tactical Policy play UI
      → C-32 policy metadata (tags / categories / annotations — list navigation)
        → C-27 suites + roll-up (run configuration + result interpretation)
          → C-28 seeds + JPA catalog
            → C-33 evaluation result store
              → C-29 thin batch pack (advanced / mass execution)
                → C-30 optional example/REST consumer (**superseded** — C-31 REST + SBOM assessment)
```

**Hard dependency:** every later story needs **C-24** shipped (or api+core usable).  
**C-31** needs **C-26** for Drools play (CUSTOM-only UI is insufficient for the story goal).  
**C-32** is **pre-suite** catalog metadata for Policy list navigation — not a suite alternative.  
**Content deps:** suite seed kinds need C-27; batch suite target needs C-27; result store needs C-27 shape + C-28 catalog boundary.  
**Do not** start a story’s WI-002+ until that story’s own WI-001 closes its GAPS. Do not pull later-story gaps into an earlier design lock.

**Policy family complete** (C-30 closed superseded 2026-09-15). Open foundation work outside this chain: e.g. [C-39](completed/20260917-policy-archive-workbench/STORY.md) archive viewer (done), [C-20](planned/store-text-search/STORY.md), [C-40](in-progress/graph-entity-edge-vocab/STORY.md) entity/edge vocab (in-progress), [C-35](completed/20260923-codegen-schema-evolution/STORY.md) (done).

---

## Policy follow-ups (outside family chain)

| Id | Story | Status | Notes |
|----|--------|--------|-------|
| **C-34** | [`policy-status-severity-vocab`](completed/20260911-policy-status-severity-vocab/STORY.md) | done | Disambiguate status `ERROR`→`EXEC_ERROR` vs finding severity; SuiteStrategy pack; after C-33; **independent of** C-29 / C-30 |
| **C-39** | [`policy-archive-workbench`](completed/20260917-policy-archive-workbench/STORY.md) | done | Archive list/load HTTP + Policy in-page Archives mode (Results / Policies / Input); after C-33; also **U-12** |

---

## Codegen / schema evolution (outside policy chain)

| Id | Story | Status | Notes |
|----|--------|--------|-------|
| **C-35** | [`codegen-schema-evolution`](completed/20260923-codegen-schema-evolution/STORY.md) | done | After **C-23**. L2 typed upgrade-to-latest hydrate; ClassToClass/MapToClass + additive fallback; SBOM Component@1→2 dual view. [`DESIGN.md`](completed/20260923-codegen-schema-evolution/DESIGN.md). Independent of C-20 and policy family. Closes C-23 G-30. |

---

## Graph lifecycle ops (outside policy chain)

| Id | Story | Status | Notes |
|----|--------|--------|-------|
| **C-36** | [`graph-ops-catalog`](completed/20260912-graph-ops-catalog/STORY.md) | done | After **C-18** / **C-22**. clear/purge/destroy/compact, backdated freeze, reset/apply, REST+Composer, recipes, example seeds. Independent of C-20, C-35, policy family. |
| **C-37** | [`transaction-recipes`](completed/20260912-transaction-recipes/STORY.md) | done | Docs: Boot autoconfigure how-to + Spring/non-Spring TX recipes. After **C-25**. |

---

## Store / validation API gaps (outside policy chain)

| Id | Story | Status | Notes |
|----|--------|--------|-------|
| **C-38** | [`api-store-improvements`](completed/20260914-api-store-improvements/STORY.md) | done | `NamedGraphStore.exists(id\|matcher)` + structured `ValidationIssue` (subject/schema) + GraphContextBar Open layout (Note1). Independent of C-20, C-30, C-35, policy family. |

---

## Graph entity/edge vocabulary (outside policy chain)

| Id | Story | Status | Notes |
|----|--------|--------|-------|
| **C-40** | [`graph-entity-edge-vocab`](in-progress/graph-entity-edge-vocab/STORY.md) | in-progress | Rename freeze `*_version_member` → `*_version_entity`; REST `/members`→`/entities`; add `/graphs/{id}/edges` CRUD; `apply-structure`; ER WI-006. Edge SQL names unchanged. Independent of C-20 / policy family. |
| **C-41** | — | backlog | Freeze-scoped edge history (not planned). See C-40 G-X4. |
