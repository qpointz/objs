# WI-002 — Store: clear / purge / destroy / compact + exceptions

**Story:** [`STORY.md`](STORY.md)  
**Stage:** A — Store lifecycle  
**Status:** done  
**Depends on:** WI-001  
**Gaps:** G-O0, G-O0a, G-O4, G-O4a, G-O5, G-O6, G-O7, G-O9, G-O10, G-O13, G-O14  

## Goal

Implement store APIs and `GraphOperationException` (extends existing `ObjsException`; migrate `GraphException`).

## APIs

- `clearGraph` (≡ REPLACE-empty)
- `purgeGraphVersion` / `purgeAllGraphVersions`
- `destroyGraph` (explicit DAO deletes; fingerprint fail-closed via SPI/hook as locked)
- `compactEntity` / `compactEdge`
- Keep `delete` softish

## Deliverables

- [x] NamedGraphStore (or facade) methods + unit/IT tests
- [x] Exception hierarchy wired; stable codes
- [x] No REST/Composer in this WI

## Out of scope

- Backdated freeze (WI-003), REST (WI-004), Stage D
