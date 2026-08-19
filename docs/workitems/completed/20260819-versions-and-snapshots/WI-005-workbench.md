# WI-005 — Workbench

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Workbench  
**Status:** done  
**Depends on:** WI-004  
**Examples:** **workbench**

## Goal

Pool/Objects = HEAD. Composer has **two** graph actions:

- **Snapshot** = `createDeepGraphVersion` (same `graph_id`; memorized milestone on that history line).
- **Clone** = `clone()` (new graph, new entity/edge ids, empty history until its own Snapshot).

Explorer **time travel** when the **current** graph has versions: **start simple** — right-hand list, newest first; click opens that version (reconstruct, read-only). **Latest** jumps to live HEAD of **that** graph.

## Explorer (C-18)

When **Open graph** loads a graph that has at least one `bom_graph_version` row:

- Show a **right pane** “Versions” (hide if none).
- List **reverse chronological** (`ORDER BY version DESC`): version, time (`created_at`), optional `annotations.label` / `kind`.
- Click a row → `getGraphVersion(graphId, version)` onto the Explorer canvas (still **read-only**). Banner or title: viewing freeze, not HEAD.
- Control **Latest** (top of list or pane header) → live `GET /graphs/{id}` (HEAD). Clear the freeze banner.

Time travel is scoped to the graph being viewed. A clone is a different `graph_id` (pane empty until that clone is Snapshotted).

Do **not** ship the from/to time box + milestone slider in C-18 (G-D9).

## Composer

| Action | Store | After success |
|--------|-------|----------------|
| **Snapshot** | `createDeepGraphVersion` | Stay on the same graph id. Explorer version list grows. No new graph in the list. |
| **Clone** | `POST /graphs/{id}/clone` | Switch Composer to the **new** graph id. That graph has no versions until its own Snapshot. |

Today’s Snapshot button currently calls clone — **split** it: Snapshot becomes freeze; add a Clone control (same row or overflow). Enabled when saved + clean (same gate as today’s Snapshot).

## API (WI-004 must expose)

- `POST /api/v1/objs/graphs/{id}/versions` (or `/snapshot`) — freeze.
- `GET /api/v1/objs/graphs/{id}/versions` — newest first; empty → no pane.
- `GET /api/v1/objs/graphs/{id}/versions/{version}` — reconstruct.
- `POST /api/v1/objs/graphs/{id}/clone` — keep; deep copy.

## Tests

- Graph with no versions: pane hidden.
- After Snapshot: list shows the new version first; click reconstructs pin-time payloads; Latest shows current HEAD after a later edit.
- After Clone: Composer current id is the new graph; version pane hidden on the clone; source graph’s version list unchanged.
- Explorer never mutates.

## Docs

[`docs/design/ui.md`](../../../design/ui.md) Explorer Graph mode + Composer Snapshot **and** Clone rows.
