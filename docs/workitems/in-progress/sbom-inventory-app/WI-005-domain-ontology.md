# WI-005 — Domain model + ontology alignment

**Story:** [`STORY.md`](STORY.md)  
**Depends on:** WI-001, WI-002  

## Goal

Align / extend existing SBOM graph schema only as needed; wire application to **compile object model from schema** (G-A7 / G-A8). Application inventory and portfolios remain SBOM tables (G-P3 / G-P10).

## Deliverables (fill after Stage 1)

- [ ] Reuse `sbom-ontology` / catalog; document any additive schema changes  
- [ ] Codegen / compile pipeline: graph schema → application object model (user-supplied schema generation)  
- [ ] Retire hand Wave* as SoT once compile path works  
- [ ] Mapping documented in GRAPH-AND-RETRIEVAL + design/sbom  
- [ ] Note any schema tweaks that help weak CDX (WI-012) or MI signals (WI-013) — keep additive and small  

## Acceptance

- Asset types come from (compiled) graph schema; ontology reuse preferred over rewrite  
- Journey 1–2 modeling choices (G-P4/G-P5) covered by schema extensions or documented encodings  
- Portfolio / MI stay domain-table driven; no portfolio objs entities  
