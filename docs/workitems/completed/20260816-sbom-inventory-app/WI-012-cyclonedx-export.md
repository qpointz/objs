# WI-012 — Weak CycloneDX export demo

**Story:** [`STORY.md`](STORY.md)  
**Journey:** 1 — Application owner (Applications tab)  
**Gaps:** G-P11 (export), G-P8 cancelled (no import)  
**Retrieval:** GRAPH-AND-RETRIEVAL R16  
**Status:** complete  

## Goal

Demo: export an application **draft** or **version** BOM graph to **weak** CycloneDX JSON — same graph, different format. Not a product-grade SBOM exporter. Lives under **Applications** chrome only (not MI).

## Locks

- Import: **not needed**  
- Minimum mapping: root application → `metadata.component`; `Component` → `components[]`; `DEPENDS_ON` → `dependencies[]`; omit the rest  
- Target JSON ~ CycloneDX 1.6  
- Small additive schema tweaks allowed only to help the demo (G-A7)

## Deliverables

- [x] `CycloneDxExportService` over draft/version `graph_id`  
- [x] `GET …/draft/export/cyclonedx` and `GET …/versions/{id}/export/cyclonedx`  
- [x] UI export links (demo labelling; no certification claims)  
- [x] Tests (sample graph → recognizable CDX JSON)  

## Acceptance

- [x] User can export draft/version and open a CycloneDX-shaped JSON without graph vocabulary in the UI  
- [x] No completeness / certification claims  
