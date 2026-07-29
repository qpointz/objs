# WI-005 — Seed import/export REST API

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Seed implementation  
**Status:** pending  
**Depends on:** WI-003; G-S10 and G-S11 in [`GAPS.md`](GAPS.md)

## Goal

Expose the common seed importer and canonical serializer over the foundation REST API without
introducing an unbounded graph dump.

## Scope

- Add a multipart YAML import endpoint using the same service as startup seeds
- Return structured counts, warnings, and document-scoped errors
- Add canonical YAML export for **all registered seed kinds**, including `Graph` (G-S11)
- Keep export handlers kind-extensible so new seed types can be added later
- Require annotation filters (or another explicit bound) for graph export — never unbounded load-all
- Generate OpenAPI descriptions and seed document examples
- Add controller slice and service integration tests

## Out of scope

- Authentication and authorization
- Remote URL import
- Asynchronous jobs for large imports
- Unfiltered export of the entire graph

## Acceptance

- [ ] REST and startup imports have identical parsing, validation, and merge behavior
- [ ] Successful import returns applied/skipped counts by document kind
- [ ] Failed import returns actionable document location and validation details
- [ ] Export emits canonical YAML covering all seed kinds present and accepted by the importer
- [ ] Graph export requires an explicit bounded filter and cannot dump the entire graph
- [ ] Adding a new seed kind later does not require changing the export endpoint contract
- [ ] OpenAPI and MockMvc tests document and verify the endpoints

