# WI-001 — Policy / suite editor layout (Note 1)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Policy/suite layout  
**Status:** done  
**Depends on:** WI-000  
**Examples:** **workbench**  
**Notes:** Intake Note removed at story closure
**Gaps:** [`GAPS.md`](GAPS.md) G-WU2-N1

## Goal

Unify Policy Evaluate and Suites into one shared chrome: left mode tabs, full-height center editor, Visual/Data on the right with a main **Output** pane underneath (splitter).

## Deliverables

- [x] Remove Categories / Tags toolbar filters (tree search remains)
- [x] Left pane **Policies | Suites** mode tabs (replace top Evaluate|Suites subnav)
- [x] Center editor full height; Object/Tasks column removed
- [x] Relocate Check/Evaluate (and suite Examine/Evaluate) into **Output** under Visual/Data
- [x] Suites: same layout including Visual + Data; results in Output
- [x] Tour + [`docs/design/policy/workbench.md`](../../../design/policy/workbench.md) + `ui.md`

## Acceptance

- [x] Policies and Suites share the Note 1 layout
- [x] Output shows Check/Evaluate findings and suite Examine/Evaluate results
- [x] No Categories/Tags toolbar filters; no Object/Tasks findings column
- [x] Tour / design docs match shipped chrome
