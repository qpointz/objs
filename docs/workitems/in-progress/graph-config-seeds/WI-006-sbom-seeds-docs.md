# WI-006 — SBOM seed migration and design documentation

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Seed implementation  
**Status:** pending  
**Depends on:** WI-004, WI-005; G-S12 and G-S13 in [`GAPS.md`](GAPS.md)

## Goal

Exercise the foundation seed mechanism by making the SBOM example consume canonical ontology and
demo graph seed resources, then document the completed persistence and seed architecture.

## Scope

- Package all canonical SBOM schemas and 28 allowed-edge rules as classpath seed documents
- Load the ontology through the shared startup seed pipeline
- Retain typed Kotlin metadata/builders and add parity tests against canonical YAML
- Convert the property-gated demo graph to a `Graph` seed resource
- Remove redundant SBOM-specific startup registration after parity is proven
- Update:
  - `docs/design/graph/persistence.md`
  - `docs/design/graph/model.md`
  - new `docs/design/graph/seeds.md`
  - `docs/design/service/rest-api.md`
  - `docs/design/sbom/canonical-spec.md`
  - `docs/design/sbom/example.md`
  - story/backlog/milestone trackers required by repository rules

## Acceptance

- [ ] SBOM ontology is loaded from canonical YAML through the shared seed pipeline
- [ ] All 23 entity schemas and 28 allowed-edge rules match typed SBOM metadata
- [ ] SBOM saves, REST API, OpenAPI schemas, Python client, and SPA behavior remain valid
- [ ] Demo seed remains property-gated and idempotent across restarts
- [ ] Design docs define format, ordering, merge, ledger, failure, and extension behavior
- [ ] C-3/C-4 and story trackers reflect the delivered state

