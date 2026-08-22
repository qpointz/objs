# Gaps — workbench-ux (U-7)

Source of truth for UX intent: this file and [`docs/design/ui.md`](../../../design/ui.md). (`UX-NOTES/` drafts removed intentionally.)
Summary table first; resolve or defer before the WI that needs the answer. Agent flips **resolved** when locked.

| ID           | Topic                              | Status         | Notes                                                                                                                                                                 |
| ------------ | ---------------------------------- | -------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| G-UX1        | Shared context surfaces            | **resolved**   | Explorer + Objects + Query share one graph context. Composer + Schema **unbound**                                                                                     |
| G-UX2        | Graph context meaning (v1)         | **resolved**   | Source of entities/edges — not only a `bom_graph`. v1 = **single opened graph** *or* selection/`obj-expr`. Multi-graph set → **deferred**                             |
| G-UX3        | Context chrome                     | **resolved**   | Replace Pic1 **(4)+(5)** with one slim bar; **same position** on Explorer / Objects / Query; collapsed **≤2–3 lines**                                                 |
| G-UX4        | Nav order                          | **resolved**   | L0: **Explorer · Objects · Query · Composer · Schema** (read-only shared-context group first)                                                                         |
| G-UX5        | Objects layout                     | **superseded** | Superseded by Note 7 (**G-UX-objchrome**, **G-UX-objgrid**, **G-UX-objview**, **G-UX-objshelf**)                                                                      |
| G-UX6        | Query layout                       | **superseded** | Superseded by **G-UX-qchrome** (Options popup; no right pane)                                                                                                         |
| G-UX7        | Explorer canvas                    | **resolved**   | Disable graph canvas when context exceeds **~300 nodes** (initial cap)                                                                                                |
| G-UX8        | Product rules kept                 | **resolved**   | Explorer / Objects / Query stay **read-only**; Composer owns writes; no theme rebrand; no C-20 `q`; no SBOM/AR UX                                                     |
| G-UX-ctx     | Graph-context chrome (v1)          | **resolved**   | Two modes only: **Graph** or **Matcher**. Collapsed bar per Pic4/Pic5; **Open** split = Graph \| Matcher. App colors (not demo). Multi-component expand → later       |
| G-UX-gver    | Graph-context version pin          | **resolved**   | Note 2: version dropdown on bar (Graph mode) between copy-id and annotations; Latest or pinned; ~10 + paging + from/to filter; removes Explorer Versions pane         |
| G-UX-eact    | Explorer action chrome             | **resolved**   | Note 3: drop type-row node/edge count; drop Open in Query; Composer + Apply layout as view actions (placement refined by **G-UX-vchrome**)                            |
| G-UX-vchrome | View chrome order                  | **resolved**   | Note 4 + refine: one row **Title · space · context**; view actions below; no help icon; uniform view-level button size — Query actions row refined by **G-UX-qchrome** |
| G-UX-erow    | Explorer type+actions + pane       | **resolved**   | Note 4: type+actions one row; resizable inspect; pills full opacity; canvas dims non-selected types (edges: either-end)                                               |
| G-UX-hist    | Context history (`localStorage`)   | **deferred**   | New / Save (named) / Recent + last context across sessions — nice-to-have; not v1-blocking                                                                            |
| G-UX-objver  | Node/edge version inspect (Note 1) | **superseded** | Replaced by Note 5 (`G-UX-odetail` / `G-UX-over`)                                                                                                                     |
| G-UX-odetail | Object details viewer              | **resolved**   | Note 5: reusable sectioned Node/Edge/Graph viewer; SBOM dividers; title fallback; no edge Annotations                                                                 |
| G-UX-over    | Object versions UX                 | **resolved**   | Note 5: stats+last 5 on select; **inline** version browser left of viewer (no modal)                                                                                  |
| G-UX-q       | Query chrome                       | **superseded** | Superseded by **G-UX-qchrome**                                                                                                                                        |
| G-UX-qchrome | Query chrome (Note 6)              | **resolved**   | Chrome row: stats left; Open in Composer · Exec · Options cog; Options popover; drop right pane; drop boxed Query title                                               |
| G-UX-qstruct | Query Structured grid (Note 6)     | **resolved**   | Table-alike wins; V/E split tables; edges: id·type·source name·role·target name; shared chrome; virtualize >200 |
| G-UX-qview   | Query object viewer (Note 6)       | **resolved**   | Right pane **inside** Graph/Structured tabs; reuse Note 5 as-is; node+edge; hide if empty; single-select row |
| G-UX-qcreate | Open in Composer from query (Note 6) | **resolved** | `contents`; disabled if no graph; refuse over-cap/huge                                                                                                                |
| G-UX-objgrid | Objects grid (Note 7)              | **resolved**   | Reuse `QueryResultGrid` chrome; Shelf + checkbox columns; page **25**; virtualize **>200**; Id link → viewer |
| G-UX-objchrome | Objects chrome row (Note 7)        | **resolved**   | Stats row above split; Search stays in Matcher; shelf actions right; no duplicate stats in grid |
| G-UX-objview | Objects object viewer (Note 7)   | **resolved**   | Id link opens viewer; close restores tabs; Note 5 as-is; keep `objs.ui.objects.sidePaneWidth` |
| G-UX-objshelf | Objects Shelf/Matcher (Note 7)    | **resolved**   | **Shelf \| Matcher**; bold + `(N)` when non-empty; Clear · New graph on chrome row; disabled when empty |
| G-UX-cgbar   | Composer graph bar (Note 8)        | **resolved**   | Visual-only match to shared bar; **`ComposerGraphBar`**; **never** `GraphContextProvider` |
| G-UX-cnew    | Composer New split (Note 8)        | **resolved**   | **New ▾** before Open: **Blank** (empty draft) \| **Matcher** (modal → new draft + merge) |
| G-UX-copen   | Composer Open (Note 8)             | **resolved**   | Single **Open** after New; `OpenGraphModal`; latest graph; no shared-context side effect |
| G-UX-sbar    | Schema context bar (Note 9)        | **resolved**   | **`SchemaContextBar`**; catalog / detail / new-draft; Paper like Composer bar; **never** shared context |
| G-UX-schrome | Schema view chrome (Note 9)        | **resolved**   | Row 1 title + bar; Row 2 view actions; Row 3 sidebar + main; **`sm`** view actions |
| G-UX-sctx    | Schema catalog chrome lift (Note 9)| **resolved**   | Drop overview header; Import/Export/Apply layout on page actions row |

