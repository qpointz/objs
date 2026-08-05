# WI-005 — Visual CRUD and linking

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Visual CRUD  
**Status:** done  
**Depends on:** WI-003, WI-004

## Goal

Enable visual construction and editing of the draft: add objects from schemas, edit via forms, create linked objects, connect existing nodes, delete with cascade.

## Scope

- Add object: pick ENTITY type (latest version default; version selectable)
- Select node/edge → side drawer with payload/properties forms + annotations
- Create linked: outgoing allow-list rules; optional copy all annotations from source
- Connect existing: choose target among draft nodes + allow-listed role
- Delete node/edge (entity delete cascades draft edges)
- new/loaded badges on selected entities

## Out of scope

- Incoming “create parent” reverse helper
- Hard cardinality blocking (warn/skip duplicate 1:1 connect only)

## Acceptance

- [x] User can add an entity from the schema list onto the canvas
- [x] Create linked produces target entity + edge and optional copied annotations
- [x] Connect existing adds an allow-listed edge between two draft nodes
- [x] Deletes update draft and pending-delete tracking
