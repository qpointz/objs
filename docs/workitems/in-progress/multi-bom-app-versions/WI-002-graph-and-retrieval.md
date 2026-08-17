# WI-002 — Graph and retrieval mapping

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Product + graph design  
**Status:** pending  
**Depends on:** WI-001

## Goal

Complete [`GRAPH-AND-RETRIEVAL.md`](GRAPH-AND-RETRIEVAL.md): constituent graphs vs materialized aggregate, draft copy (keep split vs combine), fingerprint-always-aggregate, migration of existing single-graph versions, MI/CDX entry points.

## Deliverables

- [ ] Hybrid diagram: domain tables (incl. tags, based_on, fingerprint name/category) ↔ named graphs
- [ ] Aggregate rebuild algorithm (union membership + edge collapse)
- [ ] Draft create: keep-split deep copy vs combine (one constituent from aggregate) — G-P7
- [ ] Fingerprint: copy aggregate only; no constituent rows
- [ ] Migration: existing `sbom_application_version.graph_id` → first constituent; version gets new aggregate
- [ ] Annotation / id conventions; latest RELEASED = semver-max for MI (G-Q11)

## Out of scope

- Flyway code (WI-003)
- Service implementation (WI-004)

## Acceptance

- Engineer can implement WI-003/WI-004 without inventing cardinality or copy semantics
