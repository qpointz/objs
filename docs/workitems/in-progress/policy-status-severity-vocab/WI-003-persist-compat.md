# WI-003 — Archive filters / compatibility

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  

## Done

- [x] Persist stores enum `.name`; load via `PolicyOutcomeStatus.parseToken` / `FindingSeverity.parseLegacyOrNull`
- [x] Filter dual-read: finding `ERROR`→HIGH, `WARNING`→MEDIUM; status `ERROR`→`EXEC_ERROR`
- [x] Dual-read filter test; no Flyway rewrite
