# WI-002 — Graph and retrieval mapping

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Product + graph design  
**Status:** pending  
**Depends on:** WI-001

## Goal

Complete [`GRAPH-AND-RETRIEVAL.md`](GRAPH-AND-RETRIEVAL.md): constituent graphs vs materialized aggregate, draft-from-based-on copy, migration of existing single-graph versions, fingerprint/MI/CDX entry points.

## Deliverables

- [ ] Hybrid diagram: domain tables ↔ named graphs
- [ ] Aggregate rebuild algorithm (union + edge collapse)
- [ ] Draft create copy depth (per G-Q5)
- [ ] Migration strategy for existing `sbom_application_version.graph_id`
- [ ] Annotation / id conventions

## Out of scope

- Flyway code (WI-003)
- Service implementation (WI-004)

## Acceptance

- Engineer can implement WI-003/WI-004 without inventing cardinality or copy semantics
