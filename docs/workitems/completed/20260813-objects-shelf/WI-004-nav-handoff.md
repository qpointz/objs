# WI-004 — Nav after Explorer + Composer handoff + tests

## Goal

Wire `/objects` route; place Objects **after Explorer** in L0 nav; New graph from shelf → Composer.

## Acceptance

- [x] Nav order: Explorer · Objects · Composer · Query · Schema
- [x] Handoff uses `replaceDraft` + `graphContents` (entities only)
- [x] Vitest coverage for shelf + handoff helper
