# WI-004 — Domain REST API + OpenAPI

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  

## Goal

Expose domain REST for collections, object/composition writes, collection-scoped search, and the dummy write pipeline. All persistence via programmatic objs-core.

## Deliverables

- [x] Collections CRUD-lite  
- [x] Write single object (G-P3 identity / `object_write_mode`)  
- [x] Write composition  
- [x] List / get / delete objects  
- [x] Search via existing matchers scoped to `graph_id` (G-P7)  
- [x] Write pipeline + no-op PreprocessingExtension / EventExtension (G-P5)  
- [x] Accepted-types gate  
- [x] MockMvc tests  

## Acceptance

- Journeys 1–2 callable via REST  
- Collection-scoped search works via matchers  
- Dummy write SPIs wired and no-op  
