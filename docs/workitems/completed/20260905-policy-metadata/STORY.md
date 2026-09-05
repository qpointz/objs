# Story: policy-metadata — tags, categories, annotations for Policy list navigation

**Slug:** `policy-metadata`  
**Branch:** `policy-metadata`  
**Status:** completed  
**Closed:** 2026-09-05  
**Folder:** [`docs/workitems/completed/20260905-policy-metadata/`](.)  
**Backlog:** [C-32](../../BACKLOG.md) / [U-10](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-32--normative-order) step **4 / 8**  
**Before:** [C-31 `policy-workbench`](../../completed/20260904-policy-workbench/STORY.md); hard require [C-24](../../completed/20260904-policy-evaluate-core/STORY.md)  
**Next:** [C-27 `policy-suites`](../../planned/policy-suites/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/policy/metadata.md`](../../../design/policy/metadata.md)

## Goal

Add **pre-suite** catalog metadata on Policy artefacts so the workbench **Policy list can navigate** larger inventories: **group, filter, and find**.

| Element | Role |
|---------|------|
| **Category** | App/user-**managed** vocabulary; policy assigns one known category; primary list sections |
| **Tags** | Free-text labels on the policy; secondary filter |
| **Annotations** | Objs-shaped `Map<String,String>` on the policy; dynamic find by key=value |

Foundation supplies category **store + assignment validation**, not a fixed category enum. Examples like data-quality / schema / licensing are **application** vocabularies.

This is **not** suite configuration. Suites (C-27) remain “which policies to run and how to interpret results.”

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock (metadata GAPS) (`WI-001-design-lock.md`)
- [x] WI-002 — Policy metadata model + category repository (`WI-002-model-repo.md`)
- [x] WI-003 — HTTP + workbench Policy list navigation (`WI-003-http-workbench.md`)
- [x] WI-004 — Living docs (`WI-004-living-docs.md`)

## Locked (C-32)

| Topic | Lock |
|-------|------|
| Boundary | Pre-suite catalog navigation ≠ suite run config (C-27) |
| Category | UUID + displayName + slug `[a-z]+`; create/rename; no delete while referenced |
| Category on Policy | Always required (`categoryId` UUID) |
| Tags | Non-empty; trim + lowercase; dedupe; no max |
| Annotations | Objs `Map<String,String>`; empty OK; no reserved keys |
| Semver | `version` string = major.minor; `serial` = timestamp (object head-version rule) |
| Query | Category, tags, annotation filters + name search; no paging |
| Repos | Split `CategoryRepository` / `PolicyRepository` |
| HTTP | Extend `:objs-policy-service` |
| Workbench | Category/policy tree; category list + editor; General\|Code; Mantine confirms |

## Out of scope

- Suite trees / `evaluateSuite` / folder roll-up (C-27)
- JPA / seeds for policies and categories (C-28 — in-memory until then)
- Batch (C-29), consumer extras (C-30)
- Foundation-hardcoded category enums
- Changing flat `evaluate(fragment, policyRefs)` semantics

## Notes

Inserted in the policy family **after C-31, before C-27**. Flat list pain is catalog navigation, not a lighter suite.
