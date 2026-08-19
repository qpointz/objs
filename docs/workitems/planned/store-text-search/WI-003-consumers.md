# WI-003 — Rewire workbench + SBOM + AR

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Consumers  
**Status:** planned — **not ready until WI-002**  
**Depends on:** WI-002  
**Examples:** **workbench + SBOM + AR**

## Goal

All three labeled consumers call the C-20 store API. No app loads a full graph for substring search.

- [ ] Workbench Objects (and Add-objects search if it shares the client): optional search alongside matcher; paged pool query. Do **not** replace header `/graphs/search?q=` unless WI-001 says so
- [ ] SBOM `AssetInventoryService` asset list via store. Application name `LIKE` stays domain
- [ ] AR collection object search graph-scoped via store. Collection name `LIKE` stays domain
- [ ] Tests in `:objs-service`, `:sbom-service`, `:asset-repository-service`
- [ ] Product `example.md` / README / Objects help / KDoc in this commit if the domain query param changes

## Out of scope

- Domain-table FTS
- Remaining FB-3 operators

## Acceptance

- Same search against the same pool/graph hits in all three apps
- `./gradlew :objs-core:test :objs-service:test :sbom-service:test :asset-repository-service:test`
