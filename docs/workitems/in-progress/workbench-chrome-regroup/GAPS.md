# Gaps — workbench-chrome-regroup

Summary table first; **Open — questions to lock** below has the decision checklist. Answer in-place (or reply in chat); agent flips status to **resolved** when locked.

| ID | Topic | Status | Notes |
|----|-------|--------|-------|
| G-U1 | L0/L1/L2 chrome levels | **resolved** (story lock) | See STORY § Chrome levels |
| G-U2 | Explorer Graph vs non-graph modes | **resolved** (story lock) | STORY § Explorer: two modes |
| G-U3 | Explore-scope fragment | **resolved** (story lock) | Open graph ∪ Matcher; always-visible summary |
| G-U3a | Graph header readout | **resolved** (story lock) | Pills by graph id; shared component; No annotations; truncate+expand; Composer edit graph annotations when nothing selected — see lock below |
| G-U4 | Open in… mode gating | **resolved** (story lock) | Either/or modes; clear on switch; Open in Query needs non-empty canvas; entire canvas handoffs — see lock below |
| G-U5 | New graph from selection | **resolved** (story lock) | No graphId until first Save; preserve ids; create=membership+edge upserts; handoff always replace — see lock below |
| G-U5a | Explorer read-only | **resolved** (story lock) | No mutate/create/delete from Explorer; Composer owns writes |
| G-U5b | Open in Query handoff | **resolved** (story lock) | Pass current selection (matcher or graph) as Query traverse input |
| G-U6 | Help under titles | **resolved** (story lock) | Title + help icon popover (inline copy only); no docs links — see lock below |
| G-U7 | Query same scope fragment | **deferred** | Optional; Explorer first |
| G-U8 | Per-node multi-graph provenance | **deferred** | Out of story |
| G-U9 | Shared Open-graph dialog | **resolved** (story lock) | Same modal all views; WI-007 |
| G-U10 | Graph search API (open) | **resolved** (story lock) | Extensible search contract; v1 simple match; FTS later without breaking clients — see lock below |
| G-U11 | Full-text graph search | **deferred** | Future story; reuse search endpoint |
| G-U12 | Composer chrome regroup | **resolved** (story lock) | New▾/Link/Add objects Visual L2; Validate L2; Reset/Clear L1; see lock below |
| G-U13 | Schema links new tab | **resolved** (story lock) | Selected object/edge schema in edit form & details → new browser tab — see lock below |
| G-U14 | Save vs Snapshot | **resolved** (story lock) | Enablement gates + Snapshot UX; reconciled with G-U5 first-Save create — see lock below |
| G-U16 | Schema usage scalar | **resolved** (story lock) | Prerequisite WI-009 — `usages[]` → single `usage` ENTITY\|EDGE_PROPERTIES |

## Deferred (no questions this story)

- Look-and-feel polish
- Query adopting full Explore-scope fragment (G-U7)
- Full-text search for graph open (G-U11)
- Per-node multi-graph provenance (G-U8)
- **Composer multi-select** (actions enabled only if valid for every selected item) — nice-to-have from G-U4; not required this story

---

## Resolved locks (answered)

### G-U3a — Graph header readout

**Status:** resolved

**Answers:**

1. **Placement:** Next to / close to the graph id control (existing chrome around “Graph:” / selected graph id — [`CurrentGraphBar`](../../../../objs-service/ui/src/CurrentGraphBar.tsx)). Render annotations as **pills** (same visual language as node annotation pills on the Visual canvas).
2. **Reuse:** Shared readout component used on Explorer Explore-scope (Graph mode) and Composer/Query current-graph chrome — maximize reuse.
3. **Empty:** Explicit **No annotations**. **Composer:** when **no** object/edge is selected, the side edit pane may edit **graph-level annotations** (interesting feature — in scope for this story via Composer empty-selection pane).
4. **Long values:** Truncate with expand (pill / tooltip or inline expand control).

**Implement in:** WI-002 (readout + pills), WI-004 (Composer empty-selection → graph annotations editor; wire shared header).

---

### G-U4 — Open in… mode gating

**Status:** resolved

**Answers:**

