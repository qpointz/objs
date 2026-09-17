# Gaps — policy-archive-workbench (C-39 / U-12)

**Status:** resolved (WI-001 design lock 2026-09-17).  

**Cold start (read this after STORY Cold start):**

1. [`STORY.md`](STORY.md) § **Cold start** — what exists, axis→tab map, file paths, agent rules.  
2. This file — locked list/HTTP/UI decisions (Decision log).  
3. C-33 [`PERSISTENCE-MODEL.md`](../../completed/20260907-policy-results-persistence/PERSISTENCE-MODEL.md) — `objs_policy_evaluation` / `persist_profile` / axis columns.  
4. [`workbench.md`](../../../design/policy/workbench.md) — Policy chrome (Policies\|Suites today; Archives in this story).  

**Why these gaps existed:** C-33 shipped durable archives + Suites **write** (`POST …/evaluations/suite`) but no list/read HTTP and no workbench reload/inspect UI.

**Do not reopen C-33 store shape** (tables, axes, presets, no catalog FK). This story only adds **list + read HTTP + Archives mode**.

---

## Port & HTTP

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-A1 | List summaries | **resolved** | `EvaluationArchiveSummary` + `list(limit, offset, kind?, tag?)`; row-only, no outcome hydrate. See Decision log. |
| G-A2 | Load HTTP | **resolved** | `GET /evaluations`, `GET /evaluations/{id}` (+ `?view=standard`), `DELETE` 404/204; 503 soft-fail. See Decision log. |
| G-A3 | Flat vs suite | **resolved** | One summary/document model; `kind` discriminator; UI branches Results only. See Decision log. |

## Workbench UI

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-A4 | Archives inside Policy view | **resolved** | Third in-page mode on `/policy`; read-only `PolicyArchivesPage`; no new L0 nav. See Decision log. |
| G-A5 | Results panel | **resolved** | Tree vs flat outcomes; reuse Suites widgets read-only. See Decision log. |
| G-A6 | Policies panel | **resolved** | Structured thin `executionContext` + raw JSON extras; tab label **Policies**. See Decision log. |
| G-A7 | Input panel | **resolved** | Visual \| Data \| Raw of frozen fragment; no shared graph mutation; full load in UI. See Decision log. |
| G-A8 | Conditional axes | **resolved** | Tabs iff flag ∧ payload; header chips for claimed axes. See Decision log. |
| G-A9 | Persist → Open | **resolved** | Suites Persist success → **Open archive** → Archives mode + load id. See Decision log. |

## Explicitly deferred

| Topic | Notes |
|-------|-------|
| Richer T0 policy bodies in `executionContext` | Viewer shows stored snapshot; enriching write path is a follow-up |
| Flat Policy Play persist HTTP | Out unless trivial on shared GET |
| Matrix UI | Product / not foundation |
| Deep-link `?archive=<id>` | Nice-to-have; not required in v1 |
| STANDARD load toggle in UI | API supports `?view=standard`; Archives UI always requests full load |

