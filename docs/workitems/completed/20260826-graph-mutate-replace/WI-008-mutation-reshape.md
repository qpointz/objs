# WI-008 — Mutation body reshape + builder

**Status:** done  
**Examples:** — (core + service + workbench + SBOM call-site migrate)  
**Depends on:** WI-001  
**Blocks:** WI-002

## Goal

Replace the op-first `upsert` / `delete` envelope with **kind-first** `entities` / `edges` ×
`set` / `unset`, and ship a Kotlin `bomMutation { }` builder so callers avoid nested ctors.
MERGE semantics unchanged; **no** `BoMMutateMode` / REPLACE yet (WI-002). REST **`PUT` remains MERGE**
until WI-003 (body break only in this WI).

## Locked shape (G-8)

```json
{
  "entities": { "set": [ /* BoMEntity */ ], "unset": [ "uuid" ] },
  "edges":    { "set": [ /* BoMEdge */ ],   "unset": [ "uuid" ] }
}
```

```kotlin
bomMutation {
    entities {
        set(entity)
        unset(oldId)
    }
    edges { set(edge) }
}

bomMutation { setAll(graph) }  // set-only helper (REPLACE / seeds)
```

- Drop `BoMGraphUpsert` / `BoMGraphDelete`.
- Named-graph `entities.unset` = detach; pool `entities.unset` = hard-delete (document).
- Breaking: REST, Composer, seeds, SBOM builders, tests — no dual-read.

## Acceptance

- [x] Domain types + `bomMutation` / `setAll` (+ optional `BoMGraphMutation.of(graph)`)
- [x] `BoMNamedGraphStore` + pool `BoMGraphStore` use new fields (MERGE behaviour)
- [x] REST body / OpenAPI / controller text use new shape
- [x] Workbench `graphDraft.ts` / `api.ts` emit kind-first JSON
- [x] SBOM + `GraphSeedHandler` + tests migrated
- [x] `./gradlew :objs-core:test :objs-service:test :sbom-service:test`
