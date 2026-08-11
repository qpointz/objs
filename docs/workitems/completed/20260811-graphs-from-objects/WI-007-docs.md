# WI-007 — Docs sync

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Example + docs  
**Status:** done  
**Depends on:** WI-006  
**Modules:** `docs/design/**`, `docs/public/**` as needed

## Goal

Final design/public docs match implemented behaviour (after code). Close drift from WI-001 sketches.

## Touch

- `model.md`, `persistence.md`, `annotations-and-matchers.md` (or renamed)
- `rest-api.md` — `/graphs`, `/entities`, three matchers
- `ui.md` — graph context; Open/Save/Clone; matcher modes
- Public docs if user-facing
- GAPS follow-ups if any

## Stage gate

**STORY § Stage 4 — Manual test** → **STOP**.  
**Do not start WI-008** until `stage 4 confirmed`.

## Acceptance

- [x] Docs describe pool + graphs + minimal matchers
- [x] No pack / global-graph / old matcher lists as primary model
- [x] STORY `[x]`; commit; push

## Commit message hint

`[docs] Sync docs for graphs-from-objects (WI-007)`
