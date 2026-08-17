# WI-002 — Graph and retrieval mapping

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Product + graph design  
**Status:** pending  
**Depends on:** WI-001

## Goal

Complete [`GRAPH-AND-RETRIEVAL.md`](GRAPH-AND-RETRIEVAL.md): BOM graphs vs **ephemeral** Combined SBOM, draft copy (keep split vs combine), fingerprint snapshot, migration of existing `version.graph_id`, MI/CDX entry points.

## Deliverables

- [ ] Hybrid diagram: domain tables (incl. tags, based_on, fingerprint name/category) ↔ named graphs; **no** Combined graph on the version
- [ ] Union algorithm (membership + edge collapse) used at **read time** and when materializing a fingerprint or flatten-copy
- [ ] Draft create: keep-split deep copy vs combine (one BOM from computed full union) — G-P7
- [ ] Fingerprint: persist snapshot of full union; no BOM rows
- [ ] Migration: existing `sbom_application_version.graph_id` → first BOM named `BOM`; **drop** version `graph_id`
- [ ] Annotation / id conventions; latest RELEASED = max `version_serial` (SemVer 2.0, G-Q7 / G-Q11)

## Out of scope

- Flyway code (WI-003)
- Service implementation (WI-004)

## Acceptance

- Engineer can implement WI-003/WI-004 without inventing cardinality or copy semantics
