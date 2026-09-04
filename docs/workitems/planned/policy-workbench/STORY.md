# Story: policy-workbench — tactical policy play UI

**Slug:** `policy-workbench`  
**Branch:** (not started)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/policy-workbench/`](.)  
**Backlog:** [C-31](../../BACKLOG.md) (ui surface; tracked with policy family)  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-31--normative-order) step **3 / 7**  
**Before:** [C-26 `policy-drools`](../policy-drools/STORY.md) (required for real engine play; hard require [C-24](../../completed/20260904-policy-evaluate-core/STORY.md))  
**Next:** [C-27 `policy-suites`](../policy-suites/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/policy/overview.md`](../../../design/policy/overview.md)

## Goal

Add a **tactical workbench UI** to load a fragment, pick policies, run evaluate, and inspect findings (highlight bound nodes/edges). Purpose: **test and play** with the foundation pipeline after Drools exists — not a compliance product.

Expect changes in `:objs-service-ui` / workbench chrome (capability-driven), plus whatever thin HTTP surface the design lock chooses (reuse pattern from jgrapht cycles / Query).

## Work Items

- [ ] WI-000 — Story scaffold
- [ ] WI-001 — Design lock (UI scope, routes, HTTP vs in-process, capability flag)
- [ ] WI-002 — Backend evaluate endpoint / wiring (if needed)
- [ ] WI-003 — Workbench Policy play view + findings on canvas
- [ ] WI-004 — Living docs / product tour note

## Out of scope

- Suite tree authoring / roll-up UI (comes with or after C-27)
- Portfolio × suite matrix
- Full policy authoring IDE
- SBOM-specific assessment product (C-30 may demo later)
- Regulatory packs in foundation jars

## Notes

Former mega-story deferred “workbench UI” (G-P23) lives **here**, not in C-30.
