# Cycle analysis examples

**Algorithm:** `directed-cycle-regions` (strongly connected components)  
**Materialization:** `GENERIC` (default; required for REST and workbench)  
**API:** `GET/POST /api/v1/objs/graph/algorithms/*` — see [`fragments-and-analysis.md`](fragments-and-analysis.md)  
**Module:** `:objs-jgrapht-service` (optional; included on `:objs-service-app` workbench runner)

Cycle analysis finds **directed cycle regions**: SCCs with more than one node, or singleton
components with a **self-loop**. It does **not** enumerate every elementary cycle.

## Prerequisites

Start the workbench (includes Gremlin + JGraphT algorithm REST):

```bash
./gradlew :objs-service-app:run
```

Base URL: `http://localhost:8081`  
Explorer UI: `http://localhost:8081/workbench/explorer`

The SBOM inventory app (`./gradlew :sbom-service:run`, port **8080**) does **not** expose cycle
analysis endpoints or UI.

---

## Mental model

| Scope | Matcher | Extra fields | Store path |
|-------|---------|--------------|------------|
| All graphs (header union) | `{ "all": true }` or `{ "graph-expr": "…" }` | — | `GraphStore.select` |
| One live graph | `{ "obj-expr": "true" }` (match-all in graph) | `graphId` | `GraphStore.selectInGraph` |
| Pinned version | same as live graph | `graphId`, `graphVersion` | `GraphStore.selectInGraphVersion` |
| Matcher context (workbench) | stored matcher body (`all` / `graph-expr` / chained) | optional `graphId` when graph mode | same as Query / Gremlin |

The service selects entities and edges, runs **`GraphFragmentPolicy.resolve`**, then JGraphT SCC
analysis on the resolved fragment. ERROR diagnostics → `400` before analysis runs.

---

## Discover capabilities

```bash
curl -s http://localhost:8081/api/v1/objs/graph/algorithms/capabilities | jq .
```

Expected (when `:objs-jgrapht-service` is present):

```json
{
  "algorithms": [
    {
      "id": "directed-cycle-regions",
      "materializationModes": ["GENERIC"]
    }
  ]
}
```

When the module is absent (e.g. a custom app that omits `:objs-jgrapht-service`), this returns
**404**. The workbench hides **Analyze cycles** and remains fully usable.

---

## Demo graph setup (workbench)

The default workbench H2 database starts **empty** (seeds disabled). The quickest way to get a
visible cycle:

1. Open **Composer** (`/workbench/composer`).
2. Add two entities (any types with allowed edges between them).
3. Connect **A → B** and **B → A** with the same directed role (e.g. a custom role or any allowed
   edge type in your catalog).
4. **Save** the graph (creates a named graph in the store).
5. Open **Explorer**, set graph context to that graph, click **Analyze cycles**.

Expected UI: violet highlight on both nodes and both edges; alert summarizing **1 cycle region**.

### Self-loop variant

One entity with an edge **A → A** also forms a cycle region (singleton SCC with self-loop).

### Acyclic control

A single **A → B** edge with no return path yields **no cycle regions** (empty `components`).

---

## REST examples

Replace `<GRAPH_ID>` with a UUID from `GET /api/v1/objs/graphs` after saving a demo graph.

### Match-all across graph headers

Uses the same union semantics as `POST /api/v1/objs/graphs/query`:

```bash
curl -s -X POST http://localhost:8081/api/v1/objs/graph/algorithms/cycles \
  -H 'Content-Type: application/json' \
  -d '{
    "matcher": { "all": true },
    "materialization": "GENERIC"
  }' | jq .
```

### Live graph scope

Match-all **within one graph's membership**:

```bash
curl -s -X POST http://localhost:8081/api/v1/objs/graph/algorithms/cycles \
  -H 'Content-Type: application/json' \
  -d '{
    "matcher": { "obj-expr": "true" },
    "graphId": "<GRAPH_ID>",
    "materialization": "GENERIC"
  }' | jq .
```

### Pinned graph version

After creating a version (`POST /api/v1/objs/graphs/<id>/versions`):

```bash
curl -s -X POST http://localhost:8081/api/v1/objs/graph/algorithms/cycles \
  -H 'Content-Type: application/json' \
  -d '{
    "matcher": { "obj-expr": "true" },
    "graphId": "<GRAPH_ID>",
    "graphVersion": 1,
    "materialization": "GENERIC"
  }' | jq .
```

### Graph header filter

Analyze members of graphs matching an annotation expression:

