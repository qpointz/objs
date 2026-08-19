# Story sequence (foundation)

1. **C-17** [`live-store-apis`](completed/20260819-live-store-apis/STORY.md) — **done.** Before versions: catalog helpers, reverse membership, identity query, live `copyGraph` + `mergeGraph`, paging. No snapshots, no entity/edge clocks, **no text `q`**.
2. **C-18** [`versions-and-snapshots`](planned/versions-and-snapshots/STORY.md) — identity versions, HEAD, snapshot pins.
3. **C-19** [`foundation-after-versions`](planned/foundation-after-versions/STORY.md) — **after versions**: clocks on version rows, reverse lookup of pins, remaining matcher ops (FB-3 **except** contains/`q`).
4. **C-20** [`store-text-search`](planned/store-text-search/STORY.md) — FB-3 contains/`q`. **Design first** (open GAPS). Implementation after C-17 paging. **Does not block C-18.**

C-17 is **done**. C-18 / C-19 / C-20 stay **planned** until you explicitly start them. Do not start C-18 until C-17’s live `copyGraph` / `mergeGraph` exist. Do not start C-19 until C-18 pins exist. Do not start C-20 implementation until C-20 WI-001 closes open GAPS (C-17 paging is shipped).
