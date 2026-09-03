# Story sequence (foundation)

1. **C-17** [`live-store-apis`](completed/20260819-live-store-apis/STORY.md) — **done.** Before versions: catalog helpers, reverse membership, identity query, live `copyGraph` + `mergeGraph`, paging. No snapshots, no entity/edge clocks, **no text `q`**.
2. **C-18** [`versions-and-snapshots`](completed/20260819-versions-and-snapshots/STORY.md) — **done.** HEAD+history, clocks, `createDeepGraphVersion` (Snapshot freeze, same graph). `clone()` kept as new-id deep copy. Not a snapshot graph.
3. **C-19** [`foundation-after-versions`](completed/20260822-foundation-after-versions/STORY.md) — **done:** reverse lookup of deep-version pins, remaining matcher ops (`>`, prefix). contains/`q` = C-20.
4. **C-20** [`store-text-search`](planned/store-text-search/STORY.md) — FB-3 contains/`q`. **Design first** (open GAPS). Implementation after C-17 paging. **Does not block C-18.**
5. **C-24** [`objs-policy`](planned/objs-policy/STORY.md) — Foundation policy + **suite** artefacts (hierarchy, M:N membership, folder roll-up) + enrich → **applicability** → evaluate (Drools first). **Design first** (open GAPS). Independent of C-20. Requires shipped `GraphFragment` / resolve path.
6. **C-25** [`objs-core-spring-split`](completed/20260903-objs-core-spring-split/STORY.md) — **done.** Spring-free `:objs-persistence` + `:objs-autoconfigure`; expand `:objs-api`. Independent of C-20 / C-24.

C-17, C-18, and C-25 are **done**. Do not start C-20 implementation until C-20 WI-001 closes open GAPS (C-17 paging is shipped). Do not start C-24 implementation (WI-002+) until C-24 WI-001 closes open GAPS.