## Deferred (explicit)

- **Workspace** rename (user-facing “Workspace manages data context”) — **not v1**; stick with **graph context** naming
- Multi-graph composition as graph context (SBOM-style set) — after v1 single graph
- Context history New/Save/Recent (`G-UX-hist`)
- Per-node multi-graph provenance (old U-4 G-U8)
- Composer multi-select; Schema polish beyond L0 reorder
- Store text search (C-20)

## Carry-in resolved by Note 1

- Old G-U7 (Query Explore-scope) → **in scope** as shared graph-context chrome on Query (`G-UX6`)

---

## Resolved locks (from Note 1)

### G-UX1 — Shared context surfaces

Explorer, Objects, and Query share one **graph context**. Changing it in any of those three updates all three. **Composer** and **Schema** do not bind to it.

### G-UX2 — Meaning (v1)

Graph context = source of entities and edges for the view. May be an opened graph or a selection/`obj-expr`. **v1 sticks to a single graph** (or selection); multi-graph sets are later.

### G-UX3 — Chrome

Blocks **(4)** and **(5)** become one slim graph-context bar in the **same position** on Explorer / Objects / Query. Collapsed: **max ~2–3 lines**, must not steal the view. v1 chrome layout locked in **G-UX-ctx** (Pic4/Pic5). Multi-component expand / context-edit modal → **later** (not v1).

### G-UX4 — Nav

`Explorer · Objects · Query · Composer · Schema`.

### G-UX5 / G-UX6 — Objects & Query chrome

**G-UX5 status:** **superseded** by Note 7 (**G-UX-objchrome**, **G-UX-objgrid**, **G-UX-objview**, **G-UX-objshelf**). Was: right pane **Matcher | Shelf**, vertical splitter; Search in Matcher; shelf actions in Shelf. Matcher = **`obj-expr` chained inside context**.

**G-UX6 status:** **superseded** by **G-UX-qchrome** (Options cog popover; Query right pane removed). Matcher removal and “query means = script only” remain.

