# Gaps — workbench-cosmetic

| ID | Topic | Status | Notes |
|----|-------|--------|-------|
| G-WC-objload | Objects results blank while `searchBusy` | **resolved** | Explorer-style overlay on results pane while busy (WI-001). |
| G-WC-objgraphs | Entity inspect: live graphs containing object | **resolved** | Versions-style Graphs section; HEAD only; WI-002. |
| G-WC-intake | Further cosmetic issues | **open** | Append WIs as user lists them; do not invent scope. |

## Detail

### G-WC-objload — Objects load splash

**Symptom:** Opening Objects with a bound graph context and a slow connection leaves the main results area blank until `queryAddObjects` returns.

**Cause:** [`ObjectsPage.tsx`](../../../objs-service-ui/src/ObjectsPage.tsx) hides placeholders while `searchBusy`; Search button `loading` is not enough.

**Resolution (WI-001):** Explorer-style absolute overlay on `data-tour="objects-results"` while `searchBusy` — `Loader` + “Loading objects…”. Overlay also covers prior rows on re-search. **Done.**

### G-WC-objgraphs — Graphs usage in object detail

**Need:** Show which live graphs contain the inspected entity (Versions-like chrome).

**Locks:** HEAD / live membership only (ignore pins); section only when `total > 0`; preview 5 most recent by graph `updatedAt`; expand with debounced search; per-row ⋮ Open/Edit (no row select; Graphs pane has no header graph id/⋮ — Note1).

**Resolution:** WI-002. **Done.**
