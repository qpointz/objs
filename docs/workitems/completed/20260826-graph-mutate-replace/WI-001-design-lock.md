# WI-001 — Design lock

**Status:** done  
**Examples:** docs

## Goal

Lock MERGE vs REPLACE semantics before implementation.

## Locked (see [`GAPS.md`](GAPS.md))

| Gap | Decision |
|-----|----------|
| G-1 | Enum on mutation; REST **PATCH=MERGE**, **PUT=REPLACE** |
| G-2 | REPLACE rejects non-empty `delete` |
| G-3 | Empty REPLACE allowed (clear contents) |
| G-4 | Missing ids allocate like MERGE |
| G-5 | Composer Merge vs Overwrite (+ confirm) |
| G-6 | Seed REPLACE deferred |
| G-7 | Glossary; no renames in v1 |

**Follow-on:** G-8 (kind-first body) locked after WI-001; implemented in **WI-008**.

## Acceptance

- [x] G-1…G-5 / G-7 resolved or deferred in GAPS
- [x] Normative table in `STORY.md` locked
- [x] No production code in this WI
