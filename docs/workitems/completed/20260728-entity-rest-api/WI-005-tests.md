# WI-005 — Cross-cutting API tests / gaps

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-002, WI-003, WI-004  
**Gaps:** G-R19

## Goal

Confirm controller unit tests remain green after OpenAPI annotations.

## Acceptance

- [x] `:objs-service:test` green with graph + registry controller unit tests after WI-004
- [x] No additional IT required for this story (documented as optional)
