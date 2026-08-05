# Story: Object linter visual workspace

**Slug:** `object-linter-visual`  
**Branch:** `object-linter-visual`  
**Status:** completed  
**Backlog:** U-3  
**Design:** [`docs/design/ui.md`](../../../design/ui.md), [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md), [`docs/design/graph/validation.md`](../../../design/graph/validation.md)  
**Depends on:** [`registry-graph-io-formats`](../20260805-registry-graph-io-formats/STORY.md) (merged to `dev`)

## Goal

Evolve **Object linter** from YAML/JSON-only validation into a **graph draft workspace**:

1. Optionally **load** a subgraph (matcher DSL, same modes as Graph explorer).
2. **Manipulate** the draft visually and in Text: add / edit / delete entities and edges; create linked objects; connect existing nodes.
3. **Validate** and **Apply** through one transactional **graph mutate** API (upserts + explicit deletes).

Keep existing YAML/JSON editing; add a Visual tab with polished schema-driven forms.

## Confirmed decisions

| Topic | Choice |
|-------|--------|
| Persistence | Apply via extended `PUT /api/v1/objs/graph` mutation body |
| Mutation shape | `BoMGraphMutation`: `{ upsert: { entities, edges }, delete: { entities, edges } }` (delete = ids) — do **not** overload domain `BoMGraph` |
| Validate | Same mutation body on `POST /graph/validate` (dry-run upserts + deletes) |
| `DELETE /graph` | Thin shim to mutate; deprecate in docs (canonical path = PUT mutate) |
| Seeds | Remain MERGE-only; omission never deletes |
| Connect existing | In scope — pick allow-listed role between two draft nodes |
| Entity delete | Cascade remove incident draft edges (source or target); store cascade on Apply |
| Load | Replace draft after confirm; example graph until Load/Clear |
| Forms | Custom schema-driven form from DSL `contentSchema` (not RJSF) |
| Copy annotations | Optional toggle; copy all string annotations from source |
| Cardinality | UI warn only; persist does not enforce counts |
| Text document | `entities`/`edges` only; pending deletes are Apply/UI state |

## Stages

| Stage | Work items | Exit condition |
|-------|------------|----------------|
| 0 — Scaffold | WI-000 | Story + backlog + milestone |
| 1 — Mutate API | WI-001 | Transactional mutate + validate; DELETE shim |
| 2 — Draft shell | WI-002 | Visual/Text tabs; round-trip sync |
| 3 — Load | WI-003 | Matcher load; baseline + pending deletes; cascade in draft |
| 4 — Forms | WI-004 | Schema instance forms + annotations editor |
| 5 — Visual CRUD | WI-005 | Add, edit, create-linked, connect existing, delete |
| 6 — Apply UX | WI-006 | Validate/Apply wired to mutate |
| 7 — Docs/tests | WI-007 | Design docs + light UI/core tests |

## Work Items

- [x] WI-000 — Story scaffolding (`WI-000-story-scaffold.md`)
- [x] WI-001 — Graph mutate API (`WI-001-graph-mutate-api.md`)
- [x] WI-002 — Draft model + Visual/Text shell (`WI-002-draft-visual-text-shell.md`)
- [x] WI-003 — Optional load + pending deletes (`WI-003-load-baseline-deletes.md`)
- [x] WI-004 — Schema-driven instance forms (`WI-004-schema-instance-forms.md`)
- [x] WI-005 — Visual CRUD and linking (`WI-005-visual-crud-linking.md`)
- [x] WI-006 — Validate and Apply UX (`WI-006-validate-apply-ux.md`)
- [x] WI-007 — Design docs and tests (`WI-007-docs-tests.md`)

## Scope

- `BoMGraphMutation` store/REST (PUT + validate); DELETE shim
- Object linter Visual + Text workspace
- Matcher-based Load; schema forms; linked create; connect existing; draft cascade delete
- Apply/Validate through mutate
- Design doc updates (`ui.md`, `rest-api.md`, `validation.md`)

## Out of scope

- Full undo stack; collaborative editing
- Seed export from Object linter toolbar
- Replacing Graph explorer
- Incoming “create parent” reverse helper
- Hard cardinality enforcement at persist
- Removing `DELETE /graph` in this story (shim + deprecate only)