```bash
curl -s -X POST http://localhost:8081/api/v1/objs/graph/algorithms/cycles \
  -H 'Content-Type: application/json' \
  -d '{
    "matcher": { "graph-expr": "a.env == '\''prod'\''" },
    "materialization": "GENERIC"
  }' | jq .
```

---

## Sample responses

### Two-node cycle (A ↔ B)

Fixture used in [`GraphFragmentFixtures.twoNodeCycle()`](../../../objs-jgrapht-core/src/test/kotlin/org/poc/objs/jgrapht/core/testsupport/GraphFragmentFixtures.kt):

| Entity | UUID |
|--------|------|
| A | `00000000-0000-0000-0000-000000000001` |
| B | `00000000-0000-0000-0000-000000000002` |
| edge A→B | `00000000-0000-0000-0000-000000000101` |
| edge B→A | `00000000-0000-0000-0000-000000000102` |

```json
{
  "algorithm": "directed-cycle-regions",
  "components": [
    {
      "id": "00000000-0000-0000-0000-000000000001",
      "entityIds": [
        "00000000-0000-0000-0000-000000000001",
        "00000000-0000-0000-0000-000000000002"
      ],
      "edgeIds": [
        "00000000-0000-0000-0000-000000000101",
        "00000000-0000-0000-0000-000000000102"
      ]
    }
  ],
  "stats": {
    "entityCount": 2,
    "edgeCount": 2,
    "cyclicComponentCount": 1
  },
  "diagnostics": []
}
```

**Component id** = smallest entity UUID in the SCC (unsigned RFC 4122 byte order). Here that is
`…0001`.

### Self-loop

One entity, one edge A→A — `cyclicComponentCount: 1`, `entityCount: 1`, `edgeCount: 1`. See
`GraphFragmentFixtures.selfLoop()`.

### No cycles

Acyclic graph or empty selection:

```json
{
  "algorithm": "directed-cycle-regions",
  "components": [],
  "stats": {
    "entityCount": 0,
    "edgeCount": 0,
    "cyclicComponentCount": 0
  },
  "diagnostics": []
}
```

Workbench message: *No directed cycle regions found.*

---

## Workbench Explorer

When capabilities advertise `directed-cycle-regions` + `GENERIC`:

1. Set shared graph context (graph, matcher, or All).
2. Load the canvas (open graph or run Query / Objects with results).
3. Click **Analyze cycles** (violet button in row 2).
4. Cycle entities and edges highlight in **violet**, independent of selection and type-filter dimming.
5. Click **×** beside the button to clear highlights without changing graph data.

Highlights clear automatically when graph context, version pin, or matcher changes.

---

## JVM programmatic use

Foundation modules (no Spring):

```kotlin
import org.poc.objs.jgrapht.core.analysis.DirectedCycleRegionAnalyzer
import org.poc.objs.jgrapht.core.testsupport.GraphFragmentFixtures

val analyzer = DirectedCycleRegionAnalyzer()
val analysis = analyzer.analyze(GraphFragmentFixtures.twoNodeCycle())
// analysis.components.single().entityIds.size == 2
```

Materialization guard before native graph build:

```kotlin
import org.poc.objs.api.domain.ResolvedGraphMaterialization.requireMaterializable

val fragment = DefaultGraphFragmentPolicy.resolve(input)
requireMaterializable(fragment) // throws if ERROR diagnostics present
```

Example consumers using the shared policy boundary:

- SBOM ephemeral union: `BomUnion.of(graphs)` → `ResolvedGraphFragment`
- Asset repository composition: `ObjectWriteService.writeComposition` → policy → reject errors

---

## SBOM inventory note

`:sbom-service` seeded demo graphs (e.g. `sbom-demo-graph.yaml`) are **acyclic** by default
(`DEPENDS_ON` is one-way). Use cycle analysis on SBOM data only after adding a return dependency
(e.g. in Composer) or combining graphs where union policy retains both directions. The SBOM **UI**
does not include cycle actions; use workbench Explorer or REST against a workbench store.

---

## Errors (summary)

| Situation | HTTP |
|-----------|------|
| Invalid / retired matcher | `400` + validation issues |
| Graph or version not found | `404` |
| Fragment policy ERROR diagnostics | `400` |
| Unsupported `materialization` | `400` |
| Algorithm module not installed | capabilities `404`; cycles route unavailable |

---

## Related

- Architecture and DTOs: [`fragments-and-analysis.md`](fragments-and-analysis.md)
- Gremlin (same matcher, different engine): [`gremlin-examples.md`](gremlin-examples.md)
- REST table: [`../service/rest-api.md`](../service/rest-api.md)
- Workbench Explorer: [`../ui.md`](../ui.md)
