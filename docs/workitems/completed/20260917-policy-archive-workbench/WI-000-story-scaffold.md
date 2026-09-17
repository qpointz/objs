# WI-000 — Story scaffold

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  



## Cold start

Read [`STORY.md`](STORY.md) § **Cold start** first. This WI only parks trackers under `planned/` — no production code.

## Goal

Park **C-39 / U-12** under `planned/policy-archive-workbench`; wire BACKLOG / MILESTONE / SEQUENCE; branch from latest `origin/dev`. Story stays in **`planned/`** until the first implementation WI is completed.

## Scope

- Story folder with `STORY.md`, `GAPS.md`, WI-000…005
- BACKLOG rows C-39 + U-12 `planned`
- MILESTONE Planned bullet
- SEQUENCE note (policy follow-up after C-33)
- Branch `policy-archive-workbench` based on latest `origin/dev`
- Archives UX locked: Policy in-page mode only (no new L0 nav)
- Detailed Cold start in STORY (what exists, axis map, file paths, agent rules)

## Acceptance

- [x] Story under `docs/workitems/planned/policy-archive-workbench/`
- [x] BACKLOG / MILESTONE / SEQUENCE point at `planned/`
- [x] Branch exists from `origin/dev`
- [x] STORY Cold start is enough for a new agent to start WI-001 without hunting
