# WI-004 — REST + Composer (Stages A–C)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** C1  
**Status:** done  
**Depends on:** WI-002, WI-003  
**Gaps:** G-O8, G-O11  

## Goal

Expose A–C store ops on `:objs-service` and Composer (clear / purge / destroy / dated create-version / compact). **No** Stage D reset/apply UI yet. Update product tour.

## Deliverables

- [x] REST per G-O8 (without reset/apply)
- [x] Composer actions + confirms
- [x] MockMvc + tour hooks
- [x] OpenAPI text

## Out of scope

- Stage D endpoints/UI (WI-007)
- Recipes body (WI-005)
