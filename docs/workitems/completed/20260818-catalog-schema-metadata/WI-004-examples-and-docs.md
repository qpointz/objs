# WI-004 — Example seeds + living docs

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Examples + docs  
**Status:** complete  
**Depends on:** WI-003

## Goal

Exercise the contract on in-repo examples and finish any leftover living-doc nits from WI-001.

## Deliverables

- [x] SBOM catalog seeds: at least a few `AllowedEdgeRule`s with `description` / verbs; optional tags on a schema or field
- [x] Asset-repository catalog seeds if they ship `ObjectSchema` / `AllowedEdgeRule`
- [x] Example tests still green
- [x] Design docs match shipped names (WI-001 leftover)

## Out of scope

- Rewriting the full SBOM ontology
- Example-owned Flyway versions for `bom_*`
- Example schema-browse allowed-edges UI (WI-005)

## Acceptance

- An embedder can copy example YAML for the new fields
- `./gradlew :sbom-service:test` (and `:asset-repository-service:test` if touched)