### G-UX7 — Node cap

If context has too many objects for a usable canvas, **disable graph view** (initial threshold **300 nodes**).

### G-UX8 — Non-goals

No C-20 store `q`; no example-app UX; no wholesale theme redesign; Composer remains the write surface.

### G-UX-objver — Node / edge version inspect

**Status:** superseded by Note 5 (`G-UX-odetail`, `G-UX-over`)

Note 1 shipped a thin version readout + modal. Note 5 redesigns the whole object viewer and versions flow.

### G-UX-odetail — Object details viewer (Note 5)

**Status:** resolved  
**Source:** [`UX-NOTES/Note5/Object details view.md`](UX-NOTES/Note5/Object%20details%20view.md)

Reusable **Object viewer** (props/flags to toggle sections later). Section dividers = SBOM-style `Divider` with left label.

#### Node view

| # | Rule |
|---|------|
| (1) | Drop **NODE** / kind badge |
| (2) | Title = display name; fallback **`typename-truncatedId`** (e.g. `product-ea85d`) |
| Layout | Sections: **Node** → **Payload** → **Annotations** → **Versions** |
| Node | Payload-key label style (no `:`). `type` → `typename@version` **schema link**. `id` full + **copy** |
| Payload | Keep fields; **no** outer boxed Paper |
| Annotations | **Key–value** like Payload (not pills); no outer box |
| Versions | See **G-UX-over** |

#### Edge view

Same as node, plus **Relation** section **before** Payload (source / target links).  
**No Annotations** section on edges.

#### Graph view

Canvas click with opened graph and **nothing** selected → **Graph** inspect (same section pattern; versions = graph version metadata).

#### Composition

One shared viewer; flags for Versions / Graph / Relation / Annotations as needed per context.

### G-UX-over — Object versions UX (Note 5)

**Status:** resolved  
**Source:** Note 5 § Versions + Version inspection

| Rule | Behaviour |
|------|-----------|
| Section row | `version` → object version or **LATEST** (payload-row layout) |
| On select | Lazy fetch **stats**: total count + most recent **N=5** (skeleton while loading). Thin stats API OK |
| Link | `versions N` (total) opens version browser |
| Preview list | N most recent — same chrome as **graph version selector** (version · grey date · ≤3 annotation pills) |
| Click row / link | Open version inspect |
| Inspect chrome | **Inline** left of object viewer (**not** a modal). Left: list/filter like graph versions (search under title; tighter From/To; default **10** recent). Right: Object viewer **without** Versions section; version descriptor sticky, or “select version” placeholder when none chosen |

**Supersedes Note 1:** limited stats+N on select is allowed; full filtered list only in the browser.

---

### G-UX-q — Query chrome

**Status:** **superseded** by **G-UX-qchrome**  
**Was:** Options → right-side tab; Matcher removed; query means = script only (WI-003).

---

## Note 6 — Query view

**Source:** [`UX-NOTES/Note6/Note 6.md`](UX-NOTES/Note6/Note%206.md) + pasted Query screenshot.

All Note 6 gaps below are **resolved** (`G-UX-qchrome`, `G-UX-qstruct`, `G-UX-qview`, `G-UX-qcreate`). Ready for design-lock WI + implementation WIs.

### G-UX-qchrome — Query chrome (Note 6)

**Status:** **resolved** (chrome layout + Options; Structured/viewer/create remain other gaps)

#### Locked — layout

After removing the **outer box** and the **“Query” title inside that box** around the script editor **(1)** (it duplicated the view title — view-title row **Query** from **G-UX-vchrome** stays):

| Order (top → bottom) | Content |
|----------------------|---------|
| View title row | **G-UX-vchrome**: view title **Query** · context bar (no second “Query” label on the editor) |
| **Chrome row** | **Left:** exec stats **(2)**. **Right:** actions (LTR) |
| Query box **(1)** | Script editor only (no titled outer Paper / no inner “Query” heading) |
| Results | Graph / Structured / Raw (+ viewer **(11)** → **G-UX-qview**) |
| Right pane | **Removed** (Options no longer a tab) |

**Exec stats (2):** same string as today shown before the **New graph** button (`formatGremlinStats` — duration · result count · sg1/sg2). Show when a result exists.

