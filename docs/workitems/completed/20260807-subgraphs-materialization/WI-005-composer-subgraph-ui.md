# WI-005 — Composer save / open / snapshot

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Composer + docs  
**Status:** done  
**Depends on:** WI-004, WI-007  
**Modules:** `:objs-service` UI (`objs-service/ui`)

## Goal

Composer UX to **save** a soft-link subgraph from the draft, **open** one into the draft, and run **snapshot** with an annotations map.

## Lock before coding (G-S3)

**Resolved:** opening a subgraph **replaces** the Composer draft document and baselines (same spirit as legacy `draftFromSubgraph` / `loadSubgraph`). Merge-into-draft is **out of scope** for v1.

## UI scope

### Save soft-link subgraph

- Entry point from Composer (Visual tab toolbar or side pane).
- Collect **all draft entity ids + edge ids** (v1: whole draft; optional later: selection only — document which).
- Collect header `annotations` (free-form key/value editor — G-S11: no reserved keys).
- `POST` or `PUT` `/api/v1/objs/graph/subgraphs`.
- Show resulting subgraph id.

### Open soft-link subgraph

- Discover packs with shared **`subg-expr`** matcher (Explorer and/or Composer) and/or `GET /subgraphs` list.
- Open selected pack via **`GET /subgraphs/{id}`** (programmatic get-by-id) → **replace** draft (G-S3).
- User sees **same** live entity/edge ids as store (soft) or clone ids (if opening a hard snapshot pack).

### Matcher UI

- Add **`subg-expr`** mode to `MatcherQueryForm` (same condensed/collapsible patterns as `obj-expr`) for Explorer + Composer discovery.
- Optional: paste subgraph id → get-by-id open without expression.

### Snapshot

- Choose source subgraph id (or last saved/opened).
- Form for **required** snapshot `annotations` (free-form; applied to new subgraph header and cloned entities).
- `POST …/snapshot` → show new subgraph id; affordance to **open** the new (hard) subgraph into draft (replace).

### API client

Extend `objs-service/ui/src/api.ts` with subgraph CRUD + snapshot helpers; types in `types.ts`.

## Out of scope

- MatcherQueryForm deep redesign beyond adding **`subg-expr`** mode
- Full admin console
- Platform-reserved annotation keys (G-S11 — free-form only)
- Induce-on-save helper (G-S5)

## Tests

- Prefer Vitest unit tests for any pure mapping helpers.
- Manual smoke notes in WI completion comment if E2E harness absent.

## Implementation checklist

- [x] Lock G-S3 in GAPS — **done (replace)**; implement open as replace only
- [x] API client + UI flows
- [x] Light `ui.md` note (full sync WI-006)
- [x] STORY `[x]`; commit; push

## Acceptance

- [x] Save creates soft links without duplicating objects
- [x] Open **replaces** draft and shows same live ids
- [x] Snapshot creates new ids; user can open result (replace)
- [x] G-S3 remains **resolved** (replace) in GAPS

## Commit message hint

`[feat] Composer save/open/snapshot for soft-link subgraphs (WI-005)`
