# WI-002 — Toolbar version + in-place editing

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-001

## Goal

Put version selection and Create version in the content toolbar, and fold Schema Linter editors
(Visual / Expert, lint, save update) into the Schemas detail pane.

## Scope

- Content toolbar: type title, usage pills, version select + Create version (split control)
- Absorb linter save / lint / Visual / Expert into explorer detail (no separate linter page)
- Create version stays in-place (next major) using existing versioning API behaviour
- Dirty-state Save / Lint actions in toolbar

## Acceptance

- [x] Versions are chosen only from the content toolbar (not left list)
- [x] Create version works without navigating to a separate linter route
- [x] Visual and Expert editing + lint + save update work inside Schemas