1. **Modes are optical either/or:** **Opened graph** **or** **Matcher selection** — never both. User switches mode by **Open graph…** or Matcher **Exec**; that action **resets the current view** and **clears the previous mode’s state** (no sticky graph id while in Selection; no leftover matcher canvas while in Graph).
2. **Open in Query:** Only when the canvas has objects/edges — empty canvas does not enable it (operation means “I see these objects/edges → traverse”).
3. **Handoff payload:** **Entire canvas** (not multi-select subset). Users narrow via matcher or by removing unneeded elements. *(Nice-to-have, deferred: Composer multi-select with actions allowed only if valid for each selected item.)*
4. **Graph → Composer:** Open with **graphId**; Composer **loads members from the API** — no need to pass/return Explorer canvas snapshot for that handoff.
5. **Labels:** Keep **Open in Composer** / **Open in Query** / **New graph from selection** (unchanged).

**Also captured from this reply (G-U5):** no special “back to Explorer” after Save (L0 only); orphan edges with missing endpoints — rely on **backend ignore** (no special Explorer handoff error).

**Implement in:** WI-003.

---

### G-U5 — New graph from selection (Composer handoff)

**Status:** resolved

**Answers:**

1. **No graph id on handoff.** Do **not** auto-create a graph. Composer opens with **`currentGraphId = null`** and the full canvas in the draft. The graph is **created on first Save**. **New graph** (Composer chrome) is a **different** operation: clears draft, resets graph id, prepares an empty edit session — it is **not** what handoff does.
2. **Preserve object IDs** (strict). Keep today’s draft model: Visual shows the set; mutation Text / Save sends only **new / modified / deleted** (current implementation — keep it).  
   **First Save with no graph id** (handoff / “bunch of objects” case) is special: entities are **not** cloned into new pool rows — the new graph gets **membership** (`graph_entities` / create `entityIds`) for those **same** ids, plus **edge upserts** (copies of the original edges into the new graph). May need a small Save/create orchestration (and possibly extend create/mutate payload if create-with-members+edges is cleaner than create-then-mutate). Unchanged entity payloads stay out of upsert (membership only); edited entities still upsert per existing draft logic.
3. **Handoff always replaces** the Composer draft (no merge, no confirm required). Exception: **Add objects…** inside Composer still **adds** into the current draft.

**Previously locked:** L0-only return; orphan edges → backend ignore.

**Implement in:** WI-003 (handoff replace, no graphId); WI-004 (Save creates graph when `currentGraphId == null`: membership + edge upserts; **New graph** = clear draft + clear id).

---

### G-U6 — Help under titles

**Status:** resolved

**What it was:** Long dimmed L1 subtitles under Explorer / Composer / Query titles. STORY’s “(+ ?)” was shorthand for an optional help control — not an existing UI.

**Lock:**

1. **Title only** on L1 (no always-visible subtitle blurb).
2. **Small help icon** beside the title (any clear help glyph — not required to be the character “?”) opens a **popover** with the **current subtitle text** (static copy shipped in the UI package).
3. Apply to **Explorer, Composer, Query**.
4. **No links to docs** (or any external/design-doc URLs). Workbench package must be **self-sufficient** — not deployed as a docs-backed prod site.

**Implement in:** WI-001 (note in `ui.md`) / WI-002–004 as each view’s L1 is touched.

---

### G-U10 — Graph search API

**Status:** resolved

**Intent:** Ship a **stable, easy-to-extend** client↔backend contract now. **How** search is implemented later (FTS, ranking, indexes) is **intentionally unclear** and must **not** force a client break. Backend can swap internals behind the same path.

**Contract (v1 — this story):**

| Piece | Lock |
|-------|------|
| Path | `GET /api/v1/objs/graphs/search` |
| Query | `q` (string, optional), `limit` (int, default 15, max 15 for UI), `expr` (optional graph-expr string) |
| Empty | No `q` and no `expr` → **empty list** (never full catalog) |
| v1 match | Backend-local: id / UUID-prefix + case-insensitive substring on id + annotation key/value strings; if both `q` and `expr`, **AND** |
| Order | Stable (e.g. by id); not a ranking API yet |
| Response | `{ "items": [ { "id": "<uuid>", "annotations": { } } ] }` — **same header shape as list items today** |

**Extension rules (do not break clients):**

