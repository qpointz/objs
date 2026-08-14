# WI-003 — qsynth load-data + collection query exec stats

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete

## Goal

Performance-test load path: regenerate synthetic CSVs with Docker qsynth (`rows_multiply`), load via REST without qsynth for the default extract. Show query record count and UI elapsed time on collection browse.

## Deliverables

- [x] `examples/asset-repository/demo/load-data/` model, committed `generated/*.csv`, `load.py`, README with Docker regen
- [x] Collection browse: total records + duration from request to first result

## Acceptance

- Default CSVs load with `load.py` against a running demo app
- Query bar shows `N records · Tms` after list/search
