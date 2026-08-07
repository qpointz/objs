# WI-006 — Shared matcher UI: `obj-expr` + visual chain builder

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Shared matcher UI  
**Status:** done  
**Depends on:** WI-001  
**Gaps:** G-S13, G-S20, G-S21, G-S22

## Goal

Expose **`obj-expr`** on every matcher surface and replace the chained JSON-only textarea with a **visual chain builder**. Each stage uses the **same visual editor as the standalone mode** for that kind. **JSON** is only for editing the **full chained matcher** document.

## Scope

- [`MatcherQueryForm.tsx`](../../../../objs-service/ui/src/MatcherQueryForm.tsx) (and tests):
  - Mode option **`obj-expr`** (JEXL textarea; hydrate/build) — same control reused inside chain stages
  - Standalone modes keep existing visual editors: `anno` (key/value rows), `anno-expr` / `obj-expr` (expression textarea)
  - **Chained** mode:
    - **Visual**: ordered list of stages; per stage: kind select (`anno` | `anno-expr` | `obj-expr`) + **embedded visual editor for that kind** (not a JSON blob per stage)
    - Add stage / delete stage / move up / move down
    - **JSON**: single editor for the **entire** chain array (`[{…}, {…}]`); round-trip with Visual when JSON is valid (parse errors stay on JSON until fixed)
  - Defaults unchanged per surface (G-S13): Composer Add objects → `obj-expr`; Explorer/Query → `anno`
- No duplicate forms — consumers keep using `MatcherQueryForm` / `MatcherLoadPanel`
- Unit tests: build/hydrate `obj-expr`; visual↔full-chain-JSON round-trip; stage kind switch preserves/resets fields sanely
- Naming: no shop/shopping/cart in component or test ids (G-S23)

## Out of scope

- Schema field-builder for `obj-expr` (G-S18)
- Per-stage JSON editors
- DnD libraries (up/down buttons are enough)
- Editing `ids` as a chain stage in the visual builder (edge-refresh only; optional later)

## Acceptance

- [x] Explorer, Query, and Composer Add objects can select and run `obj-expr`
- [x] Each chain stage shows the visual editor for its kind (`anno` rows / expr textarea)
- [x] JSON mode edits only the full chain definition; Visual ↔ JSON round-trips when valid
- [x] Add / reorder / delete stages work
- [x] Existing `anno` / `anno-expr` / hydrate-from-matcher paths still work
