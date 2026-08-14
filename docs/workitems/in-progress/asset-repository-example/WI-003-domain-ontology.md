# WI-003 — Collection registry + asset type ontology

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  

## Goal

Implement hybrid collection registry (`ar_collection` + `ar_collection_type`) and seed the shared asset-type schemas (SBOM-adapted) plus related-type edge allow-list.

## Deliverables

- [x] Domain entity/tables: `ar_collection` + **`ar_collection_type`** (1-* accepted types with per-type `metadata`) (G-P2)  
- [x] Create collection → objs named graph + domain row + type rows  
- [x] Seed YAML: v1 types adapted from SBOM  
- [x] Edge allow-list: `Database` —`CONTAINS`→ `Dataset`  
- [x] Registry pack wired at startup (seed profile / test properties)  
- [x] Enforce **accepted types** gate helper on collection service  
- [x] Tests: create collection with type rows; reject object type outside accepted set  

## Acceptance

- Seeded types load; collection create allocates a graph; hybrid metadata matches GAPS; no object payloads in domain tables  
