# WI-002 — API + engines + UI tokens

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  

## Done

- [x] `PolicyOutcomeStatus.EXEC_ERROR` (legacy `ERROR` parse via `parseToken`)
- [x] `FindingSeverity` enum; typed finding + reported severity fields
- [x] `SeverityRank`: five tokens only
- [x] Drools/CUSTOM helpers → null finding severity; authoring `ERROR` → status `EXEC_ERROR`
- [x] SBOM DRL finding sevs → HIGH/MEDIUM; UI labels per VOCAB-MATRIX §10
- [x] Minimal persist serialize/load + legacy parse helpers (full filter dual-read tests in WI-003)