**(8) Redundant stats:** remove other copies of **that same** stats string on Query (do not leave a duplicate next to actions or elsewhere). Distinct Structured labels (e.g. `primary · N items`) are **not** this string — leave unless a later Structured lock says otherwise.

**Right side of chrome row (LTR):**

| Callout | Control | Rules |
|---------|---------|--------|
| **(7)** | **Open in Composer** | See **G-UX-qcreate** |
| **(4)** | **Exec** | Today’s Exec (screenshot **(3)** → here) |
| after **(4)** | **Options** cog | Icon immediately after Exec |

**Options:** popover/popup **anchored to the cog**. Same content as today’s Options pane (**timeout only**). Full replacement for the right Options pane — **delete** the Query right pane / splitter.

### G-UX-qstruct — Query Structured grid (Note 6)

**Status:** **resolved**

| Rule | Lock |
|------|------|
| Mode precedence | Traversal is treated as **either** subgraph **or** projections. If both appear, **table-alike wins** (show projection/table grid; do not also show V/E grids for that result). |
| Table-alike columns | Projection / `views.table` (and scalar rows as today) — shared grid chrome |
| Vertices table | Columns: **id** · **type** · **name** (`name` = Object viewer display-name logic / fallback) |
| Edges table | Columns: **id** · **type** · **source name** · **role** · **target name** (no single edge `name` column) |
| Split | Vertices and edges are **separate tables** (not one combined grid) |
| Shared chrome | **Yes** — table-alike and V/E modes share one grid style (headers, row height, borders, density, virtualization); Objects result table is the visual reference |
| Virtualization | No library preference. Virtualize when **> 200** rows (per table) |

**id** links → object viewer **(11)** — details in **G-UX-qview**.

### G-UX-qview — Query object viewer (Note 6)

**Status:** **resolved**

| Rule | Lock |
|------|------|
| Placement | **Inside** the active result tab (**Graph** or **Structured**) as a **right pane** of that tab’s content — not a page-level pane outside the tabs |
| Splitter | Vertical splitter between canvas/grid and viewer; persist width (same idea as Explorer inspect) |
| Component | Reuse Note 5 **G-UX-odetail** / **G-UX-over** **as-is**. Slim only if reuse proves inappropriate later |
| Graph tab | Open on **node** and **edge** selection. **Hide** viewer when selection is empty |
| Structured tab | Click / select **row** opens viewer for that entity or edge. **No multi-select** |
| id link | Selecting the row (including id link) drives the same single selection → viewer |

### G-UX-qcreate — Open in Composer from query (Note 6)

**Status:** **resolved**

| Rule | Behaviour |
|------|-----------|
| Label | **Open in Composer** (replaces Query **New graph**) |
| Enable | **Disabled** when there is no graph result (no vertices/edges). Not hidden. |
| Composer mode | Opens Composer in **New graph** mode |
| Payload source | Prefer **`result.contents`**. Fall back only if needed for handoff shape (`views.graph` / canvas) — primary lock is `contents` |
| Payload | All vertices and edges from that subgraph |
| Handoff | Same pattern as Explorer “New graph from selection” (`graphContents` nav state) |
| Over-cap / huge | **Refuse** (do not open Composer / do not hand off). Surface a clear error or disabled+reason consistent with Explorer node-cap messaging |

---

## Note 7 — Objects view

**Source:** [`UX-NOTES/Note7/Note7.md`](UX-NOTES/Note7/Note7.md) + pasted Objects screenshots.

Note 7 mirrors Query Note 6: a **stats/actions chrome row** above the results area, Query **Data**-style grid on the left, and a **dual-purpose right pane** (object viewer *or* Shelf/Matcher). All Note 7 gaps below are **resolved** (`G-UX-objgrid`, `G-UX-objchrome`, `G-UX-objview`, `G-UX-objshelf`). Shipped in WI-010.

### G-UX-objgrid — Objects grid (Note 7)

**Status:** **resolved**

