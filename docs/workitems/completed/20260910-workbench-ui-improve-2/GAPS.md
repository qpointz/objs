# Gaps — workbench-ui-improve-2

| ID | Topic | Status | Notes |
|----|-------|--------|-------|
| G-WU2-intake | Further workbench UI issues | **closed** | Story closed 2026-09-10; further polish needs a new backlog item. |
| G-WU2-N1 | Policy / suite editor layout (Note 1) | **resolved** | Shared chrome; Output under Visual/Data (WI-001) |
| G-WU2-N2 | Compact graph-context chrome (Note 2) | **resolved** | Right-align compact bar (+ Schema); `N/E:` / `T/E:`; count + hover (WI-002) |
| G-WU2-N3 | Single-row view chrome (Note 3) | **resolved** | Header GraphContextBar; right-aligned actions; Schema no context bar (WI-003) |
| G-WU2-N4 | Graph Apply-layout overlay (Note 4) | **resolved** | Canvas hover toolbar + context menu; remove from view actions (WI-004) |
| G-WU2-N5 | View action button chrome (Note 5) | **resolved** | Exec split; unify button style/size (WI-005) |
| G-WU2-N6 | Graph filter toolbar (Note 6) | **resolved** | Types/Edges/Reset overlay; Policy severity (WI-006) |
| G-WU2-N6b | Fit / selector UX / Suites Object | **resolved** | Post–N6 polish (WI-007) |
| G-WU2-N7 | Policy / Query chrome (Note 7) | **resolved** | Data column funnels; suite/query chrome (WI-008) |
| G-WU2-N8 | Composer Visual L2 toolbar (Note 8) | **resolved** | Actions right; Changes only; drop on-canvas badge (WI-009) |

## Detail

### G-WU2-intake — Further workbench UI issues

**Need:** Continue cosmetic / small UX polish after [`workbench-cosmetic`](../20260903-workbench-cosmetic/STORY.md) closed with WI-000…WI-002.

**Locks:** Workbench-only (`:objs-service-ui`); intake model — no fixed WI list until listed by the user.

### G-WU2-N1 — Policy / suite editor layout

**Need:** Unify Evaluate + Suites chrome per Note 1.

**Locks:**

| Topic | Lock |
|-------|------|
| (1)(2) | Remove Categories / Tags toolbar filters |
| (4) | Left pane mode tabs (Policies ↔ Suites) |
| (6) | Center editor full height for selected policy or suite |
| (3)→(7) | Relocate bottom **tabbed** strip under Visual/Data with splitter (keep Policy\|Evaluations / Selection\|Evaluation — not a new single Output chrome) |
| **Selection** | Evaluations (and suite equivalent) lists are **selection-sensitive** to Visual/Data selection |
| (5) | No separate Object/Tasks column; **Object** tab in (3); findings for selection on **Evaluations** |
| Suites | Same layout as Policies: Visual + Data; suite results in Output |

**Resolution (WI-001):** Shared layout shipped in `:objs-service-ui` (`PolicyPage`, `PolicyPlayPage`, `PolicySuitesPage`, `PolicyModeTabs`, `PolicyGraphOutputColumn`). **Done.**

### G-WU2-N2 — Compact graph-context chrome

**Need:** Slim shared context bar per Note 2; free space between title and bar for future nav.

**Locks:**

| Topic | Lock |
|-------|------|
| Layout | Title left; flex spacer (empty for now); content-sized bar right |
| Stats | `N/E: {nodes}/{edges}` |
| Annotations | No pills on bar; `Annotations: {count}` + hover list of `k=v` |
| Hosts | Explorer / Objects / Query / Policy (`GraphContextBar`); Composer (`ComposerGraphBar`); Schema (`SchemaContextBar`) same optics |
| Out | Entity-card pills; implementing mid-row nav |

**Resolution (WI-002):** Compact `GraphContextBar` / `ComposerGraphBar` / `SchemaContextBar` + title-row spacer; `AnnotationsCountLabel`; tour + `ui.md` updated. **Done.**

### G-WU2-N3 — Single-row view chrome

**Need:** Align top chrome per Note 3.

**Locks:**

| Topic | Lock |
|-------|------|
| Layout | Shared `GraphContextBar` on AppShell header (with L0 nav); page row = quiet title \| right-aligned actions |
| Title | Smaller / lower contrast (duplicates L0 nav) |
| Hosts | Explorer, Objects, Query, Policy (header shared context); Composer (header draft bar); Schema (no context bar) |
| Composer | New ▾ → view actions split (Blank \| Matcher); Open on ComposerGraphBar |
| Schema | **No** SchemaContextBar |
| Secondary | Type pills / severity filters may stay on a row below actions |

