# Story: Composer add objects + `obj-expr`

**Slug:** `composer-draft-shopping`  
**Branch:** `composer-draft-shopping`  
**Status:** completed  
**Backlog:** C-11  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/graph/annotations-and-subgraphs.md`](../../../design/graph/annotations-and-subgraphs.md), [`docs/design/ui.md`](../../../design/ui.md), [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md)  
**Depends on:** [`object-linter-visual`](../../completed/20260805-object-linter-visual/STORY.md), [`matcher-query-language`](../../completed/20260729-matcher-query-language/STORY.md)

## Goal

Let users **add individual objects** into the Composer draft from the store: search, append (not replace), and distinguish **remove from draft** from **Apply delete**. Precise search uses a new matcher key **`obj-expr`** (`a.*` / `p.*` / `id` / `type` / `schemaVersion`). **`anno-expr` stays unchanged.**

Upgrade the **shared** matcher UI (Explorer, Composer, Query, Add objects modal): offer **`obj-expr`**, keep chain support, and replace raw-only chained editing with a **visual chain builder** (add / reorder / delete stages) plus a **JSON** edit mode for the full chain.

**Naming:** story slug/branch may say `…-shopping`; **do not** use “shop”, “shopping”, or “cart” in UI copy, component names, CSS classes, or identifiers (G-S23).

## Confirmed decisions

| Topic | Choice |
|-------|--------|
| `anno-expr` | Unchanged (bindings, pushdown, error codes) |
| New matcher | DSL **`obj-expr`**: sandboxed JEXL Boolean |
| JEXL engine | **Shared** `BoMAnnoExprEngine`; `obj-expr` uses `MATCHER_OBJ_EXPR_*` codes |
| Authoring | Users type **`obj-expr` JEXL text** (no field builder — G-S18 deferred) |
| Bindings | `a` = annotations map; `p` = payload map; top-level `id`, `type`, `schemaVersion` |
| Access | Dot/bracket on maps, e.g. `type == 'Product' && p.name == 'x' && a.app == 'y'` |
| Candidate access | Expose `id` / `type` / `schemaVersion` / `a` / `p`; **lazy** JSON deser (same as annotations) |
| Pushdown | Lower equality/`&&`/`\|\|` when possible (`type`/`id`/`schemaVersion` columns, `a`/`p` JSONB `@>`); else local JEXL |
| Matcher surfaces | **`obj-expr` in every** `MatcherQueryForm` consumer: Explorer, Composer Add objects, Query, any Load panel |
| Chaining | `obj-expr` and `ids` valid in ordered arrays; stage-0 source rules unchanged (G-S20) |
| Chain UI | **Visual builder**: add / reorder / delete stages; each stage uses the **same visual editor as standalone** for its kind (`anno` / `anno-expr` / `obj-expr`); **JSON** edits the **full chain array only** (not per-stage) |
| Composer entry | **Add objects…** modal; retire replace-style Load as primary path |
| Search UI | Shared form; Composer Add objects defaults to `obj-expr`; Explorer/Query keep `anno` default |
| Results | Entity table (id, type, ≤6 scalar payload cols); Add / In-draft; multi-select; **local page size 20** |
| Draft updates | **Immediate** merge/exclude while modal open; Done = edge refresh then close |
| `qid` | Successful Search → `beginQueryResult()` → **new `qid`** |
| Id conflict | Keep draft copy (skip overwrite) |
| Exclude vs Delete | Remove from draft = exclude (no pending delete); Delete unchanged |
| Edges | On Done: auto-merge induced edges among **store-backed** draft entity ids |
| Edge refresh query | DSL **`ids`**: `{ "ids": ["…"] }` → those entities + induced edges; invalid UUID → **400** |
| Explorer handoff | Search matcher → **auto-merge all hits + edges** into draft (append); never replace |
| UI / code naming | Prefer **Add objects**, merge, exclude — never shop/shopping/cart in product UI or code ids (G-S23) |

Example:

```yaml
obj-expr: "type == 'Dataset' && p.datasetType == 'table' && a.env == 'prod'"
```

```yaml
- anno:
    app: payments-api
- obj-expr: "type == 'Component' && p.kind == 'library'"
```

## Stages

| Stage | Work items | Exit condition |
|-------|------------|----------------|
| 0 — Scaffold | WI-000 | Story + GAPS + backlog + branch |
| 1 — Matcher | WI-001 | `obj-expr` via `/graph/query`; lazy candidates; pushdown when possible; `anno-expr` untouched |
| 2 — Draft model | WI-002 | Merge / exclude helpers + canvas Remove from draft |
| 3 — Id-set query | WI-003 | `ids` matcher for edge refresh |
| 4 — Add objects UI | WI-004 | Add objects modal + results (page 20) + Done edge merge |
| 5 — Shared matcher UI | WI-006 | `obj-expr` mode everywhere + visual chain builder (Visual \| JSON) |
| 6 — Docs | WI-005 | Design + UI docs |

## Work Items

- [x] WI-000 — Story scaffolding (`WI-000-story-scaffold.md`)
- [x] WI-001 — `obj-expr` matcher (`WI-001-obj-expr.md`)
- [x] WI-002 — Draft merge / exclude (`WI-002-draft-merge-exclude.md`)
- [x] WI-003 — `ids` matcher for edge refresh (`WI-003-ids-matcher.md`)
- [x] WI-004 — Composer Add objects modal (`WI-004-shopping-modal.md`)
- [x] WI-006 — Shared matcher UI: `obj-expr` + visual chain builder (`WI-006-matcher-ui-chain-builder.md`)
- [x] WI-005 — Design docs (`WI-005-docs.md`)

## Scope

- `obj-expr` + `ids` matcher keys; Composer Add objects modal; draft merge/exclude; induced edge merge on Done
- Shared `MatcherQueryForm`: `obj-expr` mode on **all** matcher surfaces; visual chained builder + JSON mode
- Design / UI docs

## Out of scope

- Changing `anno-expr` semantics or bindings
- Payload GIN / dedicated payload index work (pushdown still attempted via `@>`)
- Field-builder UI that emits `obj-expr` from schema fields (users write JEXL)
- Server-side query pagination / limit-offset
- Replace-draft Load path
- Edge Yes/No confirm (auto-merge only)
- Drag-and-drop polish beyond simple up/down reorder (up/down buttons sufficient in v1)
