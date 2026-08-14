# WI-003 — Batch `load.py` compositions

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete

## Goal

Synthetic load must not post one HTTP composition per edge (hours at large `rows_multiply`).

## Deliverables

- [x] Objects posted per collection in `--batch-size` chunks (default 200)
- [x] Edges posted in the same batch size with unique endpoints in the composition `objects` list
- [x] Per-collection / per-CSV post counts in the loader log

## Acceptance

- A scaled extract logs a small number of posts per table/CSV instead of one request per relation