**Resolution (WI-003):** Header-level shared context; right-aligned view actions; Schema context removed; Composer New on view actions; tour + `ui.md`. **Done.**

### G-WU2-N4 — Graph Apply-layout overlay

**Need:** Apply layout belongs on the graph surface (Note 4).

**Locks:**

| Topic | Lock |
|-------|------|
| Toolbar | Top-right on Visual/Graph canvas; semi-transparent until hover; `IconLayoutDashboard` + direction menu |
| Context menu | Apply layout (and direction) on canvas right-click |
| View actions | Remove Apply layout from title-row action bars |
| Hosts | Explorer, Query Visual, Policy Visual, Composer Visual; Schema catalog Visual same optics |

**Resolution (WI-004):** Canvas-local hover toolbar + pane/context Apply layout; removed from view action bars; tour + `ui.md`. **Done.**

### G-WU2-N5 — View action button chrome

**Need:** Consistent title-row actions + Query Exec Options UX (Note 5).

**Locks:**

| Topic | Lock |
|-------|------|
| Query | Exec is a split button; main click runs Exec; ▾ menu includes **Options** (timeout UI) |
| Size | View actions use `xs` (one step down from prior `sm`) |
| Secondary | `VIEW_ACTION_VARIANT` (`default`): export, check/validate, delete, examine, persist, reset/clear, create version, clone, open-in-composer (Query), analyze cycles, rollback |
| Primary | Filled: Explorer Open/New-from-selection; Objects New-from-shelf; Query Exec; Policy/Suites Add·Save·Evaluate; Composer New·Save; Schema Import·Create·Save·Create schema |
| Scope | Title-row / `*-view-actions` only — not canvas toolbars |

**Resolution (WI-005):** Exec ▾ → Options modal; `VIEW_ACTION_BUTTON_SIZE=xs` + `VIEW_ACTION_VARIANT=default` for secondary title-row actions; tour + `ui.md`. **Done.**

### G-WU2-N6 — Graph filter toolbar

**Need:** Compact canvas filters instead of type-pill rows (Note 6).

**Locks:**

| Topic | Lock |
|-------|------|
| Chrome | Hover-reveal toolbar (Apply-layout optics), top-left; icons for Types / Edges / Reset |
| Types | Menu checklist of canvas types (same dimming as former pills) |
| Edges | Menu checklist of edge `role` verbs |
| Combine | One filter → that rule alone; Types **and** Edges → **union** (match either); button highlight = that filter has selections |
| Reset | Clears all toolbar filters |
| Policy | Same toolbar + Severity (finding severity / None); title-row severity pills removed |
| Hosts | Explorer, Query Visual, Composer Visual, Policy/Suites Visual |

**Resolution (WI-006):** `GraphFilterToolbar` + `applyGraphCanvasFilters`; pills removed; Policy severity on Visual toolbar. **Done.**

### G-WU2-N6b — Fit to view, selector UX, Suites Object

**Need:** Post–Note 6 canvas polish (user follow-ups before Note 7).

**Locks:**

| Topic | Lock |
|-------|------|
| Fit | Icon on Apply-layout toolbar + context menu after Apply layout; fits non-dimmed nodes when any filter dims |
| Selector | Larger menu; `sm` type; selected-first sort pinned until next open; typeahead only after typing (clearable) |
| Suites | Selection \| Evaluation \| **Object**; graph select opens Object (same as Policy) |

**Resolution (WI-007):** Shipped. **Done.**

### G-WU2-N7 — Policy / Query chrome (Note 7)

**Need:** Per Note 7.

**Locks:**

| Topic | Lock |
|-------|------|
| Policy Data (1) | Severity column funnel opens checklist (remove top MultiSelect) |
| Policy Data (2) | Type column funnel, same style; shares Visual type filter set |
| Suites | Remove suite roll-up instructional label under strategy fields |
| Policy title (1) | Remove overall result pill (PASS/FAIL) beside Evaluate |
| Policy/Query (2) | Exec/eval stats right-aligned, immediately left of action buttons, spacer from title |

**Resolution (WI-008):** `ColumnFilterHeader` on Data Severity/Type; chrome cleanups. **Done.**

### G-WU2-N8 — Composer Visual L2 toolbar (Note 8)

**Need:** Per Note 8.

**Locks:**

| Topic | Lock |
|-------|------|
| (1) | Draft action cluster → right; `compact-xs`; secondary = `VIEW_ACTION_VARIANT`; New filled |
| (2) | Remove "N on canvas" badge |
| (3) | Changes only immediately left of (1) |

**Resolution (WI-009):** `ObjectLinterVisualPanel` L2 chrome. **Done.**
