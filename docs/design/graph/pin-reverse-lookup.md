# Pin-aware reverse lookup

**Status:** shipped (C-19)  
**Parent:** [persistence.md](persistence.md)  
**Story:** [C-19 foundation-after-versions](../../workitems/completed/20260822-foundation-after-versions/STORY.md)

## Problem

C-17 shipped `listGraphIdsForEntity(entityId)` against **live** membership (`objs_graph_entity` only).

After C-18, a graph can **freeze** membership via `createDeepGraphVersion`: pins live in
`objs_graph_version_member` keyed by `(graph_id, graph_version, entity_id, entity_version)`.
Live membership can change (detach, delete graph HEAD) while pins remain for reconstruct.

Callers such as SBOM asset **usage** must still see graphs that **memorized** an entity at freeze
time — e.g. a fingerprint BOM after the asset was removed from the live draft.

## API (locked)

Extend the existing method; **no new public name**:

```kotlin
fun listGraphIdsForEntity(entityId: UUID): List<UUID>
```

**Semantics:**

| Source | Rule |
|--------|------|
| Live | All `graph_id` from `objs_graph_entity` where `entity_id = ?` |
| Pins | All **distinct** `graph_id` from `objs_graph_version_member` where `entity_id = ?` (any pinned `entity_version`) |
| Result | Set union, stable sort by UUID string |

**Not returned:** `(graph_id, graph_version)` pairs — domain layers map `graph_id` → product rows
(SBOM application/version/BOM). Version-specific usage labels stay domain concern.

**Unchanged:** `listIncidentEdges(entityId, graphId?)` remains **live** graph-local edges only.
Pin-time edges are not incident lookup (reconstruct a deep version when needed).

## Index

Flyway **V5** (objs line): index on `objs_graph_version_member(entity_id)` for reverse pin lookup.
Both PostgreSQL and H2.

## Consumers

| App | Touch |
|-----|-------|
| **SBOM** | `AssetInventoryService.usageFor` — no API change; benefits automatically |
| **AR** | No product change required (live relations only); tests assert union behaviour |
| **Workbench** | No REST change in C-19 |

## Out of scope

- Reverse lookup of **edge** pins
- Point-in-time membership without `createDeepGraphVersion`
- Snapshot-graph id (there is no second graph id)
