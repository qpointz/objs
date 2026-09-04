# Story: policy-seeds-persistence — policy store + seed format

**Slug:** `policy-seeds-persistence`  
**Branch:** (not started)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/policy-seeds-persistence/`](.)  
**Backlog:** [C-28](../../BACKLOG.md)  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-31--normative-order) step **5 / 7**  
**Before:** [C-27 `policy-suites`](../policy-suites/STORY.md) (suite seed kinds); hard require [C-24](../../in-progress/policy-evaluate-core/STORY.md)  
**Next:** [C-29 `policy-batch`](../policy-batch/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/policy/overview.md`](../../../design/policy/overview.md), [`docs/design/graph/seeds.md`](../../../design/graph/seeds.md)

## Goal

Durable Policy (and Suite) persistence via Flyway on the objs line, plus portable seed kinds (`SeedDocumentHandler`) with MERGE upsert. Product packs stay in examples/apps.

## Work Items

- [ ] WI-000 — Story scaffold
- [ ] WI-001 — Design lock (persistence + seed GAPS)
- [ ] WI-002 — Flyway + JPA/repository impl
- [ ] WI-003 — Seed handlers + import tests
- [ ] WI-004 — Living docs (`seeds.md` cross-link)

## Out of scope

- Batch (C-29), REST (C-30), concrete regulatory seed content in foundation