1. **Additive query params only** later (e.g. `mode=fts`, `cursor`, `fields=…`). Unknown params: ignore or 400 documented — prefer **ignore** for forward-compat experiments; UI v1 sends only `q`/`limit`/`expr`.
2. **Additive response fields only** on items and envelope (e.g. `score`, `highlights`, `nextCursor`). UI **ignores unknown JSON fields**.
3. **Do not** rename `items` / `id` / `annotations` or change their types without a new path or version.
4. **Do not** require FTS, tsvector, or ranking in this story — v1 may be SQL `ILIKE` / in-memory filter over headers. Future story replaces implementation **behind the same endpoint**.
5. Optional later: POST variant for large `expr` bodies — only if GET length becomes a problem; keep GET as the primary UI contract.

**Out of scope clarity:** No decision yet on FTS engine, ranking, or auth filters — deferred (G-U11). Contract above is enough to implement WI-007.

**Implement in:** WI-007.

---

### G-U12 — Composer chrome regroup

**Status:** resolved

**Lock (accepted proposed default):**

1. **New ▾:** Primary click = **New** (today’s Add object); menu = **New** / **New linked**; ▾ opens menu only.
2. **New / Link enablement:** Same selection rules as today; **disabled with tooltip** when not applicable (not hidden).
3. **Add objects…:** On **Visual** L2 only — **hidden** on Text tab.
4. **Reset / Clear:** Stay on **L1** as secondary actions.
5. **Validate:** **L2 only** (both tabs) — remove duplicates elsewhere.
6. **Context menus:** Rename to **New linked** / **Link** in the same WI (WI-004).

Also already locked elsewhere: title **Composer**; no Browse schemas; **Save** / **Snapshot** separate; schema links new tab.

**Implement in:** WI-004.

---

### G-U13 — Schema links new tab

**Status:** resolved

**Lock:** When the user opens a link to the **selected object’s (or edge’s) schema** from the **edit form** or **object/edge details** pane (Explorer or Composer), open in a **new browser tab** (`target="_blank"`, `rel="noopener noreferrer"`).

Applies to type/schema deep links in those selection UIs (entity type badge → schema detail, edge property schema link, etc.). **L0 Schema** nav stays same-tab. No docs links; no special popup-blocker fallback required.

**Implement in:** WI-003 (Explorer), WI-004 (Composer).

---

### G-U14 — Save vs Snapshot (enablement)

**Status:** resolved

**Accepted proposed default**, with one **G-U5 reconciliation**: Save with **no** `graphId` is **not** disabled — first Save **creates** the graph (membership + edge upserts).

**Lock:**

1. Track **`neverSavedSinceCreate`** (or equivalent) when useful; **`currentGraphId == null`** after handoff or **New graph** = unsaved session.
2. **Save enabled** when: draft **dirty**, or **`currentGraphId == null`** (first Save creates — G-U5; empty New graph → create empty header), or has id and **`neverSavedSinceCreate`**.
3. **Save disabled** when: has id, clean, already saved.
4. **Snapshot** enabled only when saved + clean; label **Snapshot**; dialog = clone dialog; on success **switch** to new id + load.
5. Empty / unsaved session: Save enabled, Snapshot disabled.

**Implement in:** WI-004.

---

### G-U15 — Edit form / field delete / schema migrate

**Status:** resolved *(subject to future review)*

**Accepted proposed defaults:**

1. **Confirm** on **zero** copies **and** on **partial** migration.
2. **Schema ▾ catalog:** other **versions of the same type** only (e.g. `A@1.0.0` → `A@2.0.0`); not cross-type.
3. **Same type@version** re-pick: **no-op**.
4. **Field delete:** entity **payload only** (not edge property forms in this story).
5. **Required** field may be deleted in draft — **Validate** catches later.
6. Clearing text to `""` = present empty; only explicit delete omits the key.
7. **Nested:** delete whole object field **and** individual nested keys.
8. **Arrays:** existing per-item delete; array field itself can be deleted as a key.
9. After migrate: **drop** unmatched leftover keys.

**Implement in:** WI-008. Revisit UX/rules later if needed without blocking this story.

---

### G-U16 — Schema usage scalar (prerequisite)

**Status:** resolved (story lock)

A schema applies to **one** item kind only (entity **or** edge properties). Replace JSON-array column/API field **`usages`** with scalar **`usage`** (`ENTITY` | `EDGE_PROPERTIES`). Edit V1 migration in place (no prod). See [`WI-009-schema-usage-scalar.md`](WI-009-schema-usage-scalar.md).

**Implement first** before chrome WIs that filter schemas by usage (Composer Schema ▾, etc.).

---

## Open — questions to lock

*(none — all previously open gaps resolved.)*
