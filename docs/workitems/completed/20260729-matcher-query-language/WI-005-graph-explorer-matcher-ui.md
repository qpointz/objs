# WI-005 — Graph explorer matcher UI

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Graph explorer UI  
**Status:** done  
**Depends on:** WI-003; G-A5–G-A6 in [`GAPS.md`](GAPS.md)

## Goal

Extend the SBOM graph explorer so users can choose among matcher modes, build the corresponding
DSL payload, and query subgraphs through the sole graph-query endpoint.

## Scope

- Update `objs-sbom-example/ui` graph explorer with a matcher-mode selector covering:
  - **`anno`** — interactive key/value editor using Mantine controls (add/remove rows, key and value
    fields), equivalently representable as a simple key/value list;
  - **`anno-expr`** — single-line text input for a JEXL annotation predicate;
  - **chained** — multi-line JSON text box for an ordered matcher array.
- On execute, build the matching DSL body and call
  `POST /api/v1/objs/graph/query` from WI-003 with `application/json`.
- Render the returned `BoMSubgraph` with the existing graph explorer visualization/layout path.
- Surface REST validation and selection errors clearly in the UI (Mantine notifications/alerts).
- Keep the current annotation-filter UX available as the `anno` mode, or provide a clear migration
  path so existing graph-explorer usage is not broken.
- Prefer existing Mantine components already used by the UI (`TextInput`, `Button`,
  `SegmentedControl` / tabs, stacks/groups, notifications). Avoid inventing a parallel design system.
- Add lightweight client-side checks for empty `anno` maps, blank `anno-expr`, and invalid chained
  JSON before calling the API.

## Mode contracts

| Mode | UI | Request body |
|------|----|--------------|
| `anno` | Key/value rows (Mantine) | `{ "anno": { "k": "v", ... } }` |
| `anno-expr` | Single-line text | `{ "anno-expr": "version == '1.0.0' && app == 'aapp-lala'" }` |
| chained | JSON textarea | `[ { "anno": { ... } }, { "anno-expr": "..." } ]` |

All matcher modes must post to the same query endpoint. The UI must not call the removed matching
GET operation.

## Out of scope

- YAML editing in the explorer (JSON is enough for the chained text box; API still supports YAML)
- Visual pipeline builder / drag-and-drop stage editor
- Payload/content expression editors
- A separate SBOM-only matcher HTTP API
- Redesigning the whole graph explorer chrome

## Acceptance

- [x] Users can switch among `anno`, `anno-expr`, and chained modes
- [x] `anno` mode uses a Mantine-based key/value editor and posts an `anno` object
- [x] `anno-expr` mode posts a single-line expression as an `anno-expr` object
- [x] Chained mode accepts JSON array text and posts it unchanged (after client JSON parse)
- [x] Every matcher mode queries subgraphs through `POST /api/v1/objs/graph/query`
- [x] Successful responses update the existing graph view
- [x] API/DSL validation errors are visible to the user
- [x] Empty/invalid inputs are blocked client-side with clear messages