| Rule | Lock |
|------|------|
| Grid chrome | Same table chrome as Query **Data** tab (vertices / table-alike mode) — headers, row height, borders, density, paging |
| Implementation | Reuse shared grid chrome (`QueryResultGrid`, `QueryStructColumns` id/type widths) where columns align; keep Objects-specific **Shelf** + checkbox columns |
| Shelf column | Keep **Add** / on-shelf toggle column and bulk add/remove behaviour |
| Id column | **Id** is a link; click opens object viewer in the right pane (**G-UX-objview**) |
| Payload columns | Same scalar-payload column pick as today (`scalarPayloadColumns`) |
| Page size | **25** rows (match Query Data) |
| Virtualization | Virtualize when **> 200** rows (match Query Structured) |

### G-UX-objchrome — Objects chrome row (Note 7)

**Status:** **resolved**

| Order (top → bottom) | Content |
|----------------------|---------|
| View title row | **G-UX-vchrome**: view title **Objects** · context bar |
| **Chrome row** | **Left:** exec stats. **Right:** actions (LTR) |
| Main split | **Left:** results grid. **Right:** dual-purpose pane (**G-UX-objview** / **G-UX-objshelf**) |

| Rule | Lock |
|------|------|
| Top alignment | Right pane content starts on the **same row** as the grid (not offset below a pane title). Chrome row sits **above** both columns — same pattern as Query’s stats row |
| Exec stats | Move result summary / `formatQueryExecStats` off the table and Matcher form onto the **left** of the chrome row (when a search result exists) |
| Duplicate stats | **Drop** embedded summary line inside the grid — stats live **only** on the chrome row (same as Query **(8)**) |
| Search | Stays in the **Matcher** tab (not on the chrome row) |
| Chrome row actions | Shelf buttons on the **right** — see **G-UX-objshelf** (Clear shelf · New graph from shelf) |

### G-UX-objview — Objects object viewer (Note 7)

**Status:** **resolved**

| Rule | Lock |
|------|------|
| Placement | **Right pane** of the main Objects split — same splitter slot as today’s Matcher/Shelf pane |
| Open | **Id link** opens viewer; row click is for bulk shelf ops only (checkbox column unchanged) |
| Component | Reuse Note 5 **G-UX-odetail** / **G-UX-over** viewer **as-is** (same as **G-UX-qview**) |
| Pane width | **Keep current width** when switching to object viewer (no collapse / no re-default) |
| Width key | Keep **`objs.ui.objects.sidePaneWidth`** — do not share Query inspect key |
| Swap | Object viewer **replaces** Matcher + Shelf tabs while an object is selected |
| Close | Explicit **close** control on viewer (same as Explorer inspect) clears selection and restores Shelf/Matcher |
| Restore | When viewer is **closed** or **no object selected** → show **Shelf \| Matcher** tabs again (**G-UX-objshelf**) |
| Entity scope | Objects view lists **entities** only — viewer opens for **node** inspect |

### G-UX-objshelf — Objects Shelf / Matcher (Note 7)

**Status:** **resolved**

| Rule | Lock |
|------|------|
| Tab order | **Shelf** first, then **Matcher** (swap from today’s Matcher \| Shelf) |
| Shelf label | **Bold** tab label when shelf has ≥1 object; keep count suffix **`(N)`** when `N > 0` |
| Clear shelf **(3)** | **Chrome row** right side (**G-UX-objchrome**) — disabled when shelf empty |
| New graph from shelf **(4)** | **Chrome row** right side; same behaviour as today — disabled when shelf empty |
| Chrome row order (LTR) | **Clear shelf** · **New graph from shelf** (same relative order as today inside Shelf tab) |
| Matcher tab | Matcher form stays in tab body; **`obj-expr` chained in context** unchanged (**G-UX5** carry-over) |
| Search | Stays in the **Matcher** tab (**G-UX-objchrome**) |

---

## Note 8 — Composer chrome

**Source:** [`UX-NOTES/Note8/Note 8.md`](UX-NOTES/Note8/Note%208.md) + pasted Composer screenshot.

Note 8 reworks Composer’s **draft graph** chrome **(1)** so it **looks like** the shared graph-context bar on Explorer / Objects / Query. All Note 8 gaps below are **resolved** (`G-UX-cgbar`, `G-UX-cnew`, `G-UX-copen`). Shipped in WI-011.

