# Story: policy-consumer — optional REST / example consumer

**Slug:** `policy-consumer`  
**Branch:** (never started)  
**Status:** superseded (closed 2026-09-15)  
**Folder:** [`docs/workitems/completed/20260915-policy-consumer/`](.)  
**Backlog:** [C-30](../../BACKLOG.md)  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-32--normative-order) step **9 / 9**  
**Before:** [C-29 `policy-batch`](../20260912-policy-batch/STORY.md) (preferred full stack); hard require [C-24](../20260904-policy-evaluate-core/STORY.md); results store preferred [C-33](../20260907-policy-results-persistence/STORY.md)  
**Next:** — (end of policy family)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/policy/overview.md`](../../../design/policy/overview.md)

## Goal

Optional **example-app** and/or extra REST consumer beyond the workbench play UI. Workbench tactical UI is **C-31** (`policy-workbench`) — not this story.

Never put policy modules on `:objs-service` by default. Product policies/suites stay in the app.

## Superseded

Closed without implementation WIs. Intent already delivered elsewhere:

| Gap | Covered by |
|-----|------------|
| **G-P21** REST (`:objs-policy-service`) | **C-31** — playground CRUD / check / evaluate / suite evaluate / capabilities under `/api/v1/objs/policy/**` |
| **G-P22** Example consumer | **SBOM** — assessment suites, Drools seeds, `SuiteEvaluator` + `PolicyBatchEvaluator`, portfolio/app assessment UI (`:sbom-service` + `:sbom-service-ui`) |

No further C-30 work unless a **concrete** REST gap beyond workbench play is filed separately.

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`) — park only; cancelled as superseded
- [x] WI-001 — Design lock (REST vs SBOM/example scope — no workbench) — **cancelled** (superseded)
- [x] WI-002 — Chosen consumer path — **cancelled** (SBOM already is the path)
- [x] WI-003 — Living docs — **cancelled** (closure updates trackers + design pointers)

## Out of scope

- Workbench Policy play UI (**C-31**)
- Matrix product UI
- Regulatory content in foundation
