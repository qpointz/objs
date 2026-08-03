# WI-003 — Object-level allowed-edge editor

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-002

## Goal

Author allowed edges on the selected **object** (entity): extend the Allowed edges table in edit
mode with create/delete for inbound and outbound. Stop authoring relations on edge-property schemas.

## Scope

- Edit mode on Allowed edges table (direction icons retained)
- Create inbound (target = selected type) / outbound (source = selected type): other type, role,
  cardinality, propertiesPolicy NONE (default) or EDGE_PROPERTIES schema ref
- Delete via `DELETE /registry/edges`; upsert via `PUT /registry/edges`
- Reload `GET /types/{type}/edges` after mutations
- Remove `EdgeRelationsEditor` / allowedRelations from edge-schema edit flow

## Acceptance

- [x] From an object schema, user can add and remove inbound and outbound allow-list rules
- [x] Edge-property schema edit UI no longer manages allowed relations
- [x] Registry round-trip preserves cardinality and property policy
