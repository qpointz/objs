# Gaps — policy-workbench (C-31)

Close remaining **open** rows in WI-001. Assumes C-24 + C-26 usable.  
UI chrome / playground layout locked in planning pass 2026-09-04 (see Decision log).

| #      | Topic                      | Status       | Notes                                                         |
| ------ | -------------------------- | ------------ | ------------------------------------------------------------- |
| G-P23  | Workbench Policy play UI   | **resolved** | Playground page; see Decision log                             |
| G-P23a | Evaluate transport         | **resolved** | `:objs-policy-service` + check/compile; see Decision log      |
| G-P23b | Fragment source in UI      | **resolved** | Shared graph context + GraphContextBar                        |
| G-P23c | Policy picker              | **resolved** | Repo list + Add + Delete; blank Add then edit; see Decision log |
| G-P23d | Findings on canvas / tasks | **resolved** | Tabs, pills, filters, navigate; see Decision log              |
| G-P23e | Engine visibility          | **resolved** | UI-only; DROOLS-only story; see Decision log                  |
| G-P23f | Auth / enablement          | **resolved** | Capability probe + soft-fail; see Decision log                |

## Philosophy (inherited)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P38 | No product rules in foundation | **resolved** | UI may seed fixture policies only |
| G-P39 | Not default on `:objs-service` | **resolved** | Capability / optional module |

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| G-P23 | Playground UI chrome | 2026-09-04 | **Intent:** basic replaceable playground (not product Policy UI). **Nav:** top-level **Policy** after Query, before Composer; route `/policy`. **Layout:** left policy list \| content (editor \| graph \| Explorer details) \| tasks below. Splitters: left↔content; editor↔graph (default 50/50 of space left of details); graph↔details; horizontal tasks. |
| G-P23c | Policy list CRUD chrome | 2026-09-04 | Left pane: repo-backed list; **Add** at top; **trash** per row. **Add create UX:** creates a new **DROOLS** policy immediately (blank/minimal body stub), selects it, focus editor — **no** create modal. No suite tree. |
| G-P23b | Fragment / graph context | 2026-09-04 | Same shared graph context as Explorer/Objects/Query; mount **GraphContextBar** (same open dialogs). Not ComposerGraphBar. Evaluate fragment = current context. |
| G-P23 | Content columns + actions | 2026-09-04 | Col1: policy body via SyntaxCodeEditor (JSON editor stack); reloads on select. Col2: Graph View + Check/Evaluate under context bar. Col3: right inspect with **Object** \| **Tasks (N)** tabs. **Check** → compile/validate → tasks **Policy** tab. **Evaluate** (enabled when policy selected) → context fragment → tasks **Evaluations** tab. **Editor Save:** explicit **Save** persists editor body to repo; **Check/Evaluate use the current editor buffer** (may be dirty / unsaved). Dirty indicator optional. |
| G-P23d | Findings / tasks / graph | 2026-09-04 | Tasks tabs **Policy** \| **Evaluations** in **bottom** pane (full list; severity filter pills may still dim graph / optionally narrow bottom — **no** “filter bottom list to selected node/edge”). Annotate nodes/edges with severity pills (Composer StatusPill family); **highest severity** per node/edge. Severity **filter pills** above graph (Explorer type-dimming). **Right inspect pane** has tabs: **Object** \| **Tasks (N)** — `N` = findings bound to current graph selection (bold caption when `N > 0`). **Object** = Explorer `ObjectInspectPane`. **Tasks** = detail list/cards for findings on that selection (and/or the focused bottom-row finding). **Click bottom task row** → pan/select bound node/edge → Object reflects that entity/edge **and** Tasks tab shows that finding’s detail (auto-focus **Tasks** tab when arriving from bottom click; user can switch to Object). Clears prior “selection filters bottom pane” idea — scoping lives in the right **Tasks** tab instead. |
| G-P23a | Evaluate transport | 2026-09-04 | **New** `:objs-policy-service` (jgrapht/gremlin pattern): thin REST + autoconfig; **not** on `:objs-service` by default; wire into `:objs-service-app` only. **Surface:** list/create/delete policies; **evaluate** (policy × fragment from shared graph context); **check/compile validation** endpoint when needed (Drools compile / body validation → tasks **Policy** tab) — do not invent a second evaluate path for Check. **Capability** GET for soft-fail (details with G-P23f). OpenAPI tag e.g. `policy`. C-30 may extend extras later; playground CRUD+check+evaluate owns C-31. |
| G-P23e | Engine visibility | 2026-09-04 | **UI only** — show engine kind / outcome badges in playground chrome and tasks (N/A vs FAIL readable). **This story covers DROOLS only** (fixture/create defaults and Check/Evaluate paths assume `DROOLS`); no CUSTOM engine play or multi-engine picker required in C-31. Backend may still store `engine` on policy; UI does not need a CUSTOM path. |
| G-P23f | Auth / enablement | 2026-09-04 | **Not user auth** — classpath enablement only (G-P39). **Capability** `GET` on `:objs-policy-service` (jgrapht-style). UI **soft-fails** when module absent (disable Check/Evaluate; banner). **Policy nav entry always visible**. Wire module on `:objs-service-app` only. **No** roles/tokens/security layer in C-31. |
