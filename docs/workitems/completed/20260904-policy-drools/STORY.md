# Story: policy-drools — Drools PolicyEngine adapter

**Slug:** `policy-drools`  
**Branch:** `policy-drools`  
**Status:** completed  
**Closed:** 2026-09-04  
**Folder:** [`docs/workitems/completed/20260904-policy-drools/`](.)  
**Backlog:** [C-26](../../BACKLOG.md)  
**Base:** `origin/dev`  
**MR:** https://gitlab.qpointz.io/sandbox/bom-poc/-/merge_requests/57  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-31--normative-order) step **2 / 7**  
**Before:** [C-24 `policy-evaluate-core`](../20260904-policy-evaluate-core/STORY.md) (required)  
**Next:** [C-31 `policy-workbench`](../../planned/policy-workbench/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/policy/drools.md`](../../../design/policy/drools.md)

## Goal

Ship `:objs-policy-drools` as the first real `PolicyEngine` adapter. Fixture DRL only — no regulatory content in foundation jars.

## Locked (C-26)

| Topic | Lock |
|-------|------|
| Facts | `EntityFact` / `EdgeFact` (type, schema, schemaVersion, annotations) + `ObjectFact` for wired bag |
| Deps | `drools-bom` (poll Central) + `drools-engine` + `drools-xml-support` on `:objs-policy-drools` |
| Isolation | New session per evaluate; KB cache by single policy revision |
| Content | Fixture DRL only (G-P38) |

Detail: [`GAPS.md`](GAPS.md) · [`drools.md`](../../../design/policy/drools.md)

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock (G-P18–20) (`WI-001-design-lock.md`)
- [x] WI-002 — Drools module + fixture tests (`WI-002-drools-module.md`)
- [x] WI-003 — Living docs (`WI-003-living-docs.md`)

## Process notes

Story **closed** 2026-09-04 — archived under `completed/20260904-policy-drools/`; history squashed; MR !57.

## Out of scope

- Suites, seeds, batch, REST, product DRL packs
