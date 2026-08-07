# WI-007 — Snapshot domain + REST (hard materialization)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Snapshot  
**Status:** done  
**Depends on:** WI-002, WI-003  
**Modules:** `:objs-core`, `:objs-service`

## Goal

Implement **hard** subgraph materialization: always create a **new** subgraph; clone live members to new ids; link the new subgraph to those clones (G-S13–G-S15). Soft create (links only) remains WI-002/003.

## Algorithm (normative)

```text
snapshot(sourceId, annotations):   // annotations REQUIRED (may be empty map)
  source = get(sourceId) or fail 404
  oldEntities = source.subgraph.entities
  oldEdges = source.subgraph.edges

  idMap: MutableMap<UUID, UUID> = empty
  newEntities = []
  for e in oldEntities:
    newId = randomUUID()
    idMap[e.id] = newId
    newAnnotations = LinkedHashMap(e.annotations)
    for (k, v) in annotations: newAnnotations[k] = v   // merge overlay
    newEntities += e.copy(id=newId, annotations=newAnnotations, payload=deepCopy(e.payload))

  newEdges = []
  for edge in oldEdges:
    newEdges += edge.copy(
      id = randomUUID(),
      source = idMap[edge.source] !!,
      target = idMap[edge.target] !!,
      properties = deepCopy(edge.properties),
    )

  write(BoMGraph(newEntities, newEdges))

  newSubgraph = create(BoMSubgraphSpec(
    annotations = annotations.toMap(),   // G-S14: header := request map exactly
    entityIds = newEntities.map { it.id }.toSet(),
    edgeIds = newEdges.map { it.id }.toSet(),
  ))

  assert source unchanged
  return newSubgraph
```

## REST

`POST /api/v1/objs/graph/subgraphs/{id}/snapshot`  
Body: `{ "annotations": { … } }` — field **required** (missing → 400; empty object OK)  
Response `201`: GET-resolve shape for the **new** subgraph.

## Transaction

Single DB transaction: clone persist + new subgraph membership, or neither.

## Tests

| Case | Expect |
|------|--------|
| Happy path | New entity/edge ids; remapped endpoints; new subgraph links to clones |
| Annotations both | New header annotations **equal** request map; each clone has overlay keys |
| Overlay merge | Clone keeps source keys not in request; request overwrites same key |
| Missing `annotations` field | 400 |
| Source intact | Source membership + payloads unchanged |
| Missing source | 404 |
| Validation failure | Full rollback |

## Out of scope

- Soft create (already WI-002/003)
- Composer button (WI-005)
- Continuous versioning

## Implementation checklist

- [ ] Domain `snapshot` API
- [ ] REST endpoint + MockMvc
- [ ] Transaction + tests
- [ ] STORY `[x]`; commit; push

## Acceptance

- [ ] Hard path always creates a new subgraph
- [ ] Cloned ids all new; edges remapped
- [ ] Request annotations on **header and** cloned entities (overlay)
- [ ] Source unchanged
- [ ] Missing annotations field → 400

## Commit message hint

`[feat] Add hard subgraph snapshot (clone + links) (WI-007)`
