# WI-004 — Composer L2 + persist chrome

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Composer / Query L2  
**Status:** done  
**Depends on:** WI-001  
**Modules:** `:objs-service` UI

## Goal

Regroup Composer chrome to match product locks:

1. Title **Composer** (replace “Object linter”); remove **Browse schemas**.
2. Selection type → schema: **new tab** (`target="_blank"`).
3. Visual L2: split **New** ▾ (primary = **New**; menu **New** / **New linked**) + **Link** + **Add objects…** (Visual only — **hidden** on Text). Enablement = today’s selection rules with **disabled + tooltip**. Context menus renamed **New linked** / **Link**.
4. Both tabs L2: **Validate** (L2 only, no duplicates), **Save**, **Snapshot** as separate controls (no Save ▾ / Clone menu).
5. **Reset / Clear** remain on **L1** secondary.
6. **Save** enabled when dirty, or `currentGraphId == null` (first Save **creates** — G-U5), or `neverSavedSinceCreate`; disabled when clean saved graph.
   - **`currentGraphId == null`:** create graph — membership for draft entity ids (**same ids**, no clone) + **edge upserts** (+ modified entity upserts per draft logic).
   - **Graph id set:** today’s scoped `PUT` mutation.
7. **Snapshot** enabled iff saved + clean (not dirty); label **Snapshot**; dialog = clone dialog; on success **switch** to new id + load.
8. **New graph** (chrome): **clears draft**, **resets graph id**, empty edit session — **not** the Explorer handoff path and **not** implicit on handoff.
9. Shared graph-id + annotation-pills readout (WI-002). Empty selection → side pane edits **graph-level annotations**.

Reset/Clear stay secondary. Composer remains single-graph edit (or unsaved draft until first Save).

## Acceptance

- [x] Page title is Composer; Browse schemas gone
- [x] Schema links from selection open in a new tab
- [x] Visual L2 hosts New ▾ / Link / Add objects…; Text L2 does not
- [x] Save and Snapshot are separate; Clone menu removed
- [x] Save with null graphId creates graph (membership + edge upserts); New graph clears draft+id
- [x] Save / Snapshot enablement matches gates above
- [x] Validate on L2; Reset/Clear secondary
- [x] Empty selection: side pane edits graph annotations; header pills stay in sync after save
