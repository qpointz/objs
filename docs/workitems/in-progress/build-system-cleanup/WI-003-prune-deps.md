# WI-003 — Prune catalog and module dependencies

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Platform + prune  
**Status:** pending  
**Depends on:** WI-002

## Goal

Apply [`INVENTORY.md`](INVENTORY.md): delete unused catalog entries; strip redundant
module dependencies; tighten `api` vs `implementation`.

## Acceptance

- [ ] Unused catalog libraries/plugins removed  
- [ ] Per-module deps match inventory keep set  
- [ ] No intentional behaviour change (tests still meaningful)  
