# WI-002 — Draft model + Visual/Text shell

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Draft shell  
**Status:** done  
**Depends on:** WI-000

## Goal

Refactor Object linter around a shared in-memory draft graph with **Visual** and **Text** tabs that round-trip the same `entities`/`edges` document.

## Scope

- Draft model/hook holding current graph document (`graphDraft.ts`, `useGraphDraft.ts`)
- Tabs: Visual (React Flow via `GraphCanvas`) + Text (`JsonYamlEditor` YAML/JSON)
- Serialize draft → text; parse text → draft on successful parse
- Keep Validate against current draft
- Preserve example graph as initial content
- Block Visual switch while Text is unparsable

## Out of scope

- Load UI (WI-003)
- Schema forms and visual CRUD affordances (WI-004/005)
- Apply (WI-006)

## Acceptance

- [x] Switching Visual ↔ Text preserves entities and edges when text is valid
- [x] Invalid text does not corrupt the last good draft; Visual switch blocked
- [x] Existing Validate still works from the draft
