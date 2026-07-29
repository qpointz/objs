# WI-002 — Durable catalogs and Stage 1 verification

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — PostgreSQL persistence  
**Status:** pending  
**Depends on:** WI-001; G-P4 and G-P5 in [`GAPS.md`](GAPS.md)

## Goal

Wire production to PostgreSQL-authoritative catalog implementations backed by in-memory
write-through caches, keep all consumers on catalog abstractions, preserve public REST behavior,
then provide the manual PostgreSQL acceptance procedure that gates Stage 2.

## Scope

- Compose persistent schema and allowed-edge catalogs from JPA repositories plus their in-memory
  implementations
- Hydrate caches completely from PostgreSQL before validation and HTTP traffic
- Serve catalog reads from memory, including most-specific allowed-edge matching
- Persist mutations transactionally, updating/invalidation cache only after successful commit
- Spring Boot autoconfiguration: use the persistent cached catalogs by default; retain direct
  in-memory beans for focused tests and explicit non-persistent configurations
- Depend validator, registry REST, SBOM registration, OpenAPI, seeds, typed toolkit, and related
  beans only on the **catalog abstractions**
- Ensure startup ordering is deterministic with SBOM registration and other registry contributors
- Preserve existing schema and allowed-edge lookup semantics, including wildcard specificity
- Make existing registry REST create/update/delete operations durable through the abstractions
- Add restart, validator, registry REST, and regression tests (Testcontainers where PostgreSQL-specific)
- Update persistence/model/REST design documentation and backlog C-3 status
- Provide a manual PostgreSQL checklist or script covering:
  - schema and allowed-edge-rule CRUD
  - valid and invalid graph writes
  - entity/edge and registry reads after application restart
  - deletion and referential behavior

## Stage checkpoint

After this WI is committed and pushed, stop implementation. The user runs the manual PostgreSQL
procedure and provides clarification/approval before WI-003 starts.

## Acceptance

- [ ] Production uses PostgreSQL-authoritative catalogs with in-memory read caches
- [ ] Consumers compile against catalog abstractions without knowledge of JPA or cache composition
- [ ] Startup hydrates complete caches before any validation or registry request
- [ ] Failed database writes leave cache state unchanged
- [ ] Registry REST writes survive application restart
- [ ] Validation uses persisted schemas and allowed-edge rules after restart
- [ ] Removing a schema or rule updates PostgreSQL and runtime behavior consistently
- [ ] Existing graph, registry, SBOM, and OpenAPI tests pass
- [ ] Manual test instructions include setup, requests, expected responses, restart, and cleanup
- [ ] C-3 is complete when automated checks and the user's manual acceptance pass

