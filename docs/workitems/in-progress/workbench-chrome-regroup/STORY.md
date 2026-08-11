# Story: Workbench chrome regroup

**Slug:** `workbench-chrome-regroup`  
**Branch:** `workbench-chrome-regroup` (track `origin/workbench-chrome-regroup`)  
**Status:** in-progress  
**Backlog:** [U-4](../../BACKLOG.md)  
**Base:** `origin/dev` (post C-13)  
**Design:** [`ui.md`](../../../design/ui.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Regroup Explorer / Composer / Query chrome so structure matches product intent. **Explorer** leads: two explore modes and a united **Explore-scope** fragment (Open graph ∪ Matcher). **Explorer is read-only** — it never mutates the store; all writes (including creating a graph from a selection) happen in **Composer**. Composer / Query get consistent L2 action placement. Easy to revert: all work on this branch.

**Not in this story:** look-and-feel polish; per-node multi-graph provenance on the canvas.

## Explorer: read-only (normative)

Explorer may **open**, **match**, and **display** only. It must **not** call mutate/create/delete graph APIs. Handoffs navigate to Composer with state; Composer performs Save / New graph persist.

## Explorer: two modes (normative)

| Mode | Canvas | Primary exits (handoff only) |
|------|--------|------------------------------|
| **Graph** | One opened `bom_graph` | **Open in Composer** → edit that graph; **Open in Query** → pass current selection as traverse input |
| **Non-graph** | Set of entities/edges (matcher result; may span graphs) | No Composer edit-of-open-graph; **New graph from selection** → Composer with **entire** canvas set (`entities` + `edges`) as draft; **Open in Query** → pass current selection (matcher + canvas) as traverse input |

```text
Open graph…     →  Mode Graph     (clear Selection canvas / matcher result)
Matcher Exec    →  Mode Selection (clear opened graph state)
Modes are optical either/or — never both; switching resets the previous mode’s view.
```

## Explore-scope fragment (normative UI)

**Matcher** and **Open graph** are one optical/logical block — not a separate graph bar plus a distant/collapsible matcher.

```text
┌─ Explore scope (always visible) ─────────────────────────────────┐
│  Mode: Graph | Selection                                          │
│  [ Open graph… ]  ·  or  ·  [ Matcher … ] [Exec]                  │
│  ── when Mode = Graph ─────────────────────────────────────────── │
│  Id: <uuid>  [copy]   [ann pills…] or “No annotations”            │
│  Annotations: pills next to id (truncate + expand); not a KV dump │
│  ── when Mode = Selection ─────────────────────────────────────── │
│  Summary: N objects / M edges + matcher one-liner                 │
└──────────────────────────────────────────────────────────────────┘
```

Rules:

1. **United fragment** — Open graph + matcher share one panel/row family.
2. **Never hide what is explored** — Mode + summary stay on-screen while the canvas is shown. Editors may compact; the “what am I looking at?” line must not disappear.
3. **Graph mode header detail** — When a graph is opened, show **id** with easy **copy**, and annotations as **pills** next to / around the graph-id chrome (same pill language as Visual node annotations). Empty → explicit **No annotations**. Long values: **truncate with expand**. Shared component reused on Explorer / Composer / Query. **Composer:** with nothing selected, side pane can **edit graph annotations**.
4. **Open in… is mode-specific** — On L2 workspace actions, not inside the core scope fragment:
   - Graph → **Open in Composer** enabled (handoff with **graphId**; Composer loads from API — no Explorer canvas snapshot required)
   - Non-graph → Composer edit-of-graph disabled; **New graph from selection** = handoff **entire canvas** into Composer draft with **`graphId = null`** (always **replace** draft); graph created on **first Save** (membership of same entity ids + edge upserts — not entity clones). Composer **New graph** is separate (clear draft + clear id).
   - **Both modes:** **Open in Query** only when canvas is **non-empty** — pass **entire canvas** (and active matcher context) as Query traverse input
5. User must always be aware of what is currently explored (either/or mode chrome).

## Open graph search (normative UI — all views)

Replace list-all [`OpenGraphModal`](../../../../objs-service/ui/src/OpenGraphModal.tsx) with an **interactive search** dialog used by Explorer / Composer / Query.

**Default UX:** one debounced search field → server returns **≤15** hits. Do **not** load all graphs (target scale ~10K headers).

| User intent | Behavior |
|-------------|----------|
| By id | UUID / UUID-prefix shaped `q` → match on `bom_graph.id` |
| Free text | Otherwise match `q` against id string + annotation keys/values (case-insensitive) |
| Structured | Optional “Expression” expander: `graph-expr`; same result list |

**API:** `GET /api/v1/objs/graphs/search?q=&limit=15` (+ optional `expr=`). Empty `q` without `expr` → empty list. Response `{ items: [{ id, annotations }] }`. **Extensible:** additive params/fields later; no FTS required now (G-U10 / G-U11).

## Open graph (all views)

**One shared Open-graph dialog** (Explorer / Composer / Query). No per-view forks.

| Concern | This story | Later (not U-4) |
|---------|------------|-----------------|
| Shared modal | Yes — same component everywhere | — |
| Incremental search, ≤15 hits | Yes — server-side; no full list | — |
| Match v1 | Id / UUID prefix + simple free-text on id + annotation key/value strings; optional `graph-expr` | — |
| API | Dedicated search endpoint; **stable, open contract** | Same path/params can grow |
| Full-text search (FTS) | **Out of scope** | Future story plugs FTS into the same API |

Empty query: do **not** dump all graphs (empty or “type to search”).

## Chrome levels

| Level | Role | Contents |
|-------|------|----------|
| **L0** | App / view nav | [`AppLayout.tsx`](../../../../objs-service/ui/src/AppLayout.tsx) — unchanged |
| **L1** | View title + explore scope | Short title + help-icon popover (inline copy only, no docs links) \| **Explore-scope fragment** (Explorer); Composer/Query keep graph context strip as edit/query target |
| **L2** | Workspace actions | Mode exits + layout / Save / Exec as appropriate |
| **L3** | Content | Canvas / draft / script / results |

### L2 per view

| View | L2 |
|------|----|
| Explorer | **Open in…** (Composer mode-gated; **Query** always when there is a selection) / **New graph from selection** (non-graph); **Apply layout ▾** |
| Composer | Title **Composer** (not Object linter). Visual L2: **New** ▾ (**New** / **New linked**) + **Link** + **Add objects…**; both tabs: **Validate**, **Save**, **Snapshot** (separate; not Save ▾). No Browse schemas. Selected object/edge schema links (edit form / details) open in **new browser tab**. Reset/Clear secondary |
| Query | Beside Query/Matcher/Options tabs: **Exec** (+ stats) |

## Agent rules

1. One WI at a time; mark `[x]` before next; one commit + push per WI.
2. Prefer UI in `objs-service/ui` + `docs/design/ui.md`. Explorer is read-only; Composer owns all persist (including new graph from Explorer selection handoff).
3. Do not story-close unless user asks.
4. Explorer mode model is locked; do not reintroduce “current graph badge only” while canvas is a matcher set; do not add Explorer write APIs.

## Stages

| Stage | WIs | Gate |
|-------|-----|------|
| 0 Scaffold | WI-000 | docs + branch |
| 0b Prerequisite | **WI-009** | scalar schema `usage` (not JSON array) |
| 1 Design | WI-001 | `ui.md` + story locks |
| 2 Explore-scope | WI-002 | shared fragment component |
| 3 Explorer modes | WI-003 | mode state + gated Open in / New from selection |
| 3b Open graph | WI-007 | shared search modal + open search API (no FTS) |
| 4 Composer / Query L2 | WI-004, WI-005, WI-008 | action strips + edit form |
| 5 Verify | WI-006 | tests + manual |

### Stage 5 — Manual

```text
[ ] Explorer: Open graph + Matcher in one always-visible explore-scope fragment
[ ] Mode Graph vs Selection clearly shown; summary never hidden
[ ] Explorer never mutates store (no Save/create/delete from Explorer)
[ ] Graph mode: id with copy; annotation pills by id (No annotations / truncate+expand); shared on Composer/Query
[ ] Composer: empty selection → edit graph annotations in side pane
[ ] Graph mode: Open in Composer handoff with that graph id
[ ] Both modes: Open in Query only if canvas non-empty; passes entire canvas as query input
[ ] Non-graph: New graph from selection → Composer draft replace, no graphId; first Save creates graph (same entity ids + edge upserts)
[ ] Shared Open graph dialog on Explorer / Composer / Query (same component)
[ ] Open graph: incremental search ≤15; no full catalog list; no FTS in this story
[ ] Composer/Explorer: selected object/edge schema links (edit form / details) open in new browser tab
[ ] Composer Visual L2: New ▾ (New / New linked), Link, Add objects…; Validate / Save / Snapshot (gated)
[ ] Save only when pending or new graph; Snapshot only when saved + clean (Clone semantics, own dialog)
[ ] Composer edit form: no duplicate Payload/Annotations headers; field delete (absent key + deleted mark); schema change + simple migrate
[ ] Query: Exec on tab strip
[ ] L0 unchanged; Schema catalog untouched (type links from Composer/Explorer open new tab)
```

## Work Items

- [x] WI-000 — Story scaffolding (`WI-000-story-scaffold.md`)
- [x] WI-009 — Schema `usage` scalar prerequisite (`WI-009-schema-usage-scalar.md`)
- [x] WI-001 — Design: modes + explore-scope in `ui.md` (`WI-001-design-chrome.md`)
- [x] WI-002 — ExploreScopeBar (Open graph ∪ Matcher ∪ summary) (`WI-002-explore-scope.md`)
- [x] WI-003 — Explorer modes + Open in / New from selection (`WI-003-explorer-modes.md`)
- [x] WI-007 — Shared Open-graph search (API + modal; no FTS) (`WI-007-open-graph-search.md`)
- [x] WI-004 — Composer L2 + persist chrome (`WI-004-composer-l2.md`)
- [x] WI-008 — Composer edit form: delete fields, schema migrate (`WI-008-composer-edit-form.md`)
- [x] WI-005 — Query L2 moves (`WI-005-query-l2.md`)
- [x] WI-006 — Tests + docs pass (`WI-006-verify.md`) — automated green; Stage 5 manual still for user

## Out of scope

- Look-and-feel / design-system polish pass
- Per-node graph provenance labels on canvas
- Schema view chrome
- Any Explorer write path (explicitly forbidden)
- **Full-text search** for graphs (future story; keep search API open for it)