**Hard rule — visual only, never bound:** Note 8 is **chrome parity**, not context sharing. Composer’s bar must **resemble** `GraphContextBar` (Paper, layout, density, colors) but **must not** read or write **shared graph context** (**G-UX1**). Under **no circumstances** wire Composer to `GraphContextProvider`, `setGraph`, `setMatcher`, or Explorer / Objects / Query context state. Composer keeps its **own** draft graph id / annotations / stats (`currentGraphId`, Visual draft, etc.). Handoffs *into* Composer (Query, shelf, selection) seed the **draft** only — they do not imply binding.

**Supersedes:** today’s `CurrentGraphBar` on Composer (same role as **G-UX-ctx** did for old Explore-scope chrome).

### G-UX-cgbar — Composer graph bar (Note 8)

**Status:** **resolved**

| Rule | Lock |
|------|------|
| Placement | **G-UX-vchrome** row 1: view title **Composer** · **Composer graph bar** (same slot as `GraphContextBar` on Explorer / Objects / Query) |
| Visual identity | **Look only** — same chrome as shared graph-context bar: Paper, icon, id line, annotations, stats density; app colors. **Not** the same state or component wiring |
| Non-binding | **Never** bound to shared graph context (**G-UX1**). **Do not** use `GraphContextProvider`, `GraphContextBar`, `setGraph`, or `setMatcher` on Composer. Draft graph state stays in Composer (`currentGraphId`, draft entities/edges, annotations) |
| Implementation | New **`ComposerGraphBar`** — may share **presentational** primitives only (`GraphHeaderReadout`, annotation pills, copy-id, Paper shell). **Separate** component and data source; no conditional “composer mode” on shared context chrome if that couples state |
| Version | **No** version pin / dropdown — read-only **version of the opened graph** on the bar. **Open specific graph version** → separate story (deferred) |
| Stats | Node / edge counts for **Composer draft** (Visual tab), same **visual** position as shared context stats |
| Empty / no graph id | Same bar **shell** + dimmed placeholder (like shared empty context), not a separate boxed “Graph” heading |
| View actions row | **Unchanged** — Reset / Validate / Save stay on row 2 below (**G-UX-vchrome**) |
| Cleanup | Replaces today’s `CurrentGraphBar` |

### G-UX-cnew — Composer New split (Note 8)

**Status:** **resolved**

| Rule | Lock |
|------|------|
| Placement | **Before** **Open** — same action cluster as shared context bar (right side of **`ComposerGraphBar`**) |
| Control | **Split button** with menu (not a plain button) |
| Collapsed label | **New** (split chevron) |
| **Blank** | Same as today **New graph** / `onNewGraphChrome`: clear draft, clear graph id, empty canvas |
| **Matcher** | **`OpenMatcherModal`** (or equivalent) → on apply: **new blank draft** + merge matcher result entities/edges into draft (Add Objects / selection handoff pattern) |
| Menu items | **Blank** · **Matcher** |
| Matcher DSL | Same modes as elsewhere (`all` / `graph-expr` / `obj-expr` / `chained`) — **no** shared-context scoping; results merge into **Composer draft only** |
| Dirty draft | Same rules as today for New graph / Open — no new prompt UX in this WI |
| Non-binding | **Blank** / **Matcher** affect **Composer draft only** — **never** update shared graph context |

### G-UX-copen — Composer Open (Note 8)

**Status:** **resolved**

| Rule | Lock |
|------|------|
| Placement | **Same slot** as shared graph-context **Open** — right side of **`ComposerGraphBar`**, **after** New split (**G-UX-cnew**) |
| Control | **Single button** — **no** split menu (contrast with shared **Open ▾ Graph \| Matcher \| All**) |
| Label | **Open** (drop “Open graph…”) |
| Action | **`OpenGraphModal`** / open-graph dialog — **existing graph only** |
| Load | Same as today: set **Composer** draft graph id, load members into Visual draft, sync annotations |
| Non-binding | **Open** loads **Composer draft only** — **must not** call `setGraph` / `setMatcher` or otherwise change Explorer / Objects / Query shared context (**G-UX1**) |
| Styling | Match shared context **Open** primary segment — `size="xs"`, same blue styling (visual only) |

### G-UX-ctx — Graph-context chrome (v1)

**Status:** resolved  
**Source:** Note 1 § **Graph context** + Pic4 / Pic5

v1 offers **only two** context kinds (same idea as today’s Explorer Graph vs Selection):

