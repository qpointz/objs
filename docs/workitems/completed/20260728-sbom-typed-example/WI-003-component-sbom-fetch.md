# WI-003 — Component, annotations, `SbomService`

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-001, WI-002  
**Gaps:** G-S4–G-S11, G-S13–G-S15, G-S21, G-S28, G-S34  
**Example:** [`docs/design/sbom/example.md`](../../../design/sbom/example.md)

## Acceptance

- [x] Component ↔ `BoMEntity` round-trip (canonical fields)
- [x] Two apps × two versions; versioned fetch isolates BOM
- [x] Fetch by `app` alone returns all versions for that app
- [x] Provenance rules at builder; data only in foundation tables
- [x] `DEPENDS_ON` uses `CanonicalEdge` properties policy
