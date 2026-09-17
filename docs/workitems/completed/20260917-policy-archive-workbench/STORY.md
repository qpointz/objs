# Story: policy-archive-workbench — Policy Archives mode (read-only)

**Slug:** `policy-archive-workbench`  
**Branch:** `policy-archive-workbench`  
**Status:** done  
**Folder:** [`docs/workitems/completed/20260917-policy-archive-workbench/`](.)  
**Backlog:** [C-39](../../BACKLOG.md) · [U-12](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Before:** [C-33 `policy-results-persistence`](../../completed/20260907-policy-results-persistence/STORY.md) (archive write + Suites Persist); [C-31 `policy-workbench`](../../completed/20260904-policy-workbench/STORY.md)  
**Next:** —  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/policy/workbench.md`](../../../design/policy/workbench.md) · [`results.md`](../../../design/policy/results.md) · C-33 [`PERSISTENCE-MODEL.md`](../../completed/20260907-policy-results-persistence/PERSISTENCE-MODEL.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

---

## Cold start (read this first)

### What you are building

C-33 already **writes** evaluation archives. This story adds **read + browse + inspect** so workbench users can open a saved pack and look at every **content axis that was actually stored**.

| Piece | Role |
|-------|------|
| **List** | Thin summaries over `objs_policy_evaluation` (no outcome hydrate) |
| **Load** | Full `EvaluationArchiveDocument` (optional STANDARD view omits `input`) |
| **HTTP** | `GET/DELETE /api/v1/objs/policy/evaluations/**` (write `POST …/evaluations/suite` already exists) |
| **UI** | Third **in-page** Policy mode: `Policies \| Suites \| Archives` on `/policy` |

**Not** a new L0 workbench nav item. **Not** the Suites author/evaluate editor — Archives is **read-only** (delete archive optional).

### Axis → inspector (normative UX)

Persist axes are independent. The inspector shows **only present** axes (flag true **and** payload non-null). Header chips always show which axes the pack claims.

| Persist axis | Archives tab | Feature-full means |
|--------------|--------------|--------------------|
| `results` | **Results** | Suite tree *or* flat outcomes + findings (reuse live evaluation widgets; read-only) |
| `executionContext` | **Policies** | Structured T0 snapshot (policies executed + suite/strategy meta); expandable raw JSON for extras |
| `input` | **Input** | Visual \| Data \| Raw of frozen `GraphFragment`; **does not** mutate shared graph context |

Today’s workbench write builds a **thin** `executionContext` (meta + policy name/serial/engine/status — see `buildSuiteExecutionContext`). Viewer still presents it as a proper **Policies** panel; enriching bodies is **out of scope**.

### What already exists (do not rebuild)

| Area | Location |
|------|----------|
| Port + document shape | [`EvaluationArchive.kt`](../../../../objs-policy-api/src/main/kotlin/org/poc/objs/policy/api/EvaluationArchive.kt) — `saveFlat` / `saveSuite` / `load` / `loadAsStandard` / `delete` — **no `list`** |
| JPA archive | [`JpaEvaluationArchive.kt`](../../../../objs-persistence/src/main/kotlin/org/poc/objs/core/persistence/policy/JpaEvaluationArchive.kt) |
| Tables (Flyway V8) | `objs_policy_evaluation` (+ outcome/finding); see C-33 [`PERSISTENCE-MODEL.md`](../../completed/20260907-policy-results-persistence/PERSISTENCE-MODEL.md) |
| DAO (id-only today) | [`PolicyEvaluationDaos.kt`](../../../../objs-persistence/src/main/kotlin/org/poc/objs/core/persistence/policy/PolicyEvaluationDaos.kt) |
| Persist HTTP (write) | `POST /evaluations/suite` — [`ObjsPolicyController.kt`](../../../../objs-policy-service/src/main/kotlin/org/poc/objs/policy/service/web/ObjsPolicyController.kt) · [`ArchiveHttpDtos.kt`](../../../../objs-policy-service/src/main/kotlin/org/poc/objs/policy/service/web/ArchiveHttpDtos.kt) |
| Suites Persist UI | [`PolicySuitesPage.tsx`](../../../../objs-service-ui/src/PolicySuitesPage.tsx) — dialog + axes; shows id string only after save |
| Policy modes | [`PolicyPage.tsx`](../../../../objs-service-ui/src/PolicyPage.tsx) · [`PolicyModeTabs.tsx`](../../../../objs-service-ui/src/PolicyModeTabs.tsx) · [`PolicyArchivesPage.tsx`](../../../../objs-service-ui/src/PolicyArchivesPage.tsx) — `Policies \| Suites \| Archives` |
| Evaluation tree chrome | [`SuiteEvaluationTree.tsx`](../../../../objs-service-ui/src/SuiteEvaluationTree.tsx) · [`PolicyGraphOutputColumn.tsx`](../../../../objs-service-ui/src/PolicyGraphOutputColumn.tsx) |
| Capability gate | `operations` includes `"archive"` when `EvaluationArchive` bean present |

```text
Suites: Evaluate → Persist (axes) → POST /evaluations/suite → evaluationId
                                              ↓ (this story)
Archives mode: GET /evaluations → open id → GET /evaluations/{id}
                         → Results / Policies / Input (present only)
```

### Reading order

1. This **Cold start** + **Goal** + **Boundary** below.  
2. [`GAPS.md`](GAPS.md) — close in **WI-001** before any production code (WI-002+).  
3. C-33 [`PERSISTENCE-MODEL.md`](../../completed/20260907-policy-results-persistence/PERSISTENCE-MODEL.md) — store columns / `persist_profile` / axes.  
4. [`docs/design/policy/workbench.md`](../../../design/policy/workbench.md) · [`results.md`](../../../design/policy/results.md).  
5. Implement WIs in tracker order.

### Agent execution rules

1. One WI at a time in tracker order; mark `[x]` in this file **before** starting the next.  
2. First `[x]` → move folder `planned/` → `in-progress/` (same commit).  
3. One commit per finished WI (code + tests + story trackers); then `git push`.  
4. Do **not** story-close / set C-39·U-12 `done` unless the user explicitly asks.  
5. Do **not** start WI-002+ until WI-001 closes GAPS.  
6. Prefer Kotlin in policy-api / persistence / policy-service; UI TypeScript in `:objs-service-ui`.  
7. No new AppLayout L0 route — Archives stays under `/policy`.

### Suggested first commands after checkout

```bash
git fetch origin
git checkout policy-archive-workbench
git pull
./gradlew :objs-persistence:test :objs-policy-service:test -q
./gradlew :objs-service-app:run
```

Workbench: `/workbench/policy` (Policies \| Suites \| Archives). Archive soft-fail when `"archive"` capability missing.

**Next executable WI:** — (story closed 2026-09-17).

---

## Goal

Workbench can **browse and inspect** persisted evaluation archives (C-33). **Read-only** Archives area lives **inside the existing Policy view** (`/policy`) — same L0 **Policy** nav as today; **no** new top-level workbench view. Simplified chrome. Inspection is **feature-full** for each content axis that is present in the pack (see Cold start table).

Wire front-to-back: **list + load HTTP** → Archives mode. Suites Persist dialog can **Open archive** into this mode.

## Boundary

| In C-39 / U-12 | **Not** in this story |
|----------------|------------------------|
| `EvaluationArchive.list` + GET/DELETE HTTP | Changing evaluate / persist write semantics |
| Read-only Archives **mode** under `/policy` (`Policies \| Suites \| Archives`) | New L0 nav item; separate route outside Policy |
| Feature-full read-only Results / Policies / Input panels | Re-run, re-persist, catalog edit from Archives; Suites/Policies authoring chrome |
| Flat + suite archives in one list | Portfolio × suite matrix UI |
| Display whatever was stored in `executionContext` | Enriching workbench persist to freeze full T0 policy bodies |

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock — close GAPS (`WI-001-design-lock.md`)
- [x] WI-002 — Archive list + load port / JPA (`WI-002-archive-list-port.md`)
- [x] WI-003 — HTTP read surface (`WI-003-archive-http.md`)
- [x] WI-004 — Read-only Archives UI (`WI-004-archives-ui.md`)
- [x] WI-005 — Living docs (`WI-005-living-docs.md`)

## Out of scope

- Matrix / heatmap product UI
- Auto-persist on evaluate
- Policy Play flat-persist HTTP (unless it falls out of shared GET cheaply)
- Enriching `buildSuiteExecutionContext` to store full policy bodies (follow-up)
