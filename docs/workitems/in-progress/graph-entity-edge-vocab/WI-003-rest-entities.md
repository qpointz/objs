# WI-003 — REST entity paths

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Entity REST  
**Status:** done  
**Depends on:** WI-002  
**Examples:** **workbench**

## Goal

Replace graph **member** HTTP paths with **entity** paths; rename apply-freeze structure endpoint.

## Deliverables

- [x] `ObjsGraphsController`: `/members/{entityId}` → `/entities/{entityId}` (attach/detach)
- [x] Semantics unchanged: same as today’s `NamedGraphStore.attach` / `detach` (GraphMutation MERGE membership / `entities.unset`)
- [x] `…/versions/{version}/apply-membership` → `…/apply-structure`
- [x] Call store `applyGraphVersionStructure`
- [x] Update MockMvc tests + OpenAPI summaries
- [x] Update workbench [`objs-service-ui/src/api.ts`](../../../../objs-service-ui/src/api.ts) apply caller
- [x] Fix controller/Javadoc references to `/members`

## Acceptance

- [x] No production references to `/members` or `apply-membership`
- [x] Attach/detach + apply-structure MockMvc coverage
- [x] `./gradlew :objs-service:test` (ObjsGraphsControllerTest)

## Out of scope

- Graph-scoped edge CRUD (WI-004)
- Design doc / ER updates (WI-005 / WI-006)