---

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| G-A1 | List port + summary | 2026-09-17 | Add `EvaluationArchiveSummary` + `list(limit=50, offset=0, kind?, tag?)` on `EvaluationArchive`. JPA over `objs_policy_evaluation` only (no Flyway, no outcome/finding join, no tree/context/input hydrate). Order: `evaluated_at DESC`, then `evaluation_id DESC`. `kind`: exact match when set. `tag`: row matches if `tags` JSON array contains that string (portable H2/Postgres impl in WI-002). Clamp `limit` max **200**; **no** total count (empty page = end). Summary fields: `evaluationId`, `kind`, `name`, `description`, `evaluatedAtEpochMs`, `overallStatus`, `overallSeverity`, `suiteId`, `suiteName`, `tags`, `presetName`, `origin`, `durationMs`, `axes` (from `persist_profile` for list chips). Omit: `annotations`, outcomes, tree, executionContext, input. |
| G-A2 | Read HTTP | 2026-09-17 | Under `/api/v1/objs/policy`: `GET /evaluations` → `{ "items": […summaries] }` with query `limit`/`offset`/`kind`/`tag`; `GET /evaluations/{id}` → full document via `load` (input included if stored); `GET /evaluations/{id}?view=standard` → `loadAsStandard`; unknown `view` → **400**; missing id → **404**; `DELETE /evaluations/{id}` → **204** on success, **404** if missing; archive bean absent → **503** (same soft-fail as `POST …/evaluations/suite`). Do not collide with `POST …/evaluations/suite`. Response mirrors `EvaluationArchiveDocument` (thin DTO map OK). |
| G-A3 | Flat + suite | 2026-09-17 | One summary + document type for both; `kind` is `FLAT` \| `SUITE` string. One list/get/delete — no `/flat`/`/suite` read split. Load unchanged: FLAT → `tree == null`; SUITE → tree and/or outcomes as stored. Results UI (G-A5) branches on `tree != null` vs flat outcomes — presentation only. |
| G-A4 | Archives mode shell | 2026-09-17 | Extend `PolicyWorkbenchMode` with `'archives'`. `PolicyModeTabs`: **Policies \| Suites \| Archives**. `PolicyPage` mounts `PolicyArchivesPage` on same `/policy` — **no** AppLayout L0 item, **no** `/policy/archives` route. Layout: left summary list \| right inspector (or empty). Read-only: no Evaluate / Save suite / Persist / catalog edit / draft graph. Soft-fail: Archives tab visible when `"archive"` capability missing, but list shows unavailable and actions disabled. Optional Delete (confirm) → DELETE + refresh list. |
| G-A5 | Results inspector | 2026-09-17 | When present (G-A8): `tree != null` → reuse `SuiteEvaluationTree` read-only; else flat `PolicyEvaluationTable` / outcomes+findings. Findings drill-down stays **inside** the archive inspector — does not drive shared live-graph Visual selection. Header shows `overallStatus` / `overallSeverity` when set. No re-run, filter-edit, or “open in Suites”. |
| G-A6 | Policies inspector | 2026-09-17 | Tab label **Policies**. When present: structured blocks for known thin-snapshot keys — meta (`source`, `kind`, `suiteId`, `suiteName`, `executionStrategyKind`, `rollUpStrategyKind`) + policies table (`name`, `serial`, `engineKind`, `status` from `executionContext.policies[]`). Other keys → expandable pretty **Raw JSON**. Missing `policies` → meta + raw only; no fake catalog bodies. Write-path enrichment stays deferred. |
| G-A7 | Input inspector | 2026-09-17 | When present: sub-tabs **Visual \| Data \| Raw** of frozen `GraphFragment`. Fragment is **local to the inspector** — never mutates shared graph context / `currentGraphId` / Composer bar. Prefer injecting fragment into existing graph view primitives without context writes; Fit/layout local-only OK. Archives UI always uses **full** load (no STANDARD toggle in v1). Archives mode has **no** Suites/Play shared Visual\|Data column — Input owns its own Visual\|Data\|Raw. |
| G-A8 | Conditional axes | 2026-09-17 | Inspector tab appears iff axis flag **true** and payload non-null: Results → `tree != null \|\| outcomes.length > 0`; Policies → `executionContext != null`; Input → `input != null`. Header chips always show claimed axes from document/summary `axes` (muted when flag true but payload null). Also show `kind`, name-or-id, `evaluatedAt`, `overallStatus`, `presetName` when set. Default tab: first present among Results → Policies → Input; none → empty (“No durable axes in this pack”). |
| G-A9 | Persist → Open | 2026-09-17 | After Suites Persist success: keep archived id display; add **Open archive**. Click → `onModeChange('archives')`, pass `evaluationId` into Archives (lift on `PolicyPage` or one-shot callback), select row + `GET /evaluations/{id}` full. Hide **Open archive** when `"archive"` capability missing. Deep-link `?archive=` deferred. |
