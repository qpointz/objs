# WI-002 — `/graph` REST controller + error mapping

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-001  
**Gaps:** G-R1–G-R6, G-R19

## Goal

Implement graph HTTP API under `/api/v1/objs/graph` in `:objs-service`, plus shared error mapping for validation / not-found.

## Scope

| Method | Path | Behaviour |
|--------|------|-----------|
| `PUT` | `/graph` | Upsert body `BoMGraph`; return graph with ids (G-R5) |
| `POST` | `/graph/validate` | Dry-run; `200` + `BoMValidationResult` (G-R6); no persist |
| `GET` | `/graph` | Annotation query params → match-all subgraph (G-R1); empty → `400` (G-R2) |
| `DELETE` | `/graph` | Body `entityIds` / `edgeIds` (G-R3); all-or-nothing (G-R4); `204` on success |

- Keep existing `GET /api/v1/objs/status`
- Controllers return `400`/`404` with `BoMValidationResult` bodies directly (no separate advice required for these paths)
- Jackson Kotlin module on `:objs-service` for data-class JSON
- **Unit tests (mandatory, G-R19):** standalone MockMvc for all `/graph` operations + status

## Out of scope

- OpenAPI annotations (WI-004)
- Registry endpoints (WI-003)
- Load-all / per-id entity routes

## Acceptance

- [x] All four `/graph` operations behave per gaps G-R1–G-R6
- [x] Invalid writes return `400` with issue list; successful DELETE returns `204`
- [x] No HTTP endpoint dumps the entire store
- [x] Controller unit tests present and green (`:objs-service:test`)
