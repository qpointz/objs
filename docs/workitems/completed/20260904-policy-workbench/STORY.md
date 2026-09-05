# Story: policy-workbench — tactical policy play UI

**Slug:** `policy-workbench`  
**Branch:** `policy-workbench`  
**Status:** completed  
**Closed:** 2026-09-04  
**Folder:** [`docs/workitems/completed/20260904-policy-workbench/`](.)  
**Backlog:** [C-31](../../BACKLOG.md) / [U-9](../../BACKLOG.md)  
**Base:** `origin/dev`  
**MR:** https://gitlab.qpointz.io/sandbox/bom-poc/-/merge_requests/58  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-32--normative-order) step **3 / 8**  
**Before:** [C-26 `policy-drools`](../20260904-policy-drools/STORY.md) (required for real engine play; hard require [C-24](../20260904-policy-evaluate-core/STORY.md))  
**Next:** [C-32 `policy-metadata`](../20260905-policy-metadata/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/policy/workbench.md`](../../../design/policy/workbench.md)

## Goal

Add a **basic, replaceable Policy playground** in the workbench: repo policy list, code editor,
shared-context Graph View + Explorer details, Check/Evaluate, and tasks (Policy | Evaluations)
with finding severity pills/filters. Not a compliance product — easy to swap UI later; keep HTTP
evaluate/CRUD seams stable.

Expect changes in `:objs-service-ui` / workbench chrome (capability-driven), plus `:objs-policy-service`
on `:objs-service-app` (jgrapht / gremlin pattern).

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock — close remaining GAPS (`WI-001-design-lock.md`)
- [x] WI-002 — Policy HTTP: list/create/delete + check + evaluate + opt-in wiring (`WI-002-policy-http.md`)
- [x] WI-003 — Policy playground UI (list, editor, graph, details, tasks) (`WI-003-policy-play-ui.md`)
- [x] WI-004 — Living docs / product tour note (`WI-004-living-docs.md`)

## Locked (C-31)

| Topic | Lock |
|-------|------|
| Intent | Basic replaceable playground |
| Nav | **Policy** after Query, before Composer; `/policy` |
| Layout | Policies \| editor \| Visual/Data \| Object/Tasks tabs; bottom Policy/Evaluations |
| Actions | Check → Policy tab; Evaluate → Evaluations + severity pills + exec stats |
| Context | Shared graph context + `GraphContextBar` |
| Transport | `:objs-policy-service` — CRUD + check + evaluate; app-only |
| Engine | UI-only visibility; **DROOLS only** |
| Enablement | Capability GET + soft-fail; nav always on; no auth |
| Add / Save | Blank DROOLS Add; explicit Save; Check/Evaluate use editor buffer |
| Tasks detail | Right **Object** \| **Tasks (N)**; no bottom selection-filter |
| Graph pane | **Visual** (canvas; disabled over node cap) \| **Data** (annotated, severity-filterable) |

## Process notes

Story **closed** 2026-09-04 — archived under `completed/20260904-policy-workbench/`; history squashed.

## Out of scope

- Policy list navigation metadata (C-32)
- Suite tree authoring / roll-up UI (comes with or after C-27)
- Portfolio × suite matrix
- Full policy authoring IDE
- SBOM-specific assessment product (C-30 may demo later)
- Regulatory packs in foundation jars

## Notes

Former mega-story deferred “workbench UI” (G-P23) lives **here**, not in C-30.
