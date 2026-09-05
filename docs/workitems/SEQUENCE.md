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
| 5 | **C-27** | [`policy-suites`](planned/policy-suites/STORY.md) | planned | **C-32** | **C-28** |
| 6 | **C-28** | [`policy-seeds-persistence`](planned/policy-seeds-persistence/STORY.md) | planned | **C-27** | **C-29** |
| 7 | **C-29** | [`policy-batch`](planned/policy-batch/STORY.md) | planned | **C-28** | **C-30** |
| 8 | **C-30** | [`policy-consumer`](planned/policy-consumer/STORY.md) | planned (gated) | **C-29** | — (end of family) |

```text
C-24 flat evaluate
  → C-26 Drools
    → C-31 workbench tactical Policy play UI
      → C-32 policy metadata (tags / categories / annotations — list navigation)
        → C-27 suites + roll-up (run configuration + result interpretation)
          → C-28 seeds + JPA
            → C-29 thin batch pack
              → C-30 optional example/REST consumer
```

**Hard dependency:** every later story needs **C-24** shipped (or api+core usable).  
**C-31** needs **C-26** for Drools play (CUSTOM-only UI is insufficient for the story goal).  
**C-32** is **pre-suite** catalog metadata for Policy list navigation — not a suite alternative.  
**Content deps:** suite seed kinds need C-27; batch suite target needs C-27.  
**Do not** start a story’s WI-002+ until that story’s own WI-001 closes its GAPS. Do not pull later-story gaps into an earlier design lock.

**Next to work now:** C-27 [`policy-suites`](planned/policy-suites/STORY.md) (after C-32 metadata).
