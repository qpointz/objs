# Gaps — workbench-cosmetic

| ID | Topic | Status | Notes |
|----|-------|--------|-------|
| G-WC-objload | Objects results blank while `searchBusy` | **open** | Empty-state copy gated on `!searchBusy` → blank pane on slow auto-search. Fix in WI-001. |
| G-WC-intake | Further cosmetic issues | **open** | Append WIs as user lists them; do not invent scope. |

## Detail

### G-WC-objload — Objects load splash

**Symptom:** Opening Objects with a bound graph context and a slow connection leaves the main results area blank until `queryAddObjects` returns.

**Cause:** [`ObjectsPage.tsx`](../../../objs-service-ui/src/ObjectsPage.tsx) hides placeholders while `searchBusy`; Search button `loading` is not enough.

**Resolution (WI-001):** Explorer-style absolute overlay on `data-tour="objects-results"` while `searchBusy` — `Loader` + “Loading objects…”. Overlay also covers prior rows on re-search.