| Mode | How entered |
|------|-------------|
| **Graph** | Open existing graph (today’s open-graph dialog) |
| **Matcher** | Matcher / selection (today’s Explorer Selection) |

**Open** is a **split button** with menu **Graph** | **Matcher** (same control in both modes).

#### Graph mode (Pic4 + Note 2)

| # | Element |
|---|---------|
| 1 | Graph icon — context is graph-based |
| 2 | Graph UUID + copy to clipboard |
| 3 | **Graph version** control (Note 2 Pic1 **(1)**) — between copy-id and annotations |
| 4 | Graph annotations — keep small |
| 5 | Stats: node count / edge count |
| 6 | **Open** split (Graph \| Matcher) |

#### Matcher / selection mode (Pic5)

| # | Element |
|---|---------|
| 1 | Selection icon — context is selection-based |
| 2 | Selection expression (may truncate) |
| 3 | Copy expression to clipboard |
| 4 | Stats: node count / edge count |
| 5 | **Open** split (same as graph mode) |

**Visual:** Pic colors are **demo only**. Ship with the rest of the workbench (white / black / blue). Annotation pills may keep type colors as today.

**Deferred (not v1):** multi-graph / multi-component chain editor, minimized↔expanded component list, named context history (`G-UX-hist`).

### G-UX-gver — Graph-context version pin

**Status:** resolved  
**Source:** [`UX-NOTES/Note2.md`](UX-NOTES/Note2/Note2.md) + Note2-Pic1

| Rule | Behaviour |
|------|-----------|
| Placement | Graph-mode bar only: between **copy id** and **annotations** (Note2 Pic1 **(1)**) |
| Control | Light dropdown (Composer object-version style) |
| Collapsed | Shows **Latest** or the selected version number |
| Menu | ~**10** most recent versions; **paging** for older; optional **from / to** datetime filter |
| Effect | Selection **pins** that graph version on shared context — Explorer, Objects, and Query all read the same pin |
| Latest | Choosing Latest clears the pin (`graphVersion = null`) |
| Cleanup | Remove Explorer left **Versions** pane; version browsing lives only in this control |
| Matcher mode | No graph-version control (selection context is not a pinned graph revision) |

### G-UX-eact — Explorer action chrome

**Status:** resolved  
**Source:** [`UX-NOTES/Note 3.md`](UX-NOTES/Note3/Note%203.md) + Pic

| Rule | Behaviour |
|------|-----------|
| (1) Count | Remove duplicate **nodes / edges** text on the type-highlight row — stats live on the graph-context bar |
| (2) Open in Query | **Remove** — Query shares graph context; use nav to open Query |
| (3)(4) → (5) | **Open in Composer** / **New graph from selection** and **Apply layout** are **view-level actions** (row placement: **G-UX-vchrome**) |
| Type pills | Unselected: outline + `+`, **full opacity**. Selected: solid fill. Independent of canvas. |
| Canvas dim | When types selected: dim nodes **not** in the selection; edges dim only if **neither** endpoint is selected (`G-UX-erow` / Note 4 type selection) |

### G-UX-vchrome — View chrome order

**Status:** resolved  
**Source:** [`UX-NOTES/Note4.md`](UX-NOTES/Note4/Note4.md)

| Rule | Behaviour |
|------|-----------|
| Scope | **Explorer · Objects · Query · Composer** (not Schema) |
| Row 1 | **Title** · gap · context bar (shared graph context or Composer **CurrentGraphBar**) — same row |
| Drop | Help/doc icon (redundant to product tour) |
| Row 2 | View-level **action buttons** below that row |
| Sizing | View-level buttons share one size (`sm`) across those views; do not retune second-level controls in this WI |

### G-UX-erow — Explorer type+actions row + resize pane

**Status:** resolved  
**Source:** Note 4 § Explorer changes

| Rule | Behaviour |
|------|-----------|
| Type + actions | One row: type pills **left** (flex, wrap multi-row); view actions **right**, vertically aligned to that block |
| Inspect pane | Vertical splitter to resize the right inspect pane (persist width) |
| Type filter pills | Selected solid / unselected `+` outline — **never** dim pill opacity |
| Type filter canvas | Dim non-selected **nodes**; dim **edges** only when **neither** endpoint is a selected type |