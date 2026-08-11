# WI-003 — Explorer modes + Open in / New from selection

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Explorer modes  
**Status:** done  
**Depends on:** WI-002  
**Modules:** `:objs-service` UI

## Goal

Track Explorer **mode** as optical **either/or**:

| Event | Mode | Clears |
|-------|------|--------|
| Open graph… succeeds | **Graph** | Selection canvas / matcher result state |
| Matcher Exec | **Selection** (non-graph) | Opened graph id / graph-mode header state |

Switching mode **resets** the previous mode’s view — no sticky graph under Selection, no leftover matcher canvas under Graph.

L2 actions (handoffs only — **Explorer never mutates**):

- **Graph:** **Open in Composer** → navigate with **graphId**; Composer loads members from API
- **Selection:** Open in Composer for edit-of-open-graph **disabled**; **New graph from selection** → Composer with **`graphId = null`**, draft = **entire** canvas, **always replace** (not merge). Preserve entity/edge ids; keep existing draft dirty-tracking. First **Save** (WI-004) creates the graph: membership for those entity ids + edge upserts.
- **Both modes:** **Open in Query** only if canvas **non-empty** → pass **entire canvas** (+ matcher context) as Query input
- **Apply layout ▾** on L2 (view-only)
- Selection type → schema: **new tab**

Labels stay: **Open in Composer** / **Open in Query** / **New graph from selection**.

## Acceptance

- [x] Mode either/or; Open graph / Exec clear the other mode’s state
- [x] Graph mode: header id copyable; annotation pills (No annotations / truncate+expand)
- [x] Open in Composer gated to Graph mode (graphId handoff)
- [x] Open in Query disabled on empty canvas; uses entire canvas in both modes
- [x] New graph from selection: replace Composer draft, `graphId = null`, full canvas, ids preserved (no Explorer create)
- [x] No mutate/create/delete calls from Explorer code paths
- [x] Schema type links from selection open in a new tab
- [x] Title row free of Open in… / Layout
