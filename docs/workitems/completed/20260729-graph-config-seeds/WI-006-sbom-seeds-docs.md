# WI-006 — SBOM seed migration and design documentation

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Seed implementation  
**Status:** done  
**Depends on:** WI-004, WI-005; G-S12 and G-S13 in [`GAPS.md`](GAPS.md)

## Goal

Exercise the foundation seed mechanism by making the SBOM example consume canonical ontology and
demo graph seed resources, then document the completed persistence and seed architecture.

## Delivered

- `classpath:seeds/sbom-ontology.yaml` — 24 schemas + 28 allowed-edge rules
- `classpath:seeds/sbom-demo-graph.yaml` — optional second resource in the `sbom` profile
- Shared startup seed wiring via `ObjsSbomExampleAutoConfiguration`
- Typed `SbomRegistry.pack()` retained for builders + `SbomSeedParityTest`
- Design docs: `docs/design/graph/seeds.md` and updates to persistence/model/REST/SBOM/app docs

## Acceptance

- [x] SBOM ontology is loaded from canonical YAML through the shared seed pipeline
- [x] All 23 entity schemas and 28 allowed-edge rules match typed SBOM metadata
- [x] SBOM saves, REST API, OpenAPI schemas, Python client, and SPA behavior remain valid
- [x] Demo seed remains property-gated and idempotent across restarts
- [x] Design docs define format, ordering, merge, ledger, failure, and extension behavior
- [x] C-3/C-4 and story trackers reflect the delivered state
