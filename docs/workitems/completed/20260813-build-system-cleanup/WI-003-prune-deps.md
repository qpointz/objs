# WI-003 — Prune catalog and module dependencies

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Platform + prune  
**Status:** done  
**Depends on:** WI-002

## Goal

Apply [`INVENTORY.md`](INVENTORY.md): delete unused catalog entries; strip redundant
module dependencies; tighten `api` vs `implementation`.

## Acceptance

- [x] Unused catalog libraries/plugins removed  
- [x] Per-module deps match inventory keep set  
- [x] No intentional behaviour change (tests still meaningful)  
