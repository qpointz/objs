# WI-005 — Living docs

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-004  

## Cold start

Update design docs to match shipped Archives mode. Do **not** close the story unless the user asks. Keep STORY Cold start accurate if paths/behaviour changed during implementation.

## Goal

Update design docs and story trackers so Archives read path is documented.

## Scope

- [`docs/design/policy/workbench.md`](../../../design/policy/workbench.md) — Archives as Policy in-page mode, axis panels, read HTTP
- Brief note in [`results.md`](../../../design/policy/results.md) or modules that archive HTTP is no longer write-only
- STORY / GAPS final consistency

## Acceptance

- [x] workbench.md describes Archives as a Policy-view mode (not a new L0 nav)
- [x] Trackers match shipped behaviour
- [x] Story remains `planned`/`in-progress` until user asks to close
